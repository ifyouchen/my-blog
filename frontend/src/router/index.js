import {createRouter, createWebHistory} from 'vue-router';
import NProgress from 'nprogress';
import 'nprogress/nprogress.css';

NProgress.configure({ showSpinner: false, trickleSpeed: 200, minimum: 0.15 });

const HomeView = () => import('@/views/HomeView.vue');
const AuthView = () => import('@/views/AuthView.vue');
const ForgotPasswordView = () => import('@/views/ForgotPasswordView.vue');
const ResetPasswordView = () => import('@/views/ResetPasswordView.vue');
const EditorView = () => import('@/views/EditorView.vue');
const FollowingView = () => import('@/views/FollowingView.vue');
const ColumnsView = () => import('@/views/ColumnsView.vue');
const ColumnDetailView = () => import('@/views/ColumnDetailView.vue');
const TopicsView = () => import('@/views/TopicsView.vue');
const TopicDetailView = () => import('@/views/TopicDetailView.vue');
const RankingView = () => import('@/views/RankingView.vue');
const SearchView = () => import('@/views/SearchView.vue');
const NotificationsView = () => import('@/views/NotificationsView.vue');
const ArticleDetailView = () => import('@/views/ArticleDetailView.vue');
const UserProfileView = () => import('@/views/UserProfileView.vue');
const DashboardView = () => import('@/views/DashboardView.vue');
const DashboardColumnsView = () => import('@/views/DashboardColumnsView.vue');
const ProfileSettingsView = () => import('@/views/ProfileSettingsView.vue');
const AdminLayout = () => import('@/views/admin/AdminLayout.vue');
const AdminOverviewView = () => import('@/views/admin/AdminOverviewView.vue');
const AdminUsersView = () => import('@/views/admin/AdminUsersView.vue');
const AdminArticlesView = () => import('@/views/admin/AdminArticlesView.vue');
const AdminCommentsView = () => import('@/views/admin/AdminCommentsView.vue');
const AdminReportsView = () => import('@/views/admin/AdminReportsView.vue');
const AdminCategoriesView = () => import('@/views/admin/AdminCategoriesView.vue');
const AdminTagsView = () => import('@/views/admin/AdminTagsView.vue');
const AdminColumnsView = () => import('@/views/admin/AdminColumnsView.vue');
const AdminTopicsView = () => import('@/views/admin/AdminTopicsView.vue');
const AdminLogsView = () => import('@/views/admin/AdminLogsView.vue');
const AdminAdsView = () => import('@/views/admin/AdminAdsView.vue');
const AdminAnnouncementsView = () => import('@/views/admin/AdminAnnouncementsView.vue');
const AdminSensitiveWordsView = () => import('@/views/admin/AdminSensitiveWordsView.vue');
const AdminInviteCodesView = () => import('@/views/admin/AdminInviteCodesView.vue');
const CategoryDetailView = () => import('@/views/CategoryDetailView.vue');
const TagDetailView = () => import('@/views/TagDetailView.vue');
const ExploreView = () => import('@/views/ExploreView.vue');

