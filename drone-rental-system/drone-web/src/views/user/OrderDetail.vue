<template>
  <div class="order-detail-page">
    <UserHeader />

    <div class="container py-5">
      <div class="page-header d-flex justify-content-between align-items-center mb-4">
        <div>
          <router-link to="/user/orders" class="btn btn-outline-secondary me-2">
            <i class="bi bi-arrow-left me-1"></i>返回订单列表
          </router-link>
          <router-link to="/" class="btn btn-outline-primary">
            <i class="bi bi-house me-1"></i>返回首页
          </router-link>
        </div>
        <h2 class="page-title mb-0">订单详情</h2>
      </div>

      <div v-if="order" class="order-detail">
        <!-- 订单状态 -->
        <div class="status-card" :class="'status-' + order.status">
          <div class="status-icon">
            <i :class="getStatusIcon(order.status)"></i>
          </div>
          <div class="status-info">
            <h3>{{ getStatusTitle(order.status) }}</h3>
            <p>{{ getStatusDesc(order.status) }}</p>
          </div>
        </div>

        <!-- 无人机信息 -->
        <div class="info-card">
          <h4 class="card-title">无人机信息</h4>
          <div class="drone-info">
            <img :src="order.droneImage" :alt="order.droneModel" class="drone-image" />
            <div class="drone-details">
              <h5>{{ order.droneModel }}</h5>
              <p class="text-muted">编号：{{ order.droneNo }}</p>
            </div>
          </div>
        </div>

        <!-- 订单信息 -->
        <div class="info-card">
          <h4 class="card-title">订单信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">订单编号：</span>
              <span class="value">{{ order.orderNo }}</span>
            </div>
            <div class="info-item">
              <span class="label">创建时间：</span>
              <span class="value">{{ order.createTime }}</span>
            </div>
            <div class="info-item">
              <span class="label">开始时间：</span>
              <span class="value">{{ order.startTime }}</span>
            </div>
            <div class="info-item">
              <span class="label">结束时间：</span>
              <span class="value">{{ order.endTime || '进行中' }}</span>
            </div>
            <div class="info-item">
              <span class="label">租用时长：</span>
              <span class="value">{{ order.actualDuration || order.plannedDuration }}分钟</span>
            </div>
            <div class="info-item">
              <span class="label">订单金额：</span>
              <span class="value amount">¥{{ order.amount }}</span>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <button
            v-if="order.status === 0"
            class="btn btn-primary"
            @click="handlePay"
          >
            <i class="bi bi-credit-card me-2"></i>立即支付
          </button>
          <button
            v-if="order.status === 1"
            class="btn btn-primary"
            @click="handleStart"
          >
            <i class="bi bi-play-circle me-2"></i>开始使用
          </button>
          <button
            v-if="order.status === 2"
            class="btn btn-warning"
            @click="handleEnd"
          >
            <i class="bi bi-stop-circle me-2"></i>结束使用
          </button>
          <button
            v-if="order.status === 0 || order.status === 1"
            class="btn btn-outline-danger"
            @click="handleCancel"
          >
            <i class="bi bi-x-circle me-2"></i>取消订单
          </button>
          <button
            v-if="order.status === 3 && !hasReviewed"
            class="btn btn-success"
            @click="showReviewModal = true"
          >
            <i class="bi bi-star me-2"></i>评价订单
          </button>
          <button
            v-if="order.status === 3 && hasReviewed"
            class="btn btn-outline-secondary"
            disabled
          >
            <i class="bi bi-check-circle me-2"></i>已评价
          </button>
        </div>

        <!-- 评价展示 -->
        <div v-if="order.status === 3 && review" class="info-card">
          <h4 class="card-title">我的评价</h4>
          <div class="review-display">
            <div class="review-header">
              <div class="rating-stars">
                <i v-for="n in 5" :key="n"
                   :class="n <= review.rating ? 'bi bi-star-fill' : 'bi bi-star'"
                   :style="{ color: n <= review.rating ? '#ffc107' : '#e2e8f0' }"></i>
              </div>
              <span class="review-date">{{ review.createTime }}</span>
            </div>
            <p class="review-content">{{ review.content || '暂无评价内容' }}</p>
            <div v-if="review.tags" class="review-tags">
              <span v-for="tag in review.tags.split(',')" :key="tag" class="tag-badge">{{ getTagLabel(tag) }}</span>
            </div>
            <div v-if="review.adminReply" class="admin-reply">
              <div class="reply-header">
                <i class="bi bi-person-circle me-1"></i>
                <strong>管理员回复：</strong>
              </div>
              <p class="reply-content">{{ review.adminReply }}</p>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="text-center py-5">
        <p>订单不存在</p>
        <router-link to="/user/orders" class="btn btn-primary">返回订单列表</router-link>
      </div>
    </div>

    <!-- 评价弹窗 -->
    <div v-if="showReviewModal" class="review-modal-overlay" @click.self="showReviewModal = false">
      <div class="review-modal">
        <div class="modal-header">
          <h4>评价订单</h4>
          <button type="button" class="btn-close" @click="showReviewModal = false"></button>
        </div>
        <div class="modal-body">
          <div class="review-form">
            <!-- 总体评分 -->
            <div class="form-group">
              <label class="form-label">总体评分</label>
              <div class="star-rating">
                <i v-for="n in 5" :key="n"
                   :class="n <= reviewForm.rating ? 'bi bi-star-fill active' : 'bi bi-star'"
                   @click="reviewForm.rating = n"
                   @mouseover="hoverRating = n"
                   @mouseleave="hoverRating = 0"
                   :style="{ color: (n <= reviewForm.rating || n <= hoverRating) ? '#ffc107' : '#e2e8f0' }"></i>
              </div>
            </div>

            <!-- 评价标签 -->
            <div class="form-group">
              <label class="form-label">评价标签（可多选）</label>
              <div class="tag-selector">
                <span v-for="tag in reviewTags" :key="tag.value"
                      :class="['tag-option', { active: selectedTags.includes(tag.value) }]"
                      @click="toggleTag(tag.value)">
                  <i :class="tag.icon"></i> {{ tag.label }}
                </span>
              </div>
            </div>

            <!-- 服务评分 -->
            <div class="form-group">
              <label class="form-label">服务评分</label>
              <div class="star-rating small">
                <i v-for="n in 5" :key="n"
                   :class="n <= reviewForm.serviceRating ? 'bi bi-star-fill active' : 'bi bi-star'"
                   @click="reviewForm.serviceRating = n"
                   :style="{ color: n <= reviewForm.serviceRating ? '#ffc107' : '#e2e8f0' }"></i>
              </div>
            </div>

            <!-- 性价比评分 -->
            <div class="form-group">
              <label class="form-label">性价比评分</label>
              <div class="star-rating small">
                <i v-for="n in 5" :key="n"
                   :class="n <= reviewForm.valueRating ? 'bi bi-star-fill active' : 'bi bi-star'"
                   @click="reviewForm.valueRating = n"
                   :style="{ color: n <= reviewForm.valueRating ? '#ffc107' : '#e2e8f0' }"></i>
              </div>
            </div>

            <!-- 评价内容 -->
            <div class="form-group">
              <label class="form-label">评价内容</label>
              <textarea v-model="reviewForm.content" class="form-control" rows="4"
                        placeholder="分享您的使用体验，帮助其他用户了解这款无人机..."></textarea>
            </div>

            <!-- 图片上传（简化版） -->
            <div class="form-group">
              <label class="form-label">上传图片（可选）</label>
              <div class="image-upload-area">
                <i class="bi bi-cloud-upload"></i>
                <span>点击上传图片</span>
                <small>支持 jpg、png 格式，最多3张</small>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="showReviewModal = false">取消</button>
          <button type="button" class="btn btn-primary" @click="submitReview" :disabled="reviewForm.rating === 0 || submitting">
            {{ submitting ? '提交中...' : '提交评价' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UserHeader from '@/components/UserHeader.vue'
import { getOrderDetail, payOrder, startUse, endUse, cancelOrder, checkReview, getReviewByOrderId, createReview } from '@/api/user'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const review = ref(null)
const hasReviewed = ref(false)
const showReviewModal = ref(false)
const submitting = ref(false)
const hoverRating = ref(0)

const reviewForm = ref({
  orderId: null,
  rating: 0,
  serviceRating: 5,
  valueRating: 5,
  content: '',
  images: '',
  tags: ''
})

const selectedTags = ref([])

const reviewTags = [
  { value: 'flight_experience', label: '飞行体验好', icon: 'bi bi-airplane' },
  { value: 'aircraft_condition', label: '机况良好', icon: 'bi bi-check-circle' },
  { value: 'ease_of_use', label: '易于操作', icon: 'bi bi-hand-thumbs-up' },
  { value: 'cost_effectiveness', label: '性价比高', icon: 'bi bi-tag' },
  { value: 'stable', label: '飞行稳定', icon: 'bi bi-lightning' },
  { value: 'clear_photo', label: '拍照清晰', icon: 'bi bi-camera' }
]

const getTagLabel = (tag) => {
  const found = reviewTags.find(t => t.value === tag)
  return found ? found.label : tag
}

const toggleTag = (tag) => {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tag)
  }
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const getStatusIcon = (status) => {
  const iconMap = {
    0: 'bi bi-credit-card',
    1: 'bi bi-clock',
    2: 'bi bi-airplane',
    3: 'bi bi-check-circle',
    4: 'bi bi-x-circle'
  }
  return iconMap[status] || 'bi bi-question-circle'
}

const getStatusTitle = (status) => {
  const titleMap = {
    0: '待支付',
    1: '已支付',
    2: '使用中',
    3: '已完成',
    4: '已取消'
  }
  return titleMap[status] || '未知状态'
}

const getStatusDesc = (status) => {
  const descMap = {
    0: '请尽快完成支付，订单将在30分钟后自动取消',
    1: '支付成功，请在预约时间开始使用',
    2: '正在使用中，请注意安全飞行',
    3: '订单已完成，感谢您的使用',
    4: '订单已取消'
  }
  return descMap[status] || ''
}

// 加载订单详情
const loadOrderDetail = async () => {
  const id = route.params.id
  try {
    const res = await getOrderDetail(id)
    if (res.code === 200 && res.data) {
      const data = res.data
      order.value = {
        id: data.id,
        orderNo: data.orderNo,
        droneNo: data.droneNo || 'DRONE-' + data.vehicleId,
        droneModel: data.droneModel || '无人机',
        droneImage: data.droneImage || '/img/train/0001.jpg',
        startTime: formatDateTime(data.startTime || data.createTime),
        endTime: formatDateTime(data.endTime),
        plannedDuration: data.plannedDuration,
        actualDuration: data.actualDuration,
        amount: data.amount || '0.00',
        status: data.status,
        createTime: formatDateTime(data.createTime),
        startLocation: data.startLocation,
        endLocation: data.endLocation
      }

      // 设置评价表单的订单ID
      reviewForm.value.orderId = data.id

      // 检查是否已评价
      await checkOrderReview(data.id)
    }
  } catch (error) {
    console.error('加载订单详情失败:', error)
    alert('加载订单详情失败')
  }
}

// 检查订单是否已评价
const checkOrderReview = async (orderId) => {
  try {
    const res = await checkReview(orderId)
    if (res.code === 200) {
      hasReviewed.value = res.data
      if (res.data) {
        // 如果已评价，获取评价详情
        await loadOrderReview(orderId)
      }
    }
  } catch (error) {
    console.error('检查评价状态失败:', error)
  }
}

// 获取订单评价详情
const loadOrderReview = async (orderId) => {
  try {
    const res = await getReviewByOrderId(orderId)
    if (res.code === 200 && res.data) {
      review.value = {
        ...res.data,
        createTime: formatDateTime(res.data.createTime)
      }
    }
  } catch (error) {
    console.error('获取评价详情失败:', error)
  }
}

// 提交评价
const submitReview = async () => {
  if (reviewForm.value.rating === 0) {
    alert('请选择评分')
    return
  }

  submitting.value = true
  try {
    const params = {
      orderId: reviewForm.value.orderId,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content,
      tags: selectedTags.value.join(','),
      serviceRating: reviewForm.value.serviceRating,
      valueRating: reviewForm.value.valueRating
    }

    const res = await createReview(params)
    if (res.code === 200) {
      alert('评价提交成功！')
      showReviewModal.value = false
      // 重置表单
      reviewForm.value = {
        orderId: order.value.id,
        rating: 0,
        serviceRating: 5,
        valueRating: 5,
        content: '',
        images: '',
        tags: ''
      }
      selectedTags.value = []
      // 刷新评价状态
      await checkOrderReview(order.value.id)
    } else {
      alert(res.message || '提交失败')
    }
  } catch (error) {
    console.error('提交评价失败:', error)
    alert('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

const handlePay = async () => {
  if (window.showPaymentProgress) {
    window.showPaymentProgress(parseFloat(order.value.amount), 'order', order.value.id)
  } else {
    alert('支付功能加载中，请刷新页面重试')
  }
}

const handleStart = async () => {
  try {
    const res = await startUse(order.value.id)
    if (res.code === 200) {
      alert('开始使用成功')
      await loadOrderDetail()
    } else {
      alert(res.message || '操作失败')
    }
  } catch (error) {
    console.error('开始使用失败:', error)
    alert('操作失败')
  }
}

const handleEnd = async () => {
  if (confirm('确定要结束使用吗？')) {
    try {
      // 获取位置信息
      const position = await getCurrentPosition()
      const res = await endUse(
        order.value.id,
        position.latitude,
        position.longitude,
        position.location
      )
      if (res.code === 200) {
        alert('使用已结束')
        await loadOrderDetail()
      } else {
        alert(res.message || '操作失败')
      }
    } catch (error) {
      console.error('结束使用失败:', error)
      alert('操作失败')
    }
  }
}

// 获取当前位置
const getCurrentPosition = () => {
  return new Promise((resolve) => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            location: '用户位置'
          })
        },
        () => {
          resolve({ latitude: 0, longitude: 0, location: '未知位置' })
        }
      )
    } else {
      resolve({ latitude: 0, longitude: 0, location: '未知位置' })
    }
  })
}

