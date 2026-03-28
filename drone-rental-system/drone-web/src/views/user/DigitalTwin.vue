<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getMap, initMap, destoryMap } from '@/digital-twin/utils/mainMap2.js'
import { fetchMockData, setLocation } from '@/digital-twin/utils/mock.js'
import GLlayer from '#/gl-layers/lib/index.mjs'
import * as THREE from 'three'
import CropLayer from '@/digital-twin/components/cropLayer.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import * as echarts from 'echarts'

const {
  PolygonLayer,
  FlowlineLayer,
  BorderLayer,
  DrivingLayer,
  TilesLayer,
  LayerManager,
  WaterLayer
} = GLlayer

// 调试：检查 gl-layers 导入
console.log('gl-layers 导入检查:', {
  GLlayer: typeof GLlayer,
  FlowlineLayer: typeof FlowlineLayer,
  DrivingLayer: typeof DrivingLayer,
  PolygonLayer: typeof PolygonLayer,
  LayerManager: typeof LayerManager
})

// 容器
const container = ref(null)
const isLoading = ref(true)
const errorMessage = ref('')

// 图层管理
const layerManger = ref({
  layers: {},
  add(layer) {
    this.layers[layer.id] = layer
  },
  findLayerById(id) {
    return this.layers[id]
  },
  show(id) {
    const layer = this.layers[id]
    if (layer && layer.show) layer.show()
  },
  hide(id) {
    const layer = this.layers[id]
    if (layer && layer.hide) layer.hide()
  }
})

// 高德可视化类
let loca

// 无人机动画混合器
let mixer
let animationFrameId = null

// GUI 控制参数
const guiCtrl = {
  lightPositionX: 1000,
  lightPositionY: 1000,
  lightPositionZ: 1600,
  mixerPlaySpeed: 5
}

// 地图数据提示浮窗
let normalMarker

// 是否检测到有人试图越界
const isInvadeMode = ref(false)
let invadeClock = null
let invadePath = []
let invadeMarker = null
let borderRing = []  // 边界范围，用于越界检测

// 是否第一人称
const isFirstView = ref(false)

// ========== 实时无人机状态数据 ==========
// 模拟无人机的实时状态
const drones = ref([
  {
    id: 'DR-001',
    name: '巡检一号',
    status: 'flying', // flying, charging, idle, offline, returning
    battery: 85,
    speed: 12.5,
    altitude: 50,
    position: [113.532936, 22.738711],
    heading: 45,
    flightTime: 125, // 已飞行时间(秒)
    totalDistance: 2450, // 总飞行距离(米)
    signal: 98, // 信号强度
    temperature: 35, // 温度
    lastUpdate: new Date(),
    pathIndex: 0, // 当前路径索引
    color: '#00ffff' // 轨迹颜色
  }
])

// 当前选中的无人机
const selectedDrone = ref(null)

// 实时数据更新定时器
let realtimeUpdateTimer = null

// 历史轨迹数据
const historyTracks = ref([])
const isPlayingHistory = ref(false)
const historyProgress = ref(0)

// ========== 时间显示 ==========
const currentTime = ref('')
const currentDate = ref('')
let timeUpdateTimer = null

// 天气数据
const weatherInfo = ref({
  temp: 26,
  condition: '晴',
  icon: 'bi bi-sun-fill'
})

// 天气类型配置
const weatherTypes = [
  { condition: '晴', icon: 'bi bi-sun-fill', tempRange: [22, 32] },
  { condition: '多云', icon: 'bi bi-cloud-sun-fill', tempRange: [18, 28] },
  { condition: '阴', icon: 'bi bi-cloud-fill', tempRange: [15, 25] },
  { condition: '小雨', icon: 'bi bi-cloud-rain-fill', tempRange: [12, 22] },
  { condition: '大雨', icon: 'bi bi-cloud-rain-heavy-fill', tempRange: [10, 20] }
]

// 根据日期生成天气
function updateWeatherByDate() {
  const now = new Date()
  const dayOfYear = Math.floor((now - new Date(now.getFullYear(), 0, 0)) / (1000 * 60 * 60 * 24))
  const weatherIndex = dayOfYear % weatherTypes.length
  const weather = weatherTypes[weatherIndex]

  // 根据日期计算温度变化
  const tempVariation = Math.sin(dayOfYear * 0.1) * 5
  const baseTemp = (weather.tempRange[0] + weather.tempRange[1]) / 2
  const temp = Math.round(baseTemp + tempVariation)

  weatherInfo.value = {
    temp: temp,
    condition: weather.condition,
    icon: weather.icon
  }
}

// 更新时间显示
function updateTimeDisplay() {
  const now = new Date()
  const hours = now.getHours().toString().padStart(2, '0')
  const minutes = now.getMinutes().toString().padStart(2, '0')
  const seconds = now.getSeconds().toString().padStart(2, '0')
  currentTime.value = `${hours}:${minutes}:${seconds}`

  const year = now.getFullYear()
  const month = (now.getMonth() + 1).toString().padStart(2, '0')
  const day = now.getDate().toString().padStart(2, '0')
  const weekDays = ['日', '一', '二', '三', '四', '五', '六']
  const weekDay = weekDays[now.getDay()]
  currentDate.value = `${year}-${month}-${day} 星期${weekDay}`
}

// 启动时间更新
function startTimeUpdate() {
  updateWeatherByDate()
  updateTimeDisplay()
  timeUpdateTimer = setInterval(() => {
    updateTimeDisplay()
  }, 1000)
}

// 停止时间更新
function stopTimeUpdate() {
  if (timeUpdateTimer) {
    clearInterval(timeUpdateTimer)
    timeUpdateTimer = null
  }
}

// ========== 可视化图表 ==========
// 图表容器
const flightChartRef = ref(null)
const areaChartRef = ref(null)
const statsChartRef = ref(null)

// 图表实例
let flightChart = null
let areaChart = null
let statsChart = null

// 统计数据
const farmStats = ref({
  todayFlightTime: 125, // 今日飞行时长(分钟)
  todayFlightArea: 480, // 今日作业面积(亩)
  weeklyFlightData: [85, 92, 78, 125, 110, 95, 88], // 近7天飞行时长
  weeklyAreaData: [320, 380, 290, 480, 420, 350, 310], // 近7天作业面积
  cropDistribution: [ // 作物分布
    { name: '水稻', value: 1200 },
    { name: '蔬菜', value: 800 },
    { name: '果树', value: 650 },
    { name: '其他', value: 150 }
  ],
  taskStatus: [ // 任务状态
    { name: '已完成', value: 45 },
    { name: '进行中', value: 12 },
    { name: '待执行', value: 8 }
  ]
})

// 初始化飞行时长图表
function initFlightChart() {
  console.log('初始化飞行时长图表', flightChartRef.value)
  if (!flightChartRef.value) {
    console.warn('flightChartRef 不存在')
    return
  }
  // 检查容器尺寸
  const rect = flightChartRef.value.getBoundingClientRect()
  console.log('容器尺寸:', rect.width, rect.height)

  flightChart = echarts.init(flightChartRef.value)

  const option = {
    grid: { top: 10, right: 10, bottom: 20, left: 35 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 10 },
      splitLine: { lineStyle: { color: '#f1f5f9' } }
    },
    series: [{
      data: farmStats.value.weeklyFlightData,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#3b82f6', width: 2 },
      itemStyle: { color: '#3b82f6' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
          { offset: 1, color: 'rgba(59, 130, 246, 0)' }
        ])
      }
    }]
  }
  flightChart.setOption(option)
}

// 初始化作业面积图表
function initAreaChart() {
  if (!areaChartRef.value) return
  areaChart = echarts.init(areaChartRef.value)

  const option = {
    grid: { top: 10, right: 10, bottom: 20, left: 35 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 10 },
      splitLine: { lineStyle: { color: '#f1f5f9' } }
    },
    series: [{
      data: farmStats.value.weeklyAreaData,
      type: 'bar',
      barWidth: 16,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#06b6d4' },
          { offset: 1, color: '#0891b2' }
        ]),
        borderRadius: [4, 4, 0, 0]
      }
    }]
  }
  areaChart.setOption(option)
}

// 初始化统计分布图表
function initStatsChart() {
  if (!statsChartRef.value) return
  statsChart = echarts.init(statsChartRef.value)

  const option = {
    tooltip: { trigger: 'item' },
    legend: {
      orient: 'vertical',
      right: 0,
      top: 'center',
      textStyle: { fontSize: 11, color: '#64748b' },
      itemWidth: 12,
      itemHeight: 12
    },
    series: [
      {
        name: '作物分布',
        type: 'pie',
        radius: ['35%', '55%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: farmStats.value.cropDistribution,
        color: ['#3b82f6', '#06b6d4', '#10b981', '#f59e0b']
      }
    ]
  }
  statsChart.setOption(option)
}

// 初始化所有图表
function initCharts() {
  nextTick(() => {
    // 使用 requestAnimationFrame 确保DOM完全渲染
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        initFlightChart()
        initAreaChart()
        initStatsChart()

        // 添加窗口大小改变监听
        window.addEventListener('resize', handleChartResize)
      })
    })
  })
}

