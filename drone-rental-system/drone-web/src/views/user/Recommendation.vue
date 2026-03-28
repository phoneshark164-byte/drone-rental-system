<template>
  <div class="user-recommendation">
    <!-- 使用共享的 UserHeader 组件 -->
    <UserHeader />

    <!-- 主要内容 -->
    <div class="recommendation-content">
      <!-- 页面标题 -->
      <section class="page-header-section">
        <div class="container">
          <div class="row align-items-center">
            <div class="col-lg-8">
              <h1 class="page-title">
                <i class="bi bi-magic me-2"></i>智能推荐
              </h1>
              <p class="page-subtitle">根据您的需求，为您推荐最合适的无人机</p>
            </div>
            <div class="col-lg-4 text-end">
              <div class="header-stats">
                <div class="stat-item">
                  <span class="stat-number">8+</span>
                  <span class="stat-label">使用场景</span>
                </div>
                <div class="stat-item">
                  <span class="stat-number">AI</span>
                  <span class="stat-label">智能匹配</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 推荐内容 -->
      <section class="recommendation-body">
        <div class="container">
          <div class="row">
            <!-- 左侧：推荐表单 -->
            <div class="col-lg-4">
              <div class="recommendation-form card">
                <div class="card-header">
                  <h5 class="mb-0"><i class="bi bi-sliders me-2"></i>筛选条件</h5>
                </div>
                <div class="card-body">
                  <!-- 场景选择 -->
                  <div class="mb-4">
                    <label class="form-label d-flex justify-content-between">
                      <span>使用场景</span>
                      <small class="text-muted" v-if="selectedScenario">已选择</small>
                    </label>
                    <div class="scenario-grid">
                      <div
                        v-for="scenario in scenarios"
                        :key="scenario.code"
                        class="scenario-card"
                        :class="{ active: selectedScenario === scenario.code }"
                        @click="selectScenario(scenario)"
                      >
                        <div class="scenario-icon">
                          <i :class="scenario.icon || 'bi bi-circle'"></i>
                        </div>
                        <span class="scenario-name">{{ scenario.name }}</span>
                      </div>
                    </div>
                  </div>

                  <!-- 预算范围 -->
                  <div class="mb-4">
                    <label class="form-label d-flex justify-content-between">
                      <span>预算范围</span>
                      <small class="text-primary">¥{{ budget }}/小时</small>
                    </label>
                    <input
                      type="range"
                      class="form-range"
                      min="20"
                      max="500"
                      step="10"
                      v-model.number="budget"
                    />
                    <div class="d-flex justify-content-between text-muted small mt-1">
                      <span>¥20</span>
                      <span>¥500</span>
                    </div>
                  </div>

                  <!-- 租赁时长 -->
                  <div class="mb-4">
                    <label class="form-label">租赁时长</label>
                    <div class="duration-options">
                      <button
                        v-for="d in durationOptions"
                        :key="d.value"
                        class="duration-btn"
                        :class="{ active: duration === d.value }"
                        @click="duration = d.value"
                      >
                        {{ d.label }}
                      </button>
                    </div>
                  </div>

                  <!-- 获取推荐按钮 -->
                  <button class="btn btn-recommend w-100" @click="getRecommendation" :disabled="loading || !selectedScenario">
                    <span v-if="!loading">
                      <i class="bi bi-stars me-2"></i>获取推荐
                    </span>
                    <span v-else>
                      <span class="spinner-border spinner-border-sm me-2"></span>
                      推荐中...
                    </span>
                  </button>
                </div>
              </div>

              <!-- 推荐历史 -->
              <div class="card mt-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                  <h6 class="mb-0"><i class="bi bi-clock-history me-2"></i>推荐历史</h6>
                  <small v-if="history.length > 0" class="text-muted">最近5次</small>
                </div>
                <div class="card-body">
                  <div v-if="history.length === 0" class="empty-history text-center py-4">
                    <i class="bi bi-clock-history"></i>
                    <p class="mb-0">暂无推荐历史</p>
                  </div>
                  <div v-else class="history-list">
                    <div v-for="item in history.slice(0, 5)" :key="item.id" class="history-item">
                      <div class="history-left">
                        <div class="history-scenario">
                          <i :class="getScenarioIcon(item.scenario)" class="me-1"></i>
                          {{ getScenarioName(item.scenario) }}
                        </div>
                        <div class="history-time">{{ formatTime(item.createTime) }}</div>
                      </div>
                      <div class="history-meta">
                        <span v-if="item.budget" class="history-budget">¥{{ item.budget }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧：推荐结果 -->
            <div class="col-lg-8">
              <div class="recommendation-results">
                <!-- 推荐结果头部 -->
                <div class="results-header" v-if="hasSearched">
                  <div class="d-flex justify-content-between align-items-center">
                    <h4 class="mb-0">
                      <i class="bi bi-lightbulb-fill me-2 text-warning"></i>
                      为您推荐 <span class="text-primary">{{ recommendations.length }}</span> 款无人机
                    </h4>
                    <button v-if="recommendations.length > 0" class="btn btn-sm btn-outline-secondary" @click="resetFilters">
                      <i class="bi bi-arrow-clockwise me-1"></i>重置
                    </button>
                  </div>
                  <div class="results-summary mt-2" v-if="recommendations.length > 0">
                    <span class="summary-item">
                      <i class="bi bi-funnel me-1"></i>
                      基于 {{ getScenarioName(selectedScenario) }} 场景
                    </span>
                    <span class="summary-item">
                      <i class="bi bi-cash-stack me-1"></i>
                      预算 ¥{{ budget }}/小时
                    </span>
                  </div>
                </div>

                <!-- 初始状态 -->
                <div v-if="!hasSearched" class="initial-state">
                  <div class="welcome-section">
                    <div class="welcome-icon">
                      <i class="bi bi-robot"></i>
                    </div>
                    <h3>智能推荐系统</h3>
                    <p>选择您的使用场景，AI 将为您推荐最适合的无人机</p>
                  </div>
                  <div class="intro-cards">
                    <div class="intro-card" @click="quickSelect('photography')">
                      <div class="intro-icon photography">
                        <i class="bi bi-camera-fill"></i>
                      </div>
                      <h5>航拍摄影</h5>
                      <p>风景拍摄、婚纱摄影</p>
                    </div>
                    <div class="intro-card" @click="quickSelect('inspection')">
                      <div class="intro-icon inspection">
                        <i class="bi bi-building"></i>
                      </div>
                      <h5>设备巡检</h5>
                      <p>设备巡检、建筑监测</p>
                    </div>
                    <div class="intro-card" @click="quickSelect('agriculture')">
                      <div class="intro-icon agriculture">
                        <i class="bi bi-flower1"></i>
                      </div>
                      <h5>农业植保</h5>
                      <p>农田喷洒、作物监测</p>
                    </div>
                    <div class="intro-card" @click="quickSelect('racing')">
                      <div class="intro-icon racing">
                        <i class="bi bi-lightning-fill"></i>
                      </div>
                      <h5>竞速飞行</h5>
                      <p>FPV竞速、特技飞行</p>
                    </div>
                    <div class="intro-card" @click="quickSelect('survey')">
                      <div class="intro-icon survey">
                        <i class="bi bi-map"></i>
                      </div>
                      <h5>测绘测量</h5>
                      <p>地形测绘、工程测量</p>
                    </div>
                    <div class="intro-card" @click="quickSelect('delivery')">
                      <div class="intro-icon delivery">
                        <i class="bi bi-truck"></i>
                      </div>
                      <h5>物流配送</h5>
                      <p>货物运输、快递配送</p>
                    </div>
                    <div class="intro-card" @click="quickSelect('learning')">
                      <div class="intro-icon learning">
                        <i class="bi bi-mortarboard-fill"></i>
                      </div>
                      <h5>新手入门</h5>
                      <p>初学者练习、基础飞行</p>
                    </div>
                    <div class="intro-card" @click="quickSelect('surveillance')">
                      <div class="intro-icon surveillance">
                        <i class="bi bi-shield-fill-check"></i>
                      </div>
                      <h5>安防监控</h5>
                      <p>安全巡逻、实时监控</p>
                    </div>
                  </div>
                </div>

                <!-- 加载状态 -->
                <div v-else-if="loading" class="loading-state">
                  <div class="spinner-border text-primary" role="status"></div>
                  <p class="mt-3">正在为您分析最佳选择...</p>
                </div>

                <!-- 推荐结果列表 -->
                <div v-else-if="recommendations.length > 0" class="results-list">
                  <transition-group name="list">
                    <div
                      v-for="(item, index) in recommendations"
                      :key="item.vehicleId"
                      class="recommendation-card"
                      :class="{ 'top-pick': index === 0, 'high-match': (item.finalScore || item.score || 0) >= 80, 'medium-match': (item.finalScore || item.score || 0) >= 60 }"
                    >
                      <div v-if="index === 0" class="recommend-badge">
                        <i class="bi bi-trophy-fill me-1"></i>首选推荐
                      </div>
                      <div class="row g-0">
                        <div class="col-md-4">
                          <div class="vehicle-image">
                            <img :src="item.imageUrl || '/images/drone-default.jpg'" :alt="item.brand + ' ' + item.model" />
                            <div class="match-score" :class="getMatchClass(item.finalScore || item.score || 0)">
                              <div class="score-value">{{ Math.round(item.finalScore || item.score || 0) }}%</div>
                              <div class="score-label">{{ getMatchLabel(item.finalScore || item.score || 0) }}</div>
                            </div>
                          </div>
                        </div>
                        <div class="col-md-8">
                          <div class="card-body p-3">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                              <div>
                                <h5 class="card-title mb-1">{{ item.brand }} {{ item.model }}</h5>
                                <p class="vehicle-no text-muted mb-0">
                                  <i class="bi bi-upc"></i> {{ item.vehicleNo }}
                                </p>
                              </div>
                              <div class="price-tag">
                                <span class="price">¥{{ item.estimatedPrice || 80 }}</span>
                                <span class="unit">/小时</span>
                              </div>
                            </div>

                            <!-- 特征标签 -->
                            <div class="features mb-3">
                              <span class="feature-badge battery" :class="getBatteryClass(item.batteryLevel)">
                                <i class="bi bi-battery-half me-1"></i>电量 {{ item.batteryLevel }}%
                              </span>
                              <span class="feature-badge">
                                <i class="bi bi-geo-alt me-1"></i>{{ item.locationDetail || '指定地点' }}
                              </span>
                            </div>

                            <!-- 推荐理由 -->
                            <div class="recommendation-reason">
                              <i class="bi bi-check-circle-fill text-success me-1"></i>
                              <span>{{ getRecommendReason(item, index) }}</span>
                            </div>

                            <!-- 操作按钮 -->
                            <div class="action-buttons mt-3">
                              <router-link :to="`/user/vehicle/${item.vehicleId}`" class="btn btn-outline-primary btn-sm">
                                <i class="bi bi-eye me-1"></i>查看详情
                              </router-link>
                              <router-link :to="`/user/vehicles`" class="btn btn-primary btn-sm">
                                <i class="bi bi-calendar-check me-1"></i>立即预订
                              </router-link>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </transition-group>
                </div>

                <!-- 空状态 -->
                <div v-else class="empty-state">
                  <div class="empty-icon">
                    <i class="bi bi-inbox"></i>
                  </div>
                  <h5>暂无符合条件的无人机</h5>
                  <p>尝试调整预算或选择其他使用场景</p>
                  <button class="btn btn-outline-primary" @click="resetFilters">
                    <i class="bi bi-arrow-clockwise me-1"></i>重新筛选
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Toast 通知 -->
    <div class="toast-container position-fixed bottom-0 end-0 p-3">
      <div class="toast" :class="{ show: toast.show }" role="alert">
        <div class="toast-header">
          <i :class="toast.icon" class="me-2"></i>
          <strong class="me-auto">{{ toast.title }}</strong>
          <button type="button" class="btn-close" @click="toast.show = false"></button>
        </div>
        <div class="toast-body">
          {{ toast.message }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { getUserInfo } from '@/utils/auth'
import UserHeader from '@/components/UserHeader.vue'

const router = useRouter()
const scenarios = ref([])
const selectedScenario = ref('')
const budget = ref(100)
const duration = ref(60)
const durationOptions = [
  { label: '30分钟', value: 30 },
  { label: '1小时', value: 60 },
  { label: '2小时', value: 120 },
  { label: '4小时', value: 240 },
  { label: '全天', value: 480 }
]
const recommendations = ref([])
const history = ref([])
const loading = ref(false)
const hasSearched = ref(false)
const userInfo = ref(getUserInfo())

// Toast 通知
const toast = ref({
  show: false,
  title: '',
  message: '',
  icon: 'bi bi-info-circle'
})

const showToast = (title, message, type = 'info') => {
  const icons = {
    success: 'bi bi-check-circle-fill text-success',
    error: 'bi bi-x-circle-fill text-danger',
    warning: 'bi bi-exclamation-circle-fill text-warning',
    info: 'bi bi-info-circle-fill text-primary'
  }
  toast.value = {
    show: true,
    title,
    message,
    icon: icons[type] || icons.info
  }
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

const loadScenarios = async () => {
  try {
    console.log('正在加载场景数据...')
    const res = await axios.get('/user/api/recommendation/scenarios')
    console.log('场景API响应:', res.data)
    if (res.data.code === 200) {
      scenarios.value = res.data.data
      console.log('场景数据加载成功，数量:', scenarios.value.length)
    } else {
      console.error('场景API返回错误:', res.data)
    }
  } catch (error) {
    console.error('加载场景失败:', error)
  }
}

const loadHistory = async () => {
  try {
    const res = await axios.get('/user/api/recommendation/history')
    if (res.data.code === 200) {
      history.value = res.data.data
    }
  } catch (error) {
    console.error('加载历史失败:', error)
  }
}

const selectScenario = (scenario) => {
  if (scenario) {
    selectedScenario.value = scenario.code
    if (scenario.priceRange && scenario.priceRange.length === 2) {
      const avgPrice = (scenario.priceRange[0] + scenario.priceRange[1]) / 2
      budget.value = Math.round(avgPrice)
    }
  } else if (typeof scenario === 'string') {
    // 如果直接传入场景代码字符串
    selectedScenario.value = scenario
  }
}

const getRecommendation = async () => {
  if (!selectedScenario.value) {
    showToast('提示', '请选择使用场景', 'warning')
    return
  }

  console.log('开始获取推荐，场景:', selectedScenario.value, '预算:', budget.value, '时长:', duration.value)

  loading.value = true
  hasSearched.value = true

  try {
    const res = await axios.post('/user/api/recommendation/smart', {
      scenario: selectedScenario.value,
      budget: budget.value,
      duration: duration.value
    })

    console.log('推荐API响应:', res.data)

    if (res.data.code === 200) {
      recommendations.value = res.data.data.recommendations || []
      console.log('推荐结果数量:', recommendations.value.length)
      if (recommendations.value.length > 0) {
        showToast('推荐成功', `为您找到 ${recommendations.value.length} 款合适的无人机`, 'success')
      } else {
        showToast('暂无结果', '没有找到符合条件的无人机，请调整筛选条件', 'info')
      }
      // 刷新历史记录
      loadHistory()
    } else {
      console.error('推荐失败:', res.data.message)
      showToast('推荐失败', res.data.message || '获取推荐失败', 'error')
    }
  } catch (error) {
    console.error('获取推荐失败:', error)
    showToast('网络错误', '获取推荐失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  selectedScenario.value = ''
  budget.value = 100
  duration.value = 60
  hasSearched.value = false
  recommendations.value = []
}

const getScenarioName = (code) => {
  const scenario = scenarios.value.find(s => s.code === code)
  return scenario ? scenario.name : code
}

const getRecommendReason = (item, index) => {
  if (index === 0) {
    return '综合评分最高，最适合您的需求'
  }
  const score = Math.round(item.finalScore || item.score || 0)
  if (score >= 80) {
    return '匹配度很高，性价比优秀'
  } else if (score >= 60) {
    return '基本满足您的使用需求'
  } else {
    return '可作为备选方案'
  }
}

// 快速选择场景
const quickSelect = async (scenarioCode) => {
  // 如果场景数据还未加载，先等待加载
  if (scenarios.value.length === 0) {
    await loadScenarios()
  }

  const scenario = scenarios.value.find(s => s.code === scenarioCode)
  if (scenario) {
    selectScenario(scenario)
    // 自动触发推荐
    setTimeout(() => {
      getRecommendation()
    }, 300)
  } else {
    // 如果找不到场景，直接设置代码并触发推荐
    console.warn('场景未找到，直接使用代码:', scenarioCode)
    selectedScenario.value = scenarioCode
    setTimeout(() => {
      getRecommendation()
    }, 300)
  }
}

// 获取场景图标
const getScenarioIcon = (code) => {
  const icons = {
    photography: 'bi bi-camera-fill',
    agriculture: 'bi bi-flower1',
    inspection: 'bi bi-building',
    survey: 'bi bi-map',
    racing: 'bi bi-lightning-fill',
    delivery: 'bi bi-truck',
    learning: 'bi bi-mortarboard-fill',
    surveillance: 'bi bi-shield-fill-check'
  }
  return icons[code] || 'bi bi-circle'
}

// 获取时长图标
const getDurationIcon = (value) => {
  if (value <= 60) return 'bi bi-clock'
  if (value <= 240) return 'bi bi-calendar3'
  return 'bi bi-calendar-range'
}

// 获取预算等级标签
const getBudgetLabel = (value) => {
  if (value <= 50) return '经济实惠'
  if (value <= 150) return '适中预算'
  if (value <= 300) return '高端体验'
  return '顶级享受'
}

// 获取匹配度样式类
const getMatchClass = (score) => {
  if (score >= 80) return 'high-match'
  if (score >= 60) return 'medium-match'
  return 'low-match'
}

// 获取匹配度标签
const getMatchLabel = (score) => {
  if (score >= 80) return '高度匹配'
  if (score >= 60) return '匹配良好'
  return '基本匹配'
}

// 获取电量样式类
const getBatteryClass = (level) => {
  if (level >= 80) return 'battery-high'
  if (level >= 50) return 'battery-medium'
  return 'battery-low'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

onMounted(() => {
  loadScenarios()
  loadHistory()
})
</script>

<style>
/* 为了避免与Bootstrap导航栏冲突，不使用scoped */
.user-recommendation {
  min-height: 100vh;
  background: #f8f9fa;
}

/* 推荐内容区域 */
.user-recommendation .recommendation-content {
  padding-top: 70px;
}

.user-recommendation .page-header-section {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  padding: 40px 0;
  margin-bottom: 30px;
}

.user-recommendation .page-title {
  color: white;
  font-weight: 700;
  margin-bottom: 10px;
}

.user-recommendation .page-subtitle {
  color: rgba(255,255,255,0.9);
  margin: 0;
}

.user-recommendation .header-stats {
  display: flex;
  gap: 30px;
  justify-content: flex-end;
}

.user-recommendation .stat-item {
  text-align: center;
}

.user-recommendation .stat-number {
  display: block;
  font-size: 1.8rem;
  font-weight: 700;
  color: white;
}

.user-recommendation .stat-label {
  font-size: 0.85rem;
  color: rgba(255,255,255,0.8);
}

.user-recommendation .recommendation-body {
  padding-bottom: 40px;
}

/* 推荐表单卡片 */
.user-recommendation .recommendation-form .card {
  border: none;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  border-radius: 16px;
}

.user-recommendation .recommendation-form .card-header {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
  border: none;
  border-radius: 16px 16px 0 0;
  padding: 15px 20px;
}

/* 场景网格 */
.user-recommendation .scenario-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.user-recommendation .scenario-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 15px 10px;
  border: 2px solid #e9ecef;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.user-recommendation .scenario-card:hover {
  border-color: var(--primary-color);
  background: rgba(59, 130, 246, 0.1);
  transform: translateY(-2px);
}

.user-recommendation .scenario-card.active {
  border-color: var(--primary-color);
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
}

.user-recommendation .scenario-card.active .scenario-icon i {
  color: white;
}

.user-recommendation .scenario-icon {
  margin-bottom: 6px;
}

.user-recommendation .scenario-icon i {
  font-size: 22px;
  color: var(--primary-color);
}

.user-recommendation .scenario-name {
  font-size: 12px;
  color: #495057;
  font-weight: 500;
  margin-bottom: 2px;
}

/* 时长选项 */
.user-recommendation .duration-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-recommendation .duration-btn {
  flex: 1;
  min-width: 70px;
  padding: 10px 12px;
  border: 2px solid #e9ecef;
  background: white;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.user-recommendation .duration-btn:hover {
  border-color: var(--primary-color);
}

.user-recommendation .duration-btn.active {
  border-color: var(--primary-color);
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--accent-color) 100%);
  color: white;
}

.user-recommendation .duration-btn.active i {
  color: white;
}

/* 推荐按钮 */
.user-recommendation .btn-recommend {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--accent-color) 100%);
  border: none;
  color: white;
  padding: 12px;
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.2s;
}

.user-recommendation .btn-recommend:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
}

.user-recommendation .btn-recommend:disabled {
  opacity: 0.6;
}

/* 推荐结果 */
.user-recommendation .recommendation-results {
  min-height: 500px;
}

.user-recommendation .results-header {
  margin-bottom: 20px;
}

.user-recommendation .results-header h4 {
  color: #495057;
  font-weight: 600;
}

.user-recommendation .results-summary {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #6c757d;
}

.user-recommendation .summary-item {
  display: flex;
  align-items: center;
}

/* 空状态 */
.user-recommendation .empty-state,
.user-recommendation .initial-state {
  text-align: center;
  padding: 40px 20px;
  color: #6c757d;
}

.user-recommendation .welcome-section {
  margin-bottom: 35px;
}

.user-recommendation .welcome-icon {
  width: 90px;
  height: 90px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-recommendation .welcome-icon i {
  font-size: 40px;
  color: white;
}

.user-recommendation .welcome-section h3 {
  color: #495057;
  font-weight: 700;
  margin-bottom: 10px;
}

.user-recommendation .welcome-section p {
  color: #6c757d;
}

/* 初始介绍卡片 */
.user-recommendation .intro-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.user-recommendation .intro-card {
  background: white;
  padding: 30px 25px;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 4px 15px rgba(0,0,0,0.08);
  transition: all 0.3s;
  cursor: pointer;
}

.user-recommendation .intro-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.12);
}

