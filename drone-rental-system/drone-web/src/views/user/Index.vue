<template>
  <div class="user-home">
    <!-- 导航栏 -->
    <nav class="navbar navbar-expand-lg fixed-top" :class="{ 'scrolled': isScrolled }">
      <div class="container">
        <router-link class="navbar-brand" to="/">
          <i class="bi bi-airplane-fill me-2"></i>无人机租赁
        </router-link>
        <button class="navbar-toggler" type="button" @click="toggleNavbar">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" :class="{ 'show': navbarOpen }" id="navbarNav">
          <ul class="navbar-nav ms-auto">
            <li class="nav-item">
              <router-link class="nav-link" to="/">首页</router-link>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="#features">服务特色</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="#vehicles">无人机列表</a>
            </li>
            <li class="nav-item">
              <a class="nav-link" href="#about">关于我们</a>
            </li>
            <li class="nav-item" v-if="isLoggedIn">
              <router-link class="nav-link" to="/user/recommendation">智能推荐</router-link>
            </li>
            <li class="nav-item" v-if="isLoggedIn">
              <router-link class="nav-link" to="/user/orders">我的订单</router-link>
            </li>
            <li class="nav-item" v-if="isLoggedIn">
              <router-link class="nav-link" to="/user/detection">损伤检测</router-link>
            </li>
            <li class="nav-item" v-if="isLoggedIn">
              <router-link class="nav-link" to="/user/digital-twin">
                <i class="bi bi-globe-americas me-1"></i>智慧农场
              </router-link>
            </li>
            <li class="nav-item position-static" v-if="isLoggedIn">
              <a class="nav-link notification-icon" @click="showNotifications = !showNotifications">
                <i class="bi bi-bell"></i>
                <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              </a>
              <!-- 通知下拉菜单 -->
              <div class="notification-dropdown" :class="{ 'show': showNotifications }" @click.stop>
                <div class="notification-header">
                  <h6 class="mb-0">通知</h6>
                  <div class="notification-actions">
                    <a v-if="unreadCount > 0" @click="markAllAsRead" class="mark-all-btn">全部已读</a>
                  </div>
                </div>
                <div class="notification-list">
                  <div v-if="userNotifications.length === 0" class="no-notifications">
                    <i class="bi bi-bell-slash"></i>
                    <p>暂无通知</p>
                  </div>
                  <div
                    v-for="notification in userNotifications"
                    :key="notification.id"
                    class="notification-item"
                    :class="{ 'unread': notification.isRead === 0 }"
                    @click="handleNotificationClick(notification)"
                  >
                    <div class="notif-item-icon" :class="'type-' + notification.type">
                      <i class="bi" :class="getNotificationIcon(notification.type)"></i>
                    </div>
                    <div class="notification-content">
                      <div class="notification-title">{{ notification.title }}</div>
                      <div class="notification-text">{{ notification.content }}</div>
                      <div class="notification-time">{{ formatTime(notification.createTime) }}</div>
                    </div>
                    <button
                      class="notification-delete"
                      @click.stop="deleteNotification(notification.id)"
                      title="删除"
                    >
                      <i class="bi bi-x"></i>
                    </button>
                  </div>
                </div>
              </div>
            </li>
          </ul>
          <div class="d-flex ms-2 align-items-center gap-2">
            <!-- 未登录时显示登录入口下拉菜单 -->
            <template v-if="!isLoggedIn">
              <div class="dropdown position-static">
                <button
                  class="btn btn-primary rounded-pill px-4 dropdown-toggle"
                  type="button"
                  @click="showLoginDropdown = !showLoginDropdown"
                  style="background: linear-gradient(135deg, var(--primary-color), var(--accent-color)); border: none;"
                >
                  <i class="bi bi-box-arrow-in-right me-2"></i>登录入口
                </button>
                <ul class="dropdown-menu dropdown-menu-end" :class="{ 'show': showLoginDropdown }" @click.stop>
                  <li><router-link class="dropdown-item" to="/user/login" @click="showLoginDropdown = false">
                    <i class="bi bi-person me-2"></i>用户登录/注册
                  </router-link></li>
                  <li><hr class="dropdown-divider"></li>
                  <li><router-link class="dropdown-item" to="/operator/login" @click="showLoginDropdown = false">
                    <i class="bi bi-person-workspace me-2"></i>运营方登录
                  </router-link></li>
                  <li><router-link class="dropdown-item" to="/admin/login" @click="showLoginDropdown = false">
                    <i class="bi bi-shield-lock me-2"></i>管理员登录
                  </router-link></li>
                </ul>
              </div>
            </template>
            <!-- 已登录时显示用户信息和操作 -->
            <template v-else>
              <router-link to="/user/profile" class="btn btn-primary rounded-pill px-3 d-flex align-items-center"
                style="background: linear-gradient(135deg, var(--primary-color), var(--accent-color)); border: none; font-size: 14px;">
                <img v-if="userInfo?.avatar" :src="userInfo.avatar" class="user-avatar-small me-1" alt="头像" />
                <i v-else class="bi bi-person-circle me-1"></i>{{ userInfo?.username || '用户' }}
              </router-link>
              <button @click="handleLogout" class="btn btn-outline-light rounded-pill px-2" style="font-size: 14px;">
                <i class="bi bi-box-arrow-right"></i> 退出
              </button>
            </template>
          </div>
        </div>
      </div>
    </nav>

    <!-- Hero区域 -->
    <section class="hero-section">
      <div id="heroCarousel" class="carousel slide carousel-fade" data-bs-ride="carousel" data-bs-interval="5000">
        <div class="carousel-indicators">
          <button
            v-for="(slide, index) in heroSlides"
            :key="index"
            type="button"
            :data-bs-target="'#heroCarousel'"
            :data-bs-slide-to="index"
            :class="{ active: index === 0 }"
          ></button>
        </div>
        <div class="carousel-inner">
          <div
            v-for="(slide, index) in heroSlides"
            :key="index"
            class="carousel-item"
            :class="{ active: index === 0 }"
          >
            <img :src="slide.image" :alt="slide.title" />
            <div class="carousel-overlay"></div>
            <div class="carousel-caption">
              <h1 class="hero-title">{{ slide.title }}</h1>
              <p class="hero-subtitle">{{ slide.subtitle }}</p>
              <div class="hero-buttons">
                <router-link to="/user/vehicles" class="btn btn-primary btn-lg me-3">
                  <i class="bi bi-airplane me-2"></i>立即租赁
                </router-link>
                <a href="#features" class="btn btn-outline-light btn-lg">
                  <i class="bi bi-info-circle me-2"></i>了解更多
                </a>
              </div>
            </div>
          </div>
        </div>
        <button class="carousel-control-prev" type="button" data-bs-target="#heroCarousel" data-bs-slide="prev">
          <span class="carousel-control-prev-icon"></span>
        </button>
        <button class="carousel-control-next" type="button" data-bs-target="#heroCarousel" data-bs-slide="next">
          <span class="carousel-control-next-icon"></span>
        </button>
      </div>
    </section>

    <!-- 数字孪生入口 -->
    <section class="digital-twin-promo-section py-5">
      <div class="container">
        <div class="digital-twin-card">
          <div class="row align-items-center">
            <div class="col-lg-6">
              <div class="promo-content">
                <span class="promo-badge">
                  <i class="bi bi-stars me-2"></i>创新功能
                </span>
                <h2 class="promo-title">智慧农业数字孪生平台</h2>
                <p class="promo-desc">
                  3D可视化 · 实时监控 · 数据分析
                </p>
                <ul class="promo-features">
                  <li><i class="bi bi-check-circle-fill"></i>实时无人机状态监控</li>
                  <li><i class="bi bi-check-circle-fill"></i>3D地图可视化展示</li>
                  <li><i class="bi bi-check-circle-fill"></i>飞行轨迹回放分析</li>
                  <li><i class="bi bi-check-circle-fill"></i>多专题模式切换</li>
                </ul>
                <router-link to="/user/digital-twin" class="btn btn-promo">
                  <i class="bi bi-globe-americas me-2"></i>体验数字孪生
                  <i class="bi bi-arrow-right ms-2"></i>
                </router-link>
              </div>
            </div>
            <div class="col-lg-6">
              <div class="promo-visual">
                <div class="visual-circle">
                  <div class="visual-inner">
                    <i class="bi bi-airplane-engines-fill"></i>
                  </div>
                </div>
                <div class="float-icon icon-1"><i class="bi bi-geo-alt-fill"></i></div>
                <div class="float-icon icon-2"><i class="bi bi-activity"></i></div>
                <div class="float-icon icon-3"><i class="bi bi-graph-up"></i></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 服务特色 -->
    <section id="features" class="features-section py-5">
      <div class="container">
        <div class="text-center mb-5">
          <h2 class="section-title">服务特色</h2>
          <p class="section-subtitle">为您提供最便捷的无人机租赁服务</p>
        </div>
        <div class="row g-4">
          <div class="col-md-3 col-sm-6">
            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-geo-alt"></i>
              </div>
              <h5 class="feature-title">就近取还</h5>
              <p class="feature-desc">基于地图定位，轻松找到附近可用无人机</p>
            </div>
          </div>
          <div class="col-md-3 col-sm-6">
            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-clock"></i>
              </div>
              <h5 class="feature-title">灵活时长</h5>
              <p class="feature-desc">按分钟计费，支持多种租用时长选择</p>
            </div>
          </div>
          <div class="col-md-3 col-sm-6">
            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-shield-check"></i>
              </div>
              <h5 class="feature-title">安全保障</h5>
              <p class="feature-desc">定期维护检查，确保飞行安全可靠</p>
            </div>
          </div>
          <div class="col-md-3 col-sm-6">
            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-credit-card"></i>
              </div>
              <h5 class="feature-title">便捷支付</h5>
              <p class="feature-desc">多种支付方式，账单透明清晰</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 无人机列表 -->
    <section id="vehicles" class="vehicles-section py-5">
      <div class="container">
        <div class="text-center mb-5">
          <h2 class="section-title">精选无人机</h2>
          <p class="section-subtitle">多款高品质无人机供您选择</p>
        </div>
        <div class="row g-4">
          <div class="col-md-4 col-sm-6" v-for="drone in drones" :key="drone.id">
            <div class="drone-card">
              <div class="drone-image">
                <img :src="drone.image" :alt="drone.model" />
                <span class="drone-status" :class="'status-' + drone.status">
                  {{ getStatusText(drone.status) }}
                </span>
              </div>
              <div class="drone-info">
                <h5 class="drone-model">{{ drone.brand }} {{ drone.model }}</h5>
                <p class="drone-location">
                  <i class="bi bi-geo-alt me-1"></i>{{ drone.location }}
                </p>
                <div class="drone-meta">
                  <span class="battery-level">
                    <i class="bi bi-battery me-1"></i>{{ drone.battery }}%
                  </span>
                  <span class="flight-hours">
                    <i class="bi bi-clock me-1"></i>{{ drone.flightHours }}h
                  </span>
                </div>
                <div class="drone-price">
                  <span class="price">¥{{ drone.pricePerMinute }}/分钟</span>
                  <router-link
                    v-if="isLoggedIn && drone.status === 1"
                    :to="'/user/vehicle/' + drone.id"
                    class="btn btn-primary btn-sm"
                  >
                    立即租赁
                  </router-link>
                  <router-link
                    v-else-if="!isLoggedIn"
                    to="/user/login"
                    class="btn btn-outline-primary btn-sm"
                  >
                    登录租赁
                  </router-link>
                  <button v-else class="btn btn-secondary btn-sm" disabled>
                    不可用
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="text-center mt-5">
          <router-link to="/user/vehicles" class="btn btn-outline-primary btn-lg">
            查看全部 <i class="bi bi-arrow-right ms-2"></i>
          </router-link>
        </div>
      </div>
    </section>

    <!-- 无人机地图 -->
    <section id="map" class="map-section py-5">
      <div class="container">
        <div class="text-center mb-5">
          <h2 class="section-title">无人机地图</h2>
          <p class="section-subtitle">实时查看附近可用无人机的位置</p>
        </div>
        <div class="map-container">
          <div class="map-info-card">
            <h6><i class="bi bi-geo-alt-fill me-2" style="color: var(--primary-color);"></i>无人机分布</h6>
            <p id="userVehicleCount">正在加载...</p>
            <div class="map-legend">
              <div class="legend-item">
                <div class="legend-dot available"></div>
                <span>可用</span>
              </div>
              <div class="legend-item">
                <div class="legend-dot in-use"></div>
                <span>使用中</span>
              </div>
              <div class="legend-item">
                <div class="legend-dot low-battery"></div>
                <span>低电量</span>
              </div>
              <div class="legend-item">
                <div class="legend-dot charging"></div>
                <span>充电中</span>
              </div>
            </div>
          </div>
          <div id="amapContainer" class="amap-container"></div>
        </div>
      </div>
    </section>

    <!-- 无人机详情模态框 -->
    <div v-if="showDetailModal" class="detail-modal-overlay" @click="showDetailModal = false">
      <div class="detail-modal-content" @click.stop>
        <div class="detail-modal-header">
          <button class="btn-close" @click="showDetailModal = false">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="detail-modal-body" v-if="selectedDrone">
          <div class="detail-modal-image">
            <img :src="selectedDrone.imageUrl || selectedDrone.image" :alt="selectedDrone.model" />
            <span class="status-badge" :class="'status-' + selectedDrone.status">
              {{ getStatusText(selectedDrone.status) }}
            </span>
            <!-- 充电进度条 -->
            <div v-if="selectedDrone.status === 3 && selectedDrone.chargingStartTime" class="charging-progress-modal">
              <div class="charging-info">
                <span class="charging-label">充电中 {{ getChargingProgress(selectedDrone).toFixed(0) }}%</span>
                <span class="charging-time">{{ getChargingTimeRemaining(selectedDrone) }}</span>
              </div>
              <div class="charging-bar">
                <div class="charging-fill" :style="{ width: getChargingProgress(selectedDrone) + '%' }"></div>
              </div>
            </div>
          </div>
          <div class="detail-modal-info">
            <h3>{{ selectedDrone.brand }} {{ selectedDrone.model }}</h3>
            <p class="detail-no">编号：{{ selectedDrone.droneNo }}</p>

            <div class="detail-specs">
              <div class="spec-item">
                <i class="bi bi-battery-half"></i>
                <span>电量 {{ getDisplayBatteryLevel(selectedDrone) }}%</span>
              </div>
              <div class="spec-item">
                <i class="bi bi-clock"></i>
                <span>飞行时长 {{ selectedDrone.flightHours }}h</span>
              </div>
              <div class="spec-item">
                <i class="bi bi-speedometer"></i>
                <span>最高速度 {{ selectedDrone.maxSpeed }}km/h</span>
              </div>
              <div class="spec-item">
                <i class="bi bi-camera"></i>
                <span>{{ selectedDrone.camera }}相机</span>
              </div>
              <div class="spec-item">
                <i class="bi bi-geo-alt"></i>
                <span>{{ selectedDrone.location }}</span>
              </div>
            </div>

            <div class="detail-price">
              <span class="price-amount">¥{{ selectedDrone.pricePerMinute }}</span>
              <span class="price-unit">/分钟</span>
            </div>
          </div>
        </div>
        <div class="detail-modal-footer">
          <button class="btn btn-secondary" @click="showDetailModal = false">关闭</button>
          <router-link
            v-if="selectedDrone && selectedDrone.status === 1"
            :to="'/user/vehicle/' + selectedDrone.id"
            class="btn btn-primary"
            @click="showDetailModal = false"
          >
            <i class="bi bi-cart-plus me-2"></i>立即租赁
          </router-link>
          <button v-else class="btn btn-secondary" disabled>
            不可租赁
          </button>
        </div>
      </div>
    </div>

    <!-- 关于我们 -->
    <section id="about" class="about-section py-5">
      <div class="container">
        <div class="row align-items-center">
          <div class="col-lg-6">
            <h2 class="section-title">关于我们</h2>
            <p class="about-text">
              我们致力于为用户提供便捷、安全、高效的无人机租赁服务。
              通过智能化的平台和遍布各地的服务网点，让用户能够轻松享受无人机带来的乐趣和便利。
            </p>
            <div class="row mt-4">
              <div class="col-6">
                <div class="stat-item">
                  <h3 class="stat-number">1000+</h3>
                  <p class="stat-label">无人机数量</p>
                </div>
              </div>
              <div class="col-6">
                <div class="stat-item">
                  <h3 class="stat-number">50+</h3>
                  <p class="stat-label">覆盖城市</p>
                </div>
              </div>
              <div class="col-6">
                <div class="stat-item">
                  <h3 class="stat-number">10000+</h3>
                  <p class="stat-label">服务用户</p>
                </div>
              </div>
              <div class="col-6">
                <div class="stat-item">
                  <h3 class="stat-number">99.9%</h3>
                  <p class="stat-label">好评率</p>
                </div>
              </div>
            </div>
          </div>
          <div class="col-lg-6">
            <div class="about-image-placeholder img-fluid rounded-4 shadow d-flex align-items-center justify-content-center">
              <i class="bi bi-airplane fs-1 me-3"></i>
              <span class="fs-4">无人机租赁服务</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="container">
        <div class="row">
          <div class="col-md-4">
            <h5 class="footer-title">
              <i class="bi bi-airplane-fill me-2"></i>无人机租赁
            </h5>
            <p class="footer-text">专业的无人机智能租赁服务平台</p>
          </div>
          <div class="col-md-4">
            <h5 class="footer-title">快速链接</h5>
            <ul class="footer-links">
              <li><router-link to="/">首页</router-link></li>
              <li><router-link to="/user/vehicles">无人机列表</router-link></li>
              <li><router-link to="/user/login">用户登录</router-link></li>
            </ul>
          </div>
          <div class="col-md-4">
            <h5 class="footer-title">联系我们</h5>
            <p class="footer-contact">
              <i class="bi bi-telephone me-2"></i>400-888-8888
            </p>
            <p class="footer-contact">
              <i class="bi bi-envelope me-2"></i>service@dronerental.com
            </p>
          </div>
        </div>
        <hr class="footer-divider" />
        <p class="footer-copyright">&copy; 2026 无人机智能租赁系统. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Carousel } from 'bootstrap'
