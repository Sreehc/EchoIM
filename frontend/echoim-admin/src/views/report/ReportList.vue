<template>
  <div class="page-container">
    <div class="page-toolbar">
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="loadData">
        <el-option label="待处理" :value="0" />
        <el-option label="已忽略" :value="1" />
        <el-option label="已警告" :value="2" />
        <el-option label="已禁言" :value="3" />
        <el-option label="已封号" :value="4" />
      </el-select>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="reportId" label="ID" width="70" />
      <el-table-column prop="reporterNickname" label="举报人" width="120" />
      <el-table-column label="举报目标" width="140">
        <template #default="{ row }">
          <el-tag :type="row.targetType === 1 ? 'info' : 'warning'" size="small">
            {{ row.targetType === 1 ? '消息' : '用户' }}
          </el-tag>
          <span v-if="row.targetNickname" style="margin-left: 4px">{{ row.targetNickname }}</span>
          <span v-else style="margin-left: 4px; color: #909399">#{{ row.targetId }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="举报原因" min-width="120" />
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="举报时间" width="170" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button type="info" size="small" link @click="handleAction(row, 1, '忽略')">忽略</el-button>
            <el-button type="warning" size="small" link @click="handleAction(row, 2, '警告')">警告</el-button>
            <el-button type="danger" size="small" link @click="handleAction(row, 3, '禁言')">禁言</el-button>
            <el-button type="danger" size="small" link @click="handleAction(row, 4, '封号')">封号</el-button>
          </template>
          <span v-else style="color: #909399; font-size: 12px">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="page-pagination">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchReports, handleReport, type ReportItem } from '@/api/reports'

const loading = ref(false)
const tableData = ref<ReportItem[]>([])
const statusFilter = ref<number | undefined>(undefined)
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

function statusLabel(status: number): string {
  const map: Record<number, string> = { 0: '待处理', 1: '已忽略', 2: '已警告', 3: '已禁言', 4: '已封号' }
  return map[status] ?? '未知'
}

function statusTagType(status: number): string {
  const map: Record<number, string> = { 0: 'danger', 1: 'info', 2: 'warning', 3: 'danger', 4: 'danger' }
  return map[status] ?? 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchReports({
      status: statusFilter.value,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    tableData.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  pageNo.value = 1
  loadData()
}

async function handleAction(row: ReportItem, action: number, label: string) {
  await ElMessageBox.confirm(`确定要对举报 #${row.reportId} 执行「${label}」操作吗？`, '确认处理')
  await handleReport(row.reportId, action)
  ElMessage.success(`已${label}`)
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

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
