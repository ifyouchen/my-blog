/**
 * Comprehensive Test Data Seeding Script
 * Run: node scripts/seed-data.cjs
 *
 * Generates realistic test data via API:
 * - 50 users (seeduser1~50)
 * - 500 articles (10 per user)
 * - 1000+ comments (2+ per article)
 * - 20 columns (with multiple articles)
 * - Realistic views, likes, favorites
 * - User follows
 */

const API_BASE = 'http://localhost:8080/api';

// Realistic content templates
const ARTICLE_TEMPLATES = {
    titles: [
        '从零搭建高可用微服务架构', '分布式缓存设计与实现', '消息队列在系统解耦中的应用',
        'MySQL索引优化实战指南', 'Redis持久化机制深度解析', 'Docker容器化部署最佳实践',
        'Kubernetes集群管理入门', 'Vue3组合式API深入理解', 'React状态管理方案对比',
        'GraphQL与REST API设计对比', '前后端分离架构实践', '微服务网关设计与实现',
        '数据库分库分表实战', '分布式事务解决方案', '服务监控与链路追踪',
        '高并发系统设计要点', '系统性能优化方法论', '代码质量与重构技巧',
        '设计模式在真实项目中的应用', '架构设计原则与实践'
    ],
    summaries: [
        '深入分析核心技术点，结合真实项目案例，提供可落地的解决方案。',
        '从原理到实践，系统讲解技术方案的实现路径和注意事项。',
        '总结多年一线开发经验，提炼出实用的技术方案和最佳实践。',
        '全面梳理技术知识点，帮助读者构建完整的知识体系。',
        '结合生产环境案例，深入剖析问题根源并给出解决方案。',
        '实战导向，代码示例丰富，适合有一定基础的开发者学习。',
        '系统讲解技术原理，帮助读者从根本上理解技术实现。',
        '梳理项目经验，提供可复用的技术方案和代码模板。'
    ],
    categories: ['Java', 'Spring Boot', 'Vue', 'MySQL', 'Redis', 'AI工程', '运维', '后端', '前端', '数据库'],
    tags: ['Spring Boot', 'JWT', '权限设计', 'Vue 3', '编辑器', 'Markdown', 'MySQL', '索引', '分页', 'Redis'],
    coverImages: [
        'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=900&q=80',
        'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=900&q=80',
        'https://images.unsplash.com/photo-1518770660439-4636190af475?w=900&q=80',
        'https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=900&q=80',
        'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=900&q=80',
        'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=900&q=80',
        'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=900&q=80',
        'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=900&q=80'
    ]
};

const COMMENT_TEMPLATES = {
    contents: [
        '写得很好，受益匪浅！',
        '讲解清晰，对新手很友好',
        '收藏了，期待更多更新',
        '代码示例很实用',
        '终于理解这个概念了',
        '写得深入浅出，赞！',
        '能否出个视频教程？',
        '请问有配套代码仓库吗',
        '这篇比官方文档讲得好',
        '实践中遇到类似问题',
        'mark一下，回头细看',
        '写得很专业，点赞！',
        '补充一下，还可以用XXX',
        '请教个问题，关于XX',
        '已测试，方法有效'
    ],
    replies: [
        '感谢回复！',
        '明白了，谢谢',
        '已解决，赞！',
        '这个思路不错',
        '学习了',
        '确实是这样',
        '补充得很到位',
        '这个坑我也踩过'
    ]
};

const COLUMN_TEMPLATES = {
    names: [
        '微服务架构实战', '前端工程化实践', '数据库性能优化', 'DevOps最佳实践',
        '架构设计模式', '云原生技术探索', '数据架构设计', '安全防护实践',
        '性能调优实战', '代码质量提升', '分布式系统原理', '移动端开发笔记'
    ],
    summaries: [
        '系统总结某技术领域的实践经验和最佳方案',
        '从入门到精通，包含大量实战案例和代码',
        '深入剖析技术原理，帮助读者举一反三',
        '多年技术积累，干货满满的技术分享'
    ]
};

// Helpers
function randInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randItem(arr) {
    return arr[randInt(0, arr.length - 1)];
}

function randItems(arr, count) {
    const shuffled = [...arr].sort(() => Math.random() - 0.5);
    return shuffled.slice(0, count);
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function apiRequest(path, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };

    try {
        const response = await fetch(`${API_BASE}${path}`, {
            method: options.method || 'GET',
            headers,
            body: options.body
        });
        const data = await response.json();
        if (!response.ok || (data.code !== 0 && data.code !== 200)) {
            return null;
        }
        return data.code === 200 ? data : data.data;
    } catch (e) {
        return null;
    }
}

