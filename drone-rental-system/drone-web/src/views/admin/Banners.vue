<template>
  <div class="admin-banners">
    <div class="page-header">
      <h3>轮播图管理</h3>
      <button class="btn btn-primary" @click="showAddModal = true">
        <i class="bi bi-plus-lg me-1"></i>新增轮播图
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
            placeholder="搜索标题"
          />
        </div>
        <div class="col-md-2">
          <select v-model="selectedStatus" class="form-select">
            <option value="">全部状态</option>
            <option value="1">启用</option>
            <option value="0">禁用</option>
          </select>
        </div>
        <div class="col-md-2">
          <button class="btn btn-primary w-100" @click="loadBanners">
            <i class="bi bi-search me-1"></i>搜索
          </button>
        </div>
      </div>
    </div>

    <!-- 轮播图列表 -->
    <div class="banners-table-wrapper">
      <table class="table table-hover">
        <thead>
          <tr>
            <th width="80">排序</th>
            <th width="150">图片</th>
            <th>标题</th>
            <th>副标题</th>
            <th width="80">状态</th>
            <th width="200">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="banner in banners" :key="banner.id">
            <td>
              <input
                type="number"
                class="form-control form-control-sm"
                :value="banner.sortOrder"
                @change="updateSortOrder(banner, $event.target.value)"
                style="width: 60px"
              />
            </td>
            <td>
              <img :src="banner.imageUrl" class="banner-thumb" />
            </td>
            <td>{{ banner.title }}</td>
            <td class="text-muted">{{ banner.subtitle || '-' }}</td>
            <td>
              <span
                class="status-badge"
                :class="banner.status === 1 ? 'active' : 'inactive'"
                @click="toggleStatus(banner)"
              >
                {{ banner.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td>
              <button class="btn btn-sm btn-outline-primary" @click="editBanner(banner)">
                <i class="bi bi-pencil"></i>
              </button>
              <button class="btn btn-sm btn-outline-danger" @click="deleteBannerConfirm(banner)">
                <i class="bi bi-trash"></i>
              </button>
            </td>
          </tr>
          <tr v-if="banners.length === 0">
            <td colspan="6" class="text-center text-muted py-4">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑模态框 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h5>{{ showEditModal ? '编辑轮播图' : '新增轮播图' }}</h5>
          <button class="btn-close" @click="closeModal">
            <i class="bi bi-x"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">标题 *</label>
            <input v-model="form.title" type="text" class="form-control" placeholder="请输入标题" />
          </div>
          <div class="mb-3">
            <label class="form-label">副标题</label>
            <input v-model="form.subtitle" type="text" class="form-control" placeholder="请输入副标题" />
          </div>
          <div class="mb-3">
            <label class="form-label">图片 *</label>
            <ImageUpload v-model="form.imageUrl" role="admin" />
          </div>
          <div class="mb-3">
            <label class="form-label">跳转链接</label>
            <input v-model="form.linkUrl" type="text" class="form-control" placeholder="https://" />
          </div>
          <div class="mb-3">
            <label class="form-label">排序号</label>
            <input v-model.number="form.sortOrder" type="number" class="form-control" placeholder="数字越小越靠前" />
          </div>
          <div class="mb-3">
            <label class="form-label">状态</label>
            <select v-model="form.status" class="form-select">
              <option :value="1">启用</option>
              <option :value="0">禁用</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeModal">取消</button>
          <button class="btn btn-primary" @click="saveBanner" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { getBannerList, createBanner, updateBanner, deleteBanner, updateBannerStatus } from '@/api/common'

const banners = ref([])
const searchKeyword = ref('')
const selectedStatus = ref('')
const showAddModal = ref(false)
const showEditModal = ref(false)
const saving = ref(false)

const form = ref({
  id: null,
  title: '',
  subtitle: '',
  imageUrl: '',
  linkUrl: '',
  sortOrder: 0,
  status: 1
})

onMounted(() => {
  loadBanners()
})

async function loadBanners() {
  try {
    const res = await getBannerList({
      keyword: searchKeyword.value,
      status: selectedStatus.value
    })
    if (res.code === 200) {
      banners.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载失败:', error)
  }
}

function editBanner(banner) {
  form.value = { ...banner }
  showEditModal.value = true
}

function closeModal() {
  showAddModal.value = false
  showEditModal.value = false
  form.value = {
    id: null,
    title: '',
    subtitle: '',
    imageUrl: '',
    linkUrl: '',
    sortOrder: 0,
    status: 1
  }
}

async function saveBanner() {
  if (!form.value.title) {
    alert('请输入标题')
    return
  }
  if (!form.value.imageUrl) {
    alert('请上传图片')
    return
  }

  saving.value = true
  try {
    let res
    if (showEditModal.value) {
      res = await updateBanner(form.value.id, form.value)
    } else {
      res = await createBanner(form.value)
    }

    if (res.code === 200) {
      alert('保存成功')
      closeModal()
      loadBanners()
    } else {
      alert(res.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    alert('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

async function updateSortOrder(banner, value) {
  try {
    const res = await updateBanner(banner.id, { ...banner, sortOrder: parseInt(value) })
    if (res.code === 200) {
      banner.sortOrder = parseInt(value)
      // 重新排序
      banners.value.sort((a, b) => a.sortOrder - b.sortOrder)
    }
  } catch (error) {
    console.error('更新排序失败:', error)
  }
}

async function toggleStatus(banner) {
  const newStatus = banner.status === 1 ? 0 : 1
  try {
    const res = await updateBannerStatus(banner.id, newStatus)
    if (res.code === 200) {
      banner.status = newStatus
    }
  } catch (error) {
    console.error('更新状态失败:', error)
  }
}

function deleteBannerConfirm(banner) {
  if (confirm(`确定删除轮播图"${banner.title}"吗？`)) {
    deleteBannerAction(banner.id)
  }
}

async function deleteBannerAction(id) {
  try {
    const res = await deleteBanner(id)
    if (res.code === 200) {
      alert('删除成功')
      loadBanners()
    } else {
      alert(res.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    alert('删除失败，请重试')
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
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.banners-table-wrapper {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.banner-thumb {
  width: 80px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  user-select: none;
}

.status-badge.active {
  background: #d1fae5;
  color: #059669;
}

.status-badge.inactive {
  background: #f3f4f6;
  color: #6b7280;
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
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 50px rgba(0,0,0,0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h5 {
  margin: 0;
  font-weight: 600;
  font-size: 18px;
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  border-radius: 8px;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #f1f5f9;
  color: #475569;
}

.modal-body {
  padding: 24px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 0 0 16px 16px;
}
</style>
