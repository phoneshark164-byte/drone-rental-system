<template>
  <div class="admin-orders">
    <div class="page-header">
      <h3>订单管理</h3>
      <div class="header-actions">
        <button class="btn btn-outline-primary" @click="handleExport">
          <i class="bi bi-download me-1"></i>导出
        </button>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-bar">
      <div class="row g-3">
        <div class="col-md-3">
          <input
            v-model="searchKeyword"
            type="text"
            class="form-control"
            placeholder="搜索订单号、用户"
          />
        </div>
        <div class="col-md-2">
          <select v-model="selectedStatus" class="form-select">
            <option value="">全部状态</option>
            <option value="0">待支付</option>
            <option value="1">已支付</option>
            <option value="2">使用中</option>
            <option value="3">已完成</option>
            <option value="4">已取消</option>
          </select>
        </div>
        <div class="col-md-3">
          <input v-model="dateRange" type="text" class="form-control" placeholder="选择日期范围" />
        </div>
        <div class="col-md-2">
          <button class="btn btn-primary w-100" @click="handleSearch">
            <i class="bi bi-search me-1"></i>搜索
          </button>
        </div>
      </div>
    </div>

    <!-- 订单表格 -->
    <div class="table-container">
      <table class="table table-hover">
        <thead>
          <tr>
            <th>订单号</th>
            <th>用户</th>
            <th>无人机</th>
            <th>开始时间</th>
            <th>时长</th>
            <th>金额</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in filteredOrders" :key="order.id">
            <td>{{ order.orderNo }}</td>
            <td>{{ order.username }}</td>
            <td>{{ order.droneModel }}</td>
            <td>{{ order.startTime }}</td>
            <td>{{ order.duration }}分钟</td>
            <td>¥{{ order.amount }}</td>
            <td>
              <span class="badge" :class="getStatusBadgeClass(order.status)">
                {{ order.statusText }}
              </span>
            </td>
            <td>
              <button class="btn btn-sm btn-outline-primary" @click="handleViewDetail(order)">
                <i class="bi bi-eye"></i>详情
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <nav class="pagination-nav">
      <ul class="pagination justify-content-center">
        <li class="page-item disabled">
          <a class="page-link" href="#">上一页</a>
        </li>
        <li class="page-item active">
          <a class="page-link" href="#">1</a>
        </li>
        <li class="page-item">
          <a class="page-link" href="#">2</a>
        </li>
        <li class="page-item">
          <a class="page-link" href="#">3</a>
        </li>
        <li class="page-item">
          <a class="page-link" href="#">下一页</a>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList } from '@/api/admin'

const router = useRouter()

const searchKeyword = ref('')
const selectedStatus = ref('')
const dateRange = ref('')

const orders = ref([])

const STATUS_MAP = {
  0: '待支付',
  1: '已支付',
  2: '使用中',
  3: '已完成',
  4: '已取消'
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 加载订单数据
const loadOrders = async () => {
  try {
    const params = {}
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (selectedStatus.value !== '') {
      params.status = parseInt(selectedStatus.value)
    }

    const res = await getOrderList(params)
    if (res.code === 200 && res.data) {
      orders.value = res.data.map(o => ({
        id: o.id,
        orderNo: o.orderNo,
        username: o.username,
        droneModel: o.droneModel,
        droneBrand: o.droneBrand,
        startTime: formatDateTime(o.startTime || o.createTime),
        duration: o.plannedDuration || 0,
        amount: o.amount || '0.00',
        status: o.status,
        statusText: STATUS_MAP[o.status] || '未知'
      }))
    }
  } catch (error) {
    console.error('加载订单列表失败:', error)
  }
}

// 组件挂载时加载数据
onMounted(() => {
  localStorage.removeItem('adminOrders')
  loadOrders()
})

const filteredOrders = computed(() => {
  let result = orders.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(o =>
      o.orderNo.toLowerCase().includes(keyword) ||
      o.username.includes(keyword)
    )
  }

  if (selectedStatus.value !== '') {
    result = result.filter(o => o.status === parseInt(selectedStatus.value))
  }

  return result
})

const getStatusBadgeClass = (status) => {
  const classMap = {
    0: 'bg-warning',
    1: 'bg-info',
    2: 'bg-primary',
    3: 'bg-success',
    4: 'bg-secondary'
  }
  return classMap[status] || 'bg-secondary'
}

const handleSearch = async () => {
  await loadOrders()
}

const handleExport = () => {
  alert('导出订单数据')
}

const handleViewDetail = (order) => {
  router.push(`/admin/order/${order.id}`)
}

// 监听筛选条件变化，重新加载数据
watch([searchKeyword, selectedStatus], () => {
  loadOrders()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h3 {
  font-weight: 600;
  margin: 0;
}

.filter-bar {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.table-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  overflow-x: auto;
}

.table th {
  font-weight: 600;
  color: #64748b;
  font-size: 13px;
  text-transform: uppercase;
}

.table td {
  vertical-align: middle;
}

.pagination-nav {
  margin-top: 20px;
}
</style>
