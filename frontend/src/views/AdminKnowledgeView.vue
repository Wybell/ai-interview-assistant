<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { createAdminKnowledgeTopic, deleteAdminKnowledgeTopic, getAdminKnowledgeTopics, updateAdminKnowledgeTopic, type AdminKnowledgeTopic } from '@/api/admin-knowledge-api';

const topics = ref<AdminKnowledgeTopic[]>([]);
const selected = ref<AdminKnowledgeTopic | null>(null);
const editor = ref('');
const loading = ref(false);
const message = ref('');
const error = ref('');

async function load(): Promise<void> {
  loading.value = true;
  try { topics.value = await getAdminKnowledgeTopics(); } catch (requestError) { error.value = requestError instanceof Error ? requestError.message : '加载失败'; }
  finally { loading.value = false; }
}
function selectTopic(topic: AdminKnowledgeTopic): void { selected.value = topic; editor.value = JSON.stringify(topic, null, 2); message.value = ''; }
function create(): void {
  selected.value = null;
  editor.value = JSON.stringify({ direction: 'backend', language: 'Java', category: '基础', title: '新专题', summary: '', keyPoints: [''], questions: [{ question: '', answer: '', difficulty: '中级' }], published: true }, null, 2);
}
async function save(): Promise<void> {
  try {
    const data = JSON.parse(editor.value) as AdminKnowledgeTopic;
    if (selected.value?.id) await updateAdminKnowledgeTopic(selected.value.id, data); else await createAdminKnowledgeTopic(data);
    message.value = '保存成功'; await load();
  } catch (requestError) { error.value = requestError instanceof Error ? requestError.message : '保存失败'; }
}
async function remove(): Promise<void> {
  if (!selected.value?.id || !window.confirm('确定删除这个专题吗？')) return;
  await deleteAdminKnowledgeTopic(selected.value.id); selected.value = null; editor.value = ''; await load();
}
onMounted(load);
</script>

<template>
  <section class="admin-page">
    <header class="page-heading"><div><p class="eyebrow">内容维护</p><h1>知识库管理</h1><p>维护专题、核心要点和面试问题。</p></div><button class="primary-button" type="button" @click="create">新增专题</button></header>
    <div v-if="error" class="error">{{ error }}</div><div v-if="message" class="success">{{ message }}</div>
    <div class="admin-layout">
      <aside class="topic-list"><button v-for="topic in topics" :key="topic.id" type="button" class="topic-item" @click="selectTopic(topic)"><strong>{{ topic.title }}</strong><small>{{ topic.direction }} · {{ topic.language }}</small></button><p v-if="!topics.length && !loading">暂无专题</p></aside>
      <article class="editor"><h2>{{ selected ? '编辑专题' : '新增专题' }}</h2><p>使用 JSON 编辑，字段结构与接口一致。</p><textarea v-model="editor" aria-label="知识专题内容" spellcheck="false" /><div class="actions"><button class="primary-button" type="button" @click="save">保存</button><button v-if="selected" class="danger-button" type="button" @click="remove">删除</button></div></article>
    </div>
  </section>
</template>

<style scoped>
.admin-page { display: grid; gap: 24px; }.page-heading { display: flex; justify-content: space-between; align-items: end; gap: 20px; }.page-heading h1 { margin: 4px 0; font-size: 28px; }.page-heading p { margin: 0; color: var(--ink-muted); }.eyebrow { color: var(--primary) !important; font-size: 12px !important; font-weight: 750; }.admin-layout { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 20px; }.topic-list, .editor { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-md); padding: 16px; }.topic-item { display: block; width: 100%; padding: 12px; border: 0; border-radius: var(--radius-sm); background: transparent; text-align: left; cursor: pointer; }.topic-item:hover { background: var(--primary-subtle); }.topic-item strong, .topic-item small { display: block; }.topic-item small { margin-top: 4px; color: var(--ink-muted); }.editor textarea { width: 100%; min-height: 560px; padding: 14px; border: 1px solid var(--border); border-radius: var(--radius-sm); font: 13px/1.6 monospace; resize: vertical; }.editor h2 { margin: 0 0 6px; }.editor p { color: var(--ink-muted); }.actions { display: flex; gap: 10px; margin-top: 14px; }.primary-button, .danger-button { padding: 10px 16px; border: 0; border-radius: var(--radius-sm); color: #fff; cursor: pointer; }.primary-button { background: var(--primary); }.danger-button { background: var(--danger); }.error { color: var(--danger); }.success { color: var(--success); } @media (max-width: 760px) { .page-heading { align-items: start; flex-direction: column; }.admin-layout { grid-template-columns: 1fr; } }
</style>
