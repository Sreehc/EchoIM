<template>
  <div class="page-container">
    <div class="page-toolbar">
      <el-button type="primary" @click="openBanDialog">封禁用户</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="banId" label="ID" width="70" />
      <el-table-column prop="nickname" label="用户" width="140">
        <template #default="{ row }">
          {{ row.nickname || row.userNo }}
        </template>
      </el-table-column>
      <el-table-column label="封禁类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.banType === 1 ? 'warning' : 'danger'" size="small">
            {{ row.banType === 1 ? '临时' : '永久' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="封禁原因" min-width="200" show-overflow-tooltip />
      <el-table-column label="时长" width="100" align="center">
        <template #default="{ row }">
          {{ row.banType === 1 ? (row.banMinutes + '分钟') : '永久' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
            {{ row.status === 1 ? '生效中' : '已解除' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="封禁时间" width="170" />
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 1"
            type="success"
            size="small"
            link
            @click="handleUnban(row)"
          >解除封禁</el-button>
          <span v-else style="color: #909399; font-size: 12px">已解除</span>
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

    <el-dialog v-model="banDialogVisible" title="封禁用户" width="460px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="用户ID">
          <el-input-number v-model="banForm.userId" :min="1" placeholder="用户ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="封禁原因">
          <el-input v-model="banForm.reason" type="textarea" :rows="2" placeholder="封禁原因" maxlength="500" />
        </el-form-item>
        <el-form-item label="封禁时长">
          <el-radio-group v-model="banDuration">
            <el-radio :value="60">1小时</el-radio>
            <el-radio :value="1440">1天</el-radio>
            <el-radio :value="10080">7天</el-radio>
            <el-radio :value="0">永久</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="banning" @click="handleBan">确认封禁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchBans, banUser, unbanUser, type BanItem } from '@/api/bans'

const loading = ref(false)
const tableData = ref<BanItem[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const banDialogVisible = ref(false)
const banning = ref(false)
const banDuration = ref(1440)
const banForm = reactive({ userId: 0, reason: '' })

function openBanDialog() {
  banForm.userId = 0
  banForm.reason = ''
  banDuration.value = 1440
  banDialogVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchBans({ pageNo: pageNo.value, pageSize: pageSize.value })
    tableData.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSizeChange() { pageNo.value = 1; loadData() }

async function handleBan() {
  if (!banForm.userId || !banForm.reason.trim()) {
    ElMessage.warning('请填写用户ID和封禁原因')
    return
  }
  banning.value = true
  try {
    await banUser({
      userId: banForm.userId,
      reason: banForm.reason,
      banMinutes: banDuration.value > 0 ? banDuration.value : undefined,
    })
    ElMessage.success('已封禁')
    banDialogVisible.value = false
    loadData()
  } finally {
    banning.value = false
  }
}

async function handleUnban(row: BanItem) {
  await ElMessageBox.confirm(`确定要解除用户「${row.nickname || row.userNo}」的封禁吗？`, '确认解除')
  await unbanUser(row.banId)
  ElMessage.success('已解除封禁')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page-container { background: #fff; border-radius: 8px; padding: 20px; }
.page-toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.page-pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