const handleCancel = async () => {
  if (confirm('确定要取消订单吗？')) {
    try {
      const res = await cancelOrder(order.value.id, '用户主动取消')
      if (res.code === 200) {
        alert('订单已取消')
        await loadOrderDetail()
      } else {
        alert(res.message || '取消失败')
      }
    } catch (error) {
      console.error('取消订单失败:', error)
      alert('操作失败')
    }
  }
}

onMounted(() => {
  loadOrderDetail()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 30px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  color: #64748b;
  margin-bottom: 15px;
}

.back-link:hover {
  color: var(--primary-color);
}

.page-header h2 {
  font-weight: 700;
  margin: 0;
}

.status-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: white;
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.status-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.status-0 .status-icon { background: #fef3c7; color: #d97706; }
.status-1 .status-icon { background: #dbeafe; color: #2563eb; }
.status-2 .status-icon { background: #d1fae5; color: #059669; }
.status-3 .status-icon { background: #f1f5f9; color: #64748b; }
.status-4 .status-icon { background: #fee2e2; color: #dc2626; }

.status-info h3 {
  font-weight: 600;
  margin-bottom: 5px;
}

.status-info p {
  color: #64748b;
  margin: 0;
}

.info-card {
  background: white;
  border-radius: 16px;
  padding: 25px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.card-title {
  font-weight: 600;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e2e8f0;
}

.drone-info {
  display: flex;
  gap: 20px;
}

.drone-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 12px;
}

.drone-details h5 {
  font-weight: 600;
  margin-bottom: 5px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.info-item .label {
  font-size: 13px;
  color: #64748b;
}

.info-item .value {
  font-weight: 500;
}

.info-item .value.amount {
  color: var(--primary-color);
  font-size: 18px;
}

.action-buttons {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.action-buttons .btn {
  padding: 12px 30px;
  border-radius: 10px;
  font-weight: 500;
}

@media (max-width: 576px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}

/* 评价展示样式 */
.review-display {
  padding: 15px 0;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.rating-stars {
  display: flex;
  gap: 5px;
}

.rating-stars i {
  font-size: 20px;
}

.review-date {
  color: #94a3b8;
  font-size: 14px;
}

.review-content {
  color: #475569;
  line-height: 1.6;
  margin-bottom: 15px;
}

.review-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 15px;
}

.tag-badge {
  padding: 5px 12px;
  background: #f1f5f9;
  color: #64748b;
  border-radius: 20px;
  font-size: 13px;
}

.admin-reply {
  background: #f8fafc;
  padding: 15px;
  border-radius: 10px;
  margin-top: 15px;
}

.reply-header {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 8px;
}

.reply-content {
  color: #475569;
  margin: 0;
}

/* 评价弹窗样式 */
.review-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.review-modal {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 550px;
  max-height: 90vh;
  overflow: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h4 {
  margin: 0;
  font-weight: 600;
}

.modal-body {
  padding: 25px;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 15px 25px;
  border-top: 1px solid #e2e8f0;
}

.review-form .form-group {
  margin-bottom: 20px;
}

.review-form .form-label {
  font-weight: 500;
  margin-bottom: 10px;
  display: block;
}

.star-rating {
  display: flex;
  gap: 8px;
}

.star-rating i {
  font-size: 32px;
  cursor: pointer;
  transition: transform 0.2s;
  color: #e2e8f0;
}

.star-rating i:hover {
  transform: scale(1.1);
}

.star-rating i.active {
  color: #ffc107 !important;
}

.star-rating.small i {
  font-size: 24px;
}

.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-option {
  padding: 8px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  background: white;
}

.tag-option:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.tag-option.active {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #3b82f6;
}

.review-form .form-control {
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px 16px;
  resize: none;
}

.review-form .form-control:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.image-upload-area {
  border: 2px dashed #e2e8f0;
  border-radius: 10px;
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.image-upload-area:hover {
  border-color: #3b82f6;
  background: #f8fafc;
}

.image-upload-area i {
  font-size: 32px;
  color: #94a3b8;
  display: block;
  margin-bottom: 10px;
}

.image-upload-area span {
  display: block;
  color: #475569;
  margin-bottom: 5px;
}

.image-upload-area small {
  color: #94a3b8;
}
</style>
