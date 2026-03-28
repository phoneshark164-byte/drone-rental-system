<template>
  <div class="login-wrapper">
    <!-- 左侧轮播图 -->
    <div class="carousel-section">
      <div id="registerCarousel" class="carousel slide carousel-fade" data-bs-ride="carousel" data-bs-interval="4000">
        <div class="carousel-indicators">
          <button
            v-for="(slide, index) in slides"
            :key="index"
            type="button"
            :data-bs-target="'#registerCarousel'"
            :data-bs-slide-to="index"
            :class="{ active: index === 0 }"
          ></button>
        </div>
        <div class="carousel-inner">
          <div
            v-for="(slide, index) in slides"
            :key="index"
            class="carousel-item"
            :class="{ active: index === 0 }"
          >
            <img :src="slide.image" :alt="slide.title" />
            <div class="carousel-overlay"></div>
            <div class="carousel-caption-custom">
              <h1>{{ slide.title }}</h1>
              <p>{{ slide.subtitle }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧注册表单 -->
    <div class="login-section">
      <div class="login-container">
        <div class="login-header">
          <div class="login-logo">
            <i class="bi bi-airplane"></i>
          </div>
          <h1 class="login-title">用户注册</h1>
          <p class="login-subtitle">无人机智能租赁系统</p>
        </div>

        <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
        <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

        <form @submit.prevent="handleRegister">
          <div class="mb-3">
            <label class="form-label">用户名</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-person"></i></span>
              <input
                v-model="registerForm.username"
                type="text"
                class="form-control"
                placeholder="请输入用户名"
                required
              />
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label">真实姓名</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-person-badge"></i></span>
              <input
                v-model="registerForm.realName"
                type="text"
                class="form-control"
                placeholder="请输入真实姓名"
                required
              />
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label">手机号</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-phone"></i></span>
              <input
                v-model="registerForm.phone"
                type="tel"
                class="form-control"
                placeholder="请输入手机号"
                required
              />
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label">身份证号</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-card-heading"></i></span>
              <input
                v-model="registerForm.idCard"
                type="text"
                class="form-control"
                placeholder="请输入身份证号"
              />
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label">密码</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-lock"></i></span>
              <input
                v-model="registerForm.password"
                type="password"
                class="form-control"
                placeholder="请输入密码（6-20位）"
                required
              />
            </div>
          </div>

          <div class="mb-4">
            <label class="form-label">确认密码</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-lock-fill"></i></span>
              <input
                v-model="registerForm.confirmPassword"
                type="password"
                class="form-control"
                placeholder="请再次输入密码"
                required
              />
            </div>
          </div>

          <button type="submit" class="btn-login" :disabled="loading">
            <i class="bi bi-person-plus me-2"></i>{{ loading ? '注册中...' : '立即注册' }}
          </button>
        </form>

        <div class="divider">
          <span>或</span>
        </div>

        <div class="text-center">
          <span style="color: #64748b; font-size: 14px;">已有账号？</span>
          <router-link to="/user/login" class="register-link">
            <i class="bi bi-box-arrow-in-right"></i>
            立即登录
          </router-link>
        </div>

        <div class="text-center mt-3">
          <router-link to="/" style="color: #94a3b8; text-decoration: none; font-size: 14px;">
            <i class="bi bi-house"></i>
            返回首页
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Carousel } from 'bootstrap'
import { register } from '@/api/user'

const errorMessage = ref('')
const successMessage = ref('')
const loading = ref(false)

const slides = ref([
  {
    image: '/img/train/0001.jpg',
    title: '无人机智能租赁',
    subtitle: '探索世界，随心而飞'
  },
  {
    image: '/img/train/0003.jpg',
    title: '大疆 Air 3',
    subtitle: '轻巧便携，画质出众'
  },
  {
    image: '/img/train/0005.jpg',
    title: '道通 EVO II',
    subtitle: '专业级航拍体验'
  },
  {
    image: '/img/train/0007.jpg',
    title: '大疆 Avata',
    subtitle: '沉浸式飞行体验'
  }
])

const registerForm = ref({
  username: '',
  realName: '',
  phone: '',
  idCard: '',
  password: '',
  confirmPassword: ''
})

