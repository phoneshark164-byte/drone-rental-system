<template>
  <div class="admin-recharges-page">
    <div class="page-header">
      <h2>充值记录</h2>
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon">
            <i class="bi bi-currency-yen"></i>
          </div>
          <div class="stat-info">
            <div class="stat-label">总充值金额</div>
            <div class="stat-value">¥{{ stats.totalAmount || '0.00' }}</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon success">
            <i class="bi bi-calendar-check"></i>
          </div>
          <div class="stat-info">
            <div class="stat-label">今日充值</div>
            <div class="stat-value">¥{{ stats.todayAmount || '0.00' }}</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon info">
            <i class="bi bi-list-check"></i>
          </div>
          <div class="stat-info">
            <div class="stat-label">充值笔数</div>
            <div class="stat-value">{{ stats.totalCount || 0 }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="table-container">
      <div class="table-header">
        <div class="search-box">
          <input
            type="text"
            class="form-control"
            placeholder="搜索用户名或充值单号"
            v-model="searchKeyword"
            @keyup.enter="handleSearch"
          />
          <button class="btn btn-primary" @click="handleSearch">
            <i class="bi bi-search"></i>
          </button>
        </div>
      </div>

      <div class="table-responsive">
        <table class="table table-hover">
          <thead>
            <tr>
              <th>充值单号</th>
              <th>用户名</th>
              <th>充值金额</th>
              <th>支付方式</th>
              <th>状态</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="records.length === 0">
              <td colspan="6" class="text-center text-muted">暂无记录</td>
            </tr>
            <tr v-for="record in records" :key="record.id">
              <td>{{ record.rechargeNo }}</td>
              <td>{{ record.username || '-' }}</td>
              <td class="amount">¥{{ record.amount }}</td>
              <td>{{ getPaymentMethodText(record.paymentMethod) }}</td>
              <td>
                <span class="status-badge" :class="'status-' + record.status">
                  {{ getStatusText(record.status) }}
                </span>
              </td>
              <td>{{ formatTime(record.createTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-footer">
        <div class="pagination">
          <button
            class="btn btn-outline-primary btn-sm"
            :disabled="currentPage <= 1"
            @click="changePage(currentPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">第 {{ currentPage }} 页</span>
          <button
            class="btn btn-outline-primary btn-sm"
            :disabled="records.length < pageSize"
            @click="changePage(currentPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const records = ref([])
const stats = ref({
  totalAmount: '0.00',
  todayAmount: '0.00',
  totalCount: 0
})

const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

// 获取充值记录
const fetchRecords = async () => {
  try {
    const params = {
      page: currentPage.value,
      keyword: searchKeyword.value || undefined
    }
    const res = await request.get('/admin/api/recharge/list', { params })
    if (res.code === 200 && res.data) {
      records.value = res.data.records || []
    }
  } catch (error) {
    console.error('获取充值记录失败:', error)
  }
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const res = await request.get('/admin/api/recharge/stats')
    if (res.code === 200 && res.data) {
      stats.value = {
        totalAmount: (res.data.totalAmount || 0).toFixed(2),
        todayAmount: (res.data.todayAmount || 0).toFixed(2),
        totalCount: res.data.totalCount || 0
      }
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchRecords()
}

const changePage = (page) => {
  currentPage.value = page
  fetchRecords()
}

const getStatusText = (status) => {
  const statusMap = {
    0: '待支付',
    1: '充值成功',
    2: '充值失败'
  }
  return statusMap[status] || '未知'
}

const getPaymentMethodText = (method) => {
  const methodMap = {
    'WECHAT': '微信支付',
    'ALIPAY': '支付宝',
    'BALANCE': '余额'
  }
  return methodMap[method] || method
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchRecords()
  fetchStats()
})
</script>

<style scoped>
.admin-recharges-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin-bottom: 20px;
  font-weight: 700;
  color: #1e293b;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.stat-icon.success {
  background: linear-gradient(135deg, #10b981, #059669);
}

.stat-icon.info {
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  overflow: hidden;
}

.table-header {
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-box {
  display: flex;
  gap: 8px;
  width: 300px;
}

.search-box input {
  flex: 1;
}

.table-responsive {
  padding: 0 20px;
}

.table th {
  font-weight: 600;
  color: #64748b;
  border-bottom: 2px solid #e2e8f0;
}

.table td {
  vertical-align: middle;
}

.amount {
  font-weight: 600;
  color: #10b981;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-0 { background: #fef3c7; color: #d97706; }
.status-1 { background: #d1fae5; color: #059669; }
.status-2 { background: #fee2e2; color: #dc2626; }

.table-footer {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-info {
  color: #64748b;
  font-size: 14px;
}
</style>
