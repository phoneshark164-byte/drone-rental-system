<template>
  <div class="admin-dashboard">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">控制台</h1>
        <p class="page-subtitle">欢迎回来，管理员</p>
      </div>
      <div class="d-flex gap-2">
        <span class="text-muted">{{ currentDate }}</span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">
          <i class="bi bi-people"></i>
        </div>
        <div class="stat-value">{{ stats.userCount }}</div>
        <div class="stat-label">总用户数</div>
        <div class="stat-trend up">
          <i class="bi bi-arrow-up"></i>
          <span>实时</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <i class="bi bi-airplane"></i>
        </div>
        <div class="stat-value">{{ stats.vehicleCount }}</div>
        <div class="stat-label">无人机总数</div>
        <div class="stat-trend up">
          <i class="bi bi-arrow-up"></i>
          <span>实时</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <i class="bi bi-receipt"></i>
        </div>
        <div class="stat-value">{{ stats.orderCount }}</div>
        <div class="stat-label">订单总数</div>
        <div class="stat-trend up">
          <i class="bi bi-arrow-up"></i>
          <span>实时</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <i class="bi bi-currency-yen"></i>
        </div>
        <div class="stat-value">¥{{ stats.monthlyIncome }}</div>
        <div class="stat-label">本月收入</div>
        <div class="stat-trend up">
          <i class="bi bi-arrow-up"></i>
          <span>实时</span>
        </div>
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
        <div id="adminMapContainer" class="map-container"></div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-grid">
      <div class="chart-card">
        <h5 class="chart-title">
          <i class="bi bi-graph-up me-2" style="color: var(--primary-color);"></i>
          近7天订单趋势
        </h5>
        <div id="orderTrendChart" class="chart-container"></div>
      </div>
      <div class="chart-card">
        <h5 class="chart-title">
          <i class="bi bi-pie-chart me-2" style="color: var(--accent-color);"></i>
          无人机状态分布
        </h5>
        <div id="vehicleStatusChart" class="chart-container"></div>
      </div>
    </div>

    <!-- 最近订单 -->
    <div class="table-card">
      <div class="table-card-header">
        <h5 class="table-card-title">
          <i class="bi bi-clock-history me-2" style="color: var(--primary-color);"></i>
          最近订单
        </h5>
        <router-link to="/admin/orders" class="btn btn-outline-primary btn-sm">查看全部</router-link>
      </div>
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户</th>
              <th>无人机</th>
              <th>金额</th>
              <th>状态</th>
              <th>下单时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loadingOrders">
              <td colspan="6" class="text-center py-4">
                <div class="spinner-border spinner-border-sm me-2"></div>
                正在加载...
              </td>
            </tr>
            <tr v-else-if="recentOrders.length === 0">
              <td colspan="6" class="text-center py-4">暂无订单数据</td>
            </tr>
            <tr v-else v-for="order in recentOrders" :key="order.id">
              <td><span class="fw-semibold">{{ order.shortOrderNo }}</span></td>
              <td>{{ order.userName }}</td>
              <td>{{ order.vehicleNo }}</td>
              <td class="fw-semibold success-text">¥{{ order.amount }}</td>
              <td><span class="badge" :class="'badge-' + order.statusClass">{{ order.statusText }}</span></td>
              <td class="text-muted">{{ order.createTime }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getVehicleMapData, getDashboardStats, getOrderTrends, getVehicleStatusDistribution, getRecentOrders } from '@/api/admin'
import { getUserInfo, clearLoginInfo } from '@/utils/auth'
import * as echarts from 'echarts'

// 当前时间 - 用于充电进度实时更新
const currentTime = ref(Date.now())
let timeUpdateInterval = null

// 统计数据
const stats = ref({
  userCount: '-',
  vehicleCount: '-',
  orderCount: '-',
  monthlyIncome: '-'
})

// 当前日期
const currentDate = ref('')
const vehicleFilter = ref('all')
const loadingOrders = ref(true)

// 地图相关
let map = null
let markers = []
let infoWindow = null
let autoRefreshTimer = null

// 图表实例
let orderTrendChart = null
let vehicleStatusChart = null

// 最近订单
const recentOrders = ref([])

