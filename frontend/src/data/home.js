export const navItems = [
    { label: '推荐', path: '/' },
    { label: '关注', path: '/following' },
    { label: '专栏', path: '/columns' },
    { label: '排行榜', path: '/ranking' }
];

export const topics = [
    '全部',
    'Java',
    'Spring Boot',
    'Vue',
    'MySQL',
    'Redis',
    'AI 工程',
    '运维'
];

export const articles = [
    {
        id: 1,
        title: 'Spring Boot 登录鉴权从 0 到 1：JWT、权限拦截和异常返回',
        summary: '把登录态、角色权限、接口拦截、统一错误码串成完整闭环，适合直接迁移到第一版博客后端。',
        category: '后端',
        readingTime: '12 分钟阅读',
        publishedText: '刚刚更新',
        cover: 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=900&q=80',
        coverAlt: '代码编辑器中的后端服务代码',
        author: {
            id: 1,
            name: '林知远',
            avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=96&q=80'
        },
        tags: ['Spring Boot', 'JWT', '权限设计'],
        content: [
            '登录鉴权的第一步不是选择 Token 还是 Session，而是把系统里哪些接口允许游客访问、哪些接口必须登录、哪些接口需要管理员权限先定清楚。',
            '在博客系统里，JWT 适合前后端分离的第一版实现。登录成功后由后端签发 Token，前端在请求头中携带 Authorization，后端统一解析用户身份和角色。',
            '权限拦截建议放在统一过滤器中完成，Controller 只接收已经通过认证的请求。涉及文章编辑、评论删除这类资源归属判断时，仍然需要在应用服务里做二次校验。',
            '异常返回要保持稳定结构。前端只需要关心 code、message 和 data，就能在登录过期、无权限、参数错误等场景给出一致反馈。'
        ],
        stats: {
            views: '1,204 阅读',
            likes: '86 赞',
            comments: '24 评论'
        },
        featured: true
    },
    {
        id: 2,
        title: 'Vue 3 博客编辑器体验设计：草稿、预览、标签和封面上传',
        summary: '文章编辑不是一个表单，而是一条创作路径：先稳住输入，再减少发布前的不确定。',
        category: '前端',
        readingTime: '8 分钟阅读',
        publishedText: '今天',
        cover: 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=700&q=80',
        coverAlt: '笔记本电脑上的前端开发界面',
        author: {
            id: 2,
            name: '沈星',
            avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=96&q=80'
        },
        tags: ['Vue 3', '编辑器', 'Markdown'],
        content: [
            '博客编辑器需要让创作者尽快进入写作状态。标题、正文、分类、标签和封面图是第一版最重要的字段。',
            '草稿能力能显著降低发布压力。用户可以先保存标题和正文，之后再补齐摘要、封面和标签。',
            '预览区不需要一开始就做复杂排版，但 Markdown、代码块和图片展示必须稳定，因为技术文章最依赖这些表达方式。'
        ],
        stats: {
            views: '832 阅读',
            likes: '51 赞',
            comments: '13 评论'
        }
    },
    {
        id: 3,
        title: 'MySQL 文章列表查询优化：分类、标签、排序和分页索引怎么建',
        summary: '第一版不用上搜索引擎，也能通过清晰的索引和查询边界撑住常见列表场景。',
        category: '数据库',
        readingTime: '10 分钟阅读',
        publishedText: '昨天',
        cover: 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=700&q=80',
        coverAlt: '服务器机柜和数据中心设备',
        author: {
            id: 3,
            name: '周行',
            avatar: 'https://images.unsplash.com/photo-1519345182560-3f2917c472ef?auto=format&fit=crop&w=96&q=80'
        },
        tags: ['MySQL', '索引', '分页'],
        content: [
            '第一版博客列表通常会同时支持分类、标签、关键字和排序。设计索引时要先确定最高频的查询组合。',
            '首页推荐列表可以优先按发布时间排序，分类页可以用 category_id 加 published_at 的组合索引，标签页则依赖 article_tag 关联表。',
            '深分页不建议直接 LIMIT 很大的偏移量。等数据量增长后，可以使用游标分页或延迟关联减少扫描成本。'
        ],
        stats: {
            views: '1,608 阅读',
            likes: '103 赞',
            comments: '31 评论'
        }
    },
    {
        id: 4,
        title: 'DDD 单体项目怎么拆包：interfaces、application、domain 和 infrastructure',
        summary: '先把依赖方向管住，再谈模型边界。单体项目也可以有清晰的领域结构。',
        category: '架构',
        readingTime: '15 分钟阅读',
        publishedText: '本周',
        cover: 'https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=700&q=80',
        coverAlt: '团队在白板前讨论系统设计',
        author: {
            id: 4,
            name: '陈野',
            avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80'
        },
        tags: ['DDD', '架构', '单体应用'],
        content: [
            'DDD 单体项目的重点不是把包拆得很细，而是让依赖方向稳定。用户接口层调用应用层，应用层编排领域层，基础设施层实现仓储。',
            '领域模型应该表达业务概念。文章发布、下架、删除这些状态变化，应该通过领域方法完成，而不是散落在 Controller 或 Mapper 里。',
            '单体项目仍然可以保留清晰边界。后续如果业务增长，清晰的领域结构会比一开始追求微服务更可靠。'
        ],
        stats: {
            views: '2,019 阅读',
            likes: '147 赞',
            comments: '45 评论'
        }
    }
];

