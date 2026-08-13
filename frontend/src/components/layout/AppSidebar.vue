<script setup lang="ts">
import {
  BookOpen,
  ChartBar,
  FileSearch,
  ClipboardList,
  Library,
  MessagesSquare,
  PanelLeftClose,
  PanelLeftOpen,
  Sparkles,
} from '@lucide/vue';
import { useAuthStore } from '@/stores/auth';

const props = defineProps<{
  collapsed: boolean;
}>();

const emit = defineEmits<{
  toggle: [];
}>();
const authStore = useAuthStore();

const navigation = [
  { name: 'mock-interview', label: '模拟面试', icon: MessagesSquare },
  { name: 'interview-reviews', label: '面试复盘', icon: FileSearch },
  { name: 'practice', label: '面试训练', icon: BookOpen },
  { name: 'mistakes', label: '错题本', icon: ClipboardList },
  { name: 'progress', label: '学习进度', icon: ChartBar },
  { name: 'knowledge', label: '知识库', icon: Library },
] as const;

const adminNavigation = { name: 'admin-knowledge', label: '知识库管理', icon: Library } as const;

function handleNavigation(): void {
  if (!props.collapsed && window.matchMedia('(max-width: 840px)').matches) {
    emit('toggle');
  }
}
</script>

<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': collapsed }">
    <div class="sidebar__brand">
      <div class="brand-mark" aria-hidden="true"><Sparkles :size="19" /></div>
      <span v-if="!collapsed" class="brand-name">AI 面试助手</span>
      <el-tooltip :content="collapsed ? '展开导航' : '收起导航'" placement="right">
        <button class="icon-button sidebar__toggle" type="button" @click="$emit('toggle')">
          <PanelLeftOpen v-if="collapsed" :size="18" />
          <PanelLeftClose v-else :size="18" />
        </button>
      </el-tooltip>
    </div>

    <nav class="sidebar__nav" aria-label="主导航">
      <el-tooltip
        v-for="item in navigation"
        :key="item.name"
        :content="item.label"
        :disabled="!collapsed"
        placement="right"
      >
        <RouterLink
          :to="{ name: item.name }"
          :aria-label="item.label"
          class="sidebar__link"
          @click="handleNavigation"
        >
          <component :is="item.icon" :size="19" stroke-width="1.8" />
          <span v-if="!collapsed">{{ item.label }}</span>
        </RouterLink>
      </el-tooltip>
    </nav>
    <nav v-if="authStore.role === 'ADMIN'" class="sidebar__nav" aria-label="管理导航">
      <RouterLink
        :to="{ name: adminNavigation.name }"
        class="sidebar__link"
        @click="handleNavigation"
      >
        <component :is="adminNavigation.icon" :size="19" stroke-width="1.8" />
        <span v-if="!collapsed">{{ adminNavigation.label }}</span>
      </RouterLink>
    </nav>

    <div v-if="!collapsed" class="sidebar__footer">专注每一次真实练习</div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: relative;
  display: flex;
  width: 224px;
  min-height: 100dvh;
  flex-direction: column;
  flex: 0 0 224px;
  background: var(--surface);
  border-right: 1px solid var(--border);
  transition:
    width var(--transition-fast),
    flex-basis var(--transition-fast);
}

.sidebar--collapsed {
  width: 68px;
  flex-basis: 68px;
}

.sidebar__brand {
  display: flex;
  height: 68px;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border-bottom: 1px solid var(--border);
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  background: var(--primary);
  border-radius: var(--radius-sm);
  color: #ffffff;
}

.brand-name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  font-size: 15px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-button {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  color: var(--ink-muted);
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.icon-button:hover {
  background: var(--surface-hover);
  color: var(--ink-strong);
}

.sidebar__toggle {
  margin-left: auto;
}

.sidebar--collapsed .sidebar__toggle {
  position: absolute;
  top: 17px;
  right: -42px;
  z-index: 4;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
}

.sidebar__nav {
  display: grid;
  gap: 4px;
  padding: 16px 10px;
}

.sidebar__link {
  display: flex;
  height: 42px;
  align-items: center;
  gap: 11px;
  padding: 0 11px;
  border-radius: var(--radius-sm);
  color: var(--ink-muted);
  font-size: 14px;
  font-weight: 650;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.sidebar--collapsed .sidebar__link {
  justify-content: center;
  padding: 0;
}

.sidebar__link:hover {
  background: var(--surface-hover);
  color: var(--ink-strong);
}

.sidebar__link.router-link-active {
  background: var(--primary-subtle);
  color: var(--primary);
}

.sidebar__footer {
  margin-top: auto;
  padding: 20px 22px;
  color: var(--ink-muted);
  font-size: 12px;
}

@media (max-width: 840px) {
  .sidebar {
    position: fixed;
    z-index: 20;
    width: 68px;
    flex-basis: 68px;
  }

  .sidebar:not(.sidebar--collapsed) {
    width: 224px;
  }

  .sidebar--collapsed .sidebar__toggle {
    display: none;
  }
}
</style>
