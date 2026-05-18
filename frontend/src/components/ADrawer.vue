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

<style scoped>
.a-drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
  overflow: hidden;
  overscroll-behavior: contain;
}

.a-drawer {
  background: #fff;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  overscroll-behavior: contain;
}

.a-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.a-drawer-title {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.a-drawer-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #f3f4f6;
  border-radius: 6px;
  font-size: 20px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}

.a-drawer-close:hover {
  background: #e5e7eb;
  color: #111827;
}

.a-drawer-body {
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: 20px;
}

.a-drawer-footer {
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex-shrink: 0;
}

/* Transition */
.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.2s;
}
.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-slide-enter-active {
  transition: transform 0.25s ease-out;
}
.drawer-slide-leave-active {
  transition: transform 0.2s ease-in;
}
.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(100%);
}
</style>
