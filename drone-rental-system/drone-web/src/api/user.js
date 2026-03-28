import request from '@/utils/request'

// ==================== 认证 ====================

/**
 * 用户登录
 */
export function login(username, password, captchaKey, captchaCode) {
  return request.post('/user/api/login', { username, password, captchaKey, captchaCode })
}

/**
 * 用户注册
 */
export function register(data) {
  return request.post('/user/api/register', data)
}

/**
 * 用户登出
 */
export function logout() {
  return request.post('/user/api/logout')
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request.get('/user/api/me')
}

// ==================== 无人机 ====================

/**
 * 获取地图无人机数据（只返回已上架的）
 */
export function getVehicleMapData() {
  return request.get('/user/api/vehicles/map')
}

/**
 * 获取无人机详情
 */
export function getVehicleDetail(id) {
  return request.get(`/user/api/vehicle/${id}`)
}

/**
 * 获取无人机列表
 */
export function getVehicleList(params) {
  return request.get('/user/api/vehicles/list', { params })
}

// ==================== 订单 ====================

/**
 * 创建订单
 */
export function createOrder(vehicleId, plannedDuration) {
  return request.post('/user/api/order/create', null, {
    params: { vehicleId, plannedDuration }
  })
}

/**
 * 支付订单
 */
export function payOrder(id) {
  return request.post(`/user/api/order/${id}/pay`)
}

/**
 * 开始使用
 */
export function startUse(id) {
  return request.post(`/user/api/order/${id}/start`)
}

/**
 * 结束使用
 */
export function endUse(id, latitude, longitude, location) {
  return request.post(`/user/api/order/${id}/end`, null, {
    params: { latitude, longitude, location }
  })
}

/**
 * 取消订单
 */
export function cancelOrder(id, reason) {
  return request.post(`/user/api/order/${id}/cancel`, null, {
    params: { reason }
  })
}

/**
 * 获取订单列表
 */
export function getOrderList(params) {
  return request.get('/user/api/order/list', { params })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(id) {
  return request.get(`/user/api/order/${id}`)
}

/**
 * 获取订单统计
 */
export function getOrderStats() {
  return request.get('/user/api/order/stats')
}

/**
 * 获取最近订单
 */
export function getRecentOrders(limit = 5) {
  return request.get('/user/api/order/recent', {
    params: { limit }
  })
}

// ==================== 用户信息 ====================

/**
 * 更新用户信息
 */
export function updateProfile(realName, phone) {
  return request.post('/user/api/profile', null, {
    params: { realName, phone }
  })
}

/**
 * 修改密码
 */
export function changePassword(oldPassword, newPassword) {
  return request.post('/user/api/password/change', {
    oldPassword,
    newPassword
  })
}

// ==================== 轮播图 ====================

/**
 * 获取启用的轮播图列表
 */
export function getActiveBanners() {
  return request.get('/user/api/banner/list')
}

// ==================== 报修 ====================

/**
 * 提交报修
 */
export function createRepair(data) {
  return request.post('/user/api/repair/create', data)
}

/**
 * 获取我的报修列表
 */
export function getMyRepairs() {
  return request.get('/user/api/repair/my')
}

/**
 * 获取报修详情
 */
export function getRepairDetail(id) {
  return request.get(`/user/api/repair/${id}`)
}

// ==================== AI 损伤检测 ====================

/**
 * 获取检测服务状态
 */
export function getDetectionStatus() {
  return request.get('/user/api/detect/status')
}

/**
 * 上传图片进行损伤检测
 */
export function detectImageDamage(file, vehicleId, orderId) {
  const formData = new FormData()
  formData.append('file', file)
  if (vehicleId) formData.append('vehicle_id', vehicleId)
  if (orderId) formData.append('order_id', orderId)

  return request.post('/user/api/detect/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 上传多张图片进行损伤检测
 */
export function detectMultipleImages(files, vehicleId, orderId) {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  if (vehicleId) formData.append('vehicle_id', vehicleId)
  if (orderId) formData.append('order_id', orderId)

  return request.post('/user/api/detect/images', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 上传视频进行损伤检测
 */
export function detectVideoDamage(file, vehicleId, orderId) {
  const formData = new FormData()
  formData.append('file', file)
  if (vehicleId) formData.append('vehicle_id', vehicleId)
  if (orderId) formData.append('order_id', orderId)

  return request.post('/user/api/detect/video', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// ==================== 评价 ====================

/**
 * 创建评价
 */
export function createReview(data) {
  return request.post('/user/api/review/create', null, { params: data })
}

/**
 * 检查订单是否已评价
 */
export function checkReview(orderId) {
  return request.get(`/user/api/review/check/${orderId}`)
}

/**
 * 获取订单评价
 */
export function getReviewByOrderId(orderId) {
  return request.get(`/user/api/review/order/${orderId}`)
}

/**
 * 获取我的评价列表
 */
export function getMyReviews(page = 1, size = 10) {
  return request.get('/user/api/review/my', { params: { page, size } })
}

/**
 * 获取无人机评价列表
 */
export function getVehicleReviews(vehicleId, page = 1, size = 10) {
  return request.get(`/user/api/review/vehicle/${vehicleId}`, { params: { page, size } })
}

/**
 * 获取无人机评价统计
 */
export function getReviewStats(vehicleId) {
  return request.get(`/user/api/review/stats/${vehicleId}`)
}

/**
 * 点赞评价
 */
export function likeReview(reviewId) {
  return request.post(`/user/api/review/${reviewId}/like`)
}

