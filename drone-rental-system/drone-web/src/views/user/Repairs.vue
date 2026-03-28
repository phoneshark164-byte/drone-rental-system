<template>
  <div class="user-repairs-page">
    <UserHeader />

    <div class="container py-5">
      <div class="page-header d-flex justify-content-between align-items-center mb-4">
        <div>
          <router-link to="/user/vehicles" class="btn btn-outline-secondary me-2">
            <i class="bi bi-arrow-left me-1"></i>返回列表
          </router-link>
          <router-link to="/" class="btn btn-outline-primary">
            <i class="bi bi-house me-1"></i>返回首页
          </router-link>
        </div>
        <h2 class="page-title mb-0">我的报修</h2>
      </div>

      <!-- 统计 -->
      <div class="stats-row mb-4">
        <div class="stat-card">
          <div class="stat-icon total">
            <i class="bi bi-list-check"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ repairStats.total }}</div>
            <div class="stat-label">全部报修</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon pending">
            <i class="bi bi-clock"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ repairStats.pending }}</div>
            <div class="stat-label">待处理</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon processing">
            <i class="bi bi-tools"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ repairStats.processing }}</div>
            <div class="stat-label">处理中</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon completed">
            <i class="bi bi-check-circle"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ repairStats.completed }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
      </div>

      <!-- 报修列表 -->
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="mt-3">加载中...</p>
      </div>

      <div v-else-if="repairs.length === 0" class="empty-state text-center py-5">
        <i class="bi bi-inbox display-4 text-muted"></i>
        <p class="text-muted mt-3">暂无报修记录</p>
        <router-link to="/user/vehicles" class="btn btn-primary">
          <i class="bi bi-plus-circle me-1"></i>浏览无人机
        </router-link>
      </div>

      <div v-else class="repairs-list">
        <div v-for="repair in repairs" :key="repair.id" class="repair-card">
          <div class="repair-header">
            <div class="repair-info">
              <span class="repair-no">{{ repair.repairNo }}</span>
              <span class="status-badge" :class="'status-' + repair.status">
                {{ getStatusText(repair.status) }}
              </span>
            </div>
            <span class="repair-time">{{ formatDateTime(repair.createTime) }}</span>
          </div>

          <div class="repair-body">
            <div class="drone-info">
              <img :src="repair.imageUrl || '/img/train/0001.jpg'" alt="无人机" class="drone-thumb" />
              <div>
                <div class="drone-no">{{ repair.droneNo }}</div>
                <div class="drone-model">{{ repair.brand }} {{ repair.model }}</div>
              </div>
            </div>

            <div class="fault-info">
              <div class="fault-type">
                <i class="bi bi-exclamation-triangle text-danger"></i>
                {{ repair.faultType }}
              </div>
              <div class="fault-desc">{{ repair.faultDescription }}</div>
            </div>

            <div v-if="repair.status === 1 || repair.status === 2" class="repair-progress">
              <div class="progress-header">
                <span>处理进度</span>
                <span class="handler-info" v-if="repair.handlerName">
                  <i class="bi bi-person-fill"></i> {{ repair.handlerName }}
                </span>
              </div>
              <div class="progress-track">
                <div class="progress-bar" :class="'progress-' + repair.status">
                  <div class="progress-fill" :style="{ width: getProgressWidth(repair.status) }"></div>
                </div>
              </div>
              <div class="progress-text" v-if="repair.status === 2">
                <i class="bi bi-check-circle text-success"></i>
                维修完成
              </div>
            </div>

            <div v-if="repair.handleResult" class="handle-result">
              <div class="result-label">处理结果：</div>
              <div class="result-text">{{ repair.handleResult }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import UserHeader from '@/components/UserHeader.vue'
import { getMyRepairs } from '@/api/user'

const loading = ref(false)
const repairs = ref([])

const repairStats = computed(() => ({
  total: repairs.value.length,
  pending: repairs.value.filter(r => r.status === 0).length,
  processing: repairs.value.filter(r => r.status === 1).length,
  completed: repairs.value.filter(r => r.status === 2).length
}))

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const getStatusText = (status) => {
  const statusMap = {
    0: '待处理',
    1: '处理中',
    2: '已完成',
    3: '已关闭'
  }
  return statusMap[status] || '未知'
}

const getProgressWidth = (status) => {
  if (status === 1) return '50%'
  if (status === 2) return '100%'
  return '0%'
}

const loadRepairs = async () => {
  loading.value = true
  try {
    const res = await getMyRepairs()
    if (res.code === 200 && res.data) {
      repairs.value = res.data
    }
  } catch (error) {
    console.error('加载报修列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRepairs()
})
</script>

<style scoped>
.page-title {
  font-weight: 700;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-icon.total { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.pending { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-icon.processing { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.stat-icon.completed { background: linear-gradient(135deg, #43e97b, #38f9d7); }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  color: #64748b;
  font-size: 13px;
}

.repairs-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.repair-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.repair-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 16px;
}

.repair-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.repair-no {
  font-weight: 600;
  color: #1e293b;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-0 { background: #fef3c7; color: #d97706; }
.status-1 { background: #dbeafe; color: #2563eb; }
.status-2 { background: #d1fae5; color: #059669; }
.status-3 { background: #e5e7eb; color: #6b7280; }

.repair-time {
  color: #94a3b8;
  font-size: 13px;
}

.repair-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drone-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.drone-thumb {
  width: 80px;
  height: 80px;
  border-radius: 10px;
  object-fit: cover;
}

.drone-no {
  font-weight: 600;
  color: #1e293b;
}

.drone-model {
  color: #64748b;
  font-size: 13px;
}

.fault-info {
  background: #fef2f2;
  border-radius: 12px;
  padding: 16px;
  border-left: 4px solid #ef4444;
}

.fault-type {
  font-weight: 600;
  color: #ef4444;
  margin-bottom: 8px;
}

.fault-desc {
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
}

.repair-progress {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.handler-info {
  color: #3b82f6;
}

.progress-track {
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.progress-text {
  margin-top: 12px;
  font-size: 13px;
  color: #059669;
  font-weight: 500;
}

.handle-result {
  background: #f0fdf4;
  border-radius: 12px;
  padding: 16px;
}

.result-label {
  font-weight: 600;
  color: #059669;
  margin-bottom: 8px;
}

.result-text {
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