// 模拟无人机位置数据
const mockVehicleData = ref([
  { id: 1, droneNo: 'DRONE-A001', brand: '大疆', model: 'Mavic 3', imageUrl: '/img/train/0001.jpg', lat: 39.0842, lng: 117.2009, batteryLevel: 85, status: 'available', location: '北京朝阳区' },
  { id: 2, droneNo: 'DRONE-A002', brand: '大疆', model: 'Air 3', imageUrl: '/img/train/0003.jpg', lat: 31.2304, lng: 121.4737, batteryLevel: 92, status: 'available', location: '上海浦东' },
  { id: 3, droneNo: 'DRONE-B001', brand: '道通', model: 'EVO II', imageUrl: '/img/train/0005.jpg', lat: 23.1291, lng: 113.2644, batteryLevel: 78, status: 'available', location: '广州天河' },
  { id: 4, droneNo: 'DRONE-B002', brand: '大疆', model: 'Mini 3', imageUrl: '/img/train/0006.jpg', lat: 22.5431, lng: 114.0579, batteryLevel: 45, status: 'in-use', location: '深圳南山' },
  { id: 5, droneNo: 'DRONE-C001', brand: '大疆', model: 'Avata', imageUrl: '/img/train/0007.jpg', lat: 30.5728, lng: 104.0668, batteryLevel: 88, status: 'available', location: '成都武侯' },
  { id: 6, droneNo: 'DRONE-C002', brand: '道通', model: 'Lite+', imageUrl: '/img/train/0008.jpg', lat: 30.2741, lng: 120.1551, batteryLevel: 95, status: 'available', location: '杭州西湖' },
  { id: 7, droneNo: 'DRONE-D001', brand: '大疆', model: 'Mavic Air', imageUrl: '/img/train/0001.jpg', lat: 29.5630, lng: 106.5516, batteryLevel: 30, status: 'available', location: '重庆渝中' },
  { id: 8, droneNo: 'DRONE-D002', brand: '道通', model: 'EVO Nano', imageUrl: '/img/train/0003.jpg', lat: 34.3416, lng: 108.9398, batteryLevel: 25, status: 'repair', location: '西安雁塔' },
  { id: 9, droneNo: 'DRONE-E001', brand: '大疆', model: 'Mini SE', imageUrl: '/img/train/0005.jpg', lat: 36.0671, lng: 120.3826, batteryLevel: 88, status: 'in-use', location: '青岛市南' },
  { id: 10, droneNo: 'DRONE-E002', brand: '大疆', model: 'Air 2S', imageUrl: '/img/train/0006.jpg', lat: 32.0603, lng: 118.7969, batteryLevel: 75, status: 'available', location: '南京鼓楼' }
])

