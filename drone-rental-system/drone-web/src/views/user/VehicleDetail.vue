<template>
  <div class="vehicle-detail-page">
    <!-- 用户头部导航 -->
    <UserHeader />

    <div class="container py-5">
      <div class="page-header d-flex justify-content-between align-items-center mb-4">
        <router-link to="/user/vehicles" class="btn btn-outline-secondary">
          <i class="bi bi-arrow-left me-1"></i>返回列表
        </router-link>
        <router-link to="/" class="btn btn-outline-primary">
          <i class="bi bi-house me-1"></i>返回首页
        </router-link>
      </div>
      <div v-if="drone" class="row">
        <!-- 左侧无人机详情 -->
        <div class="col-lg-8">
          <div class="detail-card">
            <div class="detail-image">
              <img :src="drone.imageUrl" :alt="drone.model" />
              <span class="status-badge" :class="'status-' + drone.status">
                {{ getStatusText(drone.status) }}
              </span>
            </div>

            <div class="detail-info">
              <h2 class="detail-title">{{ drone.brand }} {{ drone.model }}</h2>
              <p class="detail-no">编号：{{ drone.droneNo }}</p>

              <div class="detail-specs">
                <div class="spec-item">
                  <i class="bi bi-battery-half"></i>
                  <span>电量 {{ currentBatteryLevel }}%</span>
                </div>
                <div class="spec-item">
                  <i class="bi bi-clock"></i>
                  <span>飞行时长 {{ drone.flightHours }}h</span>
                </div>
                <div class="spec-item">
                  <i class="bi bi-speedometer"></i>
                  <span>最高速度 {{ drone.maxSpeed }}km/h</span>
                </div>
                <div class="spec-item">
                  <i class="bi bi-camera"></i>
                  <span>{{ drone.camera }}相机</span>
                </div>
                <div class="spec-item">
                  <i class="bi bi-geo-alt"></i>
                  <span>{{ drone.locationDetail }}</span>
                </div>
              </div>

              <!-- 充电进度条 -->
              <div v-if="drone.status === 3" class="charging-progress-section">
                <div class="charging-header">
                  <span class="charging-title">
                    <i class="bi bi-lightning-charge"></i>
                    正在充电
                  </span>
                  <span class="charging-percent">{{ Math.round(getChargingProgress) }}%</span>
                </div>
                <div class="charging-progress-bar">
                  <div
                    class="charging-progress-fill"
                    :style="{ width: getChargingProgress + '%' }"
                  >
                    <span class="charging-glow"></span>
                  </div>
                </div>
                <div class="charging-info">
                  <span class="charging-time">{{ formatRemainingTime(getRemainingTime) }}</span>
                  <span class="charging-battery">预计 {{ Math.round(currentBatteryLevel) }}% → 100%</span>
                </div>
              </div>

              <div class="detail-description">
                <h4>产品介绍</h4>
                <p>{{ drone.description }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧租赁面板 -->
        <div class="col-lg-4">
          <div class="rental-card">
            <div class="rental-price">
              <span class="price-amount">¥{{ drone.pricePerMinute }}</span>
              <span class="price-unit">/分钟</span>
            </div>

            <div class="rental-form">
              <div class="mb-3">
                <label class="form-label">租赁时长</label>
                <div class="duration-options">
                  <button
                    v-for="option in durationOptions"
                    :key="option.value"
                    class="duration-btn"
                    :class="{ 'active': form.duration === option.value }"
                    @click="form.duration = option.value"
                  >
                    {{ option.label }}
                  </button>
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label">开始时间</label>
                <input
                  v-model="form.startTime"
                  type="datetime-local"
                  class="form-control"
                  :min="minDateTime"
                />
              </div>

              <div class="rental-summary">
                <div class="summary-row">
                  <span>租赁时长：</span>
                  <span>{{ form.duration }}分钟</span>
                </div>
                <div class="summary-row">
                  <span>预计费用：</span>
                  <span class="summary-amount">¥{{ (drone.pricePerMinute * form.duration).toFixed(2) }}</span>
                </div>
              </div>

              <button
                class="btn btn-rental w-100"
                :disabled="drone.status !== 1 || loading"
                @click="handleRental"
              >
                <i class="bi bi-check-circle me-2"></i>
                {{ loading ? '提交中...' : '立即租赁' }}
              </button>

              <button
                class="btn btn-report-fault w-100 mt-2"
                @click="showRepairModal = true"
              >
                <i class="bi bi-exclamation-triangle me-2"></i>
                报告故障
              </button>

              <div v-if="drone.status !== 1" class="unavailable-hint">
                <i class="bi bi-info-circle me-1"></i>
                该无人机当前不可用
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="mt-3">加载中...</p>
      </div>
    </div>

    <!-- 报修模态框 -->
    <div v-if="showRepairModal" class="modal-overlay" @click="showRepairModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h5>报告无人机故障</h5>
          <button class="btn-close" @click="showRepairModal = false">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">无人机编号</label>
            <input type="text" class="form-control" :value="drone?.droneNo" disabled />
          </div>
          <div class="mb-3">
            <label class="form-label">故障类型</label>
            <select v-model="repairForm.faultType" class="form-select">
              <option value="">请选择故障类型</option>
              <option value="电池故障">电池故障</option>
              <option value="电机故障">电机故障</option>
              <option value="螺旋桨损坏">螺旋桨损坏</option>
              <option value="相机故障">相机故障</option>
              <option value="遥控器失灵">遥控器失灵</option>
              <option value="定位异常">定位异常</option>
              <option value="其他故障">其他故障</option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-label">故障描述</label>
            <textarea
              v-model="repairForm.faultDescription"
              class="form-control"
              rows="4"
              placeholder="请详细描述故障情况..."
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showRepairModal = false">取消</button>
          <button class="btn btn-danger" @click="handleSubmitRepair" :disabled="repairLoading">
            {{ repairLoading ? '提交中...' : '提交报修' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UserHeader from '@/components/UserHeader.vue'
import { createOrder, getVehicleDetail, createRepair } from '@/api/user'

const route = useRoute()
const router = useRouter()

const drone = ref(null)
const loading = ref(false)
const currentTime = ref(Date.now())
let timeUpdateInterval = null

// 报修相关
const showRepairModal = ref(false)
const repairLoading = ref(false)
const repairForm = ref({
  faultType: '',
  faultDescription: ''
})

const form = ref({
  duration: 60,
  startTime: ''
})

const durationOptions = [
  { label: '30分钟', value: 30 },
  { label: '1小时', value: 60 },
  { label: '2小时', value: 120 },
  { label: '4小时', value: 240 }
]

const minDateTime = computed(() => {
  const now = new Date()
  now.setMinutes(now.getMinutes() + 30)
  return now.toISOString().slice(0, 16)
})

const getStatusText = (status) => {
  const statusMap = {
    0: '故障',
    1: '空闲',
    2: '使用中',
    3: '充电中',
    4: '维护中'
  }
  return statusMap[status] || '未知'
}

// 充电进度计算
const getChargingProgress = computed(() => {
  if (!drone.value || drone.value.status !== 3 || !drone.value.chargingStartTime || !drone.value.chargingDuration) {
    return 0
  }
  const elapsed = currentTime.value - drone.value.chargingStartTime
  return Math.min(100, Math.max(0, (elapsed / drone.value.chargingDuration) * 100))
})

// 当前电量（充电时实时计算）
const currentBatteryLevel = computed(() => {
  if (!drone.value) return 0
  if (drone.value.status !== 3 || !drone.value.startBatteryLevel) {
    return drone.value.batteryLevel || 0
  }
  const startBattery = drone.value.startBatteryLevel
  const progress = getChargingProgress.value / 100
  return Math.min(100, Math.round(startBattery + (100 - startBattery) * progress))
})

// 剩余充电时间（秒）
const getRemainingTime = computed(() => {
  if (!drone.value || drone.value.status !== 3 || !drone.value.chargingDuration) {
    return 0
  }
  const elapsed = currentTime.value - drone.value.chargingStartTime
  const remaining = drone.value.chargingDuration - elapsed
  return Math.max(0, Math.round(remaining / 1000))
})

// 格式化剩余时间
const formatRemainingTime = (seconds) => {
  if (seconds <= 0) return '即将充满'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return `约${hours}小时${minutes}分钟充满`
  }
  return `约${minutes}分钟充满`
}

const handleRental = async () => {
  if (!form.value.startTime) {
    alert('请选择开始时间')
    return
  }

  loading.value = true
  try {
    // 调用创建订单API
    const res = await createOrder(drone.value.id, form.value.duration)
    if (res.code === 200) {
      alert('订单创建成功！')
      router.push('/user/orders')
    } else {
      alert(res.message || '创建订单失败')
    }
  } catch (error) {
    console.error('租赁失败:', error)
    alert('租赁失败，请重试')
  } finally {
    loading.value = false
  }
}

// 加载无人机详情数据
const loadVehicleDetail = async () => {
  const id = route.params.id
  try {
    // 从API加载无人机详情
    const res = await getVehicleDetail(id)
    if (res.code === 200 && res.data) {
      const v = res.data
      // 处理充电数据
      let chargingStartTime = null
      let chargingDuration = null
      let startBatteryLevel = null

      if (v.status === 3 && v.chargingStartTime) {
        chargingStartTime = new Date(v.chargingStartTime).getTime()
        startBatteryLevel = v.startBatteryLevel || v.batteryLevel || 0
        const batteryNeeded = 100 - startBatteryLevel
        const hoursToFull = 2 // 充满需要2小时
        chargingDuration = (batteryNeeded / 100) * hoursToFull * 60 * 60 * 1000
      }

      drone.value = {
        id: v.id,
        droneNo: v.droneNo || v.vehicleNo,
        brand: v.brand,
        model: v.model,
        imageUrl: v.imageUrl || '/img/train/0001.jpg',
        locationDetail: v.locationDetail,
        batteryLevel: v.batteryLevel,
        status: v.status,
        flightHours: v.flightHours || 0,
        maxSpeed: v.maxSpeed || 75,
        camera: v.camera || '4K',
        pricePerMinute: v.pricePerMinute || 0.5,
        description: `${v.brand} ${v.model} - 高性能航拍无人机`,
        // 充电相关数据
        chargingStartTime,
        chargingDuration,
        startBatteryLevel
      }
    } else {
      alert('无人机不存在')
      router.push('/')
      return
    }
  } catch (error) {
    console.error('加载无人机详情失败:', error)
    alert('加载失败，请重试')
    router.push('/')
    return
  }

  // 设置默认开始时间
  const now = new Date()
  now.setHours(now.getHours() + 1)
  form.value.startTime = now.toISOString().slice(0, 16)
}

onMounted(() => {
  loadVehicleDetail()
  // 启动实时更新
  timeUpdateInterval = setInterval(() => {
    currentTime.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  // 清理定时器
  if (timeUpdateInterval) {
    clearInterval(timeUpdateInterval)
    timeUpdateInterval = null
  }
})

// 监听路由参数变化，当切换到不同的无人机详情时重新加载
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    drone.value = null // 重置数据
    loadVehicleDetail()
  }
})

// 监听路由路径变化，确保从其他页面返回时刷新数据
watch(() => route.path, (newPath, oldPath) => {
  if (newPath.startsWith('/user/vehicle/') && oldPath !== newPath) {
    loadVehicleDetail()
  }
})

// 提交报修
const handleSubmitRepair = async () => {
  if (!repairForm.value.faultType) {
    alert('请选择故障类型')
    return
  }
  if (!repairForm.value.faultDescription.trim()) {
    alert('请描述故障情况')
    return
  }

  repairLoading.value = true
  try {
    const res = await createRepair({
      vehicleId: drone.value.id,
      vehicleNo: drone.value.droneNo,
      faultType: repairForm.value.faultType,
      faultDescription: repairForm.value.faultDescription
    })
    if (res.code === 200) {
      alert('报修提交成功！运营方会尽快处理')
      showRepairModal.value = false
      repairForm.value = { faultType: '', faultDescription: '' }
    } else {
      alert(res.message || '提交失败')
    }
  } catch (error) {
    console.error('提交报修失败:', error)
    alert('提交失败，请重试')
  } finally {
    repairLoading.value = false
  }
}
</script>

<style scoped>
.detail-card {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  margin-bottom: 20px;
}

.detail-image {
  position: relative;
  height: 400px;
}

.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  position: absolute;
  top: 20px;
  right: 20px;
  padding: 10px 20px;
  border-radius: 25px;
  font-size: 14px;
  font-weight: 600;
}

