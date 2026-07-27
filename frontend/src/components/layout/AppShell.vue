<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';

import AppHeader from '@/components/layout/AppHeader.vue';
import AppSidebar from '@/components/layout/AppSidebar.vue';

const MOBILE_NAVIGATION_BREAKPOINT = 840;

function shouldCollapseNavigation(): boolean {
  return typeof window !== 'undefined' && window.innerWidth <= MOBILE_NAVIGATION_BREAKPOINT;
}

const navigationCollapsed = ref(shouldCollapseNavigation());
let mobileNavigationQuery: MediaQueryList | null = null;

function toggleNavigation(): void {
  navigationCollapsed.value = !navigationCollapsed.value;
}

function collapseNavigationOnSmallScreen(event: MediaQueryListEvent): void {
  if (event.matches) {
    navigationCollapsed.value = true;
  }
}

onMounted(() => {
  // 与 CSS 断点保持一致，避免小屏初次进入时展开侧栏遮住训练内容。
  mobileNavigationQuery = window.matchMedia(`(max-width: ${MOBILE_NAVIGATION_BREAKPOINT}px)`);
  mobileNavigationQuery.addEventListener('change', collapseNavigationOnSmallScreen);
});

onBeforeUnmount(() => {
  mobileNavigationQuery?.removeEventListener('change', collapseNavigationOnSmallScreen);
});
</script>

<template>
  <div class="app-shell">
    <AppSidebar :collapsed="navigationCollapsed" @toggle="toggleNavigation" />
    <div class="app-shell__content">
      <AppHeader @toggle-navigation="toggleNavigation" />
      <main class="app-shell__main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100dvh;
}

.app-shell__content {
  min-width: 0;
  flex: 1;
}

.app-shell__main {
  width: min(100%, var(--content-max));
  margin: 0 auto;
  padding: 32px;
}

@media (max-width: 840px) {
  .app-shell__content {
    margin-left: 68px;
  }

  .app-shell__main {
    padding: 22px 18px;
  }
}

@media (max-width: 560px) {
  .app-shell__main {
    padding: 18px 14px;
  }
}
</style>
