<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { deleteKnowledgeDocument, getKnowledgeDocuments, uploadKnowledgeDocument, type KnowledgeDocument } from '@/api/admin-knowledge-api';

const direction = ref<'frontend' | 'backend'>('backend');
const language = ref('Java');
const selectedFile = ref<File | null>(null);
const documents = ref<KnowledgeDocument[]>([]);
const loading = ref(false); const uploading = ref(false); const error = ref(''); const message = ref('');
const languageOptions = { frontend: ['JavaScript', 'TypeScript', 'Vue', 'React'], backend: ['Java', 'Python', 'Go', 'C#', 'Node.js', 'TypeScript'] } as const;
const currentLanguages = computed(() => languageOptions[direction.value]);
function resetLanguage(): void { language.value = currentLanguages.value[0]; }
function handleFileChange(event: Event): void { selectedFile.value = (event.target as HTMLInputElement).files?.[0] ?? null; }
async function loadDocuments(): Promise<void> { loading.value = true; error.value = ''; try { documents.value = await getKnowledgeDocuments(direction.value, language.value); } catch (e) { error.value = e instanceof Error ? e.message : '资料加载失败'; } finally { loading.value = false; } }
async function upload(): Promise<void> { if (!selectedFile.value) { error.value = '请先选择 .docx 文件'; return; } uploading.value = true; error.value = ''; try { await uploadKnowledgeDocument(direction.value, language.value, selectedFile.value); message.value = '资料上传成功'; selectedFile.value = null; await loadDocuments(); } catch (e) { error.value = e instanceof Error ? e.message : '资料上传失败'; } finally { uploading.value = false; } }
async function remove(document: KnowledgeDocument): Promise<void> { if (!window.confirm(`确定删除「${document.title}」吗？`)) return; try { await deleteKnowledgeDocument(document.id); message.value = '资料已删除'; await loadDocuments(); } catch (e) { error.value = e instanceof Error ? e.message : '资料删除失败'; } }
watch(direction, resetLanguage); watch([direction, language], loadDocuments); onMounted(loadDocuments);
</script>

<template>
  <section class="admin-page"><header class="page-heading"><div><p class="eyebrow">管理员操作</p><h1>知识库资料管理</h1><p>选择资料归属后上传 Word 文档，系统会自动解析正文。</p></div></header>
    <div class="upload-panel"><div class="filters"><el-select v-model="direction" aria-label="选择方向"><el-option label="前端" value="frontend" /><el-option label="后端" value="backend" /></el-select><el-select v-model="language" aria-label="选择语言或技术"><el-option v-for="item in currentLanguages" :key="item" :label="item" :value="item" /></el-select></div><div class="upload-row"><input type="file" accept=".docx" @change="handleFileChange" /><button class="primary-button" type="button" :disabled="uploading" @click="upload">{{ uploading ? '正在解析...' : '上传 Word 文档' }}</button></div><small>仅支持 20MB 以内的 .docx 文件</small></div>
    <div v-if="error" class="error">{{ error }}</div><div v-if="message" class="success">{{ message }}</div>
    <section class="documents"><h2>{{ language }}资料</h2><div v-if="loading" class="empty">正在加载...</div><div v-else-if="!documents.length" class="empty">当前分类暂无资料</div><div v-for="document in documents" v-else :key="document.id" class="document-row"><div><strong>{{ document.title }}</strong><small>{{ document.sourceFileName || document.title }}</small></div><button class="danger-button" type="button" @click="remove(document)">删除资料</button></div></section>
  </section>
</template>

<style scoped>
.admin-page { display: grid; gap: 22px; }.page-heading h1 { margin: 4px 0; font-size: 28px; }.page-heading p { margin: 0; color: var(--ink-muted); }.eyebrow { color: var(--primary) !important; font-size: 12px !important; font-weight: 750; }.upload-panel, .documents { display: grid; gap: 16px; padding: 22px; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-md); }.filters, .upload-row { display: flex; gap: 12px; align-items: center; }.filters :deep(.el-select) { width: 160px; }.upload-row input { flex: 1; }.primary-button, .danger-button { padding: 10px 16px; border: 0; border-radius: var(--radius-sm); color: #fff; cursor: pointer; }.primary-button { background: var(--primary); }.primary-button:disabled { opacity: .6; cursor: wait; }.danger-button { background: var(--danger); }.document-row { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 14px 0; border-top: 1px solid var(--border); }.document-row strong, .document-row small { display: block; }.document-row small, .empty { color: var(--ink-muted); }.error { color: var(--danger); }.success { color: var(--success); } @media (max-width: 700px) { .filters, .upload-row { align-items: stretch; flex-direction: column; }.filters :deep(.el-select), .upload-row input, .upload-row button { width: 100%; } .document-row { align-items: flex-start; flex-direction: column; } }
</style>
