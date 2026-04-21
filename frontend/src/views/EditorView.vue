<script setup>
import { computed, inject, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createArticleApi, getEditableArticleApi, updateArticleApi } from '@/api/articles';
import { getCategoriesApi, getTagsApi } from '@/api/admin';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import RichMarkdownEditor from '@/components/RichMarkdownEditor.vue';
import { topics } from '@/data/home';

const DRAFT_KEY = 'my-blog-editor-draft';

const defaultDraft = {
    title: '',
    summary: '',
    content: `# 开始写作

用一篇文章把你最近解决的问题写清楚。`,
    category: 'Spring Boot',
    tags: 'Spring Boot, JWT',
    coverUrl: 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=700&q=80'
};

const coverOptions = [
    defaultDraft.coverUrl,
    'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=700&q=80',
    'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=700&q=80'
];

const route = useRoute();
const router = useRouter();
const loginModal = inject('loginModal', { requireLogin: () => false });
const isEditMode = computed(() => route.name === 'editorEdit');
const draft = reactive(loadDraft());
const statusMessage = ref('');
const validationErrors = ref([]);
const publishLoading = ref(false);
const publishedArticle = ref(null);
const feedbackType = ref('info');
const categoryOptions = ref(topics.slice(1));
const tagOptions = ref([]);
const pageLoading = ref(false);
const lastSavedSnapshot = ref('');

const wordCount = computed(() => draft.content.trim().length);
const hasUnsavedChanges = computed(() => createDraftSnapshot(draft) !== lastSavedSnapshot.value);

function loadDraft() {
    try {
        const raw = localStorage.getItem(DRAFT_KEY);
        return raw ? { ...defaultDraft, ...JSON.parse(raw) } : { ...defaultDraft };
    } catch (error) {
        return { ...defaultDraft };
    }
}

function createDraftSnapshot(currentDraft) {
    return JSON.stringify({
        title: currentDraft.title || '',
        summary: currentDraft.summary || '',
        content: currentDraft.content || '',
        category: currentDraft.category || '',
        tags: currentDraft.tags || '',
        coverUrl: currentDraft.coverUrl || ''
    });
}

function syncSavedSnapshot() {
    lastSavedSnapshot.value = createDraftSnapshot(draft);
}

const validateDraft = () => {
    const errors = [];
    if (!draft.title.trim()) {
        errors.push('标题不能为空');
    }
    if (!draft.summary.trim()) {
        errors.push('摘要不能为空');
    }
    if (!draft.content.trim()) {
        errors.push('正文不能为空');
    }
    if (!draft.category) {
        errors.push('请选择分类');
    }
    validationErrors.value = errors;
    return errors.length === 0;
};

const syncDraft = (article) => {
    draft.title = article.title || '';
    draft.summary = article.summary || '';
    draft.content = article.rawContent || '';
    draft.category = article.category || categoryOptions.value[0] || '';
    draft.tags = Array.isArray(article.tags) ? article.tags.join(', ') : '';
    draft.coverUrl = article.coverUrl || defaultDraft.coverUrl;
};

const fetchMetadata = async () => {
    try {
        const [categories, tags] = await Promise.all([
            getCategoriesApi(true),
            getTagsApi(true)
        ]);
        if (categories?.length) {
            categoryOptions.value = categories.map((item) => item.name);
            if (!draft.category) {
                draft.category = categoryOptions.value[0];
            }
        }
        if (tags?.length) {
            tagOptions.value = tags.map((item) => item.name);
        }
    } catch (error) {
        categoryOptions.value = topics.slice(1);
        tagOptions.value = [];
    }
};

const fetchArticle = async () => {
    if (!isEditMode.value) {
        syncSavedSnapshot();
        return;
    }
    pageLoading.value = true;
    try {
        const article = await getEditableArticleApi(route.params.id);
        syncDraft(article);
        localStorage.removeItem(DRAFT_KEY);
        syncSavedSnapshot();
    } catch (error) {
        statusMessage.value = error.message || '文章加载失败';
        feedbackType.value = 'error';
    } finally {
        pageLoading.value = false;
    }
};

const persistArticle = async (status) => {
    if (publishLoading.value) {
        return null;
    }
    const actionText = status === 'PUBLISHED' ? '发布' : '保存草稿';
    const canContinue = loginModal.requireLogin(() => persistArticle(status), {
        title: `登录后${actionText}`,
        message: `登录后可以${actionText}文章，并在个人中心管理内容。`,
        actionText: `登录并${actionText}`
    });
    if (!canContinue) {
        statusMessage.value = `请先登录后再${actionText}`;
        feedbackType.value = 'warning';
        return null;
    }
    if (!validateDraft()) {
        statusMessage.value = `${actionText}前请补齐必要信息`;
        feedbackType.value = 'warning';
        return null;
    }

    publishLoading.value = true;
    statusMessage.value = status === 'PUBLISHED' ? '正在发布文章，请稍候...' : '正在保存草稿，请稍候...';
    feedbackType.value = 'info';
    publishedArticle.value = null;

    try {
        const article = isEditMode.value
            ? await updateArticleApi(route.params.id, draft, status)
            : await createArticleApi(draft, status);
        syncDraft(article);
        if (!isEditMode.value) {
            await router.replace(`/editor/${article.id}`);
        }
        localStorage.removeItem(DRAFT_KEY);
        if (status === 'PUBLISHED') {
            publishedArticle.value = article;
            statusMessage.value = `发布成功，文章 ID：${article.id}`;
            feedbackType.value = 'success';
        } else {
            statusMessage.value = `草稿已保存，文章 ID：${article.id}`;
            feedbackType.value = 'success';
        }
        syncSavedSnapshot();
        validationErrors.value = [];
        return article;
    } catch (error) {
        localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
        statusMessage.value = error.message || `${actionText}失败`;
        feedbackType.value = 'error';
        return null;
    } finally {
        publishLoading.value = false;
    }
};

