<template>
  <div class="page-container">
    <div class="page-toolbar">
      <el-input
        v-model="newWord"
        placeholder="输入敏感词"
        clearable
        style="width: 240px"
        @keyup.enter="handleAdd"
      />
      <el-select v-model="newLevel" style="width: 100px">
        <el-option label="普通" :value="1" />
        <el-option label="严重" :value="2" />
      </el-select>
      <el-select v-model="newAction" style="width: 100px">
        <el-option label="标记" :value="1" />
        <el-option label="拦截" :value="2" />
      </el-select>
      <el-button type="primary" @click="handleAdd" :disabled="!newWord.trim()">添加</el-button>
      <el-button @click="handleReload" :loading="reloading">刷新缓存</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="word" label="敏感词" min-width="200" />
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button type="danger" size="small" link @click="handleRemove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchSensitiveWords, addSensitiveWord, removeSensitiveWord, reloadSensitiveWordCache } from '@/api/sensitive-words'

interface WordItem {
  word: string
  id?: number
}

const loading = ref(false)
const reloading = ref(false)
const tableData = ref<WordItem[]>([])
const newWord = ref('')
const newLevel = ref(1)
const newAction = ref(1)

async function loadData() {
  loading.value = true
  try {
    const words = await fetchSensitiveWords()
    tableData.value = words.map(w => ({ word: w }))
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  const word = newWord.value.trim()
  if (!word) return
  await addSensitiveWord(word, 'default', newLevel.value, newAction.value)
  ElMessage.success('已添加')
  newWord.value = ''
  loadData()
}

async function handleRemove(row: WordItem) {
  await ElMessageBox.confirm(`确定要删除敏感词「${row.word}」吗？`, '确认删除')
  if (row.id) {
    await removeSensitiveWord(row.id)
  }
  ElMessage.success('已删除')
  loadData()
}

async function handleReload() {
  reloading.value = true
  try {
    await reloadSensitiveWordCache()
    ElMessage.success('缓存已刷新')
  } finally {
    reloading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.page-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
