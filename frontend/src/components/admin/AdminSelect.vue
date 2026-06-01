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

<style scoped src="@/styles/components/admin/AdminSelect.css"></style>