.status-0 { background: #fee2e2; color: #dc2626; }
.status-1 { background: #d1fae5; color: #059669; }
.status-2 { background: #fef3c7; color: #d97706; }
.status-3 { background: #dbeafe; color: #2563eb; }
.status-4 { background: #e5e7eb; color: #6b7280; }

.detail-info {
  padding: 30px;
}

.detail-title {
  font-weight: 700;
  margin-bottom: 5px;
}

.detail-no {
  color: #64748b;
  margin-bottom: 25px;
}

.detail-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 30px;
  padding-bottom: 30px;
  border-bottom: 1px solid #e2e8f0;
}

.spec-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
}

.spec-item i {
  font-size: 20px;
  color: var(--primary-color);
}

.detail-description h4 {
  font-weight: 600;
  margin-bottom: 15px;
}

.detail-description p {
  color: #64748b;
  line-height: 1.8;
}

/* 充电进度条 */
.charging-progress-section {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 30px;
  border: 1px solid #bae6fd;
}

.charging-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.charging-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #0369a1;
  font-size: 16px;
}

.charging-title i {
  color: #fbbf24;
  font-size: 20px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.1);
  }
}

.charging-percent {
  font-size: 24px;
  font-weight: 700;
  color: #0284c7;
}

.charging-progress-bar {
  height: 12px;
  background: #e0f2fe;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
  margin-bottom: 12px;
}

