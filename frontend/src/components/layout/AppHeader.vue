<script setup lang="ts">
import { ChevronDown, LoaderCircle, LogOut, Menu, UserRound } from '@lucide/vue';
import { computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

import { useAuthStore } from '@/stores/auth';
import { useModelStore } from '@/stores/models';

const emit = defineEmits<{
  toggleNavigation: [];
}>();

const router = useRouter();
const authStore = useAuthStore();
const modelStore = useModelStore();

const selectedModelId = computed<number | undefined>(
  () => modelStore.preference?.modelId ?? modelStore.selectedModel?.id,
);

async function handleModelChange(modelId: number): Promise<void> {
  try {
    await modelStore.select(modelId);
    ElMessage.success('模型偏好已保存');
  } catch (error) {
    const message = error instanceof Error ? error.message : '模型切换失败，请稍后重试';
    ElMessage.error(message);
  }
}

function handleAccountCommand(command: string): void {
  if (command === 'logout') {
    authStore.logout();
    void router.replace({ name: 'login' });
  }
}

onMounted(async () => {
  try {
    await modelStore.load();
  } catch (error) {
    const message = error instanceof Error ? error.message : '模型目录暂不可用';
    ElMessage.warning(message);
  }
});
</script>

<template>
  <header class="app-header">
    <el-tooltip content="切换导航" placement="bottom">
      <button class="header-icon" type="button" @click="emit('toggleNavigation')">
        <Menu :size="20" />
      </button>
    </el-tooltip>

    <div class="app-header__spacer" />

    <div class="model-select" :class="{ 'model-select--loading': modelStore.isLoading }">
      <span class="model-select__label">当前模型</span>
      <LoaderCircle v-if="modelStore.isLoading" class="spin" :size="16" />
      <el-select
        v-else
        :model-value="selectedModelId"
        :loading="modelStore.isSaving"
        :disabled="modelStore.models.length === 0"
        size="small"
        aria-label="选择 AI 模型"
        @change="handleModelChange"
      >
        <el-option
          v-for="model in modelStore.models"
          :key="model.id"
          :label="model.displayName"
          :value="model.id"
        >
          <div class="model-option">
            <span>{{ model.displayName }}</span>
            <small v-if="model.defaultModel">系统默认</small>
          </div>
        </el-option>
      </el-select>
    </div>

    <el-dropdown trigger="click" @command="handleAccountCommand">
      <button class="account-trigger" type="button">
        <span class="account-trigger__avatar"><UserRound :size="16" /></span>
        <span class="account-trigger__name">{{ authStore.username }}</span>
        <ChevronDown :size="16" />
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="logout">
            <LogOut :size="15" />
            退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  min-height: 68px;
  align-items: center;
  gap: 16px;
  padding: 0 28px;
  background: rgb(255 255 255 / 88%);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(10px);
}

.header-icon {
  display: none;
  width: 36px;
  height: 36px;
  place-items: center;
  padding: 0;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  color: var(--ink);
}

.header-icon:hover {
  background: var(--surface-hover);
}

.app-header__spacer {
  flex: 1;
}

.model-select {
  display: flex;
  align-items: center;
  gap: 10px;
}

.model-select__label {
  color: var(--ink-muted);
  font-size: 13px;
  font-weight: 650;
}

.model-select :deep(.el-select) {
  width: 176px;
}

.model-option {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.model-option small {
  color: var(--ink-muted);
}

.account-trigger {
  display: flex;
  height: 38px;
  align-items: center;
  gap: 8px;
  padding: 0 4px 0 0;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  color: var(--ink);
}

.account-trigger:hover {
  color: var(--primary);
}

.account-trigger__avatar {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: 50%;
}

.account-trigger__name {
  max-width: 110px;
  overflow: hidden;
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spin {
  animation: spin 800ms linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 840px) {
  .app-header {
    padding: 0 18px;
  }

  .header-icon {
    display: grid;
  }
}

@media (max-width: 640px) {
  .model-select__label,
  .account-trigger__name {
    display: none;
  }

  .model-select :deep(.el-select) {
    width: 146px;
  }
}
</style>
