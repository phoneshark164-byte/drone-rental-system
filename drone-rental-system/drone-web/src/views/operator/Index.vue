<template>
  <div class="page-header">
    <div>
      <h1 class="page-title">运营控制台</h1>
      <p class="text-muted mt-2">欢迎回来，{{ userInfo.operatorName || '运营员' }}</p>
    </div>
    <div class="text-muted">
      <i class="bi bi-geo-alt me-2"></i>
      <span>{{ userInfo.areaName || '四川大学锦江学院' }}</span>
    </div>
  </div>

  <!-- 统计卡片 -->
  <div class="stats-grid">
    <div class="stat-card">
      <div class="stat-icon green">
        <i class="bi bi-airplane"></i>
      </div>
      <div class="stat-value">{{ stats.vehicleCount }}</div>
      <div class="stat-label">管理无人机</div>
    </div>
    <div class="stat-card">
      <div class="stat-icon blue">
        <i class="bi bi-receipt"></i>
      </div>
      <div class="stat-value">{{ stats.orderCount }}</div>
      <div class="stat-label">订单总数</div>
    </div>
    <div class="stat-card">
      <div class="stat-icon orange">
        <i class="bi bi-tools"></i>
      </div>
      <div class="stat-value">{{ stats.repairCount }}</div>
      <div class="stat-label">报修记录</div>
    </div>
  </div>

  <!-- 地图卡片 -->
  <div class="chart-grid">
    <div class="chart-card" style="grid-column: 1/-1;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h5 class="chart-title mb-0">
          <i class="bi bi-geo-alt me-2" style="color: var(--primary-color);"></i>
          无人机实时位置
        </h5>
        <div class="d-flex gap-2">
          <select id="vehicleFilter" v-model="vehicleFilter" @change="filterVehicles" class="form-select form-select-sm" style="width: 120px;">
            <option value="all">全部无人机</option>
            <option value="available">可用</option>
            <option value="in-use">使用中</option>
            <option value="repair">维修中</option>
            <option value="offline">离线</option>
          </select>
          <button class="btn btn-primary btn-sm" @click="refreshVehicleLocations">
            <i class="bi bi-arrow-repeat me-1"></i>刷新
          </button>
        </div>
      </div>
      <div class="map-legend mb-2">
        <span class="legend-item"><span class="legend-dot available"></span>可用</span>
        <span class="legend-item"><span class="legend-dot in-use"></span>使用中</span>
        <span class="legend-item"><span class="legend-dot repair"></span>维修中</span>
        <span class="legend-item"><span class="legend-dot offline"></span>离线</span>
      </div>
      <div id="operatorMapContainer" class="map-container"></div>
    </div>
  </div>

  <!-- 快捷操作 -->
  <div class="actions-card">
    <h5 class="actions-title">
      <i class="bi bi-lightning me-2" style="color: var(--primary-color);"></i>快捷操作
    </h5>
    <div class="row g-3">
      <div class="col-6 col-md-3">
        <router-link to="/operator/vehicles" class="action-btn">
          <i class="bi bi-plus-circle"></i>
          <span>添加无人机</span>
        </router-link>
      </div>
      <div class="col-6 col-md-3">
        <router-link to="/operator/vehicles" class="action-btn">
          <i class="bi bi-geo-alt"></i>
          <span>无人机定位</span>
        </router-link>
      </div>
      <div class="col-6 col-md-3">
        <router-link to="/operator/orders" class="action-btn">
          <i class="bi bi-clipboard-check"></i>
          <span>订单处理</span>
        </router-link>
      </div>
      <div class="col-6 col-md-3">
        <router-link to="/operator/repairs" class="action-btn">
          <i class="bi bi-tools"></i>
          <span>报修处理</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getVehicleMapData, getDashboardStats } from '@/api/operator'
import { getUserInfo, clearLoginInfo } from '@/utils/auth'

// 当前时间 - 用于充电进度实时更新
const currentTime = ref(Date.now())
let timeUpdateInterval = null

const userInfo = ref({})
const stats = ref({
  vehicleCount: 0,
  orderCount: 0,
  repairCount: 0
})
const vehicleFilter = ref('all')
let map = null
let markers = []
let infoWindow = null
let autoRefreshTimer = null
let currentVehicle = null // 当前显示的无人机信息