import { useRouter, useRoute } from 'vue-router'
import { getActiveBanners, getVehicleMapData, getVehicleList } from '@/api/user'
import { isSessionValid, clearLoginInfo, getUserInfo } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const isScrolled = ref(false)
const navbarOpen = ref(false)
const isLoggedIn = ref(isSessionValid())
const userInfo = ref(getUserInfo())
const showLoginDropdown = ref(false)
const currentTime = ref(Date.now()) // 响应式当前时间
const showDetailModal = ref(false)
const selectedDrone = ref(null)

// 余额相关
const userBalance = ref('0.00')

// 时间更新定时器
let timeUpdateInterval = null

// 地图相关
let map = null
let markers = []
const mapScriptLoaded = ref(false)

// 通知相关
const showNotifications = ref(false)
const unreadCount = ref(0)
const userNotifications = ref([])

// 获取用户信息（同时检查会话是否有效）
const loadUserInfo = () => {
  const valid = isSessionValid()
  isLoggedIn.value = valid
  if (valid) {
    userInfo.value = getUserInfo()
  } else {
    userInfo.value = null
  }
}

// 退出登录
const handleLogout = () => {
  clearLoginInfo()
  isLoggedIn.value = false
  userInfo.value = null
}

// 点击外部关闭下拉菜单
const handleClickOutside = (event) => {
  if (showLoginDropdown.value) {
    const dropdown = event.target.closest('.dropdown')
    if (!dropdown || !dropdown.contains(event.target)) {
      showLoginDropdown.value = false
    }
  }
  if (showNotifications.value) {
    const notificationDropdown = event.target.closest('.notification-dropdown')
    const notificationIcon = event.target.closest('.notification-icon')
    if (!notificationDropdown && !notificationIcon) {
      showNotifications.value = false
    }
  }
}