const handleRegister = async () => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    loading.value = false
    return
  }

  if (registerForm.value.password.length < 6) {
    errorMessage.value = '密码长度不能少于6位'
    loading.value = false
    return
  }

  try {
    // 调用注册API
    const res = await register({
      username: registerForm.value.username,
      password: registerForm.value.password,
      confirmPassword: registerForm.value.confirmPassword,
      phone: registerForm.value.phone,
      realName: registerForm.value.realName
    })

    if (res.code === 200) {
      successMessage.value = '注册成功！正在跳转登录页面...'
      setTimeout(() => {
        window.location.href = '/user/login'
      }, 1500)
    } else {
      errorMessage.value = res.message || '注册失败'
    }
  } catch (error) {
    console.error('注册失败:', error)
    errorMessage.value = error.response?.data?.message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 初始化轮播图
  const carouselEl = document.getElementById('registerCarousel')
  if (carouselEl) {
    new Carousel(carouselEl, {
      interval: 4000,
      wrap: true,
      ride: 'carousel'
    })
  }
})
</script>

<style scoped>
:root {
  --primary-color: #3b82f6;
  --accent-color: #06b6d4;
}

.login-wrapper {
  display: flex;
  width: 100%;
  min-height: 100vh;
}

/* 左侧轮播图区域 */
.carousel-section {
  flex: 1;
  position: sticky;
  top: 0;
  min-height: 100vh;
  overflow: hidden;
  margin: 0;
  padding: 0;
  flex-shrink: 0;
}

.carousel-section .carousel {
  height: 100vh;
  margin: 0;
  padding: 0;
}

.carousel-section .carousel-inner {
  height: 100%;
}

.carousel-section .carousel-item {
  height: 100vh;
  position: relative;
}

.carousel-section .carousel-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
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

.carousel-caption-custom {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: white;
  text-shadow: 2px 2px 10px rgba(0,0,0,0.3);
  z-index: 10;
}

.carousel-caption-custom h1 {
  font-size: 48px;
  font-weight: 800;
  margin-bottom: 15px;
  color: white;
}

.carousel-caption-custom p {
  font-size: 20px;
  color: rgba(255,255,255,0.9);
}

/* 轮播图指示器 */
.carousel-section :deep(.carousel-indicators) {
  bottom: 30px;
  z-index: 20;
}

.carousel-section :deep(.carousel-indicators button) {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin: 0 6px;
  background-color: rgba(255, 255, 255, 0.5);
  border: none;
}

.carousel-section :deep(.carousel-indicators button.active) {
  background-color: white;
}

/* 右侧表单区域 */
.login-section {
  flex: 0 0 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  padding: 30px;
  box-shadow: -5px 0 30px rgba(0,0,0,0.1);
}

.login-container {
  width: 100%;
  max-width: 380px;
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-logo {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  font-size: 26px;
  color: white;
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 6px;
}

.login-subtitle {
  color: #64748b;
  font-size: 13px;
}

.form-label {
  color: #475569;
  font-weight: 500;
  font-size: 13px;
  margin-bottom: 6px;
}

.input-group {
  display: flex;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.2s;
}

.input-group:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.input-group-text {
  background: #f8fafc;
  border: none;
  color: #64748b;
  padding: 10px 12px;
  font-size: 16px;
}

.form-control {
  flex: 1;
  border: none;
  padding: 10px 12px;
  font-size: 14px;
}

.form-control:focus {
  box-shadow: none;
}

.btn-login {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border: none;
  border-radius: 12px;
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 8px;
}

.btn-login:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.3);
}

.btn-login:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.alert {
  border-radius: 12px;
  border: none;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 13px;
}

/* 减少表单字段间距 */
form .mb-3 {
  margin-bottom: 12px !important;
}

form .mb-4 {
  margin-bottom: 16px !important;
}

.alert-danger {
  background: #fee2e2;
  color: #dc2626;
}

.alert-success {
  background: #d1fae5;
  color: #059669;
}

.divider {
  display: flex;
  align-items: center;
  margin: 16px 0;
  color: #94a3b8;
}

.divider::before, .divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.divider span {
  padding: 0 15px;
  font-size: 13px;
}

.register-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--primary-color);
  font-weight: 500;
  transition: color 0.2s;
}

.register-link:hover {
  color: var(--accent-color);
}

/* 响应式 */
@media (max-width: 992px) {
  .carousel-section {
    display: none;
  }
  .login-section {
    flex: 1;
  }
}

@media (max-width: 576px) {
  .login-section {
    padding: 20px;
  }
}
</style>
