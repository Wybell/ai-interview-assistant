<script setup lang="ts">
import type { Component } from 'vue';

withDefaults(
  defineProps<{
    title: string;
    description: string;
    icon?: Component;
  }>(),
  {
    icon: undefined,
  },
);
</script>

<template>
  <div class="empty-state">
    <component :is="icon" v-if="icon" :size="30" stroke-width="1.6" aria-hidden="true" />
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <div v-if="$slots.default" class="empty-state__action">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: grid;
  min-height: 220px;
  place-items: center;
  align-content: center;
  gap: 8px;
  padding: 32px 20px;
  color: var(--ink-muted);
  text-align: center;
}

.empty-state h3 {
  color: var(--ink);
  font-size: 16px;
  font-weight: 700;
}

.empty-state p {
  max-width: 360px;
  font-size: 14px;
}

.empty-state__action {
  margin-top: 8px;
}
</style>
