<template>
  <div class="admin-settings">
    <div class="page-header">
      <h3>系统设置</h3>
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
      <p class="mt-3">加载中...</p>
    </div>

    <div class="settings-content" v-else>
      <!-- 租赁规则 -->
      <div class="setting-section">
        <h4 class="section-title">
          <i class="bi bi-gear me-2"></i>租赁规则
        </h4>
        <div class="rules-grid">
          <div class="rule-item" v-for="rule in rentalRules" :key="rule.id">
            <label class="rule-label">{{ rule.ruleName }}</label>
            <div class="rule-input">
              <input
                v-model="rule.ruleValue"
                type="text"
                class="form-control"
                :disabled="!editMode"
              />
              <span class="rule-unit">{{ getRuleUnit(rule.ruleKey) }}</span>
            </div>
            <small class="text-muted">{{ rule.remark }}</small>
          </div>
        </div>
      </div>

      <!-- 系统配置 -->
      <div class="setting-section">
        <h4 class="section-title">
          <i class="bi bi-sliders me-2"></i>系统配置
        </h4>
        <div class="config-list">
          <div class="config-item">
            <div class="config-info">
              <h5>系统名称</h5>
              <p>显示在页面标题和导航栏中的系统名称</p>
            </div>
            <input
              v-model="systemConfig.name"
              type="text"
              class="form-control"
              style="width: 300px"
              :disabled="!editMode"
            />
          </div>
          <div class="config-item">
            <div class="config-info">
              <h5>默认押金金额</h5>
              <p>用户注册时需要缴纳的押金金额</p>
            </div>
            <div class="input-group" style="width: 200px">
              <input
                v-model.number="systemConfig.deposit"
                type="number"
                class="form-control"
                :disabled="!editMode"
              />
              <span class="input-group-text">元</span>
            </div>
          </div>
          <div class="config-item">
            <div class="config-info">
              <h5>注册需缴纳押金</h5>
              <p>开启后用户注册时必须缴纳押金</p>
            </div>
            <div class="form-check form-switch">
              <input
                v-model="systemConfig.depositRequired"
                class="form-check-input"
                type="checkbox"
                :disabled="!editMode"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 地图配置 -->
      <div class="setting-section">
        <h4 class="section-title">
          <i class="bi bi-map me-2"></i>地图配置
        </h4>
        <div class="config-list">
          <div class="config-item">
            <div class="config-info">
              <h5>高德地图 API Key</h5>
              <p>用于显示地图和定位功能</p>
            </div>
            <input
              v-model="mapConfig.apiKey"
              type="text"
              class="form-control"
              style="width: 400px"
              :disabled="!editMode"
            />
          </div>
          <div class="config-item">
            <div class="config-info">
              <h5>安全代码</h5>
              <p>高德地图安全密钥</p>
            </div>
            <input
              v-model="mapConfig.securityCode"
              type="text"
              class="form-control"
              style="width: 400px"
              :disabled="!editMode"
            />
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="settings-actions">
        <button
          v-if="!editMode"
          class="btn btn-primary"
          @click="editMode = true"
        >
          <i class="bi bi-pencil me-1"></i>编辑设置
        </button>
        <template v-else>
          <button class="btn btn-outline-secondary" @click="handleCancel">
            <i class="bi bi-x-lg me-1"></i>取消
          </button>
          <button class="btn btn-primary" @click="handleSave" :disabled="saving">
            <i class="bi bi-check-lg me-1"></i>{{ saving ? '保存中...' : '保存更改' }}
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getSystemConfigs, updateSystemConfigs } from '@/api/admin'

const loading = ref(true)
const saving = ref(false)
const editMode = ref(false)

// 原始配置备份（用于取消时恢复）
const originalConfigs = ref({})

const rentalRules = ref([
  { id: 1, ruleKey: 'rental.price.per.hour', ruleName: '每小时租金', ruleValue: '20.00', remark: '每小时租赁价格' },
  { id: 2, ruleKey: 'rental.price.per.minute', ruleName: '每分钟租金', ruleValue: '0.50', remark: '每分钟租赁价格' },
  { id: 3, ruleKey: 'rental.deposit.amount', ruleName: '押金金额', ruleValue: '500.00', remark: '用户需缴纳的押金' },
  { id: 4, ruleKey: 'rental.max.duration', ruleName: '最大租用时长', ruleValue: '480', remark: '最大租用时长（分钟）' },
  { id: 5, ruleKey: 'rental.min.duration', ruleName: '最小租用时长', ruleValue: '30', remark: '最小租用时长（分钟）' },
  { id: 6, ruleKey: 'rental.cancel.minutes', ruleName: '可取消时间', ruleValue: '30', remark: '订单开始前多少分钟内可取消' }
])

