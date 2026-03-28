<template>
  <div class="operator-layout">
    <div class="sidebar">
      <div class="sidebar-brand">
        <i class="bi bi-airplane me-2"></i>无人机租赁
      </div>
      <nav>
        <router-link to="/operator" class="nav-link active">
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
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const userInfo = ref({})

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
}

/* 侧边栏 - 运营方浅色主题 */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 260px;
  background: linear-gradient(180deg, #f0fdf4 0%, #dcfce7 100%);
  border-right: 1px solid #bbf7d0;
  padding: 20px 0;
  z-index: 1000;
  box-shadow: 2px 0 10px rgba(34, 197, 94, 0.1);
  display: flex;
  flex-direction: column;
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

.nav-link.active {
  background: linear-gradient(135deg, #16a34a, #22c55e);
  color: white;
  box-shadow: 0 4px 15px rgba(22, 163, 74, 0.3);
}

.nav-link.router-link-active {
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

.main-content {
  flex: 1;
  margin-left: 260px;
  padding: 30px;
  min-height: 100vh;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 50%, #bbf7d0 100%);
  position: relative;
  box-sizing: border-box;
  width: calc(100% - 260px);
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
  left: 260px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle at 30% 70%, rgba(16, 185, 129, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 70% 30%, rgba(5, 150, 105, 0.05) 0%, transparent 30%);
  pointer-events: none;
  z-index: 0;
}

/* 添加波纹图案 */
.operator-layout {
  background-image:
    radial-gradient(circle at 20% 50%, rgba(5, 150, 105, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(16, 185, 129, 0.03) 0%, transparent 50%);
  background-attachment: fixed;
}

/* 确保内容在装饰层之上 */
.main-content > * {
  position: relative;
  z-index: 1;
}

@media (max-width: 992px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.3s;
  }
  .sidebar.show {
    transform: translateX(0);
  }
  .main-content {
    margin-left: 0;
  }
}
</style>
