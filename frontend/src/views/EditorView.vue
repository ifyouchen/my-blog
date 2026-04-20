<script setup>
import { computed, inject, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { createArticleApi } from '@/api/articles';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import RichMarkdownEditor from '@/components/RichMarkdownEditor.vue';
import { topics } from '@/data/home';

const DRAFT_KEY = 'my-blog-editor-draft';

const defaultDraft = {
    title: 'Spring Boot 登录鉴权从 0 到 1',
    summary: '把登录态、角色权限、接口拦截、统一错误码串成完整闭环。',
    content: `# 登录鉴权设计

第一版博客系统采用 JWT 认证。

## 核心流程

- 登录成功后签发 Token
- 前端请求时携带 Authorization
- 后端统一解析身份和角色
- 资源归属由应用服务校验

\`\`\`java
@Transactional(rollbackFor = Exception.class)
public ArticleId publish(PublishArticleCommand command) {
    return articleAppService.publish(command);
}
\`\`\``,
    category: 'Spring Boot',
    tags: 'Spring Boot, JWT, 权限设计',
    coverUrl: 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=700&q=80'
};

const coverOptions = [
    defaultDraft.coverUrl,
    'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=700&q=80',
    'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=700&q=80'
];

const loadDraft = () => {
    try {
        const raw = localStorage.getItem(DRAFT_KEY);
        return raw ? { ...defaultDraft, ...JSON.parse(raw) } : defaultDraft;
    } catch (error) {
        return defaultDraft;
    }
};

const draft = reactive(loadDraft());
const statusMessage = ref('');
const validationErrors = ref([]);
const publishLoading = ref(false);
const publishedArticle = ref(null);
const feedbackType = ref('info');
const router = useRouter();
const loginModal = inject('loginModal', { requireLogin: () => false });

const wordCount = computed(() => draft.content.trim().length);

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

const saveDraft = () => {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
    statusMessage.value = `草稿已保存，当前正文 ${wordCount.value} 字`;
    feedbackType.value = 'success';
    publishedArticle.value = null;
    validationErrors.value = [];
};

const publishArticle = async () => {
    if (publishLoading.value) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => publishArticle(), {
        title: '登录后发布文章',
        message: '登录后可以发布文章、保存到创作中心，并在个人主页展示。',
        actionText: '登录并继续发布'
    });
    if (!canContinue) {
        statusMessage.value = '请先登录后再发布文章';
        feedbackType.value = 'warning';
        return;
    }
    if (!validateDraft()) {
        statusMessage.value = '发布前请补齐必要信息';
        feedbackType.value = 'warning';
        return;
    }
    publishLoading.value = true;
    publishedArticle.value = null;
    statusMessage.value = '正在发布文章，请稍候...';
    feedbackType.value = 'info';
    try {
        const article = await createArticleApi(draft, 'PUBLISHED');
        localStorage.removeItem(DRAFT_KEY);
        publishedArticle.value = article;
        statusMessage.value = `发布成功，文章 ID：${article.id}`;
        feedbackType.value = 'success';
    } catch (error) {
        localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
        statusMessage.value = error.message || '后端未启动，已保留本地草稿';
        feedbackType.value = 'error';
    } finally {
        publishLoading.value = false;
    }
};

const changeCover = () => {
    const currentIndex = coverOptions.indexOf(draft.coverUrl);
    const nextIndex = currentIndex >= 0 ? (currentIndex + 1) % coverOptions.length : 0;
    draft.coverUrl = coverOptions[nextIndex];
    statusMessage.value = '已更换封面预览';
    feedbackType.value = 'info';
    publishedArticle.value = null;
};

const viewPublishedArticle = () => {
    if (publishedArticle.value?.id) {
        router.push(`/articles/${publishedArticle.value.id}`);
    }
};

const continueWriting = () => {
    publishedArticle.value = null;
    statusMessage.value = '可以继续编辑当前内容，或保存为新的草稿';
    feedbackType.value = 'info';
};
</script>

<template>
    <SiteHeader />
    <main class="page-shell editor-layout">
        <section class="editor-main">
            <div class="section-heading">
                <div>
                    <p class="eyebrow">创作中心</p>
                    <h1>发布文章</h1>
                </div>
                <div class="editor-actions">
                    <button type="button" :disabled="publishLoading" @click="saveDraft">保存草稿</button>
                    <button
                        class="primary-action"
                        type="button"
                        :disabled="publishLoading"
                        @click="publishArticle"
                    >
                        {{ publishLoading ? '发布中...' : '发布文章' }}
                    </button>
                </div>
            </div>

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
                    <option v-for="topic in topics.slice(1)" :key="topic" :value="topic">{{ topic }}</option>
                </select>
            </section>
            <section class="side-section">
                <p class="eyebrow">标签</p>
                <input v-model="draft.tags" type="text">
            </section>
            <section class="side-section upload-box">
                <p class="eyebrow">封面图</p>
                <img :src="draft.coverUrl" alt="文章封面预览">
                <button type="button" @click="changeCover">更换封面</button>
            </section>
        </aside>
    </main>
</template>