const systemConfig = reactive({
  name: '无人机智能租赁系统',
  deposit: 500,
  depositRequired: true
})

const mapConfig = reactive({
  apiKey: '',
  securityCode: ''
})

const getRuleUnit = (ruleKey) => {
  const unitMap = {
    'rental.price.per.hour': '元/小时',
    'rental.price.per.minute': '元/分钟',
    'rental.deposit.amount': '元',
    'rental.max.duration': '分钟',
    'rental.min.duration': '分钟',
    'rental.cancel.minutes': '分钟'
  }
  return unitMap[ruleKey] || ''
}

// 加载系统配置
const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await getSystemConfigs()
    if (res.code === 200 && res.data) {
      const data = res.data

      // 更新租赁规则
      rentalRules.value.forEach(rule => {
        if (data[rule.ruleKey]) {
          rule.ruleValue = data[rule.ruleKey]
        }
      })

      // 更新系统配置
      systemConfig.name = data['system.name'] || '无人机智能租赁系统'
      systemConfig.deposit = parseFloat(data['system.deposit']) || 500
      systemConfig.depositRequired = data['system.deposit.required'] === 'true'

      // 更新地图配置
      mapConfig.apiKey = data['map.api.key'] || ''
      mapConfig.securityCode = data['map.security.code'] || ''

      // 保存原始配置
      originalConfigs.value = JSON.parse(JSON.stringify(data))
    }
  } catch (error) {
    console.error('加载系统配置失败:', error)
    alert('加载系统配置失败，请重试')
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  // 恢复原始配置
  const data = originalConfigs.value
  rentalRules.value.forEach(rule => {
    if (data[rule.ruleKey]) {
      rule.ruleValue = data[rule.ruleKey]
    }
  })
  systemConfig.name = data['system.name'] || '无人机智能租赁系统'
  systemConfig.deposit = parseFloat(data['system.deposit']) || 500
  systemConfig.depositRequired = data['system.deposit.required'] === 'true'
  mapConfig.apiKey = data['map.api.key'] || ''
  mapConfig.securityCode = data['map.security.code'] || ''

  editMode.value = false
}

const handleSave = async () => {
  saving.value = true
  try {
    // 构建配置对象
    const configs = {}

    // 添加租赁规则
    rentalRules.value.forEach(rule => {
      configs[rule.ruleKey] = rule.ruleValue
    })

    // 添加系统配置
    configs['system.name'] = systemConfig.name
    configs['system.deposit'] = String(systemConfig.deposit)
    configs['system.deposit.required'] = String(systemConfig.depositRequired)

    // 添加地图配置
    configs['map.api.key'] = mapConfig.apiKey
    configs['map.security.code'] = mapConfig.securityCode

    const res = await updateSystemConfigs(configs)
    if (res.code === 200) {
      alert('设置已保存')
      editMode.value = false
      // 重新加载配置
      await loadConfigs()
    } else {
      alert(res.message || '保存失败')
    }
  } catch (error) {
    console.error('保存设置失败:', error)
    alert('保存设置失败，请重试')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-header h3 {
  font-weight: 600;
  margin: 0;
}

.settings-content {
  max-width: 900px;
}

.setting-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.section-title {
  font-weight: 600;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e2e8f0;
  color: #1e293b;
}

.rules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.rule-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rule-label {
  font-weight: 500;
  font-size: 14px;
  color: #475569;
}

.rule-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rule-input .form-control {
  flex: 1;
}

.rule-unit {
  color: #64748b;
  font-size: 14px;
  white-space: nowrap;
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.config-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.config-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.config-info h5 {
  font-weight: 600;
  margin-bottom: 4px;
}

.config-info p {
  color: #64748b;
  margin: 0;
  font-size: 14px;
}

.settings-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}
</style>
