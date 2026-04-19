import { createRouter, createWebHistory } from 'vue-router';
import AdminView from '@/views/AdminView.vue';
import ArticleDetailView from '@/views/ArticleDetailView.vue';
import AuthView from '@/views/AuthView.vue';
import CollectionView from '@/views/CollectionView.vue';
import DashboardView from '@/views/DashboardView.vue';
import EditorView from '@/views/EditorView.vue';
import HomeView from '@/views/HomeView.vue';
import SearchView from '@/views/SearchView.vue';
import UserProfileView from '@/views/UserProfileView.vue';

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
        path: '/editor/new',
        name: 'editor',
        component: EditorView,
        meta: {
            title: '写文章'
        }
    },
    {
        path: '/following',
        name: 'following',
        component: CollectionView,
        meta: {
            title: '关注'
        }
    },
    {
        path: '/columns',
        name: 'columns',
        component: CollectionView,
        meta: {
            title: '专栏'
        }
    },
    {
        path: '/ranking',
        name: 'ranking',
        component: CollectionView,
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
        path: '/admin',
        name: 'admin',
        component: AdminView,
        meta: {
            title: '管理后台'
        }
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior() {
        return { top: 0 };
    }
});

router.afterEach((to) => {
    document.title = to.meta.title ? `${to.meta.title} - my-blog` : 'my-blog';
});

export default router;
