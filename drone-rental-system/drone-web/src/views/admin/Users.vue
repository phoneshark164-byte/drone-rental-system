<template>
  <div class="admin-users">
    <div class="page-header">
      <h3>用户管理</h3>
      <button class="btn btn-primary" @click="openAddModal">
        <i class="bi bi-plus-lg me-1"></i>添加用户
      </button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-bar">
      <div class="row g-3">
        <div class="col-md-4">
          <input
            v-model="searchKeyword"
            type="text"
            class="form-control"
            placeholder="搜索用户名、手机号"
          />
        </div>
        <div class="col-md-3">
          <select v-model="selectedStatus" class="form-select">
            <option value="">全部状态</option>
            <option value="1">正常</option>
            <option value="0">禁用</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-primary w-100" @click="handleSearch">
            <i class="bi bi-search me-1"></i>搜索
          </button>
        </div>
      </div>
    </div>

    <!-- 用户表格 -->
    <div class="table-container">
      <table class="table table-hover">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>真实姓名</th>
            <th>手机号</th>
            <th>押金</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in paginatedUsers" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.realName || '-' }}</td>
            <td>{{ user.phone || '-' }}</td>
            <td>¥{{ user.deposit }}</td>
            <td>
              <span class="badge" :class="user.status === 1 ? 'bg-success' : 'bg-danger'">
                {{ user.status === 1 ? '正常' : '禁用' }}
              </span>
            </td>
            <td>{{ user.createTime }}</td>
            <td>
              <div class="btn-group">
                <button class="btn btn-sm btn-outline-primary" @click="handleEdit(user)">
                  <i class="bi bi-pencil"></i>
                </button>
                <button
                  class="btn btn-sm"
                  :class="user.status === 1 ? 'btn-outline-danger' : 'btn-outline-success'"
                  @click="handleToggleStatus(user)"
                >
                  <i :class="user.status === 1 ? 'bi bi-lock' : 'bi bi-unlock'"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" @click="handleDelete(user)">
                  <i class="bi bi-trash"></i>
                </button>
              </div>
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
          v-for="page in totalPages"
          :key="page"
          class="page-item"
          :class="{ active: currentPage === page }"
        >
          <a class="page-link" href="#" @click.prevent="goToPage(page)">{{ page }}</a>
        </li>
        <li class="page-item" :class="{ disabled: currentPage === totalPages }">
          <a class="page-link" href="#" @click.prevent="goToPage(currentPage + 1)">下一页</a>
        </li>
      </ul>
    </nav>

    <!-- 添加用户模态框 -->
    <div v-if="showAddModal" class="modal-overlay" @click="cancelAdd">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h5>添加用户</h5>
          <button class="btn-close" @click="cancelAdd">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">用户名 <span class="text-danger">*</span></label>
            <input v-model="addForm.username" type="text" class="form-control" placeholder="请输入用户名" />
          </div>
          <div class="mb-3">
            <label class="form-label">密码 <span class="text-danger">*</span></label>
            <input v-model="addForm.password" type="password" class="form-control" placeholder="请输入密码" />
          </div>
          <div class="mb-3">
            <label class="form-label">真实姓名</label>
            <input v-model="addForm.realName" type="text" class="form-control" placeholder="请输入真实姓名" />
          </div>
          <div class="mb-3">
            <label class="form-label">手机号</label>
            <input v-model="addForm.phone" type="text" class="form-control" placeholder="请输入手机号" />
          </div>
          <div class="mb-3">
            <label class="form-label">押金 (元)</label>
            <input v-model.number="addForm.deposit" type="number" class="form-control" placeholder="0" min="0" step="0.01" />
          </div>
          <div class="mb-3">
            <label class="form-label">状态</label>
            <select v-model="addForm.status" class="form-select">
              <option value="1">正常</option>
              <option value="0">禁用</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="cancelAdd">取消</button>
          <button class="btn btn-primary" @click="saveAdd">添加</button>
        </div>
      </div>
    </div>

    <!-- 编辑用户模态框 -->
    <div v-if="showEditModal" class="modal-overlay" @click="showEditModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h5>编辑用户</h5>
          <button class="btn-close" @click="showEditModal = false">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">用户名</label>
            <input v-model="editForm.username" type="text" class="form-control" disabled />
          </div>
          <div class="mb-3">
            <label class="form-label">真实姓名</label>
            <input v-model="editForm.realName" type="text" class="form-control" />
          </div>
          <div class="mb-3">
            <label class="form-label">手机号</label>
            <input v-model="editForm.phone" type="text" class="form-control" />
          </div>
          <div class="mb-3">
            <label class="form-label">押金 (元)</label>
            <input v-model.number="editForm.deposit" type="number" class="form-control" min="0" step="0.01" />
          </div>
          <div class="mb-3">
            <label class="form-label">状态</label>
            <select v-model="editForm.status" class="form-select">
              <option value="1">正常</option>
              <option value="0">禁用</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showEditModal = false">取消</button>
          <button class="btn btn-primary" @click="saveEdit">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getUserList, createUser, updateUser, deleteUser, updateUserStatus } from '@/api/admin'

