<template>
  <div class="admin-logs">
    <div class="page-header">
      <h3>操作日志</h3>
      <button class="btn btn-outline-danger" @click="handleClearLogs">
        <i class="bi bi-trash me-1"></i>清空日志
      </button>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <div class="row g-3">
        <div class="col-md-3">
          <input
            v-model="searchKeyword"
            type="text"
            class="form-control"
            placeholder="搜索操作者、操作内容"
          />
        </div>
        <div class="col-md-2">
          <select v-model="selectedType" class="form-select">
            <option value="">全部类型</option>
            <option value="USER">用户</option>
            <option value="OPERATOR">运营方</option>
            <option value="ADMIN">管理员</option>
          </select>
        </div>
        <div class="col-md-2">
          <select v-model="selectedStatus" class="form-select">
            <option value="">全部状态</option>
            <option value="1">成功</option>
            <option value="0">失败</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-primary w-100" @click="handleSearch">
            <i class="bi bi-search me-1"></i>搜索
          </button>
        </div>
      </div>
    </div>

    <!-- 日志表格 -->
    <div class="table-container">
      <table class="table table-hover">
        <thead>
          <tr>
            <th>ID</th>
            <th>操作者</th>
            <th>操作类型</th>
            <th>请求方法</th>
            <th>IP地址</th>
            <th>状态</th>
            <th>执行时长</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in paginatedLogs" :key="log.id">
            <td>{{ log.id }}</td>
            <td>
              <div class="operator-info">
                <span class="operator-type" :class="'type-' + log.operatorType">
                  {{ log.operatorType }}
                </span>
                <span>{{ log.operatorName || '-' }}</span>
              </div>
            </td>
            <td>{{ log.operation }}</td>
            <td class="method-cell">{{ log.method }}</td>
            <td>
              <div>
                <div>{{ log.ip }}</div>
                <small class="text-muted">{{ log.location }}</small>
              </div>
            </td>
            <td>
              <span class="badge" :class="log.status === 1 ? 'bg-success' : 'bg-danger'">
                {{ log.status === 1 ? '成功' : '失败' }}
              </span>
            </td>
            <td>{{ log.executeTime }}ms</td>
            <td>{{ log.createTime }}</td>
            <td>
              <button class="btn btn-sm btn-outline-danger" @click="handleDeleteLog(log)">
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
        <li v-for="page in displayedPages" :key="page" class="page-item" :class="{ active: page === currentPage }">
          <a class="page-link" href="#" @click.prevent="goToPage(page)">{{ page }}</a>
        </li>
        <li class="page-item" :class="{ disabled: currentPage === totalPages }">
          <a class="page-link" href="#" @click.prevent="goToPage(currentPage + 1)">下一页</a>
        </li>
      </ul>
      <div class="pagination-info">
        <span>共 {{ filteredLogs.length }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页</span>
      </div>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getLogList, clearLogs, deleteLog } from '@/api/admin'

const searchKeyword = ref('')
const selectedType = ref('')
const selectedStatus = ref('')

const logs = ref([])
const currentPage = ref(1)
const pageSize = ref(10)

const STATUS_MAP = {
  0: '失败',
  1: '成功'
}

const TYPE_MAP = {
  'USER': '用户',
  'OPERATOR': '运营方',
  'ADMIN': '管理员'
}

// 计算总页数
const totalPages = computed(() => {
  return Math.ceil(filteredLogs.value.length / pageSize.value) || 1
})

// 显示的页码范围
const displayedPages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value

  // 始终显示第一页
  pages.push(1)

  // 如果当前页距离第一页超过2，显示省略号
  if (current > 3) {
    pages.push('...')
  }

  // 显示当前页附近的页码
  for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) {
    pages.push(i)
  }

  // 如果当前页距离最后一页超过2，显示省略号
  if (current < total - 2) {
    pages.push('...')
  }

  // 始终显示最后一页（如果总页数大于1）
  if (total > 1) {
    pages.push(total)
  }

  return pages
})

// 当前页的日志
const paginatedLogs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredLogs.value.slice(start, end)
})

// 跳转到指定页
const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 监听筛选条件变化，重置到第一页
watch([searchKeyword, selectedType, selectedStatus], () => {
  currentPage.value = 1
})

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 加载日志数据
const loadLogs = async () => {
  try {
    const params = {}
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (selectedType.value) {
      params.operatorType = selectedType.value
    }
    if (selectedStatus.value !== '') {
      params.status = parseInt(selectedStatus.value)
    }

    const res = await getLogList(params)
    if (res.code === 200 && res.data) {
      logs.value = res.data.map(l => ({
        id: l.id,
        operatorType: l.operatorType,
        operatorId: l.operatorId,
        operatorName: l.operatorName,
        operation: l.operation,
        method: l.method,
        ip: l.ip,
        location: l.location,
        status: l.status,
        executeTime: l.executeTime,
        createTime: formatDateTime(l.createTime),
        errorMsg: l.errorMsg
      }))
    }
  } catch (error) {
    console.error('加载日志列表失败:', error)
  }
}

// 组件挂载时加载数据
onMounted(() => {
  localStorage.removeItem('adminLogs')
  loadLogs()
})

const filteredLogs = computed(() => {
  let result = logs.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(l =>
      l.operatorName.toLowerCase().includes(keyword) ||
      l.operation.toLowerCase().includes(keyword)
    )
  }

  if (selectedType.value) {
    result = result.filter(l => l.operatorType === selectedType.value)
  }

  if (selectedStatus.value !== '') {
    result = result.filter(l => l.status === parseInt(selectedStatus.value))
  }

  return result
})

const handleSearch = async () => {
  await loadLogs()
}

const handleDeleteLog = async (log) => {
  if (confirm(`确定要删除这条日志吗？`)) {
    try {
      const res = await deleteLog(log.id)
      if (res.code === 200) {
        alert('删除成功')
        await loadLogs()
      } else {
        alert(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除日志失败:', error)
      alert('删除失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const handleClearLogs = async () => {
  if (confirm('确定要清空所有日志吗？此操作不可恢复！')) {
    try {
      const res = await clearLogs()
      if (res.code === 200) {
        alert('清空成功')
        logs.value = []
      } else {
        alert(res.message || '清空失败')
      }
    } catch (error) {
      console.error('清空日志失败:', error)
      alert('清空失败: ' + (error.response?.data?.message || error.message))
    }
  }
}
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

.operator-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.operator-type {
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.type-USER { background: #dbeafe; color: #2563eb; }
.type-OPERATOR { background: #fef3c7; color: #d97706; }
.type-ADMIN { background: #fee2e2; color: #dc2626; }

.method-cell {
  font-family: monospace;
  font-size: 13px;
  color: #64748b;
}

.pagination-nav {
  margin-top: 20px;
}

.pagination-info {
  text-align: center;
  margin-top: 10px;
  color: #64748b;
  font-size: 14px;
}

.pagination .page-item {
  cursor: pointer;
}

.pagination .page-item.disabled {
  cursor: not-allowed;
}

.pagination .page-link {
  cursor: pointer;
}

.pagination .page-item.disabled .page-link {
  cursor: not-allowed;
}
</style>
