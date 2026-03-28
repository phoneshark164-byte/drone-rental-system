<template>
  <div class="ai-chat-widget" :class="{ 'dragging': isDragging }" :style="{ transform: `translate(${widgetPosition.x}px, ${widgetPosition.y}px)` }">
    <!-- 聊天按钮 -->
    <div class="chat-toggle" :class="{ 'active': isOpen }" @pointerdown="handlePointerDown">
      <img src="/kefu/01.png" alt="AI助手" class="drone-icon" draggable="false" />
      <span v-if="!isOpen" class="tooltip">AI助手</span>
      <span v-if="unreadCount > 0 && !isOpen" class="badge">{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
    </div>

    <!-- 聊天面板 -->
    <div class="chat-panel" :class="{ 'open': isOpen }">
      <!-- 面板头部 -->
      <div class="panel-header">
        <div class="header-left">
          <div class="ai-avatar-small">
            <img src="/kefu/01.png" alt="AI助手" />
          </div>
          <div>
            <h6 class="mb-0">AI 助手</h6>
            <small class="status-text">
              <span class="status-dot"></span>
              在线
            </small>
          </div>
        </div>
        <button class="close-btn" @click="toggleChat">
          <i class="bi bi-x-lg"></i>
        </button>
      </div>

      <!-- 聊天内容区域 -->
      <div class="panel-body" ref="messagesContainer">
        <!-- 欢迎消息 -->
        <div v-if="messages.length === 0" class="welcome-section">
          <div class="welcome-avatar">
            <img src="/kefu/01.png" alt="AI助手" />
          </div>
          <div class="welcome-bubble">
            <p>你好！我是AI助手</p>
            <p>我可以帮你：</p>
            <ul>
              <li>识别无人机图片</li>
              <li>推荐合适机型</li>
              <li>解答使用问题</li>
            </ul>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-for="msg in messages" :key="msg.id" class="message" :class="msg.role">
          <div v-if="msg.role === 'assistant'" class="message-avatar assistant">
            <img src="/kefu/01.png" alt="AI助手" />
          </div>
          <div v-else class="message-avatar user">
            <i class="bi bi-person"></i>
          </div>

          <div class="message-bubble">
            <div v-if="msg.image" class="message-image">
              <img :src="msg.image" alt="上传的图片" />
            </div>
            <div class="message-text" v-html="formatMessage(msg.content)"></div>
          </div>
        </div>

        <!-- 加载动画 -->
        <div v-if="loading" class="message assistant">
          <div class="message-avatar assistant">
            <img src="/kefu/01.png" alt="AI助手" />
          </div>
          <div class="message-bubble">
            <div class="typing-indicator">
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 图片预览 -->
      <div v-if="previewImage" class="image-preview-area">
        <img :src="previewImage" alt="预览" />
        <button class="remove-preview" @click="removeImage">
          <i class="bi bi-x"></i>
        </button>
      </div>

      <!-- 输入区域 -->
      <div class="panel-footer">
        <div class="input-toolbar">
          <button class="tool-btn" @click="selectImage" title="上传图片">
            <i class="bi bi-image"></i>
          </button>
          <input
            type="file"
            ref="imageInput"
            accept="image/*"
            @change="handleImageSelect"
            style="display: none"
          />
        </div>
        <div class="input-wrapper">
          <textarea
            v-model="userInput"
            class="chat-input"
            placeholder="输入问题..."
            rows="1"
            @keydown.enter.exact.prevent="sendMessage"
            ref="textareaRef"
          ></textarea>
          <button
            class="send-btn"
            @click="sendMessage"
            :disabled="!userInput.trim() && !previewImage"
          >
            <i class="bi bi-send-fill"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'

const isOpen = ref(false)
const messages = ref([])
const userInput = ref('')
const loading = ref(false)
const previewImage = ref(null)
const imageInput = ref(null)
const messagesContainer = ref(null)
const textareaRef = ref(null)
const unreadCount = ref(0)

// 拖动相关
const widgetPosition = ref({ x: 0, y: 0 })
const isDragging = ref(false)
const dragStartPos = ref({ x: 0, y: 0 })
const hasMoved = ref(false)

let messageIdCounter = 0

// 处理指针按下事件（统一处理鼠标和触摸）
const handlePointerDown = (e) => {
  // 只响应主按钮（左键或触摸）
  if (e.button !== 0 && e.button !== undefined) return

  hasMoved.value = false
  isDragging.value = true

  const clientX = e.clientX
  const clientY = e.clientY

  dragStartPos.value = {
    x: clientX - widgetPosition.value.x,
    y: clientY - widgetPosition.value.y,
    startX: clientX,
    startY: clientY
  }

  window.addEventListener('pointermove', onDrag)
  window.addEventListener('pointerup', stopDrag)
  window.addEventListener('pointercancel', stopDrag)
}

// 拖动中
const onDrag = (e) => {
  if (!isDragging.value) return

  e.preventDefault()

  const clientX = e.clientX
  const clientY = e.clientY

  const newX = clientX - dragStartPos.value.x
  const newY = clientY - dragStartPos.value.y

  // 检测是否有明显移动（从初始位置计算）
  if (Math.abs(clientX - dragStartPos.value.startX) > 5 ||
      Math.abs(clientY - dragStartPos.value.startY) > 5) {
    hasMoved.value = true
  }

  // 元素从右下角定位，需要限制范围
  // 初始位置是 bottom: 20px, right: 20px
  // translate(x, y) 中：
  //   - x > 0 向左移动, x < 0 向右移动
  //   - y > 0 向上移动, y < 0 向下移动
  const minX = -(window.innerWidth - 120)  // 允许向右移动
  const maxX = window.innerWidth - 120      // 允许向左移动
  const minY = -(window.innerHeight - 120) // 允许向下移动
  const maxY = window.innerHeight - 120    // 允许向上移动

  widgetPosition.value = {
    x: Math.max(minX, Math.min(newX, maxX)),
    y: Math.max(minY, Math.min(newY, maxY))
  }
}

// 停止拖动
const stopDrag = (e) => {
  const wasDragging = isDragging.value
  const didMove = hasMoved.value

  isDragging.value = false

  // 清理事件监听
  window.removeEventListener('pointermove', onDrag)
  window.removeEventListener('pointerup', stopDrag)
  window.removeEventListener('pointercancel', stopDrag)

  // 如果没有明显移动，视为点击，切换聊天面板
  if (wasDragging && !didMove) {
    toggleChat()
  }
}

onUnmounted(() => {
  window.removeEventListener('pointermove', onDrag)
  window.removeEventListener('pointerup', stopDrag)
  window.removeEventListener('pointercancel', stopDrag)
})

const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    unreadCount.value = 0
    nextTick(() => scrollToBottom())
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const formatMessage = (content) => {
  if (!content) return ''
  return content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

const selectImage = () => {
  imageInput.value?.click()
}

const handleImageSelect = (event) => {
  const file = event.target.files[0]
  if (file) {
    if (file.size > 5 * 1024 * 1024) {
      alert('图片大小不能超过5MB')
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      previewImage.value = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

const removeImage = () => {
  previewImage.value = null
  if (imageInput.value) {
    imageInput.value.value = ''
  }
}

const sendMessage = async () => {
  if (!userInput.value.trim() && !previewImage.value) return
  if (loading.value) return

  const imageBase64 = previewImage.value
  const text = userInput.value.trim()

  // 添加用户消息
  const userMessage = {
    id: ++messageIdCounter,
    role: 'user',
    content: text,
    image: imageBase64,
    timestamp: new Date().toISOString()
  }
  messages.value.push(userMessage)

  userInput.value = ''
  previewImage.value = null
  if (imageInput.value) {
    imageInput.value.value = ''
  }

  if (!isOpen.value) {
    isOpen.value = true
  }
  scrollToBottom()
  loading.value = true

  try {
    const res = await request.post('/user/api/assistant/chat2', {
      message: text,
      image: imageBase64
    })

    if (res.code === 200) {
      const assistantMessage = {
        id: ++messageIdCounter,
        role: 'assistant',
        content: res.data.reply,
        timestamp: new Date().toISOString()
      }
      messages.value.push(assistantMessage)
    } else {
      throw new Error(res.message || '请求失败')
    }
  } catch (error) {
    console.error('AI请求失败:', error)
    messages.value.push({
      id: ++messageIdCounter,
      role: 'assistant',
      content: '抱歉，AI助手暂时无法响应。请稍后再试。',
      timestamp: new Date().toISOString()
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  // 自适应文本框高度
  if (textareaRef.value) {
    textareaRef.value.addEventListener('input', function() {
      this.style.height = 'auto'
      this.style.height = Math.min(this.scrollHeight, 100) + 'px'
    })
  }
})
</script>

<style scoped>
.ai-chat-widget {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 9999;
  transition: transform 0.1s ease-out;
}

.ai-chat-widget.dragging {
  transition: none;
}

.ai-chat-widget.dragging .chat-toggle {
  cursor: grabbing;
}

/* 聊天按钮 */
.chat-toggle {
  width: 100px;
  height: 100px;
  cursor: grab;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
  user-select: none;
  touch-action: none;
}

.chat-toggle .drone-icon {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.chat-toggle:active {
  cursor: grabbing;
}

.chat-toggle:hover {
  transform: scale(1.1);
}

.chat-toggle.active {
  /* 移除旋转效果 */
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}

.tooltip {
  position: absolute;
  right: 70px;
  background: #333;
  color: white;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 14px;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s;
}

.tooltip::after {
  content: ' (可拖动)';
  font-size: 11px;
  opacity: 0.7;
}

.chat-toggle:hover .tooltip {
  opacity: 1;
}

.chat-toggle .badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #ef4444;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 11px;
  font-weight: 600;
  min-width: 18px;
  text-align: center;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

/* 聊天面板 */
.chat-panel {
  position: absolute;
  bottom: 80px;
  right: 0;
  width: 380px;
  height: 500px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  opacity: 0;
  visibility: hidden;
  transform: translateY(20px) scale(0.95);
  transition: all 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

.chat-panel.open {
  opacity: 1;
  visibility: visible;
  transform: translateY(0) scale(1);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  border-radius: 20px 20px 0 0;
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-avatar-small {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: transparent;
}

.ai-avatar-small img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.header-left h6 {
  font-size: 16px;
  font-weight: 600;
}

.status-text {
  font-size: 12px;
  opacity: 0.9;
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #10b981;
  border-radius: 50%;
  animation: blink 2s infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 聊天内容 */
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f8f9fa;
}

.welcome-section {
  text-align: center;
  padding: 20px;
}

.welcome-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  margin: 0 auto 12px;
  overflow: hidden;
  background: transparent;
}

.welcome-avatar img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.welcome-bubble {
  background: white;
  border-radius: 12px;
  padding: 16px;
  display: inline-block;
  text-align: left;
}

.welcome-bubble p {
  margin: 4px 0;
  font-size: 14px;
  color: #666;
}

.welcome-bubble ul {
  margin: 12px 0 0 20px;
  padding: 0;
}

.welcome-bubble li {
  font-size: 13px;
  color: #666;
  margin: 6px 0;
}

.message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  animation: messageIn 0.3s ease;
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.message-avatar.assistant {
  background: transparent;
  color: white;
}

.message-avatar.assistant img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.message-avatar.user {
  background: #e0e0e0;
  color: #666;
}

.message-bubble {
  max-width: 75%;
}

.message.assistant .message-bubble {
  margin-left: 0;
}

.message.user .message-bubble {
  margin-left: auto;
}

.message-image {
  margin-bottom: 6px;
}

.message-image img {
  max-width: 150px;
  max-height: 120px;
  border-radius: 8px;
  object-fit: cover;
}

.message-text {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
}

.message.assistant .message-text {
  background: white;
  color: #333;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message.user .message-text {
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: white;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: white;
  border-radius: 12px;
  width: fit-content;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background: #ccc;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-8px);
  }
}

/* 图片预览 */
.image-preview-area {
  padding: 8px 16px;
  background: white;
  border-top: 1px solid #eee;
  position: relative;
}

.image-preview-area img {
  max-height: 80px;
  border-radius: 8px;
}

.remove-preview {
  position: absolute;
  top: 8px;
  right: 16px;
  width: 24px;
  height: 24px;
  border: none;
  background: rgba(239, 68, 68, 0.9);
  color: white;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 输入区域 */
.panel-footer {
  padding: 12px 16px;
  background: white;
  border-top: 1px solid #eee;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.tool-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 8px;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.tool-btn:hover {
  background: #e0e0e0;
  color: #333;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: #f5f7fa;
  border-radius: 12px;
  padding: 8px 12px;
}

.chat-input {
  flex: 1;
  border: none;
  background: transparent;
  resize: none;
  font-size: 14px;
  max-height: 60px;
  outline: none;
  font-family: inherit;
}

.send-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: white;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 480px) {
  .chat-panel {
    width: calc(100vw - 40px);
    right: -20px;
    bottom: 70px;
  }

  .chat-toggle {
    width: 80px;
    height: 80px;
  }
}
</style>