// 处理图表大小调整
function handleChartResize() {
  if (flightChart) flightChart.resize()
  if (areaChart) areaChart.resize()
  if (statsChart) statsChart.resize()
}

// 销毁图表
function disposeCharts() {
  window.removeEventListener('resize', handleChartResize)
  if (flightChart) { flightChart.dispose(); flightChart = null }
  if (areaChart) { areaChart.dispose(); areaChart = null }
  if (statsChart) { statsChart.dispose(); statsChart = null }
}

// 路径数据（用于模拟移动）
let dronePathData = []

// 为每架无人机创建不同偏移的路径
function getDronePath(droneId) {
  if (!dronePathData.length) return []

  const drone = drones.value.find(d => d.id === droneId)
  if (!drone) return []

  // 根据无人机的 pathIndex 创建偏移路径
  const pathLength = dronePathData.length
  const offset = drone.pathIndex || 0

  // 创建循环路径
  return [
    ...dronePathData.slice(offset % pathLength),
    ...dronePathData.slice(0, (offset % pathLength))
  ]
}

// 实时轨迹线（用于显示无人机飞过的路径）
const realtimeTrails = ref({}) // 存储每架无人机的轨迹线
let trailMarkers = {} // 存储轨迹标记点

const allLayers = [
  { id: 'borderLayer', name: '区域边界', visible: true },
  { id: 'farmLayer', name: '农田', visible: true },
  { id: 'poolLayer', name: '鱼塘', visible: true },
  { id: 'waterLayer', name: '水域', visible: true },
  { id: 'buildingLayer', name: '村居', visible: true },
  { id: 'dronLayer', name: '无人机', visible: true },
  { id: 'dronPathLayer', name: '无人机轨迹', visible: true },
  { id: 'cropLayer', name: '农产品识别', visible: true },
  { id: 'riskLayer', name: '灾害预测', visible: false },
  { id: 'producationLayer', name: '年产量预测', visible: false },
  { id: 'serverLayer', name: '服务点', visible: false },
]

const SETTING = {
  center: [113.532936, 22.738711],
  alone: false,
  topicMap: {
    product: {
      label: '生产',
      layers: ['farmLayer', 'poolLayer', 'waterLayer', 'buildingLayer', 'riskLayer', 'cropLayer']
    },
    security: {
      label: '安全',
      layers: ['borderLayer', 'waterLayer', 'buildingLayer', 'dronLayer', 'dronPathLayer', 'serverLayer']
    },
    value: {
      label: '经济',
      layers: ['farmLayer', 'poolLayer', 'cropLayer', 'producationLayer']
    },
    environment: {
      label: '环境',
      layers: ['farmLayer', 'waterLayer', 'cropLayer', 'poolLayer']
    },
    monitoring: {
      label: '监测',
      layers: ['farmLayer', 'dronLayer', 'dronPathLayer', 'buildingLayer']
    },
    irrigation: {
      label: '灌溉',
      layers: ['farmLayer', 'poolLayer', 'waterLayer', 'cropLayer']
    }
  }
}

// 位置配置
const LOCATIONS = {
  shenzhen: {
    name: '深圳农场',
    center: [113.532936, 22.738711],
    zoom: 15.5,
    pitch: 42.0,
    rotation: 4.9
  },
  sichuan: {
    name: '雨涵农场',
    center: [104.25, 31.08],
    zoom: 15.0,
    pitch: 45.0,
    rotation: 0
  }
}

// 当前位置
const currentLocation = ref('shenzhen')

// 初始化地图
async function init() {
  try {
    // 使用 gl-layers 的 LayerManager
    if (LayerManager) {
      layerManger.value = new LayerManager()
      console.log('使用 gl-layers LayerManager:', layerManger.value)
    } else {
      console.warn('LayerManager 不可用，使用默认 layerManger')
    }

    // 获取当前位置配置
    const loc = LOCATIONS[currentLocation.value]

    const map = await initMap({
      viewMode: '3D',
      dom: container.value,
      showBuildingBlock: false,
      center: loc.center,
      zoom: loc.zoom,
      pitch: loc.pitch,
      rotation: loc.rotation,
      mapStyle: 'amap://styles/light',
      skyColor: '#c8edff'
    })

    // 添加卫星图层
    const satelliteLayer = new AMap.TileLayer.Satellite()
    map.add([satelliteLayer])

    // 初始化 Loca
    loca = new Loca.Container({ map })

    // 创建标记
    normalMarker = new AMap.Marker({
      offset: [70, -15],
      zooms: [1, 22]
    })

    // 键盘快捷键切换专题
    document.addEventListener('keydown', handleKeyPress)

    isLoading.value = false
  } catch (error) {
    console.error('地图初始化失败:', error)
    errorMessage.value = '地图初始化失败: ' + error.message
    isLoading.value = false
  }
}

// 键盘事件处理
function handleKeyPress(event) {
  const keyCode = event.keyCode
  if (keyCode === 49) {
    switchTopic('product')
  } else if (keyCode === 50) {
    switchTopic('security')
  } else if (keyCode === 51) {
    switchTopic('value')
  }
}

// 初始化所有图层
async function initLayers() {
  console.log('开始初始化所有图层...')

  // 为每个图层单独添加错误处理，确保某个图层失败不影响其他图层
  const layers = [
    { name: '边界图层', fn: initBorderLayer },
    { name: '农田图层', fn: initFarmLayer },
    { name: '池塘图层', fn: initPoolLayer },
    { name: '水域图层', fn: initWaterLayer },
    { name: '建筑图层', fn: initBuildingLayer },
    { name: '无人机图层', fn: initDroneLayer },
    { name: '作物图层', fn: initCropLayer },
    { name: '风险图层', fn: initRiskLayer },
    { name: '产量图层', fn: initProducationLayer },
    { name: '服务点图层', fn: initServePOILayer }
  ]

  for (let i = 0; i < layers.length; i++) {
    const { name, fn } = layers[i]
    const step = i + 1
    try {
      console.log(`${step}. 初始化${name}`)
      await fn()
      console.log(`${step}. ${name}初始化完成`)
    } catch (error) {
      console.error(`${step}. ${name}初始化失败:`, error)
      // 继续初始化下一个图层
    }
  }

  console.log('所有图层初始化流程结束')
}

// 区域边界图层
async function initBorderLayer() {
  const map = getMap()
  const data = await fetchMockData('border.geojson')

  if (BorderLayer) {
    // 使用 gl-layers 的 BorderLayer
    const layer = new BorderLayer({
      id: 'borderLayer',
      alone: SETTING.alone,
      map,
      wallColor: '#3dfcfc',
      wallHeight: 100,
      data,
      speed: 0.3,
      animate: true,
      zooms: [11, 22],
      altitude: 0
    })
    layerManger.value.add(layer)
  } else {
    // 简化版本
    const geo = new Loca.GeoJSONSource({ data })
    const layer = new Loca.PolygonLayer({
      zIndex: 10,
      opacity: 0.8,
      zooms: [11, 22],
    })
    layer.setSource(geo)
    layer.setStyle({
      color: '#3dfcfc',
      borderColor: '#3dfcfc',
      borderWidth: 2,
      fillColor: 'rgba(61, 252, 252, 0.1)',
      height: 100,
    })
    loca.add(layer)
    layer.id = 'borderLayer'
    layerManger.value.add(layer)
  }
}

// 农田图层
async function initFarmLayer() {
  const map = getMap()
  const data = await fetchMockData('farm.geojson')

  if (!data || !data.features || !Array.isArray(data.features)) {
    console.warn('农田数据为空或格式错误')
    return
  }

  data.features.forEach(item => {
    const { used } = item.properties
    item.properties.color = used == 1 ? "#33a02c" : (used == 0 ? "#b2df8a" : "#ceb89e")
  })

  if (PolygonLayer) {
    const layer = new PolygonLayer({
      id: 'farmLayer',
      alone: SETTING.alone,
      map,
      data,
      lineWidth: 0,
      opacity: 0.4,
      interact: true,
      zIndex: 100,
      altitude: 2
    })
    layerManger.value.add(layer)
  } else {
    const geo = new Loca.GeoJSONSource({ data })
    const layer = new Loca.PolygonLayer({
      zIndex: 5,
      opacity: 0.6,
      zooms: [14, 22],
    })
    layer.setSource(geo)
    layer.setStyle({
      fillColor: '#33a02c',
      fillOpacity: 0.6,
      height: 5,
    })
    loca.add(layer)
    layer.id = 'farmLayer'
    layerManger.value.add(layer)
  }
}