// 获取用户余额
const fetchBalance = async () => {
  if (!isLoggedIn.value) return
  try {
    const { default: request } = await import('@/utils/request')
    const res = await request.get('/user/api/balance')
    if (res.code === 200) {
      userBalance.value = (res.data.balance || 0).toFixed(2)
    }
  } catch (e) {
    console.error('获取余额失败', e)
  }
}

// 获取用户通知
const fetchUserNotifications = async () => {
  if (!isLoggedIn.value) return
  try {
    const { default: request } = await import('@/utils/request')
    const res = await request.get('/user/api/notification/list')
    if (res.code === 200) {
      userNotifications.value = res.data || []
      unreadCount.value = userNotifications.value.filter(n => n.isRead === 0).length
    }
  } catch (e) {
    console.error('获取通知失败', e)
  }
}

// 获取通知图标
const getNotificationIcon = (type) => {
  const iconMap = {
    info: 'bi-info-circle-fill',
    success: 'bi-check-circle-fill',
    warning: 'bi-exclamation-triangle-fill',
    danger: 'bi-x-circle-fill'
  }
  return iconMap[type] || 'bi-bell-fill'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'

  return date.toLocaleDateString('zh-CN')
}

// 点击通知
const handleNotificationClick = async (notification) => {
  if (notification.isRead === 0) {
    try {
      const { default: request } = await import('@/utils/request')
      await request.post(`/user/api/notification/${notification.id}/read`)
      notification.isRead = 1
      unreadCount.value = userNotifications.value.filter(n => n.isRead === 0).length
    } catch (e) {
      console.error('标记已读失败', e)
    }
  }

  // 如果是订单相关通知，跳转到订单详情
  if (notification.relatedType === 'order' && notification.relatedId) {
    showNotifications.value = false
    router.push(`/user/order/${notification.relatedId}`)
  }
}

