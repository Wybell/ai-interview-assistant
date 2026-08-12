import { createApp } from 'vue';
import {
  ElButton,
  ElDrawer,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElInput,
  ElOption,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTooltip,
} from 'element-plus';
import { createPinia } from 'pinia';
import 'element-plus/dist/index.css';

import App from '@/App.vue';
import router from '@/router';
import { useAuthStore } from '@/stores/auth';
import '@/styles/base.css';
import '@/styles/element-plus.css';

const app = createApp(App);
const pinia = createPinia();
const authStore = useAuthStore(pinia);

app.use(pinia);
app.use(router);
app.use(ElButton);
app.use(ElDrawer);
app.use(ElDropdown);
app.use(ElDropdownItem);
app.use(ElDropdownMenu);
app.use(ElInput);
app.use(ElOption);
app.use(ElRadioButton);
app.use(ElRadioGroup);
app.use(ElSelect);
app.use(ElTable);
app.use(ElTableColumn);
app.use(ElTag);
app.use(ElTooltip);

window.addEventListener('auth:expired', () => {
  authStore.logout();
  if (router.currentRoute.value.name !== 'login') {
    void router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } });
  }
});

app.mount('#app');
