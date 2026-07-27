<script setup lang="ts">
import { ArrowRight, LockKeyhole, Sparkles, UserRound } from '@lucide/vue';
import { reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
});

async function handleSubmit(): Promise<void> {
  if (form.username.trim().length < 3 || form.username.trim().length > 20) {
    ElMessage.warning('用户名长度应为 3 到 20 个字符');
    return;
  }
  if (form.password.length < 6 || form.password.length > 32) {
    ElMessage.warning('密码长度应为 6 到 32 个字符');
    return;
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return;
  }

  try {
    await authStore.register({ username: form.username.trim(), password: form.password });
    await router.replace({ name: 'practice' });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败，请稍后重试');
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-page__intro">
      <div class="brand-mark"><Sparkles :size="22" /></div>
      <p class="auth-page__brand">AI 面试助手</p>
      <h1>从第一道题开始，建立可复用的面试表达能力。</h1>
      <p class="auth-page__description">
        创建账号后即可保存每次训练结果，用数据识别真正需要补强的知识点。
      </p>
    </section>

    <section class="auth-card" aria-labelledby="register-heading">
      <div class="auth-card__heading">
        <p>创建你的训练空间</p>
        <h2 id="register-heading">注册账号</h2>
      </div>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <label>
          <span>用户名</span>
          <el-input
            v-model="form.username"
            :prefix-icon="UserRound"
            autocomplete="username"
            maxlength="20"
          />
        </label>
        <label>
          <span>密码</span>
          <el-input
            v-model="form.password"
            :prefix-icon="LockKeyhole"
            type="password"
            autocomplete="new-password"
            maxlength="32"
            show-password
          />
        </label>
        <label>
          <span>确认密码</span>
          <el-input
            v-model="form.confirmPassword"
            :prefix-icon="LockKeyhole"
            type="password"
            autocomplete="new-password"
            maxlength="32"
            show-password
          />
        </label>
        <el-button
          native-type="submit"
          type="primary"
          :loading="authStore.isSubmitting"
          :icon="ArrowRight"
        >
          创建账号
        </el-button>
      </form>

      <p class="auth-card__footer">
        已有账号？<RouterLink :to="{ name: 'login' }">返回登录</RouterLink>
      </p>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100dvh;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  background: var(--surface);
}

.auth-page__intro {
  display: grid;
  align-content: center;
  gap: 18px;
  padding: clamp(48px, 8vw, 128px);
  background: var(--canvas);
}

.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  background: var(--primary);
  border-radius: var(--radius-sm);
  color: #ffffff;
}

.auth-page__brand {
  color: var(--primary);
  font-size: 14px;
  font-weight: 800;
}

.auth-page h1 {
  max-width: 510px;
  color: var(--ink-strong);
  font-size: clamp(32px, 4vw, 52px);
  line-height: 1.18;
}

.auth-page__description {
  max-width: 470px;
  color: var(--ink-muted);
  font-size: 16px;
  line-height: 1.8;
}

.auth-card {
  display: grid;
  width: min(100%, 430px);
  align-self: center;
  gap: 28px;
  margin: auto;
  padding: 30px;
}

.auth-card__heading p {
  margin-bottom: 4px;
  color: var(--ink-muted);
  font-size: 14px;
  font-weight: 650;
}

.auth-card__heading h2 {
  font-size: 28px;
  line-height: 1.3;
}

.auth-form {
  display: grid;
  gap: 18px;
}

.auth-form label {
  display: grid;
  gap: 8px;
  color: var(--ink);
  font-size: 14px;
  font-weight: 700;
}

.auth-form :deep(.el-button) {
  width: 100%;
  height: 42px;
  margin-top: 4px;
}

.auth-card__footer {
  color: var(--ink-muted);
  font-size: 14px;
}

.auth-card__footer a {
  color: var(--primary);
  font-weight: 750;
}

@media (max-width: 820px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-page__intro {
    min-height: 290px;
    padding: 44px 28px;
  }

  .auth-page h1 {
    font-size: 34px;
  }

  .auth-card {
    width: min(100%, 520px);
    padding: 40px 28px 52px;
  }
}
</style>
