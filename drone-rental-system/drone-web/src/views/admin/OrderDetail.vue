<template>
  <div class="admin-order-detail">
    <div class="page-header">
      <button class="btn btn-outline-secondary" @click="$router.go(-1)">
        <i class="bi bi-arrow-left me-1"></i>返回
      </button>
      <h3>订单详情</h3>
    </div>

    <div v-if="order" class="order-detail">
      <!-- 订单状态卡片 -->
      <div class="status-card">
        <div class="status-header">
          <h4>订单状态</h4>
          <span class="status-badge" :class="'status-' + order.status">
            {{ getStatusText(order.status) }}
          </span>
        </div>
        <div class="status-timeline">
          <div class="timeline-item" :class="{ active: order.status >= 0 }">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <h6>待支付</h6>
              <p>{{ order.createTime }}</p>
            </div>
          </div>
          <div class="timeline-item" :class="{ active: order.status >= 1 }">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <h6>已支付</h6>
              <p>{{ order.payTime || '-' }}</p>
            </div>
          </div>
          <div class="timeline-item" :class="{ active: order.status >= 2 }">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <h6>使用中</h6>
              <p>{{ order.startTime }}</p>
            </div>
          </div>
          <div class="timeline-item" :class="{ active: order.status >= 3 }">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <h6>已完成</h6>
              <p>{{ order.endTime || '-' }}</p>
            </div>
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
            <span class="label">用户：</span>
            <span class="value">{{ order.username }}</span>
          </div>
          <div class="info-item">
            <span class="label">创建时间：</span>
            <span class="value">{{ order.createTime }}</span>
          </div>
          <div class="info-item">
            <span class="label">支付时间：</span>
            <span class="value">{{ order.payTime || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">计划时长：</span>
            <span class="value">{{ order.plannedDuration }}分钟</span>
          </div>
          <div class="info-item">
            <span class="label">实际时长：</span>
            <span class="value">{{ order.actualDuration || '-' }}分钟</span>
          </div>
          <div class="info-item">
            <span class="label">订单金额：</span>
            <span class="value amount">¥{{ order.amount }}</span>
          </div>
          <div class="info-item" v-if="order.cancelReason">
            <span class="label">取消原因：</span>
            <span class="value">{{ order.cancelReason }}</span>
          </div>
        </div>
      </div>

      <!-- 无人机信息 -->
      <div class="info-card">
        <h4 class="card-title">无人机信息</h4>
        <div class="drone-detail">
          <img :src="order.droneImage" class="drone-image" />
          <div class="drone-info">
            <div class="info-item">
              <span class="label">编号：</span>
              <span class="value">{{ order.droneNo }}</span>
            </div>
            <div class="info-item">
              <span class="label">品牌型号：</span>
              <span class="value">{{ order.droneBrand }} {{ order.droneModel }}</span>
            </div>
            <div class="info-item">
              <span class="label">起点位置：</span>
              <span class="value">{{ order.startLocation }}</span>
            </div>
            <div class="info-item" v-if="order.endLocation">
              <span class="label">终点位置：</span>
              <span class="value">{{ order.endLocation }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-card">
        <h4 class="card-title">订单操作</h4>
        <div class="action-buttons">
          <button
            v-if="order.status === 0"
            class="btn btn-danger"
            @click="handleCancel"
          >
            <i class="bi bi-x-circle me-1"></i>取消订单
          </button>
          <button
            v-if="order.status === 2"
            class="btn btn-warning"
            @click="handleForceEnd"
          >
            <i class="bi bi-stop-circle me-1"></i>强制结束
          </button>
          <button class="btn btn-outline-primary" @click="handleRefresh">
            <i class="bi bi-arrow-clockwise me-1"></i>刷新
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const order = ref(null)

const getStatusText = (status) => {
  const statusMap = {
    0: '待支付',
    1: '已支付',
    2: '使用中',
    3: '已完成',
    4: '已取消'
  }
  return statusMap[status] || '未知'
}

const handleCancel = () => {
  if (confirm('确定要取消此订单吗？')) {
    alert('订单已取消')
    order.value.status = 4
  }
}

const handleForceEnd = () => {
  if (confirm('确定要强制结束此订单吗？')) {
    alert('订单已结束')
    order.value.status = 3
  }
}

const handleRefresh = () => {
  // TODO: 刷新订单数据
  alert('刷新成功')
}

onMounted(() => {
  const id = route.params.id
  // TODO: 从API加载订单详情
  // 模拟数据
  order.value = {
    id: parseInt(id),
    orderNo: 'DR20260317001',
    username: '张三',
    droneNo: 'DRONE-A001',
    droneBrand: '大疆',
    droneModel: 'Mavic 3',
    droneImage: '/img/train/0001.jpg',
    startTime: '2026-03-17 14:00',
    endTime: '2026-03-17 15:00',
    plannedDuration: 60,
    actualDuration: 60,
    amount: '30.00',
    status: 3,
    createTime: '2026-03-17 13:50',
    payTime: '2026-03-17 13:52',
    startLocation: '北京市朝阳区三里屯',
    endLocation: '北京市朝阳区国贸'
  }
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h3 {
  font-weight: 600;
  margin: 0;
}

.order-detail {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.status-card,
.info-card,
.action-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.status-card {
  grid-column: 1 / -1;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.status-header h4 {
  font-weight: 600;
  margin: 0;
}

.status-badge {
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.status-0 { background: #fef3c7; color: #d97706; }
.status-1 { background: #dbeafe; color: #2563eb; }
.status-2 { background: #d1fae5; color: #059669; }
.status-3 { background: #f1f5f9; color: #64748b; }
.status-4 { background: #fee2e2; color: #dc2626; }

.status-timeline {
  display: flex;
  justify-content: space-between;
  position: relative;
}

.status-timeline::before {
  content: '';
  position: absolute;
  top: 16px;
  left: 50px;
  right: 50px;
  height: 2px;
  background: #e2e8f0;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 1;
}

.timeline-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e2e8f0;
  border: 3px solid white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.timeline-item.active .timeline-dot {
  background: var(--primary-color);
}

.timeline-content {
  text-align: center;
  margin-top: 12px;
}

.timeline-content h6 {
  font-weight: 600;
  margin-bottom: 4px;
}

.timeline-content p {
  color: #64748b;
  font-size: 12px;
  margin: 0;
}

.card-title {
  font-weight: 600;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
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

.drone-detail {
  display: flex;
  gap: 20px;
}

.drone-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 12px;
}

.drone-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .order-detail {
    grid-template-columns: 1fr;
  }
}
</style>
