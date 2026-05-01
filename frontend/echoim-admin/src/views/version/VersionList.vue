<template>
  <div class="page-container">
    <div class="page-toolbar">
      <el-button type="primary" :icon="Plus" @click="openDialog()">发布新版本</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="versionId" label="ID" width="80" />
      <el-table-column prop="versionCode" label="版本号" width="120" />
      <el-table-column prop="versionName" label="版本名称" min-width="160" />
      <el-table-column prop="platform" label="平台" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.platform }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="强更" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.forceUpdate === 1 ? 'danger' : 'info'" size="small">
            {{ row.forceUpdate === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="灰度比例" width="100" align="center">
        <template #default="{ row }">
          {{ row.grayPercent ?? 100 }}%
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="publishStatusType(row.publishStatus)" size="small">
            {{ publishStatusLabel(row.publishStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="openDialog(row)">
            编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="editingItem ? '编辑版本' : '发布新版本'"
      width="540px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="版本号" prop="versionCode">
          <el-input v-model="form.versionCode" placeholder="如 v0.2.0" />
        </el-form-item>
        <el-form-item label="版本名称" prop="versionName">
          <el-input v-model="form.versionName" placeholder="如 EchoIM Beta" />
        </el-form-item>
        <el-form-item label="平台" prop="platform">
          <el-select v-model="form.platform" placeholder="选择平台" style="width: 100%">
            <el-option label="web" value="web" />
            <el-option label="android" value="android" />
            <el-option label="ios" value="ios" />
          </el-select>
        </el-form-item>
        <el-form-item label="更新说明">
          <el-input v-model="form.releaseNote" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
        <el-form-item label="强制更新" prop="forceUpdate">
          <el-switch v-model="form.forceUpdate" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="灰度比例" prop="grayPercent">
          <el-slider v-model="form.grayPercent" :min="0" :max="100" :step="5" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { fetchVersions, createVersion, updateVersion } from '@/api/versions'
import type { VersionItem } from '@/types/api'

const loading = ref(false)
const tableData = ref<VersionItem[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingItem = ref<VersionItem | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  versionCode: '',
  versionName: '',
  platform: 'web',
  releaseNote: '',
  forceUpdate: 0 as number,
  grayPercent: 100,
})

const rules: FormRules = {
  versionCode: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  versionName: [{ required: true, message: '请输入版本名称', trigger: 'blur' }],
  platform: [{ required: true, message: '请选择平台', trigger: 'change' }],
  forceUpdate: [{ required: true, message: '请选择是否强更', trigger: 'change' }],
  grayPercent: [{ required: true, message: '请设置灰度比例', trigger: 'change' }],
}

function publishStatusLabel(status: number) {
  return { 0: '未发布', 1: '已发布', 2: '已下线' }[status] || '未知'
}

function publishStatusType(status: number) {
  return ({ 0: 'info', 1: 'success', 2: 'danger' }[status] || 'info') as 'info' | 'success' | 'danger'
}

async function loadData() {
  loading.value = true
  try {
    tableData.value = await fetchVersions()
  } finally {
    loading.value = false
  }
}

function openDialog(item?: VersionItem) {
  if (item) {
    editingItem.value = item
    form.versionCode = item.versionCode
    form.versionName = item.versionName
    form.platform = item.platform
    form.releaseNote = item.releaseNote || ''
    form.forceUpdate = item.forceUpdate ?? 0
    form.grayPercent = item.grayPercent ?? 100
  } else {
    editingItem.value = null
    form.platform = 'web'
    form.forceUpdate = 0
    form.grayPercent = 100
  }
  dialogVisible.value = true
}

function resetForm() {
  form.versionCode = ''
  form.versionName = ''
  form.platform = 'web'
  form.releaseNote = ''
  form.forceUpdate = 0
  form.grayPercent = 100
  editingItem.value = null
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      versionCode: form.versionCode,
      versionName: form.versionName,
      platform: form.platform,
      releaseNote: form.releaseNote || undefined,
      forceUpdate: form.forceUpdate,
      grayPercent: form.grayPercent,
    }
    if (editingItem.value) {
      await updateVersion(editingItem.value.versionId, payload)
      ElMessage.success('更新成功')
    } else {
      await createVersion(payload)
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
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
