import request from '@/utils/request'

// ==================== 用户管理 ====================

/**
 * 获取用户列表
 */
export function getUserList(params) {
  return request.get('/admin/api/user/list', { params })
}

/**
 * 获取用户详情
 */
export function getUserDetail(id) {
  return request.get(`/admin/api/user/${id}`)
}

/**
 * 添加用户
 */
export function createUser(data) {
  return request.post('/admin/api/user', data)
}

/**
 * 更新用户
 */
export function updateUser(id, data) {
  return request.put(`/admin/api/user/${id}`, data)
}

/**
 * 删除用户
 */
export function deleteUser(id) {
  return request.delete(`/admin/api/user/${id}`)
}

/**
 * 修改用户状态
 */
export function updateUserStatus(id, status) {
  return request.post(`/admin/api/user/${id}/status`, null, {
    params: { status }
  })
}

// ==================== 无人机管理 ====================

/**
 * 获取无人机列表
 */
export function getVehicleList(params) {
  return request.get('/admin/api/vehicle/list', { params })
}

/**
 * 获取无人机详情
 */
export function getVehicleDetail(id) {
  return request.get(`/admin/api/vehicle/${id}`)
}

/**
 * 添加无人机
 */
export function createVehicle(data) {
  return request.post('/admin/api/vehicle', data)
}

/**
 * 更新无人机
 */
export function updateVehicle(id, data) {
  return request.put(`/admin/api/vehicle/${id}`, data)
}

/**
 * 删除无人机
 */
export function deleteVehicle(id) {
  return request.delete(`/admin/api/vehicle/${id}`)
}

/**
 * 修改无人机状态
 */
export function updateVehicleStatus(id, status) {
  return request.post(`/admin/api/vehicle/${id}/status`, null, {
    params: { status }
  })
}

/**
 * 上架无人机
 */
export function listVehicle(id) {
  return request.post(`/admin/api/vehicle/${id}/list`)
}

/**
 * 下架无人机
 */
export function unlistVehicle(id) {
  return request.post(`/admin/api/vehicle/${id}/unlist`)
}

/**
 * 获取地图无人机数据
 */
export function getVehicleMapData() {
  return request.get('/admin/api/vehicles/map')
}

// ==================== 订单管理 ====================

/**
 * 获取订单列表
 */
export function getOrderList(params) {
  return request.get('/admin/api/order/list', { params })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(id) {
  return request.get(`/admin/api/order/${id}`)
}

/**
 * 获取订单统计数据
 */
export function getOrderStats() {
  return request.get('/admin/api/order/stats')
}

/**
 * 获取最近订单
 */
export function getRecentOrders(limit = 5) {
  return request.get('/admin/api/orders/recent', {
    params: { limit }
  })
}

// ==================== 统计数据 ====================

/**
 * 获取仪表板统计数据
 */
export function getDashboardStats() {
  return request.get('/admin/api/dashboard/stats')
}

/**
 * 测试地理编码
 */
export function testGeocode(address) {
  return request.get('/admin/api/geocode/test', {
    params: { address }
  })
}

// ==================== 操作日志 ====================

/**
 * 获取操作日志列表
 */
export function getLogList(params) {
  return request.get('/admin/api/log/list', { params })
}

/**
 * 清空操作日志
 */
export function clearLogs() {
  return request.delete('/admin/api/log/clear')
}

/**
 * 删除单条操作日志
 */
export function deleteLog(id) {
  return request.delete(`/admin/api/log/${id}`)
}

// ==================== 系统设置 ====================

/**
 * 获取所有系统配置
 */
export function getSystemConfigs() {
  return request.get('/admin/api/config/list')
}

/**
 * 更新系统配置
 */
export function updateSystemConfigs(configs) {
  return request.post('/admin/api/config/update', configs)
}

// ==================== 评价管理 ====================

/**
 * 获取评价列表
 */
export function getReviewList(params) {
  return request.get('/admin/api/reviews', { params })
}

/**
 * 获取评价详情
 */
export function getReviewDetail(id) {
  return request.get(`/admin/api/review/${id}`)
}

/**
 * 管理员回复评价
 */
export function replyReview(id, reply) {
  return request.post(`/admin/api/review/${id}/reply`, null, { params: { reply } })
}

/**
 * 显示/隐藏评价
 */
export function updateReviewStatus(id, status) {
  return request.post(`/admin/api/review/${id}/status`, null, { params: { status } })
}

/**
 * 删除评价
 */
export function deleteReview(id) {
  return request.delete(`/admin/api/review/${id}`)
}

/**
 * 获取评价统计
 */
export function getReviewStats() {
  return request.get('/admin/api/review/stats')
}
