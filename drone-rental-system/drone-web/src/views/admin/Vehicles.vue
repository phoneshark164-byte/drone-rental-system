<template>
  <div class="admin-vehicles">
    <div class="page-header">
      <h3>无人机管理</h3>
      <button class="btn btn-primary" @click="openAddModal">
        <i class="bi bi-plus-lg me-1"></i>添加无人机
      </button>
    </div>

    <!-- 统计卡片 -->
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
        <span class="stat-value">{{ vehicleStats.inUse }}</span>
        <span class="stat-label">使用中</span>
      </div>
      <div class="stat-item danger">
        <span class="stat-value">{{ vehicleStats.fault }}</span>
        <span class="stat-label">故障</span>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-bar">
      <div class="row g-3">
        <div class="col-md-3">
          <input
            v-model="searchKeyword"
            type="text"
            class="form-control"
            placeholder="搜索编号、品牌"
          />
        </div>
        <div class="col-md-2">
          <select v-model="selectedBrand" class="form-select">
            <option value="">全部品牌</option>
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

    <!-- 无人机表格 -->
    <div class="table-container">
      <table class="table table-hover">
        <thead>
          <tr>
            <th>编号</th>
            <th>图片</th>
            <th>品牌型号</th>
            <th>位置</th>
            <th>电量</th>
            <th>飞行时长</th>
            <th>状态</th>
            <th>上架</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="drone in paginatedDrones" :key="drone.id">
            <td>{{ drone.droneNo }}</td>
            <td>
              <img :src="drone.imageUrl" class="vehicle-thumb" />
            </td>
            <td>
              <div class="vehicle-info">
                <strong>{{ drone.brand }}</strong>
                <span class="text-muted">{{ drone.model }}</span>
              </div>
            </td>
            <td>{{ drone.locationDetail }}</td>
            <td>
              <div class="battery-level">
                <div class="battery-bar">
                  <div class="battery-fill" :style="{ width: getDisplayBatteryLevel(drone) + '%' }"></div>
                </div>
                <span>{{ getDisplayBatteryLevel(drone) }}%</span>
              </div>
              <!-- 充电进度条 -->
              <div v-if="drone.status === 3 && drone.chargingStartTime" class="charging-progress-inline">
                <div class="charging-info">
                  <span class="charging-label">充电中 {{ getChargingProgress(drone).toFixed(0) }}%</span>
                  <span class="charging-time">{{ getChargingTimeRemaining(drone) }}</span>
                </div>
              </div>
            </td>
            <td>{{ drone.flightHours }}h</td>
            <td>
              <span class="badge" :class="getStatusBadgeClass(drone.status)">
                {{ getStatusText(drone.status) }}
              </span>
            </td>
            <td>
              <span
                class="listed-badge"
                :class="drone.isListed === 1 ? 'listed' : 'unlisted'"
                @click="toggleListed(drone)"
              >
                {{ drone.isListed === 1 ? '已上架' : '已下架' }}
              </span>
            </td>
            <td>
              <div class="btn-group">
                <button class="btn btn-sm btn-outline-primary" @click="handleView(drone)">
                  <i class="bi bi-eye"></i>
                </button>
                <button class="btn btn-sm btn-outline-warning" @click="handleEdit(drone)">
                  <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" @click="handleDelete(drone)">
                  <i class="bi bi-trash"></i>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <nav class="pagination-nav" v-if="totalPages > 1">
      <ul class="pagination justify-content-center">
        <li class="page-item" :class="{ disabled: currentPage === 1 }">
          <a class="page-link" href="#" @click.prevent="goToPage(currentPage - 1)">上一页</a>
        </li>
        <li
          v-for="page in totalPages"
          :key="page"
          class="page-item"
          :class="{ active: currentPage === page }"
        >
          <a class="page-link" href="#" @click.prevent="goToPage(page)">{{ page }}</a>
        </li>
        <li class="page-item" :class="{ disabled: currentPage === totalPages }">
          <a class="page-link" href="#" @click.prevent="goToPage(currentPage + 1)">下一页</a>
        </li>
      </ul>
    </nav>

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
              <span class="status-badge-inline" :class="getStatusBadgeClass(selectedDrone.status)">
                {{ getStatusText(selectedDrone.status) }}
              </span>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">电量:</span>
            <span class="detail-value">{{ getDisplayBatteryLevel(selectedDrone) }}%</span>
          </div>
          <!-- 充电进度 -->
          <div v-if="selectedDrone.status === 3 && selectedDrone.chargingStartTime" class="detail-row">
            <span class="detail-label">充电进度:</span>
            <span class="detail-value">
              <div class="charging-progress-modal">
                <div class="charging-bar-modal">
                  <div class="charging-fill-modal" :style="{ width: getChargingProgress(selectedDrone) + '%' }"></div>
                </div>
                <span class="charging-text">{{ getChargingProgress(selectedDrone).toFixed(0) }}% - {{ getChargingTimeRemaining(selectedDrone) }}</span>
              </div>
            </span>
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
            <label class="form-label">飞行时长 (小时)</label>
            <input v-model.number="editForm.flightHours" type="number" class="form-control" min="0" step="0.1" />
          </div>
          <div class="mb-3">
            <label class="form-label">地区</label>
            <input v-model="editForm.locationDetail" type="text" class="form-control" placeholder="请输入地区名称，如：北京朝阳区、四川省广汉市三星堆博物馆" />
            <div v-if="editForm.lat && editForm.lng" class="coords-display">
              <i class="bi bi-geo-alt"></i>
              <span>纬度: {{ editForm.lat.toFixed(4) }}</span>
              <span>经度: {{ editForm.lng.toFixed(4) }}</span>
            </div>
            <div v-else-if="isGeocoding" class="coords-loading">
              <i class="bi bi-arrow-clockwise spin"></i>
              <span>正在获取坐标...</span>
            </div>
            <div v-else class="coords-hint">
              <i class="bi bi-info-circle"></i>
              <span>输入地区名称将自动获取坐标（支持全国任意地址）</span>
            </div>
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
            <input v-model="addForm.locationDetail" type="text" class="form-control" placeholder="请输入地区名称，如：北京朝阳区、四川省广汉市三星堆博物馆" />
            <div v-if="addForm.lat && addForm.lng" class="coords-display">
              <i class="bi bi-geo-alt"></i>
              <span>纬度: {{ addForm.lat.toFixed(4) }}</span>
              <span>经度: {{ addForm.lng.toFixed(4) }}</span>
            </div>
            <div v-else-if="isAddGeocoding" class="coords-loading">
              <i class="bi bi-arrow-clockwise spin"></i>
              <span>正在获取坐标...</span>
            </div>
            <div v-else class="coords-hint">
              <i class="bi bi-info-circle"></i>
              <span>输入地区名称将自动获取坐标（支持全国任意地址）</span>
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
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { createVehicle, deleteVehicle, getVehicleList, listVehicle, unlistVehicle, updateVehicle, getDashboardStats } from '@/api/admin'

