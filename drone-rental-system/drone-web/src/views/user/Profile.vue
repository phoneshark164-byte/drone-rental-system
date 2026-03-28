<template>
  <div class="user-profile-page">
    <UserHeader />

    <div class="container py-5">
      <div class="page-header d-flex justify-content-between align-items-center mb-4">
        <h2 class="page-title mb-0">个人中心</h2>
        <router-link to="/" class="btn btn-outline-primary">
          <i class="bi bi-house me-1"></i>返回首页
        </router-link>
      </div>

      <div class="row">
        <div class="col-lg-4">
          <div class="profile-card">
            <div class="profile-avatar" :class="{ uploading: uploadingAvatar }">
              <img :src="userInfo.avatar || '/images/default-avatar.png'" alt="头像" />
              <div v-if="uploadingAvatar" class="avatar-loading">
                <div class="spinner-border spinner-border-sm"></div>
              </div>
              <button class="avatar-edit" @click="triggerAvatarUpload" :disabled="uploadingAvatar">
                <i class="bi bi-camera"></i>
              </button>
              <input
                type="file"
                ref="avatarInput"
                accept="image/*"
                @change="handleAvatarUpload"
                style="display: none"
              />
            </div>
            <h4>{{ userInfo.realName || userInfo.username }}</h4>
            <p class="text-muted">{{ userInfo.phone }}</p>
            <div class="profile-stats">
              <div class="stat-item">
                <span class="stat-value">{{ stats.totalOrders }}</span>
                <span class="stat-label">总订单</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ stats.totalHours }}</span>
                <span class="stat-label">飞行小时</span>
              </div>
            </div>
          </div>
        </div>

        <div class="col-lg-8">
          <div class="info-card">
            <div class="card-header">
              <h5>基本信息</h5>
              <button class="btn btn-sm btn-outline-primary" @click="editMode = !editMode">
                <i class="bi bi-pencil me-1"></i>{{ editMode ? '取消' : '编辑' }}
              </button>
            </div>
            <form @submit.prevent="handleSave">
              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label">用户名</label>
                  <input type="text" class="form-control" :value="userInfo.username" disabled />
                </div>
                <div class="col-md-6">
                  <label class="form-label">真实姓名</label>
                  <input
                    v-model="profileForm.realName"
                    type="text"
                    class="form-control"
                    :disabled="!editMode"
                  />
                </div>
                <div class="col-md-6">
                  <label class="form-label">手机号</label>
                  <input
                    v-model="profileForm.phone"
                    type="tel"
                    class="form-control"
                    :disabled="!editMode"
                  />
                </div>
                <div class="col-md-6">
                  <label class="form-label">身份证号</label>
                  <input
                    v-model="profileForm.idCard"
                    type="text"
                    class="form-control"
                    :disabled="!editMode"
                  />
                </div>
              </div>
              <div v-if="editMode" class="mt-4">
                <button type="submit" class="btn btn-primary">
                  <i class="bi bi-check me-1"></i>保存修改
                </button>
              </div>
            </form>
          </div>

          <div class="info-card">
            <div class="card-header">
              <h5>账户安全</h5>
            </div>
            <div class="security-options">
              <div class="security-item">
                <div class="security-info">
                  <i class="bi bi-lock"></i>
                  <div>
                    <h6>登录密码</h6>
                    <p class="text-muted">定期更换密码，保护账户安全</p>
                  </div>
                </div>
                <button class="btn btn-outline-primary btn-sm" @click="showPasswordModal = true">
                  修改密码
                </button>
              </div>
              <div class="security-item">
                <div class="security-info">
                  <i class="bi bi-credit-card"></i>
                  <div>
                    <h6>押金</h6>
                    <p class="text-muted">当前押金：¥{{ userInfo.deposit || 0 }}</p>
                  </div>
                </div>
                <button class="btn btn-primary btn-sm" @click="showDepositModal = true" v-if="!userInfo.deposit || userInfo.deposit === 0">
                  缴纳押金
                </button>
                <span class="badge bg-success" v-else>已缴纳</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改密码模态框 -->
    <div v-if="showPasswordModal" class="modal-overlay" @click.self="showPasswordModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h5>修改密码</h5>
          <button class="btn-close" @click="showPasswordModal = false">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <form @submit.prevent="handlePasswordChange">
          <div class="mb-3">
            <label class="form-label">原密码</label>
            <input v-model="passwordForm.oldPassword" type="password" class="form-control" required />
          </div>
          <div class="mb-3">
            <label class="form-label">新密码</label>
            <input v-model="passwordForm.newPassword" type="password" class="form-control" required />
          </div>
          <div class="mb-4">
            <label class="form-label">确认新密码</label>
            <input v-model="passwordForm.confirmPassword" type="password" class="form-control" required />
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" @click="showPasswordModal = false">
              取消
            </button>
            <button type="submit" class="btn btn-primary">确认修改</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 缴纳押金模态框 -->
    <div v-if="showDepositModal" class="modal-overlay" @click.self="showDepositModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h5>缴纳押金</h5>
          <button class="btn-close" @click="showDepositModal = false">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="deposit-body">
          <div class="deposit-info mb-3">
            <i class="bi bi-info-circle me-2"></i>
            <span>缴纳押金后即可租用无人机，订单完成后押金将自动退还到账户余额</span>
          </div>
          <div class="mb-3">
            <label class="form-label">押金金额</label>
            <div class="deposit-amount-display">¥{{ depositAmount }}</div>
          </div>
          <div class="mb-3">
            <label class="form-label">支付方式</label>
            <div class="payment-methods">
              <div class="payment-method active">
                <i class="bi bi-wallet2"></i>
                <span>余额支付</span>
              </div>
            </div>
          </div>
          <div class="balance-hint">
            当前余额：¥{{ userBalance }}
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline-secondary" @click="showDepositModal = false">
            取消
          </button>
          <button type="button" class="btn btn-primary" @click="handleDeposit" :disabled="depositLoading">
            <span v-if="depositLoading"><span class="spinner-border spinner-border-sm me-1"></span>处理中...</span>
            <span v-else>确认缴纳</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import UserHeader from '@/components/UserHeader.vue'

