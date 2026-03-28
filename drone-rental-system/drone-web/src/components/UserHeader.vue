<template>
  <nav class="user-header navbar navbar-expand-lg" :class="{ 'scrolled': isScrolled }">
    <div class="container">
      <router-link class="navbar-brand" to="/">
        <i class="bi bi-airplane-fill me-2"></i>无人机租赁
      </router-link>
      <button class="navbar-toggler" type="button" @click="toggleNavbar">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" :class="{ 'show': navbarOpen }">
        <ul class="navbar-nav ms-auto">
          <li class="nav-item">
            <router-link class="nav-link" to="/">首页</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/user/vehicles">无人机列表</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/user/recommendation">智能推荐</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/user/orders">我的订单</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/user/repairs">我的报修</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/user/detection">
              <i class="bi bi-robot me-1"></i>AI 检测
            </router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/user/digital-twin">
              <i class="bi bi-globe-americas me-1"></i>智慧农场
            </router-link>
          </li>
          <li class="nav-item" v-if="userInfo">
            <a class="nav-link balance-display" @click="showRechargeModal">
              <i class="bi bi-wallet2 me-1"></i>余额: ¥{{ userBalance }}
            </a>
          </li>
          <li class="nav-item position-static" v-if="userInfo">
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
        <div class="d-flex ms-2 align-items-center gap-2" v-if="userInfo">
          <router-link to="/user/profile" class="btn btn-primary rounded-pill px-3 d-flex align-items-center"
            style="background: linear-gradient(135deg, var(--primary-color), var(--accent-color)); border: none; font-size: 14px;">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" class="user-avatar-inline me-1" alt="头像" />
            <i v-else class="bi bi-person-circle me-1"></i>{{ userInfo.username || '用户' }}
          </router-link>
          <button @click="handleLogout" class="btn btn-outline-light rounded-pill px-2" style="font-size: 14px;">
            <i class="bi bi-box-arrow-right"></i> 退出
          </button>
        </div>
        <div class="d-flex ms-2 gap-2" v-else>
          <router-link class="btn btn-outline-primary rounded-pill px-3" to="/user/login">登录</router-link>
          <router-link class="btn btn-primary rounded-pill px-3" to="/user/register"
            style="background: linear-gradient(135deg, var(--primary-color), var(--accent-color)); border: none;">注册</router-link>
        </div>
      </div>
    </div>

    <!-- 充值模态框 -->
    <div class="modal fade" ref="rechargeModal" tabindex="-1">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">账户充值</h5>
            <button type="button" class="btn-close" @click="hideRechargeModal"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label">当前余额</label>
              <div class="current-balance">¥{{ userBalance }}</div>
            </div>
            <div class="mb-3">
              <label class="form-label">充值金额</label>
              <div class="amount-options mb-2">
                <button class="amount-btn" @click="selectAmount(50)" :class="{ active: rechargeAmount === 50 }">¥50</button>
                <button class="amount-btn" @click="selectAmount(100)" :class="{ active: rechargeAmount === 100 }">¥100</button>
                <button class="amount-btn" @click="selectAmount(200)" :class="{ active: rechargeAmount === 200 }">¥200</button>
                <button class="amount-btn" @click="selectAmount(500)" :class="{ active: rechargeAmount === 500 }">¥500</button>
              </div>
              <input type="number" class="form-control" v-model.number="rechargeAmount" min="1" step="1" placeholder="或输入自定义金额">
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="hideRechargeModal">取消</button>
            <button type="button" class="btn btn-primary" @click="handleRecharge" :disabled="rechargeLoading">
              <span v-if="rechargeLoading"><span class="spinner-border spinner-border-sm me-1"></span>充值中...</span>
              <span v-else>确认充值</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 支付进度模态框 -->
    <div class="modal fade" ref="paymentModal" tabindex="-1" data-bs-backdrop="static">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content payment-modal">
          <div class="modal-body text-center">
            <div class="payment-progress" :class="{ 'success': paymentStatus === 'success', 'error': paymentStatus === 'error' }">
              <div class="progress-circle">
                <svg viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r="45" fill="none" stroke="#e0e0e0" stroke-width="8"/>
                  <circle cx="50" cy="50" r="45" fill="none" :stroke="paymentStatus === 'processing' ? '#3b82f6' : paymentStatus === 'success' ? '#10b981' : '#ef4444'" stroke-width="8"
                    :stroke-dasharray="paymentProgress" stroke-dashoffset="0" transform="rotate(-90 50 50)" class="progress-ring"/>
                </svg>
                <div class="progress-icon">
                  <i class="bi bi-hourglass-split" v-if="paymentStatus === 'processing'"></i>
                  <i class="bi bi-check-lg" v-if="paymentStatus === 'success'"></i>
                  <i class="bi bi-x-lg" v-if="paymentStatus === 'error'"></i>
                </div>
              </div>
              <h4 class="mt-3">{{ paymentTitle }}</h4>
              <p class="text-muted">{{ paymentMessage }}</p>
              <div v-if="paymentAmount > 0" class="payment-amount">
                支付金额: <span class="amount">¥{{ paymentAmount }}</span>
              </div>
            </div>
          </div>
          <div class="modal-footer justify-content-center" v-if="paymentStatus !== 'processing'">
            <button type="button" class="btn btn-primary" @click="hidePaymentModal">确定</button>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const isScrolled = ref(false)
