<script setup>
import { EMOJI_LIST } from '@/utils/emojis';

const emit = defineEmits(['select']);

const handleSelect = (emoji) => {
  emit('select', emoji.char);
};
</script>

<template>
  <div class="emoji-picker">
    <div class="emoji-picker-header">表情</div>
    <div class="emoji-grid">
      <button
        v-for="emoji in EMOJI_LIST"
        :key="emoji.char"
        class="emoji-item"
        :title="emoji.name"
        @click="handleSelect(emoji)"
        type="button"
      >
        {{ emoji.char }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.emoji-picker {
  width: 320px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.emoji-picker-header {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  border-bottom: 1px solid var(--line);
  background: var(--surface-soft);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 0;
  padding: 6px;
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
