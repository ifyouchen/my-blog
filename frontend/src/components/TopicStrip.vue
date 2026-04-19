<script setup>
import { ref } from 'vue';
import { topics } from '@/data/home';

const activeTopic = ref('全部');
</script>

<template>
    <section class="topic-strip" aria-label="技术分类">
        <button
            v-for="topic in topics"
            :key="topic"
            class="topic"
            :class="{ active: activeTopic === topic }"
            type="button"
            @click="activeTopic = topic"
        >
            {{ topic }}
        </button>
    </section>
</template>