const navbarOpen = ref(false)
const userInfo = ref(null)
const userBalance = ref('0.00')

// 通知相关
const showNotifications = ref(false)
const unreadCount = ref(0)
const userNotifications = ref([])

// 充值相关
const rechargeModal = ref(null)
const rechargeAmount = ref(100)
const rechargeLoading = ref(false)

// 支付进度相关
const paymentModal = ref(null)
const paymentStatus = ref('processing') // processing, success, error
const paymentProgress = ref('0 283')
const paymentTitle = ref('支付处理中...')
const paymentMessage = ref('正在处理您的支付请求，请稍候')
const paymentAmount = ref(0)

const toggleNavbar = () => {
  navbarOpen.value = !navbarOpen.value
}

const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
}

// 获取用户通知
const fetchUserNotifications = async () => {
  if (!userInfo.value) return
  try {
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
      const res = await request.post(`/user/api/notification/${notification.id}/read`)
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
    const res = await request.delete(`/user/api/notification/${id}`)
    if (res.code === 200) {
      userNotifications.value = userNotifications.value.filter(n => n.id !== id)
      unreadCount.value = userNotifications.value.filter(n => n.isRead === 0).length
    }
  } catch (e) {
    console.error('删除通知失败', e)
  }
}

// 点击外部关闭下拉菜单
const handleClickOutside = (event) => {
  if (showNotifications.value) {
    const notificationDropdown = event.target.closest('.notification-dropdown')
    const notificationIcon = event.target.closest('.notification-icon')
    if (!notificationDropdown && !notificationIcon) {
      showNotifications.value = false
    }
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('userRole')
  userInfo.value = null
  router.push('/user/login')
}

// 获取用户余额
const fetchBalance = async () => {
  try {
    const res = await request.get('/user/api/balance')
    if (res.code === 200) {
      userBalance.value = (res.data.balance || 0).toFixed(2)
    }
  } catch (e) {
    console.error('获取余额失败', e)
  }
}

// 选择充值金额
const selectAmount = (amount) => {
  rechargeAmount.value = amount
}

// 显示充值模态框
const showRechargeModal = () => {
  const modal = new window.bootstrap.Modal(rechargeModal.value)
  modal.show()
}

// 隐藏充值模态框
const hideRechargeModal = () => {
  const modal = window.bootstrap.Modal.getInstance(rechargeModal.value)
  if (modal) modal.hide()
}

// 处理充值
const handleRecharge = async () => {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) {
    alert('请输入有效的充值金额')
    return
  }

  rechargeLoading.value = true
  try {
    // 创建充值订单
    const createRes = await request.post('/user/api/recharge/create', {
      amount: rechargeAmount.value,
      paymentMethod: 'BALANCE'
    })

    if (createRes.code === 200) {
      // 模拟支付进度
      showPaymentProgress(rechargeAmount.value, 'recharge', createRes.data.rechargeNo)
    } else {
      alert(createRes.message || '创建充值订单失败')
      rechargeLoading.value = false
    }
  } catch (e) {
    console.error('充值失败', e)
    alert('充值失败: ' + (e.message || '未知错误'))
    rechargeLoading.value = false
  }
}

