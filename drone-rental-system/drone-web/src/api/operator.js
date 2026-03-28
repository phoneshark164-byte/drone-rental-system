import request from '@/utils/request'

// ==================== 认证 ====================

/**
 * 运营方登录
 */
export function login(username, password, captchaKey, captchaCode) {
  return request.post('/operator/api/login', { username, password, captchaKey, captchaCode })
}

/**
 * 获取当前运营方信息
 */
export function getCurrentOperator() {
  return request.get('/operator/api/me')
}

/**
 * 运营方登出
 */
export function logout() {
  return request.post('/operator/api/logout')
}

// ==================== 无人机管理 ====================

/**
 * 获取无人机列表
 */
export function getVehicleList(params) {
  return request.get('/operator/api/vehicles', { params })
}

/**
 * 获取无人机详情
 */
export function getVehicleDetail(id) {
  return request.get(`/operator/api/vehicle/${id}`)
}

/**
 * 添加无人机
 */
export function createVehicle(data) {
  return request.post('/operator/api/vehicle', data)
}

/**
 * 更新无人机
 */
export function updateVehicle(id, data) {
  return request.put(`/operator/api/vehicle/${id}`, data)
}

/**
 * 删除无人机
 */
export function deleteVehicle(id) {
  return request.delete(`/operator/api/vehicle/${id}`)
}

/**
 * 修改无人机状态
 */
export function updateVehicleStatus(id, status) {
  return request.post(`/operator/api/vehicle/${id}/status`, null, {
    params: { status }
  })
}

/**
 * 获取地图无人机数据
 */
export function getVehicleMapData() {
  return request.get('/operator/api/vehicles/map')
}

// ==================== 订单管理 ====================

/**
 * 获取订单列表
 */
export function getOrderList(params) {
  return request.get('/operator/api/order/list', { params })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(id) {
  return request.get(`/operator/api/order/${id}`)
}

/**
 * 获取订单统计
 */
export function getOrderStats() {
  return request.get('/operator/api/order/stats')
}

// ==================== 报修管理 ====================

/**
 * 提交报修
 */
export function createRepair(data) {
  return request.post('/operator/api/repair', data)
}

/**
 * 获取报修列表
 */
export function getRepairList(params) {
  return request.get('/operator/api/repair/list', { params })
}

/**
 * 获取报修详情
 */
export function getRepairDetail(id) {
  return request.get(`/operator/api/repair/${id}`)
}

/**
 * 开始处理报修
 */
export function startRepair(id) {
  return request.post(`/operator/api/repair/${id}/start`)
}

/**
 * 完成维修
 */
export function completeRepair(id, handleResult) {
  return request.post(`/operator/api/repair/${id}/complete`, null, {
    params: { handleResult }
  })
}

// ==================== 统计数据 ====================

/**
 * 获取仪表板统计数据
 */
export function getDashboardStats() {
  return request.get('/operator/api/dashboard/stats')
}
