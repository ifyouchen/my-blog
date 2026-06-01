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

<style scoped src="@/styles/components/EmojiPicker.css"></style>
