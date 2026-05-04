import { expect, test } from '@playwright/test';

const USER_ACCOUNT = process.env.E2E_USER_ACCOUNT || 'demo';
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

test.describe('guest smoke', () => {
    test('home page renders with live content containers', async ({ page }) => {
        await page.goto('/');
        await expect(page.getByTestId('home-page')).toBeVisible();
        await expect(page.locator('[data-feed-root]')).toBeVisible();
        await expect(page.getByTestId('home-specials')).toBeVisible();
        await expect(page.getByTestId('home-authors')).toBeVisible();
    });

    test('following page shows login guide for guests', async ({ page }) => {
        await page.goto('/following');
        await expect(page.getByTestId('following-page')).toBeVisible();
        await expect(page.getByTestId('following-login-empty')).toBeVisible();
    });

    test('columns page renders list and detail entry', async ({ page }) => {
        await page.goto('/columns');
        await expect(page.getByTestId('columns-page')).toBeVisible();
        await expect(page.getByTestId('columns-grid')).toBeVisible();
        const firstColumn = page.getByTestId('column-card').first();
        await expect(firstColumn).toBeVisible();
        await firstColumn.getByRole('link').first().click();
        await expect(page).toHaveURL(/\/columns\/\d+/);
    });

    test('ranking page renders article and author boards', async ({ page }) => {
        await page.goto('/ranking');
        await expect(page.getByTestId('ranking-page')).toBeVisible();
        await expect(page.getByTestId('ranking-articles')).toBeVisible();
        await expect(page.getByTestId('ranking-authors')).toBeVisible();
    });

    test('article detail renders comments and guest write action redirects to login', async ({ page }) => {
        await page.goto('/');
        await page.locator('[data-feed-root] .post-item h3 a[href^="/articles/"]').first().click();
        await expect(page.getByTestId('article-detail-page')).toBeVisible();
        await expect(page.getByTestId('comment-panel')).toBeVisible();
        await page.getByTestId('header-write-article').click();
        await expect(page.getByTestId('login-modal')).toBeVisible();
    });

    test('search empty-state action switches tab via route query', async ({ page }) => {
        await page.goto('/search?tab=users&keyword=unlikely_keyword_zzzzzz');
        await expect(page.getByText('暂无匹配作者')).toBeVisible();
        await page.getByRole('button', { name: '查看相关文章' }).click();
        await expect(page).toHaveURL(/\/search\?.*tab=articles/);
    });
});

test.describe('authenticated smoke', () => {
    test('home feed tab switch syncs query', async ({ page }) => {
        await login(page, USER_ACCOUNT, USER_PASSWORD);
        await page.goto('/');
        await expect(page.getByTestId('home-page')).toBeVisible();
        // 默认应为关注 tab（登录用户）
        const followingTab = page.locator('.feed-tabs button', { hasText: '关注' });
        await expect(followingTab).toBeVisible();
        await followingTab.click();
        await expect(page).toHaveURL(/feedTab=following/);
        const recommendTab = page.locator('.feed-tabs button', { hasText: '推荐' });
        await recommendTab.click();
        await expect(page).toHaveURL(/feedTab=recommend/);
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
        await page.goto('/');
        await page.locator('[data-feed-root] .post-item h3 a[href^="/articles/"]').first().click();
        await expect(page.getByTestId('article-detail-page')).toBeVisible();
        await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
        await page.waitForTimeout(500);

        await expect(page.locator('.deep-read-recommend')).toHaveCount(0);

        const relatedSections = page.getByTestId('article-related-section');
        expect(await relatedSections.count()).toBeLessThanOrEqual(1);
        const relatedSection = relatedSections.first();
        if (await relatedSection.isVisible({ timeout: 2000 }).catch(() => false)) {
            await expect(relatedSection.locator('.article-related-item').first()).toBeVisible();
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

        await page.goto('/');
        await page.locator('[data-feed-root] .post-item h3 a[href^="/articles/"]').first().click();
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
        test.skip(!ADMIN_ACCOUNT || !ADMIN_PASSWORD, 'Set E2E_ADMIN_ACCOUNT and E2E_ADMIN_PASSWORD to run admin smoke.');
        await login(page, ADMIN_ACCOUNT, ADMIN_PASSWORD);
        await page.goto('/admin/overview');
        await expect(page.getByTestId('admin-layout')).toBeVisible();
        await expect(page.getByTestId('admin-overview-stats')).toBeVisible();
        await page.goto('/admin/users');
        await expect(page.getByTestId('admin-users-table')).toBeVisible();
    });
});
