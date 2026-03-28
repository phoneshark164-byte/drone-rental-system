<template>
  <div class="admin-notifications">
    <div class="page-header">
      <h2>系统通知</h2>
      <button class="btn btn-primary btn-sm" @click="markAllAsRead" v-if="notifications.length > 0">
        <i class="bi bi-check2-all me-1"></i>全部已读
      </button>
    </div>

    <!-- 通知统计 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon bg-primary">
          <i class="bi bi-bell"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ unreadCount }}</div>
          <div class="stat-label">未读通知</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon bg-success">
          <i class="bi bi-check-circle"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ notifications.length }}</div>
          <div class="stat-label">总通知数</div>
        </div>
      </div>
    </div>

    <!-- 通知列表 -->
    <div class="notifications-list">
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border" role="status"></div>
      </div>
      <div v-else-if="notifications.length === 0" class="empty-state">
        <i class="bi bi-bell-slash"></i>
        <p>暂无通知</p>
      </div>
      <div v-else>
        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item"
          :class="{ unread: notification.isRead === 0 }"
          @click="markAsRead(notification)"
        >
          <div class="notification-icon" :class="getTypeClass(notification.type)">
            <i :class="getTypeIcon(notification.type)"></i>
          </div>
          <div class="notification-content">
            <div class="notification-header">
              <h5 class="notification-title">{{ notification.title }}</h5>
              <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
            </div>
            <p class="notification-text">{{ notification.content }}</p>
          </div>
          <div class="notification-actions">
            <button
              v-if="notification.isRead === 0"
              class="btn btn-sm btn-outline-primary"
              @click.stop="markAsRead(notification)"
            >
              标记已读
            </button>
            <button
              class="btn btn-sm btn-outline-danger"
              @click.stop="deleteNotification(notification.id)"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const notifications = ref([])
const unreadCount = ref(0)
const loading = ref(false)

const loadNotifications = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/api/notification/list')
    if (res.code === 200) {
      notifications.value = res.data || []
      unreadCount.value = notifications.value.filter(n => n.isRead === 0).length
    }
  } catch (e) {
    console.error('加载通知失败', e)
  } finally {
    loading.value = false
  }
}

const markAsRead = async (notification) => {
  if (notification.isRead === 1) return
  try {
    const res = await request.post(`/admin/api/notification/${notification.id}/read`)
    if (res.code === 200) {
      notification.isRead = 1
      notification.readTime = new Date().toISOString()
      unreadCount.value = notifications.value.filter(n => n.isRead === 0).length
      // 通知侧边栏更新未读数量
      window.dispatchEvent(new CustomEvent('admin-notification-updated'))
    }
  } catch (e) {
    console.error('标记已读失败', e)
  }
}

const markAllAsRead = async () => {
  try {
    const res = await request.post('/admin/api/notification/read-all')
    if (res.code === 200) {
      notifications.value.forEach(n => {
        n.isRead = 1
      })
      unreadCount.value = 0
      // 通知侧边栏更新未读数量
      window.dispatchEvent(new CustomEvent('admin-notification-updated'))
    }
  } catch (e) {
    console.error('全部已读失败', e)
  }
}

const deleteNotification = async (id) => {
  if (!confirm('确定要删除这条通知吗？')) return
  try {
    const res = await request.delete(`/admin/api/notification/${id}`)
    if (res.code === 200) {
      notifications.value = notifications.value.filter(n => n.id !== id)
      unreadCount.value = notifications.value.filter(n => n.isRead === 0).length
      // 通知侧边栏更新未读数量
      window.dispatchEvent(new CustomEvent('admin-notification-updated'))
    }
  } catch (e) {
    console.error('删除通知失败', e)
  }
}

const getTypeClass = (type) => {
  const classes = {
    info: 'bg-info',
    success: 'bg-success',
    warning: 'bg-warning',
    danger: 'bg-danger'
  }
  return classes[type] || 'bg-info'
}

const getTypeIcon = (type) => {
  const icons = {
    info: 'bi bi-info-circle',
    success: 'bi bi-check-circle',
    warning: 'bi bi-exclamation-triangle',
    danger: 'bi bi-x-circle'
  }
  return icons[type] || 'bi bi-bell'
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

onMounted(() => {
  loadNotifications()
  // 每30秒刷新一次未读数量
  setInterval(() => {
    loadNotifications()
  }, 30000)
})
</script>

<style scoped>
.admin-notifications {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-weight: 700;
}

.stats-row {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  flex: 1;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-icon.bg-primary { background: linear-gradient(135deg, #3b82f6, #06b6d4); }
.stat-icon.bg-success { background: linear-gradient(135deg, #10b981, #059669); }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
}

.notifications-list {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.notification-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
  cursor: pointer;
  transition: background 0.2s;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: #f8fafc;
}

.notification-item.unread {
  background: #f0f9ff;
}

.notification-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notification-icon.bg-info { background: #dbeafe; color: #3b82f6; }
.notification-icon.bg-success { background: #d1fae5; color: #10b981; }
.notification-icon.bg-warning { background: #fef3c7; color: #f59e0b; }
.notification-icon.bg-danger { background: #fee2e2; color: #ef4444; }

.notification-content {
  flex: 1;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.notification-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.notification-time {
  font-size: 12px;
  color: #94a3b8;
}

.notification-text {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.notification-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-state i {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
}
</style>
