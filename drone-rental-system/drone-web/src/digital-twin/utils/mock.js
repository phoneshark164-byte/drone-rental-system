// 不再使用 axios，改用 fetch API

// 当前位置，默认为深圳
let currentLocation = 'shenzhen'

/**
 * 设置当前位置
 * @param location {String} 位置名称: 'shenzhen' 或 'sichuan'
 */
export function setLocation(location) {
  console.log('设置位置为:', location)
  currentLocation = location
}

/**
 * 获得当前位置
 */
export function getLocation() {
  return currentLocation
}

/**
 * 通用的获取MOCK数据方法
 * @param name {String} 文件名
 * @param options {object} 配置参数
 * @returns {Promise}
 */
export function fetchMockData (name, options) {
    if (!name) {
      console.error('缺少模拟数据JOSN文件名')
      return Promise.resolve(null)
    }
    return new Promise((resolve) => {
      const ext = /json/.test(name) ? '' : '.json'
      // 尝试先从当前位置目录加载，如果失败则从默认目录加载
      const locationPath = currentLocation === 'shenzhen' ? '' : `${currentLocation}/`
      const url = `/digital-twin-static/mock/${locationPath}${name}${ext}`
      // 添加时间戳防止缓存
      const cacheBuster = `?t=${Date.now()}`
      console.log('加载数据:', url + cacheBuster)

      // 使用 fetch API 替代 axios，避免潜在的解析问题
      fetch(url + cacheBuster)
        .then(response => {
          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`)
          }
          return response.text()
        })
        .then(text => {
          // 手动解析 JSON
          const data = JSON.parse(text)
          console.log('数据加载成功:', name, '特征数:', data?.features?.length || 0)
          resolve(data)
        })
        .catch((err) => {
          console.warn('从位置目录加载失败，尝试默认目录:', err.message)
          // 如果当前位置目录不存在，从默认目录加载
          fetch(`/digital-twin-static/mock/${name}${ext}`)
            .then(response => {
              if (!response.ok) {
                throw new Error(`HTTP ${response.status}`)
              }
              return response.text()
            })
            .then(text => {
              const data = JSON.parse(text)
              console.log('从默认目录加载成功:', name, '特征数:', data?.features?.length || 0)
              resolve(data)
            })
            .catch(() => {
              console.warn(`数据文件 ${name} 加载失败`)
              resolve(null)
            })
        })
    })
  }