// 模拟无人机数据
const mockVehicleData = ref([
  { id: 1, droneNo: 'DRONE-E001', brand: '道通', model: 'EVO II', imageUrl: '/img/train/0005.jpg', lat: 29.544, lng: 103.765, batteryLevel: 95, status: 'available', location: '四川大学锦江学院' },
  { id: 2, droneNo: 'DRONE-E002', brand: '大疆', model: 'Mini 3', imageUrl: '/img/train/0006.jpg', lat: 29.545, lng: 103.766, batteryLevel: 88, status: 'in-use', location: '四川大学锦江学院' },
  { id: 3, droneNo: 'DRONE-E003', brand: '大疆', model: 'Mavic 3', imageUrl: '/img/train/0001.jpg', lat: 29.546, lng: 103.767, batteryLevel: 25, status: 'available', location: '四川大学锦江学院' },
  { id: 4, droneNo: 'DRONE-E004', brand: '道通', model: 'Lite+', imageUrl: '/img/train/0008.jpg', lat: 29.547, lng: 103.768, batteryLevel: 65, status: 'available', location: '四川大学锦江学院' },
  { id: 5, droneNo: 'DRONE-E005', brand: '大疆', model: 'Air 3', imageUrl: '/img/train/0003.jpg', lat: 29.543, lng: 103.764, batteryLevel: 0, status: 'repair', location: '四川大学锦江学院' }
])

// 从API同步无人机数据
const syncDroneData = async () => {
  try {
    const res = await getVehicleMapData()
    if (res.code === 200 && res.data) {
      // 状态映射：后端状态 -> 地图状态
      const statusMap = {
        0: 'repair',
        1: 'available',
        2: 'in-use',
        3: 'charging',
        4: 'maintenance'
      }

      // 用API数据替换现有数据
      mockVehicleData.value = res.data.map(drone => ({
        id: drone.id,
        droneNo: drone.vehicleNo,
        brand: drone.brand,
        model: drone.model,
        lat: drone.lat,
        lng: drone.lng,
        batteryLevel: drone.batteryLevel,
        imageUrl: drone.imageUrl || '/img/train/0001.jpg',
        status: statusMap[drone.status] || 'available',
        location: drone.location
      }))
    }
  } catch (e) {
    console.error('同步无人机数据失败:', e)
  }
}

// 加载高德地图脚本
const loadAMapScript = () => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.src = 'https://webapi.amap.com/maps?v=2.0&key=bff5e715168525e6b64ceff26b8723a8&plugin=AMap.Scale,AMap.ToolBar'
    script.onload = resolve
    script.onerror = reject
    document.head.appendChild(script)
  })
}

// 初始化地图
const initMap = async () => {
  try {
    await loadAMapScript()

    map = new AMap.Map('operatorMapContainer', {
      zoom: 4,
      center: [105.0, 35.0],
      viewMode: '2D',
      mapStyle: 'amap://styles/normal',
      showIndoorMap: false
    })

    // 添加比例尺
    map.addControl(new AMap.Scale())

    // 添加工具栏
    map.addControl(new AMap.ToolBar({
      position: 'RT'
    }))

    // 创建信息窗体
    infoWindow = new AMap.InfoWindow({
      offset: new AMap.Pixel(0, -30),
      size: new AMap.Size(260, 0),
      closeWhenClickMap: true
    })

    // 加载无人机位置
    loadVehicleLocations()

    // 设置自动刷新
    autoRefreshTimer = setInterval(() => {
      loadVehicleLocations()
    }, 30000)
  } catch (error) {
    console.error('地图初始化失败:', error)
  }
}

