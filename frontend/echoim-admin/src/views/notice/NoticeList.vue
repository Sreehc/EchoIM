<template>
  <div class="page-container">
    <div class="page-toolbar">
      <el-button type="primary" @click="openCreateDialog">发布公告</el-button>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px" @change="loadData">
        <el-option label="已发布" :value="1" />
        <el-option label="已撤回" :value="2" />
      </el-select>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.noticeType === 1 ? 'primary' : 'warning'" size="small">
            {{ row.noticeType === 1 ? '全员' : '指定用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已发布' : '已撤回' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="发布时间" width="170" />
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 1"
            type="warning"
            size="small"
            link
            @click="handleWithdraw(row)"
          >撤回</el-button>
          <span v-else style="color: #909399; font-size: 12px">已撤回</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="page-pagination">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="createDialogVisible" title="发布公告" width="520px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="createForm.title" placeholder="公告标题" maxlength="200" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="createForm.content" type="textarea" :rows="5" placeholder="公告内容" maxlength="5000" show-word-limit />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="createForm.noticeType">
            <el-radio :value="1">全员公告</el-radio>
            <el-radio :value="2">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createForm.noticeType === 2" label="目标用户">
          <el-input
            v-model="createForm.targetUserIds"
            type="textarea"
            :rows="3"
            placeholder="输入用户 ID，支持逗号或换行分隔"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchNotices, createNotice, withdrawNotice, type NoticeItem } from '@/api/notices'

const loading = ref(false)
const tableData = ref<NoticeItem[]>([])
const statusFilter = ref<number | undefined>(undefined)
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const createDialogVisible = ref(false)
const creating = ref(false)
const createForm = reactive({ title: '', content: '', noticeType: 1, targetUserIds: '' })

function openCreateDialog() {
  createForm.title = ''
  createForm.content = ''
  createForm.noticeType = 1
  createForm.targetUserIds = ''
  createDialogVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchNotices({ status: statusFilter.value, pageNo: pageNo.value, pageSize: pageSize.value })
    tableData.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSizeChange() { pageNo.value = 1; loadData() }

async function handleCreate() {
  if (!createForm.title.trim() || !createForm.content.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  if (createForm.noticeType === 2 && !createForm.targetUserIds.trim()) {
    ElMessage.warning('请填写目标用户 ID')
    return
  }
  creating.value = true
  try {
    await createNotice({
      title: createForm.title,
      content: createForm.content,
      noticeType: createForm.noticeType,
      targetUserIds: createForm.noticeType === 2 ? createForm.targetUserIds : undefined,
    })
    ElMessage.success('公告已发布')
    createDialogVisible.value = false
    loadData()
  } finally {
    creating.value = false
  }
}

async function handleWithdraw(row: NoticeItem) {
  await ElMessageBox.confirm(`确定要撤回公告「${row.title}」吗？`, '确认撤回')
  await withdrawNotice(row.id)
  ElMessage.success('已撤回')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page-container { background: #fff; border-radius: 8px; padding: 20px; }
.page-toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.page-pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