.user-recommendation .intro-card h5 {
  font-size: 16px;
  margin-bottom: 8px;
  color: #495057;
  font-weight: 600;
}

.user-recommendation .intro-card p {
  font-size: 13px;
  color: #6c757d;
  margin: 0;
}

.user-recommendation .intro-icon {
  width: 65px;
  height: 65px;
  margin: 0 auto 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-recommendation .intro-icon i {
  font-size: 28px;
  color: white;
}

.user-recommendation .intro-icon.photography {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
}

.user-recommendation .intro-icon.inspection {
  background: linear-gradient(135deg, var(--warning-color), #d97706);
}

.user-recommendation .intro-icon.agriculture {
  background: linear-gradient(135deg, var(--success-color), #059669);
}

.user-recommendation .intro-icon.racing {
  background: linear-gradient(135deg, var(--danger-color), #dc2626);
}

.user-recommendation .intro-icon.survey {
  background: linear-gradient(135deg, var(--accent-color), #0891b2);
}

.user-recommendation .intro-icon.delivery {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}

.user-recommendation .intro-icon.learning {
  background: linear-gradient(135deg, #14b8a6, #0d9488);
}

.user-recommendation .intro-icon.surveillance {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
}

/* 推荐结果列表 */
.user-recommendation .recommendation-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  position: relative;
  transition: transform 0.2s;
  border: none;
}

.user-recommendation .recommendation-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
}

.user-recommendation .recommendation-card.top-pick {
  border: 2px solid #ffc107;
}

.user-recommendation .recommend-badge {
  position: absolute;
  top: 15px;
  left: 15px;
  background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
  color: white;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  z-index: 1;
}

.user-recommendation .vehicle-image {
  position: relative;
  height: 220px;
  overflow: hidden;
  background: #f8f9fa;
}

.vehicle-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-recommendation .match-score {
  position: absolute;
  bottom: 15px;
  right: 15px;
  background: rgba(59, 130, 246, 0.95);
  color: white;
  padding: 10px 16px;
  border-radius: 12px;
  text-align: center;
}

.user-recommendation .score-value {
  font-size: 20px;
  font-weight: 700;
}

.user-recommendation .score-label {
  font-size: 11px;
  opacity: 0.9;
}

.user-recommendation .match-score.high-match {
  background: linear-gradient(135deg, #10b981, #059669);
}

.user-recommendation .match-score.medium-match {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.user-recommendation .match-score.low-match {
  background: linear-gradient(135deg, #6c757d, #495057);
}

.user-recommendation .vehicle-no {
  font-size: 13px;
}

.user-recommendation .price-tag {
  text-align: right;
}

.user-recommendation .price-tag .price {
  font-size: 24px;
  font-weight: 700;
  color: var(--primary-color);
}

.user-recommendation .price-tag .unit {
  font-size: 14px;
  color: #6c757d;
}

.user-recommendation .features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-recommendation .feature-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  background: #f8f9fa;
  border-radius: 20px;
  font-size: 12px;
  color: #6c757d;
}

.user-recommendation .feature-badge.battery {
  background: #e3f2fd;
  color: #1976d2;
}

.user-recommendation .feature-badge.battery-high {
  background: #d1fae5;
  color: #059669;
}

.user-recommendation .feature-badge.battery-medium {
  background: #fef3c7;
  color: #d97706;
}

.user-recommendation .feature-badge.battery-low {
  background: #fee2e2;
  color: #dc2626;
}

.user-recommendation .recommendation-reason {
  font-size: 14px;
  color: #495057;
  background: rgba(59, 130, 246, 0.1);
  padding: 12px 16px;
  border-radius: 10px;
  border-left: 3px solid var(--primary-color);
}

.user-recommendation .action-buttons {
  display: flex;
  gap: 10px;
}

/* 加载状态 */
.user-recommendation .loading-state {
  text-align: center;
  padding: 100px 20px;
}

.user-recommendation .loading-state .spinner-border {
  width: 50px;
  height: 50px;
}

/* 历史记录 */
.user-recommendation .history-list {
  max-height: 200px;
  overflow-y: auto;
}

.user-recommendation .history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f1f3f5;
}

.user-recommendation .history-item:last-child {
  border-bottom: none;
}

.user-recommendation .history-left {
  display: flex;
  flex-direction: column;
}

.user-recommendation .history-scenario {
  font-size: 13px;
  font-weight: 500;
  color: #495057;
}

.user-recommendation .history-time {
  font-size: 11px;
  color: #6c757d;
  margin-top: 2px;
}

.user-recommendation .history-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #6c757d;
}

.user-recommendation .history-budget {
  color: #10b981;
  font-weight: 500;
}

.user-recommendation .empty-history {
  color: #adb5bd;
}

.user-recommendation .empty-history i {
  font-size: 32px;
  display: block;
  margin-bottom: 8px;
}

/* Toast 通知 */
.user-recommendation .toast {
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s;
}

.user-recommendation .toast.show {
  opacity: 1;
  visibility: visible;
}

/* 列表过渡动画 */
.user-recommendation .list-enter-active,
.user-recommendation .list-leave-active {
  transition: all 0.4s ease;
}

.user-recommendation .list-enter-from {
  opacity: 0;
  transform: translateY(30px);
}

.user-recommendation .list-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

/* 响应式 */
@media (max-width: 992px) {
  .user-recommendation .intro-cards {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .user-recommendation .intro-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .user-recommendation .scenario-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .user-recommendation .results-summary {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