export const specials = [
    {
        title: 'Spring Security 实战',
        image: 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=220&q=80',
        alt: '网络安全主题图'
    },
    {
        title: 'Vue 3 工程化',
        image: 'https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&w=220&q=80',
        alt: '代码屏幕主题图'
    },
    {
        title: 'MySQL 性能调优',
        image: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=220&q=80',
        alt: '数据分析屏幕主题图'
    }
];

export const authors = [
    {
        name: '秦川',
        title: '后端架构 · 36 篇',
        avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=96&q=80'
    },
    {
        name: '叶澜',
        title: '前端工程 · 28 篇',
        avatar: 'https://images.unsplash.com/photo-1531123897727-8f129e1688ce?auto=format&fit=crop&w=96&q=80'
    },
    {
        name: '何青',
        title: '数据库 · 22 篇',
        avatar: 'https://images.unsplash.com/photo-1527980965255-d3b416303d12?auto=format&fit=crop&w=96&q=80'
    }
];

export const comments = [
    {
        id: 1,
        author: '许舟',
        avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=96&q=80',
        content: '权限矩阵先写清楚这一点很有用，后面写接口的时候会少很多返工。',
        time: '12 分钟前'
    },
    {
        id: 2,
        author: '南乔',
        avatar: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=96&q=80',
        content: '建议后续把刷新 Token 和用户禁用后的旧 Token 处理也补一篇。',
        time: '38 分钟前'
    },
    {
        id: 3,
        author: '陆离',
        avatar: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=96&q=80',
        content: '这个结构很适合毕业项目，也不会显得只是 CRUD。',
        time: '今天'
    }
];

export const dashboardArticles = [
    {
        id: 1,
        title: 'Spring Boot 登录鉴权从 0 到 1：JWT、权限拦截和异常返回',
        status: '已发布',
        views: 1204,
        likes: 86,
        comments: 24,
        updatedAt: '2026-04-19 19:40'
    },
    {
        id: 5,
        title: '博客项目数据库表设计复盘',
        status: '草稿',
        views: 0,
        likes: 0,
        comments: 0,
        updatedAt: '2026-04-19 18:20'
    },
    {
        id: 6,
        title: '前端路由守卫和登录态恢复',
        status: '已下架',
        views: 421,
        likes: 18,
        comments: 6,
        updatedAt: '2026-04-18 22:10'
    }
];

export const adminStats = [
    { label: '用户总数', value: '12,860' },
    { label: '文章总数', value: '2,481' },
    { label: '评论总数', value: '18,392' },
    { label: '今日新增', value: '46' }
];

export const adminUsers = [
    { id: 1, username: 'linzy', email: 'lin@example.com', status: '正常', createdAt: '2026-04-01' },
    { id: 2, username: 'shenxing', email: 'shen@example.com', status: '正常', createdAt: '2026-04-03' },
    { id: 3, username: 'temp_user', email: 'temp@example.com', status: '禁用', createdAt: '2026-04-10' }
];