async function login(account, password) {
    const data = await apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ account, password })
    });
    return data?.token;
}

async function register(index) {
    const username = `seeduser${index}`;
    const email = `seeduser${index}@example.com`;

    const data = await apiRequest('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
            username,
            password: '123456',
            email,
            nickname: `用户${index}`
        })
    });

    if (data?.token) {
        return { username, password: '123456', token: data.token };
    }

    // Try login if exists
    const token = await login(username, '123456');
    if (token) {
        return { username, password: '123456', token };
    }
    return null;
}

async function createArticle(token, userIndex, articleIndex) {
    const headers = { Authorization: `Bearer ${token}` };
    const title = `${randItem(ARTICLE_TEMPLATES.titles)} ${userIndex}-${articleIndex}`;
    const category = randItem(ARTICLE_TEMPLATES.categories);
    const tags = randItems(ARTICLE_TEMPLATES.tags, randInt(2, 4));
    const cover = randItem(ARTICLE_TEMPLATES.coverImages);

    const content = `# ${title}\n\n` +
        `## 背景\n\n在实际项目中，我们经常遇到这样的场景：${randItem(ARTICLE_TEMPLATES.summaries)}\n\n` +
        `## 问题分析\n\n本文将深入分析这个问题，并给出具体的解决方案。` +
        `\n\n## 解决方案\n\n核心代码实现如下：\n\n\`\`\`java\n` +
        `public class Example {\n    private final String config;\n    \n    public Example(String config) {\n        this.config = config;\n    }\n    \n    public void process() {\n        System.out.println("Processing: " + config);\n    }\n}\n` +
        `\`\`\`\n\n## 总结\n\n本文介绍了${category}的核心概念和实践方法，希望对大家有所帮助。`;

    const data = await apiRequest('/articles', {
        method: 'POST',
        headers,
        body: JSON.stringify({
            title,
            summary: randItem(ARTICLE_TEMPLATES.summaries),
            content,
            category,
            tags,
            coverUrl: cover,
            status: 'PUBLISHED'
        })
    });

    return data?.id;
}

async function createComment(token, articleId, content) {
    const headers = { Authorization: `Bearer ${token}` };
    await apiRequest(`/articles/${articleId}/comments`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ content })
    });
}

async function followUser(token, userId) {
    const headers = { Authorization: `Bearer ${token}` };
    await apiRequest(`/users/${userId}/follow`, {
        method: 'POST',
        headers
    });
}

async function createColumn(token, name, summary, articleIds) {
    const headers = { Authorization: `Bearer ${token}` };
    const cover = randItem(ARTICLE_TEMPLATES.coverImages);

    // Note: Column creation API might differ - using article IDs
    const data = await apiRequest('/columns', {
        method: 'POST',
        headers,
        body: JSON.stringify({
            title: name,
            summary,
            coverUrl: cover
        })
    });

    return data?.id;
}

async function subscribeColumn(token, columnId) {
    const headers = { Authorization: `Bearer ${token}` };
    await apiRequest(`/columns/${columnId}/subscribe`, {
        method: 'POST',
        headers
    });
}

async function likeArticle(token, articleId) {
    const headers = { Authorization: `Bearer ${token}` };
    await apiRequest(`/articles/${articleId}/like`, {
        method: 'POST',
        headers
    });
}

async function favoriteArticle(token, articleId) {
    const headers = { Authorization: `Bearer ${token}` };
    await apiRequest(`/articles/${articleId}/favorite`, {
        method: 'POST',
        headers
    });
}

