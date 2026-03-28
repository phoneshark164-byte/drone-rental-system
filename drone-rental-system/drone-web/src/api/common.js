import request from '@/utils/request'

/**
 * 上传图片
 * @param {File} file 图片文件
 * @param {string} role 角色 (admin/operator/user)
 */
export function uploadImage(file, role = 'admin') {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/${role}/api/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 获取启用的轮播图列表（用户端）
 */
export function getActiveBanners() {
  return request.get('/user/api/banner/list')
}

/**
 * 获取轮播图列表（管理员）
 */
export function getBannerList(params) {
  return request.get('/admin/api/banner/list', { params })
}

/**
 * 获取轮播图详情
 */
export function getBannerDetail(id) {
  return request.get(`/admin/api/banner/${id}`)
}

/**
 * 新增轮播图
 */
export function createBanner(data) {
  return request.post('/admin/api/banner', data)
}

/**
 * 修改轮播图
 */
export function updateBanner(id, data) {
  return request.put(`/admin/api/banner/${id}`, data)
}

/**
 * 删除轮播图
 */
export function deleteBanner(id) {
  return request.delete(`/admin/api/banner/${id}`)
}

/**
 * 修改轮播图状态
 */
export function updateBannerStatus(id, status) {
  return request.post(`/admin/api/banner/${id}/status`, null, {
    params: { status }
  })
}