// 全部标记为已读
const markAllAsRead = async () => {
  try {
    const { default: request } = await import('@/utils/request')
    const res = await request.post('/user/api/notification/read-all')
    if (res.code === 200) {
      userNotifications.value.forEach(n => n.isRead = 1)
      unreadCount.value = 0
    }
  } catch (e) {
    console.error('标记全部已读失败', e)
  }
}

// 删除通知
const deleteNotification = async (id) => {
  try {
    const { default: request } = await import('@/utils/request')
    const res = await request.delete(`/user/api/notification/${id}`)
    if (res.code === 200) {
      userNotifications.value = userNotifications.value.filter(n => n.id !== id)
      unreadCount.value = userNotifications.value.filter(n => n.isRead === 0).length
    }
  } catch (e) {
    console.error('删除通知失败', e)
  }
}

// 显示充值页面（跳转到个人中心）
const showRechargeModal = () => {
  router.push('/user/profile')
}

const heroSlides = ref([])

// 从API加载轮播图
const loadBanners = async () => {
  try {
    const res = await getActiveBanners()
    console.log('轮播图API响应:', res)
    if (res.code === 200 && res.data) {
      console.log('轮播图数据:', res.data)
      heroSlides.value = res.data.map(item => ({
        image: item.imageUrl,
        title: item.title,
        subtitle: item.subtitle || ''
      }))
      console.log('处理后的轮播图:', heroSlides.value)
    } else {
      console.log('轮播图响应code不是200或data为空')
    }
  } catch (error) {
    console.error('加载轮播图失败:', error)
    // 使用默认数据作为后备
    heroSlides.value = [
      { image: '/img/train/0001.jpg', title: '无人机智能租赁系统', subtitle: '探索世界，随心而飞' }
    ]
  }
}

const drones = ref([])

// 从API加载无人机列表
const loadVehicles = async () => {
  try {
    const res = await getVehicleList({})
    if (res.code === 200 && res.data) {
      drones.value = res.data.map(d => {
        // 处理充电数据
        let chargingStartTime = null
        let chargingDuration = null
        let startBatteryLevel = null

        if (d.status === 3 && d.chargingStartTime) {
          chargingStartTime = new Date(d.chargingStartTime).getTime()
          startBatteryLevel = d.startBatteryLevel || d.batteryLevel || 0
          const batteryNeeded = 100 - startBatteryLevel
          const hoursToFull = 2 // 充满需要2小时
          chargingDuration = (batteryNeeded / 100) * hoursToFull * 60 * 60 * 1000
        }

        return {
          ...d,
          location: d.locationDetail,
          battery: d.batteryLevel,
          image: d.imageUrl,
          chargingStartTime,
          chargingDuration,
          startBatteryLevel
        }
      })
    }
  } catch (error) {
    console.error('加载无人机列表失败:', error)
  }
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
    const startBattery = drone.startBatteryLevel || drone.batteryLevel || drone.battery
    const currentBattery = Math.round(startBattery + (100 - startBattery) * progress)
    return Math.min(100, currentBattery)
  }
  return drone.batteryLevel || drone.battery
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

