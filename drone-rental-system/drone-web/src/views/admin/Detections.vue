<template>
  <div class="admin-detections">
    <div class="page-header">
      <h3>AI 损伤检测记录</h3>
      <button class="btn btn-outline-primary" @click="loadData">
        <i class="bi bi-arrow-clockwise me-1"></i>刷新
      </button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: #e3f2fd; color: #1976d2;">
          <i class="bi bi-clipboard-data"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalCount }}</div>
          <div class="stat-label">总检测次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #fff3e0; color: #f57c00;">
          <i class="bi bi-exclamation-triangle"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.damageCount }}</div>
          <div class="stat-label">检测到损伤</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #ffebee; color: #c62828;">
          <i class="bi bi-person"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.userRespCount }}</div>
          <div class="stat-label">用户责任</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #fff8e1; color: #f9a825;">
          <i class="bi bi-building"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.operatorRespCount }}</div>
          <div class="stat-label">运营方责任</div>
        </div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <div class="row g-3">
        <div class="col-md-3">
          <input
            v-model="filterKeyword"
            type="text"
            class="form-control"
            placeholder="搜索检测编号、用户名称"
            @keyup.enter="loadData"
          />
        </div>
        <div class="col-md-2">
          <select v-model="filterResponsibility" class="form-select" @change="loadData">
            <option value="">全部责任</option>
            <option value="user">用户责任</option>
            <option value="operator">运营方责任</option>
            <option value="shared">共同责任</option>
            <option value="none">无损伤</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-primary w-100" @click="loadData">
            <i class="bi bi-search me-1"></i>搜索
          </button>
        </div>
      </div>
    </div>

    <!-- 检测记录表格 -->
    <div class="table-container">
      <table class="table table-hover">
        <thead>
          <tr>
            <th>检测编号</th>
            <th>用户</th>
            <th>关联无人机</th>
            <th>损伤数量</th>
            <th>严重程度</th>
            <th>责任判定</th>
            <th>检测时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="text-center py-4">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">加载中...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="detections.length === 0">
            <td colspan="8" class="text-center py-4 text-muted">暂无检测记录</td>
          </tr>
          <tr v-else v-for="item in detections" :key="item.id">
            <td>
              <span class="detection-no">{{ item.detectionNo }}</span>
            </td>
            <td>{{ item.userName }}</td>
            <td>{{ item.vehicleNo || '-' }}</td>
            <td>
              <span class="badge" :class="item.damageCount > 0 ? 'bg-warning' : 'bg-success'">
                {{ item.damageCount }} 处
              </span>
            </td>
            <td>
              <span class="severity-badge" :class="'severity-' + item.overallSeverity">
                {{ getSeverityText(item.overallSeverity) }}
              </span>
            </td>
            <td>
              <span class="responsibility-badge" :class="'resp-' + item.responsibility">
                {{ getResponsibilityText(item.responsibility) }}
              </span>
            </td>
            <td>{{ formatTime(item.createTime) }}</td>
            <td>
              <button class="btn btn-sm btn-outline-primary me-1" @click="viewDetail(item)">
                <i class="bi bi-eye"></i>
              </button>
              <button class="btn btn-sm btn-outline-danger" @click="deleteDetection(item)">
                <i class="bi bi-trash"></i>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <nav class="pagination-nav" v-if="totalPages > 1">
      <ul class="pagination justify-content-center">
        <li class="page-item" :class="{ disabled: currentPage === 1 }">
          <a class="page-link" href="#" @click.prevent="goToPage(currentPage - 1)">上一页</a>
        </li>
        <li
          v-for="page in displayedPages"
          :key="page"
          class="page-item"
          :class="{ active: page === currentPage }"
        >
          <a class="page-link" href="#" @click.prevent="goToPage(page)">{{ page }}</a>
        </li>
        <li class="page-item" :class="{ disabled: currentPage === totalPages }">
          <a class="page-link" href="#" @click.prevent="goToPage(currentPage + 1)">下一页</a>
        </li>
      </ul>
    </nav>

    <!-- 详情弹窗 -->
    <Teleport to="body">
      <div class="modal fade" id="detailModal" tabindex="-1" ref="detailModal" v-if="selectedDetection">
        <div class="modal-dialog modal-lg">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">检测详情 - {{ selectedDetection.detectionNo }}</h5>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>
            <div class="modal-body">
            <!-- 基本信息 -->
            <div class="detail-section">
              <h6>基本信息</h6>
              <div class="row">
                <div class="col-6">
                  <label>检测用户</label>
                  <div>{{ selectedDetection.userName }}</div>
                </div>
                <div class="col-6">
                  <label>关联无人机</label>
                  <div>{{ selectedDetection.vehicleNo || '未关联' }}</div>
                </div>
                <div class="col-6">
                  <label>检测时间</label>
                  <div>{{ selectedDetection.createTime }}</div>
                </div>
                <div class="col-6">
                  <label>推理耗时</label>
                  <div>{{ selectedDetection.inferenceTime }} 秒</div>
                </div>
              </div>
            </div>

            <!-- 检测结果 -->
            <div class="detail-section">
              <h6>检测结果</h6>
              <div class="result-summary">
                <div class="summary-item">
                  <span class="summary-label">损伤数量</span>
                  <span class="summary-value">{{ selectedDetection.damageCount }} 处</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">严重程度</span>
                  <span class="summary-value" :class="'severity-' + selectedDetection.overallSeverity">
                    {{ getSeverityText(selectedDetection.overallSeverity) }}
                  </span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">责任判定</span>
                  <span class="summary-value" :class="'resp-' + selectedDetection.responsibility">
                    {{ getResponsibilityText(selectedDetection.responsibility) }}
                  </span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">判定原因</span>
                  <span class="summary-value">{{ selectedDetection.responsibilityReason }}</span>
                </div>
              </div>
            </div>

            <!-- 检测图片 -->
            <div class="detail-section">
              <h6>检测图片</h6>
              <div class="detection-images">
                <div class="image-item">
                  <label>原始图片</label>
                  <img v-if="selectedDetection.imageUrl" :src="selectedDetection.imageUrl" alt="原始图片" />
                  <div v-else class="no-image">暂无图片</div>
                </div>
                <div class="image-item">
                  <label>标注结果</label>
                  <img v-if="selectedDetection.resultImageUrl" :src="selectedDetection.resultImageUrl" alt="标注结果" />
                  <div v-else class="no-image">暂无图片</div>
                </div>
              </div>
            </div>

            <!-- 关联报修 -->
            <div class="detail-section" v-if="selectedDetection.autoRepairId">
              <h6>关联报修</h6>
              <div class="alert alert-info">
                <i class="bi bi-info-circle me-2"></i>
                系统已自动创建报修单，报修ID: {{ selectedDetection.autoRepairId }}
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeModal">关闭</button>
          </div>
        </div>
      </div>
    </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import axios from 'axios'
