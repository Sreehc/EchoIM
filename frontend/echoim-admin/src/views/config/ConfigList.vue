<template>
  <div class="page-container">
    <div class="page-toolbar">
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增配置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="configId" label="ID" width="80" />
      <el-table-column prop="configKey" label="配置键" min-width="200" />
      <el-table-column prop="configValue" label="配置值" min-width="200" />
      <el-table-column prop="configName" label="配置名称" min-width="160" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
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
      :title="editingItem ? '编辑配置' : '新增配置'"
      width="500px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如 file.max-size-mb" :disabled="!!editingItem" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="配置值" />
        </el-form-item>
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="可读名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
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
import { fetchConfigs, createConfig, updateConfig } from '@/api/configs'
import type { ConfigItem } from '@/types/api'

const loading = ref(false)
const tableData = ref<ConfigItem[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingItem = ref<ConfigItem | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  configKey: '',
  configValue: '',
  configName: '',
  remark: '',
})

const rules: FormRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    tableData.value = await fetchConfigs()
  } finally {
    loading.value = false
  }
}

function openDialog(item?: ConfigItem) {
  if (item) {
    editingItem.value = item
    form.configKey = item.configKey
    form.configValue = item.configValue
    form.configName = item.configName
    form.remark = item.remark || ''
  } else {
    editingItem.value = null
  }
  dialogVisible.value = true
}

function resetForm() {
  form.configKey = ''
  form.configValue = ''
  form.configName = ''
  form.remark = ''
  editingItem.value = null
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = { configKey: form.configKey, configValue: form.configValue, configName: form.configName, remark: form.remark || undefined }
    if (editingItem.value) {
      await updateConfig(editingItem.value.configId, payload)
      ElMessage.success('更新成功')
    } else {
      await createConfig(payload)
      ElMessage.success('新增成功')
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
