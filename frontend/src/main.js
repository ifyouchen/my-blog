import { createApp } from 'vue';
import { createHead } from '@unhead/vue';
import App from './App.vue';
import router from './router';
import './styles/global.css';

createApp(App)
    .use(router)
    .use(createHead())
    .mount('#app');