.charging-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #22d3ee 0%, #0ea5e9 50%, #0284c7 100%);
  border-radius: 10px;
  position: relative;
  transition: width 0.5s ease;
  min-width: 2%;
}

.charging-glow {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 20px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6));
  animation: shimmer 2s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    opacity: 0;
    transform: translateX(-10px);
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateX(10px);
  }
}

.charging-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.charging-time {
  font-size: 14px;
  color: #0369a1;
  font-weight: 500;
}

.charging-battery {
  font-size: 14px;
  color: #64748b;
}

/* 租赁卡片 */
.rental-card {
  background: white;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  position: sticky;
  top: 100px;
}

.rental-price {
  text-align: center;
  padding-bottom: 25px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 25px;
}

.price-amount {
  font-size: 36px;
  font-weight: 700;
  color: var(--primary-color);
}

.price-unit {
  font-size: 16px;
  color: #64748b;
}

.duration-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.duration-btn {
  padding: 12px;
  border: 2px solid #e2e8f0;
  background: white;
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.2s;
}

.duration-btn:hover {
  border-color: var(--primary-color);
}

.duration-btn.active {
  border-color: var(--primary-color);
  background: rgba(59, 130, 246, 0.1);
  color: var(--primary-color);
}

.rental-summary {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 25px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.summary-row:last-child {
  margin-bottom: 0;
  padding-top: 10px;
  border-top: 1px dashed #e2e8f0;
  font-weight: 600;
}

.summary-amount {
  color: var(--primary-color);
  font-size: 18px;
}

.btn-rental {
  padding: 16px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
}

.btn-rental:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.3);
}

.btn-rental:disabled {
  opacity: 0.7;
}

.btn-report-fault {
  padding: 12px;
  background: #fff;
  border: 2px solid #ef4444;
  color: #ef4444;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-report-fault:hover {
  background: #ef4444;
  color: #fff;
}

/* 报修模态框样式 */
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
  max-width: 500px;
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

.unavailable-hint {
  margin-top: 15px;
  padding: 12px;
  background: #fef3c7;
  color: #d97706;
  border-radius: 10px;
  font-size: 14px;
  text-align: center;
}

.detail-image-placeholder {
  width: 100%;
  height: 400px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 80px;
}
</style>
