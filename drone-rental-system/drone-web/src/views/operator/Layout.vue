<template>
  <div class="operator-layout">
    <!-- 外部汉堡按钮（用于打开侧边栏） -->
    <button v-if="!sidebarOpen" class="hamburger-btn" @click="toggleSidebar">
      <i class="bi bi-list"></i>
    </button>

    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ 'show': sidebarOpen }">
      <div class="sidebar-header">
        <div class="sidebar-brand">
          <i class="bi bi-airplane me-2"></i>无人机租赁
        </div>
        <button class="sidebar-close-btn" @click="closeSidebar">
          <i class="bi bi-x"></i>
        </button>
      </div>
      <nav>
        <router-link to="/operator" class="nav-link" exact-match>
          <i class="bi bi-speedometer2"></i>
          <span>控制台</span>
        </router-link>
        <router-link to="/operator/vehicles" class="nav-link">
          <i class="bi bi-airplane"></i>
          <span>无人机管理</span>
        </router-link>
        <router-link to="/operator/orders" class="nav-link">
          <i class="bi bi-receipt"></i>
          <span>订单管理</span>
        </router-link>
        <router-link to="/operator/repairs" class="nav-link">
          <i class="bi bi-tools"></i>
          <span>报修管理</span>
        </router-link>
      </nav>
      <form class="logout-btn" @submit.prevent="handleLogout">
        <button type="submit" style="background: none; border: none; color: #dc2626; cursor: pointer; display: flex; align-items: center; gap: 8px; font-size: 16px; width: 100%; padding: 0; justify-content: center;">
          <i class="bi bi-box-arrow-right"></i>退出登录
        </button>
      </form>
    </div>

    <!-- 主内容区 -->
    <div class="main-content" :class="{ 'pushed': sidebarOpen }">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const userInfo = ref({})
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
    router.push('/operator/login')
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
})
</script>

<style scoped>
:root {
  --primary-color: #059669;
  --accent-color: #10b981;
}

.operator-layout {
  display: flex;
  min-height: 100vh;
  width: 100vw;
  overflow-x: hidden;
  background-image:
    radial-gradient(circle at 20% 50%, rgba(5, 150, 105, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(16, 185, 129, 0.03) 0%, transparent 50%);
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
  color: #16a34a;
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
  background: linear-gradient(180deg, #f0fdf4 0%, #dcfce7 100%);
  border-right: 1px solid #bbf7d0;
  padding: 20px 0;
  z-index: 1000;
  box-shadow: 2px 0 20px rgba(34, 197, 94, 0.15);
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
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #16a34a, #22c55e);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

nav {
  display: block;
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
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

.nav-link.active,
.nav-link:deep(.router-link-active) {
  background: linear-gradient(135deg, #16a34a, #22c55e);
  color: white;
  box-shadow: 0 4px 15px rgba(22, 163, 74, 0.3);
}

.nav-link i {
  font-size: 18px;
  width: 20px;
}

.logout-btn {
  margin: 0 15px;
  padding: 12px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  border-radius: 12px;
  text-align: center;
  transition: all 0.2s;
  margin-top: auto;
}

.logout-btn:hover {
  background: #fecaca;
}

/* 主内容区 */
.main-content {
  flex: 1;
  padding: 90px 30px 30px 30px;
  min-height: 100vh;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 50%, #bbf7d0 100%);
  position: relative;
  box-sizing: border-box;
  width: 100%;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.main-content.pushed {
  margin-left: 280px;
  padding-left: 30px;
  width: calc(100% - 280px);
}

/* 运营端独特的背景装饰 */
.main-content::before {
  content: '';
  position: fixed;
  top: 0;
  right: 0;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(5, 150, 105, 0.1) 0%, transparent 70%);
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
  background: radial-gradient(circle at 30% 70%, rgba(16, 185, 129, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 70% 30%, rgba(5, 150, 105, 0.05) 0%, transparent 30%);
  pointer-events: none;
  z-index: 0;
}

/* 确保内容在装饰层之上 */
.main-content > * {
  position: relative;
  z-index: 1;
}
</style>