// 池塘图层
async function initPoolLayer() {
  const map = getMap()
  const data = await fetchMockData('pool.geojson')

  if (PolygonLayer) {
    const layer = new PolygonLayer({
      id: 'poolLayer',
      alone: SETTING.alone,
      map,
      data,
      lineWidth: 0,
      opacity: 0.4,
      interact: true,
      zIndex: 100,
      altitude: 2
    })
    layerManger.value.add(layer)
  } else {
    const geo = new Loca.GeoJSONSource({ data })
    const layer = new Loca.PolygonLayer({
      zIndex: 5,
      opacity: 0.6,
      zooms: [14, 22],
    })
    layer.setSource(geo)
    layer.setStyle({
      fillColor: '#19bbf1',
      fillOpacity: 0.6,
      height: 5,
    })
    loca.add(layer)
    layer.id = 'poolLayer'
    layerManger.value.add(layer)
  }
}

// 水域图层
async function initWaterLayer() {
  const map = getMap()
  const data = await fetchMockData('water.geojson')

  if (WaterLayer) {
    const layer = new WaterLayer({
      id: 'waterLayer',
      map,
      data,
      alone: SETTING.alone,
      zooms: [16, 22],
      animate: true,
      waterColor: '#CFEACD',
      altitude: -5
    })
    layerManger.value.add(layer)
  } else {
    const geo = new Loca.GeoJSONSource({ data })
    const layer = new Loca.PolygonLayer({
      zIndex: 3,
      opacity: 0.7,
      zooms: [16, 22],
    })
    layer.setSource(geo)
    layer.setStyle({
      fillColor: '#CFEACD',
      fillOpacity: 0.7,
      height: 0,
    })
    loca.add(layer)
    layer.id = 'waterLayer'
    layerManger.value.add(layer)
  }
}

// 建筑图层
async function initBuildingLayer() {
  const map = getMap()

  // 使用 TilesLayer 加载 3D 建筑模型
  if (TilesLayer) {
    const layer = new TilesLayer({
      id: 'buildingLayer',
      title: '村居建筑图层',
      alone: SETTING.alone,
      map,
      center: [113.531905, 22.737473],
      zooms: [4, 30],
      interact: false,
      tilesURL: '/digital-twin-static/3dtiles/all/tileset.0.json',
      needShadow: true
    })

    layer.on('complete', ({ scene }) => {
      // 调整模型的亮度
      const aLight = new THREE.AmbientLight(0xffffff, 0.5)
      scene.add(aLight)
    })

    layerManger.value.add(layer)
  } else {
    console.warn('TilesLayer 不可用，跳过建筑图层')
  }
}

// 无人机巡航图层
async function initDroneLayer() {
  const map = getMap()
  console.log('开始初始化无人机图层，当前位置:', currentLocation.value)
  const data = await fetchMockData('dronWander2.geojson')
  console.log('无人机路径数据:', data)

  if (!data || !data.features || data.features.length === 0) {
    console.error('无人机路径数据为空或格式错误')
    return
  }

  const NPC = await getDroneModel()

  if (!NPC) {
    console.warn('无人机模型加载失败，跳过无人机图层')
    return
  }

  console.log('无人机模型加载成功，开始创建图层')

  // 1. 创建无人机移动图层
  if (DrivingLayer) {
    console.log('使用 gl-layers 创建无人机移动图层，路径点数:', data.features[0].geometry.coordinates[0].length)
    const droneMoveLayer = new DrivingLayer({
      id: 'dronLayer',
      map,
      zooms: [4, 30],
      path: data,
      altitude: 50,
      speed: 50.0,
      NPC,
      interact: true
    })

    droneMoveLayer.on('complete', ({ scene }) => {
      console.log('无人机移动图层加载完成')
      const aLight = new THREE.AmbientLight(0xffffff, 3.5)
      scene.add(aLight)
      // 确保无人机图层可见
      droneMoveLayer.show()
      droneMoveLayer.resume()
      console.log('无人机图层已显示并开始移动')
    })

    droneMoveLayer.on('error', (err) => {
      console.error('无人机移动图层加载失败:', err)
    })

    layerManger.value.add(droneMoveLayer)
    console.log('无人机移动图层已添加到 layerManger')
  } else {
    console.warn('DrivingLayer 不可用')
  }

  // 2. 创建闪光路径图层 - 使用 Loca 原生 LineLayer
  console.log('创建闪光路径图层')

  // 主路径层 - 发光效果
  const pathGeo = new Loca.GeoJSONSource({ data })
  const glowPathLayer = new Loca.LineLayer({
    zIndex: 100,
    opacity: 1,
    zooms: [14, 22],
    visible: false  // 默认隐藏，点击巡航时显示
  })

  glowPathLayer.setSource(pathGeo)

  // 设置发光效果样式
  glowPathLayer.setStyle({
    lineWidth: 8,
    height: 50,
    // 使用渐变色模拟闪光效果
    lineColors: {
      type: 'linear',
      colorStops: [
        [0.0, '#00ffff'],  // 青色
        [0.25, '#00ff00'], // 绿色
        [0.5, '#ffff00'],  // 黄色
        [0.75, '#ff00ff'], // 紫色
        [1.0, '#00ffff']   // 青色
      ]
    },
    lineType: 'solid'
  })

  loca.add(glowPathLayer)
  glowPathLayer.id = 'dronPathLayer'

  // 创建自定义 show/hide 方法
  glowPathLayer._visible = false

  glowPathLayer.show = function() {
    this._visible = true
    this.setVisible(true)
    // 同时显示脉冲层
    const pulseLayer = layerManger.value.findLayerById('dronPulseLayer')
    if (pulseLayer && pulseLayer.setVisible) {
      pulseLayer.setVisible(true)
    }
  }

  glowPathLayer.hide = function() {
    this._visible = false
    this.setVisible(false)
    // 同时隐藏脉冲层
    const pulseLayer = layerManger.value.findLayerById('dronPulseLayer')
    if (pulseLayer && pulseLayer.setVisible) {
      pulseLayer.setVisible(false)
    }
  }

  layerManger.value.add(glowPathLayer)

  // 3. 创建沿路径的脉冲光点层 - 使用 Marker 实现闪光点效果
  const pulseData = {
    type: 'FeatureCollection',
    features: []
  }

  if (data.features && data.features[0] && data.features[0].geometry) {
    const coords = data.features[0].geometry.coordinates
    // 沿路径取点作为脉冲点
    for (let i = 0; i < coords.length; i += 5) {
      pulseData.features.push({
        type: 'Feature',
        geometry: {
          type: 'Point',
          coordinates: coords[i]
        },
        properties: {
          radius: 30 + (i % 5) * 5  // 变化的半径
        }
      })
    }
  }

  // 使用 ScatterLayer 代替 PulseLayer（高德 Loca 可能没有 PulseLayer）
  const pulseGeo = new Loca.GeoJSONSource({ data: pulseData })
  const pulseLayer = new Loca.ScatterLayer({
    zIndex: 101,
    opacity: 0.9,
    zooms: [14, 22],
    visible: false  // 默认隐藏
  })

  pulseLayer.setSource(pulseGeo)

  pulseLayer.setStyle({
    unit: 'meter',
    size: [30, 30],
    color: '#00ffff',
    opacity: 0.8
  })

  loca.add(pulseLayer)
  pulseLayer.id = 'dronPulseLayer'

  layerManger.value.add(pulseLayer)

  console.log('无人机图层和闪光路径图层已创建完成')
}

// 加载无人机模型
function getDroneModel() {
  return new Promise((resolve) => {
    const loader = new GLTFLoader()
    loader.load(
      '/digital-twin-static/model/drone/drone1.glb',
      (gltf) => {
        const model = gltf.scene.children[0]
        const size = 10.0
        model.scale.set(size, size, size)

        mixer = new THREE.AnimationMixer(gltf.scene)
        const action = mixer.clipAction(gltf.animations[0])
        action.setEffectiveTimeScale(guiCtrl.mixerPlaySpeed)
        action.play()

        console.log('无人机模型加载成功，尺寸:', size)
        resolve(model)
      },
      undefined,
      (error) => {
        console.error('无人机模型加载失败:', error)
        resolve(null)
      }
    )
  })
}

// 动画循环
function animateFn() {
  animationFrameId = requestAnimationFrame(animateFn)
  if (mixer) {
    mixer.update(0.01)
  }
}