import { Modal } from 'bootstrap'

const detections = ref([])
const stats = ref({
  totalCount: 0,
  damageCount: 0,
  userRespCount: 0,
  operatorRespCount: 0,
  noDamageCount: 0
})
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(0)
const filterKeyword = ref('')
const filterResponsibility = ref('')
const selectedDetection = ref(null)
const detailModal = ref(null)
let modalInstance = null

const displayedPages = computed(() => {
  const pages = []
  const maxShown = 5
  let start = Math.max(1, currentPage.value - Math.floor(maxShown / 2))
  let end = Math.min(totalPages.value, start + maxShown - 1)
  if (end - start < maxShown - 1) {
    start = Math.max(1, end - maxShown + 1)
  }
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (filterResponsibility.value) {
      params.responsibility = filterResponsibility.value
    }
    if (filterKeyword.value) {
      params.keyword = filterKeyword.value
    }

    const res = await axios.get('/admin/api/detections', { params })
    if (res.data.code === 200) {
      detections.value = res.data.data.records || []
      totalPages.value = res.data.data.pages || 1
    }
  } catch (error) {
    console.error('加载检测记录失败:', error)
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await axios.get('/admin/api/detection/stats')
    if (res.data.code === 200) {
      stats.value = res.data.data
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    loadData()
  }
}

const viewDetail = async (item) => {
  try {
    const res = await axios.get(`/admin/api/detection/${item.id}`)
    if (res.data.code === 200) {
      selectedDetection.value = res.data.data
      // 等待 DOM 更新（Teleport 需要）
      await nextTick()
      const modalEl = document.getElementById('detailModal')
      if (modalEl) {
        // 销毁旧实例
        if (modalInstance) {
          modalInstance.dispose()
          modalInstance = null
        }
        // 创建新实例
        modalInstance = new Modal(modalEl, {
          backdrop: true,
          keyboard: true,
          focus: true
        })
        modalInstance.show()
      }
    }
  } catch (error) {
    console.error('加载详情失败:', error)
  }
}

const closeModal = () => {
  if (modalInstance) {
    modalInstance.hide()
  }
  // 立即清空数据，关闭动画由 Bootstrap 处理
  selectedDetection.value = null
}

const deleteDetection = async (item) => {
  if (!confirm(`确定要删除检测记录 ${item.detectionNo} 吗？`)) {
    return
  }

  try {
    const res = await axios.delete(`/admin/api/detection/${item.id}`)
    if (res.data.code === 200) {
      alert('删除成功')
      loadData()
      loadStats()
    } else {
      alert(res.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    alert('删除失败，请稍后重试')
  }
}

const getSeverityText = (severity) => {
  const map = {
    'minor': '轻微',
    'moderate': '中等',
    'severe': '严重'
  }
  return map[severity] || '-'
}

const getResponsibilityText = (resp) => {
  const map = {
    'user': '用户责任',
    'operator': '运营方责任',
    'shared': '共同责任',
    'none': '无损伤'
  }
  return map[resp] || '-'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
  loadStats()
})
</script>

<style scoped>
.admin-detections {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h3 {
  margin: 0;
  font-weight: 600;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
}

.filter-bar {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.table-container {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.table {
  margin: 0;
}

.table th {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
}

.detection-no {
  font-family: monospace;
  color: #3b82f6;
}

.severity-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.severity-minor {
  background: #d1fae5;
  color: #059669;
}

.severity-moderate {
  background: #fef3c7;
  color: #d97706;
}

.severity-severe {
  background: #fee2e2;
  color: #dc2626;
}

.responsibility-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.resp-user {
  background: #fee2e2;
  color: #dc2626;
}

.resp-operator {
  background: #fef3c7;
  color: #d97706;
}

.resp-shared {
  background: #dbeafe;
  color: #2563eb;
}

.resp-none {
  background: #d1fae5;
  color: #059669;
}

.pagination-nav {
  margin-top: 20px;
}

.detail-section {
  padding: 16px 0;
  border-bottom: 1px solid #e5e7eb;
}

.detail-section:last-child {
  border-bottom: none;
}

.detail-section h6 {
  font-weight: 600;
  margin-bottom: 12px;
  color: #374151;
}

.detail-section label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.summary-item {
  background: #f9fafb;
  padding: 12px;
  border-radius: 8px;
}

.summary-label {
  display: block;
  font-size: 12px;
  color: #6b7280;
}

.summary-value {
  font-weight: 600;
  color: #1f2937;
}

.detection-images {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.image-item label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.image-item img {
  width: 100%;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.no-image {
  width: 100%;
  height: 200px;
  background: #f3f4f6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}
</style>
