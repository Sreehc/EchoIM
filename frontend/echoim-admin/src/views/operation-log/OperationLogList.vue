<template>
  <div class="page-container">
    <div class="page-toolbar">
      <el-input
        v-model="moduleFilter"
        placeholder="模块名筛选"
        clearable
        style="width: 180px"
        @keyup.enter="loadData"
        @clear="loadData"
      />
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="logId" label="ID" width="70" />
      <el-table-column prop="adminNickname" label="操作人" width="120" />
      <el-table-column prop="moduleName" label="模块" width="120" />
      <el-table-column prop="actionName" label="操作" width="140" />
      <el-table-column prop="targetType" label="目标类型" width="100" />
      <el-table-column prop="targetId" label="目标ID" width="100" />
      <el-table-column prop="requestIp" label="IP" width="140" />
      <el-table-column prop="createdAt" label="操作时间" width="170" />
      <el-table-column prop="contentJson" label="详情" min-width="200" show-overflow-tooltip />
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
import { fetchOperationLogs, type OperationLogItem } from '@/api/operation-logs'

const loading = ref(false)
const tableData = ref<OperationLogItem[]>([])
const moduleFilter = ref('')
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function loadData() {
  loading.value = true
  try {
    const res = await fetchOperationLogs({
      moduleName: moduleFilter.value || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    tableData.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSizeChange() { pageNo.value = 1; loadData() }

onMounted(loadData)
</script>

<style scoped>
.page-container { background: #fff; border-radius: 8px; padding: 20px; }
.page-toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.page-pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