const routes = [
    {
        path: '/',
        name: 'home',
        component: HomeView,
        meta: {
            title: '首页'
        }
    },
    {
        path: '/login',
        name: 'login',
        component: AuthView,
        meta: {
            title: '登录'
        }
    },
    {
        path: '/register',
        name: 'register',
        component: AuthView,
        meta: {
            title: '注册'
        }
    },
    {
        path: '/auth/forgot-password',
        name: 'forgotPassword',
        component: ForgotPasswordView,
        meta: {
            title: '忘记密码'
        }
    },
    {
        path: '/auth/reset-password',
        name: 'resetPassword',
        component: ResetPasswordView,
        meta: {
            title: '重置密码'
        }
    },
    {
        path: '/editor/new',
        name: 'editor',
        component: EditorView,
        meta: {
            title: '写文章'
        }
    },
    {
        path: '/editor/:id',
        name: 'editorEdit',
        component: EditorView,
        meta: {
            title: '编辑文章'
        }
    },
    {
        path: '/following',
        name: 'following',
        component: FollowingView,
        meta: {
            title: '关注'
        }
    },
    {
        path: '/columns',
        name: 'columns',
        component: ColumnsView,
        meta: {
            title: '专栏'
        }
    },
    {
        path: '/columns/:id',
        name: 'columnDetail',
        component: ColumnDetailView,
        meta: {
            title: '专栏详情'
        }
    },
    {
        path: '/topics',
        name: 'topics',
        component: TopicsView,
        meta: {
            title: '专题'
        }
    },
    {
        path: '/topics/:id',
        name: 'topicDetail',
        component: TopicDetailView,
        meta: {
            title: '专题详情'
        }
    },
    {
        path: '/explore',
        name: 'explore',
        component: ExploreView,
        meta: {
            title: '探索'
        }
    },
    {
        path: '/categories/:id',
        name: 'categoryDetail',
        component: CategoryDetailView,
        meta: {
            title: '分类'
        }
    },
    {
        path: '/tags/:id',
        name: 'tagDetail',
        component: TagDetailView,
        meta: {
            title: '标签'
        }
    },
    {
        path: '/ranking',
        name: 'ranking',
        component: RankingView,
        meta: {
            title: '排行榜'
        }
    },
    {
        path: '/search',
        name: 'search',
        component: SearchView,
        meta: {
            title: '搜索'
        }
    },
    {
        path: '/notifications',
        name: 'notifications',
        component: NotificationsView,
        meta: {
            title: '通知中心'
        }
    },
    {
        path: '/articles/:id',
        name: 'articleDetail',
        component: ArticleDetailView,
        meta: {
            title: '文章详情'
        }
    },
    {
        path: '/users/:id',
        name: 'userProfile',
        component: UserProfileView,
        meta: {
            title: '个人主页'
        }
    },
    {
        path: '/dashboard/overview',
        name: 'dashboardOverview',
        component: DashboardView,
        meta: {
            title: '创作概览'
        }
    },
    {
        path: '/dashboard/articles',
        name: 'dashboardArticles',
        component: DashboardView,
        meta: {
            title: '我的文章'
        }
    },
    {
        path: '/dashboard/favorites',
        name: 'favorites',
        component: DashboardView,
        meta: {
            title: '我的收藏'
        }
    },
    {
        path: '/dashboard/columns',
        name: 'dashboardColumns',
        component: DashboardColumnsView,
        meta: {
            title: '我的专栏'
        }
    },
    {
        path: '/settings/profile',
        name: 'profileSettings',
        component: ProfileSettingsView,
        meta: {
            title: '个人资料'
        }
    },
    {
        path: '/admin',
        component: AdminLayout,
        meta: {
            title: '管理后台'
        },
        children: [
            {
                path: '',
                redirect: '/admin/overview'
            },
            {
                path: 'overview',
                name: 'adminOverview',
                component: AdminOverviewView,
                meta: {
                    title: '后台概览',
                    adminTitle: '后台概览',
                    adminDescription: '查看站点核心数据、处理重点和最近管理员操作。'
                }
            },
            {
                path: 'users',
                name: 'adminUsers',
                component: AdminUsersView,
                meta: {
                    title: '用户管理',
                    adminTitle: '用户管理',
                    adminDescription: '筛选用户状态、检索账号信息，并快速处理启用与禁用。'
                }
            },
            {
                path: 'articles',
                name: 'adminArticles',
                component: AdminArticlesView,
                meta: {
                    title: '文章管理',
                    adminTitle: '文章管理',
                    adminDescription: '统一管理文章状态、分类归属和基础数据表现。'
                }
            },
            {
                path: 'comments',
                name: 'adminComments',
                component: AdminCommentsView,
                meta: {
                    title: '评论管理',
                    adminTitle: '评论管理',
                    adminDescription: '定位问题评论，按文章或关键词筛查并执行删除。'
                }
            },
            {
                path: 'reports',
                name: 'adminReports',
                component: AdminReportsView,
                meta: {
                    title: '举报管理',
                    adminTitle: '举报管理',
                    adminDescription: '处理用户举报，联动文章下架、评论删除和用户禁用。'
                }
            },
            {
                path: 'categories',
                name: 'adminCategories',
                component: AdminCategoriesView,
                meta: {
                    title: '分类管理',
                    adminTitle: '分类管理',
                    adminDescription: '维护内容分类体系，统一处理启用状态和排序。'
                }
            },
            {
                path: 'taxonomy',
                redirect: '/admin/categories'
            },
            {
                path: 'tags',
                name: 'adminTags',
                component: AdminTagsView,
                meta: {
                    title: '标签管理',
                    adminTitle: '标签管理',
                    adminDescription: '维护标签体系，保证搜索、编辑器和后台数据一致。'
                }
            },
            {
                path: 'columns',
                name: 'adminColumns',
                component: AdminColumnsView,
                meta: {
                    title: '专栏管理',
                    adminTitle: '专栏管理',
                    adminDescription: '新增、编辑、删除专栏，管理专栏状态和排序。'
                }
            },
            {
                path: 'topics',
                name: 'adminTopics',
                component: AdminTopicsView,
                meta: {
                    title: '专题管理',
                    adminTitle: '专题管理',
                    adminDescription: '新增、编辑、删除专题，管理专题文章归属。'
                }
            },
            {
                path: 'ads',
                name: 'adminAds',
                component: AdminAdsView,
                meta: {
                    title: '广告管理',
                    adminTitle: '广告管理',
                    adminDescription: '配置广告位投放内容，管理广告时间范围、启用状态和展示顺序。'
                }
            },
            {
                path: 'announcements',
                name: 'adminAnnouncements',
                component: AdminAnnouncementsView,
                meta: {
                    title: '公告管理',
                    adminTitle: '公告管理',
                    adminDescription: '创建、编辑、发布和撤回平台公告，实时推送给用户。'
                }
            },
            {
                path: 'sensitive-words',
                name: 'adminSensitiveWords',
                component: AdminSensitiveWordsView,
                meta: {
                    title: '敏感词管理',
                    adminTitle: '敏感词管理',
                    adminDescription: '管理内容审核敏感词库，配置警告或拦截规则。'
                }
            },
            {
                path: 'invite-codes',
                name: 'adminInviteCodes',
                component: AdminInviteCodesView,
                meta: {
                    title: '邀请码管理',
                    adminTitle: '邀请码管理',
                    adminDescription: '查看与管理站点邀请码，跟踪使用情况。'
                }
            },
            {
                path: 'logs',
                name: 'adminLogs',
                component: AdminLogsView,
                meta: {
                    title: '管理员日志',
                    adminTitle: '管理员日志',
                    adminDescription: '按操作类型、结果状态和时间范围追踪后台审计记录。'
                }
            }
        ]
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition;
        }
        const samePathNoScroll = ['/', '/dashboard/overview', '/dashboard/articles', '/dashboard/columns', '/dashboard/favorites', '/search'];
        if (to.path === from.path && samePathNoScroll.includes(to.path)) {
            return false;
        }
        return { top: 0 };
    }
});

router.beforeEach((to, from) => {
    if (to.path !== from.path) {
        NProgress.start();
    }
});

router.afterEach(() => {
    NProgress.done();
});

router.onError(() => {
    NProgress.done();
});

export default router;
