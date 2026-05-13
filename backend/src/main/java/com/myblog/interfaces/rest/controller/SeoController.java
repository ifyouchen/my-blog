package com.myblog.interfaces.rest.controller;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.aggregate.Topic;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.TopicRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SEO 相关接口控制器。
 * <p>
 * 提供 {@code robots.txt}、{@code sitemap.xml} 和 {@code rss.xml} 接口，
 * 帮助搜索引擎抓取内容。
 * sitemap 和 RSS 内容有本地时间缓存（TTL 30 分钟），过期后自动重新生成。
 * </p>
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
public class SeoController {

    /** Sitemap 时间格式（ISO 8601，+08:00 时区）。 */
    private static final DateTimeFormatter SITEMAP_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'+08:00'");
    /** RSS pubDate 时间格式（RFC 822）。 */
    private static final DateTimeFormatter RSS_DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z");
    /** 缓存有效期（30 分钟）。 */
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;
    /** RSS Feed 最多返回的文章条数。 */
    private static final int RSS_LIMIT = 20;

    private final ArticleRepository articleRepository;
    private final ColumnRepository columnRepository;
    private final TopicRepository topicRepository;

    /** 本地缓存，key 为 "sitemap" 或 "rss"。 */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public SeoController(ArticleRepository articleRepository,
                         ColumnRepository columnRepository,
                         TopicRepository topicRepository) {
        this.articleRepository = articleRepository;
        this.columnRepository = columnRepository;
        this.topicRepository = topicRepository;
    }

    /**
     * 返回 robots.txt 内容。
     *
     * @return robots.txt 纯文本内容
     */
    @GetMapping(value = "/robots.txt", produces = "text/plain; charset=utf-8")
    public String robotsTxt() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("User-agent: *\n");
        sb.append("Allow: /\n");
        sb.append("Disallow: /api/admin/\n");
        sb.append("Disallow: /api/dashboard/\n");
        sb.append("Disallow: /api/settings/\n");
        sb.append("Disallow: /api/notifications/\n");
        sb.append("Disallow: /api/uploads/\n");
        sb.append("\n");
        sb.append("Sitemap: https://myblog.example.com/sitemap.xml\n");
        return sb.toString();
    }

    /**
     * 返回站点地图 XML。有缓存时直接返回缓存内容。
     *
     * @return sitemap.xml 内容
     */
    @GetMapping(value = "/sitemap.xml", produces = "application/xml; charset=utf-8")
    public String sitemapXml() {
        String cached = getCached("sitemap");
        if (cached != null) {
            return cached;
        }
        String xml = buildSitemap();
        cache.put("sitemap", new CacheEntry(xml, System.currentTimeMillis()));
        return xml;
    }

    /**
     * 返回 RSS Feed XML。有缓存时直接返回缓存内容。
     *
     * @return rss.xml 内容
     */
    @GetMapping(value = "/rss.xml", produces = "application/xml; charset=utf-8")
    public String rssXml() {
        String cached = getCached("rss");
        if (cached != null) {
            return cached;
        }
        String xml = buildRss();
        cache.put("rss", new CacheEntry(xml, System.currentTimeMillis()));
        return xml;
    }

    /**
     * 构建完整的 Sitemap XML 字符串。
     *
     * @return XML 字符串
     */
    private String buildSitemap() {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        List<Article> articles = articleRepository.findPublishedWithLimit(null, null, null, null, Integer.MAX_VALUE, 0);
        for (Article article : articles) {
            String slug = article.getSlug();
            String url = slug != null && !slug.isEmpty()
                ? "/articles/" + article.getId().getValue() + "-" + slug
                : "/articles/" + article.getId().getValue();
            appendUrl(xml, url, article.getUpdatedAt());
        }

        List<Column> columns = columnRepository.findPublished(1, Integer.MAX_VALUE);
        for (Column column : columns) {
            appendUrl(xml, "/columns/" + column.getId().getValue(), column.getUpdatedAt());
        }

        List<Topic> topics = topicRepository.findPublished(1, Integer.MAX_VALUE);
        for (Topic topic : topics) {
            appendUrl(xml, "/topics/" + topic.getId().getValue(), topic.getUpdatedAt());
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    /**
     * 向 Sitemap 追加一个 URL 条目。
     *
     * @param xml     构建中的 XML
     * @param url     页面路径
     * @param lastMod 最后修改时间
     */
    private void appendUrl(StringBuilder xml, String url, LocalDateTime lastMod) {
        xml.append("  <url>\n");
        xml.append("    <loc>https://myblog.example.com").append(url).append("</loc>\n");
        if (lastMod != null) {
            xml.append("    <lastmod>").append(lastMod.format(SITEMAP_DATE_FMT)).append("</lastmod>\n");
        }
        xml.append("  </url>\n");
    }

    /**
     * 构建完整的 RSS Feed XML 字符串。
     *
     * @return XML 字符串
     */
    private String buildRss() {
        List<Article> articles = articleRepository.findPublishedWithLimit(null, null, null, "latest", RSS_LIMIT, 0);

        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>my-blog</title>\n");
        xml.append("    <link>https://myblog.example.com</link>\n");
        xml.append("    <description>一个面向技术创作者和读者的博客社区平台</description>\n");
        xml.append("    <language>zh-CN</language>\n");
        xml.append("    <atom:link href=\"https://myblog.example.com/rss.xml\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        for (Article article : articles) {
            String slug = article.getSlug();
            String url = slug != null && !slug.isEmpty()
                ? "/articles/" + article.getId().getValue() + "-" + slug
                : "/articles/" + article.getId().getValue();

            xml.append("    <item>\n");
            xml.append("      <title>").append(escapeXml(article.getTitle())).append("</title>\n");
            xml.append("      <link>https://myblog.example.com").append(url).append("</link>\n");
            xml.append("      <description>").append(escapeXml(article.getSummary())).append("</description>\n");
            if (article.getPublishedAt() != null) {
                xml.append("      <pubDate>").append(article.getPublishedAt().format(RSS_DATE_FMT)).append("</pubDate>\n");
            }
            xml.append("    </item>\n");
        }

        xml.append("  </channel>\n");
        xml.append("</rss>\n");
        return xml.toString();
    }

    /**
     * 获取未过期的缓存内容。
     *
     * @param key 缓存键
     * @return 缓存内容，缓存缺失或已过期则返回 null
     */
    private String getCached(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && System.currentTimeMillis() - entry.timestamp < CACHE_TTL_MS) {
            return entry.content;
        }
        return null;
    }

    /**
     * 转义 XML 特殊字符。
     *
     * @param value 原始字符串
     * @return 转义后的字符串
     */
    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    /**
     * 缓存条目，包含内容和生成时间戳。
     */
    private static class CacheEntry {
        final String content;
        final long timestamp;

        CacheEntry(String content, long timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}
