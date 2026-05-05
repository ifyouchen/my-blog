<script setup>
import { computed, ref } from 'vue';
import { EMOJI_GROUPS } from '@/utils/emojis';

const emit = defineEmits(['select']);
const activeGroupId = ref(EMOJI_GROUPS[0]?.id || '');

const activeGroup = computed(() => (
  EMOJI_GROUPS.find((group) => group.id === activeGroupId.value) || EMOJI_GROUPS[0]
));

const handleSelect = (emoji) => {
  emit('select', emoji.char);
};
</script>

<template>
  <div class="emoji-picker">
    <div class="emoji-grid">
      <button
        v-for="emoji in activeGroup.items"
        :key="`${emoji.name}-${emoji.char}`"
        class="emoji-item"
        :title="emoji.name"
        @click="handleSelect(emoji)"
        type="button"
      >
        {{ emoji.char }}
      </button>
    </div>
    <div class="emoji-picker-header">
      <button
        v-for="group in EMOJI_GROUPS"
        :key="group.id"
        type="button"
        class="emoji-tab"
        :class="{ active: activeGroupId === group.id }"
        :title="group.label"
        @click="activeGroupId = group.id"
      >
        <span>{{ group.icon }}</span>
        <small>{{ group.label }}</small>
      </button>
    </div>
  </div>
</template>

<style scoped>
.emoji-picker {
  width: 352px;
  max-width: calc(100vw - 32px);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.emoji-picker-header {
  display: flex;
  gap: 2px;
  padding: 6px;
  overflow-x: auto;
  border-top: 1px solid var(--line);
  background: var(--surface-soft);
  scrollbar-width: thin;
}

.emoji-tab {
  display: grid;
  min-width: 46px;
  height: 44px;
  padding: 4px 6px;
  place-items: center;
  color: var(--muted);
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}

.emoji-tab span {
  font-size: 17px;
  line-height: 1;
}

.emoji-tab small {
  font-size: 11px;
  line-height: 1.2;
  white-space: nowrap;
}

.emoji-tab:hover,
.emoji-tab.active {
  color: var(--text);
  background: var(--surface);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 0;
  max-height: min(336px, calc(100vh - 180px));
  overflow-y: auto;
  padding: 6px;
  scrollbar-width: thin;
}

.emoji-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 38px;
  padding: 0;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  transition: background-color 0.1s, transform 0.1s;
}

.emoji-item:hover {
  background: var(--surface-soft);
  transform: scale(1.15);
}

.emoji-item:active {
  transform: scale(0.95);
}
</style>