// 作物类型图层
async function initCropLayer() {
  const map = getMap()
  const data1 = await fetchMockData('crop.geojson')
  const data2 = await fetchMockData('poolCenter.geojson')
  data1.features = data1.features.concat(data2.features)

  const layer = new CropLayer({
    id: 'cropLayer',
    data: data1,
    zooms: [16, 22],
    zIndex: 200,
    map
  })

  layer.on('mouseover', (e) => {
    const { crop, style } = e.data
    normalMarker.setPosition(e.data.lnglat)
    normalMarker.setOffset(new AMap.Pixel(90, -10))

    let content = ''
    if (style <= 4) {
      content = `<div class="amap-info-window">
        <p>作物: ${crop}</p>
        <p>识别匹配度: ${parseInt(Math.random() * 20) + 80}%</p>
        <p>产量预计: ${parseInt(Math.random() * 30) + 20}吨</p>
      </div>`
    } else {
      content = `<div class="amap-info-window">
        <p>作物: ${crop}</p>
        <p>产量预计: ${parseInt(Math.random() * 20) + 10}吨</p>
      </div>`
    }

    normalMarker.setContent(content)
    normalMarker.setMap(map)
  })
  layer.on('mouseout', () => {
    map.remove(normalMarker)
  })

  layerManger.value.add(layer)
}

// 灾害风险检测图层
async function initRiskLayer() {
  const map = getMap()
  const data = await fetchMockData('fertility.geojson')

  const heatmap = new Loca.HeatMapLayer({
    zIndex: 10,
    opacity: 1,
    visible: false,
    zooms: [2, 22],
  })

  const geo = new Loca.GeoJSONSource({ data })
  heatmap.setSource(geo, {
    radius: 150,
    unit: 'meter',
    height: 300,
    gradient: {
      1: '#FF4C2F',
      0.8: '#FAA53F',
      0.6: '#FFF100',
      0.5: '#7DF675',
      0.4: '#5CE182',
      0.2: '#29CF6F',
    },
    value: function (index, feature) {
      return feature.properties.weight ?? 0
    },
    min: 0,
    max: 100,
    visible: true
  })

  loca.add(heatmap)

  // 添加点击交互 - 显示风险值
  map.on('click', function (e) {
    const feat = heatmap.queryFeature(e.pixel.toArray())
    if (feat) {
      normalMarker.setAnchor('bottom-center')
      normalMarker.setOffset(new AMap.Pixel(0, -40))
      normalMarker.setPosition(feat.lnglat)
      normalMarker.setContent(`<div style="margin-bottom: 15px; border:1px solid #fff;
         border-radius: 4px;color: #fff; width: 150px;
         text-align: center; background: rgba(0,0,0,0.7); padding: 8px;">风险值: ${feat.value ?? 0}</div>`)
      normalMarker.setMap(map)
    }
  })

  heatmap.id = 'riskLayer'
  layerManger.value.add(heatmap)
}

// 产量预测图层
async function initProducationLayer() {
  const map = getMap()
  const data = await fetchMockData('productation.geojson')

  const hLayer = new Loca.HexagonLayer({
    zIndex: 220,
    opacity: 0.9,
    visible: false,
    zooms: [2, 22]
  })

  const geo = new Loca.GeoJSONSource({ data })
  hLayer.setSource(geo)

  const colors = [
    '#e0422f',
    '#fdb96a',
    '#d5ed88',
    '#5ab760',
    '#006837',
    '#4c99c6',
  ].reverse()

  hLayer.setStyle({
    unit: 'px',
    radius: 20,
    gap: 0,
    altitude: 0,
    visible: false,
    height: function (index, feature) {
      var weight = feature.coordinates[0].properties.value ?? 0
      return weight * 2
    },
    topColor: function (index, feature) {
      var ranks = feature.coordinates[0].properties.value ?? 0
      return ranks < 10 ?
        colors[0] : ranks < 30 ?
          colors[1] : ranks < 60 ?
            colors[2] : ranks < 80 ?
              colors[3] : ranks < 90 ?
                colors[4] : colors[5]
    },
    sideTopColor: function (index, feature) {
      var ranks = feature.coordinates[0].properties.value ?? 0
      return ranks < 10 ?
        colors[0] : ranks < 30 ?
          colors[1] : ranks < 60 ?
            colors[2] : ranks < 80 ?
              colors[3] : ranks < 90 ?
                colors[4] : colors[5]
    },
    sideBottomColor: function (index, feature) {
      var ranks = feature.coordinates[0].properties.value ?? 0
      return ranks < 10 ?
        colors[0] : ranks < 30 ?
          colors[1] : ranks < 60 ?
            colors[2] : ranks < 80 ?
              colors[3] : ranks < 90 ?
                colors[4] : colors[5]
    }
  })

  loca.add(hLayer)
  hLayer.id = 'producationLayer'
  layerManger.value.add(hLayer)
}

// 服务点图层
async function initServePOILayer() {
  const map = getMap()
  const data = await fetchMockData('serverpoi.geojson')

  const labelsLayer = new Loca.LabelsLayer({
    zooms: [10, 20],
  })

  const geo = new Loca.GeoJSONSource({ data })
  labelsLayer.setSource(geo)
  labelsLayer.setStyle({
    icon: {
      type: 'image',
      image: '/digital-twin-static/icons/server_poi.png',
      size: [48, 75],
      anchor: 'center',
    },
    text: {
      content: (index, feat) => feat.properties.name,
      style: {
        fontSize: 12,
        fontWeight: 'normal',
        fillColor: '#5CDE8E',
        strokeColor: '#000',
        strokeWidth: 2,
      },
      direction: 'bottom',
    },
    extData: (index, feat) => {
      return feat.properties
    }
  })

  loca.add(labelsLayer)
  labelsLayer.id = 'serverLayer'
  layerManger.value.add(labelsLayer)

  // 添加交互事件
  labelsLayer.on('complete', () => {
    const labelMarkers = labelsLayer.getLabelsLayer().getAllOverlays()
    for (let marker of labelMarkers) {
      marker.on('mouseover', (e) => {
        const position = e.data.data && e.data.data.position
        const { name, area, department, manager, contact } = marker.getExtData()
        if (position) {
          normalMarker.setContent(
            `<div class="amap-info-window">
              <p>名称：${name}</p>
              <p>部门：${department}</p>
              <p>面积：${area} 平方米</p>
              <p>负责人：${manager}</p>
              <p>电话：${contact}</p>
            </div>`,
          )
          normalMarker.setOffset(new AMap.Pixel(90, -40))
          normalMarker.setPosition(position)
          normalMarker.setMap(map)
        }
      })
      marker.on('mouseout', () => {
        normalMarker.setMap(null)
      })
      marker.on('click', () => {
        const { view } = marker.getExtData()
        if (view) {
          setMapView(view)
        }
      })
    }
  })
}

// 设置地图视图
function setMapView({ center, zoom, pitch, rotation }) {
  const map = getMap()
  if (center) map.setCenter(center)
  if (zoom) map.setZoom(zoom)
  if (pitch) map.setPitch(pitch)
  if (rotation) map.setRotation(rotation)
}

// 越界告警功能
async function toggleInvade() {
  const map = getMap()
  const borderLayer = layerManger.value.findLayerById('borderLayer')
  isInvadeMode.value = !isInvadeMode.value

  if (isInvadeMode.value) {
    await initInvade()
  } else {
    clearInvade()
    // 重置边界颜色为青色
    if (borderLayer && borderLayer.setColor) {
      borderLayer.setColor('#3dfcfc')
    }
  }
}

// 初始化越界检测
async function initInvade() {
  const map = getMap()
  const borderLayer = layerManger.value.findLayerById('borderLayer')

  // 加载边界数据作为检测范围
  const borderData = await fetchMockData('border.geojson')
  if (borderData && borderData.features && borderData.features[0]) {
    borderRing = borderData.features[0].geometry.coordinates[0]
  }

  // 加载入侵者路径
  const { features } = await fetchMockData('invade-path.geojson')
  invadePath = features[0].geometry.coordinates[0]

  // 创建入侵者标记
  invadeMarker = new AMap.Marker({
    content: `<img style="width:30px;" src="/digital-twin-static/icons/ico-invade.png">`,
    anchor: 'bottom-center',
    offset: new AMap.Pixel(-15, -20)
  })
  map.add(invadeMarker)

  let currentIndex = 0
  let currentBorderColor = '#3dfcfc'

  invadeClock = setInterval(() => {
    if (invadePath.length === 0) return

    currentIndex = (currentIndex + 1) % invadePath.length
    const pos = invadePath[currentIndex]
    invadeMarker.setPosition(pos)

    // 判断是否在边界内
    const isInRing = borderRing.length > 0 ? AMap.GeometryUtil.isPointInRing(pos, borderRing) : false
    console.log('入侵者在边界内:', isInRing)

    // 根据是否越界改变边界颜色
    const targetColor = isInRing ? '#ff0000' : '#3dfcfc'
    if (currentBorderColor !== targetColor) {
      currentBorderColor = targetColor
      if (borderLayer && borderLayer.setColor) {
        borderLayer.setColor(targetColor)
        console.log('边界颜色变为:', targetColor)
      }
    }
  }, 1000)
}

