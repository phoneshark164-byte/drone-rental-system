<template>
  <div class="admin-reviews">
    <div class="page-header">
      <h3>评价管理</h3>
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalCount || 0 }}</div>
          <div class="stat-label">总评价数</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.visibleCount || 0 }}</div>
          <div class="stat-label">已显示</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.hiddenCount || 0 }}</div>
          <div class="stat-label">已隐藏</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.avgRating || 0 }}</div>
          <div class="stat-label">平均评分</div>
        </div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <div class="row g-3">
        <div class="col-md-4">
          <input
            v-model="searchKeyword"
            type="text"
            class="form-control"
            placeholder="搜索订单号、无人机编号、用户名..."
            @keyup.enter="loadReviews"
          />
        </div>
        <div class="col-md-2">
          <select v-model="selectedStatus" class="form-select" @change="loadReviews">
            <option value="">全部状态</option>
            <option value="1">显示</option>
            <option value="0">隐藏</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-primary w-100" @click="loadReviews">
            <i class="bi bi-search me-1"></i>搜索
          </button>
        </div>
      </div>
    </div>

    <!-- 评价列表 -->
    <div class="reviews-table-wrapper">
      <table class="table table-hover">
        <thead>
          <tr>
            <th width="14%">订单号</th>
            <th width="14%">无人机</th>
            <th width="14%">用户</th>
            <th width="14%">评分</th>
            <th width="15%">评价内容</th>
            <th width="15%">状态</th>
            <th width="14%">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="review in reviews" :key="review.id">
            <td>
              <small class="text-muted">{{ review.orderNo }}</small>
            </td>
            <td>
              <span class="badge bg-light text-dark">{{ review.droneNo }}</span>
            </td>
            <td>
              <div class="user-info">
                <span>{{ review.username }}</span>
              </div>
            </td>
            <td>
              <div class="rating-display">
                <i v-for="n in 5" :key="n"
                   :class="n <= review.rating ? 'bi bi-star-fill' : 'bi bi-star'"
                   :style="{ color: n <= review.rating ? '#ffc107' : '#e2e8f0', fontSize: '14px' }"></i>
                <span class="rating-text">{{ review.rating }}分</span>
              </div>
            </td>
            <td>
              <div class="review-content-preview">
                <span class="reviewed-badge" :class="{ 'replied': review.adminReply }" @click="viewReview(review)">
                  <i :class="review.adminReply ? 'bi bi-chat-check' : 'bi bi-chat-dots'" class="me-1"></i>
                  {{ review.adminReply ? '已回复' : '已评价' }}
                </span>
              </div>
            </td>
            <td>
              <span
                class="status-badge"
                :class="review.status === 1 ? 'active' : 'inactive'"
                @click="toggleStatus(review)"
              >
                {{ review.status === 1 ? '显示' : '隐藏' }}
              </span>
            </td>
            <td>
              <button class="btn btn-sm btn-outline-primary" @click="viewReview(review)" title="查看详情">
                <i class="bi bi-eye"></i>
              </button>
              <button class="btn btn-sm btn-outline-success" @click="replyReview(review)" title="回复">
                <i class="bi bi-chat"></i>
              </button>
              <button class="btn btn-sm btn-outline-danger" @click="deleteReviewConfirm(review)" title="删除">
                <i class="bi bi-trash"></i>
              </button>
            </td>
          </tr>
          <tr v-if="reviews.length === 0">
            <td colspan="7" class="text-center text-muted py-4">暂无评价数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="totalPages > 1">
      <nav>
        <ul class="pagination">
          <li class="page-item" :class="{ disabled: currentPage === 1 }">
            <a class="page-link" href="#" @click.prevent="changePage(currentPage - 1)">上一页</a>
          </li>
          <li v-for="page in displayedPages" :key="page" class="page-item" :class="{ active: page === currentPage }">
            <a class="page-link" href="#" @click.prevent="changePage(page)">{{ page }}</a>
          </li>
          <li class="page-item" :class="{ disabled: currentPage === totalPages }">
            <a class="page-link" href="#" @click.prevent="changePage(currentPage + 1)">下一页</a>
          </li>
        </ul>
      </nav>
    </div>

    <!-- 查看详情模态框 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content modal-lg" @click.stop>
        <div class="modal-header">
          <h5>评价详情</h5>
          <button class="btn-close" @click="closeModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <div v-if="currentReview" class="review-detail">
            <div class="detail-row">
              <span class="detail-label">订单号：</span>
              <span class="detail-value">{{ currentReview.orderNo }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">无人机：</span>
              <span class="detail-value">{{ currentReview.droneNo }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">用户：</span>
              <span class="detail-value">{{ currentReview.username }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">评价时间：</span>
              <span class="detail-value">{{ formatDateTime(currentReview.createTime) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">总体评分：</span>
              <span class="detail-value">
                <i v-for="n in 5" :key="n"
                   :class="n <= currentReview.rating ? 'bi bi-star-fill' : 'bi bi-star'"
                   :style="{ color: n <= currentReview.rating ? '#ffc107' : '#e2e8f0' }"></i>
                {{ currentReview.rating }} 分
              </span>
            </div>
            <div class="detail-row" v-if="currentReview.serviceRating">
              <span class="detail-label">服务评分：</span>
              <span class="detail-value">
                <i v-for="n in 5" :key="n"
                   :class="n <= currentReview.serviceRating ? 'bi bi-star-fill' : 'bi bi-star'"
                   :style="{ color: n <= currentReview.serviceRating ? '#ffc107' : '#e2e8f0' }"></i>
                {{ currentReview.serviceRating }} 分
              </span>
            </div>
            <div class="detail-row" v-if="currentReview.valueRating">
              <span class="detail-label">性价比评分：</span>
              <span class="detail-value">
                <i v-for="n in 5" :key="n"
                   :class="n <= currentReview.valueRating ? 'bi bi-star-fill' : 'bi bi-star'"
                   :style="{ color: n <= currentReview.valueRating ? '#ffc107' : '#e2e8f0' }"></i>
                {{ currentReview.valueRating }} 分
              </span>
            </div>
            <div class="detail-row full-width">
              <span class="detail-label">评价标签：</span>
              <span class="detail-value">
                <div v-if="currentReview.tags" class="review-tags">
                  <span v-for="tag in currentReview.tags.split(',')" :key="tag" class="tag-badge">
                    {{ getTagLabel(tag) }}
                  </span>
                </div>
                <span v-else class="text-muted">无</span>
              </span>
            </div>
            <div class="detail-row full-width">
              <span class="detail-label">评价内容：</span>
              <div class="detail-value content-box">{{ currentReview.content || '无文字内容' }}</div>
            </div>
            <div class="detail-row full-width" v-if="currentReview.adminReply">
              <span class="detail-label">管理员回复：</span>
              <div class="detail-value reply-box">
                <div class="reply-time">{{ formatDateTime(currentReview.adminReplyTime) }}</div>
                <p>{{ currentReview.adminReply }}</p>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeModal">关闭</button>
          <button class="btn btn-primary" @click="replyReview(currentReview)">
            <i class="bi bi-chat me-1"></i>回复
          </button>
        </div>
      </div>
    </div>

    <!-- 回复模态框 -->
    <div v-if="showReplyModal" class="modal-overlay" @click="closeReplyModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h5>回复评价</h5>
          <button class="btn-close" @click="closeReplyModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="original-review">
            <div class="review-meta">
              <span class="badge bg-light text-dark">{{ currentReview?.droneNo }}</span>
              <span class="rating-stars">
                <i v-for="n in 5" :key="n"
                   :class="n <= (currentReview?.rating || 0) ? 'bi bi-star-fill' : 'bi bi-star'"
                   :style="{ color: n <= (currentReview?.rating || 0) ? '#ffc107' : '#e2e8f0', fontSize: '14px' }"></i>
              </span>
              <span class="review-date">{{ formatDateTime(currentReview?.createTime) }}</span>
            </div>
            <p class="review-content">{{ currentReview?.content || '无文字内容' }}</p>
          </div>
          <div class="reply-form">
            <label class="form-label">回复内容 *</label>
            <textarea
              v-model="replyContent"
              class="form-control"
              rows="4"
              placeholder="请输入回复内容..."
              maxlength="500"
            ></textarea>
            <div class="char-count">{{ replyContent.length }}/500</div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeReplyModal">取消</button>
          <button class="btn btn-primary" @click="submitReply" :disabled="!replyContent.trim() || submitting">
            {{ submitting ? '提交中...' : '提交回复' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getReviewList, getReviewStats, updateReviewStatus, deleteReview, replyReview as apiReplyReview } from '@/api/admin'

const reviews = ref([])
const stats = ref({ totalCount: 0, visibleCount: 0, hiddenCount: 0, avgRating: 0 })
const searchKeyword = ref('')
const selectedStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(0)

const showDetailModal = ref(false)
const showReplyModal = ref(false)
const currentReview = ref(null)
const replyContent = ref('')
const submitting = ref(false)

const reviewTags = [
  { value: 'flight_experience', label: '飞行体验好' },
  { value: 'aircraft_condition', label: '机况良好' },
  { value: 'ease_of_use', label: '易于操作' },
  { value: 'cost_effectiveness', label: '性价比高' },
  { value: 'stable', label: '飞行稳定' },
  { value: 'clear_photo', label: '拍照清晰' }
]

const displayedPages = computed(() => {
  const pages = []
  const start = Math.max(1, currentPage.value - 2)
  const end = Math.min(totalPages.value, currentPage.value + 2)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

const getTagLabel = (tag) => {
  const found = reviewTags.find(t => t.value === tag)
  return found ? found.label : tag
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

// 加载评价列表
const loadReviews = async () => {
  try {
    const res = await getReviewList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      status: selectedStatus.value !== '' ? selectedStatus.value : undefined
    })
    if (res.code === 200) {
      reviews.value = res.data.records || []
      totalPages.value = res.data.pages || 1
    }
  } catch (error) {
    console.error('加载评价列表失败:', error)
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    const res = await getReviewStats()
    if (res.code === 200) {
      stats.value = res.data
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadReviews()
}

const viewReview = (review) => {
  currentReview.value = review
  showDetailModal.value = true
}

const replyReview = (review) => {
  currentReview.value = review
  replyContent.value = review.adminReply || ''
  showReplyModal.value = true
  showDetailModal.value = false
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    alert('请输入回复内容')
    return
  }

  submitting.value = true
  try {
    const res = await apiReplyReview(currentReview.value.id, replyContent.value)
    if (res.code === 200) {
      alert('回复成功')
      closeReplyModal()
      loadReviews()
    } else {
      alert(res.message || '回复失败')
    }
  } catch (error) {
    console.error('回复失败:', error)
    alert('回复失败，请重试')
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (review) => {
  const newStatus = review.status === 1 ? 0 : 1
  try {
    const res = await updateReviewStatus(review.id, newStatus)
    if (res.code === 200) {
      review.status = newStatus
      loadStats()
    } else {
      alert(res.message || '操作失败')
    }
  } catch (error) {
    console.error('更新状态失败:', error)
    alert('操作失败')
  }
}

const deleteReviewConfirm = (review) => {
  if (confirm(`确定要删除这条评价吗？\n订单号：${review.orderNo}\n用户：${review.username}`)) {
    deleteReviewItem(review)
  }
}

const deleteReviewItem = async (review) => {
  try {
    const res = await deleteReview(review.id)
    if (res.code === 200) {
      alert('删除成功')
      loadReviews()
      loadStats()
    } else {
      alert(res.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    alert('删除失败')
  }
}

const closeModal = () => {
  showDetailModal.value = false
  currentReview.value = null
}

const closeReplyModal = () => {
  showReplyModal.value = false
  currentReview.value = null
  replyContent.value = ''
}

onMounted(() => {
  loadReviews()
  loadStats()
})
</script>

<style scoped>
.admin-reviews {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.page-header h3 {
  font-weight: 600;
  margin: 0;
}

.stats-cards {
  display: flex;
  gap: 15px;
}

.stat-card {
  background: white;
  border-radius: 10px;
  padding: 15px 20px;
  min-width: 100px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #3b82f6;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  margin-top: 5px;
}

.filter-bar {
  background: white;
  border-radius: 10px;
  padding: 15px 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.reviews-table-wrapper {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.reviews-table-wrapper table {
  margin: 0;
}

.reviews-table-wrapper th {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
  text-align: center;
}

.reviews-table-wrapper td {
  vertical-align: middle;
  text-align: center;
}

.user-info {
  font-size: 13px;
}

.rating-display {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.rating-text {
  font-size: 13px;
  color: #64748b;
  margin-left: 3px;
}

.review-content-preview {
  max-width: 300px;
}

.review-content-preview p {
  margin: 0;
  font-size: 13px;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.reviewed-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  background: #eff6ff;
  color: #3b82f6;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.reviewed-badge:hover {
  background: #3b82f6;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.reviewed-badge.replied {
  background: #d1fae5;
  color: #059669;
}

.reviewed-badge.replied:hover {
  background: #059669;
  color: white;
  box-shadow: 0 2px 8px rgba(5, 150, 105, 0.3);
}

.review-tags {
  display: flex;
  gap: 5px;
  margin-top: 5px;
}

.tag-badge {
  padding: 2px 8px;
  background: #f1f5f9;
  color: #64748b;
  border-radius: 12px;
  font-size: 11px;
}

.admin-reply-preview {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 5px;
  color: #059669;
  font-size: 12px;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.status-badge.active {
  background: #d1fae5;
  color: #059669;
}

.status-badge.inactive {
  background: #fee2e2;
  color: #dc2626;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.page-link {
  cursor: pointer;
}

/* 模态框样式 */
.modal-overlay {
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

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow: auto;
}

.modal-lg {
  max-width: 600px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h5 {
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #64748b;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 15px 20px;
  border-top: 1px solid #e2e8f0;
}

/* 评价详情 */
.review-detail {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.detail-row.full-width {
  flex-direction: column;
  align-items: stretch;
}

.detail-label {
  color: #64748b;
  font-size: 13px;
  min-width: 80px;
}

.detail-value {
  color: #475569;
  font-size: 14px;
}

.detail-value.content-box {
  background: #f8fafc;
  padding: 12px;
  border-radius: 8px;
  white-space: pre-wrap;
  line-height: 1.5;
}

.detail-value.reply-box {
  background: #f0fdf4;
  padding: 12px;
  border-radius: 8px;
}

.reply-time {
  font-size: 12px;
  color: #059669;
  margin-bottom: 5px;
}

/* 原评价展示 */
.original-review {
  background: #f8fafc;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.review-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.rating-stars {
  display: flex;
  gap: 2px;
}

.review-date {
  font-size: 12px;
  color: #94a3b8;
}

.review-content {
  color: #475569;
  line-height: 1.5;
}

/* 回复表单 */
.reply-form {
  margin-top: 15px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #475569;
  font-size: 14px;
}

.char-count {
  text-align: right;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 5px;
}
</style>
