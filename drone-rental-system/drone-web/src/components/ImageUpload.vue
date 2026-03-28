<template>
  <div class="image-upload">
    <div v-if="!imageUrl" class="upload-placeholder" @click="selectFile">
      <i class="bi bi-cloud-upload"></i>
      <span>点击上传图片</span>
    </div>
    <div v-else class="upload-preview">
      <img :src="imageUrl" alt="预览" />
      <div class="preview-mask">
        <button class="btn btn-sm btn-light" @click="selectFile">
          <i class="bi bi-pencil"></i> 更换
        </button>
        <button class="btn btn-sm btn-danger" @click="removeImage">
          <i class="bi bi-trash"></i> 删除
        </button>
      </div>
      <div v-if="uploading" class="upload-progress">
        <div class="progress">
          <div class="progress-bar" :style="{ width: uploadProgress + '%' }"></div>
        </div>
      </div>
    </div>
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { uploadImage } from '@/api/common'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  role: {
    type: String,
    default: 'admin'
  }
})

const emit = defineEmits(['update:modelValue'])

const fileInput = ref(null)
const imageUrl = ref(props.modelValue)
const uploading = ref(false)
const uploadProgress = ref(0)

watch(() => props.modelValue, (newVal) => {
  imageUrl.value = newVal
})

function selectFile() {
  fileInput.value.click()
}

async function handleFileChange(event) {
  const file = event.target.files[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }

  // 验证文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    alert('图片大小不能超过5MB')
    return
  }

  // 显示本地预览
  const reader = new FileReader()
  reader.onload = (e) => {
    imageUrl.value = e.target.result
  }
  reader.readAsDataURL(file)

  // 上传到服务器
  uploading.value = true
  uploadProgress.value = 0

  try {
    const result = await uploadImage(file, props.role)
    if (result.code === 200 && result.data) {
      imageUrl.value = result.data.url
      emit('update:modelValue', result.data.url)
    }
  } catch (error) {
    console.error('上传失败:', error)
    alert('图片上传失败，请重试')
    imageUrl.value = props.modelValue
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    // 清空文件输入
    event.target.value = ''
  }
}

function removeImage() {
  imageUrl.value = ''
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-upload {
  width: 100%;
}

.upload-placeholder {
  width: 100%;
  height: 200px;
  border: 2px dashed #dee2e6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: #6c757d;
}

.upload-placeholder:hover {
  border-color: #0d6efd;
  color: #0d6efd;
  background: #f8f9fa;
}

.upload-placeholder i {
  font-size: 48px;
  margin-bottom: 8px;
}

.upload-preview {
  position: relative;
  width: 100%;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
}

.upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.upload-preview:hover .preview-mask {
  opacity: 1;
}

.upload-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px;
  background: rgba(0, 0, 0, 0.7);
}

.progress {
  height: 6px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: #0d6efd;
  transition: width 0.3s;
}
</style>