// 清除越界检测
function clearInvade() {
  if (invadeClock) {
    clearInterval(invadeClock)
    invadeClock = null
  }
  if (invadeMarker) {
    const map = getMap()
    map.remove(invadeMarker)
    invadeMarker = null
  }
  invadePath = []
  borderRing = []
}

// 切换无人机第一人称状态
function toggleDronView() {
  const map = getMap()
  const dronLayer = layerManger.value.findLayerById('dronLayer')
  const dronPathLayer = layerManger.value.findLayerById('dronPathLayer')
  const targetValue = isFirstView.value ? false : true

  console.log('toggleDronView 调用:', {
    isFirstView: isFirstView.value,
    targetValue,
    dronLayer: dronLayer ? '找到' : '未找到',
    dronPathLayer: dronPathLayer ? '找到' : '未找到',
    currentZoom: map.getZoom()
  })

  if (dronLayer && dronLayer.setCameraFollow) {
    dronLayer.setCameraFollow(targetValue)
  }
  if (dronPathLayer) {
    const action = targetValue ? 'show' : 'hide'
    console.log('执行 FlowlineLayer.' + action + '()')
    dronPathLayer[action]()
  }
  map.setZoom(targetValue ? 17.5 : 15, false)

  isFirstView.value = targetValue
}

// 切换图层
function toggleLayer(id) {
  const layerInfo = allLayers.find(l => l.id === id)
  if (layerInfo) {
    layerInfo.visible = !layerInfo.visible
    const layer = layerManger.value.findLayerById(id)
    if (layer) {
      const fn = layerInfo.visible ? 'show' : 'hide'
      if (layer[fn]) layer[fn]()
    }
  }
}

// 切换专题
function switchTopic(name) {
  const { layers, label } = SETTING.topicMap[name]

  allLayers.forEach(({ name, id }) => {
    const layer = layerManger.value.findLayerById(id)
    if (!layer) {
      console.log(`${id} 不存在`)
      return
    }
    if (layers.includes(layer.id)) {
      layer.show()
      animateLayer(layer)
    } else {
      layer.hide()
    }
  })
}

// 给图层的显示增加动画效果
function animateLayer(layer) {
  switch (layer.id) {
    case 'riskLayer':
      if (layer.addAnimate) {
        layer.addAnimate({
          key: 'height',
          value: [0, 1],
          duration: 2000,
          easing: 'BackOut',
        })
        layer.addAnimate({
          key: 'radius',
          value: [0, 1],
          duration: 2000,
          easing: 'BackOut',
          transform: 1000,
          random: true,
          delay: 5000,
        })
      }
      break
    case 'producationLayer':
      if (layer.addAnimate) {
        layer.addAnimate({
          key: 'height',
          value: [0, 1],
          duration: 500,
          easing: 'Linear',
          transform: 500,
          random: true,
          delay: 5 * 1000,
        })
      }
      break
    default:
      break
  }
}

// 回到中心
function gotoCenter() {
  const map = getMap()
  if (map) {
    const loc = LOCATIONS[currentLocation.value]
    map.setCenter(loc.center)
  }
}

// 切换位置
async function switchLocation(locationKey) {
  if (locationKey === currentLocation.value) return

  // 停止动画
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }

  // 清除无人机混合器
  if (mixer) {
    mixer.stopAllAction()
    mixer = null
  }

  // 清除实时轨迹
  clearRealtimeTrails()

  // 清除所有图层
  clearAllLayers()

  // 销毁当前地图
  destoryMap()

  // 重置 Loca 容器
  loca = null

  // 更新位置
  currentLocation.value = locationKey
  setLocation(locationKey)

  // 更新设置
  const loc = LOCATIONS[locationKey]
  SETTING.center = loc.center

  // 重新加载路径数据
  dronePathData = []

  // 重新初始化地图和图层
  isLoading.value = true
  try {
    await init()
    await initLayers()
    animateFn()
    isLoading.value = false
  } catch (error) {
    console.error('切换位置失败:', error)
    errorMessage.value = '切换位置失败: ' + error.message
    isLoading.value = false
  }
}

// 清除所有图层
function clearAllLayers() {
  const layers = layerManger.value.layers || {}
  Object.keys(layers).forEach(key => {
    const layer = layers[key]
    if (layer) {
      // 尝试调用不同类型的销毁方法
      if (layer.destroy) {
        layer.destroy()
      } else if (layer.setMap) {
        layer.setMap(null)
      }
    }
  })
  layerManger.value.layers = {}

  // 清除 Loca 图层
  if (loca) {
    try {
      loca.clear()
    } catch (e) {
      console.warn('清除 Loca 图层失败:', e)
    }
  }

  // 清除标记
  if (normalMarker) {
    try {
      normalMarker.setMap(null)
    } catch (e) {
      console.warn('清除标记失败:', e)
    }
  }

  // 重置状态
  isFirstView.value = false
}

// ========== 实时无人机状态相关函数 ==========

// 启动实时数据更新
function startRealtimeUpdate() {
  if (realtimeUpdateTimer) return

  // 加载路径数据用于模拟移动
  loadDronePathData()

  // 每秒更新一次
  realtimeUpdateTimer = setInterval(() => {
    updateDroneStatus()
    updateFarmStats() // 更新统计数据
  }, 1000)

  console.log('实时数据更新已启动')
}

// 停止实时数据更新
function stopRealtimeUpdate() {
  if (realtimeUpdateTimer) {
    clearInterval(realtimeUpdateTimer)
    realtimeUpdateTimer = null
    console.log('实时数据更新已停止')
  }
}

// 更新统计数据（模拟动态变化）
function updateFarmStats() {
  // 随机增加今日飞行时长和作业面积
  if (Math.random() > 0.7) {
    farmStats.value.todayFlightTime = Math.min(500, farmStats.value.todayFlightTime + Math.random() * 2)
    farmStats.value.todayFlightArea = Math.min(1000, farmStats.value.todayFlightArea + Math.random() * 5)
  }

  // 每30秒更新一次图表数据
  const now = new Date()
  if (!farmStats.value.lastChartUpdate || now - farmStats.value.lastChartUpdate > 30000) {
    farmStats.value.lastChartUpdate = now

    // 模拟近7天数据变化
    farmStats.value.weeklyFlightData = farmStats.value.weeklyFlightData.map(val =>
      Math.max(50, Math.min(200, val + (Math.random() - 0.5) * 20))
    )
    farmStats.value.weeklyAreaData = farmStats.value.weeklyAreaData.map(val =>
      Math.max(200, Math.min(600, val + (Math.random() - 0.5) * 50))
    )

    // 更新图表
    if (flightChart) {
      flightChart.setOption({
        series: [{ data: farmStats.value.weeklyFlightData }]
      })
    }
    if (areaChart) {
      areaChart.setOption({
        series: [{ data: farmStats.value.weeklyAreaData }]
      })
    }
  }
}

// 加载无人机路径数据
async function loadDronePathData() {
  const data = await fetchMockData('dronWander2.geojson')
  if (data && data.features && data.features[0]) {
    dronePathData = data.features[0].geometry.coordinates[0]
  }
}