const userInfo = ref({})
const editMode = ref(false)
const showPasswordModal = ref(false)
const showDepositModal = ref(false)
const userBalance = ref('0.00')
const avatarInput = ref(null)

// 押金相关配置
const depositAmount = ref(100)  // 默认押金金额
const depositLoading = ref(false)

// 头像上传相关
const uploadingAvatar = ref(false)

const profileForm = reactive({
  realName: '',
  phone: '',
  idCard: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const stats = ref({
  totalOrders: 0,
  totalHours: 0
})

const handleSave = async () => {
  // TODO: 调用更新API
  alert('保存成功')
  editMode.value = false
}

const handlePasswordChange = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }

  if (passwordForm.newPassword.length < 6) {
    alert('新密码长度不能少于6位')
    return
  }

  try {
    const { default: request } = await import('@/utils/request')
    const res = await request.post('/user/api/password/change', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    if (res.code === 200) {
      // 清除本地存储的用户信息
      localStorage.removeItem('userInfo')
      localStorage.removeItem('token')
      localStorage.removeItem('userRole')

      alert('密码修改成功，请重新登录')

      // 跳转到登录页
      window.location.href = '/user/login'
    } else {
      alert(res.message || '密码修改失败')
    }
  } catch (e) {
    console.error('修改密码失败', e)
    alert(e.response?.data?.message || '密码修改失败，请重试')
  }
}

// 获取用户余额
const fetchBalance = async () => {
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

// 触发头像上传
const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

// 处理头像上传
const handleAvatarUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }

  // 验证文件大小 (最大5MB)
  if (file.size > 5 * 1024 * 1024) {
    alert('图片大小不能超过5MB')
    return
  }

  uploadingAvatar.value = true
  try {
    // 创建FormData
    const formData = new FormData()
    formData.append('file', file)

    // 上传图片
    const { default: request } = await import('@/utils/request')
    const uploadRes = await request.post('/user/api/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    if (uploadRes.code === 200) {
      const avatarUrl = uploadRes.data.url

      // 更新用户头像
      const updateRes = await request.post('/user/api/profile/avatar', { avatar: avatarUrl })

      if (updateRes.code === 200) {
        userInfo.value.avatar = avatarUrl
        // 更新localStorage
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
        // 更新UserHeader组件中的用户信息
        window.dispatchEvent(new CustomEvent('user-avatar-updated', { detail: { avatar: avatarUrl } }))
        alert('头像上传成功')
      } else {
        alert(updateRes.message || '更新头像失败')
      }
    } else {
      alert(uploadRes.message || '上传失败')
    }
  } catch (e) {
    console.error('上传头像失败', e)
    alert('上传失败，请重试')
  } finally {
    uploadingAvatar.value = false
    // 清空input，允许重复上传同一文件
    if (event.target) {
      event.target.value = ''
    }
  }
}