const currentTime = ref(Date.now()) // 响应式当前时间
let timeUpdateInterval = null

onMounted(() => {
  // 清除旧的 localStorage 数据（迁移到数据库后不再需要）
  localStorage.removeItem('adminDrones')

  // 从API加载无人机列表和统计数据
  loadVehicles()
  loadStats()
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

const searchKeyword = ref('')
const selectedBrand = ref('')
const selectedStatus = ref('')
const showAddModal = ref(false)
const showViewModal = ref(false)
const showEditModal = ref(false)
const selectedDrone = ref(null)
const isGeocoding = ref(false)
const isAddGeocoding = ref(false) // 添加模态框的地理编码状态

// 分页
const currentPage = ref(1)
const pageSize = 3 // 每页显示数量

// 高德地图API Key（与涛行电瓶车系统相同）
const AMAP_API_KEY = 'c692ec51e7cc024d0853e05ea8ee2979'
const GEOCODING_URL = 'https://restapi.amap.com/v3/geocode/geo'

// 地区坐标映射
const locationCoords = {
  '北京朝阳区': { lat: 39.0842, lng: 117.2009 },
  '北京东城区': { lat: 39.1056, lng: 116.4106 },
  '北京海淀区': { lat: 39.9593, lng: 116.2985 },
  '上海浦东': { lat: 31.2304, lng: 121.4737 },
  '上海黄浦区': { lat: 31.2269, lng: 121.4726 },
  '上海静安区': { lat: 31.2286, lng: 121.4449 },
  '广州天河': { lat: 23.1291, lng: 113.2644 },
  '广州天河区': { lat: 23.1291, lng: 113.2644 },
  '深圳南山': { lat: 22.5431, lng: 114.0579 },
  '深圳南山区': { lat: 22.5431, lng: 114.0579 },
  '成都武侯': { lat: 30.5728, lng: 104.0668 },
  '成都武侯区': { lat: 30.5728, lng: 104.0668 },
  '杭州西湖': { lat: 30.2741, lng: 120.1551 },
  '杭州西湖区': { lat: 30.2741, lng: 120.1551 },
  '重庆渝中': { lat: 29.5630, lng: 106.5516 },
  '重庆渝中区': { lat: 29.5630, lng: 106.5516 },
  '西安雁塔': { lat: 34.3416, lng: 108.9398 },
  '西安雁塔区': { lat: 34.3416, lng: 108.9398 },
  '青岛市南': { lat: 36.0671, lng: 120.3826 },
  '青岛市南区': { lat: 36.0671, lng: 120.3826 },
  '南京鼓楼': { lat: 32.0603, lng: 118.7969 },
  '南京鼓楼区': { lat: 32.0603, lng: 118.7969 },
  '四川大学锦江学院': { lat: 29.544, lng: 103.765 }
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
  lng: null,
  isListed: 1,
  flightHours: 0
})
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

// 使用高德地图REST API进行地理编码（与涛行电瓶车系统相同的方式）
const geocodeLocation = async (address) => {
  if (!address || address.trim().length < 2) return null

  try {
    const url = `${GEOCODING_URL}?key=${AMAP_API_KEY}&address=${encodeURIComponent(address)}`
    const response = await fetch(url)
    const result = await response.json()

    console.log('地理编码响应:', result)

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
let addGeocodeTimer = null

// 监听地区变化，自动更新坐标（编辑表单）
watch(() => editForm.value.locationDetail, async (newLocation) => {
  // 先检查预设映射
  if (locationCoords[newLocation]) {
    editForm.value.lat = locationCoords[newLocation].lat
    editForm.value.lng = locationCoords[newLocation].lng
    isGeocoding.value = false
    return
  }

  // 如果是预设之外的地址，使用地理编码
  if (newLocation && newLocation.length > 2) {
    isGeocoding.value = true
    // 清除之前的定时器
    if (geocodeTimer) {
      clearTimeout(geocodeTimer)
    }
    // 防抖，等待用户输入完成
    geocodeTimer = setTimeout(async () => {
      const coords = await geocodeLocation(newLocation)
      if (coords) {
        editForm.value.lat = coords.lat
        editForm.value.lng = coords.lng
      } else {
        // 未找到坐标，清空
        editForm.value.lat = null
        editForm.value.lng = null
      }
      isGeocoding.value = false
    }, 800)
  } else {
    editForm.value.lat = null
    editForm.value.lng = null
    isGeocoding.value = false
  }
})

// 监听地区变化，自动更新坐标（添加表单）
watch(() => addForm.value.locationDetail, async (newLocation) => {
  // 先检查预设映射
  if (locationCoords[newLocation]) {
    addForm.value.lat = locationCoords[newLocation].lat
    addForm.value.lng = locationCoords[newLocation].lng
    isAddGeocoding.value = false
    return
  }

  // 如果是预设之外的地址，使用地理编码
  if (newLocation && newLocation.length > 2) {
    isAddGeocoding.value = true
    // 清除之前的定时器
    if (addGeocodeTimer) {
      clearTimeout(addGeocodeTimer)
    }
    // 防抖，等待用户输入完成
    addGeocodeTimer = setTimeout(async () => {
      const coords = await geocodeLocation(newLocation)
      if (coords) {
        addForm.value.lat = coords.lat
        addForm.value.lng = coords.lng
      } else {
        // 未找到坐标，清空
        addForm.value.lat = null
        addForm.value.lng = null
      }
      isAddGeocoding.value = false
    }, 800)
  } else {
    addForm.value.lat = null
    addForm.value.lng = null
    isAddGeocoding.value = false
  }
})

const vehicleStats = ref({
  total: 0,
  available: 0,
  inUse: 0,
  fault: 0,
  charging: 0
})

// 加载统计数据
const loadStats = async () => {
  try {
    console.log('正在加载统计数据...')
    const res = await getDashboardStats()
    console.log('统计数据响应:', res)
    if (res.code === 200 && res.data) {
      vehicleStats.value = {
        total: res.data.vehicleCount || 0,
        available: res.data.vehicleAvailable || 0,
        inUse: res.data.vehicleInUse || 0,
        fault: res.data.vehicleFault || 0,
        charging: res.data.vehicleCharging || 0
      }
      console.log('更新后的统计数据:', vehicleStats.value)
    } else {
      console.warn('统计数据返回异常:', res)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const drones = ref([])

// 从API加载无人机列表
const loadVehicles = async () => {
  try {
    const res = await getVehicleList({})
    if (res.code === 200 && res.data) {
      drones.value = res.data.map(v => ({
        id: v.id,
        droneNo: v.vehicleNo, // 后端返回的是vehicleNo
        brand: v.brand,
        model: v.model,
        imageUrl: v.imageUrl,
        locationDetail: v.locationDetail,
        lat: v.latitude,
        lng: v.longitude,
        batteryLevel: v.batteryLevel,
        flightHours: v.flightHours || 0,
        status: v.status,
        isListed: v.isListed
      }))
    }
  } catch (error) {
    console.error('加载无人机列表失败:', error)
  }
}

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

  if (selectedBrand.value) {
    result = result.filter(d => d.brand === selectedBrand.value)
  }

  if (selectedStatus.value !== '') {
    result = result.filter(d => d.status === parseInt(selectedStatus.value))
  }

  return result
})

// 分页后的数据
const paginatedDrones = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return filteredDrones.value.slice(start, end)
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredDrones.value.length / pageSize)
})

// 切换页码
const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 监听筛选条件变化，重置到第一页
watch([searchKeyword, selectedBrand, selectedStatus], () => {
  currentPage.value = 1
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

const getStatusBadgeClass = (status) => {
  const classMap = {
    0: 'bg-danger',
    1: 'bg-success',
    2: 'bg-primary',
    3: 'bg-info',
    4: 'bg-warning'
  }
  return classMap[status] || 'bg-secondary'
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

const handleSearch = () => {
  // 触发computed重新计算
}

const handleView = (drone) => {
  selectedDrone.value = drone
  showViewModal.value = true
}

const handleEdit = (drone) => {
  isGeocoding.value = false
  editForm.value = {
    id: drone.id,
    droneNo: drone.droneNo,
    brand: drone.brand,
    model: drone.model,
    status: drone.status,
    batteryLevel: drone.batteryLevel,
    locationDetail: drone.locationDetail,
    lat: drone.lat || null,
    lng: drone.lng || null,
    isListed: drone.isListed ?? 1,
    flightHours: drone.flightHours || 0
  }
  showEditModal.value = true
}

const saveEdit = async () => {
  try {
    const res = await updateVehicle(editForm.value.id, {
      vehicleNo: editForm.value.droneNo,
      brand: editForm.value.brand,
      model: editForm.value.model,
      color: editForm.value.color || '',
      imageUrl: editForm.value.imageUrl,
      batteryLevel: editForm.value.batteryLevel,
      latitude: editForm.value.lat,
      longitude: editForm.value.lng,
      locationDetail: editForm.value.locationDetail,
      status: parseInt(editForm.value.status) || 1,
      flightHours: editForm.value.flightHours || 0,
      isListed: editForm.value.isListed ?? 1
    })

    if (res.code === 200) {
      alert('修改成功')
      showEditModal.value = false
      // 刷新列表和统计
      loadVehicles()
      loadStats()
    } else {
      alert(res.message || '修改失败')
    }
  } catch (error) {
    console.error('修改失败:', error)
    alert('修改失败，请重试')
  }
}

const handleDelete = async (drone) => {
  if (confirm(`确定要删除无人机 ${drone.droneNo} 吗？`)) {
    try {
      const res = await deleteVehicle(drone.id)
      if (res.code === 200) {
        // 从本地数组中移除
        const index = drones.value.findIndex(d => d.id === drone.id)
        if (index > -1) {
          drones.value.splice(index, 1)
        }
        alert('删除成功')
        // 刷新统计
        loadStats()
      } else {
        alert(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除失败:', error)
      alert('删除失败，请重试')
    }
  }
}

// 打开添加模态框
const openAddModal = () => {
  // 生成新的编号
  const maxId = drones.value.length > 0 ? Math.max(...drones.value.map(d => d.id)) : 0
  const newId = maxId + 1
  const prefix = ['A', 'B', 'C', 'D', 'E'][Math.floor(Math.random() * 5)]
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
  isAddGeocoding.value = false
  showAddModal.value = true
}

// 保存新无人机
const saveAdd = async () => {
  // 验证必填字段
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
    // 调用后端API创建无人机
    const res = await createVehicle({
      vehicleNo: addForm.value.droneNo,
      brand: addForm.value.brand,
      model: addForm.value.model,
      color: addForm.value.color || '',
      imageUrl: addForm.value.imageUrl,
      batteryLevel: addForm.value.batteryLevel,
      latitude: addForm.value.lat,
      longitude: addForm.value.lng,
      locationDetail: addForm.value.locationDetail,
      status: addForm.value.status,
      isListed: 1
    })

    if (res.code === 200) {
      alert('添加成功')
      showAddModal.value = false
      // 刷新列表和统计
      loadVehicles()
      loadStats()
    } else {
      alert(res.message || '添加失败')
    }
  } catch (error) {
    console.error('添加失败:', error)
    alert('添加失败，请重试')
  }
}

// 取消添加
const cancelAdd = () => {
  showAddModal.value = false
}

// 切换上架/下架状态
const toggleListed = async (drone) => {
  const newStatus = drone.isListed === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'

  if (confirm(`确定要${action}无人机 ${drone.droneNo} 吗？`)) {
    try {
      // 调用后端API
      await (newStatus === 1 ? listVehicle(drone.id) : unlistVehicle(drone.id))

      // 更新本地状态
      drone.isListed = newStatus
      // 刷新统计
      loadStats()
    } catch (error) {
      console.error(`${action}失败:`, error)
      alert(`${action}失败，请重试`)
    }
  }
}
</script>

<style scoped>
.admin-vehicles {
  position: relative;
  z-index: 1;
}

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
  gap: 20px;
  margin-bottom: 20px;
}

.stat-item {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  color: #64748b;
  font-size: 14px;
}

.stat-item.success .stat-value { color: #10b981; }
.stat-item.warning .stat-value { color: #f59e0b; }
.stat-item.danger .stat-value { color: #ef4444; }

.filter-bar {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.table-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  overflow-x: auto;
}

.vehicle-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
}

.vehicle-info {
  display: flex;
  flex-direction: column;
}

.vehicle-info .text-muted {
  font-size: 12px;
}

.battery-level {
  display: flex;
  align-items: center;
  gap: 8px;
}

.battery-bar {
  width: 40px;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.battery-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #2563eb);
  transition: width 0.3s;
}

/* 充电进度条样式 */
.charging-progress-inline {
  margin-top: 6px;
  font-size: 11px;
}

.charging-progress-inline .charging-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.charging-label {
  color: #d97706;
  font-weight: 500;
}

.charging-time {
  color: #f59e0b;
  font-size: 10px;
}

/* 详情模态框中的充电进度 */
.charging-progress-modal {
  width: 100%;
}

.charging-bar-modal {
  width: 100%;
  height: 10px;
  background: #e2e8f0;
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 6px;
}

.charging-fill-modal {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #2563eb);
  transition: width 0.5s ease;
}

.charging-text {
  font-size: 12px;
  color: #64748b;
}

.table th {
  font-weight: 600;
  color: #64748b;
  font-size: 13px;
  text-transform: uppercase;
}

.table td {
  vertical-align: middle;
}

.pagination-nav {
  margin-top: 20px;
}

.pagination {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 4px;
}

.page-item {
  display: block;
}

.page-link {
  display: block;
  padding: 8px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #475569;
  text-decoration: none;
  transition: all 0.2s;
  background: white;
  min-width: 40px;
  text-align: center;
}

.page-link:hover {
  background: #f1f5f9;
  color: #2563eb;
  border-color: #bfdbfe;
}

.page-item.active .page-link {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
  border-color: transparent;
}

.page-item.disabled .page-link {
  color: #cbd5e1;
  pointer-events: none;
  background: #f8fafc;
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

.status-badge-inline {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge-inline.bg-success {
  background: #dbeafe;
  color: #2563eb;
}

.status-badge-inline.bg-primary {
  background: #dbeafe;
  color: #2563eb;
}

.status-badge-inline.bg-danger {
  background: #fee2e2;
  color: #dc2626;
}

.status-badge-inline.bg-warning {
  background: #fef3c7;
  color: #d97706;
}

.status-badge-inline.bg-info {
  background: #dbeafe;
  color: #3b82f6;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #1e293b;
  font-size: 14px;
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
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.coords-display {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  border-radius: 8px;
  font-size: 13px;
  color: #2563eb;
  font-weight: 500;
}

.coords-display i {
  font-size: 16px;
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

.coords-hint i {
  font-size: 14px;
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

.btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid;
  display: inline-block;
  text-align: center;
}

.btn-primary {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
}

.btn-primary:hover {
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.btn-secondary {
  background: #64748b;
  color: white;
}

.btn-outline-primary {
  background: transparent !important;
  border: 1px solid #3b82f6 !important;
  color: #3b82f6 !important;
}

.btn-outline-primary:hover {
  background: #3b82f6;
  color: white;
}

.btn-outline-warning {
  background: transparent !important;
  border: 1px solid #f59e0b !important;
  color: #f59e0b !important;
}

.btn-outline-warning:hover {
  background: #f59e0b;
  color: white;
}

.btn-outline-danger {
  background: transparent !important;
  border: 1px solid #ef4444 !important;
  color: #ef4444 !important;
}

.btn-outline-danger:hover {
  background: #ef4444;
  color: white;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.btn-group {
  display: flex;
  gap: 6px;
}

.me-1 {
  margin-right: 4px;
}

.mb-3 {
  margin-bottom: 16px;
}

.row {
  display: flex;
  flex-wrap: wrap;
  margin: -8px;
}

.g-3 > * {
  padding: 8px;
}

.col-md-3 {
  flex: 0 0 25%;
  max-width: 25%;
  padding: 8px;
}

.col-md-2 {
  flex: 0 0 16.666667%;
  max-width: 16.666667%;
  padding: 8px;
}

.w-100 {
  width: 100%;
}

.justify-content-center {
  justify-content: center;
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
  border-color: #3b82f6;
  transform: scale(1.05);
}

.image-option.selected {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
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
  background: #3b82f6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
}

.listed-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
}

.listed-badge.listed {
  background: #d1fae5;
  color: #059669;
}

.listed-badge.unlisted {
  background: #f3f4f6;
  color: #6b7280;
}

.listed-badge:hover {
  opacity: 0.8;
}

.col-md-6 {
  flex: 0 0 50%;
  max-width: 50%;
  padding: 8px;
}
</style>