// 更新无人机状态（模拟实时数据）
function updateDroneStatus() {
  const now = new Date()
  const map = getMap()

  drones.value.forEach((drone, index) => {
    // 只更新飞行中的无人机
    if (drone.status !== 'flying') return

    // 保存旧位置用于绘制轨迹
    const oldPosition = [...drone.position]

    // 模拟电量消耗（每架无人机消耗速度略有不同）
    const batteryDrain = 0.03 + Math.random() * 0.04
    drone.battery = Math.max(0, drone.battery - batteryDrain)

    // 模拟速度变化 (8-15 m/s)，每架无人机速度范围不同
    const minSpeed = 8 + index * 2
    const maxSpeed = 13 + index * 2
    drone.speed = minSpeed + Math.random() * (maxSpeed - minSpeed)

    // 模拟高度变化 (40-60 m)，每架无人机高度不同
    drone.altitude = 40 + index * 5 + Math.random() * 15

    // 模拟信号强度变化
    drone.signal = Math.min(100, Math.max(80, drone.signal + (Math.random() - 0.5) * 5))

    // 模拟温度变化
    drone.temperature = Math.min(45, Math.max(25, drone.temperature + (Math.random() - 0.5) * 2))

    // 更新飞行时间
    drone.flightTime += 1

    // 模拟位置移动（沿路径，每架无人机有不同的起始位置）
    if (dronePathData.length > 0) {
      // 使用无人机的 pathIndex 作为偏移，使它们分散在路径上
      const baseIndex = (drone.pathIndex || 0)
      const pathIndex = (baseIndex + drone.flightTime * 2) % dronePathData.length
      const nextPos = dronePathData[Math.floor(pathIndex)]
      if (nextPos) {
        drone.position = nextPos
        // 计算航向
        const nextIndex = (Math.floor(pathIndex) + 1) % dronePathData.length
        const afterPos = dronePathData[nextIndex]
        drone.heading = calculateHeading(nextPos, afterPos)
      }
    }

    // 更新总距离
    drone.totalDistance += drone.speed * 0.2

    // 电量低时自动返航
    if (drone.battery < 20 && drone.status === 'flying') {
      drone.status = 'returning'
    }

    // 更新时间
    drone.lastUpdate = now

    // 绘制实时轨迹（每5秒绘制一段轨迹，使用无人机指定的颜色）
    if (drone.flightTime % 5 === 0 && map) {
      addRealtimeTrailPoint(drone.id, oldPosition, drone.position, drone.color)
    }
  })

  // 更新当前选中的无人机信息
  if (selectedDrone.value) {
    const updated = drones.value.find(d => d.id === selectedDrone.value.id)
    if (updated) {
      selectedDrone.value = { ...updated }
    }
  }
}

// 添加实时轨迹点
function addRealtimeTrailPoint(droneId, fromPos, toPos, color = '#00ffff') {
  const map = getMap()
  if (!map) return

  // 初始化该无人机的轨迹数组
  if (!realtimeTrails.value[droneId]) {
    realtimeTrails.value[droneId] = []
  }

  // 添加轨迹线段，使用指定的颜色
  const polyline = new AMap.Polyline({
    path: [fromPos, toPos],
    strokeColor: color,
    strokeWeight: 3,
    strokeOpacity: 0.8,
    lineJoin: 'round'
  })

  polyline.setMap(map)
  realtimeTrails.value[droneId].push(polyline)

  // 限制轨迹长度（保留最近50段）
  if (realtimeTrails.value[droneId].length > 50) {
    const oldPolyline = realtimeTrails.value[droneId].shift()
    oldPolyline.setMap(null)
  }
}

// 清除实时轨迹
function clearRealtimeTrails(droneId = null) {
  if (droneId) {
    // 清除指定无人机的轨迹
    if (realtimeTrails.value[droneId]) {
      realtimeTrails.value[droneId].forEach(polyline => {
        polyline.setMap(null)
      })
      realtimeTrails.value[droneId] = []
    }
  } else {
    // 清除所有轨迹
    Object.keys(realtimeTrails.value).forEach(id => {
      realtimeTrails.value[id].forEach(polyline => {
        polyline.setMap(null)
      })
    })
    realtimeTrails.value = {}
  }
}

// 计算两点之间的航向角
function calculateHeading(from, to) {
  const fromLng = from[0] * Math.PI / 180
  const fromLat = from[1] * Math.PI / 180
  const toLng = to[0] * Math.PI / 180
  const toLat = to[1] * Math.PI / 180

  const y = Math.sin(toLng - fromLng) * Math.cos(toLat)
  const x = Math.cos(fromLat) * Math.sin(toLat) - Math.sin(fromLat) * Math.cos(toLat) * Math.cos(toLng - fromLng)

  let heading = Math.atan2(y, x) * 180 / Math.PI
  if (heading < 0) heading += 360
  return Math.round(heading)
}

// 选择无人机
function selectDrone(drone) {
  selectedDrone.value = { ...drone }

  // 将地图中心移动到无人机位置
  const map = getMap()
  if (map) {
    map.setCenter(drone.position)
  }
}

// 获取状态文本
function getStatusText(status) {
  const statusMap = {
    flying: '飞行中',
    charging: '充电中',
    idle: '待机中',
    offline: '离线',
    returning: '返航中'
  }
  return statusMap[status] || status
}

// 获取状态颜色
function getStatusColor(status) {
  const colorMap = {
    flying: '#10b981',      // 绿色
    charging: '#3b82f6',    // 蓝色
    idle: '#6b7280',        // 灰色
    offline: '#ef4444',     // 红色
    returning: '#f59e0b'    // 橙色
  }
  return colorMap[status] || '#6b7280'
}

// 获取电量颜色
function getBatteryColor(battery) {
  if (battery > 50) return '#10b981'
  if (battery > 20) return '#f59e0b'
  return '#ef4444'
}

// 历史轨迹回放
function playHistoryTrack() {
  if (!selectedDrone.value || !dronePathData.length) return

  isPlayingHistory.value = true
  historyProgress.value = 0

  const map = getMap()
  const totalPoints = Math.min(dronePathData.length, 100)
  let currentPoint = 0

  // 创建回放轨迹线
  const playbackPolyline = new AMap.Polyline({
    path: [],
    strokeColor: '#f59e0b',
    strokeWeight: 4,
    strokeOpacity: 0.9,
    lineJoin: 'round'
  })
  playbackPolyline.setMap(map)

  // 创建回放标记
  const playbackMarker = new AMap.Marker({
    position: dronePathData[0],
    icon: new AMap.Icon({
      image: '/digital-twin-static/icons/ico-invade.png',
      size: new AMap.Size(30, 30),
      imageSize: new AMap.Size(30, 30)
    }),
    offset: new AMap.Pixel(-15, -15)
  })
  playbackMarker.setMap(map)

  const interval = setInterval(() => {
    if (!isPlayingHistory.value || currentPoint >= totalPoints) {
      isPlayingHistory.value = false
      clearInterval(interval)
      // 清除回放元素
      setTimeout(() => {
        playbackPolyline.setMap(null)
        playbackMarker.setMap(null)
      }, 2000)
      return
    }

    currentPoint++
    historyProgress.value = Math.floor((currentPoint / totalPoints) * 100)

    // 更新轨迹
    const newPath = dronePathData.slice(0, currentPoint)
    playbackPolyline.setPath(newPath)

    // 更新标记位置
    const currentPos = dronePathData[currentPoint - 1]
    playbackMarker.setPosition(currentPos)

    // 更新地图中心跟随标记
    map.setCenter(currentPos)
  }, 50) // 每50ms更新一次，约5秒完成回放
}

// 停止历史回放
function stopHistoryTrack() {
  isPlayingHistory.value = false
  historyProgress.value = 0
}

// 格式化时间（秒 -> 时:分:秒）
function formatTime(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) {
    return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }
  return `${m}:${s.toString().padStart(2, '0')}`
}

onMounted(async () => {
  // 确保容器已渲染（使用多重检查）
  await nextTick()

  // 使用 requestAnimationFrame 确保在浏览器下一帧渲染
  await new Promise(resolve => requestAnimationFrame(resolve))

  // 再次等待确保 DOM 完全就绪
  await nextTick()

  // 先设置当前位置
  setLocation(currentLocation.value)

  // 检查容器是否存在
  if (!container.value) {
    console.error('地图容器不存在')
    errorMessage.value = '地图容器初始化失败'
    return
  }

  // 打印容器信息用于调试
  console.log('地图容器已准备:', container.value.offsetWidth, container.value.offsetHeight)

  await init()
  await initLayers()
  animateFn()

  // 启动实时数据更新
  startRealtimeUpdate()

  // 启动时间显示
  startTimeUpdate()

  // 初始化图表
  initCharts()

  // 默认选中第一架无人机
  if (drones.value.length > 0) {
    selectedDrone.value = { ...drones.value[0] }
  }

  // 添加调试函数到 window 对象
  window.getMapView = function () {
    const map = getMap()
    const center = map.getCenter()
    const zoom = map.getZoom()
    const pitch = map.getPitch()
    const rotation = map.getRotation()

    const res = {
      center: [center.lng, center.lat],
      zoom,
      pitch,
      rotation
    }
    console.log('地图视图:', res)
    return res
  }

  window.setMapView = function ({ center, zoom, pitch, rotation }) {
    setMapView({ center, zoom, pitch, rotation })
  }
})

onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  clearInvade()
  document.removeEventListener('keydown', handleKeyPress)
  stopRealtimeUpdate()
  stopTimeUpdate()

  // 销毁图表
  disposeCharts()

  const map = getMap()
  if (map) {
    map.destroy()
  }
})
</script>

