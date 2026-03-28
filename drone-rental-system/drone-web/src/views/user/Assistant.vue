<template>
  <div class="ai-assistant-page">
    <UserHeader />

    <div class="assistant-container">
      <!-- 左侧：对话区域 -->
      <div class="chat-section">
        <div class="chat-header">
          <div class="header-info">
            <div class="ai-avatar">
              <i class="bi bi-robot"></i>
            </div>
            <div>
              <h4 class="mb-0">AI 多模态助手</h4>
              <small class="text-muted">支持图像识别、智能问答</small>
            </div>
          </div>
          <button class="btn btn-sm btn-outline-secondary" @click="clearChat">
            <i class="bi bi-trash me-1"></i>清空对话
          </button>
        </div>

        <div class="chat-messages" ref="messagesContainer">
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="welcome-icon">
              <i class="bi bi-chat-square-text"></i>
            </div>
            <h5>你好！我是 AI 多模态助手</h5>
            <p>我可以帮助你：</p>
            <ul class="feature-list">
              <li><i class="bi bi-image"></i> 识别无人机图片中的内容</li>
              <li><i class="bi bi-search"></i> 分析损伤情况</li>
              <li><i class="bi bi-info-circle"></i> 回答无人机相关问题</li>
              <li><i class="bi bi-tools"></i> 提供使用建议</li>
            </ul>
            <p class="text-muted">上传图片或输入问题开始对话</p>
          </div>

          <div v-for="msg in messages" :key="msg.id" class="message" :class="msg.role">
            <div v-if="msg.role === 'assistant'" class="message-avatar assistant">
              <i class="bi bi-robot"></i>
            </div>
            <div v-else class="message-avatar user">
              <img v-if="userInfo?.avatar" :src="userInfo.avatar" alt="用户" />
              <i v-else class="bi bi-person"></i>
            </div>

            <div class="message-content">
              <div v-if="msg.image" class="message-image">
                <img :src="msg.image" alt="上传的图片" />
              </div>
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
              <div v-if="msg.role === 'assistant'" class="message-time">
                {{ formatTime(msg.timestamp) }}
              </div>
            </div>
          </div>

          <div v-if="loading" class="message assistant">
            <div class="message-avatar assistant">
              <i class="bi bi-robot"></i>
            </div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input-section">
          <!-- 图片预览 -->
          <div v-if="previewImage" class="image-preview">
            <img :src="previewImage" alt="预览" />
            <button class="remove-image" @click="removeImage">
              <i class="bi bi-x"></i>
            </button>
          </div>

          <div class="input-wrapper">
            <button class="input-action-btn" @click="selectImage" title="上传图片">
              <i class="bi bi-image"></i>
            </button>
            <input
              type="file"
              ref="imageInput"
              accept="image/*"
              @change="handleImageSelect"
              style="display: none"
            />
            <textarea
              v-model="userInput"
              class="chat-input"
              placeholder="输入问题或上传图片..."
              rows="1"
              @keydown.enter.exact.prevent="sendMessage"
              ref="textareaRef"
            ></textarea>
            <button class="send-btn" @click="sendMessage" :disabled="!userInput.trim() && !previewImage">
              <i class="bi bi-send-fill"></i>
            </button>
          </div>

          <!-- 快捷问题 -->
          <div class="quick-questions" v-if="messages.length === 0">
            <span
              v-for="q in quickQuestions"
              :key="q"
              class="quick-question-btn"
              @click="askQuickQuestion(q)"
            >
              {{ q }}
            </span>
          </div>
        </div>
      </div>

      <!-- 右侧：功能面板 -->
      <div class="features-panel">
        <div class="panel-section">
          <h6><i class="bi bi-lightbulb me-2"></i>使用技巧</h6>
          <ul class="tips-list">
            <li>上传无人机损伤照片，我可以帮你分析损坏程度</li>
            <li>发送无人机型号，我可以告诉你参数和适用场景</li>
            <li>描述使用场景，我可以推荐合适的无人机</li>
          </ul>
        </div>

        <div class="panel-section">
          <h6><i class="bi bi-clock-history me-2"></i>对话历史</h6>
          <div v-if="chatHistory.length > 0" class="history-list">
            <div
              v-for="(item, index) in chatHistory"
              :key="index"
              class="history-item"
              @click="loadHistory(index)"
            >
              <i class="bi bi-chat-left-text"></i>
              <span>{{ item.title || '新对话' }}</span>
              <small>{{ formatTime(item.timestamp) }}</small>
            </div>
          </div>
          <p v-else class="text-muted text-center">暂无历史记录</p>
        </div>

        <div class="panel-section model-info">
          <h6><i class="bi bi-cpu me-2"></i>AI 模型</h6>
          <div class="model-status">
            <span class="status-dot online"></span>
            <span>多模态大模型</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import UserHeader from '@/components/UserHeader.vue'
