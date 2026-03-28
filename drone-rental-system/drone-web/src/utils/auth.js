/**
 * 会话管理工具
 * 用于开发环境下控制登录会话的有效期
 */

// 会话超时时间（毫秒）
// 开发环境默认 30 分钟，可以根据需要调整
const SESSION_TIMEOUT = 30 * 60 * 1000

const STORAGE_KEY = {
  TOKEN: 'token',
  USER_ROLE: 'userRole',
  LOGIN_TIME: 'loginTime'
}

/**
 * 设置登录信息（包含时间戳）
 */
export function setLoginInfo(token, userRole, userInfo) {
  const loginTime = Date.now()
  localStorage.setItem(STORAGE_KEY.TOKEN, token)
  localStorage.setItem(STORAGE_KEY.USER_ROLE, userRole)
  localStorage.setItem(STORAGE_KEY.LOGIN_TIME, loginTime.toString())
  if (userInfo) {
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
  }
}

/**
 * 检查会话是否有效
 * @returns {boolean} 会话是否有效
 */
export function isSessionValid() {
  const token = localStorage.getItem(STORAGE_KEY.TOKEN)
  if (!token) {
    return false
  }

  const loginTimeStr = localStorage.getItem(STORAGE_KEY.LOGIN_TIME)
  if (!loginTimeStr) {
    // 如果没有登录时间，说明是旧版本的登录，清除并要求重新登录
    clearLoginInfo()
    return false
  }

  const loginTime = parseInt(loginTimeStr, 10)
  const now = Date.now()
  const elapsed = now - loginTime

  if (elapsed > SESSION_TIMEOUT) {
    // 会话已过期
    clearLoginInfo()
    return false
  }

  return true
}

/**
 * 获取剩余会话时间（分钟）
 */
export function getSessionMinutesRemaining() {
  const loginTimeStr = localStorage.getItem(STORAGE_KEY.LOGIN_TIME)
  if (!loginTimeStr) return 0

  const loginTime = parseInt(loginTimeStr, 10)
  const elapsed = Date.now() - loginTime
  const remaining = SESSION_TIMEOUT - elapsed

  return Math.max(0, Math.floor(remaining / 60000))
}

/**
 * 清除登录信息
 */
export function clearLoginInfo() {
  localStorage.removeItem(STORAGE_KEY.TOKEN)
  localStorage.removeItem(STORAGE_KEY.USER_ROLE)
  localStorage.removeItem(STORAGE_KEY.LOGIN_TIME)
  localStorage.removeItem('userInfo')
}

/**
 * 获取当前用户角色
 */
export function getUserRole() {
  if (!isSessionValid()) {
    return null
  }
  return localStorage.getItem(STORAGE_KEY.USER_ROLE)
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  if (!isSessionValid()) {
    return null
  }
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      return JSON.parse(userInfoStr)
    } catch (e) {
      return null
    }
  }
  return null
}
