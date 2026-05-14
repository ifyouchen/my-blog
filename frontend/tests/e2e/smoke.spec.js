import { expect, test } from '@playwright/test';
import fs from 'node:fs/promises';
import JSZip from 'jszip';

const USER_ACCOUNT = process.env.E2E_USER_ACCOUNT || 'u92mpnsm4';
const USER_PASSWORD = process.env.E2E_USER_PASSWORD || '123456';
const ADMIN_ACCOUNT = process.env.E2E_ADMIN_ACCOUNT || '';
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD || '';

async function login(page, account, password) {
    await page.goto('/login');
    await expect(page.getByTestId('login-form')).toBeVisible();
    await page.getByTestId('login-account-input').fill(account);
    await page.getByTestId('login-password-input').fill(password);
    await page.getByTestId('login-submit').click();
    await page.waitForURL('**/dashboard/articles');
}

async function mockArticleExportFixture(page, { articleId = 123, withImage = true } = {}) {
    const articleContent = [
        '## Only Body Heading',
        '',
        'Visible body paragraph for export checks.',
        '',
        ...(withImage ? [
            '![Export image](/api/uploads/files/export-test.svg)',
            ''
        ] : []),
        ...Array.from(
            {length: 18},
            (_, index) => `Extra export paragraph ${index + 1} keeps the PDF body long enough to render.`
        ),
        '',
        '```js',
        'console.log("export body only");',
        '```'
    ].join('\n');

    await page.route(`**/api/articles/${articleId}`, async (route) => {
        await route.fulfill({
            contentType: 'application/json',
            body: JSON.stringify({
                code: 0,
                data: {
                    id: articleId,
                    title: 'Export Metadata Title',
                    summary: 'Metadata summary must not be exported.',
                    category: '测试',
                    status: 'PUBLISHED',
                    content: articleContent,
                    coverUrl: '',
                    author: {
                        id: 2,
                        username: 'export-author',
                        nickname: 'Export Author',
                        avatar: ''
                    },
                    tags: ['export'],
                    viewCount: 7,
                    likeCount: 3,
                    favoriteCount: 1,
                    commentCount: 4,
                    publishedAt: '2026-05-14 10:00:00',
                    updatedAt: '2026-05-14 11:00:00',
                    slug: withImage ? 'export-test' : 'export-no-image-test'
                }
            })
        });
    });
    await page.route(`**/api/articles/${articleId}/recommendations?*`, async (route) => {
        await route.fulfill({
            contentType: 'application/json',
            body: JSON.stringify({ code: 0, data: { sections: [] } })
        });
    });
    await page.route(`**/api/articles/${articleId}/neighbors`, async (route) => {
        await route.fulfill({
            contentType: 'application/json',
            body: JSON.stringify({ code: 0, data: { prev: null, next: null } })
        });
    });
    await page.route(`**/api/articles/${articleId}/comments?*`, async (route) => {
        await route.fulfill({
            contentType: 'application/json',
            body: JSON.stringify({ code: 0, data: { items: [], total: 0 } })
        });
    });
    await page.route('**/api/uploads/files/export-test.svg', async (route) => {
        const svg = [
            '<svg xmlns="http://www.w3.org/2000/svg" width="80" height="40">',
            '<rect width="80" height="40" fill="#2563eb"/>',
            '</svg>'
        ].join('');
        await route.fulfill({
            contentType: 'image/svg+xml',
            body: svg
        });
    });
}

