<template>
  <div class="user-vehicles-page">
    <!-- 用户头部导航 -->
    <UserHeader />

    <div class="container py-5">
      <div class="page-header d-flex justify-content-between align-items-center mb-4">
        <h2 class="page-title mb-0">
          <i class="bi bi-airplane me-2"></i>无人机列表
        </h2>
        <router-link to="/" class="btn btn-outline-primary">
          <i class="bi bi-house me-1"></i>返回首页
        </router-link>
      </div>

      <!-- 搜索和筛选 -->
      <div class="filter-bar bg-white rounded-3 p-4 mb-4 shadow-sm">
        <div class="row g-3">
          <div class="col-md-4">
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-search"></i></span>
              <input
                v-model="searchKeyword"
                type="text"
                class="form-control"
                placeholder="搜索品牌或型号"
              />
            </div>
          </div>
          <div class="col-md-3">
            <select v-model="selectedBrand" class="form-select">
              <option value="">全部品牌</option>
              <option value="大疆">大疆</option>
              <option value="道通">道通</option>
            </select>
          </div>
          <div class="col-md-3">
            <select v-model="selectedStatus" class="form-select">
              <option value="">全部状态</option>
              <option value="1">空闲</option>
              <option value="2">使用中</option>
              <option value="3">充电中</option>
            </select>
          </div>
          <div class="col-md-2">
            <button class="btn btn-primary w-100" @click="filterDrones">
              <i class="bi bi-funnel me-1"></i>筛选
            </button>
          </div>
        </div>
      </div>

      <!-- 无人机列表 -->
      <div v-if="filteredDrones.length > 0" class="row g-4">
        <div class="col-lg-4 col-md-6" v-for="drone in filteredDrones" :key="drone.id">
          <div class="drone-card">
            <div class="drone-image">
              <img :src="drone.imageUrl" :alt="drone.model" />
              <span class="drone-status" :class="'status-' + drone.status">
                {{ getStatusText(drone.status) }}
              </span>
              <div class="battery-indicator">
                <i class="bi bi-battery-half"></i>
                {{ getDisplayBatteryLevel(drone) }}%
              </div>
              <!-- 充电进度条 -->
              <div v-if="drone.status === 3 && drone.chargingStartTime" class="charging-progress-overlay">
                <div class="charging-info">
                  <span class="charging-label">充电中</span>
                  <span class="charging-time">{{ getChargingTimeRemaining(drone) }}</span>
                </div>
                <div class="charging-bar">
                  <div class="charging-fill" :style="{ width: getChargingProgress(drone) + '%' }"></div>
                </div>
              </div>
            </div>
            <div class="drone-info">
              <div class="drone-header">
                <h5 class="drone-model">{{ drone.brand }} {{ drone.model }}</h5>
                <span class="drone-no">{{ drone.droneNo }}</span>
              </div>
              <p class="drone-location">
                <i class="bi bi-geo-alt me-1"></i>{{ drone.locationDetail }}
              </p>
              <div class="drone-specs">
                <span class="spec-item">
                  <i class="bi bi-clock me-1"></i>{{ drone.flightHours }}h
                </span>
                <span class="spec-item">
                  <i class="bi bi-speedometer me-1"></i>{{ drone.maxSpeed }}km/h
                </span>
                <span class="spec-item">
                  <i class="bi bi-camera me-1"></i>{{ drone.camera }}
                </span>
              </div>
              <div class="drone-footer">
                <div class="price">
                  <span class="price-amount">¥{{ drone.pricePerMinute }}</span>
                  <span class="price-unit">/分钟</span>
                </div>
                <router-link
                  :to="'/user/vehicle/' + drone.id"
                  class="btn btn-primary"
                  :class="{ 'btn-disabled': drone.status !== 1 }"
                >
                  <i class="bi bi-arrow-right me-1"></i>详情
                </router-link>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <i class="bi bi-search"></i>
        <p>没有找到符合条件的无人机</p>
        <button class="btn btn-outline-primary" @click="resetFilter">
          <i class="bi bi-arrow-counterclockwise me-1"></i>重置筛选
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import UserHeader from '@/components/UserHeader.vue'
import { getVehicleList } from '@/api/user'

const router = useRouter()
const route = useRoute()

const searchKeyword = ref('')
const currentTime = ref(Date.now()) // 响应式当前时间
const selectedBrand = ref('')
const selectedStatus = ref('1')

// 时间更新定时器
let timeUpdateInterval = null

const drones = ref([])
const loading = ref(false)

