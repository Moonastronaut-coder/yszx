import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './app/App.vue';
import { router } from './app/router';
import '@yunshu/ui/theme.css';
import 'element-plus/dist/index.css';

createApp(App).use(createPinia()).use(router).mount('#app');
