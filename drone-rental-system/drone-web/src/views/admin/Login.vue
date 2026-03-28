<template>
  <div class="login-wrapper">
    <!-- 左侧轮播图 -->
    <div class="carousel-section">
      <div id="adminCarousel" class="carousel slide carousel-fade" data-bs-ride="carousel" data-bs-interval="4000">
        <div class="carousel-indicators">
          <button
            v-for="(slide, index) in slides"
            :key="index"
            type="button"
            :data-bs-target="'#adminCarousel'"
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

    <!-- 右侧登录表单 -->
    <div class="login-section">
      <div class="login-container">
        <div class="login-header">
          <div class="login-logo admin-logo">
            <i class="bi bi-shield-check"></i>
          </div>
          <h1 class="login-title">管理员登录</h1>
          <p class="login-subtitle">无人机智能租赁系统 - 管理后台</p>
        </div>

        <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
        <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

        <form @submit.prevent="handleLogin">
          <div class="mb-4">
            <label class="form-label">用户名</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-person"></i></span>
              <input
                v-model="loginForm.username"
                type="text"
                class="form-control"
                placeholder="请输入管理员用户名"
                required
              />
            </div>
          </div>

          <div class="mb-4">
            <label class="form-label">密码</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-lock"></i></span>
              <input
                v-model="loginForm.password"
                type="password"
                class="form-control"
                placeholder="请输入密码"
                required
              />
            </div>
          </div>

          <div class="mb-4">
            <label class="form-label">验证码</label>
            <div class="captcha-group">
              <div class="input-group">
                <span class="input-group-text"><i class="bi bi-shield-check"></i></span>
                <input
                  v-model="loginForm.captchaCode"
                  type="text"
                  class="form-control"
                  placeholder="请输入验证码"
                  required
                />
              </div>
              <div class="captcha-image-wrapper" @click="refreshCaptcha" title="点击刷新">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <div v-else class="captcha-loading">加载中...</div>
              </div>
            </div>
          </div>

          <button type="submit" class="btn-login" :disabled="loading">
            <i class="bi bi-box-arrow-in-right me-2"></i>{{ loading ? '登录中...' : '登录' }}
          </button>
        </form>

        <div class="text-center mt-4">
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
import { setLoginInfo } from '@/utils/auth'
import axios from 'axios'

const errorMessage = ref('')
const successMessage = ref('')
const loading = ref(false)

const captchaKey = ref('')
const captchaImage = ref('')

const slides = ref([
  {
    image: '/img/train/0001.jpg',
    title: '无人机智能租赁',
    subtitle: '专业管理平台'
  },
  {
    image: '/img/train/0003.jpg',
    title: '高效运营',
    subtitle: '实时监控无人机状态'
  },
  {
    image: '/img/train/0005.jpg',
    title: '数据分析',
    subtitle: '智能决策支持系统'
  },
  {
    image: '/img/train/0007.jpg',
    title: '安全保障',
    subtitle: '全方位安全防护'
  }
])

const loginForm = ref({
  username: '',
  password: '',
  captchaCode: ''
})

const handleLogin = async () => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    // 调用管理员登录API
    const res = await axios.post('/admin/api/login', {
      username: loginForm.value.username,
      password: loginForm.value.password,
      captchaKey: captchaKey.value,
      captchaCode: loginForm.value.captchaCode
    })

    if (res.data.code === 200) {
      setLoginInfo('admin-token', 'admin', { username: loginForm.value.username, adminName: '系统管理员' })
      successMessage.value = '登录成功，正在跳转...'
      setTimeout(() => {
        window.location.href = '/admin'
      }, 500)
    } else {
      errorMessage.value = res.data.message || '用户名或密码错误'
      refreshCaptcha()
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '登录失败，请重试'
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

// 获取验证码
const refreshCaptcha = async () => {
  try {
    const res = await axios.get('/api/captcha')
    if (res.data.code === 200) {
      captchaKey.value = res.data.data.key
      captchaImage.value = res.data.data.image
      loginForm.value.captchaCode = ''
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

onMounted(() => {
  // 初始化轮播图
  const carouselEl = document.getElementById('adminCarousel')
  if (carouselEl) {
    new Carousel(carouselEl, {
      interval: 4000,
      wrap: true,
      ride: 'carousel'
    })
  }
  // 加载验证码
  refreshCaptcha()
})
</script>

<style scoped>
:root {
  --primary-color: #1e293b;
  --accent-color: #0f172a;
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
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.4) 0%, rgba(15, 23, 42, 0.3) 100%);
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
  padding: 40px;
  box-shadow: -5px 0 30px rgba(0,0,0,0.1);
}

.login-container {
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-logo {
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

.login-logo.admin-logo {
  background: linear-gradient(135deg, #1e293b, #0f172a);
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
}

.login-subtitle {
  color: #64748b;
  font-size: 14px;
}

.form-label {
  color: #475569;
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 8px;
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
  box-shadow: 0 0 0 4px rgba(30, 41, 59, 0.1);
}

.input-group-text {
  background: #f8fafc;
  border: none;
  color: #64748b;
  padding: 14px 16px;
  font-size: 18px;
}

.form-control {
  flex: 1;
  border: none;
  padding: 14px 16px;
  font-size: 15px;
}

.form-control:focus {
  box-shadow: none;
}

/* 验证码样式 */
.captcha-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-group .input-group {
  flex: 1;
}

.captcha-image-wrapper {
  width: 120px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid #e2e8f0;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.captcha-image-wrapper:hover {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(30, 41, 59, 0.1);
}

.captcha-image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-loading {
  font-size: 12px;
  color: #94a3b8;
}

.btn-login {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border: none;
  border-radius: 12px;
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 10px;
}

.btn-login:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(30, 41, 59, 0.3);
}

.btn-login:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.alert {
  border-radius: 12px;
  border: none;
  padding: 14px 20px;
  margin-bottom: 24px;
}

.alert-danger {
  background: #fee2e2;
  color: #dc2626;
}

.alert-success {
  background: #d1fae5;
  color: #059669;
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