// 显示支付进度
const showPaymentProgress = (amount, type, orderNo) => {
  hideRechargeModal()
  paymentAmount.value = amount
  paymentStatus.value = 'processing'
  paymentProgress.value = '0 283'
  paymentTitle.value = type === 'recharge' ? '充值处理中...' : '支付处理中...'
  paymentMessage.value = '正在处理您的支付请求，请稍候'

  const modal = new window.bootstrap.Modal(paymentModal.value)
  modal.show()

  // 模拟支付进度动画
  let progress = 0
  const interval = setInterval(() => {
    progress += 5
    const circumference = 2 * Math.PI * 45
    paymentProgress.value = `${(progress / 100) * circumference} ${circumference}`

    if (progress >= 100) {
      clearInterval(interval)
      // 执行支付/充值
      processPayment(type, orderNo)
    }
  }, 100)
}

// 处理支付
const processPayment = async (type, orderNo) => {
  try {
    if (type === 'recharge') {
      await request.post('/user/api/recharge/pay', { rechargeNo: orderNo })
    } else if (type === 'order') {
      await request.post(`/user/api/order/${orderNo}/pay/balance`)
    }

    paymentStatus.value = 'success'
    paymentTitle.value = type === 'recharge' ? '充值成功' : '支付成功'
    paymentMessage.value = type === 'recharge' ? '充值已到账，请刷新查看' : '订单支付成功'
    paymentProgress.value = '283 283'

    // 刷新余额
    await fetchBalance()

    // 刷新页面以更新订单状态
    if (type === 'order') {
      setTimeout(() => {
        window.location.reload()
      }, 1500)
    }
  } catch (e) {
    paymentStatus.value = 'error'
    paymentTitle.value = type === 'recharge' ? '充值失败' : '支付失败'
    paymentMessage.value = e.message || '操作失败，请重试'
  }

  rechargeLoading.value = false
}

// 隐藏支付模态框
const hidePaymentModal = () => {
  const modal = window.bootstrap.Modal.getInstance(paymentModal.value)
  if (modal) modal.hide()
}

// 暴露方法给外部调用
window.showPaymentProgress = showPaymentProgress

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  document.addEventListener('click', handleClickOutside)
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      userInfo.value = JSON.parse(userInfoStr)
      fetchBalance()
      fetchUserNotifications()
      // 每30秒刷新一次通知
      setInterval(fetchUserNotifications, 30000)
    } catch (e) {
      userInfo.value = null
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
:root {
  --primary-color: #3b82f6;
  --accent-color: #06b6d4;
}

.user-header {
  background: transparent;
  padding: 15px 0;
  transition: all 0.3s;
  border-bottom: none;
}

.user-header.scrolled {
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
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

/* 低空监控连接特殊样式 */
/* 确保导航栏内容在一行显示 */
@media (min-width: 992px) {
  .navbar-nav {
    flex-wrap: nowrap;
  }
  .nav-item {
    white-space: nowrap;
  }
}

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

/* 用户头像 inline */
.user-avatar-inline {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.3);
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

/* 充值模态框 */
.amount-options {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.amount-btn {
  padding: 8px 20px;
  border: 2px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
}

.amount-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.amount-btn.active {
  border-color: var(--primary-color);
  background: var(--primary-color);
  color: white;
}

.current-balance {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary-color);
}

/* 支付进度模态框 */
.payment-modal {
  border: none;
  border-radius: 16px;
  overflow: hidden;
}

.progress-circle {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto;
}

.progress-ring {
  transition: stroke-dasharray 0.5s ease;
  transform-origin: center;
}

.progress-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  color: #3b82f6;
}

.payment-progress.success .progress-icon {
  color: #10b981;
}

.payment-progress.error .progress-icon {
  color: #ef4444;
}

.payment-amount {
  margin-top: 20px;
  padding: 15px;
  background: #f8fafc;
  border-radius: 8px;
}

.payment-amount .amount {
  font-size: 24px;
  font-weight: 700;
  color: var(--primary-color);
}
</style>
