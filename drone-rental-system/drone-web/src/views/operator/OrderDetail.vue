<template>
  <div class="operator-order-detail">
    <div class="page-header">
      <button class="btn btn-outline-secondary" @click="$router.go(-1)">
        <i class="bi bi-arrow-left me-1"></i>返回
      </button>
      <h3>订单详情</h3>
    </div>

    <div v-if="order" class="order-detail">
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
            <span class="label">订单状态：</span>
            <span class="status-badge" :class="'status-' + order.status">
              {{ getStatusText(order.status) }}
            </span>
          </div>
          <div class="info-item">
            <span class="label">订单金额：</span>
            <span class="value amount">¥{{ order.amount }}</span>
          </div>
          <div class="info-item">
            <span class="label">开始时间：</span>
            <span class="value">{{ order.startTime }}</span>
          </div>
          <div class="info-item">
            <span class="label">结束时间：</span>
            <span class="value">{{ order.endTime || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">计划时长：</span>
            <span class="value">{{ order.plannedDuration }}分钟</span>
          </div>
          <div class="info-item">
            <span class="label">实际时长：</span>
            <span class="value">{{ order.actualDuration || '-' }}分钟</span>
          </div>
        </div>
      </div>

      <!-- 无人机信息 -->
      <div class="info-card">
        <h4 class="card-title">无人机信息</h4>
        <div class="drone-detail">
          <img :src="order.droneImage" class="drone-image" />
          <div class="drone-info">
            <div class="info-item-full">
              <span class="label">编号：</span>
              <span class="value">{{ order.droneNo }}</span>
            </div>
            <div class="info-item-full">
              <span class="label">品牌型号：</span>
              <span class="value">{{ order.droneBrand }} {{ order.droneModel }}</span>
            </div>
            <div class="info-item-full">
              <span class="label">起点位置：</span>
              <span class="value">{{ order.startLocation }}</span>
            </div>
            <div class="info-item-full" v-if="order.endLocation">
              <span class="label">终点位置：</span>
              <span class="value">{{ order.endLocation }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 位置信息 -->
      <div class="info-card" v-if="order.startLatitude">
        <h4 class="card-title">位置信息</h4>
        <div class="map-container">
          <div class="location-item">
            <i class="bi bi-geo-alt text-success"></i>
            <div>
              <strong>起点</strong>
              <p>{{ order.startLocation }}</p>
              <small class="text-muted">{{ order.startLatitude }}, {{ order.startLongitude }}</small>
            </div>
          </div>
          <div class="location-item" v-if="order.endLatitude">
            <i class="bi bi-geo-alt text-danger"></i>
            <div>
              <strong>终点</strong>
              <p>{{ order.endLocation }}</p>
              <small class="text-muted">{{ order.endLatitude }}, {{ order.endLongitude }}</small>
            </div>
          </div>
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

onMounted(() => {
  const id = route.params.id
  // TODO: 从API加载订单详情
  // 模拟数据
  order.value = {
    id: parseInt(id),
    orderNo: 'DR20260317001',
    username: '张三',
    droneNo: 'DRONE-E001',
    droneBrand: '道通',
    droneModel: 'EVO II',
    droneImage: '/img/train/0005.jpg',
    startTime: '2026-03-17 14:00',
    endTime: '2026-03-17 15:00',
    plannedDuration: 60,
    actualDuration: 60,
    amount: '30.00',
    status: 3,
    startLocation: '四川大学锦江学院',
    endLocation: '西青区大学城',
    startLatitude: '39.0842',
    startLongitude: '117.2009',
    endLatitude: '39.0755',
    endLongitude: '117.0492'
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

.info-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.card-title {
  font-weight: 600;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
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
  color: #059669;
  font-size: 18px;
}

.status-badge {
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  display: inline-block;
}

.status-0 { background: #fef3c7; color: #d97706; }
.status-1 { background: #dbeafe; color: #2563eb; }
.status-2 { background: #d1fae5; color: #059669; }
.status-3 { background: #f1f5f9; color: #64748b; }
.status-4 { background: #fee2e2; color: #dc2626; }

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

.info-item-full {
  display: flex;
  gap: 8px;
}

.info-item-full .label {
  color: #64748b;
  min-width: 80px;
}

.map-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.location-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.location-item i {
  font-size: 24px;
}

.location-item strong {
  display: block;
  margin-bottom: 4px;
}

.location-item p {
  margin: 0 0 4px 0;
}

.location-item small {
  margin: 0;
}
</style>