// Main seeding
async function seed() {
    console.log('=== Comprehensive Test Data Seeding ===\n');

    const USER_COUNT = 50;
    const ARTICLES_PER_USER = 10;
    const COMMENTS_PER_ARTICLE = 3;

    try {
        // Step 1: Create users
        console.log(`--- Creating ${USER_COUNT} Users ---`);
        const users = [];
        for (let i = 1; i <= USER_COUNT; i++) {
            const user = await register(i);
            if (user) {
                users.push(user);
            }
            if (i % 10 === 0) console.log(`  Created ${i} users...`);
            await sleep(50);
        }
        console.log(`  Total users: ${users.length}\n`);

        // Step 2: Create articles
        console.log(`--- Creating ${users.length * ARTICLES_PER_USER} Articles ---`);
        const articleIds = [];
        for (let u = 0; u < users.length; u++) {
            const user = users[u];
            for (let a = 1; a <= ARTICLES_PER_USER; a++) {
                const id = await createArticle(user.token, u + 1, a);
                if (id) {
                    articleIds.push(id);
                }
            }
            if ((u + 1) % 5 === 0) console.log(`  Created ${(u + 1) * ARTICLES_PER_USER} articles...`);
            await sleep(100);
        }
        console.log(`  Total articles: ${articleIds.length}\n`);

        // Step 3: Create comments
        console.log(`--- Creating ${Math.min(articleIds.length * COMMENTS_PER_ARTICLE, 1500)} Comments ---`);
        let commentCount = 0;
        const commentTargetCount = Math.min(articleIds.length * COMMENTS_PER_ARTICLE, 1500);
        for (let i = 0; i < articleIds.length && commentCount < commentTargetCount; i++) {
            const commentCountForArticle = randInt(1, COMMENTS_PER_ARTICLE);
            for (let c = 0; c < commentCountForArticle && commentCount < commentTargetCount; c++) {
                const commenter = users[randInt(0, users.length - 1)];
                const content = randItem(COMMENT_TEMPLATES.contents);
                await createComment(commenter.token, articleIds[i], content);
                commentCount++;
            }
            if ((i + 1) % 10 === 0) console.log(`  Created ${commentCount} comments...`);
            await sleep(30);
        }
        console.log(`  Total comments: ${commentCount}\n`);

        // Step 4: Create user follows
        console.log('--- Creating User Follows ---');
        let followCount = 0;
        for (let i = 0; i < users.length; i++) {
            const followCountForUser = randInt(5, 20);
            const targets = randItems(
                users.filter((_, idx) => idx !== i),
                followCountForUser
            );
            for (const target of targets) {
                await followUser(users[i].token, i + 2); // User IDs start from 2 in DB
                followCount++;
            }
            if ((i + 1) % 10 === 0) console.log(`  ${(i + 1)} users followed others...`);
            await sleep(20);
        }
        console.log(`  Total follow actions: ${followCount}\n`);

        // Step 5: Create columns
        console.log('--- Creating 20 Columns ---');
        const columnIds = [];
        for (let i = 0; i < 20; i++) {
            const author = users[i % users.length];
            const name = `${COLUMN_TEMPLATES.names[i % COLUMN_TEMPLATES.names.length]}第${Math.floor(i / 12) + 1}期`;
            const summary = COLUMN_TEMPLATES.summaries[i % COLUMN_TEMPLATES.summaries.length];
            const colId = await createColumn(author.token, name, summary, []);
            if (colId) {
                columnIds.push(colId);
            }
            if ((i + 1) % 5 === 0) console.log(`  Created ${i + 1} columns...`);
            await sleep(50);
        }
        console.log(`  Total columns: ${columnIds.length}\n`);

        // Step 6: Column subscriptions
        console.log('--- Creating Column Subscriptions ---');
        let subCount = 0;
        for (const user of users.slice(0, 30)) {
            const colsToSub = randItems(columnIds, randInt(2, 5));
            for (const colId of colsToSub) {
                await subscribeColumn(user.token, colId);
                subCount++;
            }
        }
        console.log(`  Total subscriptions: ${subCount}\n`);

        // Step 7: Simulate likes and favorites
        console.log('--- Simulating Likes and Favorites ---');
        let likeCount = 0;
        let favCount = 0;

        // Each user likes some articles
        for (const user of users) {
            const likedArticles = randItems(articleIds, randInt(20, 50));
            for (const artId of likedArticles) {
                await likeArticle(user.token, artId);
                likeCount++;
            }
            if (likeCount % 100 === 0) console.log(`  ${likeCount} likes simulated...`);
            await sleep(20);
        }

        // Each user favorites some articles
        for (const user of users.slice(0, 40)) {
            const favArticles = randItems(articleIds, randInt(10, 30));
            for (const artId of favArticles) {
                await favoriteArticle(user.token, artId);
                favCount++;
            }
            if (favCount % 50 === 0) console.log(`  ${favCount} favorites simulated...`);
            await sleep(20);
        }
        console.log(`  Total likes: ${likeCount}, favorites: ${favCount}\n`);

        console.log('=== Seeding Complete ===');
        console.log('Summary:');
        console.log(`  - ${users.length} users`);
        console.log(`  - ${articleIds.length} articles`);
        console.log(`  - ${commentCount} comments`);
        console.log(`  - ${followCount} follows`);
        console.log(`  - ${columnIds.length} columns`);
        console.log(`  - ${subCount} column subscriptions`);
        console.log(`  - ${likeCount} article likes`);
        console.log(`  - ${favCount} article favorites`);

    } catch (error) {
        console.error('Seeding error:', error.message);
    }
}

seed();