// 从API加载无人机列表
const loadVehicles = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (selectedStatus.value !== '') {
      params.status = parseInt(selectedStatus.value)
    }
    if (selectedBrand.value) {
      params.brand = selectedBrand.value
    }

    const res = await getVehicleList(params)
    if (res.code === 200 && res.data) {
      drones.value = res.data
    }
  } catch (error) {
    console.error('加载无人机列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取充电进度百分比
const getChargingProgress = (drone) => {
  if (!drone.chargingStartTime || !drone.chargingDuration) return 0
  const elapsed = currentTime.value - new Date(drone.chargingStartTime).getTime()
  return Math.min(100, Math.max(0, (elapsed / drone.chargingDuration) * 100))
}

// 获取显示的电池电量（充电时动态增长）
const getDisplayBatteryLevel = (drone) => {
  // 如果正在充电，根据进度计算当前电量
  if (drone.status === 3 && drone.chargingStartTime && drone.chargingDuration) {
    const progress = getChargingProgress(drone) / 100
    const startBattery = drone.startBatteryLevel || drone.batteryLevel
    const currentBattery = Math.round(startBattery + (100 - startBattery) * progress)
    return Math.min(100, currentBattery)
  }
  return drone.batteryLevel
}

// 获取充电剩余时间
const getChargingTimeRemaining = (drone) => {
  if (!drone.chargingStartTime || !drone.chargingDuration) return ''

  const elapsed = currentTime.value - new Date(drone.chargingStartTime).getTime()
  const remaining = Math.max(0, drone.chargingDuration - elapsed)

  const hours = Math.floor(remaining / (60 * 60 * 1000))
  const minutes = Math.floor((remaining % (60 * 60 * 1000)) / (60 * 1000))
  const seconds = Math.floor((remaining % (60 * 1000)) / 1000)

  if (hours > 0) {
    return `剩余 ${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
  return `剩余 ${minutes}:${String(seconds).padStart(2, '0')}`
}

const filteredDrones = computed(() => {
  let result = drones.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(d =>
      d.brand.toLowerCase().includes(keyword) ||
      d.model.toLowerCase().includes(keyword) ||
      d.droneNo.toLowerCase().includes(keyword)
    )
  }

  if (selectedBrand.value) {
    result = result.filter(d => d.brand === selectedBrand.value)
  }

  if (selectedStatus.value !== '') {
    result = result.filter(d => d.status === parseInt(selectedStatus.value))
  }

  return result
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

const filterDrones = () => {
  loadVehicles()
}

const resetFilter = () => {
  searchKeyword.value = ''
  selectedBrand.value = ''
  selectedStatus.value = '1'
  loadVehicles()
}

// 监听筛选条件变化，自动重新加载
watch([searchKeyword, selectedBrand, selectedStatus], () => {
  loadVehicles()
})

onMounted(() => {
  loadVehicles()
  // 每秒更新当前时间
  timeUpdateInterval = setInterval(() => {
    currentTime.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timeUpdateInterval) {
    clearInterval(timeUpdateInterval)
  }
})

// 监听路由变化，当返回列表页时刷新数据
watch(() => route.path, (newPath, oldPath) => {
  if (newPath === '/user/vehicles') {
    loadVehicles()
  }
})

// 监听路由参数变化（用于强制刷新）
watch(() => route.query.refresh, (refresh) => {
  if (refresh) {
    loadVehicles()
  }
})
</script>

<style scoped>
.page-title {
  font-weight: 700;
  margin-bottom: 30px;
  color: #1e293b;
}

.filter-bar {
  border: 1px solid #e2e8f0;
}

.drone-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  transition: all 0.3s;
  height: 100%;
}

.drone-card:hover {
  box-shadow: 0 15px 35px rgba(0,0,0,0.1);
  transform: translateY(-5px);
}

.drone-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.drone-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.drone-status {
  position: absolute;
  top: 15px;
  right: 15px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-0 { background: #fee2e2; color: #dc2626; }
.status-1 { background: #d1fae5; color: #059669; }
.status-2 { background: #fef3c7; color: #d97706; }
.status-3 { background: #dbeafe; color: #2563eb; }
.status-4 { background: #e5e7eb; color: #6b7280; }

.battery-indicator {
  position: absolute;
  bottom: 15px;
  left: 15px;
  background: rgba(0,0,0,0.7);
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
}

/* 充电进度条样式 */
.charging-progress-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.85), rgba(0,0,0,0.65));
  padding: 12px 15px;
  color: white;
}

.charging-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.charging-label {
  font-size: 12px;
  font-weight: 600;
  color: #fbbf24;
  display: flex;
  align-items: center;
  gap: 4px;
}

.charging-label::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #fbbf24;
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}

.charging-time {
  font-size: 11px;
  color: #fcd34d;
}

.charging-bar {
  width: 100%;
  height: 6px;
  background: rgba(255,255,255,0.3);
  border-radius: 3px;
  overflow: hidden;
}

.charging-fill {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.drone-info {
  padding: 20px;
}

.drone-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.drone-model {
  font-weight: 600;
  margin: 0;
  color: #1e293b;
}

.drone-no {
  font-size: 12px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 6px;
}

.drone-location {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 15px;
}

.drone-specs {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e2e8f0;
}

.spec-item {
  font-size: 13px;
  color: #64748b;
}

.drone-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  display: flex;
  align-items: baseline;
}

.price-amount {
  font-size: 22px;
  font-weight: 700;
  color: var(--primary-color);
}

.price-unit {
  font-size: 14px;
  color: #64748b;
  margin-left: 4px;
}

.btn-disabled {
  opacity: 0.5;
  pointer-events: none;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #94a3b8;
}

.empty-state i {
  font-size: 64px;
  display: block;
  margin-bottom: 20px;
}

.empty-state p {
  font-size: 18px;
  margin-bottom: 30px;
}

.drone-image-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
}
</style>
