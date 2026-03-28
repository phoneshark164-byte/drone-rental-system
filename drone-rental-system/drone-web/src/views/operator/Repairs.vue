<template>
  <div class="operator-repairs">
    <div class="page-header">
      <h3>故障报修</h3>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-item">
        <span class="stat-value">{{ repairStats.total }}</span>
        <span class="stat-label">总数</span>
      </div>
      <div class="stat-item warning">
        <span class="stat-value">{{ repairStats.pending }}</span>
        <span class="stat-label">待处理</span>
      </div>
      <div class="stat-item primary">
        <span class="stat-value">{{ repairStats.processing }}</span>
        <span class="stat-label">处理中</span>
      </div>
      <div class="stat-item success">
        <span class="stat-value">{{ repairStats.completed }}</span>
        <span class="stat-label">已完成</span>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <div class="row g-3">
        <div class="col-md-3">
          <input
            v-model="searchKeyword"
            type="text"
            class="form-control"
            placeholder="搜索报修单号、无人机编号"
          />
        </div>
        <div class="col-md-2">
          <select v-model="selectedStatus" class="form-select">
            <option value="">全部状态</option>
            <option value="0">待处理</option>
            <option value="1">处理中</option>
            <option value="2">已完成</option>
            <option value="3">已关闭</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-primary w-100" @click="loadRepairs">
            <i class="bi bi-search me-1"></i>搜索
          </button>
        </div>
      </div>
    </div>

    <!-- 报修列表 -->
    <div class="repairs-grid" v-if="!loading">
      <div v-for="repair in filteredRepairs" :key="repair.id" class="repair-card">
        <div class="repair-header">
          <span class="repair-no">{{ repair.repairNo }}</span>
          <span class="status-badge" :class="'status-' + repair.status">
            {{ getStatusText(repair.status) }}
          </span>
        </div>
        <div class="repair-body">
          <!-- 无人机图片 -->
          <div class="repair-image">
            <img :src="repair.imageUrl || '/img/train/0001.jpg'" :alt="repair.droneNo" />
            <div class="drone-info-overlay">
              <span class="drone-no-tag">{{ repair.droneNo }}</span>
              <span class="drone-model-tag">{{ repair.brand }} {{ repair.model }}</span>
            </div>
          </div>
          <div class="repair-fault">
            <strong>故障类型：</strong>{{ repair.faultType }}
          </div>
          <div class="repair-description">
            {{ repair.faultDescription }}
          </div>
          <div class="repair-reporter">
            <i class="bi bi-person"></i>
            <span>{{ repair.reporterName }} - {{ repair.reporterType === 'USER' ? '用户' : '运营员' }}</span>
            <span class="reporter-phone">{{ repair.reporterPhone }}</span>
          </div>
          <div class="repair-time">
            <i class="bi bi-clock"></i>
            <span>{{ formatDateTime(repair.createTime) }}</span>
          </div>
        </div>
        <div class="repair-actions">
          <button
            v-if="repair.status === 0"
            class="btn btn-primary btn-sm"
            @click="handleProcess(repair)"
          >
            <i class="bi bi-tools me-1"></i>处理
          </button>
          <button
            v-if="repair.status === 1"
            class="btn btn-success btn-sm"
            @click="handleComplete(repair)"
          >
            <i class="bi bi-check-lg me-1"></i>完成
          </button>
          <button class="btn btn-outline-primary btn-sm" @click="handleViewDetail(repair)">
            <i class="bi bi-eye"></i>详情
          </button>
        </div>
      </div>
    </div>
    <div v-else class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">加载中...</span>
      </div>
    </div>

    <!-- 详情模态框 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="showDetailModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h5>报修详情</h5>
          <button class="btn-close" @click="showDetailModal = false">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body" v-if="selectedRepair">
          <div class="detail-section">
            <h6 class="detail-section-title">报修信息</h6>
            <div class="detail-row">
              <span class="detail-label">报修单号:</span>
              <span class="detail-value">{{ selectedRepair.repairNo }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">当前状态:</span>
              <span class="detail-value">
                <span class="status-badge-lg" :class="'status-' + selectedRepair.status">
                  {{ getStatusText(selectedRepair.status) }}
                </span>
              </span>
            </div>
            <div class="detail-row">
              <span class="detail-label">报修时间:</span>
              <span class="detail-value">{{ formatDateTime(selectedRepair.createTime) }}</span>
            </div>
          </div>

          <div class="detail-section">
            <h6 class="detail-section-title">无人机信息</h6>
            <div class="detail-drone-image">
              <img :src="selectedRepair.imageUrl || '/img/train/0001.jpg'" :alt="selectedRepair.droneNo" />
              <div class="drone-info-text">
                <div class="detail-row">
                  <span class="detail-label">无人机编号:</span>
                  <span class="detail-value">{{ selectedRepair.droneNo }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">品牌型号:</span>
                  <span class="detail-value">{{ selectedRepair.brand }} {{ selectedRepair.model }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h6 class="detail-section-title">故障信息</h6>
            <div class="detail-row">
              <span class="detail-label">故障类型:</span>
              <span class="detail-value fault-type">{{ selectedRepair.faultType }}</span>
            </div>
            <div class="detail-row-full">
              <span class="detail-label">故障描述:</span>
              <p class="detail-description">{{ selectedRepair.faultDescription }}</p>
            </div>
          </div>

          <div class="detail-section">
            <h6 class="detail-section-title">报修人信息</h6>
            <div class="detail-row">
              <span class="detail-label">报修人:</span>
              <span class="detail-value">{{ selectedRepair.reporterName }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">报修人类型:</span>
              <span class="detail-value">{{ selectedRepair.reporterType === 'USER' ? '用户' : '运营员' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">联系电话:</span>
              <span class="detail-value">{{ selectedRepair.reporterPhone }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showDetailModal = false">关闭</button>
          <button
            v-if="selectedRepair && selectedRepair.status === 0"
            class="btn btn-primary"
            @click="handleProcess(selectedRepair); showDetailModal = false"
          >
            <i class="bi bi-tools me-1"></i>开始处理
          </button>
          <button
            v-if="selectedRepair && selectedRepair.status === 1"
            class="btn btn-success"
            @click="handleComplete(selectedRepair); showDetailModal = false"
          >
            <i class="bi bi-check-lg me-1"></i>完成处理
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getRepairList, startRepair, completeRepair } from '@/api/operator'

const searchKeyword = ref('')
const selectedStatus = ref('')
const showDetailModal = ref(false)
const selectedRepair = ref(null)
const loading = ref(false)

const repairStats = ref({
  total: 0,
  pending: 0,
  processing: 0,
  completed: 0
})

const repairs = ref([])

// 格式化日期时间
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

// 加载报修列表
const loadRepairs = async () => {
  loading.value = true
  try {
    const res = await getRepairList()
    if (res.code === 200 && res.data) {
      repairs.value = res.data
      updateStats()
    }
  } catch (error) {
    console.error('加载报修列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 更新统计数据
const updateStats = () => {
  repairStats.value.total = repairs.value.length
  repairStats.value.pending = repairs.value.filter(r => r.status === 0).length
  repairStats.value.processing = repairs.value.filter(r => r.status === 1).length
  repairStats.value.completed = repairs.value.filter(r => r.status === 2).length
}

const filteredRepairs = computed(() => {
  let result = repairs.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(r =>
      r.repairNo.toLowerCase().includes(keyword) ||
      r.droneNo.toLowerCase().includes(keyword)
    )
  }

  if (selectedStatus.value !== '') {
    result = result.filter(r => r.status === parseInt(selectedStatus.value))
  }

  return result
})

const getStatusText = (status) => {
  const statusMap = {
    0: '待处理',
    1: '处理中',
    2: '已完成',
    3: '已关闭'
  }
  return statusMap[status] || '未知'
}

const handleViewDetail = (repair) => {
  selectedRepair.value = repair
  showDetailModal.value = true
}

const handleProcess = async (repair) => {
  if (confirm(`确认开始处理报修单 ${repair.repairNo} 吗？`)) {
    try {
      const res = await startRepair(repair.id)
      if (res.code === 200) {
        alert('开始处理成功')
        await loadRepairs()
      } else {
        alert(res.message || '操作失败')
      }
    } catch (error) {
      console.error('处理失败:', error)
      alert('操作失败')
    }
  }
}

const handleComplete = async (repair) => {
  const handleResult = prompt('请输入处理结果：')
  if (handleResult !== null) {
    try {
      const res = await completeRepair(repair.id, handleResult)
      if (res.code === 200) {
        alert('完成处理成功')
        await loadRepairs()
      } else {
        alert(res.message || '操作失败')
      }
    } catch (error) {
      console.error('完成处理失败:', error)
      alert('操作失败')
    }
  }
}

onMounted(() => {
  loadRepairs()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h3 {
  font-weight: 600;
  margin: 0;
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-item {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.stat-label {
  color: #64748b;
  font-size: 13px;
}

.stat-item.warning .stat-value { color: #f59e0b; }
.stat-item.primary .stat-value { color: #3b82f6; }
.stat-item.success .stat-value { color: #10b981; }

.filter-bar {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.repairs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.repair-card {
  background: white;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
}

.repair-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.repair-no {
  font-weight: 600;
  font-size: 14px;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-0 { background: #fef3c7; color: #d97706; }
.status-1 { background: #dbeafe; color: #2563eb; }
.status-2 { background: #d1fae5; color: #059669; }
.status-3 { background: #e5e7eb; color: #6b7280; }

.repair-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.repair-image {
  position: relative;
  width: 100%;
  height: 160px;
  border-radius: 10px;
  overflow: hidden;
}

.repair-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.drone-info-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.75), transparent);
  padding: 30px 12px 10px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.drone-no-tag {
  background: rgba(255,255,255,0.95);
  color: #059669;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.drone-model-tag {
  background: rgba(5, 150, 105, 0.9);
  color: white;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
}

.repair-fault {
  color: #ef4444;
  font-size: 14px;
}

.repair-description {
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.repair-reporter {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 13px;
}

.reporter-phone {
  margin-left: auto;
  color: #94a3b8;
}

.repair-time {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.repair-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.repair-actions .btn {
  flex: 1;
}

/* 详情模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 550px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 50px rgba(0,0,0,0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h5 {
  margin: 0;
  font-weight: 600;
  font-size: 18px;
  color: #1e293b;
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  border-radius: 8px;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #f1f5f9;
  color: #475569;
}

.modal-body {
  padding: 24px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 0 0 16px 16px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #f1f5f9;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f8fafc;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-row-full {
  padding: 10px 0;
  border-bottom: 1px solid #f8fafc;
}

.detail-label {
  font-weight: 500;
  color: #64748b;
  font-size: 14px;
  min-width: 100px;
}

.detail-value {
  color: #1e293b;
  font-size: 14px;
  text-align: right;
}

.fault-type {
  color: #ef4444;
  font-weight: 500;
}

.detail-description {
  margin: 8px 0 0 0;
  padding: 12px;
  background: #fef2f2;
  border-radius: 8px;
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
  border-left: 3px solid #ef4444;
}

.status-badge-lg {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
}

/* 详情中的无人机图片 */
.detail-drone-image {
  display: flex;
  gap: 16px;
  align-items: center;
}

.detail-drone-image img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.drone-info-text {
  flex: 1;
}
</style>