// 从API同步无人机数据
const syncDroneData = async () => {
  try {
    const res = await getVehicleMapData()
    if (res.code === 200 && res.data) {
      // 将无人机管理的状态码映射到地图状态
      const statusMap = {
        0: 'repair',
        1: 'available',
        2: 'in-use',
        3: 'charging',
        4: 'maintenance'
      }

      // 清空现有数据，用API数据替换
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

// 加载统计数据
const loadDashboardStats = async () => {
  try {
    const res = await getDashboardStats()
    if (res.code === 200 && res.data) {
      stats.value = {
        userCount: res.data.userCount !== undefined ? Number(res.data.userCount).toLocaleString() : '-',
        vehicleCount: res.data.vehicleCount !== undefined ? Number(res.data.vehicleCount).toLocaleString() : '-',
        orderCount: res.data.orderCount !== undefined ? Number(res.data.orderCount).toLocaleString() : '-',
        monthlyIncome: res.data.monthlyIncome !== undefined ? Number(res.data.monthlyIncome).toLocaleString() : '-'
      }
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    stats.value = {
      userCount: '-',
      vehicleCount: '-',
      orderCount: '-',
      monthlyIncome: '-'
    }
  }
}

// 加载最近订单
const loadRecentOrders = async () => {
  loadingOrders.value = true
  try {
    const res = await getRecentOrders(5)
    if (res.code === 200 && res.data) {
      // 状态映射
      const statusMap = {
        0: { text: '待支付', class: 'warning' },
        1: { text: '已支付', class: 'info' },
        2: { text: '使用中', class: 'secondary' },
        3: { text: '已完成', class: 'success' },
        4: { text: '已取消', class: 'danger' }
      }

      recentOrders.value = res.data.map(order => {
        const statusInfo = statusMap[order.status] || { text: '未知', class: 'secondary' }
        // 格式化时间
        let createTime = ''
        if (order.createTime) {
          const date = new Date(order.createTime)
          createTime = `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
        }

        return {
          id: order.id,
          orderNo: order.orderNo,
          shortOrderNo: order.orderNo && order.orderNo.length > 12 ? order.orderNo.substring(0, 8) + '...' : order.orderNo,
          userName: order.userName || '未知',
          vehicleNo: order.vehicleNo || '未知',
          amount: order.amount || '0.00',
          status: order.status,
          statusText: statusInfo.text,
          statusClass: statusInfo.class,
          createTime: createTime
        }
      })
    }
  } catch (error) {
    console.error('加载最近订单失败:', error)
    // 发生错误时使用空数组
    recentOrders.value = []
  } finally {
    loadingOrders.value = false
  }
}

// ========== 地图相关函数 ==========
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

    map = new AMap.Map('adminMapContainer', {
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
    }, 30000) // 30秒刷新一次
  } catch (error) {
    console.error('地图初始化失败:', error)
  }
}

// 加载无人机位置（首次加载，从API获取数据）
const loadVehicleLocations = async () => {
  if (!map) return

  // 同步来自API的数据
  await syncDroneData()

  // 添加标记到地图
  addVehicleMarkersToMap()
}

// 仅添加标记到地图（使用已有数据，不重新从API获取）
const addVehicleMarkersToMap = () => {
  if (!map) return

  const vehicleData = mockVehicleData.value

  // 清除现有标记
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

// 创建无人机标记
const createVehicleMarker = (vehicle) => {
  const statusClass = {
    'available': 'available',
    'in-use': 'in-use',
    'repair': 'repair',
    'offline': 'offline'
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
}

// 筛选无人机
const filterVehicles = async () => {
  if (!map) return

  // 同步数据
  await syncDroneData()

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

// 初始化图表
const initCharts = async () => {
  await nextTick()

  // 初始化订单趋势图
  const orderTrendDom = document.getElementById('orderTrendChart')
  if (orderTrendDom) {
    orderTrendChart = echarts.init(orderTrendDom)
    await loadOrderTrendChart()
  }

  // 初始化状态分布图
  const vehicleStatusDom = document.getElementById('vehicleStatusChart')
  if (vehicleStatusDom) {
    vehicleStatusChart = echarts.init(vehicleStatusDom)
    await loadVehicleStatusChart()
  }

  // 监听窗口大小变化
  window.addEventListener('resize', handleChartResize)
}

// 加载订单趋势图数据
const loadOrderTrendChart = async () => {
  try {
    const res = await getOrderTrends()
    if (res.code === 200 && res.data) {
      const { dates, orderCounts, orderAmounts } = res.data

      const option = {
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#e2e8f0',
          borderWidth: 1,
          textStyle: { color: '#1e293b' },
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: ['订单数量', '订单金额'],
          bottom: 0,
          textStyle: { color: '#64748b' }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLine: { lineStyle: { color: '#e2e8f0' } },
          axisLabel: { color: '#64748b' }
        },
        yAxis: [
          {
            type: 'value',
            name: '订单数量',
            position: 'left',
            axisLine: { show: false },
            axisLabel: { color: '#64748b' },
            splitLine: { lineStyle: { color: '#f1f5f9' } }
          },
          {
            type: 'value',
            name: '金额(元)',
            position: 'right',
            axisLine: { show: false },
            axisLabel: { color: '#64748b' },
            splitLine: { show: false }
          }
        ],
        series: [
          {
            name: '订单数量',
            type: 'bar',
            data: orderCounts,
            barWidth: '35%',
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#3b82f6' },
                { offset: 1, color: '#06b6d4' }
              ]),
              borderRadius: [6, 6, 0, 0]
            }
          },
          {
            name: '订单金额',
            type: 'line',
            yAxisIndex: 1,
            data: orderAmounts,
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            lineStyle: {
              width: 3,
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#f59e0b' },
                { offset: 1, color: '#f97316' }
              ])
            },
            itemStyle: { color: '#f97316' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(249, 115, 22, 0.3)' },
                { offset: 1, color: 'rgba(249, 115, 22, 0.05)' }
              ])
            }
          }
        ]
      }

      orderTrendChart.setOption(option)
    }
  } catch (error) {
    console.error('加载订单趋势图失败:', error)
  }
}

// 加载状态分布图数据
const loadVehicleStatusChart = async () => {
  try {
    const res = await getVehicleStatusDistribution()
    if (res.code === 200 && res.data) {
      const { statusNames, statusCounts } = res.data

      const option = {
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#e2e8f0',
          borderWidth: 1,
          textStyle: { color: '#1e293b' },
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: '10%',
          top: 'center',
          textStyle: { color: '#64748b' }
        },
        color: ['#94a3b8', '#10b981', '#3b82f6', '#f59e0b', '#8b5cf6'],
        series: [
          {
            name: '无人机状态',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['35%', '50%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: true,
              position: 'outside',
              formatter: '{b}\n{c}台',
              color: '#64748b',
              fontSize: 13
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 16,
                fontWeight: 'bold'
              },
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.1)'
              }
            },
            labelLine: {
              show: true,
              length: 15,
              length2: 10,
              smooth: 0.2
            },
            data: statusNames.map((name, index) => ({
              name,
              value: statusCounts[index]
            }))
          }
        ]
      }

      vehicleStatusChart.setOption(option)
    }
  } catch (error) {
    console.error('加载状态分布图失败:', error)
  }
}

// 处理图表自适应
const handleChartResize = () => {
  orderTrendChart?.resize()
  vehicleStatusChart?.resize()
}

onMounted(async () => {
  // 检查会话是否有效
  if (!getUserInfo()) {
    window.location.href = '/admin/login'
    return
  }

  // 设置当前日期
  currentDate.value = new Date().toLocaleDateString('zh-CN', {
    year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
  })

  // 加载数据
  loadDashboardStats()
  loadRecentOrders()

  // 每秒更新当前时间，用于充电进度实时更新
  timeUpdateInterval = setInterval(() => {
    currentTime.value = Date.now()
    // 每秒重新加载地图标记（不再同步API数据，减少请求）
    if (map) {
      addVehicleMarkersToMap()
    }
  }, 1000)

  // 初始化地图
  await nextTick()
  initMap()

  // 初始化图表
  initCharts()
})

onUnmounted(() => {
  // 清理定时器
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

  // 清理图表
  if (orderTrendChart) {
    orderTrendChart.dispose()
    orderTrendChart = null
  }
  if (vehicleStatusChart) {
    vehicleStatusChart.dispose()
    vehicleStatusChart = null
  }

  // 移除窗口大小监听
  window.removeEventListener('resize', handleChartResize)
})
</script>

<style scoped>
:root {
  --primary-color: #3b82f6;
  --accent-color: #06b6d4;
  --success-color: #10b981;
  --warning-color: #f59e0b;
  --danger-color: #ef4444;
}

.admin-dashboard {
  width: 100%;
  max-width: 100%;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  width: 100%;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 0;
}

.page-subtitle {
  color: #64748b;
  margin-top: 5px;
  margin-bottom: 0;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
  width: 100%;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  transition: all 0.3s;
  border: 1px solid #e2e8f0;
  width: 100%;
  box-sizing: border-box;
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

.stat-icon.blue {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #3b82f6;
}

.stat-icon.green {
  background: linear-gradient(135deg, #d1fae5, #a7f3d0);
  color: #10b981;
}

.stat-icon.purple {
  background: linear-gradient(135deg, #ede9fe, #ddd6fe);
  color: #8b5cf6;
}

.stat-icon.orange {
  background: linear-gradient(135deg, #fed7aa, #fdba74);
  color: #f97316;
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

.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  margin-top: 10px;
}

.stat-trend.up {
  background: #d1fae5;
  color: #059669;
}

.stat-trend.down {
  background: #fee2e2;
  color: #dc2626;
}

/* 图表卡片 */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
  width: 100%;
}

.chart-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  border: 1px solid #e2e8f0;
  width: 100%;
  box-sizing: border-box;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

.chart-container {
  height: 300px;
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

/* 无人机标记样式 - 需要在全局样式中生效 */
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
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
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

/* 表格卡片 */
.table-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  border: 1px solid #e2e8f0;
  width: 100%;
  box-sizing: border-box;
}

.table-card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
}

.table-card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.table {
  margin-bottom: 0;
}

.table th {
  background: #f8fafc;
  border-color: #e2e8f0;
  color: #64748b;
  font-weight: 600;
  padding: 16px 24px;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.table td {
  border-color: #f1f5f9;
  padding: 16px 24px;
  color: #475569;
  vertical-align: middle;
}

.table tbody tr {
  transition: background 0.2s;
}

.table tbody tr:hover {
  background: #f8fafc;
}

.success-text {
  color: var(--success-color);
}

/* 状态徽章 */
.badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.badge-success {
  background: #d1fae5;
  color: #059669;
}

.badge-info {
  background: #dbeafe;
  color: #2563eb;
}

.badge-warning {
  background: #fef3c7;
  color: #d97706;
}

.badge-danger {
  background: #fee2e2;
  color: #dc2626;
}

.badge-secondary {
  background: #f1f5f9;
  color: #64748b;
}

/* 按钮 */
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
  box-shadow: 0 5px 20px rgba(59, 130, 246, 0.3);
  color: white;
}

.btn-outline-primary {
  border: 2px solid var(--primary-color);
  color: var(--primary-color);
  background: transparent;
}

.btn-outline-primary:hover {
  background: var(--primary-color);
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

/* 响应式 */
@media (max-width: 992px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
