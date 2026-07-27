import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

import AppShell from '@/components/layout/AppShell.vue';
import { readSession } from '@/utils/session';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { publicOnly: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { publicOnly: true },
  },
  {
    path: '/',
    component: AppShell,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: { name: 'practice' } },
      { path: 'practice', name: 'practice', component: () => import('@/views/PracticeView.vue') },
      { path: 'mistakes', name: 'mistakes', component: () => import('@/views/MistakesView.vue') },
      { path: 'progress', name: 'progress', component: () => import('@/views/ProgressView.vue') },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: { name: 'practice' },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

router.beforeEach((to) => {
  const authenticated = Boolean(readSession());
  if (to.meta.requiresAuth && !authenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (to.meta.publicOnly && authenticated) {
    return { name: 'practice' };
  }
  return true;
});

export default router;