<template>
  <div class="digital-twin-page">
    <!-- 大屏顶部标题栏 -->
    <div class="dashboard-header">
      <div class="header-left">
        <button class="back-btn" @click="$router.push('/')">
          <i class="bi bi-arrow-left-circle-fill"></i>
          <span>返回首页</span>
        </button>
      </div>
      <div class="header-title">
        <img src="/农场中心.png" alt="logo" class="header-logo">
        <span>智慧农业数字孪生平台</span>
      </div>
      <div class="header-info">
        <div class="time-weather">
          <div class="time-part">
            <span class="current-time">{{ currentTime }}</span>
            <span class="date">{{ currentDate }}</span>
          </div>
          <div class="weather-divider"></div>
          <div class="weather-part">
            <i :class="weatherInfo.icon + ' weather-icon'"></i>
            <span class="weather-temp">{{ weatherInfo.temp }}°C</span>
            <span class="weather-text">{{ weatherInfo.condition }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-content">
        <div class="spinner"></div>
        <p>正在加载地图...</p>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-if="errorMessage" class="error-overlay">
      <div class="error-content">
        <p>{{ errorMessage }}</p>
        <button class="btn btn-primary" @click="() => $router.push('/')">返回首页</button>
      </div>
    </div>

    <!-- 大屏主体布局 -->
    <div class="dashboard-container">
      <!-- 左侧面板 -->
      <div class="left-panel">
        <!-- 农场位置选择盒子 -->
        <div class="panel-box location-box">
          <div class="box-title">
            <i class="bi bi-geo-alt-fill me-2"></i>农场位置
          </div>
          <div class="box-content">
            <div
              v-for="(loc, key) in LOCATIONS"
              :key="key"
              class="location-item"
              :class="{ active: currentLocation === key }"
              @click="switchLocation(key)"
            >
              <i class="bi bi-pin-map-fill me-1"></i>{{ loc.name }}
            </div>
          </div>
        </div>

        <!-- 图层管理盒子 -->
        <div class="panel-box layers-box">
          <div class="box-title">
            <i class="bi bi-layers-fill me-2"></i>图层管理
          </div>
          <div class="box-content">
            <div class="layer-grid">
              <div
                v-for="(item, index) in allLayers"
                :key="index"
                class="layer-item"
                :class="{ active: item.visible }"
                @click="toggleLayer(item.id)"
              >
                <i :class="item.visible ? 'bi bi-check-square-fill' : 'bi bi-square'"></i>
                {{ item.name }}
              </div>
            </div>
          </div>
        </div>

        <!-- 专题选择盒子 -->
        <div class="panel-box topic-box">
          <div class="box-title">
            <i class="bi bi-grid-fill me-2"></i>专题模式
          </div>
          <div class="box-content">
            <div
              v-for="(item, key) in SETTING.topicMap"
              :key="key"
              class="topic-item"
              @click="switchTopic(key)"
            >
              {{ item.label }}
            </div>
          </div>
        </div>
      </div>

      <!-- 中间地图区域 -->
      <div class="center-panel">
        <div ref="container" class="map-wrapper"></div>
        <div class="map-overlay"></div>
      </div>

      <!-- 右侧面板 -->
      <div class="right-panel">
        <!-- 无人机实时监控盒子 -->
        <div class="panel-box drone-monitor-box">
          <div class="box-title">
            <i class="bi bi-airplane-engines-fill me-2"></i>无人机实时监控
            <span class="live-badge">
              <span class="live-dot"></span>
              实时
            </span>
          </div>
          <div class="box-content">
            <!-- 无人机信息 -->
            <div class="drone-info-card">
              <div class="drone-header">
                <span class="drone-color-dot" :style="{ background: drones[0].color }"></span>
                <span class="drone-name">{{ drones[0].name }}</span>
                <span class="drone-status" :style="{ color: getStatusColor(drones[0].status) }">
                  {{ getStatusText(drones[0].status) }}
                </span>
              </div>

              <!-- 状态数据网格 -->
              <div class="status-grid">
                <div class="status-item">
                  <span class="status-label">电量</span>
                  <div class="battery-mini">
                    <div class="battery-mini-fill" :style="{
                      width: drones[0].battery + '%',
                      background: getBatteryColor(drones[0].battery)
                    }"></div>
                    <span class="battery-mini-text">{{ drones[0].battery.toFixed(0) }}%</span>
                  </div>
                </div>
                <div class="status-item">
                  <span class="status-label">速度</span>
                  <span class="status-value">{{ drones[0].speed.toFixed(1) }} m/s</span>
                </div>
                <div class="status-item">
                  <span class="status-label">高度</span>
                  <span class="status-value">{{ drones[0].altitude.toFixed(0) }} m</span>
                </div>
                <div class="status-item">
                  <span class="status-label">航向</span>
                  <span class="status-value">{{ drones[0].heading }}°</span>
                </div>
                <div class="status-item full">
                  <span class="status-label">位置</span>
                  <span class="status-value small">{{ drones[0].position[0].toFixed(6) }}, {{ drones[0].position[1].toFixed(6) }}</span>
                </div>
                <div class="status-item">
                  <span class="status-label">飞行时间</span>
                  <span class="status-value">{{ formatTime(drones[0].flightTime) }}</span>
                </div>
                <div class="status-item">
                  <span class="status-label">总里程</span>
                  <span class="status-value">{{ drones[0].totalDistance.toFixed(0) }} m</span>
                </div>
                <div class="status-item">
                  <span class="status-label">信号</span>
                  <div class="signal-mini">
                    <i v-for="i in 4" :key="i" class="bi bi-signal-fill"
                      :style="{ opacity: i <= drones[0].signal / 25 ? 1 : 0.3 }"></i>
                    <span>{{ drones[0].signal }}%</span>
                  </div>
                </div>
                <div class="status-item">
                  <span class="status-label">温度</span>
                  <span class="status-value">{{ drones[0].temperature.toFixed(1) }}°C</span>
                </div>
              </div>
            </div>

            <!-- 控制按钮 -->
            <div class="control-section">
              <div class="control-buttons">
                <button
                  class="ctrl-btn"
                  :class="{ active: isPlayingHistory }"
                  @click="isPlayingHistory ? stopHistoryTrack() : playHistoryTrack()"
                >
                  <i :class="isPlayingHistory ? 'bi bi-pause-fill' : 'bi bi-play-fill'"></i>
                  {{ isPlayingHistory ? '暂停' : '回放' }}
                </button>
                <button class="ctrl-btn ctrl-btn-danger" @click="clearRealtimeTrails()">
                  <i class="bi bi-trash-fill"></i>
                  清除轨迹
                </button>
              </div>
              <div v-if="isPlayingHistory" class="progress-bar-mini">
                <div class="progress-mini-fill" :style="{ width: historyProgress + '%' }"></div>
                <span class="progress-mini-text">{{ historyProgress }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 数据统计盒子 -->
        <div class="panel-box stats-box">
          <div class="box-title">
            <i class="bi bi-bar-chart-fill me-2"></i>数据统计
          </div>
          <div class="box-content">
            <!-- 飞行时长图表 -->
            <div class="chart-container">
              <div class="chart-title">近7天飞行时长</div>
              <div ref="flightChartRef" class="chart"></div>
            </div>

            <!-- 作业面积图表 -->
            <div class="chart-container">
              <div class="chart-title">近7天作业面积</div>
              <div ref="areaChartRef" class="chart"></div>
            </div>

            <!-- 作物分布图表 -->
            <div class="chart-container">
              <div class="chart-title">作物分布</div>
              <div ref="statsChartRef" class="chart"></div>
            </div>
          </div>
        </div>

        <!-- 底部控制栏 -->
        <div class="bottom-toolbar">
          <div class="toolbar-group">
            <button class="toolbar-btn" @click="gotoCenter()">
              <i class="bi bi-house-door-fill"></i>
              <span>回到中心</span>
            </button>
            <button class="toolbar-btn" @click="toggleInvade()" :class="{ active: isInvadeMode }">
              <i class="bi bi-shield-exclamation"></i>
              <span>{{ isInvadeMode ? '关闭告警' : '越界告警' }}</span>
            </button>
            <button class="toolbar-btn" @click="toggleDronView()" :class="{ active: isFirstView }">
              <i class="bi bi-airplane"></i>
              <span>{{ isFirstView ? '退出巡航' : '无人机巡航' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.digital-twin-page {
  min-height: 100vh;
  background: #f0f2f5;
  overflow: hidden;
}

.loading-overlay,
.error-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  z-index: 2000;
}

.loading-content,
.error-content {
  text-align: center;
  color: #333;
}

.loading-content p,
.error-content p {
  color: #666;
  margin-bottom: 1rem;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 3px solid rgba(59, 130, 246, 0.2);
  border-top-color: #3b82f6;
  border-radius: 50%;
  margin: 0 auto 20px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ========== 大屏布局样式 ========== */
.digital-twin-page {
  min-height: 100vh;
  background: #f0f2f5;
  overflow: hidden;
}

/* 大屏顶部标题栏 */
.dashboard-header {
  height: 56px;
  background: linear-gradient(90deg, #ffffff 0%, #f8fafc 100%);
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 100;
}

.dashboard-header::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--primary-color, #3b82f6), var(--accent-color, #06b6d4), transparent);
}

.header-left {
  display: flex;
  align-items: center;
  width: 200px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: linear-gradient(135deg, var(--primary-color, #3b82f6), var(--accent-color, #06b6d4));
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.back-btn:hover {
  transform: translateX(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.back-btn i {
  font-size: 16px;
}

.header-title {
  display: flex;
  align-items: center;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-color, #3b82f6), var(--accent-color, #06b6d4));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
}

.header-logo {
  width: 28px;
  height: 28px;
  margin-right: 8px;
  object-fit: contain;
}

.header-info {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 280px;
}

.time-weather {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 14px;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
  border-radius: 20px;
  border: 1px solid #e5e7eb;
}

.time-part {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weather-divider {
  width: 1px;
  height: 20px;
  background: #e5e7eb;
}

.weather-part {
  display: flex;
  align-items: center;
  gap: 6px;
}

.current-time {
  font-size: 15px;
  font-weight: 600;
  color: var(--primary-color, #3b82f6);
  font-family: 'Courier New', monospace;
}

.date {
  font-size: 12px;
  color: #94a3b8;
}

.weather-icon {
  font-size: 16px;
  color: #f59e0b;
}

.weather-temp {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.weather-text {
  font-size: 12px;
  color: #78716c;
}

/* 大屏主体容器 */
.dashboard-container {
  display: grid;
  grid-template-columns: 260px 1fr 300px;
  gap: 16px;
  padding: 16px;
  height: calc(100vh - 56px);
  position: relative;
}

/* 左侧面板 */
.left-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 50;
}

/* 中间地图区域 */
.center-panel {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.map-wrapper {
  width: 100%;
  height: 100%;
  background: #e5e7eb;
  border-radius: 12px;
}

.map-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  background: radial-gradient(ellipse at center, transparent 70%, rgba(0, 0, 0, 0.05) 100%);
  z-index: 5;
}

/* 右侧面板 */
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 50;
  overflow-y: auto;
  overflow-x: visible;
  padding-right: 4px;
  max-height: calc(100vh - 80px);
}

/* 右侧面板滚动条样式 */
.right-panel::-webkit-scrollbar {
  width: 4px;
}

.right-panel::-webkit-scrollbar-track {
  background: transparent;
}

.right-panel::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}

.right-panel::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 面板盒子基础样式 */
.panel-box {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.panel-box .box-title {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  background: linear-gradient(90deg, var(--primary-color, #3b82f6), var(--accent-color, #06b6d4));
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.panel-box .box-title i {
  color: #ffffff;
}

.panel-box .box-content {
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
  background: #fafbfc;
}

/* 自定义滚动条 */
.panel-box .box-content::-webkit-scrollbar {
  width: 4px;
}

.panel-box .box-content::-webkit-scrollbar-track {
  background: #f1f3f5;
  border-radius: 2px;
}

.panel-box .box-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}

/* 专题模式盒子 */
.topic-box {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.topic-box .box-content {
  flex: 1;
  max-height: none;
}

.panel-box .box-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 农场位置选择 */
.location-item {
  padding: 10px 12px;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  border-radius: 6px;
  margin-bottom: 4px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

.location-item:hover {
  background: var(--primary-color, #3b82f6);
  color: #ffffff;
  border-color: var(--primary-color, #3b82f6);
}

.location-item.active {
  background: linear-gradient(135deg, var(--primary-color, #3b82f6), var(--accent-color, #06b6d4));
  color: #ffffff;
  font-weight: 600;
  border-color: transparent;
}

.location-item i {
  color: inherit;
}

/* 图层网格 */
.layer-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.layer-item {
  padding: 8px 12px;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  border-radius: 6px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

.layer-item:hover {
  background: #f1f5f9;
  color: var(--primary-color, #3b82f6);
  border-color: var(--primary-color, #3b82f6);
}

.layer-item.active {
  color: #10b981;
  background: #f0fdf4;
  border-color: #10b981;
}

.layer-item i {
  margin-right: 8px;
  color: inherit;
}

/* 专题选择 */
.topic-item {
  padding: 10px 12px;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 6px;
  margin-bottom: 4px;
  text-align: center;
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

.topic-item:hover {
  background: var(--primary-color, #3b82f6);
  color: #ffffff;
  border-color: var(--primary-color, #3b82f6);
}

/* ========== 数据统计盒子 ========== */
.stats-box {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.stats-box .box-content {
  max-height: none !important;
  overflow-y: visible !important;
  padding: 12px;
  background: #fafbfc;
  min-height: 400px;
}

/* 今日数据卡片 */
.today-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
  width: 100%;
  min-width: 0;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  min-width: 0;
  width: 100%;
}

.stat-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 16px;
  color: #fff;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.stat-icon.cyan {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 11px;
  color: #94a3b8;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.stat-value small {
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
  margin-left: 2px;
}

/* 图表容器 */
.chart-container {
  margin-bottom: 12px;
  min-height: 150px;
}

.chart-container:last-child {
  margin-bottom: 0;
}

.chart-title {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #e5e7eb;
}

.chart {
  width: 100%;
  height: 120px;
  min-height: 120px;
}

.bottom-toolbar {
  flex-shrink: 0;
  position: sticky;
  bottom: 0;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 10;
}

.toolbar-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #475569;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.toolbar-btn:hover {
  background: var(--primary-color, #3b82f6);
  color: #ffffff;
  border-color: var(--primary-color, #3b82f6);
}

.toolbar-btn.active {
  background: linear-gradient(135deg, var(--primary-color, #3b82f6), var(--accent-color, #06b6d4));
  color: #ffffff;
  border-color: transparent;
}

/* 无人机监控盒子 */
.drone-monitor-box {
  flex-shrink: 0;
}

.drone-monitor-box .box-content {
  max-height: none;
  display: flex;
  flex-direction: column;
}

.live-badge {
  display: flex;
  align-items: center;
  margin-left: auto;
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.live-dot {
  width: 6px;
  height: 6px;
  background: #ef4444;
  border-radius: 50%;
  margin-right: 4px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* 无人机信息卡片 */
.drone-info-card {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.drone-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e7eb;
}

.drone-color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  box-shadow: 0 0 6px currentColor;
}

.drone-name {
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
}

.drone-status {
  margin-left: auto;
  font-size: 12px;
  font-weight: 500;
}

/* 状态网格 */
.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.status-item.full {
  grid-column: 1 / -1;
}

.status-label {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.status-value {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  font-family: 'Courier New', monospace;
}

.status-value.small {
  font-size: 11px;
  word-break: break-all;
  color: #475569;
}

/* 迷你电量条 */
.battery-mini {
  position: relative;
  height: 20px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.battery-mini-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 4px;
  transition: all 0.5s ease;
}

.battery-mini-text {
  position: relative;
  z-index: 1;
  font-size: 11px;
  font-weight: 700;
  color: #475569;
  text-shadow: 0 1px 1px rgba(255, 255, 255, 0.8);
}

/* 迷你信号条 */
.signal-mini {
  display: flex;
  align-items: center;
  gap: 2px;
}

.signal-mini i {
  font-size: 10px;
  color: #10b981;
}

.signal-mini span {
  margin-left: 4px;
  font-size: 12px;
  color: #475569;
  font-weight: 500;
}

/* 控制区域 */
.control-section {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
}

.control-buttons {
  display: flex;
  gap: 8px;
}

.ctrl-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  color: #475569;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.ctrl-btn:hover {
  background: var(--primary-color, #3b82f6);
  color: #ffffff;
  border-color: var(--primary-color, #3b82f6);
}

.ctrl-btn.active {
  background: var(--accent-color, #06b6d4);
  border-color: var(--accent-color, #06b6d4);
  color: #ffffff;
}

.ctrl-btn-danger {
  border-color: #fecaca;
}

.ctrl-btn-danger:hover {
  background: #ef4444;
  color: #ffffff;
  border-color: #ef4444;
}

/* 迷你进度条 */
.progress-bar-mini {
  margin-top: 10px;
  height: 20px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.progress-mini-fill {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
  border-radius: 4px;
  transition: width 0.1s linear;
}

.progress-mini-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  font-weight: 600;
  color: #78350f;
}
</style>

<style>
/* 高德地图信息窗口样式 */
.amap-info-window {
  width: 150px;
  background: #fff;
  border-radius: 8px;
  padding: 8px 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  position: relative;
  font-size: 12px;
}

.amap-info-window p {
  line-height: 1.6em;
  margin: 0;
  color: #333;
}

/* 地图容器样式调整 */
.digital-twin-page .amap-container {
  border-radius: 12px;
}
</style>
