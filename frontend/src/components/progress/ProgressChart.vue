<script setup lang="ts">
import { init, use, type EChartsType } from 'echarts/core';
import { BarChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { BarChart3 } from '@lucide/vue';
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import AppEmptyState from '@/components/common/AppEmptyState.vue';
import type { StudyProgress } from '@/types/interview';

use([BarChart, GridComponent, TooltipComponent, CanvasRenderer]);

const props = defineProps<{
  items: StudyProgress[];
}>();

const chartElement = ref<HTMLDivElement>();
let chart: EChartsType | null = null;
let resizeObserver: ResizeObserver | null = null;

function renderChart(): void {
  if (!chart || props.items.length === 0) {
    return;
  }

  chart.setOption({
    animationDuration: 380,
    grid: { top: 20, right: 18, bottom: 34, left: 42 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value: number) => `${Number(value).toFixed(1)} 分`,
    },
    xAxis: {
      type: 'category',
      data: props.items.map((item) => item.tag),
      axisLabel: { color: '#6c7f79', interval: 0, rotate: props.items.length > 5 ? 24 : 0 },
      axisLine: { lineStyle: { color: '#dce7e3' } },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 10,
      interval: 2,
      axisLabel: { color: '#6c7f79' },
      splitLine: { lineStyle: { color: '#eef4f2' } },
    },
    series: [
      {
        type: 'bar',
        data: props.items.map((item) => Number(item.avgScore.toFixed(1))),
        barMaxWidth: 42,
        itemStyle: { color: '#0f766e', borderRadius: [4, 4, 0, 0] },
      },
    ],
  });
}

onMounted(async () => {
  await nextTick();
  if (!chartElement.value) {
    return;
  }
  chart = init(chartElement.value);
  resizeObserver = new ResizeObserver(() => chart?.resize());
  resizeObserver.observe(chartElement.value);
  renderChart();
});

watch(
  () => props.items,
  () => renderChart(),
  { deep: true },
);

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  chart?.dispose();
});
</script>

<template>
  <div class="progress-chart">
    <div
      v-if="items.length"
      ref="chartElement"
      class="progress-chart__canvas"
      aria-label="各知识点平均得分柱状图"
    />
    <AppEmptyState
      v-else
      title="暂无学习数据"
      description="完成一次评分后，这里会按知识点展示平均得分。"
      :icon="BarChart3"
    />
  </div>
</template>

<style scoped>
.progress-chart {
  min-height: 310px;
}

.progress-chart__canvas {
  width: 100%;
  height: 310px;
}
</style>
