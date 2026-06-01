<script setup>
import {onMounted, onUnmounted} from 'vue';

const props = defineProps({
  visible: Boolean,
  title: {type: String, default: ''},
  width: {type: String, default: '480px'},
  closeOnClickOverlay: {type: Boolean, default: true},
});

const emit = defineEmits(['update:visible', 'close']);

let overlayPointerDownOnSelf = false;

const doClose = () => {
  emit('update:visible', false);
  emit('close');
};

const onOverlayPointerDown = (event) => {
  overlayPointerDownOnSelf = event.target === event.currentTarget;
};

const resetOverlayPointer = () => {
  overlayPointerDownOnSelf = false;
};

const onOverlayPointerUp = (event) => {
  const shouldClose = props.closeOnClickOverlay
      && overlayPointerDownOnSelf
      && event.target === event.currentTarget;
  resetOverlayPointer();
  if (shouldClose) {
    doClose();
  }
};

const onKeydown = (e) => {
  if (e.key === 'Escape') doClose();
};

onMounted(() => {
  document.addEventListener('keydown', onKeydown);
});

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown);
});
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer-fade">
      <div
        v-if="visible"
        class="a-drawer-overlay"
        @pointerdown="onOverlayPointerDown"
        @pointerup="onOverlayPointerUp"
        @pointercancel="resetOverlayPointer"
      >
        <Transition name="drawer-slide" appear>
          <div v-if="visible" class="a-drawer" :style="{width}">
            <div class="a-drawer-header">
              <h3 class="a-drawer-title">{{ title }}</h3>
              <button type="button" class="a-drawer-close" @click="doClose" aria-label="关闭">&times;</button>
            </div>
            <div class="a-drawer-body">
              <slot />
            </div>
            <div v-if="$slots.footer" class="a-drawer-footer">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped src="@/styles/components/ADrawer.css"></style>