const searchKeyword = ref('')
const selectedStatus = ref('')
const currentPage = ref(1)
const pageSize = 5
const showAddModal = ref(false)
const showEditModal = ref(false)

// 从API加载用户数据
const users = ref([])

const loadUsers = async () => {
  try {
    const res = await getUserList({ keyword: searchKeyword.value })
    if (res.code === 200 && res.data) {
      users.value = res.data.map(u => ({
        id: u.id,
        username: u.username,
        realName: u.realName || '',
        phone: u.phone || '',
        deposit: u.deposit || 0,
        status: u.status,
        createTime: formatDateTime(u.createTime)
      }))
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
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

// 组件挂载时加载数据
onMounted(() => {
  // 清除localStorage中的模拟数据
  localStorage.removeItem('adminUsers')
  loadUsers()
})

// 添加用户表单
const addForm = ref({
  username: '',
  password: '',
  realName: '',
  phone: '',
  deposit: 0,
  status: 1
})

// 编辑用户表单
const editForm = ref({
  id: null,
  username: '',
  realName: '',
  phone: '',
  deposit: 0,
  status: 1
})

const filteredUsers = computed(() => {
  let result = users.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(u =>
      u.username.toLowerCase().includes(keyword) ||
      (u.realName && u.realName.toLowerCase().includes(keyword)) ||
      (u.phone && u.phone.includes(keyword))
    )
  }

  if (selectedStatus.value !== '') {
    result = result.filter(u => u.status === parseInt(selectedStatus.value))
  }

  return result
})

// 分页后的数据
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return filteredUsers.value.slice(start, end)
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredUsers.value.length / pageSize)
})

// 切换页码
const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadUsers()
}

// 打开添加模态框
const openAddModal = () => {
  addForm.value = {
    username: '',
    password: '',
    realName: '',
    phone: '',
    deposit: 0,
    status: 1
  }
  showAddModal.value = true
}

// 保存新用户
const saveAdd = async () => {
  if (!addForm.value.username) {
    alert('请输入用户名')
    return
  }
  if (!addForm.value.password) {
    alert('请输入密码')
    return
  }

  try {
    const res = await createUser({
      username: addForm.value.username,
      password: addForm.value.password,
      realName: addForm.value.realName || '',
      phone: addForm.value.phone || '',
      deposit: addForm.value.deposit || 0,
      status: addForm.value.status
    })

    if (res.code === 200) {
      alert('添加成功')
      showAddModal.value = false
      await loadUsers()
    } else {
      alert(res.message || '添加失败')
    }
  } catch (error) {
    console.error('添加用户失败:', error)
    alert('添加失败: ' + (error.response?.data?.message || error.message))
  }
}

// 取消添加
const cancelAdd = () => {
  showAddModal.value = false
}

const handleEdit = (user) => {
  editForm.value = {
    id: user.id,
    username: user.username,
    realName: user.realName || '',
    phone: user.phone || '',
    deposit: user.deposit,
    status: user.status
  }
  showEditModal.value = true
}

