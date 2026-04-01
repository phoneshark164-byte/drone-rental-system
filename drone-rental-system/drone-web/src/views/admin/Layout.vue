<template>
  <div class="admin-layout">
    <!-- 外部汉堡按钮（用于打开侧边栏） -->
    <button v-if="!sidebarOpen" class="hamburger-btn" @click="toggleSidebar">
      <i class="bi bi-list"></i>
    </button>

    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ 'show': sidebarOpen }">
      <div class="sidebar-header">
        <div class="sidebar-brand">
          <i class="bi bi-airplane-fill me-2"></i>无人机租赁
        </div>
        <button class="sidebar-close-btn" @click="closeSidebar">
          <i class="bi bi-x"></i>
        </button>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-section">管理功能</div>
        <router-link to="/admin" class="nav-link" exact-match>
          <i class="bi bi-speedometer2"></i>
          <span>仪表盘</span>
        </router-link>
        <router-link to="/admin/users" class="nav-link">
          <i class="bi bi-people"></i>
          <span>用户管理</span>
        </router-link>
        <router-link to="/admin/vehicles" class="nav-link">
          <i class="bi bi-airplane"></i>
          <span>无人机管理</span>
        </router-link>
        <router-link to="/admin/orders" class="nav-link">
          <i class="bi bi-receipt"></i>
          <span>订单管理</span>
        </router-link>
        <router-link to="/admin/reviews" class="nav-link">
          <i class="bi bi-star"></i>
          <span>评价管理</span>
        </router-link>
        <router-link to="/admin/banners" class="nav-link">
          <i class="bi bi-images"></i>
          <span>轮播图管理</span>
        </router-link>

        <div class="nav-section">财务管理</div>
        <router-link to="/admin/recharges" class="nav-link">
          <i class="bi bi-wallet2"></i>
          <span>充值记录</span>
        </router-link>
        <router-link to="/admin/payments" class="nav-link">
          <i class="bi bi-credit-card"></i>
          <span>支付记录</span>
        </router-link>
        <router-link to="/admin/detections" class="nav-link">
          <i class="bi bi-robot"></i>
          <span>AI检测记录</span>
        </router-link>

        <div class="nav-section">系统</div>
        <router-link to="/admin/notifications" class="nav-link">
          <i class="bi bi-bell"></i>
          <span>系统通知</span>
          <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
        </router-link>
        <router-link to="/admin/logs" class="nav-link">
          <i class="bi bi-journal-text"></i>
          <span>操作日志</span>
        </router-link>
        <router-link to="/admin/settings" class="nav-link">
          <i class="bi bi-gear"></i>
          <span>系统设置</span>
        </router-link>
      </nav>

      <div class="logout-btn" @click="handleLogout">
        <i class="bi bi-box-arrow-right"></i>
        <span>退出登录</span>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="main-content" :class="{ 'pushed': sidebarOpen }">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const userInfo = ref({})
const unreadCount = ref(0)
const sidebarOpen = ref(false)

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}

const closeSidebar = () => {
  sidebarOpen.value = false
}

const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('userRole')
    router.push('/admin/login')
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await request.get('/admin/api/notification/unread-count')
    if (res.code === 200) {
      unreadCount.value = res.data.count || 0
    }
  } catch (e) {
    console.error('获取未读通知失败', e)
  }
}

onMounted(() => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      userInfo.value = JSON.parse(userInfoStr)
    } catch (e) {
      console.error(e)
    }
  }
  // 获取未读通知数量
  fetchUnreadCount()
  // 每30秒刷新一次未读数量
  setInterval(fetchUnreadCount, 30000)
  // 监听通知更新事件
  window.addEventListener('admin-notification-updated', fetchUnreadCount)
})

onUnmounted(() => {
  window.removeEventListener('admin-notification-updated', fetchUnreadCount)
})

// 监听路由变化，刷新未读数量
watch(() => route.path, () => {
  fetchUnreadCount()
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

.admin-layout {
  display: flex;
  min-height: 100vh;
  width: 100vw;
  overflow-x: hidden;
  background-image:
    radial-gradient(circle at 20% 50%, rgba(59, 130, 246, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(6, 182, 212, 0.03) 0%, transparent 50%);
  background-attachment: fixed;
}

/* 汉堡菜单按钮 */
.hamburger-btn {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 999;
  width: 45px;
  height: 45px;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  color: #2563eb;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
  backdrop-filter: blur(10px);
}

.hamburger-btn:hover {
  background: white;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
}

/* 侧边栏内的关闭按钮 */
.sidebar-close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #64748b;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: all 0.2s;
  flex-shrink: 0;
}

.sidebar-close-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #1e293b;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 0 24px;
  margin-bottom: 30px;
}

/* 侧边栏 */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 280px;
  background: linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%);
  border-right: 1px solid #bfdbfe;
  padding: 20px 0;
  z-index: 1000;
  box-shadow: 2px 0 20px rgba(59, 130, 246, 0.15);
  display: flex;
  flex-direction: column;
  transform: translateX(-100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar.show {
  transform: translateX(0);
}

.sidebar-brand {
  padding: 0 24px;
  margin-bottom: 30px;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-section {
  padding: 0 16px;
  margin-bottom: 10px;
  margin-top: 10px;
  color: #64748b;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.nav-section:first-of-type {
  margin-top: 0;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: #475569;
  text-decoration: none;
  border-radius: 0 25px 25px 0;
  margin-right: 15px;
  transition: all 0.2s;
  font-weight: 500;
}

.nav-link:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}

.nav-link:deep(.router-link-active),
.nav-link.active {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
  box-shadow: 0 4px 15px rgba(37, 99, 235, 0.3);
}

.nav-link i {
  font-size: 18px;
  width: 20px;
}

.nav-link .badge {
  background: #ef4444;
  color: white;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: auto;
}

.logout-btn {
  margin: 20px 15px 0;
  padding: 12px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
  border-radius: 12px;
  text-align: center;
  transition: all 0.2s;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 500;
}

.logout-btn:hover {
  background: #fecaca;
}

/* 主内容区 */
.main-content {
  flex: 1;
  padding: 90px 30px 30px 80px;
  min-height: 100vh;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 50%, #bfdbfe 100%);
  box-sizing: border-box;
  width: 100%;
  position: relative;
  z-index: 1;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.main-content.pushed {
  margin-left: 280px;
  padding-left: 30px;
  width: calc(100% - 280px);
}

/* 管理员页面独特的背景装饰 */
.main-content::before {
  content: '';
  position: fixed;
  top: 0;
  right: 0;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.main-content::after {
  content: '';
  position: fixed;
  bottom: 0;
  left: 0;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.08) 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.03) 1px, transparent 1px);
  background-size: 20px 20px;
}

/* 确保内容在装饰层之上 */
.main-content > * {
  position: relative;
  z-index: 1;
}
</style>
