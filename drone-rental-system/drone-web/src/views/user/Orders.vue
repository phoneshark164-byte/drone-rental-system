<template>
  <div class="user-orders-page">
    <!-- 用户头部导航 -->
    <UserHeader />

    <div class="container py-5">
      <div class="page-header d-flex justify-content-between align-items-center mb-4">
        <h2 class="page-title mb-0">我的订单</h2>
        <router-link to="/" class="btn btn-outline-primary">
          <i class="bi bi-house me-1"></i>返回首页
        </router-link>
      </div>

      <div class="order-filters mb-4">
        <div class="btn-group" role="group">
          <button
            v-for="tab in orderTabs"
            :key="tab.value"
            class="btn"
            :class="currentTab === tab.value ? 'btn-primary' : 'btn-outline-primary'"
            @click="currentTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div v-if="filteredOrders.length > 0" class="order-list">
        <div
          v-for="order in filteredOrders"
          :key="order.id"
          class="order-card"
          @click="goToDetail(order.id)"
        >
          <div class="order-header">
            <div class="order-no">
              <span class="label">订单编号：</span>{{ order.orderNo }}
            </div>
            <span class="order-status" :class="'status-' + order.status">
              {{ getStatusText(order.status) }}
            </span>
          </div>
          <div class="order-body">
            <div class="drone-info">
              <img :src="order.droneImage" :alt="order.droneModel" class="drone-thumb" />
              <div class="drone-details">
                <h5>{{ order.droneModel }}</h5>
                <p class="text-muted">{{ order.droneNo }}</p>
              </div>
            </div>
            <div class="order-info">
              <div class="info-row">
                <span class="label">开始时间：</span>
                <span>{{ order.startTime }}</span>
              </div>
              <div class="info-row">
                <span class="label">租用时长：</span>
                <span>{{ order.duration }}分钟</span>
              </div>
            </div>
            <div class="order-amount">
              <span class="amount">¥{{ order.amount }}</span>
            </div>
          </div>
          <div class="order-footer">
            <span class="create-time">{{ order.createTime }}</span>
            <div class="order-actions">
              <button
                v-if="order.status === 0"
                class="btn btn-primary btn-sm me-2"
                @click.stop="handlePay(order)"
              >
                <i class="bi bi-wallet2 me-1"></i>余额支付
              </button>
              <button class="btn btn-outline-primary btn-sm" @click.stop="goToDetail(order.id)">
                查看详情 <i class="bi bi-arrow-right ms-1"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <i class="bi bi-inbox"></i>
        <p>暂无订单</p>
        <router-link to="/user/vehicles" class="btn btn-primary">
          <i class="bi bi-airplane me-2"></i>去租赁无人机
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import UserHeader from '@/components/UserHeader.vue'
import { getOrderList } from '@/api/user'

const router = useRouter()

const currentTab = ref('all')
const orderTabs = [
  { label: '全部', value: 'all' },
  { label: '待支付', value: '0' },
  { label: '使用中', value: '2' },
  { label: '已完成', value: '3' },
  { label: '已取消', value: '4' }
]

const orders = ref([])
const loading = ref(false)

// 从API加载订单列表
const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList({})
    if (res.code === 200 && res.data) {
      orders.value = res.data.map(order => ({
        id: order.id,
        orderNo: order.orderNo,
        droneId: order.vehicleId,
        droneNo: order.droneNo || 'DRONE-' + order.vehicleId,
        droneModel: order.droneModel || '无人机',
        droneImage: order.droneImage || '/img/train/0001.jpg',
        startTime: formatDateTime(order.startTime || order.createTime),
        duration: order.plannedDuration || 60,
        amount: order.amount || '0.00',
        status: order.status,
        createTime: formatDateTime(order.createTime)
      }))
    }
  } catch (error) {
    console.error('加载订单失败:', error)
  } finally {
    loading.value = false
  }
}

// 格式化日期时间
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

onMounted(() => {
  loadOrders()
})

const filteredOrders = computed(() => {
  if (currentTab.value === 'all') {
    // 全部：排除已取消的订单
    return orders.value.filter(order => order.status !== 4)
  }
  if (currentTab.value === '4') {
    // 已取消：只显示已取消的订单
    return orders.value.filter(order => order.status === 4)
  }
  // 其他标签：显示对应状态的订单（也排除已取消的）
  return orders.value.filter(order => order.status === parseInt(currentTab.value))
})

const getStatusText = (status) => {
  const statusMap = {
    0: '待支付',
    1: '已支付',
    2: '使用中',
    3: '已完成',
    4: '已取消',
    5: '退款中',
    6: '已退款'
  }
  return statusMap[status] || '未知'
}

const goToDetail = (id) => {
  router.push(`/user/order/${id}`)
}

// 余额支付订单
const handlePay = (order) => {
  if (window.showPaymentProgress) {
    window.showPaymentProgress(parseFloat(order.amount), 'order', order.id)
  } else {
    alert('支付功能加载中，请刷新页面重试')
  }
}
</script>

<style scoped>
.page-title {
  font-weight: 700;
  margin-bottom: 30px;
  color: #1e293b;
}

.order-filters .btn-group {
  flex-wrap: wrap;
}

.order-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: all 0.3s;
}

.order-card:hover {
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 15px;
  border-bottom: 1px solid #e2e8f0;
}

.order-no {
  font-size: 14px;
  color: #64748b;
}

.order-no .label {
  font-weight: 500;
}

.order-status {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.status-0 { background: #fef3c7; color: #d97706; }
.status-1 { background: #dbeafe; color: #2563eb; }
.status-2 { background: #d1fae5; color: #059669; }
.status-3 { background: #f1f5f9; color: #64748b; }
.status-4 { background: #fee2e2; color: #dc2626; }

.order-body {
  display: flex;
  padding: 20px 0;
  gap: 30px;
}

.drone-info {
  display: flex;
  gap: 15px;
  flex: 1;
}

.drone-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 12px;
}

.drone-details h5 {
  margin: 0 0 5px 0;
  font-weight: 600;
}

.drone-details p {
  margin: 0;
  font-size: 13px;
}

.order-info {
  flex: 1;
}

.info-row {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-row .label {
  color: #64748b;
  width: 80px;
}

.order-amount {
  text-align: right;
}

.order-amount .amount {
  font-size: 24px;
  font-weight: 700;
  color: var(--primary-color);
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #e2e8f0;
}

.create-time {
  font-size: 13px;
  color: #94a3b8;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #94a3b8;
}

.empty-state i {
  font-size: 64px;
  display: block;
  margin-bottom: 20px;
}

.empty-state p {
  font-size: 18px;
  margin-bottom: 30px;
}

.image-placeholder {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