// 打开详情模态框
const openDetailModal = (vehicle) => {
  selectedDrone.value = vehicle
  showDetailModal.value = true
}

let carousel = null

const toggleNavbar = () => {
  navbarOpen.value = !navbarOpen.value
}

const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
}

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

// ========== 地图相关函数 ==========
// 加载高德地图脚本
const loadAMapScript = () => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      mapScriptLoaded.value = true
      resolve()
      return
    }
    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.src = 'https://webapi.amap.com/maps?v=2.0&key=bff5e715168525e6b64ceff26b8723a8'
    script.onload = () => {
      mapScriptLoaded.value = true
      resolve()
    }
    script.onerror = reject
    document.head.appendChild(script)
  })
}

// 初始化地图
const initMap = async () => {
  try {
    await loadAMapScript()

    map = new AMap.Map('amapContainer', {
      zoom: 4,
      center: [105.0, 35.0],
      viewMode: '3D'
    })

    // 显示加载中状态
    const vehicleCountEl = document.getElementById('userVehicleCount')
    if (vehicleCountEl) {
      vehicleCountEl.innerHTML = `
        <div style="font-weight:600;color:#1e293b;margin-bottom:8px;">全国</div>
        <div style="color:#64748b;">正在加载无人机...</div>
      `
    }

    // 获取无人机数据
    await fetchVehicleData()
  } catch (error) {
    console.error('地图初始化失败:', error)
  }
}

// 获取无人机地图数据
const fetchVehicleData = async () => {
  try {
    // 先尝试从API获取数据
    const res = await getVehicleMapData()
    let vehicles = []

    if (res.code === 200 && res.data) {
      // API返回的数据格式转换
      vehicles = res.data.map(v => {
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

        return {
          id: v.id,
          droneNo: v.vehicleNo,
          brand: v.brand,
          model: v.model,
          imageUrl: v.imageUrl,
          lat: v.lat,
          lng: v.lng,
          batteryLevel: v.batteryLevel,
          status: v.status,
          location: v.location,
          flightHours: 0,
          maxSpeed: 75,
          camera: '4K',
          pricePerMinute: 0.5,
          chargingStartTime,
          chargingDuration,
          startBatteryLevel
        }
      })
    } else {
      // API失败，使用本地数据
      throw new Error('API调用失败')
    }

    // 更新无人机统计
    updateVehicleCount('全国', vehicles)

    // 添加标记
    addVehicleMarkers(vehicles)
  } catch (error) {
    console.error('从API获取数据失败，使用本地数据:', error)

    // 默认无人机坐标数据（后备方案）
    const defaultCoordinates = {
      1: { lat: 39.0842, lng: 117.2009, droneNo: 'DRONE-A001' },
      2: { lat: 31.2304, lng: 121.4737, droneNo: 'DRONE-A002' },
      3: { lat: 23.1291, lng: 113.2644, droneNo: 'DRONE-B001' },
      4: { lat: 22.5431, lng: 114.0579, droneNo: 'DRONE-B002' },
      5: { lat: 30.5728, lng: 104.0668, droneNo: 'DRONE-C001' },
      6: { lat: 30.2741, lng: 120.1551, droneNo: 'DRONE-C002' }
    }

    // 从 localStorage 获取管理员保存的位置数据
    const savedDrones = localStorage.getItem('adminDrones')
    const mockData = drones.value.map(drone => {
      const coords = defaultCoordinates[drone.id]
      let lat = coords?.lat
      let lng = coords?.lng

      // 如果有管理员保存的坐标，使用保存的坐标
      if (savedDrones) {
        try {
          const parsedDrones = JSON.parse(savedDrones)
          const savedDrone = parsedDrones.find(d => d.id === drone.id)
          if (savedDrone && savedDrone.lat && savedDrone.lng) {
            lat = savedDrone.lat
            lng = savedDrone.lng
          }
        } catch (e) {
          console.error('解析坐标数据失败:', e)
        }
      }

      return {
        id: drone.id,
        droneNo: coords?.droneNo || `DRONE-${drone.id}`,
        brand: drone.brand,
        model: drone.model,
        imageUrl: drone.image,
        lat,
        lng,
        batteryLevel: drone.battery,
        battery: drone.battery,
        status: drone.status,
        location: drone.location,
        flightHours: drone.flightHours,
        maxSpeed: drone.maxSpeed || 75,
        camera: drone.camera || '4K',
        pricePerMinute: drone.pricePerMinute,
        chargingStartTime: drone.chargingStartTime || null,
        chargingDuration: drone.chargingDuration || null,
        startBatteryLevel: drone.startBatteryLevel || null
      }
    })

    updateVehicleCount('全国', mockData)
    addVehicleMarkers(mockData)
  }
}

// 更新无人机统计
const updateVehicleCount = (locationText, vehicleData) => {
  const available = vehicleData.filter(v => v.status === 1).length
  const inUse = vehicleData.filter(v => v.status === 2).length
  const lowBattery = vehicleData.filter(v => v.batteryLevel < 50).length

  const vehicleCountEl = document.getElementById('userVehicleCount')
  if (vehicleCountEl) {
    let html = ''
    if (locationText) {
      html += `<div style="font-weight:600;color:#1e293b;margin-bottom:8px;">${locationText}</div>`
    }
    html += `
      <span style="color:#10b981;">可用: ${available}</span> |
      <span style="color:#3b82f6;">使用中: ${inUse}</span> |
      <span style="color:#f59e0b;">低电量: ${lowBattery}</span>
    `
    vehicleCountEl.innerHTML = html
  }
}

