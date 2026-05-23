package com.myblog.application.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 两级缓存实现：L1 = Caffeine 本地缓存，L2 = Redis 分布式缓存（使用 StringRedisTemplate）。
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author Codex
 * @since 1.0.0
 */
public class TwoTierCache<K, V> implements Cache<K, V> {

    private static final Logger log = LoggerFactory.getLogger(TwoTierCache.class);

    private static final String TOPIC_PREFIX = "twoTierCache:inv:";
    private static final String ENTRY_PREFIX = "twoTierCache:entry:";
    private static final String KEY_SET_KEY = "twoTierCache:keys:";

    private static final ObjectMapper cachingMapper = createCachingMapper();

    private static ObjectMapper createCachingMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(DeserializationFeature.USE_LONG_FOR_INTS);
        mapper.activateDefaultTypingAsProperty(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            "@class"
        );
        return mapper;
    }

    private final String name;
    private final Cache<K, V> l1Cache;
    private final StringRedisTemplate redis;
    private final RTopic invalidationTopic;
    private final String instanceId;
    private final long l2TtlSec;

    public TwoTierCache(String name, long l1TtlMs, long l2TtlSec,
                         long maxSize, RedissonClient redisson, StringRedisTemplate redis) {
        this.name = name;
        this.instanceId = UUID.randomUUID().toString();
        this.l2TtlSec = l2TtlSec;
        this.redis = redis;

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
            .expireAfterWrite(l1TtlMs, TimeUnit.MILLISECONDS);
        if (maxSize > 0) {
            builder.maximumSize(maxSize);
        }
        this.l1Cache = builder.build();

        this.invalidationTopic = redisson.getTopic(TOPIC_PREFIX + name, StringCodec.INSTANCE);

        this.invalidationTopic.addListener(String.class, (channel, msg) -> {
            if (msg == null) return;
            String[] parts = msg.split(":", 2);
            if (parts.length == 2 && instanceId.equals(parts[0])) return;
            if ("*".equals(parts.length == 2 ? parts[1] : msg)) {
                l1Cache.invalidateAll();
            } else {
                l1Cache.invalidate(parts.length == 2 ? parts[1] : msg);
            }
        });
    }

    private String keyToString(K key) {
        if (key == null) return "null";
        return key.toString();
    }

    private String entryKey(K key) {
        return ENTRY_PREFIX + name + ":" + keyToString(key);
    }

    private String valueToJson(V value) {
        try {
            return cachingMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cache value for " + name, e);
        }
    }

    private static final int MAX_LOG_LENGTH = 200;

    private String valueToLogString(V value) {
        if (value == null) {
            return "null";
        }
        try {
            String json = cachingMapper.writeValueAsString(value);
            if (json.length() > MAX_LOG_LENGTH) {
                return json.substring(0, MAX_LOG_LENGTH) + "...";
            }
            return json;
        } catch (JsonProcessingException e) {
            String s = String.valueOf(value);
            if (s.length() > MAX_LOG_LENGTH) {
                return s.substring(0, MAX_LOG_LENGTH) + "...";
            }
            return s;
        }
    }

    @SuppressWarnings("unchecked")
    private V jsonToValue(String json) {
        try {
            Object value = cachingMapper.readValue(json, Object.class);
            if (value instanceof Number) {
                value = ((Number) value).longValue();
            }
            return (V) value;
        } catch (JsonProcessingException e) {
            log.warn("TwoTierCache[{}] deserialize failed", name, e);
            return null;
        }
    }

    @Override
    public V getIfPresent(Object key) {
        V value = l1Cache.getIfPresent(key);
        if (value != null) {
            log.debug("TwoTierCache[{}] L1 hit for key={}, value={}", name, key, valueToLogString(value));
            return value;
        }

        @SuppressWarnings("unchecked")
        K k = (K) key;
        String json = redis.opsForValue().get(entryKey(k));
        if (json != null) {
            value = jsonToValue(json);
            if (value != null) {
                l1Cache.put(k, value);
                log.debug("TwoTierCache[{}] L2 hit for key={}, value={}", name, key, valueToLogString(value));
                return value;
            }
            log.warn("TwoTierCache[{}] L2 corrupt data for key={}, json={}", name, key, json);
            redis.delete(entryKey(k));
        }
        return value;
    }

    @Override
    public V get(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);

        V value = l1Cache.getIfPresent(key);
        if (value != null) {
            log.debug("TwoTierCache[{}] L1 hit for key={}, value={}", name, key, valueToLogString(value));
            return value;
        }

        String json = redis.opsForValue().get(entryKey(key));
        if (json != null) {
            value = jsonToValue(json);
            if (value != null) {
                l1Cache.put(key, value);
                log.debug("TwoTierCache[{}] L2 hit for key={}, value={}", name, key, valueToLogString(value));
                return value;
            }
            log.warn("TwoTierCache[{}] L2 corrupt data for key={}", name, key);
            redis.delete(entryKey(key));
        }

        value = mappingFunction.apply(key);
        if (value != null) {
            redis.opsForValue().set(entryKey(key), valueToJson(value), l2TtlSec, TimeUnit.SECONDS);
            redis.opsForSet().add(KEY_SET_KEY + name, keyToString(key));
            l1Cache.put(key, value);
        }
        return value;
    }

    @Override
    public void put(K key, V value) {
        log.debug("TwoTierCache[{}] put key={}, value={}", name, key, valueToLogString(value));
        redis.opsForValue().set(entryKey(key), valueToJson(value), l2TtlSec, TimeUnit.SECONDS);
        redis.opsForSet().add(KEY_SET_KEY + name, keyToString(key));
        l1Cache.put(key, value);
        publishInvalidation(key);
    }

    @Override
    public void invalidate(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        String ek = entryKey(k);
        redis.delete(ek);
        redis.opsForSet().remove(KEY_SET_KEY + name, keyToString(k));
        l1Cache.invalidate(key);
        publishInvalidation(k);
    }

    @Override
    public void invalidateAll() {
        Set<String> keys = redis.opsForSet().members(KEY_SET_KEY + name);
        if (keys != null) {
            for (String key : keys) {
                redis.delete(ENTRY_PREFIX + name + ":" + key);
            }
        }
        redis.delete(KEY_SET_KEY + name);
        l1Cache.invalidateAll();
        publishInvalidateAll();
    }

    @Override
    public void invalidateAll(Iterable<?> keys) {
        for (Object key : keys) {
            invalidate(key);
        }
    }

    @Override
    public long estimatedSize() {
        return l1Cache.estimatedSize();
    }

    @Override
    public ConcurrentMap<K, V> asMap() {
        return new TwoTierCacheMapView();
    }

    @Override
    public void cleanUp() {
        l1Cache.cleanUp();
    }

    @Override
    public Map<K, V> getAllPresent(Iterable<?> keys) {
        Map<K, V> result = new ConcurrentHashMap<>();
        for (Object key : keys) {
            V value = getIfPresent(key);
            if (value != null) result.put((K) key, value);
        }
        return result;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public CacheStats stats() {
        return l1Cache.stats();
    }

    @Override
    public Policy<K, V> policy() {
        return null;
    }

    private void publishInvalidation(K key) {
        try {
            invalidationTopic.publish(instanceId + ":" + keyToString(key));
        } catch (Exception e) {
            log.warn("TwoTierCache[{}] failed to publish invalidation", name, e);
        }
    }

    private void publishInvalidateAll() {
        try {
            invalidationTopic.publish("*");
        } catch (Exception e) {
            log.warn("TwoTierCache[{}] failed to publish invalidateAll", name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private class TwoTierCacheMapView implements ConcurrentMap<K, V> {

        private final ConcurrentMap<K, V> delegate = l1Cache.asMap();

        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean containsKey(Object key) { return delegate.containsKey(key); }
        @Override public boolean containsValue(Object value) { return delegate.containsValue(value); }
        @Override public V get(Object key) { return delegate.get(key); }

        @Override
        public V put(K key, V value) {
            TwoTierCache.this.put(key, value);
            return value;
        }

        @Override
        public V remove(Object key) {
            V old = delegate.get(key);
            TwoTierCache.this.invalidate(key);
            return old;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void clear() { TwoTierCache.this.invalidateAll(); }

        @Override
        public Set<K> keySet() { return new TwoTierKeySetView(delegate.keySet()); }
        @Override public Collection<V> values() { return delegate.values(); }
        @Override public Set<Entry<K, V>> entrySet() { return delegate.entrySet(); }

        @Override
        public V putIfAbsent(K key, V value) {
            V existing = delegate.putIfAbsent(key, value);
            if (existing == null) TwoTierCache.this.put(key, value);
            return existing;
        }

        @Override
        public boolean remove(Object key, Object value) {
            boolean removed = delegate.remove(key, value);
            if (removed) TwoTierCache.this.invalidate(key);
            return removed;
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            boolean replaced = delegate.replace(key, oldValue, newValue);
            if (replaced) TwoTierCache.this.put(key, newValue);
            return replaced;
        }

        @Override
        public V replace(K key, V value) {
            V old = delegate.replace(key, value);
            if (old != null) TwoTierCache.this.put(key, value);
            return old;
        }
    }

    private class TwoTierKeySetView implements Set<K> {

        private final Set<K> delegate;

        TwoTierKeySetView(Set<K> delegate) { this.delegate = delegate; }

        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean contains(Object o) { return delegate.contains(o); }
        @Override public Object[] toArray() { return delegate.toArray(); }
        @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
        @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }

        @Override
        public Iterator<K> iterator() {
            Iterator<K> it = delegate.iterator();
            return new Iterator<K>() {
                private K current;
                @Override public boolean hasNext() { return it.hasNext(); }
                @Override public K next() { current = it.next(); return current; }
                @Override
                public void remove() {
                    it.remove();
                    if (current != null) TwoTierCache.this.invalidate(current);
                }
            };
        }

        @Override public boolean add(K k) { return delegate.add(k); }

        @Override
        public boolean remove(Object o) {
            boolean r = delegate.remove(o);
            if (r) TwoTierCache.this.invalidate(o);
            return r;
        }

        @Override public boolean addAll(Collection<? extends K> c) { return delegate.addAll(c); }

        @Override
        public boolean retainAll(Collection<?> c) {
            Set<K> toRemove = new java.util.HashSet<>(delegate);
            toRemove.removeAll(c);
            for (K key : toRemove) TwoTierCache.this.invalidate(key);
            return delegate.retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            for (Object key : c) TwoTierCache.this.invalidate(key);
            return delegate.removeAll(c);
        }

        @Override
        public void clear() { delegate.clear(); }
    }
}
