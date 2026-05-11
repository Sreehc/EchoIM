<template>
  <div class="dashboard">
    <el-row :gutter="16" class="dashboard__cards">
      <el-col :span="6">
        <el-card shadow="hover" class="dashboard__card">
          <div class="card-value">{{ overview.totalUsers }}</div>
          <div class="card-label">总用户数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="dashboard__card">
          <div class="card-value">{{ overview.newUsersToday }}</div>
          <div class="card-label">今日新增</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="dashboard__card">
          <div class="card-value">{{ overview.totalMessages }}</div>
          <div class="card-label">总消息数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="dashboard__card">
          <div class="card-value highlight">{{ overview.onlineUsers }}</div>
          <div class="card-label">在线用户</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="dashboard__charts">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="chart-header">
              <span>消息趋势</span>
              <el-radio-group v-model="trendDays" size="small" @change="loadTrends">
                <el-radio-button :value="7">近7天</el-radio-button>
                <el-radio-button :value="30">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-area">
            <div v-if="messageTrend.length" class="bar-chart">
              <div class="bar-chart__y-axis">
                <span v-for="tick in yTicks" :key="tick">{{ tick }}</span>
              </div>
              <div class="bar-chart__body">
                <div
                  v-for="item in messageTrend"
                  :key="item.date"
                  class="bar-chart__col"
                >
                  <div
                    class="bar-chart__bar bar-chart__bar--message"
                    :style="{ height: barHeight(item.count) + '%' }"
                    :title="`${item.date}: ${item.count}`"
                  >
                    <span v-if="item.count > 0" class="bar-chart__bar-label">{{ item.count }}</span>
                  </div>
                  <div class="bar-chart__bar bar-chart__bar--user"
                    :style="{ height: barHeightUser(findUserCount(item.date)) + '%' }"
                    :title="`新增用户: ${findUserCount(item.date)}`"
                  ></div>
                  <span class="bar-chart__label">{{ formatShortDate(item.date) }}</span>
                </div>
              </div>
              <div class="bar-chart__legend">
                <span class="legend-item"><i class="legend-dot legend-dot--message"></i>消息数</span>
                <span class="legend-item"><i class="legend-dot legend-dot--user"></i>新增用户</span>
              </div>
            </div>
            <div v-else class="chart-empty">暂无数据</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>消息类型分布</template>
          <div class="chart-area">
            <div v-if="messageTypes.length" class="pie-chart">
              <div
                v-for="(item, index) in messageTypes"
                :key="item.name"
                class="pie-chart__item"
              >
                <div class="pie-chart__bar-wrap">
                  <div
                    class="pie-chart__bar"
                    :style="{
                      width: pieBarWidth(item.value) + '%',
                      background: chartColors[index % chartColors.length]
                    }"
                  ></div>
                </div>
                <span class="pie-chart__label">{{ item.name }}</span>
                <span class="pie-chart__value">{{ item.value }}</span>
                <span class="pie-chart__percent">{{ piePercent(item.value) }}%</span>
              </div>
            </div>
            <div v-else class="chart-empty">暂无数据</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="dashboard__bottom">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>实时在线统计</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="当前在线">{{ onlineStats.currentOnline }}</el-descriptions-item>
            <el-descriptions-item label="消息接收总量">{{ onlineStats.messagesReceived }}</el-descriptions-item>
            <el-descriptions-item label="消息发送总量">{{ onlineStats.messagesSent }}</el-descriptions-item>
            <el-descriptions-item label="连接建立总数">{{ onlineStats.connectionsOpened }}</el-descriptions-item>
            <el-descriptions-item label="连接关闭总数">{{ onlineStats.connectionsClosed }}</el-descriptions-item>
            <el-descriptions-item label="鉴权失败数">{{ onlineStats.authFailures }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>今日概览</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="今日消息量">{{ overview.messagesToday }}</el-descriptions-item>
            <el-descriptions-item label="今日新增用户">{{ overview.newUsersToday }}</el-descriptions-item>
            <el-descriptions-item label="在线峰值">{{ onlineStats.connectionsOpened }}</el-descriptions-item>
            <el-descriptions-item label="消息发送总量">{{ onlineStats.messagesSent }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  fetchDashboardOverview,
  fetchMessageTrend,
  fetchUserTrend,
  fetchMessageTypeBreakdown,
  fetchOnlineStats,
  type DashboardOverview,
  type TrendItem,
  type MessageTypeItem,
  type OnlineStats,
} from '@/api/dashboard'

const overview = ref<DashboardOverview>({
  totalUsers: 0,
  newUsersToday: 0,
  totalMessages: 0,
  messagesToday: 0,
  onlineUsers: 0,
})
const messageTrend = ref<TrendItem[]>([])
const userTrend = ref<TrendItem[]>([])
const messageTypes = ref<MessageTypeItem[]>([])
const onlineStats = ref<OnlineStats>({
  currentOnline: 0,
  messagesReceived: 0,
  messagesSent: 0,
  connectionsOpened: 0,
  connectionsClosed: 0,
  authFailures: 0,
})
const trendDays = ref(7)
const chartColors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#36cfc9']

const maxMessageCount = computed(() => Math.max(1, ...messageTrend.value.map(i => i.count)))
const maxUserCount = computed(() => Math.max(1, ...userTrend.value.map(i => i.count)))
const totalTypeValue = computed(() => messageTypes.value.reduce((s, i) => s + i.value, 0))

const yTicks = computed(() => {
  const max = maxMessageCount.value
  const step = Math.ceil(max / 4)
  return [0, step, step * 2, step * 3, step * 4].filter(v => v <= max * 1.2)
})

function barHeight(count: number) {
  return (count / maxMessageCount.value) * 100
}

function barHeightUser(count: number) {
  return (count / maxUserCount.value) * 100
}

function findUserCount(date: string): number {
  return userTrend.value.find(u => u.date === date)?.count ?? 0
}

function pieBarWidth(value: number) {
  return totalTypeValue.value > 0 ? (value / totalTypeValue.value) * 100 : 0
}

function piePercent(value: number) {
  return totalTypeValue.value > 0 ? ((value / totalTypeValue.value) * 100).toFixed(1) : '0'
}

function formatShortDate(date: string): string {
  const d = new Date(date)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

async function loadTrends() {
  const [msgTrend, usrTrend] = await Promise.all([
    fetchMessageTrend(trendDays.value),
    fetchUserTrend(trendDays.value),
  ])
  messageTrend.value = msgTrend
  userTrend.value = usrTrend
}

async function loadData() {
  const [ov, mt, , os] = await Promise.all([
    fetchDashboardOverview(),
    fetchMessageTypeBreakdown(),
    loadTrends(),
    fetchOnlineStats(),
  ])
  overview.value = ov
  messageTypes.value = mt
  onlineStats.value = os
}

onMounted(loadData)
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard__cards {
  margin-bottom: 0;
}

.dashboard__card {
  text-align: center;
}

.card-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.card-value.highlight {
  color: #409eff;
}

.card-label {
  margin-top: 8px;
  font-size: 14px;
  color: #909399;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-area {
  min-height: 280px;
}

.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 280px;
  color: #c0c4cc;
}

.bar-chart {
  display: flex;
  gap: 8px;
  height: 260px;
  position: relative;
}

.bar-chart__y-axis {
  display: flex;
  flex-direction: column-reverse;
  justify-content: space-between;
  width: 40px;
  font-size: 11px;
  color: #909399;
  text-align: right;
  padding-bottom: 24px;
}

.bar-chart__body {
  flex: 1;
  display: flex;
  align-items: flex-end;
  gap: 4px;
  padding-bottom: 24px;
  border-left: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.bar-chart__col {
  flex: 1;
  display: flex;
  gap: 2px;
  align-items: flex-end;
  justify-content: center;
  position: relative;
  height: 100%;
}

.bar-chart__bar {
  width: 40%;
  min-height: 2px;
  border-radius: 3px 3px 0 0;
  position: absolute;
  bottom: 24px;
  transition: height 0.4s ease;
}

.bar-chart__bar--message {
  left: 10%;
  background: #409eff;
}

.bar-chart__bar--user {
  right: 10%;
  background: #67c23a;
  opacity: 0.7;
}

.bar-chart__bar-label {
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: #606266;
  white-space: nowrap;
}

.bar-chart__label {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: #909399;
  white-space: nowrap;
}

.bar-chart__legend {
  position: absolute;
  bottom: -4px;
  right: 0;
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #606266;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.legend-dot--message { background: #409eff; }
.legend-dot--user { background: #67c23a; }

.pie-chart {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 0;
}

.pie-chart__item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pie-chart__bar-wrap {
  flex: 1;
  height: 20px;
  background: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
}

.pie-chart__bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
  min-width: 2px;
}

.pie-chart__label {
  width: 40px;
  font-size: 13px;
  color: #303133;
}

.pie-chart__value {
  width: 50px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.pie-chart__percent {
  width: 50px;
  text-align: right;
  font-size: 12px;
  color: #909399;
}

.dashboard__charts .el-card {
  height: 100%;
}

.dashboard__bottom .el-card {
  height: 100%;
}
</style>
