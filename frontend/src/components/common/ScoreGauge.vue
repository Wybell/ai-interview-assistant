<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  score: number;
}>();

const radius = 42;
const circumference = 2 * Math.PI * radius;
const normalizedScore = computed(() => Math.min(10, Math.max(0, props.score)));
const offset = computed(() => circumference * (1 - normalizedScore.value / 10));
const tone = computed(() => {
  if (normalizedScore.value >= 8) {
    return 'success';
  }
  if (normalizedScore.value >= 6) {
    return 'warning';
  }
  return 'danger';
});
</script>

<template>
  <div class="score-gauge" :class="`score-gauge--${tone}`" :aria-label="`本次得分 ${score} 分`">
    <svg viewBox="0 0 104 104" role="img" aria-hidden="true">
      <circle class="score-gauge__track" cx="52" cy="52" :r="radius" />
      <circle
        class="score-gauge__value"
        cx="52"
        cy="52"
        :r="radius"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="offset"
      />
    </svg>
    <div class="score-gauge__label">
      <strong>{{ score }}</strong>
      <span>/ 10</span>
    </div>
  </div>
</template>

<style scoped>
.score-gauge {
  position: relative;
  display: grid;
  width: 108px;
  height: 108px;
  place-items: center;
}

.score-gauge svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.score-gauge circle {
  fill: none;
  stroke-width: 8;
}

.score-gauge__track {
  stroke: var(--surface-muted);
}

.score-gauge__value {
  stroke: var(--primary);
  stroke-linecap: round;
  transition: stroke-dashoffset 420ms ease;
}

.score-gauge--success .score-gauge__value {
  stroke: var(--success);
}

.score-gauge--warning .score-gauge__value {
  stroke: var(--warning);
}

.score-gauge--danger .score-gauge__value {
  stroke: var(--danger);
}

.score-gauge__label {
  position: absolute;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.score-gauge__label strong {
  font-size: 30px;
  line-height: 1;
}

.score-gauge__label span {
  color: var(--ink-muted);
  font-size: 12px;
}
</style>
