<template>
  <router-view />
  <!-- 客服悬浮窗 - 仅在用户页面或未登录时显示 -->
  <AIChatWidget v-if="showCustomerService" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AIChatWidget from '@/components/AIChatWidget.vue'

const route = useRoute()

// 只在用户页面或未登录时显示客服
const showCustomerService = computed(() => {
  const path = route.path
  // 用户页面路径: /, /user/*, /user/login, /user/register
  const isUserPage = path === '/' || path.startsWith('/user/')
  // 管理员和运营方页面不显示
  return isUserPage
})
</script>

<style>
:root {
  --primary-color: #3b82f6;
  --accent-color: #06b6d4;
  --success-color: #10b981;
  --warning-color: #f59e0b;
  --danger-color: #ef4444;
  --secondary-color: #1e293b;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f8f9fa;
  color: #333;
}

a {
  text-decoration: none;
  color: inherit;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border: none;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #2563eb, #0891b2);
}
</style>