// 加载无人机位置
const loadVehicleLocations = async () => {
  if (!map) return

  try {
    // 从API获取数据
    const res = await getVehicleMapData()
    let vehicleData = []

    if (res.code === 200 && res.data) {
      console.log('API返回的原始数据:', res.data)
      // 转换API返回的数据格式
      const statusMap = {
        0: 'repair',
        1: 'available',
        2: 'in-use',
        3: 'charging',
        4: 'maintenance'
      }
      vehicleData = res.data.map(v => {
        // 处理充电开始时间
        let chargingStartTime = null
        let chargingDuration = null
        if (v.chargingStartTime && v.status === 3) {
          chargingStartTime = new Date(v.chargingStartTime).getTime()
          // 计算充电时长
          if (v.startBatteryLevel != null) {
            const startBattery = v.startBatteryLevel || 0
            const batteryNeeded = 100 - startBattery
            const hoursToFull = 2
            chargingDuration = (batteryNeeded / 100) * hoursToFull * 60 * 60 * 1000
          }
          console.log('API返回充电无人机:', v.vehicleNo, '开始时间:', v.chargingStartTime, '计算后:', chargingStartTime)
        }

        return {
          id: v.id,
          droneNo: v.vehicleNo,
          brand: v.brand,
          model: v.model,
          imageUrl: v.imageUrl,
          lat: v.lat,
          lng: v.lng,
          batteryLevel: v.batteryLevel,
          status: statusMap[v.status] || 'available',
          location: v.location,
          chargingStartTime: chargingStartTime,
          chargingDuration: chargingDuration,
          startBatteryLevel: v.startBatteryLevel
        }
      })
    } else {
      // API失败，使用本地模拟数据
      vehicleData = mockVehicleData.value
    }

    // 清除现有标记
    if (markers.length > 0) {
      map.remove(markers)
      markers = []
    }

    let filteredData = vehicleData
    if (vehicleFilter.value !== 'all') {
      filteredData = vehicleData.filter(v => v.status === vehicleFilter.value)
    }

    filteredData.forEach(vehicle => {
      const marker = createVehicleMarker(vehicle)
      if (marker) {
        // 调试：打印无人机数据
        marker.on('click', () => {
          console.log('点击标记，无人机数据:', vehicle)
          console.log('无人机状态:', vehicle.status)
          console.log('充电开始时间:', vehicle.chargingStartTime)
          console.log('充电时长:', vehicle.chargingDuration)
          showVehicleInfo(vehicle)
        })
        markers.push(marker)
      }
    })

    map.add(markers)
  } catch (error) {
    console.error('加载无人机数据失败，使用本地数据:', error)
    // 失败时使用本地模拟数据
    const vehicleData = mockVehicleData.value
    if (markers.length > 0) {
      map.remove(markers)
      markers = []
    }
    vehicleData.forEach(vehicle => {
      const marker = createVehicleMarker(vehicle)
      if (marker) {
        marker.on('click', () => showVehicleInfo(vehicle))
        markers.push(marker)
      }
    })
    map.add(markers)
  }
}

// 创建无人机标记
const createVehicleMarker = (vehicle) => {
  const statusClass = {
    'available': 'available',
    'in-use': 'in-use',
    'repair': 'repair',
    'offline': 'offline',
    'charging': 'charging',
    'maintenance': 'repair'
  }[vehicle.status] || 'offline'

  const content = `
    <div class="vehicle-marker ${statusClass}">
      <i class="bi bi-airplane"></i>
    </div>
  `

  return new AMap.Marker({
    position: [vehicle.lng, vehicle.lat],
    content: content,
    offset: new AMap.Pixel(-18, -18),
    title: vehicle.droneNo
  })
}

// 获取充电进度百分比
const getChargingProgress = (vehicle) => {
  if (!vehicle.chargingStartTime || !vehicle.chargingDuration) return 0
  const elapsed = currentTime.value - vehicle.chargingStartTime
  return Math.min(100, Math.max(0, (elapsed / vehicle.chargingDuration) * 100))
}

// 获取显示的电池电量（充电时动态增长）
const getDisplayBatteryLevel = (vehicle) => {
  // 如果正在充电，根据进度计算当前电量
  if (vehicle.status === 'charging' && vehicle.chargingStartTime && vehicle.chargingDuration) {
    const progress = getChargingProgress(vehicle) / 100
    const startBattery = vehicle.startBatteryLevel || vehicle.batteryLevel
    const currentBattery = Math.round(startBattery + (100 - startBattery) * progress)
    return Math.min(100, currentBattery)
  }
  return vehicle.batteryLevel
}