// 保存编辑
const saveEdit = async () => {
  try {
    const res = await updateUser(editForm.value.id, {
      realName: editForm.value.realName || '',
      phone: editForm.value.phone || '',
      deposit: editForm.value.deposit || 0,
      status: editForm.value.status
    })

    if (res.code === 200) {
      alert('保存成功')
      showEditModal.value = false
      await loadUsers()
    } else {
      alert(res.message || '保存失败')
    }
  } catch (error) {
    console.error('更新用户失败:', error)
    alert('保存失败: ' + (error.response?.data?.message || error.message))
  }
}

const handleToggleStatus = async (user) => {
  const action = user.status === 1 ? '禁用' : '启用'
  if (confirm(`确定要${action}用户 ${user.username} 吗？`)) {
    try {
      const newStatus = user.status === 1 ? 0 : 1
      const res = await updateUserStatus(user.id, newStatus)
      if (res.code === 200) {
        await loadUsers()
      } else {
        alert(res.message || '操作失败')
      }
    } catch (error) {
      console.error('修改用户状态失败:', error)
      alert('操作失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const handleDelete = async (user) => {
  if (confirm(`确定要删除用户 ${user.username} 吗？`)) {
    try {
      const res = await deleteUser(user.id)
      if (res.code === 200) {
        alert('删除成功')
        await loadUsers()
      } else {
        alert(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除用户失败:', error)
      alert('删除失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

// 监听筛选条件变化，重置到第一页并重新加载数据
watch([searchKeyword, selectedStatus], async () => {
  currentPage.value = 1
  await loadUsers()
})
</script>

<style scoped>
.admin-users {
  position: relative;
  z-index: 1;
}

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
  border-bottom: 2px solid #e2e8f0;
}

.table td {
  vertical-align: middle;
}

.pagination-nav {
  margin-top: 20px;
}

.pagination {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 4px;
}

.page-item {
  display: block;
}

.page-link {
  display: block;
  padding: 8px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #475569;
  text-decoration: none;
  transition: all 0.2s;
  background: white;
  min-width: 40px;
  text-align: center;
}

.page-link:hover {
  background: #f1f5f9;
  color: #2563eb;
  border-color: #bfdbfe;
}

.page-item.active .page-link {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
  border-color: transparent;
}

.page-item.disabled .page-link {
  color: #cbd5e1;
  pointer-events: none;
  background: #f8fafc;
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
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0,0,0,0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h5 {
  margin: 0;
  font-weight: 600;
}

.btn-close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.btn-close:hover {
  color: #1e293b;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #1e293b;
  font-size: 14px;
}

.text-danger {
  color: #ef4444;
}

.form-control, .form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
}

.form-control:focus, .form-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid;
  display: inline-block;
  text-align: center;
}

.btn-primary {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
}

.btn-primary:hover {
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.btn-secondary {
  background: #64748b;
  color: white;
}

.btn-outline-primary {
  background: transparent !important;
  border: 1px solid #3b82f6 !important;
  color: #3b82f6 !important;
}

.btn-outline-primary:hover {
  background: #3b82f6;
  color: white;
}

.btn-outline-danger {
  background: transparent !important;
  border: 1px solid #ef4444 !important;
  color: #ef4444 !important;
}

.btn-outline-danger:hover {
  background: #ef4444;
  color: white;
}

.btn-outline-success {
  background: transparent !important;
  border: 1px solid #10b981 !important;
  color: #10b981 !important;
}

.btn-outline-success:hover {
  background: #10b981;
  color: white;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.btn-group {
  display: flex;
  gap: 6px;
}

.me-1 {
  margin-right: 4px;
}

.mb-3 {
  margin-bottom: 16px;
}

.row {
  display: flex;
  flex-wrap: wrap;
  margin: -8px;
}

.g-3 > * {
  padding: 8px;
}

.col-md-4 {
  flex: 0 0 33.333333%;
  max-width: 33.333333%;
  padding: 8px;
}

.col-md-3 {
  flex: 0 0 25%;
  max-width: 25%;
  padding: 8px;
}

.col-md-2 {
  flex: 0 0 16.666667%;
  max-width: 16.666667%;
  padding: 8px;
}

.w-100 {
  width: 100%;
}

.justify-content-center {
  justify-content: center;
}
</style>