// 创建标记图标
const createMarkerIcon = (color) => {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="32" height="32">
      <circle cx="16" cy="16" r="12" fill="${color}" stroke="white" stroke-width="2"/>
      <circle cx="16" cy="16" r="6" fill="white"/>
    </svg>
  `
  return `data:image/svg+xml;base64,${btoa(unescape(encodeURIComponent(svg)))}`
}

// 添加无人机标记
const addVehicleMarkers = (vehicles) => {
  if (!map) return

  // 清除现有标记
  if (markers.length > 0) {
    map.remove(markers)
    markers = []
  }

  vehicles.forEach(vehicle => {
    if (!vehicle.lat || !vehicle.lng) return

    // 根据状态确定颜色
    let color = '#10b981' // 可用-绿色
    if (vehicle.status === 2) color = '#3b82f6' // 使用中-蓝色
    else if (vehicle.status === 3) color = '#f59e0b' // 充电中-橙色
    else if (vehicle.batteryLevel < 50) color = '#f59e0b' // 低电量-橙色

    // 创建标记
    const marker = new AMap.Marker({
      position: [vehicle.lng, vehicle.lat],
      icon: new AMap.Icon({
        size: new AMap.Size(32, 32),
        image: createMarkerIcon(color),
        imageSize: new AMap.Size(32, 32)
      }),
      title: `${vehicle.brand} ${vehicle.droneNo}`,
      extData: vehicle
    })

    // 计算当前电量和充电进度
    let displayBattery = vehicle.batteryLevel
    let chargingInfo = ''
    if (vehicle.status === 3 && vehicle.chargingStartTime && vehicle.chargingDuration) {
      const elapsed = Date.now() - vehicle.chargingStartTime
      const progress = Math.min(100, Math.max(0, (elapsed / vehicle.chargingDuration) * 100))
      const startBattery = vehicle.startBatteryLevel || vehicle.batteryLevel
      displayBattery = Math.round(startBattery + (100 - startBattery) * (progress / 100))

      const remaining = Math.max(0, vehicle.chargingDuration - elapsed)
      const minutes = Math.floor((remaining % (60 * 60 * 1000)) / (60 * 1000))
      const seconds = Math.floor((remaining % (60 * 1000)) / 1000)
      chargingInfo = `<div style="margin-top:8px;padding:6px;background:#fef3c7;border-radius:6px;font-size:11px;">
        <span style="color:#d97706;">⚡ 充电中 ${progress.toFixed(0)}%</span>
        <span style="color:#b45309;float:right;">剩余 ${minutes}:${String(seconds).padStart(2, '0')}</span>
      </div>`
    }

    // 创建信息窗体（包含无人机图片）
    const content = `
      <div style="padding:0;min-width:260px;border-radius:12px;overflow:hidden;background:white;">
        <div style="position:relative;height:120px;">
          <img src="${vehicle.imageUrl}" style="width:100%;height:100%;object-fit:cover;">
          <div style="position:absolute;top:8px;right:8px;padding:4px 8px;border-radius:12px;font-size:11px;font-weight:600;background:${color};color:white;">
            ${vehicle.status === 1 ? '可用' : vehicle.status === 2 ? '使用中' : vehicle.status === 3 ? '充电中' : '其他'}
          </div>
        </div>
        <div style="padding:12px;">
          <h6 style="margin:0 0 4px;font-weight:700;color:#1e293b;">${vehicle.brand} ${vehicle.model}</h6>
          <p style="margin:4px 0;font-size:12px;color:#64748b;">
            📍 ${vehicle.droneNo} - ${vehicle.location}
          </p>
          <div style="display:flex;gap:15px;margin:8px 0;">
            <span style="font-size:12px;color:#64748b;">
              ⚡ ${displayBattery.toFixed(0)}%
            </span>
            <span style="font-size:12px;color:#64748b;">
              🕐 ${vehicle.flightHours}h
            </span>
            <span style="font-size:12px;color:#64748b;">
              💰 ¥${vehicle.pricePerMinute}/分
            </span>
          </div>
          ${chargingInfo}
          <button onclick="window.openDetailModal && window.openDetailModal(${vehicle.id})"
                  class="btn btn-sm btn-primary" style="width:100%;font-size:12px;padding:8px;margin-top:8px;border-radius:8px;background:linear-gradient(135deg,#3b82f6,#06b6d4);border:none;">
            查看详情
          </button>
        </div>
      </div>
    `

    const infoWindow = new AMap.InfoWindow({
      content: content,
      offset: new AMap.Pixel(0, -32)
    })

    marker.on('click', () => {
      infoWindow.open(map, marker.getPosition())
    })

    markers.push(marker)
  })

  map.add(markers)
}

onMounted(async () => {
  window.addEventListener('scroll', handleScroll)
  document.addEventListener('click', handleClickOutside)
  loadUserInfo()
  fetchBalance()
  fetchUserNotifications()
  // 每30秒刷新一次通知
  setInterval(fetchUserNotifications, 30000)
  // 加载轮播图
  loadBanners()
  // 从API加载无人机数据
  loadVehicles()
  // 每秒更新当前时间
  timeUpdateInterval = setInterval(() => {
    currentTime.value = Date.now()
  }, 1000)
  // 等待 DOM 更新后初始化轮播图
  await nextTick()
  const carouselEl = document.getElementById('heroCarousel')
  if (carouselEl) {
    carousel = new Carousel(carouselEl, {
      interval: 5000,
      wrap: true
    })
  }
  // 初始化地图
  initMap()

  // 将 openDetailModal 函数暴露到全局，供地图信息窗口调用
  window.openDetailModal = (id) => {
    const vehicle = drones.value.find(d => d.id === id)
    if (vehicle) {
      selectedDrone.value = vehicle
      showDetailModal.value = true
    }
  }

  // 监听头像更新事件
  window.addEventListener('user-avatar-updated', handleAvatarUpdated)
})

// 处理头像更新事件
const handleAvatarUpdated = (event) => {
  loadUserInfo()
}

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('user-avatar-updated', handleAvatarUpdated)
  if (carousel) {
    carousel.dispose()
  }
  if (timeUpdateInterval) {
    clearInterval(timeUpdateInterval)
  }
  // 清理全局函数
  window.openDetailModal = null
})

// 监听路由变化，当返回首页时刷新数据
watch(() => route.path, (newPath, oldPath) => {
  if (newPath === '/user/index' && oldPath !== '/user/index') {
    // 从其他页面返回首页时，刷新数据
    loadUserInfo()
    loadBanners()
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
:root {
  --primary-color: #3b82f6;
  --accent-color: #06b6d4;
}

/* 导航栏 */
.navbar {
  background: transparent;
  padding: 15px 0;
  transition: all 0.3s;
  border-bottom: none;
}

.navbar .container {
  max-width: 1400px;
}

/* 确保导航栏内容在一行显示 */
@media (min-width: 992px) {
  .navbar-nav {
    flex-wrap: nowrap;
  }
  .nav-item {
    white-space: nowrap;
  }
}

.navbar.scrolled {
  background: rgba(255, 255, 255, 0.98);
  padding: 10px 0;
  box-shadow: 0 2px 20px rgba(0,0,0,0.1);
  backdrop-filter: blur(10px);
}

.navbar-brand {
  font-weight: 700;
  font-size: 20px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-right: 15px;
  white-space: nowrap;
}

.nav-link {
  color: #333 !important;
  font-weight: 500;
  margin: 0 5px;
  padding: 8px 8px !important;
  font-size: 15px;
  transition: color 0.2s;
}

.nav-link:hover {
  color: var(--primary-color) !important;
}

/* 低空监控链接特殊样式 */
/* 余额显示 */
.balance-display {
  cursor: pointer;
  color: #10b981 !important;
  font-weight: 600;
  transition: all 0.3s;
  padding: 8px 10px !important;
  margin: 0 3px !important;
}

.balance-display:hover {
  color: #059669 !important;
}

/* 数字孪生链接 */
.digital-twin-link {
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  color: white !important;
  border-radius: 20px;
  padding: 6px 16px !important;
  font-weight: 500;
  transition: all 0.3s;
}

.digital-twin-link:hover {
  background: linear-gradient(135deg, #7c3aed, #4f46e5);
  color: white !important;
  transform: translateY(-1px);
}

/* 通知图标 */
.notification-icon {
  position: relative;
  cursor: pointer;
  color: #333 !important;
  transition: all 0.3s;
  padding: 8px 10px !important;
  margin: 0 3px !important;
}

.notification-icon:hover {
  color: var(--primary-color) !important;
}

.notification-icon .badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #ef4444;
  color: white;
  border-radius: 10px;
  padding: 2px 5px;
  font-size: 10px;
  font-weight: 600;
  min-width: 16px;
  text-align: center;
  line-height: 1;
}

/* 通知下拉菜单 */
.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  width: 380px;
  max-height: 500px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  display: none;
  overflow: hidden;
  margin-top: 10px;
}

.notification-dropdown.show {
  display: block;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  background: #f8f9fa;
}

.notification-header h6 {
  font-weight: 600;
  color: #333;
  margin: 0;
}

.notification-actions .mark-all-btn {
  font-size: 13px;
  color: var(--primary-color);
  cursor: pointer;
  text-decoration: none;
}

.notification-actions .mark-all-btn:hover {
  text-decoration: underline;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.no-notifications {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.no-notifications i {
  font-size: 48px;
  display: block;
  margin-bottom: 10px;
  opacity: 0.5;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.notification-item:hover {
  background: #f8f9fa;
}

.notification-item.unread {
  background: #f0f7ff;
}

.notification-item.unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--primary-color);
}

.notif-item-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  flex-shrink: 0;
}

.notif-item-icon.type-info {
  background: #dbeafe;
  color: #3b82f6;
}

.notif-item-icon.type-success {
  background: #d1fae5;
  color: #10b981;
}

.notif-item-icon.type-warning {
  background: #fef3c7;
  color: #f59e0b;
}

.notif-item-icon.type-danger {
  background: #fee2e2;
  color: #ef4444;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  font-size: 14px;
}

.notification-text {
  color: #666;
  font-size: 13px;
  line-height: 1.4;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-time {
  color: #999;
  font-size: 12px;
}

.notification-delete {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 5px;
  opacity: 0;
  transition: all 0.2s;
}

.notification-item:hover .notification-delete {
  opacity: 1;
}

.notification-delete:hover {
  color: #ef4444;
}

/* 用户头像 */
.user-avatar-small {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

/* Hero区域 */
.hero-section {
  position: relative;
  padding-top: 72px;
  min-height: 100vh;
  overflow: hidden;
}

.hero-section .carousel {
  height: calc(100vh - 72px);
}

.hero-section .carousel-item {
  height: calc(100vh - 72px);
}

.hero-section .carousel-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.3) 0%, rgba(6, 182, 212, 0.2) 100%);
  z-index: 1;
}

.carousel-caption {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: white;
  text-shadow: 2px 2px 10px rgba(0,0,0,0.3);
  z-index: 10;
}

.carousel-caption h1 {
  font-size: 56px;
  font-weight: 800;
  margin-bottom: 20px;
  color: white;
}

.carousel-caption p {
  font-size: 24px;
  color: rgba(255,255,255,0.9);
  margin-bottom: 40px;
}

/* 轮播图指示器 */
.hero-section :deep(.carousel-indicators) {
  bottom: 30px;
  z-index: 20;
}

.hero-section :deep(.carousel-indicators button) {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin: 0 6px;
  background-color: rgba(255, 255, 255, 0.5);
  border: none;
}

.hero-section :deep(.carousel-indicators button.active) {
  background-color: white;
}

/* 轮播图控制按钮 */
.hero-section :deep(.carousel-control-prev),
.hero-section :deep(.carousel-control-next) {
  z-index: 20;
  width: 60px;
}

.hero-section :deep(.carousel-control-prev-icon),
.hero-section :deep(.carousel-control-next-icon) {
  width: 30px;
  height: 30px;
}

/* 服务特色 */
/* 数字孪生入口 */
.digital-twin-promo-section {
  background: #f8fafc;
}

.digital-twin-card {
  background: transparent;
  border-radius: 0;
  border: none;
  box-shadow: none;
  padding: 20px 0;
}

.promo-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: #fff;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 15px;
}

.promo-title {
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.promo-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 20px;
}

.promo-features {
  list-style: none;
  padding: 0;
  margin-bottom: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.promo-features li {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 13px;
}

.promo-features li i {
  color: var(--primary-color);
}

.btn-promo {
  display: inline-flex;
  align-items: center;
  padding: 10px 24px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: #fff;
  border-radius: 10px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.3s;
  border: none;
  text-decoration: none;
}

.btn-promo:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.25);
  color: #fff;
}

.promo-visual {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  min-height: 350px;
}

.visual-circle {
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(37, 99, 235, 0.05) 100%);
  border: 2px solid rgba(59, 130, 246, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  animation: pulse 3s ease-in-out infinite;
}

.visual-inner {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 64px;
  animation: float 4s ease-in-out infinite;
}

.float-icon {
  position: absolute;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  font-size: 20px;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.2);
}

.float-icon.icon-1 {
  top: 20px;
  right: 40px;
  animation: float 3s ease-in-out infinite 0s;
}

.float-icon.icon-2 {
  bottom: 40px;
  left: 30px;
  animation: float 3s ease-in-out infinite 1s;
}

.float-icon.icon-3 {
  top: 50%;
  right: 10px;
  animation: float 3s ease-in-out infinite 2s;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.8;
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-15px);
  }
}

.features-section {
  background: #f8fafc;
}

.section-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 15px;
  color: #1e293b;
}

.section-subtitle {
  font-size: 18px;
  color: #64748b;
}

.feature-card {
  background: white;
  padding: 30px;
  border-radius: 16px;
  text-align: center;
  transition: all 0.3s;
  height: 100%;
}

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(59, 130, 246, 0.15);
}

.feature-icon {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  font-size: 32px;
  color: white;
}

.feature-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #1e293b;
}

.feature-desc {
  color: #64748b;
  font-size: 14px;
  margin: 0;
}

/* 无人机列表 */
.vehicles-section {
  background: white;
}

.drone-card {
  background: #f8fafc;
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s;
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

.drone-info {
  padding: 20px;
}

.drone-model {
  font-weight: 600;
  margin-bottom: 10px;
  color: #1e293b;
}

.drone-location {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 15px;
}

.drone-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}

.battery-level, .flight-hours {
  font-size: 13px;
  color: #64748b;
}

.drone-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #e2e8f0;
}

.price {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary-color);
}

/* 关于我们 */
.about-section {
  background: #f8fafc;
}

.about-text {
  font-size: 16px;
  line-height: 1.8;
  color: #64748b;
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  color: #64748b;
  margin: 0;
}

/* 页脚 */
.footer {
  background: #1e293b;
  color: white;
  padding: 60px 0 20px;
}

.footer-title {
  font-weight: 600;
  margin-bottom: 20px;
}

.footer-text {
  color: #94a3b8;
}

.footer-links {
  list-style: none;
  padding: 0;
}

.footer-links li {
  margin-bottom: 10px;
}

.footer-links a {
  color: #94a3b8;
  transition: color 0.2s;
}

.footer-links a:hover {
  color: white;
}

.footer-contact {
  color: #94a3b8;
  margin-bottom: 10px;
}

.footer-divider {
  border-color: #334155;
  margin: 30px 0;
}

.footer-copyright {
  text-align: center;
  color: #64748b;
  margin: 0;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 36px;
  }
  .hero-subtitle {
    font-size: 18px;
  }
  .section-title {
    font-size: 28px;
  }
}

/* 占位符样式 */
.about-image-placeholder {
  height: 300px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
}

.drone-image-placeholder {
  height: 180px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
}

/* 地图样式 */
.map-section {
  background: #f8f9fa;
}

.map-container {
  position: relative;
  display: flex;
  gap: 20px;
}

.map-info-card {
  position: absolute;
  top: 20px;
  left: 20px;
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 10;
  min-width: 200px;
}

.map-info-card h6 {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 15px;
}

.map-info-card p {
  color: #64748b;
  font-size: 13px;
  margin-bottom: 5px;
}

.map-legend {
  display: flex;
  gap: 15px;
  margin-top: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
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

.legend-dot.low-battery {
  background: #f59e0b;
}

.legend-dot.charging {
  background: #f59e0b;
}

/* 详情模态框样式 */
.detail-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.detail-modal-content {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25);
}

.detail-modal-header {
  position: relative;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.detail-modal-header .btn-close {
  position: absolute;
  top: 16px;
  right: 20px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #64748b;
}

.detail-modal-header .btn-close:hover {
  color: #1e293b;
}

.detail-modal-body {
  display: flex;
  flex-direction: column;
}

@media (min-width: 768px) {
  .detail-modal-body {
    flex-direction: row;
  }
}

.detail-modal-image {
  position: relative;
  width: 100%;
  height: 250px;
}

@media (min-width: 768px) {
  .detail-modal-image {
    width: 50%;
    height: 350px;
  }
}

.detail-modal-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-modal-image {
  background: #f8fafc;
}

.detail-modal-image img {
  display: block;
}

.detail-modal-image .status-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.detail-modal-info {
  padding: 24px;
  flex: 1;
}

.detail-modal-info h3 {
  font-weight: 700;
  margin-bottom: 8px;
  color: #1e293b;
}

.detail-modal-info .detail-no {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 20px;
}

.detail-modal-info .detail-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
}

.detail-modal-info .spec-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #64748b;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.detail-modal-info .detail-price {
  margin-bottom: 16px;
}

.detail-modal-info .price-amount {
  font-size: 32px;
  font-weight: 700;
  color: #3b82f6;
}

.detail-modal-info .price-unit {
  font-size: 16px;
  color: #64748b;
}

/* 模态框中的充电进度条 */
.charging-progress-modal {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.85), rgba(0,0,0,0.65));
  padding: 12px 16px;
  color: white;
}

.charging-progress-modal .charging-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.charging-progress-modal .charging-label {
  font-size: 13px;
  font-weight: 600;
  color: #fbbf24;
}

.charging-progress-modal .charging-time {
  font-size: 12px;
  color: #fcd34d;
}

.charging-progress-modal .charging-bar {
  width: 100%;
  height: 6px;
  background: rgba(255,255,255,0.3);
  border-radius: 3px;
  overflow: hidden;
}

.charging-progress-modal .charging-fill {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.detail-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
}

.detail-modal-footer .btn {
  padding: 10px 24px;
  border-radius: 10px;
  font-weight: 500;
}

.amap-container {
  width: 100%;
  height: 500px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
</style>
