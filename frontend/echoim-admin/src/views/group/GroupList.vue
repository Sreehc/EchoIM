<template>
  <div class="page-container">
    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="groupId" label="群组ID" width="100" />
      <el-table-column prop="groupName" label="群组名称" min-width="200" />
      <el-table-column prop="ownerUserId" label="群主ID" width="120" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '已解散' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            type="danger"
            size="small"
            link
            :disabled="row.status !== 1"
            @click="handleDissolve(row)"
          >
            解散
          </el-button>
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
import { fetchGroups, dissolveGroup } from '@/api/groups'
import type { AdminGroupItem } from '@/types/api'

const loading = ref(false)
const tableData = ref<AdminGroupItem[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function loadData() {
  loading.value = true
  try {
    const res = await fetchGroups({ pageNo: pageNo.value, pageSize: pageSize.value })
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

async function handleDissolve(row: AdminGroupItem) {
  await ElMessageBox.confirm(
    `确定要解散群组「${row.groupName}」吗？此操作不可撤销。`,
    '确认解散',
    { type: 'warning' },
  )
  await dissolveGroup(row.groupId)
  ElMessage.success('群组已解散')
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

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