const saveDraft = async () => {
    await persistArticle('DRAFT');
};

const publishArticle = async () => {
    await persistArticle('PUBLISHED');
};

const changeCover = () => {
    const currentIndex = coverOptions.indexOf(draft.coverUrl);
    const nextIndex = currentIndex >= 0 ? (currentIndex + 1) % coverOptions.length : 0;
    draft.coverUrl = coverOptions[nextIndex];
    statusMessage.value = '已更换封面预览';
    feedbackType.value = 'info';
};

const viewPublishedArticle = () => {
    if (publishedArticle.value?.id) {
        router.push(`/articles/${publishedArticle.value.id}`);
    }
};

const continueWriting = () => {
    publishedArticle.value = null;
    statusMessage.value = '可以继续编辑当前内容';
    feedbackType.value = 'info';
};

watch(
    () => route.params.id,
    () => {
        if (isEditMode.value) {
            fetchArticle();
            return;
        }
        Object.assign(draft, loadDraft());
        syncSavedSnapshot();
    }
);

onMounted(async () => {
    await fetchMetadata();
    await fetchArticle();
    if (!isEditMode.value) {
        syncSavedSnapshot();
    }
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell editor-layout">
        <section class="editor-main">
            <div class="section-heading">
                <div>
                    <p class="eyebrow">创作中心</p>
                    <h1>{{ isEditMode ? '编辑文章' : '发布文章' }}</h1>
                </div>
                <div class="editor-actions">
                    <button type="button" :disabled="publishLoading || pageLoading" @click="saveDraft">保存草稿</button>
                    <button
                        class="primary-action"
                        type="button"
                        :disabled="publishLoading || pageLoading"
                        @click="publishArticle"
                    >
                        {{ publishLoading ? '提交中...' : '发布文章' }}
                    </button>
                </div>
            </div>

            <p v-if="pageLoading" class="loading-text">正在加载文章内容...</p>

            <section
                v-if="publishedArticle"
                class="publish-result"
                aria-live="polite"
            >
                <div>
                    <p class="eyebrow">发布成功</p>
                    <h2>{{ publishedArticle.title }}</h2>
                    <p>文章已经发布到站点，读者现在可以在文章详情页查看。</p>
                    <div class="publish-meta">
                        <span>ID {{ publishedArticle.id }}</span>
                        <span>{{ publishedArticle.category }}</span>
                        <span>{{ publishedArticle.publishedText }}</span>
                    </div>
                </div>
                <div class="publish-result-actions">
                    <button class="primary-action" type="button" @click="viewPublishedArticle">查看文章</button>
                    <button type="button" @click="continueWriting">继续编辑</button>
                </div>
            </section>

            <label class="editor-title">
                <span class="sr-only">文章标题</span>
                <input v-model="draft.title" type="text" placeholder="请输入文章标题">
            </label>

            <label class="editor-summary">
                <span>文章摘要</span>
                <textarea v-model="draft.summary" placeholder="用一两句话介绍这篇文章"></textarea>
            </label>

            <RichMarkdownEditor v-model="draft.content" />
            <div class="editor-feedback">
                <span>正文 {{ wordCount }} 字</span>
                <span :class="['editor-dirty', hasUnsavedChanges ? 'warning' : 'saved']">
                    {{ hasUnsavedChanges ? '未保存改动' : '内容已同步到当前编辑态' }}
                </span>
                <span v-if="statusMessage" :class="['editor-status', feedbackType]">
                    {{ statusMessage }}
                </span>
            </div>
            <ul v-if="validationErrors.length" class="error-list">
                <li v-for="error in validationErrors" :key="error">{{ error }}</li>
            </ul>
        </section>

        <aside class="editor-side">
            <ArticleToc
                :content="draft.content"
                target-selector=".rich-markdown-editor .ProseMirror"
            />
            <section class="side-section">
                <p class="eyebrow">分类</p>
                <select v-model="draft.category">
                    <option v-for="topic in categoryOptions" :key="topic" :value="topic">{{ topic }}</option>
                </select>
            </section>
            <section class="side-section">
                <p class="eyebrow">标签</p>
                <input v-model="draft.tags" type="text" :placeholder="tagOptions.length ? `例如：${tagOptions.slice(0, 3).join(', ')}` : '多个标签用英文逗号分隔'">
            </section>
            <section class="side-section upload-box">
                <p class="eyebrow">封面图</p>
                <img :src="draft.coverUrl" alt="文章封面预览">
                <button type="button" @click="changeCover">更换封面</button>
            </section>
        </aside>
    </main>
</template>
