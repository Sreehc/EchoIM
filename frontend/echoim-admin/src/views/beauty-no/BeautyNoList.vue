<template>
  <div class="page-container">
    <div class="page-toolbar">
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增靓号</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="beautyNoId" label="ID" width="80" />
      <el-table-column prop="beautyNo" label="靓号" min-width="160" />
      <el-table-column label="等级" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="levelTagType(row.levelType)" size="small">
            {{ levelLabel(row.levelType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '可用' : '已分配' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" link @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      title="新增靓号"
      width="440px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="靓号" prop="beautyNo">
          <el-input v-model="form.beautyNo" placeholder="如 88888" maxlength="20" />
        </el-form-item>
        <el-form-item label="等级" prop="levelType">
          <el-select v-model="form.levelType" placeholder="选择等级" style="width: 100%">
            <el-option label="普通" :value="1" />
            <el-option label="高级" :value="2" />
            <el-option label="至尊" :value="3" />
          </el-select>
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { fetchBeautyNos, createBeautyNo, deleteBeautyNo } from '@/api/beauty-nos'
import type { BeautyNoItem } from '@/types/api'

const loading = ref(false)
const tableData = ref<BeautyNoItem[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  beautyNo: '',
  levelType: 1 as number,
  remark: '',
})

const rules: FormRules = {
  beautyNo: [{ required: true, message: '请输入靓号', trigger: 'blur' }],
  levelType: [{ required: true, message: '请选择等级', trigger: 'change' }],
}

function levelLabel(level: number) {
  return { 1: '普通', 2: '高级', 3: '至尊' }[level] || '未知'
}

function levelTagType(level: number) {
  return ({ 1: 'info', 2: 'warning', 3: 'danger' }[level] || 'info') as 'info' | 'warning' | 'danger'
}

async function loadData() {
  loading.value = true
  try {
    tableData.value = await fetchBeautyNos()
  } finally {
    loading.value = false
  }
}

function openDialog() {
  dialogVisible.value = true
}

function resetForm() {
  form.beautyNo = ''
  form.levelType = 1
  form.remark = ''
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await createBeautyNo({ beautyNo: form.beautyNo, levelType: form.levelType, remark: form.remark || undefined })
    ElMessage.success('新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: BeautyNoItem) {
  await ElMessageBox.confirm(`确定要删除靓号「${row.beautyNo}」吗？`, '确认删除', { type: 'warning' })
  await deleteBeautyNo(row.beautyNoId)
  ElMessage.success('已删除')
  loadData()
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