// 获取充电剩余时间
const getChargingTimeRemaining = (vehicle) => {
  if (!vehicle.chargingStartTime || !vehicle.chargingDuration) return ''

  const elapsed = currentTime.value - vehicle.chargingStartTime
  const remaining = Math.max(0, vehicle.chargingDuration - elapsed)

  const hours = Math.floor(remaining / (60 * 60 * 1000))
  const minutes = Math.floor((remaining % (60 * 60 * 1000)) / (60 * 1000))
  const seconds = Math.floor((remaining % (60 * 1000)) / 1000)

  if (hours > 0) {
    return `剩余 ${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
  return `剩余 ${minutes}:${String(seconds).padStart(2, '0')}`
}

// 显示无人机信息
const showVehicleInfo = (vehicle) => {
  // 保存当前显示的无人机
  currentVehicle = vehicle
  updateInfoWindow(vehicle)
}

// 更新信息窗体内容
const updateInfoWindow = (vehicle) => {
  // 调试日志
  console.log('更新无人机信息:', vehicle)
  console.log('充电开始时间:', vehicle.chargingStartTime)
  console.log('充电时长:', vehicle.chargingDuration)
  console.log('当前时间:', currentTime.value)

  const statusText = {
    'available': '可用',
    'in-use': '使用中',
    'repair': '维修中',
    'offline': '离线',
    'charging': '充电中'
  }[vehicle.status] || '未知'

  const displayBattery = getDisplayBatteryLevel(vehicle)
  const chargingProgress = getChargingProgress(vehicle)
  const chargingTime = getChargingTimeRemaining(vehicle)

  console.log('充电进度:', chargingProgress)
  console.log('显示电量:', displayBattery)

  let chargingInfo = ''
  if (vehicle.status === 'charging' && vehicle.chargingStartTime) {
    chargingInfo = `
      <div class="info-window-item">
        <span class="info-window-label">充电进度:</span>
        <span class="info-window-value charging-progress-text">${chargingProgress.toFixed(0)}% - ${chargingTime}</span>
      </div>
      <div class="charging-progress-bar">
        <div class="charging-progress-fill" style="width: ${chargingProgress}%"></div>
      </div>
    `
  }

  const content = `
    <div class="info-window-content">
      <div class="info-window-image">
        <img src="${vehicle.imageUrl || '/img/train/0001.jpg'}" alt="${vehicle.droneNo}" />
      </div>
      <div class="info-window-title">${vehicle.brand} ${vehicle.model}</div>
      <div class="info-window-item">
        <span class="info-window-label">编号:</span>
        <span class="info-window-value">${vehicle.droneNo}</span>
      </div>
      <div class="info-window-item">
        <span class="info-window-label">位置:</span>
        <span class="info-window-value">${vehicle.location}</span>
      </div>
      <div class="info-window-item">
        <span class="info-window-label">状态:</span>
        <span class="info-window-value">${statusText}</span>
      </div>
      <div class="info-window-item">
        <span class="info-window-label">电量:</span>
        <span class="info-window-value">${displayBattery}%</span>
      </div>
      ${chargingInfo}
    </div>
  `

  infoWindow.setContent(content)
  infoWindow.open(map, [vehicle.lng, vehicle.lat])

  // 监听信息窗体关闭事件
  infoWindow.on('close', () => {
    currentVehicle = null
  })
}

// 筛选无人机
const filterVehicles = () => {
  if (!map) return

  // 清除现有标记
  if (markers.length > 0) {
    map.remove(markers)
    markers = []
  }

  let filteredData = mockVehicleData.value
  if (vehicleFilter.value !== 'all') {
    filteredData = mockVehicleData.value.filter(v => v.status === vehicleFilter.value)
  }

  filteredData.forEach(vehicle => {
    const marker = createVehicleMarker(vehicle)
    if (marker) {
      marker.on('click', () => showVehicleInfo(vehicle))
      markers.push(marker)
    }
  })

  map.add(markers)
}

// 刷新无人机位置
const refreshVehicleLocations = () => {
  loadVehicleLocations()
}

// 加载统计数据
const loadStats = async () => {
  try {
    const res = await getDashboardStats()
    if (res.code === 200 && res.data) {
      stats.value = {
        vehicleCount: res.data.vehicleCount || 0,
        orderCount: res.data.orderCount || 0,
        repairCount: res.data.repairCount || 0
      }
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

onMounted(async () => {
  // 清除旧的 localStorage 数据
  localStorage.removeItem('adminDrones')

  // 检查会话是否有效
  const info = getUserInfo()
  if (!info) {
    // 会话无效，跳转到登录页
    window.location.href = '/operator/login'
    return
  }
  userInfo.value = info

  // 同步无人机数据
  await syncDroneData()

  // 加载统计数据
  await loadStats()

  // 每秒更新当前时间，用于充电进度实时更新
  timeUpdateInterval = setInterval(() => {
    currentTime.value = Date.now()
    // 如果当前显示的是充电中的无人机，更新信息窗体
    if (currentVehicle && currentVehicle.status === 'charging' && currentVehicle.chargingStartTime) {
      // 更新当前无人机的时间相关数据
      currentVehicle.chargingStartTime = currentVehicle.chargingStartTime // 保持不变
      updateInfoWindow(currentVehicle)
    }
  }, 1000)

  // 初始化地图
  await initMap()
})

onUnmounted(() => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
  }
  if (timeUpdateInterval) {
    clearInterval(timeUpdateInterval)
  }

  // 清理地图
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped>
:root {
  --primary-color: #059669;
  --accent-color: #10b981;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
}

.text-muted {
  color: #64748b;
}

.mt-2 {
  margin-top: 8px;
}

.me-2 {
  margin-right: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 15px;
}

.stat-icon.green {
  background: linear-gradient(135deg, #d1fae5, #a7f3d0);
  color: #059669;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #3b82f6;
}

.stat-icon.orange {
  background: linear-gradient(135deg, #fed7aa, #fdba74);
  color: #ea580c;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 5px;
}

.stat-label {
  color: #64748b;
  font-size: 14px;
}

/* 图表卡片 */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

.chart-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  border: 1px solid #e2e8f0;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

/* 地图容器 */
.map-container {
  width: 100%;
  height: 400px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  border: 1px solid #e2e8f0;
}

.map-legend {
  display: flex;
  gap: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-dot.available {
  background: #10b981;
}

.legend-dot.in-use {
  background: #3b82f6;
}

.legend-dot.repair {
  background: #f59e0b;
}

.legend-dot.offline {
  background: #94a3b8;
}

/* 无人机标记样式 */
:deep(.vehicle-marker) {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 3px solid white;
  box-shadow: 0 2px 10px rgba(0,0,0,0.2);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

:deep(.vehicle-marker:hover) {
  transform: scale(1.1);
  box-shadow: 0 4px 15px rgba(5, 150, 105, 0.4);
}

:deep(.vehicle-marker.available) {
  background: linear-gradient(135deg, #10b981, #059669);
}

:deep(.vehicle-marker.in-use) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

:deep(.vehicle-marker.repair) {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

:deep(.vehicle-marker.charging) {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  animation: pulse-charging 1.5s ease-in-out infinite;
}

@keyframes pulse-charging {
  0%, 100% {
    box-shadow: 0 2px 10px rgba(245, 158, 11, 0.3);
  }
  50% {
    box-shadow: 0 4px 20px rgba(245, 158, 11, 0.6);
  }
}

:deep(.vehicle-marker.offline) {
  background: #94a3b8;
}

/* 信息窗体样式 */
:deep(.amap-info-content) {
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
  padding: 12px;
}

:deep(.info-window-content) {
  min-width: 240px;
}

:deep(.info-window-title) {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
  font-size: 16px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 5px;
}

/* 信息窗口图片样式 */
:deep(.info-window-image) {
  width: 100%;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 10px;
}

:deep(.info-window-image img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

:deep(.info-window-item) {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 13px;
}

:deep(.info-window-label) {
  color: #64748b;
}

:deep(.info-window-value) {
  color: #1e293b;
  font-weight: 500;
}

/* 充电进度样式 */
:deep(.charging-progress-text) {
  color: #d97706;
}

:deep(.charging-progress-bar) {
  width: 100%;
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
  margin-top: 6px;
}

:deep(.charging-progress-fill) {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
  border-radius: 3px;
  transition: width 0.5s ease;
}

/* 快捷操作 */
.actions-card {
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  border: 1px solid #e2e8f0;
}

.actions-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 24px;
}

.row {
  display: flex;
  flex-wrap: wrap;
  margin-right: -12px;
  margin-left: -12px;
}

.g-3 > * {
  padding: 0 12px;
  margin-bottom: 16px;
}

.col-6 {
  flex: 0 0 50%;
  max-width: 50%;
  padding: 0 12px;
}

.col-md-3 {
  flex: 0 0 25%;
  max-width: 25%;
}

@media (min-width: 768px) {
  .col-md-3 {
    flex: 0 0 25%;
    max-width: 25%;
  }
}

.action-btn {
  padding: 20px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  transition: all 0.3s;
  background: #f8fafc;
  color: #64748b;
}

.action-btn:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
  color: var(--primary-color);
}

.action-btn i {
  font-size: 28px;
}

.action-btn span {
  font-weight: 500;
}

/* 按钮样式 */
.btn {
  padding: 10px 20px;
  border-radius: 10px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border: none;
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(5, 150, 105, 0.3);
  color: white;
}

.btn-sm {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
}

.form-select-sm {
  padding: 6px 24px 6px 12px;
  font-size: 13px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background-color: white;
}

.d-flex {
  display: flex;
}

.justify-content-between {
  justify-content: space-between;
}

.align-items-center {
  align-items: center;
}

.gap-2 {
  gap: 8px;
}

.mb-0 {
  margin-bottom: 0;
}

.mb-2 {
  margin-bottom: 8px;
}

.mb-3 {
  margin-bottom: 12px;
}
</style>