import { getUserInfo } from '@/utils/auth'

const router = useRouter()
const userInfo = ref(getUserInfo())

const messages = ref([])
const userInput = ref('')
const loading = ref(false)
const previewImage = ref(null)
const imageInput = ref(null)
const messagesContainer = ref(null)
const textareaRef = ref(null)
const chatHistory = ref([])

const quickQuestions = [
  '如何选择合适的无人机？',
  '无人机损坏了怎么办？',
  '押金如何退还？',
  '帮我分析这张图片'
]

let messageIdCounter = 0

const formatMessage = (content) => {
  if (!content) return ''
  // 简单的markdown格式转换
  return content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
    .replace(/• /g, '• ')
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
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

  scrollToBottom()
  loading.value = true

  try {
    const res = await request.post('/user/api/assistant/chat', {
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

const askQuickQuestion = (question) => {
  userInput.value = question
  nextTick(() => {
    sendMessage()
  })
}

const clearChat = () => {
  if (confirm('确定要清空对话吗？')) {
    if (messages.value.length > 0) {
      // 保存到历史
      chatHistory.value.unshift({
        timestamp: new Date().toISOString(),
        messageCount: messages.value.length
      })
    }
    messages.value = []
  }
}

const loadHistory = (index) => {
  // 历史记录加载功能（简化版）
  alert('历史记录功能开发中...')
}

onMounted(() => {
  // 自适应文本框高度
  if (textareaRef.value) {
    textareaRef.value.addEventListener('input', function() {
      this.style.height = 'auto'
      this.style.height = Math.min(this.scrollHeight, 120) + 'px'
    })
  }
})
</script>

<style scoped>
.ai-assistant-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.assistant-container {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 80px 20px 20px;
  gap: 20px;
  height: calc(100vh - 80px);
}

.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.chat-header h4 {
  font-size: 16px;
  font-weight: 600;
}

.chat-header .btn-outline-secondary {
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

.chat-header .btn-outline-secondary:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8f9fa;
}

.welcome-message {
  text-align: center;
  padding: 40px 20px;
  color: #666;
}

.welcome-icon {
  font-size: 64px;
  color: #667eea;
  margin-bottom: 20px;
}

.welcome-message h5 {
  font-size: 20px;
  margin-bottom: 16px;
  color: #333;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 20px 0;
  display: inline-block;
  text-align: left;
}

.feature-list li {
  padding: 8px 0;
  font-size: 14px;
}

.feature-list li i {
  color: #667eea;
  margin-right: 8px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
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
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-avatar.assistant {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.message-avatar.user {
  background: #e0e0e0;
}

.message-avatar.user img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.message.user .message-content {
  margin-left: auto;
}

.message-image {
  margin-bottom: 8px;
}

.message-image img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  object-fit: contain;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
}

.message.assistant .message-text {
  background: white;
  color: #333;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message.user .message-text {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
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
  width: 8px;
  height: 8px;
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
    transform: translateY(-10px);
  }
}

.chat-input-section {
  padding: 16px 20px;
  background: white;
  border-top: 1px solid #eee;
}

.image-preview {
  display: inline-block;
  position: relative;
  margin-bottom: 12px;
}

.image-preview img {
  max-height: 100px;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.remove-image {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ef4444;
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: #f5f7fa;
  border-radius: 12px;
  padding: 8px 12px;
}

.input-action-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: #667eea;
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.input-action-btn:hover {
  background: rgba(102, 126, 234, 0.1);
}

.chat-input {
  flex: 1;
  border: none;
  background: transparent;
  resize: none;
  font-size: 14px;
  max-height: 120px;
  outline: none;
  font-family: inherit;
}

.send-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.quick-question-btn {
  padding: 6px 12px;
  background: #f0f0f0;
  border-radius: 20px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-question-btn:hover {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.features-panel {
  width: 300px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-section {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.panel-section h6 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.tips-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tips-list li {
  font-size: 13px;
  color: #666;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.tips-list li:last-child {
  border-bottom: none;
}

.history-list {
  max-height: 200px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.history-item:hover {
  background: #f5f7fa;
}

.history-item i {
  color: #667eea;
}

.history-item small {
  margin-left: auto;
  color: #999;
  font-size: 11px;
}

.model-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

@media (max-width: 768px) {
  .assistant-container {
    flex-direction: column;
  }

  .features-panel {
    width: 100%;
    display: none;
  }

  .message-content {
    max-width: 85%;
  }
}
</style>