// 处理缴纳押金
const handleDeposit = async () => {
  // 检查余额是否足够
  const balanceNum = parseFloat(userBalance.value)
  if (balanceNum < depositAmount.value) {
    alert('余额不足，请先充值')
    showDepositModal.value = false
    return
  }

  depositLoading.value = true
  try {
    const { default: request } = await import('@/utils/request')
    const res = await request.post('/user/api/deposit/pay', {
      amount: depositAmount.value
    })

    if (res.code === 200) {
      alert('押金缴纳成功')
      showDepositModal.value = false
      // 更新用户信息
      userInfo.value.deposit = depositAmount.value
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      // 刷新余额
      await fetchBalance()
    } else {
      alert(res.message || '缴纳失败')
    }
  } catch (e) {
    console.error('缴纳押金失败', e)
    alert('缴纳失败，请重试')
  } finally {
    depositLoading.value = false
  }
}

onMounted(() => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      userInfo.value = JSON.parse(userInfoStr)
      profileForm.realName = userInfo.value.realName || ''
      profileForm.phone = userInfo.value.phone || ''
      profileForm.idCard = userInfo.value.idCard || ''
    } catch (e) {
      console.error(e)
    }
  }
  // 获取余额
  fetchBalance()
  // 获取统计数据
  fetchStats()
})

// 获取用户统计数据
const fetchStats = async () => {
  try {
    const { default: request } = await import('@/utils/request')
    const res = await request.get('/user/api/order/stats')
    if (res.code === 200) {
      stats.value = {
        totalOrders: res.data.total || 0,
        totalHours: res.data.totalHours || 0
      }
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
    stats.value = {
      totalOrders: 0,
      totalHours: 0
    }
  }
}
</script>

<style scoped>
:root {
  --primary-color: #3b82f6;
}

.page-title {
  font-weight: 700;
  margin-bottom: 30px;
}

.profile-card {
  background: white;
  border-radius: 20px;
  padding: 30px;
  padding-bottom: 50px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 520px;
}

.profile-avatar {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-edit {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 36px;
  height: 36px;
  background: var(--primary-color);
  border: 3px solid white;
  border-radius: 50%;
  color: white;
  cursor: pointer;
  transition: transform 0.2s;
  z-index: 2;
}

.avatar-edit:hover:not(:disabled) {
  transform: scale(1.1);
}

.avatar-edit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 头像上传加载状态 */
.avatar-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.avatar-loading .spinner-border {
  color: white;
  width: 24px;
  height: 24px;
}

.profile-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-top: auto;
  padding-top: 40px;
  border-top: 1px solid #e2e8f0;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--primary-color);
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.info-card {
  background: white;
  border-radius: 16px;
  padding: 25px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e2e8f0;
}

.card-header h5 {
  font-weight: 600;
  margin: 0;
}

.form-label {
  font-weight: 500;
  font-size: 14px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #f1f5f9;
}

.security-item:last-child {
  border-bottom: none;
}

.security-info {
  display: flex;
  gap: 15px;
}

.security-info i {
  font-size: 24px;
  color: var(--primary-color);
}

.security-info h6 {
  font-weight: 600;
  margin-bottom: 3px;
}

.security-info p {
  margin: 0;
  font-size: 14px;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 450px;
  padding: 30px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.modal-header h5 {
  font-weight: 600;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #64748b;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 25px;
}

/* 头像上传时的遮罩 */
.profile-avatar {
  position: relative;
}

.profile-avatar.uploading::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-edit:hover {
  transform: scale(1.1);
}

/* 押金相关样式 */
.deposit-body {
  padding: 10px 0;
}

.deposit-info {
  background: #eff6ff;
  border-left: 3px solid var(--primary-color);
  padding: 12px;
  border-radius: 8px;
  font-size: 14px;
  color: #475569;
  display: flex;
  align-items: center;
}

.deposit-amount-display {
  font-size: 32px;
  font-weight: 700;
  color: var(--primary-color);
  padding: 15px;
  background: #f0f9ff;
  border-radius: 12px;
  text-align: center;
}

.payment-methods {
  display: flex;
  gap: 10px;
}

.payment-method {
  flex: 1;
  padding: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.payment-method:hover {
  border-color: var(--primary-color);
}

.payment-method.active {
  border-color: var(--primary-color);
  background: #f0f9ff;
  color: var(--primary-color);
}

.payment-method i {
  font-size: 20px;
}

.balance-hint {
  text-align: center;
  color: #64748b;
  font-size: 14px;
  padding: 10px;
  background: #f8fafc;
  border-radius: 8px;
}
</style>