test.describe('guest smoke', () => {
    test('home page renders with live content containers', async ({ page }) => {
        const recommendResponsePromise = page.waitForResponse((response) => (
            response.url().includes('/api/articles')
            && response.url().includes('sort=recommend')
            && response.ok()
        )).catch(() => null);
        await page.goto('/');
        await expect(page.getByTestId('home-page')).toBeVisible();
        await expect(page.locator('.main-nav a', { hasText: '首页' })).toBeVisible();
        await expect(page.getByTestId('login-modal')).toHaveCount(0);
        await expect(page.getByTestId('home-channel-bar')).toBeVisible();
        await expect(page.getByTestId('home-portal-hero')).toBeVisible();
        await expect(page.getByTestId('home-focus-article')).toBeVisible();
        await expect(page.locator('[data-feed-root]')).toBeVisible();
        await expect(page.getByTestId('home-sidebar')).toHaveCount(1);

        const focusHref = await page.getByTestId('home-focus-article').getAttribute('href');
        if (focusHref && focusHref !== '/') {
            await page.getByTestId('home-focus-article').click();
            await expect(page).toHaveURL(/\/articles\/\d+/);
        }
        expect(await recommendResponsePromise).not.toBeNull();
    });

    test('home mobile keeps expanded categories after selection', async ({ page }) => {
        await page.setViewportSize({ width: 390, height: 844 });
        await page.goto('/');
        await expect(page.getByTestId('home-page')).toBeVisible();
        const expandButton = page.getByRole('button', { name: '展开更多' });
        if (!await expandButton.isVisible({ timeout: 5000 }).catch(() => false)) {
            return;
        }
        await expandButton.click();
        const topics = page.locator('.topic-list .topic');
        const topicCount = await topics.count();
        if (topicCount <= 8) {
            return;
        }
        const targetTopic = topics.nth(topicCount - 1);
        const topicName = (await targetTopic.textContent())?.trim() || '';
        await targetTopic.click();
        await expect(page.getByRole('button', { name: '收起' })).toBeVisible();
        await expect(page.locator('.topic-list .topic.active', { hasText: topicName })).toBeVisible();
        const overflowState = await page.evaluate(() => ({
            viewportWidth: document.documentElement.clientWidth,
            scrollWidth: document.documentElement.scrollWidth
        }));
        expect(overflowState.scrollWidth).toBeLessThanOrEqual(overflowState.viewportWidth + 1);
    });

    test('following page shows login guide for guests', async ({ page }) => {
        await page.goto('/following');
        await expect(page.getByTestId('following-page')).toBeVisible();
        await expect(page.getByTestId('following-login-empty')).toBeVisible();
        await expect(page.getByTestId('following-login-cta')).toBeVisible();
        await expect(page.getByTestId('following-ranking-cta')).toBeVisible();
    });

    test('columns page renders list and detail entry', async ({ page }) => {
        await page.goto('/columns');
        await expect(page.getByTestId('columns-page')).toBeVisible();
        await expect(page.getByTestId('columns-grid')).toBeVisible();
        const firstColumn = page.getByTestId('column-card').first();
        await expect(firstColumn).toBeVisible();
        await firstColumn.click({ position: { x: 12, y: 12 } });
        await expect(page).toHaveURL(/\/columns\/\d+/);
    });

    test('user profile switches between latest posts and columns', async ({ page }) => {
        await page.goto('/users/2');
        await expect(page.getByTestId('profile-content-tabs')).toBeVisible();
        await expect(page.getByTestId('profile-posts-tab')).toHaveAttribute('aria-selected', 'true');
        await expect(page.getByTestId('profile-article-toolbar')).toBeVisible();

        const columnsResponsePromise = page.waitForResponse((response) => (
            response.url().includes('/api/columns/users/2')
            && response.request().method() === 'GET'
        )).catch(() => null);
        await page.getByTestId('profile-columns-tab').click();
        await expect(page.getByTestId('profile-columns-tab')).toHaveAttribute('aria-selected', 'true');
        await expect(page.getByTestId('profile-article-toolbar')).toHaveCount(0);
        await expect(page.getByTestId('profile-columns-section')).toBeVisible();

        const columnsResponse = await columnsResponsePromise;
        expect(columnsResponse).not.toBeNull();
        expect(columnsResponse.ok()).toBeTruthy();

        await page.getByTestId('profile-posts-tab').click();
        await expect(page.getByTestId('profile-posts-tab')).toHaveAttribute('aria-selected', 'true');
        await expect(page.getByTestId('profile-article-toolbar')).toBeVisible();
    });

    test('ranking page renders article and author boards', async ({ page }) => {
        const categoriesResponsePromise = page.waitForResponse((response) => (
            response.url().includes('/api/categories') && response.ok()
        )).catch(() => null);
        await page.goto('/ranking');
        await expect(page.getByTestId('ranking-page')).toBeVisible();
        await expect(page.getByTestId('ranking-toolbar')).toBeVisible();
        await expect(page.getByRole('button', { name: '7天' })).toBeVisible();
        await expect(page.getByRole('button', { name: '总榜' })).toBeVisible();
        await expect(page.getByText('结果会短暂缓存以保证响应速度')).toBeVisible();
        const categoriesResponse = await categoriesResponsePromise;
        if (categoriesResponse) {
            const payload = await categoriesResponse.json().catch(() => null);
            const categories = Array.isArray(payload?.data) ? payload.data : (payload?.data?.items || []);
            const firstCategory = categories.find((category) => category?.name);
            if (firstCategory) {
                await expect(page.getByRole('button', { name: firstCategory.name, exact: true })).toBeVisible();
            }
        }
        await page.goto('/ranking?period=all');
        await expect(page.getByTestId('ranking-page')).toBeVisible();
        await page.evaluate(() => {
            window.scrollTo(0, 320);
            const periodButton = Array.from(document.querySelectorAll('.ranking-filter-options button'))
                .find((button) => button.textContent.trim() === '30天');
            periodButton?.click();
        });
        await expect(page).toHaveURL(/period=30d/);
        expect(await page.evaluate(() => window.scrollY)).toBeGreaterThan(250);

        const clickedCategory = await page.evaluate(() => {
            window.scrollTo(0, 320);
            const categoryButton = Array.from(document.querySelectorAll(
                '.ranking-filter-options.category .ranking-category-button'
            )).find((button) => button.textContent.trim() !== '全部');
            categoryButton?.click();
            return Boolean(categoryButton);
        });
        if (clickedCategory) {
            await page.waitForFunction(() => new URL(window.location.href).searchParams.has('category'));
            expect(await page.evaluate(() => window.scrollY)).toBeGreaterThan(250);
        }

        const categoryLabels = await page.locator('.ranking-filter-options.category button').allTextContents();
        await page.locator('.main-nav a', { hasText: '专栏' }).click();
        await expect(page).toHaveURL(/\/columns/);
        await page.locator('.main-nav a', { hasText: '排行榜' }).click();
        await expect(page.getByTestId('ranking-page')).toBeVisible();
        await expect(page.locator('.ranking-filter-placeholder')).toHaveCount(0);
        await expect(page.locator('.ranking-filter-options.category button')).toHaveText(categoryLabels);
        await expect(page.getByTestId('ranking-articles')).toBeVisible();
        await expect(page.getByTestId('ranking-authors')).toBeVisible();
    });

    test('ranking mobile switches between article and author boards', async ({ page }) => {
        await page.setViewportSize({ width: 390, height: 844 });
        await page.goto('/ranking');
        await expect(page.getByTestId('ranking-mobile-tabs')).toBeVisible();
        await expect(page.getByTestId('ranking-articles')).toBeVisible();

        const categoryToggle = page.locator('.ranking-category-toggle');
        if (await categoryToggle.isVisible({ timeout: 5000 }).catch(() => false)) {
            await expect(page.locator('.ranking-filter-options.category .ranking-category-button:visible'))
                .toHaveCount(4);
            await categoryToggle.click();
            await expect(categoryToggle).toHaveText('收起');
            expect(await page.locator('.ranking-filter-options.category .ranking-category-button:visible').count())
                .toBeGreaterThan(4);
        }

        await page.getByRole('button', { name: '作者榜' }).click();
        await expect(page.getByTestId('ranking-authors')).toBeVisible();
        await expect(page.getByTestId('ranking-articles')).not.toBeVisible();

        const overflowState = await page.evaluate(() => ({
            viewportWidth: document.documentElement.clientWidth,
            scrollWidth: document.documentElement.scrollWidth,
            authorCards: Array.from(document.querySelectorAll('.ranking-author-card')).map((element) => {
                const rect = element.getBoundingClientRect();
                return {
                    left: rect.left,
                    right: rect.right
                };
            })
        }));
        expect(overflowState.scrollWidth).toBeLessThanOrEqual(overflowState.viewportWidth + 1);
        for (const card of overflowState.authorCards) {
            expect(card.left).toBeGreaterThanOrEqual(-1);
            expect(card.right).toBeLessThanOrEqual(overflowState.viewportWidth + 1);
        }
    });

    test('explore page loads passive recommendations without login modal', async ({ page }) => {
        await page.goto('/explore');
        await expect(page.getByTestId('explore-page')).toBeVisible();
        await expect(page.getByTestId('login-modal')).toHaveCount(0);
    });

    test('article detail is readable for guests without auth modal', async ({ page }) => {
        await page.goto('/articles/206-go-206');
        await expect(page.getByTestId('article-detail-page')).toBeVisible();
        await expect(page.getByTestId('article-detail-main')).toBeVisible();
        await expect(page.getByTestId('comment-panel')).toBeVisible();
        await expect(page.getByTestId('login-modal')).toHaveCount(0);
    });

    test('article guest interactions open login modal only after click', async ({ page }) => {
        await page.goto('/articles/206-go-206');
        await expect(page.getByTestId('article-detail-page')).toBeVisible();
        await expect(page.getByTestId('login-modal')).toHaveCount(0);

        await page.getByRole('button', { name: '点赞' }).first().click();
        await expect(page.getByTestId('login-modal')).toBeVisible();
        await page.getByRole('button', { name: '关闭登录弹窗' }).click();

        await page.getByTestId('comment-composer-input').first().click();
        await expect(page.getByTestId('login-modal')).toBeVisible();
    });

    test('article detail exports only body as markdown html and pdf', async ({ page }) => {
        await mockArticleExportFixture(page);
        await page.addInitScript(() => {
            window.__articlePdfPrintCount = 0;
            window.addEventListener('message', (event) => {
                if (event.data?.type === 'article-export-test-print') {
                    window.__articlePdfPrintCount += 1;
                }
            });
            window.print = () => {
                window.parent?.postMessage({type: 'article-export-test-print'}, '*');
            };
        });
        await page.goto('/articles/123-export-test');
        await expect(page.getByTestId('article-detail-page')).toBeVisible();
        await expect(page.getByTestId('article-export-button')).toBeVisible();

        await page.getByTestId('article-export-button').click();
        await expect(page.getByTestId('article-export-menu')).toBeVisible();
        const markdownDownloadPromise = page.waitForEvent('download');
        await page.getByTestId('article-export-md').click();
        const markdownDownload = await markdownDownloadPromise;
        expect(markdownDownload.suggestedFilename()).toMatch(/\.zip$/);
        const markdownPath = await markdownDownload.path();
        const markdownZip = await JSZip.loadAsync(await fs.readFile(markdownPath));
        const markdownEntry = markdownZip.file('article.md');
        expect(markdownEntry).not.toBeNull();
        expect(markdownZip.file('assets/image-001.svg')).not.toBeNull();
        const markdownText = await markdownEntry.async('string');
        expect(markdownText).toContain('Visible body paragraph for export checks.');
        expect(markdownText).toContain('![Export image](assets/image-001.svg)');
        expect(markdownText).not.toContain('/api/uploads/files/export-test.svg');
        expect(markdownText).not.toContain('Export Metadata Title');
        expect(markdownText).not.toContain('Export Author');
        expect(markdownText).not.toContain('Metadata summary must not be exported.');

        await page.getByTestId('article-export-button').click();
        const htmlDownloadPromise = page.waitForEvent('download');
        await page.getByTestId('article-export-html').click();
        const htmlDownload = await htmlDownloadPromise;
        expect(htmlDownload.suggestedFilename()).toMatch(/\.html$/);
        const htmlPath = await htmlDownload.path();
        const htmlText = await fs.readFile(htmlPath, 'utf8');
        expect(htmlText).toContain('Visible body paragraph for export checks.');
        expect(htmlText).toContain('data:image/svg+xml;base64,');
        expect(htmlText).not.toContain('Export Metadata Title');
        expect(htmlText).not.toContain('文章正文导出');
        expect(htmlText).not.toContain('/articles/123-export-test');
        expect(htmlText).not.toContain('Export Author');
        expect(htmlText).not.toContain('Metadata summary must not be exported.');
        expect(htmlText).not.toContain('code-copy-button');
        expect(htmlText).toContain('@page');
        expect(htmlText).toContain('margin: 14mm 16mm;');
        expect(htmlText).toContain('@top-left');
        expect(htmlText).toContain('@top-center');
        expect(htmlText).toContain('@bottom-center');
        expect(htmlText).toMatch(/@top-center\s*{\s*content:\s*"";/);
        expect(htmlText).toMatch(/@bottom-center\s*{[\s\S]*content:\s*counter\(page\)\s*"\/"\s*counter\(pages\);/);
        expect(htmlText).toContain('@bottom-right');

        await page.getByTestId('article-export-button').click();
        await expect(page.getByTestId('article-export-menu')).toBeVisible();
        await page.evaluate(() => window.scrollTo(0, 320));
        const scrollYBeforePdfExport = await page.evaluate(() => window.scrollY);
        const pdfPrintPromise = page.evaluate(() => new Promise((resolve) => {
            window.addEventListener('article-export:pdf-print-requested', () => {
                resolve(window.scrollY);
            }, {once: true});
        }));
        await page.evaluate(() => {
            const pdfOption = document.querySelector('[data-testid="article-export-pdf"]');
            if (!pdfOption) {
                throw new Error('PDF export option missing');
            }
            pdfOption.click();
        });
        const scrollYWhenPdfPrintStarted = await pdfPrintPromise;
        expect(Math.abs(scrollYWhenPdfPrintStarted - scrollYBeforePdfExport)).toBeLessThanOrEqual(2);
        await page.waitForFunction(() => window.__articlePdfPrintCount === 1);
        await expect(page.locator('.toast-message', { hasText: 'PDF 打印窗口已打开' })).toBeVisible();
        await expect(page.locator('.toast-message', { hasText: 'PDF 导出已完成' })).toHaveCount(0);
        const printFrame = page.getByTestId('article-pdf-print-frame');
        await expect(printFrame).toHaveCount(1);
        await expect(printFrame).toHaveAttribute('aria-hidden', 'true');
        const printFrameBody = page.frameLocator('[data-testid="article-pdf-print-frame"]')
            .locator('.article-export-body');
        await expect(page.frameLocator('[data-testid="article-pdf-print-frame"]').locator('title')).toHaveText('');
        await expect(printFrameBody).toContainText('Visible body paragraph for export checks.');
        await expect(printFrameBody).not.toContainText('文章正文导出');
        await expect(printFrameBody).not.toContainText('/articles/123-export-test');
        await expect(printFrameBody.locator('img')).toHaveAttribute('src', /data:image\/svg\+xml;base64,/);
        await expect(printFrameBody.locator('canvas')).toHaveCount(0);

        await mockArticleExportFixture(page, { articleId: 124, withImage: false });
        await page.goto('/articles/124-export-no-image-test');
        await expect(page.getByTestId('article-detail-page')).toBeVisible();
        await page.getByTestId('article-export-button').click();
        const markdownWithoutImageDownloadPromise = page.waitForEvent('download');
        await page.getByTestId('article-export-md').click();
        const markdownWithoutImageDownload = await markdownWithoutImageDownloadPromise;
        expect(markdownWithoutImageDownload.suggestedFilename()).toMatch(/\.md$/);
        const markdownWithoutImagePath = await markdownWithoutImageDownload.path();
        const markdownWithoutImageText = await fs.readFile(markdownWithoutImagePath, 'utf8');
        expect(markdownWithoutImageText).toContain('Visible body paragraph for export checks.');
        expect(markdownWithoutImageText).not.toContain('Export Metadata Title');
        expect(markdownWithoutImageText).not.toContain('Export Author');
    });

    test('guest write action still opens login modal', async ({ page }) => {
        await page.goto('/');
        await page.getByTestId('header-write-article').click();
        await expect(page.getByTestId('login-modal')).toBeVisible();
    });

    test('search empty-state action switches tab via route query', async ({ page }) => {
        await page.goto('/search?tab=users&keyword=unlikely_keyword_zzzzzz');
        await expect(page.getByText('未找到「unlikely_keyword_zzzzzz」相关结果')).toBeVisible();
        await page.getByRole('button', { name: '查看文章' }).click();
        await expect(page).toHaveURL(/\/search\?.*tab=articles/);
    });
});

test.describe('authenticated smoke', () => {
    test('home feed channel tabs sync sort query', async ({ page }) => {
        await login(page, USER_ACCOUNT, USER_PASSWORD);
        await page.goto('/');
        await expect(page.getByTestId('home-page')).toBeVisible();
        await expect(page.locator('.feed-tabs button')).toHaveText(['推荐', '最新', '热门']);
        const focusTitle = (await page.locator('[data-testid="home-focus-article"] .portal-focus-title')
            .textContent())?.trim();

        const latestTab = page.locator('.feed-tabs button', { hasText: '最新' });
        await latestTab.click();
        await expect(page).toHaveURL(/sort=latest/);
        await expect(latestTab).toHaveClass(/active/);
        if (focusTitle) {
            await expect(page.locator('[data-testid="home-focus-article"] .portal-focus-title')).toHaveText(focusTitle);
        }

        const hotTab = page.locator('.feed-tabs button', { hasText: '热门' });
        await hotTab.click();
        await expect(page).toHaveURL(/sort=hot/);
        await expect(hotTab).toHaveClass(/active/);
        if (focusTitle) {
            await expect(page.locator('[data-testid="home-focus-article"] .portal-focus-title')).toHaveText(focusTitle);
        }

        const recommendTab = page.locator('.feed-tabs button', { hasText: '推荐' });
        await recommendTab.click();
        await expect(page).toHaveURL(/\/$/);
        await expect(recommendTab).toHaveClass(/active/);
    });

    test('search empty state shows suggestion chips', async ({ page }) => {
        await page.goto('/search?keyword=zzzznonexistent999999&tab=articles');
        await expect(page.locator('.search-empty-fallback')).toBeVisible({ timeout: 10000 });
        await expect(page.locator('.search-empty-fallback .search-empty-chip').first()).toBeVisible();
        // 点击热门标签应跳转
        const hotChip = page.locator('.search-empty-chip.hot').first();
        if (await hotChip.isVisible()) {
            await hotChip.click();
            await expect(page).toHaveURL(/\/search\?.*tab=articles/);
        }
    });

    test('article detail keeps a single related recommendations section', async ({ page }) => {
        await page.goto('/articles/206-go-206');
        await expect(page.getByTestId('article-detail-page')).toBeVisible();
        await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
        await page.waitForTimeout(500);

        await expect(page.locator('.deep-read-recommend')).toHaveCount(0);

        const relatedSections = page.getByTestId('article-related-section');
        expect(await relatedSections.count()).toBeLessThanOrEqual(1);
        const relatedSection = relatedSections.first();
        if (await relatedSection.isVisible({ timeout: 2000 }).catch(() => false)) {
            await expect(relatedSection.locator('.article-related-item').first()).toBeVisible();
            const relatedBox = await relatedSection.boundingBox();
            const articleBox = await page.getByTestId('article-detail-main').boundingBox();
            expect(relatedBox?.x).toBeLessThan(articleBox?.x);
        }
    });

    test('dashboard task card CTA navigates correctly', async ({ page }) => {
        await login(page, USER_ACCOUNT, USER_PASSWORD);
        await page.goto('/dashboard/overview');
        await expect(page.getByTestId('dashboard-overview-page')).toBeVisible();
        const taskCard = page.locator('.task-card').first();
        if (await taskCard.isVisible({ timeout: 3000 }).catch(() => false)) {
            const ctaButton = taskCard.locator('.task-card-cta');
            await expect(ctaButton).toBeVisible();
            const ctaText = await ctaButton.textContent();
            await ctaButton.click();
            // 验证跳转到了对应页面
            await expect(page).not.toHaveURL(/\/dashboard\/overview/);
        }
    });
    test('logged-in user can open core creator pages', async ({ page }) => {
        await login(page, USER_ACCOUNT, USER_PASSWORD);
        await expect(page.getByTestId('dashboard-articles-page')).toBeVisible();

        await page.goto('/');
        await expect(page.getByTestId('home-page')).toBeVisible();

        await page.goto('/editor/new');
        await expect(page.getByTestId('editor-page')).toBeVisible();
        await expect(page.getByTestId('editor-title-input')).toBeVisible();
    });

    test('logged-in user can follow author, subscribe column, and manage own comment', async ({ page }) => {
        await login(page, USER_ACCOUNT, USER_PASSWORD);

        await page.goto('/ranking');
        const followButton = page.getByTestId('author-follow-button').first();
        await expect(followButton).toBeVisible();
        const followLabel = await followButton.textContent();
        await followButton.click();
        await expect(followButton).not.toHaveText(followLabel || '');

        await page.goto('/columns');
        const subscribeButton = page.getByTestId('column-subscribe-button').first();
        await expect(subscribeButton).toBeVisible();
        const subscribeLabel = await subscribeButton.textContent();
        await subscribeButton.click();
        await expect(subscribeButton).not.toHaveText(subscribeLabel || '');

        await page.goto('/articles/206-go-206');
        await expect(page.getByTestId('comment-panel')).toBeVisible();

        const uniqueComment = `smoke-comment-${Date.now()}`;
        const commentPanel = page.getByTestId('comment-panel');
        await commentPanel.getByTestId('comment-composer-input').first().fill(uniqueComment);
        await commentPanel.getByTestId('comment-composer-submit').first().click();

        const createdComment = commentPanel.getByTestId('comment-root-item').filter({ hasText: uniqueComment }).first();
        await expect(createdComment).toBeVisible();
        await expect(commentPanel.locator('.comment-panel-refresh')).toHaveCount(0);
        await createdComment.getByTestId('comment-like-button').click();
        await createdComment.getByTestId('comment-delete-button').click();
        await page.getByRole('button', { name: '确认删除' }).click();
        await expect(createdComment).toHaveCount(0);
    });

    test('admin workspace smoke', async ({ page }) => {
        test.skip(
            !ADMIN_ACCOUNT || !ADMIN_PASSWORD,
            'Set E2E_ADMIN_ACCOUNT and E2E_ADMIN_PASSWORD to run admin smoke.'
        );
        await login(page, ADMIN_ACCOUNT, ADMIN_PASSWORD);
        await page.goto('/admin/overview');
        await expect(page.getByTestId('admin-layout')).toBeVisible();
        await expect(page.getByTestId('admin-overview-stats')).toBeVisible();
        await page.goto('/admin/users');
        await expect(page.getByTestId('admin-users-table')).toBeVisible();
    });
});
