<template>
  <div class="operator-vehicles">
    <div class="page-header">
      <h3>无人机管理</h3>
      <button class="btn btn-primary" @click="openAddModal">
        <i class="bi bi-plus-lg me-1"></i>添加无人机
      </button>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-item">
        <span class="stat-value">{{ vehicleStats.total }}</span>
        <span class="stat-label">总数</span>
      </div>
      <div class="stat-item success">
        <span class="stat-value">{{ vehicleStats.available }}</span>
        <span class="stat-label">空闲</span>
      </div>
      <div class="stat-item warning">
        <span class="stat-value">{{ vehicleStats.charging }}</span>
        <span class="stat-label">充电中</span>
      </div>
      <div class="stat-item danger">
        <span class="stat-value">{{ vehicleStats.fault }}</span>
        <span class="stat-label">故障</span>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <div class="row g-3">
        <div class="col-md-4">
          <input
            v-model="searchKeyword"
            type="text"
            class="form-control"
            placeholder="搜索编号、品牌"
          />
        </div>
        <div class="col-md-2">
          <select v-model="selectedStatus" class="form-select">
            <option value="">全部状态</option>
            <option value="0">故障</option>
            <option value="1">空闲</option>
            <option value="2">使用中</option>
            <option value="3">充电中</option>
            <option value="4">维护中</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-primary w-100" @click="handleSearch">
            <i class="bi bi-search me-1"></i>搜索
          </button>
        </div>
      </div>
    </div>

    <!-- 无人机列表 -->
    <div class="vehicles-grid">
      <div v-for="drone in filteredDrones" :key="drone.id" class="vehicle-card">
        <div class="vehicle-image">
          <img :src="drone.imageUrl" :alt="drone.model" />
          <span class="status-badge" :class="'status-' + drone.status">
            {{ getStatusText(drone.status) }}
          </span>
        </div>
        <div class="vehicle-info">
          <h5>{{ drone.brand }} {{ drone.model }}</h5>
          <p class="vehicle-no">{{ drone.droneNo }}</p>
          <div class="vehicle-metrics">
            <span class="metric">
              <i class="bi bi-battery-half"></i>{{ getDisplayBatteryLevel(drone) }}%
            </span>
            <span class="metric">
              <i class="bi bi-clock"></i>{{ drone.flightHours }}h
            </span>
            <span class="metric">
              <i class="bi bi-geo-alt"></i>{{ drone.locationDetail }}
            </span>
          </div>
          <!-- 充电进度条 -->
          <div v-if="drone.status === 3 && drone.chargingStartTime" class="charging-progress">
            <div class="progress-header">
              <span class="progress-label">充电中</span>
              <span class="progress-time">{{ getChargingTimeRemaining(drone) }}</span>
            </div>
            <div class="progress-bar-bg">
              <div class="progress-bar-fill" :style="{ width: getChargingProgress(drone) + '%' }"></div>
            </div>
          </div>
        </div>
        <div class="vehicle-actions">
          <button class="btn btn-sm btn-outline-primary" @click="handleView(drone)">
            <i class="bi bi-eye"></i>详情
          </button>
          <button class="btn btn-sm btn-outline-warning" @click="handleEdit(drone)">
            <i class="bi bi-pencil"></i>编辑
          </button>
          <button
            class="btn btn-sm"
            :class="drone.status === 3 && drone.chargingStartTime ? 'btn-danger' : 'btn-outline-success'"
            @click="handleCharge(drone)"
          >
            <i class="bi bi-lightning-charge"></i>{{ drone.status === 3 && drone.chargingStartTime ? '停止' : '充电' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 详情模态框 -->
    <div v-if="showViewModal" class="modal-overlay" @click="showViewModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h5>无人机详情</h5>
          <button class="btn-close" @click="showViewModal = false">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body" v-if="selectedDrone">
          <!-- 无人机图片 -->
          <div class="detail-image">
            <img :src="selectedDrone.imageUrl" :alt="selectedDrone.droneNo" />
          </div>
          <div class="detail-row">
            <span class="detail-label">编号:</span>
            <span class="detail-value">{{ selectedDrone.droneNo }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">品牌:</span>
            <span class="detail-value">{{ selectedDrone.brand }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">型号:</span>
            <span class="detail-value">{{ selectedDrone.model }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">状态:</span>
            <span class="detail-value">
              <span class="status-badge-inline" :class="'status-' + selectedDrone.status">
                {{ getStatusText(selectedDrone.status) }}
              </span>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">电量:</span>
            <span class="detail-value">{{ getDisplayBatteryLevel(selectedDrone) }}%</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">飞行时长:</span>
            <span class="detail-value">{{ selectedDrone.flightHours }}小时</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">位置:</span>
            <span class="detail-value">{{ selectedDrone.locationDetail }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showViewModal = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 编辑模态框 -->
    <div v-if="showEditModal" class="modal-overlay" @click="showEditModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h5>编辑无人机</h5>
          <button class="btn-close" @click="showEditModal = false">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">编号</label>
            <input v-model="editForm.droneNo" type="text" class="form-control" disabled />
          </div>
          <div class="mb-3">
            <label class="form-label">品牌</label>
            <input v-model="editForm.brand" type="text" class="form-control" />
          </div>
          <div class="mb-3">
            <label class="form-label">型号</label>
            <input v-model="editForm.model" type="text" class="form-control" />
          </div>
          <div class="mb-3">
            <label class="form-label">状态</label>
            <select v-model="editForm.status" class="form-select">
              <option value="0">故障</option>
              <option value="1">空闲</option>
              <option value="2">使用中</option>
              <option value="3">充电中</option>
              <option value="4">维护中</option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-label">电量 (%)</label>
            <input v-model.number="editForm.batteryLevel" type="number" class="form-control" min="0" max="100" />
          </div>
          <div class="mb-3">
            <label class="form-label">位置</label>
            <input v-model="editForm.locationDetail" type="text" class="form-control" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showEditModal = false">取消</button>
          <button class="btn btn-primary" @click="saveEdit">保存</button>
        </div>
      </div>
    </div>

    <!-- 添加无人机模态框 -->
    <div v-if="showAddModal" class="modal-overlay" @click="cancelAdd">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h5>添加无人机</h5>
          <button class="btn-close" @click="cancelAdd">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">编号</label>
            <input v-model="addForm.droneNo" type="text" class="form-control" placeholder="自动生成或手动输入" />
          </div>
          <div class="row mb-3">
            <div class="col-md-6">
              <label class="form-label">品牌</label>
              <select v-model="addForm.brand" class="form-select">
                <option value="大疆">大疆 (DJI)</option>
                <option value="道通">道通 (Autel)</option>
                <option value="零度">零度</option>
                <option value="极飞">极飞 (XAG)</option>
                <option value="华科尔">华科尔 (Walkera)</option>
                <option value="昊翔">昊翔 (Yuneec)</option>
                <option value="派诺特">派诺特 (Parrot)</option>
                <option value="哈博">哈博 (Hover)</option>
                <option value="亿航">亿航 (EHang)</option>
                <option value="零零">零零 (ZeroZero)</option>
              </select>
            </div>
            <div class="col-md-6">
              <label class="form-label">型号</label>
              <input v-model="addForm.model" type="text" class="form-control" placeholder="如：Mavic 3" />
            </div>
          </div>
          <div class="row mb-3">
            <div class="col-md-6">
              <label class="form-label">状态</label>
              <select v-model="addForm.status" class="form-select">
                <option value="1">空闲</option>
                <option value="0">故障</option>
                <option value="2">使用中</option>
                <option value="3">充电中</option>
                <option value="4">维护中</option>
              </select>
            </div>
            <div class="col-md-6">
              <label class="form-label">电量 (%)</label>
              <input v-model.number="addForm.batteryLevel" type="number" class="form-control" min="0" max="100" />
            </div>
          </div>
          <div class="row mb-3">
            <div class="col-md-6">
              <label class="form-label">飞行时长 (小时)</label>
              <input v-model.number="addForm.flightHours" type="number" class="form-control" min="0" step="0.1" />
            </div>
          </div>
          <div class="mb-3">
            <label class="form-label">地区</label>
            <input v-model="addForm.locationDetail" type="text" class="form-control" placeholder="请输入地区名称" />
            <div v-if="addForm.lat && addForm.lng" class="coords-display">
              <i class="bi bi-geo-alt"></i>
              <span>纬度: {{ addForm.lat.toFixed(4) }}</span>
              <span>经度: {{ addForm.lng.toFixed(4) }}</span>
            </div>
            <div v-else-if="isGeocoding" class="coords-loading">
              <i class="bi bi-arrow-clockwise spin"></i>
              <span>正在获取坐标...</span>
            </div>
            <div v-else class="coords-hint">
              <i class="bi bi-info-circle"></i>
              <span>输入地区名称将自动获取坐标</span>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-label">选择图片</label>
            <div class="image-selector">
              <div
                v-for="(img, index) in availableImages"
                :key="index"
                class="image-option"
                :class="{ 'selected': addForm.imageUrl === img }"
                @click="addForm.imageUrl = img"
              >
                <img :src="img" :alt="'图片' + (index + 1)" />
                <div v-if="addForm.imageUrl === img" class="check-badge">
                  <i class="bi bi-check-lg"></i>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="cancelAdd">取消</button>
          <button class="btn btn-primary" @click="saveAdd">添加</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { getVehicleList, updateVehicle, createVehicle, updateVehicleStatus } from '@/api/operator'

const searchKeyword = ref('')
const currentTime = ref(Date.now()) // 响应式当前时间
const selectedStatus = ref('')
const showAddModal = ref(false)
const showViewModal = ref(false)
const showEditModal = ref(false)
const selectedDrone = ref(null)
const isGeocoding = ref(false)

// 高德地图API Key
const AMAP_API_KEY = 'c692ec51e7cc024d0853e05ea8ee2979'
const GEOCODING_URL = 'https://restapi.amap.com/v3/geocode/geo'

// 地区坐标映射
const locationCoords = {
  '北京朝阳区': { lat: 39.0842, lng: 117.2009 },
  '北京东城区': { lat: 39.1056, lng: 116.4106 },
  '上海浦东': { lat: 31.2304, lng: 121.4737 },
  '上海黄浦区': { lat: 31.2269, lng: 121.4726 },
  '广州天河': { lat: 23.1291, lng: 113.2644 },
  '深圳南山': { lat: 22.5431, lng: 114.0579 },
  '成都武侯': { lat: 30.5728, lng: 104.0668 },
  '杭州西湖': { lat: 30.2741, lng: 120.1551 },
  '西部科技园': { lat: 29.544, lng: 103.765 }
}

const editForm = ref({
  id: null,
  droneNo: '',
  brand: '',
  model: '',
  status: 1,
  batteryLevel: 100,
  locationDetail: '',
  lat: null,
  lng: null
})

// 添加无人机表单
const addForm = ref({
  droneNo: '',
  brand: '大疆',
  model: '',
  status: 1,
  batteryLevel: 100,
  locationDetail: '',
  lat: null,
  lng: null,
  imageUrl: '/img/train/0001.jpg',
  flightHours: 0
})

// 可用的图片列表
const availableImages = [
  '/img/train/0001.jpg',
  '/img/train/0003.jpg',
  '/img/train/0005.jpg',
  '/img/train/0006.jpg',
  '/img/train/0007.jpg',
  '/img/train/0008.jpg'
]

// 使用高德地图REST API进行地理编码
const geocodeLocation = async (address) => {
  if (!address || address.trim().length < 2) return null

  try {
    const url = `${GEOCODING_URL}?key=${AMAP_API_KEY}&address=${encodeURIComponent(address)}`
    const response = await fetch(url)
    const result = await response.json()

    if (result.status === '1' && result.geocodes && result.geocodes.length > 0) {
      const location = result.geocodes[0].location
      if (location) {
        const [lng, lat] = location.split(',')
        return { lat: parseFloat(lat), lng: parseFloat(lng) }
      }
    }
    return null
  } catch (e) {
    console.error('地理编码失败:', e)
    return null
  }
}

// 防抖定时器
let geocodeTimer = null

// 监听添加表单地区变化，自动更新坐标
watch(() => addForm.value.locationDetail, async (newLocation) => {
  if (locationCoords[newLocation]) {
    addForm.value.lat = locationCoords[newLocation].lat
    addForm.value.lng = locationCoords[newLocation].lng
    isGeocoding.value = false
    return
  }

  if (newLocation && newLocation.length > 2) {
    isGeocoding.value = true
    if (geocodeTimer) {
      clearTimeout(geocodeTimer)
    }
    geocodeTimer = setTimeout(async () => {
      const coords = await geocodeLocation(newLocation)
      if (coords) {
        addForm.value.lat = coords.lat
        addForm.value.lng = coords.lng
      } else {
        addForm.value.lat = null
        addForm.value.lng = null
      }
      isGeocoding.value = false
    }, 800)
  } else {
    addForm.value.lat = null
    addForm.value.lng = null
    isGeocoding.value = false
  }
})

const drones = ref([])

// 从API加载无人机数据
const loadVehicles = async () => {
  try {
    const res = await getVehicleList()
    if (res.code === 200 && res.data) {
      drones.value = res.data.map(v => {
        // 处理充电开始时间
        let chargingStartTime = null
        if (v.chargingStartTime) {
          // 后端返回的是LocalDateTime字符串，转换为时间戳
          chargingStartTime = new Date(v.chargingStartTime).getTime()
        }

        // 如果正在充电但没有充电时长，估算一个（假设2小时从起始电量充到100%）
        let chargingDuration = null
        if (v.status === 3 && chargingStartTime && v.startBatteryLevel != null) {
          // 根据起始电量估算充电时长（假设2小时充满）
          const startBattery = v.startBatteryLevel || 0
          const batteryNeeded = 100 - startBattery
          const hoursToFull = 2
          const duration = (batteryNeeded / 100) * hoursToFull * 60 * 60 * 1000
          chargingDuration = duration
        }

        return {
          id: v.id,
          droneNo: v.vehicleNo || v.droneNo,
          brand: v.brand,
          model: v.model,
          status: v.status,
          batteryLevel: v.batteryLevel || 0,
          flightHours: v.flightHours || 0,
          locationDetail: v.locationDetail || '',
          lat: v.latitude,
          lng: v.longitude,
          imageUrl: v.imageUrl || '/img/train/0001.jpg',
          chargingStartTime: chargingStartTime,
          chargingDuration: chargingDuration,
          startBatteryLevel: v.startBatteryLevel || null
        }
      })
    }
  } catch (error) {
    console.error('加载无人机列表失败:', error)
  }
}

const vehicleStats = computed(() => {
  const total = drones.value.length
  const available = drones.value.filter(d => d.status === 1).length
  const charging = drones.value.filter(d => d.status === 3).length
  const fault = drones.value.filter(d => d.status === 0).length
  const inUse = drones.value.filter(d => d.status === 2).length
  return { total, available, charging, fault, inUse }
})

const filteredDrones = computed(() => {
  let result = drones.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(d =>
      d.droneNo.toLowerCase().includes(keyword) ||
      d.brand.toLowerCase().includes(keyword) ||
      d.model.toLowerCase().includes(keyword)
    )
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

const handleSearch = () => {
  // 触发computed重新计算
}

const handleView = (drone) => {
  selectedDrone.value = drone
  showViewModal.value = true
}

const handleEdit = (drone) => {
  editForm.value = {
    id: drone.id,
    droneNo: drone.droneNo,
    brand: drone.brand,
    model: drone.model,
    status: drone.status,
    batteryLevel: drone.batteryLevel,
    locationDetail: drone.locationDetail
  }
  showEditModal.value = true
}

const saveEdit = async () => {
  try {
    const res = await updateVehicle(editForm.value.id, {
      brand: editForm.value.brand,
      model: editForm.value.model,
      status: editForm.value.status,
      batteryLevel: editForm.value.batteryLevel,
      locationDetail: editForm.value.locationDetail
    })
    if (res.code === 200) {
      await loadVehicles() // 重新加载数据
      showEditModal.value = false
    } else {
      alert(res.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    alert('保存失败: ' + (error.response?.data?.message || error.message))
  }
}

const handleCharge = async (drone) => {
  // 如果已经在充电，询问是否停止
  if (drone.status === 3 && drone.chargingStartTime) {
    if (confirm(`${drone.droneNo} 正在充电中，是否停止充电？`)) {
      try {
        const res = await updateVehicleStatus(drone.id, 1) // 改为空闲状态
        if (res.code === 200) {
          await loadVehicles()
        } else {
          alert(res.message || '操作失败')
        }
      } catch (error) {
        console.error('停止充电失败:', error)
        alert('操作失败')
      }
    }
    return
  }

  // 开始充电（系统自动计算充电时长，约2小时充满）
  try {
    const res = await updateVehicleStatus(drone.id, 3)
    if (res.code === 200) {
      await loadVehicles()
      startChargingCheck()
    } else {
      alert(res.message || '操作失败')
    }
  } catch (error) {
    console.error('开始充电失败:', error)
    alert('操作失败')
  }
}

// 充电检查定时器
let chargingCheckInterval = null

// 启动充电检查
const startChargingCheck = () => {
  if (chargingCheckInterval) return

  chargingCheckInterval = setInterval(async () => {
    const now = Date.now()
    let hasCharging = false

    for (const drone of drones.value) {
      if (drone.status === 3 && drone.chargingStartTime && drone.chargingDuration) {
        hasCharging = true
        const elapsed = now - drone.chargingStartTime
        if (elapsed >= drone.chargingDuration) {
          // 充电完成，调用API更新状态
          try {
            await updateVehicleStatus(drone.id, 1)
            // 重新加载数据
            await loadVehicles()
          } catch (e) {
            console.error('充电完成更新失败:', e)
          }
        }
      }
    }

    // 如果没有正在充电的无人机，停止定时器
    if (!hasCharging) {
      clearInterval(chargingCheckInterval)
      chargingCheckInterval = null
    }
  }, 1000) // 每秒检查一次
}

// 获取充电进度百分比
const getChargingProgress = (drone) => {
  if (!drone.chargingStartTime || !drone.chargingDuration) return 0
  const elapsed = currentTime.value - drone.chargingStartTime
  return Math.min(100, Math.max(0, (elapsed / drone.chargingDuration) * 100))
}

// 获取显示的电池电量（充电时动态增长）
const getDisplayBatteryLevel = (drone) => {
  // 如果正在充电，根据进度计算当前电量
  if (drone.status === 3 && drone.chargingStartTime && drone.chargingDuration) {
    const progress = getChargingProgress(drone) / 100
    // 假设充电时从当前电量充到 100%
    const startBattery = drone.startBatteryLevel || drone.batteryLevel
    const currentBattery = Math.round(startBattery + (100 - startBattery) * progress)
    return Math.min(100, currentBattery)
  }
  return drone.batteryLevel
}

// 获取充电剩余时间
const getChargingTimeRemaining = (drone) => {
  if (!drone.chargingStartTime || !drone.chargingDuration) return ''

  const elapsed = currentTime.value - drone.chargingStartTime
  const remaining = Math.max(0, drone.chargingDuration - elapsed)

  const hours = Math.floor(remaining / (60 * 60 * 1000))
  const minutes = Math.floor((remaining % (60 * 60 * 1000)) / (60 * 1000))
  const seconds = Math.floor((remaining % (60 * 1000)) / 1000)

  if (hours > 0) {
    return `剩余 ${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
  return `剩余 ${minutes}:${String(seconds).padStart(2, '0')}`
}

// 打开添加模态框
const openAddModal = () => {
  const maxId = drones.value.length > 0 ? Math.max(...drones.value.map(d => d.id)) : 0
  const newId = maxId + 1
  const prefix = ['E', 'F', 'G', 'H'][Math.floor(Math.random() * 4)]
  addForm.value = {
    droneNo: `DRONE-${prefix}${String(newId).padStart(3, '0')}`,
    brand: '大疆',
    model: '',
    status: 1,
    batteryLevel: 100,
    locationDetail: '',
    lat: null,
    lng: null,
    imageUrl: '/img/train/0001.jpg',
    flightHours: 0
  }
  isGeocoding.value = false
  showAddModal.value = true
}

// 保存新无人机
const saveAdd = async () => {
  if (!addForm.value.brand || !addForm.value.model) {
    alert('请填写品牌和型号')
    return
  }
  if (!addForm.value.locationDetail) {
    alert('请填写地区')
    return
  }
  if (!addForm.value.lat || !addForm.value.lng) {
    alert('请等待坐标获取完成或选择有效的地区')
    return
  }

  try {
    const res = await createVehicle({
      vehicleNo: addForm.value.droneNo,
      brand: addForm.value.brand,
      model: addForm.value.model,
      status: addForm.value.status,
      batteryLevel: addForm.value.batteryLevel,
      locationDetail: addForm.value.locationDetail,
      latitude: addForm.value.lat,
      longitude: addForm.value.lng,
      imageUrl: addForm.value.imageUrl,
      flightHours: addForm.value.flightHours
    })
    if (res.code === 200) {
      await loadVehicles() // 重新加载数据
      showAddModal.value = false
    } else {
      alert(res.message || '添加失败')
    }
  } catch (error) {
    console.error('添加失败:', error)
    alert('添加失败: ' + (error.response?.data?.message || error.message))
  }
}

// 取消添加
const cancelAdd = () => {
  showAddModal.value = false
}

// 时间更新定时器
let timeUpdateInterval = null

// 页面加载时从API加载数据
onMounted(async () => {
  await loadVehicles()
  // 每秒更新当前时间，触发进度条和剩余时间更新
  timeUpdateInterval = setInterval(() => {
    currentTime.value = Date.now()
  }, 1000)
  // 检查是否有正在充电的无人机，启动定时检查
  const hasCharging = drones.value.some(d => d.status === 3 && d.chargingStartTime)
  if (hasCharging) {
    startChargingCheck()
  }
})

// 页面卸载时清理定时器
onUnmounted(() => {
  if (timeUpdateInterval) {
    clearInterval(timeUpdateInterval)
  }
  if (chargingCheckInterval) {
    clearInterval(chargingCheckInterval)
  }
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

.stat-item.success .stat-value { color: #10b981; }
.stat-item.warning .stat-value { color: #f59e0b; }
.stat-item.danger .stat-value { color: #ef4444; }

.filter-bar {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.row {
  display: flex;
  flex-wrap: wrap;
  margin: -8px;
}

.g-3 > * {
  padding: 8px;
}

.col-md-4 {
  flex: 0 0 33.333333%;
  max-width: 33.333333%;
  padding: 8px;
}

.col-md-2 {
  flex: 0 0 16.666667%;
  max-width: 16.666667%;
  padding: 8px;
}

.form-control, .form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
}

.form-control:focus, .form-select:focus {
  outline: none;
  border-color: #059669;
  box-shadow: 0 0 0 3px rgba(5, 150, 105, 0.1);
}

.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.vehicle-card {
  background: white;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: all 0.3s;
}

.vehicle-card:hover {
  box-shadow: 0 8px 20px rgba(0,0,0,0.1);
  transform: translateY(-3px);
}

.vehicle-image {
  position: relative;
  height: 180px;
}

.vehicle-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
}

.status-0 { background: #fee2e2; color: #dc2626; }
.status-1 { background: #d1fae5; color: #059669; }
.status-2 { background: #dbeafe; color: #2563eb; }
.status-3 { background: #fef3c7; color: #d97706; }
.status-4 { background: #e5e7eb; color: #6b7280; }

.vehicle-info {
  padding: 16px;
}

.vehicle-info h5 {
  font-weight: 600;
  margin-bottom: 4px;
}

.vehicle-no {
  color: #64748b;
  font-size: 13px;
  margin-bottom: 12px;
}

.vehicle-metrics {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.metric {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 充电进度条样式 */
.charging-progress {
  margin-top: 12px;
  padding: 10px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-radius: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.progress-label {
  font-size: 12px;
  font-weight: 600;
  color: #d97706;
  display: flex;
  align-items: center;
  gap: 4px;
}

.progress-label::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #f59e0b;
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

.progress-time {
  font-size: 11px;
  color: #b45309;
  font-weight: 500;
}

.progress-bar-bg {
  width: 100%;
  height: 8px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
  border-radius: 4px;
  transition: width 0.5s ease;
  position: relative;
}

.progress-bar-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.vehicle-actions {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #e2e8f0;
}

.vehicle-actions .btn {
  flex: 1;
}

.btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #059669, #10b981);
  color: white;
}

.btn-primary:hover {
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
}

.btn-secondary {
  background: #64748b;
  color: white;
}

.btn-outline-primary {
  background: transparent;
  border: 1px solid #059669;
  color: #059669;
}

.btn-outline-primary:hover {
  background: #059669;
  color: white;
}

.btn-outline-warning {
  background: transparent;
  border: 1px solid #f59e0b;
  color: #f59e0b;
}

.btn-outline-warning:hover {
  background: #f59e0b;
  color: white;
}

.btn-outline-success {
  background: transparent;
  border: 1px solid #10b981;
  color: #10b981;
}

.btn-outline-success:hover {
  background: #10b981;
  color: white;
}

.btn-danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
}

.btn-danger:hover {
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.w-100 {
  width: 100%;
}

.mb-3 {
  margin-bottom: 16px;
}

/* 模态框样式 */
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
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0,0,0,0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h5 {
  margin: 0;
  font-weight: 600;
}

.btn-close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.btn-close:hover {
  color: #1e293b;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
}

.detail-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  width: 100px;
  font-weight: 500;
  color: #64748b;
}

.detail-value {
  flex: 1;
  color: #1e293b;
}

/* 详情中的无人机图片 */
.detail-image {
  width: 100%;
  height: 180px;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 16px;
}

.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge-inline {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #1e293b;
  font-size: 14px;
}

/* 坐标显示样式 */
.coords-display {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #d1fae5, #a7f3d0);
  border-radius: 8px;
  font-size: 13px;
  color: #059669;
  font-weight: 500;
}

.coords-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f1f5f9;
  border-radius: 8px;
  font-size: 12px;
  color: #64748b;
}

.coords-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #fff7ed;
  border-radius: 8px;
  font-size: 12px;
  color: #f59e0b;
}

.coords-loading .spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 图片选择器样式 */
.image-selector {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.image-option {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 3px solid #e2e8f0;
  transition: all 0.2s;
}

.image-option:hover {
  border-color: #059669;
  transform: scale(1.05);
}

.image-option.selected {
  border-color: #059669;
  box-shadow: 0 0 0 3px rgba(5, 150, 105, 0.2);
}

.image-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.check-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  background: #059669;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
}

.col-md-6 {
  flex: 0 0 50%;
  max-width: 50%;
  padding: 8px;
}
</style>
