<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';

const props = defineProps({
    modelValue: {
        type: [String, Number, Boolean],
        default: ''
    },
    options: {
        type: Array,
        default: () => []
    },
    placeholder: {
        type: String,
        default: '请选择'
    },
    disabled: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['update:modelValue', 'change']);

const rootRef = ref(null);
const open = ref(false);
const highlightedIndex = ref(-1);

const selectedIndex = computed(() =>
    props.options.findIndex(option => option.value === props.modelValue)
);

const selectedOption = computed(() =>
    selectedIndex.value >= 0 ? props.options[selectedIndex.value] : null
);

const displayLabel = computed(() => selectedOption.value?.label || props.placeholder);

const close = () => {
    open.value = false;
};

const toggle = () => {
    if (props.disabled) {
        return;
    }
    open.value = !open.value;
};

const selectOption = (option) => {
    if (props.disabled || option?.disabled) {
        return;
    }
    const changed = option.value !== props.modelValue;
    emit('update:modelValue', option.value);
    if (changed) {
        emit('change', option.value, option);
    }
    close();
};

const moveHighlight = (delta) => {
    const enabledOptions = props.options
        .map((option, index) => ({option, index}))
        .filter(item => !item.option.disabled);
    if (!enabledOptions.length) {
        return;
    }
    const current = enabledOptions.findIndex(item => item.index === highlightedIndex.value);
    const next = current < 0
        ? (delta > 0 ? 0 : enabledOptions.length - 1)
        : (current + delta + enabledOptions.length) % enabledOptions.length;
    highlightedIndex.value = enabledOptions[next].index;
};

const handleKeydown = (event) => {
    if (props.disabled) {
        return;
    }
    if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        if (open.value && highlightedIndex.value >= 0) {
            selectOption(props.options[highlightedIndex.value]);
        } else {
            toggle();
        }
        return;
    }
    if (event.key === 'ArrowDown') {
        event.preventDefault();
        open.value = true;
        moveHighlight(1);
        return;
    }
    if (event.key === 'ArrowUp') {
        event.preventDefault();
        open.value = true;
        moveHighlight(-1);
        return;
    }
    if (event.key === 'Escape') {
        close();
    }
};

const handlePointerDown = (event) => {
    if (!rootRef.value?.contains(event.target)) {
        close();
    }
};

watch(open, (visible) => {
    if (visible) {
        highlightedIndex.value = selectedIndex.value >= 0 ? selectedIndex.value : 0;
    }
});

watch(() => props.modelValue, () => {
    highlightedIndex.value = selectedIndex.value;
});

onMounted(() => {
    document.addEventListener('pointerdown', handlePointerDown);
});

onBeforeUnmount(() => {
    document.removeEventListener('pointerdown', handlePointerDown);
});
</script>

<template>
    <div ref="rootRef" :class="['admin-select', {open, disabled}]">
        <button
            type="button"
            class="admin-select-trigger"
            :disabled="disabled"
            role="combobox"
            :aria-expanded="open"
            aria-haspopup="listbox"
            @click="toggle"
            @keydown="handleKeydown"
        >
            <span :class="['admin-select-label', {placeholder: !selectedOption}]">{{ displayLabel }}</span>
            <span class="admin-select-arrow" aria-hidden="true"></span>
        </button>
        <Transition name="admin-select-pop">
            <div v-if="open" class="admin-select-menu" role="listbox">
                <button
                    v-for="(option, index) in options"
                    :key="`${option.value}-${option.label}`"
                    type="button"
                    :class="[
                        'admin-select-option',
                        {
                            selected: option.value === modelValue,
                            highlighted: index === highlightedIndex,
                            disabled: option.disabled
                        }
                    ]"
                    role="option"
                    :aria-selected="option.value === modelValue"
                    :disabled="option.disabled"
                    @mouseenter="highlightedIndex = index"
                    @click="selectOption(option)"
                >
                    <span>{{ option.label }}</span>
                    <span v-if="option.value === modelValue" class="admin-select-check">✓</span>
                </button>
            </div>
        </Transition>
    </div>
</template>

<style scoped>
.admin-select {
    position: relative;
    width: 100%;
    min-width: 0;
}

.admin-select-trigger {
    display: inline-flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    width: 100%;
    height: 40px;
    padding: 0 12px 0 14px;
    color: #111827;
    background: #fff;
    border: 1px solid #cfd6e3;
    border-radius: 8px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: border-color 0.15s, box-shadow 0.15s, background-color 0.15s;
}

.admin-select-trigger:hover:not(:disabled) {
    border-color: #b9c2d0;
    background: #f8fafc;
}

.admin-select.open .admin-select-trigger,
.admin-select-trigger:focus-visible {
    border-color: #4f46e5;
    box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
    outline: none;
}

.admin-select-trigger:disabled {
    opacity: 0.55;
    cursor: not-allowed;
}

.admin-select-label {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.admin-select-label.placeholder {
    color: #9ca3af;
}

.admin-select-arrow {
    width: 8px;
    height: 8px;
    border-right: 2px solid #475569;
    border-bottom: 2px solid #475569;
    transform: rotate(45deg) translateY(-2px);
    transition: transform 0.15s;
    flex: 0 0 auto;
}

.admin-select.open .admin-select-arrow {
    transform: rotate(225deg) translate(-1px, -1px);
}

.admin-select-menu {
    position: absolute;
    z-index: 80;
    top: calc(100% + 6px);
    left: 0;
    right: 0;
    max-height: 240px;
    padding: 6px;
    overflow-y: auto;
    background: #fff;
    border: 1px solid #dbe2ee;
    border-radius: 10px;
    box-shadow: 0 18px 45px rgba(15, 23, 42, 0.16);
}

.admin-select-option {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    width: 100%;
    min-height: 36px;
    padding: 0 10px;
    color: #334155;
    background: transparent;
    border: 0;
    border-radius: 7px;
    font-size: 13px;
    font-weight: 600;
    text-align: left;
    cursor: pointer;
}

.admin-select-option:hover:not(:disabled),
.admin-select-option.highlighted:not(:disabled) {
    color: #1e1b4b;
    background: #eef2ff;
}

.admin-select-option.selected {
    color: #3730a3;
    background: #e0e7ff;
}

.admin-select-option:disabled {
    color: #9ca3af;
    cursor: not-allowed;
}

.admin-select-check {
    color: #4f46e5;
    font-size: 12px;
    font-weight: 800;
}

.admin-select-pop-enter-active,
.admin-select-pop-leave-active {
    transition: opacity 0.12s ease, transform 0.12s ease;
}

.admin-select-pop-enter-from,
.admin-select-pop-leave-to {
    opacity: 0;
    transform: translateY(-4px);
}
</style>
