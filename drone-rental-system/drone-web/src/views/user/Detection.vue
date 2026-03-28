<template>
  <div class="detection-page">
    <UserHeader />

    <div class="container py-5">
      <div class="page-header d-flex justify-content-between align-items-center mb-4">
        <div>
          <router-link to="/user/vehicles" class="btn btn-outline-secondary me-2">
            <i class="bi bi-arrow-left me-1"></i>返回列表
          </router-link>
          <router-link to="/" class="btn btn-outline-primary">
            <i class="bi bi-house me-1"></i>返回首页
          </router-link>
        </div>
        <h2 class="page-title mb-0">AI 损伤检测</h2>
      </div>

      <!-- 检测服务状态 -->
      <div class="status-card mb-4" :class="{ 'status-ok': serviceStatus.available, 'status-error': !serviceStatus.available }">
        <div class="status-icon">
          <i :class="serviceStatus.available ? 'bi bi-check-circle' : 'bi bi-x-circle'"></i>
        </div>
        <div class="status-info">
          <h5>检测服务状态</h5>
          <p>{{ serviceStatus.available ? 'AI 检测服务运行中' : 'AI 检测服务未启动' }}</p>
          <button v-if="!serviceStatus.available" class="btn btn-sm btn-light mt-2" @click="checkServiceStatus">
            <i class="bi bi-arrow-clockwise me-1"></i>重新检查
          </button>
        </div>
      </div>

      <!-- 检测模式切换 -->
      <div class="mode-tabs mb-4">
        <button class="mode-tab" :class="{ active: detectMode === 'image' }" @click="detectMode = 'image'">
          <i class="bi bi-images me-2"></i>多图片检测
        </button>
        <button class="mode-tab" :class="{ active: detectMode === 'video' }" @click="detectMode = 'video'">
          <i class="bi bi-camera-video me-2"></i>视频检测
        </button>
      </div>

      <!-- 检测区域 -->
      <div class="row">
        <!-- 上传区域 -->
        <div class="col-lg-6 mb-4">
          <div class="upload-card">
            <h4 class="card-title">
              <i class="bi bi-cloud-upload me-2"></i>{{ detectMode === 'image' ? '上传多角度图片' : '上传检测视频' }}
            </h4>

            <!-- 图片上传模式 -->
            <div v-if="detectMode === 'image'">
              <div class="upload-area multi-upload" :class="{ 'dragover': isDragOver }"
                @dragover.prevent="isDragOver = true" @dragleave.prevent="isDragOver = false"
                @drop.prevent="handleDrop" @click="triggerFileInput">
                <input ref="fileInput" type="file" accept="image/*" class="d-none" multiple @change="handleFileSelect">
                <div v-if="selectedFiles.length === 0" class="upload-placeholder">
                  <i class="bi bi-images display-4 text-muted"></i>
                  <p class="mb-0">拖拽图片到此处，或点击选择文件</p>
                  <small class="text-muted">支持 1-6 张图片，JPG、PNG、GIF 格式</small>
                </div>
                <div v-else class="multi-preview">
                  <div v-for="(file, index) in selectedFiles" :key="index" class="preview-item">
                    <img :src="getFilePreviewUrl(file)" alt="预览" />
                    <span class="preview-index">{{ index + 1 }}</span>
                    <button class="btn-remove-small" @click.stop="removeFile(index)">
                      <i class="bi bi-x"></i>
                    </button>
                  </div>
                  <div v-if="selectedFiles.length < 6" class="add-more" @click.stop="triggerFileInput">
                    <i class="bi bi-plus-lg"></i>
                  </div>
                </div>
              </div>

              <!-- 拍摄指引 -->
              <div class="photo-guide">
                <h6><i class="bi bi-info-circle me-2"></i>拍摄建议</h6>
                <div class="guide-steps">
                  <div class="step"><span class="step-num">1</span><span class="step-text">正面 - 无人机前方视角</span></div>
                  <div class="step"><span class="step-num">2</span><span class="step-text">背面 - 无人机后方视角</span></div>
                  <div class="step"><span class="step-num">3</span><span class="step-text">左侧 - 左侧螺旋桨特写</span></div>
                  <div class="step"><span class="step-num">4</span><span class="step-text">右侧 - 右侧螺旋桨特写</span></div>
                  <div class="step"><span class="step-num">5</span><span class="step-text">顶部 - 机身顶部俯视</span></div>
                  <div class="step"><span class="step-num">6</span><span class="step-text">底部 - 相机和传感器</span></div>
                </div>
              </div>
            </div>

            <!-- 视频上传模式 -->
            <div v-else>
              <div class="upload-area" :class="{ 'dragover': isDragOver, 'has-file': selectedVideo }"
                @dragover.prevent="isDragOver = true" @dragleave.prevent="isDragOver = false"
                @drop.prevent="handleVideoDrop" @click="triggerVideoInput">
                <input ref="videoInput" type="file" accept="video/*" class="d-none" @change="handleVideoSelect">
                <div v-if="!selectedVideo" class="upload-placeholder">
                  <i class="bi bi-camera-video display-4 text-muted"></i>
                  <p class="mb-0">拖拽视频到此处，或点击选择文件</p>
                  <small class="text-muted">支持 MP4、AVI、MOV 格式，建议拍摄无人机旋转360°</small>
                </div>
                <div v-else class="video-preview">
                  <video :src="videoPreviewUrl" controls></video>
                  <button class="btn-remove" @click.stop="removeVideo">
                    <i class="bi bi-x"></i>
                  </button>
                </div>
              </div>

              <!-- 视频拍摄指引 -->
              <div class="video-guide">
                <h6><i class="bi bi-info-circle me-2"></i>拍摄建议</h6>
                <ul>
                  <li>将无人机放置在平稳的表面</li>
                  <li>围绕无人机缓慢拍摄360°全景</li>
                  <li>确保每个角度都有清晰画面</li>
                  <li>建议视频时长 5-15 秒</li>
                </ul>
              </div>
            </div>

            <!-- 无人机选择 -->
            <div class="mt-3">
              <label class="form-label">关联无人机 (可选)</label>
              <select v-model="selectedVehicleId" class="form-select">
                <option value="">不关联无人机</option>
                <option v-for="vehicle in vehicles" :key="vehicle.id" :value="vehicle.id">
                  {{ vehicle.droneNo }} - {{ vehicle.brand }} {{ vehicle.model }}
                </option>
              </select>
            </div>

            <!-- 检测按钮 -->
            <button class="btn btn-detect w-100 mt-3" :disabled="!hasFiles || detecting"
              @click="handleDetect">
              <span v-if="detecting" class="spinner-border spinner-border-sm me-2"></span>
              <i v-else class="bi bi-search me-2"></i>
              {{ detecting ? '检测中...' : '开始检测' }}
            </button>
            <div v-if="!serviceStatus.available" class="text-warning mt-2 small">
              <i class="bi bi-exclamation-triangle me-1"></i>检测服务未连接，检测结果可能不准确
            </div>
          </div>
        </div>

        <!-- 检测结果区域 -->
        <div class="col-lg-6 mb-4">
          <div class="result-card">
            <h4 class="card-title">
              <i class="bi bi-clipboard-data me-2"></i>检测结果
            </h4>

            <div v-if="!hasResult" class="result-placeholder">
              <i class="bi bi-clipboard-x display-4 text-muted"></i>
              <p class="mb-0 text-muted">{{ detectMode === 'image' ? '上传多张图片后开始检测' : '上传视频后开始检测' }}</p>
            </div>

            <div v-else class="result-content">
              <!-- 检测概要 -->
              <div class="detection-summary">
                <div class="summary-item">
                  <span class="summary-label">检测状态</span>
                  <span class="summary-value" :class="getStatusClass(result.damage_analysis?.responsibility)">
                    {{ getStatusText(result.damage_analysis?.responsibility) }}
                  </span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">检测数量</span>
                  <span class="summary-value">{{ result.image_count || 1 }} 张</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">损伤数量</span>
                  <span class="summary-value">{{ result.damage_analysis?.damage_count || 0 }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">严重程度</span>
                  <span class="summary-value" :class="getSeverityClass(result.damage_analysis?.overall_severity)">
                    {{ getSeverityText(result.damage_analysis?.overall_severity) }}
                  </span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">检测时间</span>
                  <span class="summary-value">{{ result.total_inference_time || result.inference_time }}s</span>
                </div>
              </div>

              <!-- 图片结果展示 -->
              <div v-if="result.image_results && result.image_results.length > 0" class="image-results">
                <h6 class="mb-3">各角度检测结果</h6>
                <div class="image-grid">
                  <div v-for="(img, index) in result.image_results" :key="index" class="image-result-item"
                    @click="showImageDetail(img)">
                    <img :src="img.result_path" alt="检测结果" />
                    <div class="image-badge">
                      <span class="badge" :class="img.damage_count > 0 ? 'bg-danger' : 'bg-success'">
                        {{ img.damage_count }} 处损伤
                      </span>
                    </div>
                    <div class="image-index">{{ index + 1 }}</div>
                  </div>
                </div>
              </div>

              <!-- 单张图片结果 -->
              <div v-else-if="result.result_image_url" class="single-result">
                <img :src="result.result_image_url" alt="检测结果" @click="showImageModal(result.result_image_url)" />
              </div>

              <!-- 检测到的损伤 -->
              <div v-if="result.damage_analysis?.detected_damages?.length > 0 || result.damage_analysis?.damage_count > 0"
                class="damages-list">
                <h6 class="mb-3">检测详情</h6>
                <div v-if="result.damage_analysis?.detected_damages"
                  v-for="(damage, index) in result.damage_analysis.detected_damages" :key="index" class="damage-item">
                  <div class="damage-icon">
                    <i class="bi bi-exclamation-triangle"></i>
                  </div>
                  <div class="damage-info">
                    <div class="damage-type">{{ damage.type }}</div>
                    <div class="damage-meta">
                      <span class="badge" :class="getSeverityClass(damage.severity)">{{ getSeverityText(damage.severity) }}</span>
                      <span class="confidence">置信度: {{ (damage.confidence * 100).toFixed(1) }}%</span>
                    </div>
                  </div>
                </div>
                <div v-else class="damage-summary-text">
                  共检测到 {{ result.damage_analysis.damage_count }} 处损伤
                </div>
              </div>

              <!-- 责任判定 -->
              <div class="responsibility-card mt-3">
                <h6>责任判定</h6>
                <div class="responsibility-content">
                  <div class="responsibility-label" :class="result.damage_analysis?.responsibility">
                    {{ getResponsibilityText(result.damage_analysis?.responsibility) }}
                  </div>
                  <p class="responsibility-reason">{{ result.damage_analysis?.responsibility_reason }}</p>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="result-actions mt-3">
                <button v-if="result.damage_analysis?.damage_count > 0" class="btn btn-primary"
                  @click="createRepairFromDetection">
                  <i class="bi bi-tools me-1"></i>创建报修
                </button>
                <button class="btn btn-outline-secondary" @click="resetDetection">
                  <i class="bi bi-arrow-counterclockwise me-1"></i>重新检测
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 检测说明 -->
      <div class="info-card mt-4">
        <h5><i class="bi bi-info-circle me-2"></i>检测说明</h5>
        <div class="row">
          <div class="col-md-6">
            <h6>多图片检测</h6>
            <ul class="info-list">
              <li>支持上传 1-6 张不同角度的图片</li>
              <li>系统综合分析所有图片的检测结果</li>
              <li>建议按拍摄指引上传完整角度</li>
            </ul>
          </div>
          <div class="col-md-6">
            <h6>视频检测</h6>
            <ul class="info-list">
              <li>拍摄无人机旋转 360° 的视频</li>
              <li>系统自动提取关键帧进行分析</li>
              <li>适合快速全面检测</li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片详情弹窗 -->
    <div class="modal fade" id="imageModal" tabindex="-1" ref="imageModal">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">检测详情</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <img :src="selectedImageUrl" alt="检测详情" class="w-100" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import UserHeader from '@/components/UserHeader.vue'
import { detectMultipleImages, detectVideoDamage, getDetectionStatus, getVehicleList } from '@/api/user'
import { Modal } from 'bootstrap'

const router = useRouter()

const serviceStatus = ref({ available: false })
const detectMode = ref('image')
const selectedFiles = ref([])
const selectedVideo = ref(null)
const videoPreviewUrl = ref('')
const isDragOver = ref(false)
const detecting = ref(false)
const hasResult = ref(false)
const result = ref({})
const selectedVehicleId = ref('')
const vehicles = ref([])
const selectedImageUrl = ref('')
const imageModal = ref(null)
let modalInstance = null

const fileInput = ref(null)
const videoInput = ref(null)

const hasFiles = computed(() => {
  return detectMode.value === 'image' ? selectedFiles.value.length > 0 : selectedVideo.value !== null
})

const triggerFileInput = () => {
  fileInput.value.click()
}

const triggerVideoInput = () => {
  videoInput.value.click()
}

const handleFileSelect = (e) => {
  const files = Array.from(e.target.files)
  addFiles(files)
}

const handleDrop = (e) => {
  isDragOver.value = false
  const files = Array.from(e.dataTransfer.files).filter(f => f.type.startsWith('image/'))
  addFiles(files)
}

const addFiles = (files) => {
  const remaining = 6 - selectedFiles.value.length
  const toAdd = files.slice(0, remaining)

  for (const file of toAdd) {
    if (!selectedFiles.value.some(f => f.name === file.name)) {
      selectedFiles.value.push(file)
    }
  }

  if (files.length > remaining) {
    alert('最多只能上传6张图片，已忽略多余的图片')
  }

  hasResult.value = false
}

const removeFile = (index) => {
  selectedFiles.value.splice(index, 1)
}

const getFilePreviewUrl = (file) => {
  return URL.createObjectURL(file)
}

const handleVideoSelect = (e) => {
  const file = e.target.files[0]
  if (file && file.type.startsWith('video/')) {
    selectedVideo.value = file
    videoPreviewUrl.value = URL.createObjectURL(file)
    hasResult.value = false
  }
}

const handleVideoDrop = (e) => {
  isDragOver.value = false
  const file = e.dataTransfer.files[0]
  if (file && file.type.startsWith('video/')) {
    selectedVideo.value = file
    videoPreviewUrl.value = URL.createObjectURL(file)
    hasResult.value = false
  }
}

const removeVideo = () => {
  selectedVideo.value = null
  videoPreviewUrl.value = ''
  if (videoInput.value) {
    videoInput.value.value = ''
  }
}

const checkServiceStatus = async () => {
  try {
    const res = await getDetectionStatus()
    if (res.code === 200) {
      serviceStatus.value = res.data
    }
  } catch (error) {
    console.error('检查服务状态失败:', error)
    serviceStatus.value = { available: false }
  }
}

const handleDetect = async () => {
  console.log('开始检测，模式:', detectMode.value)
  console.log('选择的文件:', selectedFiles.value)
  console.log('选择的视频:', selectedVideo.value)

  if (detectMode.value === 'image' && selectedFiles.value.length === 0) {
    alert('请先上传图片')
    return
  }
  if (detectMode.value === 'video' && !selectedVideo.value) {
    alert('请先上传视频')
    return
  }

  detecting.value = true
  try {
    let res
    if (detectMode.value === 'image') {
      console.log('调用多图片检测...')
      res = await detectMultipleImages(selectedFiles.value, selectedVehicleId.value, null)
    } else {
      console.log('调用视频检测...')
      res = await detectVideoDamage(selectedVideo.value, selectedVehicleId.value, null)
    }

    console.log('检测响应:', res)

    if (res.code === 200) {
      result.value = res.data
      hasResult.value = true

      if (res.data.auto_repair_no) {
        alert('检测完成！系统已自动创建报修单: ' + res.data.auto_repair_no)
      }
    } else {
      alert(res.message || '检测失败')
    }
  } catch (error) {
    console.error('检测失败:', error)
    alert('检测失败，请重试: ' + error.message)
  } finally {
    detecting.value = false
  }
}

const showImageDetail = (img) => {
  selectedImageUrl.value = img.result_path
  if (!modalInstance) {
    modalInstance = new Modal(imageModal.value)
  }
  modalInstance.show()
}

const showImageModal = (url) => {
  selectedImageUrl.value = url
  if (!modalInstance) {
    modalInstance = new Modal(imageModal.value)
  }
  modalInstance.show()
}

const createRepairFromDetection = () => {
  if (result.value.auto_repair_id) {
    alert('报修记录已自动创建')
  } else {
    router.push('/user/repairs')
  }
}

const resetDetection = () => {
  hasResult.value = false
  result.value = {}
  selectedFiles.value = []
  selectedVideo.value = null
  videoPreviewUrl.value = ''
}

const getStatusClass = (status) => {
  const classes = {
    'user': 'text-danger',
    'operator': 'text-warning',
    'shared': 'text-info',
    'none': 'text-success'
  }
  return classes[status] || 'text-muted'
}

const getStatusText = (status) => {
  const texts = {
    'user': '用户责任',
    'operator': '运营方责任',
    'shared': '共同责任',
    'none': '无损伤'
  }
  return texts[status] || '未知'
}

const getSeverityClass = (severity) => {
  const classes = {
    'minor': 'text-success',
    'moderate': 'text-warning',
    'severe': 'text-danger'
  }
  return classes[severity] || 'text-muted'
}

const getSeverityText = (severity) => {
  const texts = {
    'minor': '轻微',
    'moderate': '中等',
    'severe': '严重'
  }
  return texts[severity] || '未知'
}

const getResponsibilityText = (status) => {
  const texts = {
    'user': '用户责任',
    'operator': '运营方责任',
    'shared': '共同责任',
    'none': '无损伤'
  }
  return texts[status] || '未知'
}

const loadVehicles = async () => {
  try {
    const res = await getVehicleList()
    if (res.code === 200 && res.data) {
      vehicles.value = res.data
    }
  } catch (error) {
    console.error('加载无人机列表失败:', error)
  }
}

onMounted(() => {
  checkServiceStatus()
  loadVehicles()
})
</script>

<style scoped>
.page-title { font-weight: 700; }
.status-card { display: flex; align-items: center; gap: 16px; background: white; border-radius: 16px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.status-card.status-ok { border-left: 4px solid #10b981; }
.status-card.status-error { border-left: 4px solid #ef4444; }
.status-icon { width: 48px; height: 48px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; }
.status-ok .status-icon { background: #d1fae5; color: #059669; }
.status-error .status-icon { background: #fee2e2; color: #dc2626; }
.mode-tabs { display: flex; gap: 16px; background: white; border-radius: 16px; padding: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.mode-tab { flex: 1; padding: 12px 24px; border: none; background: transparent; border-radius: 12px; font-weight: 500; color: #64748b; cursor: pointer; transition: all 0.3s; }
.mode-tab.active { background: linear-gradient(135deg, #3b82f6, #8b5cf6); color: white; }
.upload-card, .result-card, .info-card { background: white; border-radius: 16px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); height: 100%; }
.card-title { font-weight: 600; margin-bottom: 20px; }
.upload-area { border: 2px dashed #e2e8f0; border-radius: 12px; padding: 40px 20px; text-align: center; cursor: pointer; transition: all 0.3s; min-height: 250px; display: flex; align-items: center; justify-content: center; }
.upload-area:hover, .upload-area.dragover { border-color: #3b82f6; background: #f8fafc; }
.upload-placeholder i { font-size: 48px; opacity: 0.5; }
.multi-upload { min-height: 200px; }
.multi-preview { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; width: 100%; }
.preview-item { position: relative; aspect-ratio: 1; }
.preview-item img { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; }
.preview-index { position: absolute; top: 4px; left: 4px; width: 24px; height: 24px; background: rgba(0,0,0,0.6); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; }
.btn-remove-small { position: absolute; top: 4px; right: 4px; width: 24px; height: 24px; border-radius: 50%; background: rgba(220, 38, 38, 0.9); color: white; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.add-more { aspect-ratio: 1; border: 2px dashed #cbd5e1; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #94a3b8; font-size: 24px; cursor: pointer; }
.add-more:hover { border-color: #3b82f6; color: #3b82f6; }
.video-preview { position: relative; width: 100%; }
.video-preview video { width: 100%; max-height: 250px; border-radius: 8px; }
.btn-remove { position: absolute; top: 8px; right: 8px; width: 32px; height: 32px; border-radius: 50%; background: rgba(0,0,0,0.5); color: white; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.photo-guide, .video-guide { background: #f8fafc; border-radius: 12px; padding: 16px; margin-top: 16px; }
.photo-guide h6, .video-guide h6 { font-weight: 600; margin-bottom: 12px; color: #475569; }
.guide-steps { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.step { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.step-num { width: 20px; height: 20px; background: #3b82f6; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 600; flex-shrink: 0; }
.video-guide ul { list-style: none; padding: 0; margin: 0; }
.video-guide li { padding: 4px 0; font-size: 14px; color: #475569; }
.video-guide li:before { content: "•"; color: #3b82f6; font-weight: bold; margin-right: 8px; }
.btn-detect { padding: 14px; background: linear-gradient(135deg, #3b82f6, #8b5cf6); border: none; border-radius: 12px; color: white; font-weight: 600; transition: all 0.3s; }
.btn-detect:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 10px 30px rgba(59, 130, 246, 0.3); }
.btn-detect:disabled { opacity: 0.6; cursor: not-allowed; }
.result-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 300px; color: #94a3b8; }
.result-placeholder i { font-size: 48px; opacity: 0.5; }
.detection-summary { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 20px; }
.summary-item { background: #f8fafc; padding: 12px; border-radius: 8px; }
.summary-label { font-size: 12px; color: #64748b; }
.summary-value { font-weight: 600; color: #1e293b; }
.image-results { margin-bottom: 20px; }
.image-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.image-result-item { position: relative; cursor: pointer; }
.image-result-item img { width: 100%; border-radius: 8px; transition: transform 0.3s; }
.image-result-item:hover img { transform: scale(1.05); }
.image-badge { position: absolute; top: 4px; left: 4px; }
.image-index { position: absolute; bottom: 4px; right: 4px; width: 20px; height: 20px; background: rgba(0,0,0,0.6); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; }
.single-result img { width: 100%; border-radius: 8px; cursor: pointer; }
.damages-list { max-height: 200px; overflow-y: auto; margin-bottom: 20px; }
.damage-item { display: flex; align-items: center; gap: 12px; padding: 12px; background: #fef2f2; border-radius: 8px; margin-bottom: 8px; }
.damage-icon { width: 36px; height: 36px; background: #fee2e2; color: #dc2626; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.damage-type { font-weight: 600; color: #1e293b; }
.damage-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.damage-summary-text { padding: 12px; background: #fef2f2; border-radius: 8px; color: #dc2626; text-align: center; font-weight: 500; }
.responsibility-card { background: #f0f9ff; border-radius: 12px; padding: 16px; }
.responsibility-content { margin-top: 12px; }
.responsibility-label { display: inline-block; padding: 6px 16px; border-radius: 20px; font-weight: 600; color: white; }
.responsibility-label.user { background: #ef4444; }
.responsibility-label.operator { background: #f59e0b; }
.responsibility-label.shared { background: #3b82f6; }
.responsibility-label.none { background: #10b981; }
.responsibility-reason { margin-top: 8px; color: #475569; font-size: 14px; }
.result-actions { display: flex; gap: 12px; }
.result-actions .btn { flex: 1; }
.info-list { list-style: none; padding: 0; margin: 0; }
.info-list li { padding: 4px 0; color: #475569; font-size: 14px; }
.info-list li:before { content: "✓"; color: #10b981; font-weight: bold; margin-right: 8px; }
</style>
