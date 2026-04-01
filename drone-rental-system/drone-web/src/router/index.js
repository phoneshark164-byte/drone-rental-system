import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // 用户端路由
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/user/Index.vue'),
    meta: { title: '首页 - 无人机智能租赁' }
  },
  {
    path: '/user/login',
    name: 'UserLogin',
    component: () => import('@/views/user/Login.vue'),
    meta: { title: '用户登录 - 无人机智能租赁' }
  },
  {
    path: '/user/register',
    name: 'UserRegister',
    component: () => import('@/views/user/Register.vue'),
    meta: { title: '用户注册 - 无人机智能租赁' }
  },
  {
    path: '/user/vehicles',
    name: 'UserVehicles',
    component: () => import('@/views/user/VehicleList.vue'),
    meta: { title: '无人机列表 - 无人机智能租赁', requiresAuth: true }
  },
  {
    path: '/user/vehicle/:id',
    name: 'UserVehicleDetail',
    component: () => import('@/views/user/VehicleDetail.vue'),
    meta: { title: '无人机详情 - 无人机智能租赁', requiresAuth: true }
  },
  {
    path: '/user/orders',
    name: 'UserOrders',
    component: () => import('@/views/user/Orders.vue'),
    meta: { title: '我的订单 - 无人机智能租赁', requiresAuth: true }
  },
  {
    path: '/user/order/:id',
    name: 'UserOrderDetail',
    component: () => import('@/views/user/OrderDetail.vue'),
    meta: { title: '订单详情 - 无人机智能租赁', requiresAuth: true }
  },
  {
    path: '/user/repairs',
    name: 'UserRepairs',
    component: () => import('@/views/user/Repairs.vue'),
    meta: { title: '我的报修 - 无人机智能租赁', requiresAuth: true }
  },
  {
    path: '/user/detection',
    name: 'UserDetection',
    component: () => import('@/views/user/Detection.vue'),
    meta: { title: 'AI 损伤检测 - 无人机智能租赁', requiresAuth: true }
  },
  {
    path: '/user/recommendation',
    name: 'UserRecommendation',
    component: () => import('@/views/user/Recommendation.vue'),
    meta: { title: '智能推荐 - 无人机智能租赁', requiresAuth: true }
  },
  {
    path: '/user/assistant',
    name: 'UserAssistant',
    component: () => import('@/views/user/Assistant.vue'),
    meta: { title: 'AI 多模态助手 - 无人机智能租赁', requiresAuth: true }
  },
  {
    path: '/user/digital-twin',
    name: 'UserDigitalTwin',
    component: () => import('@/views/user/DigitalTwin.vue'),
    meta: { title: '智慧农场 - 无人机智能租赁' }
  },
  {
    path: '/user/profile',
    name: 'UserProfile',
    component: () => import('@/views/user/Profile.vue'),
    meta: { title: '个人中心 - 无人机智能租赁', requiresAuth: true }
  },
  // 管理员端路由
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Index.vue'),
        meta: { title: '管理后台 - 无人机智能租赁' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理 - 无人机智能租赁' }
      },
      {
        path: 'vehicles',
        name: 'AdminVehicles',
        component: () => import('@/views/admin/Vehicles.vue'),
        meta: { title: '无人机管理 - 无人机智能租赁' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/Orders.vue'),
        meta: { title: '订单管理 - 无人机智能租赁' }
      },
      {
        path: 'reviews',
        name: 'AdminReviews',
        component: () => import('@/views/admin/Reviews.vue'),
        meta: { title: '评价管理 - 无人机智能租赁' }
      },
      {
        path: 'banners',
        name: 'AdminBanners',
        component: () => import('@/views/admin/Banners.vue'),
        meta: { title: '轮播图管理 - 无人机智能租赁' }
      },
      {
        path: 'recharges',
        name: 'AdminRecharges',
        component: () => import('@/views/admin/Recharges.vue'),
        meta: { title: '充值记录 - 无人机智能租赁' }
      },
      {
        path: 'payments',
        name: 'AdminPayments',
        component: () => import('@/views/admin/Payments.vue'),
        meta: { title: '支付记录 - 无人机智能租赁' }
      },
      {
        path: 'order/:id',
        name: 'AdminOrderDetail',
        component: () => import('@/views/admin/OrderDetail.vue'),
        meta: { title: '订单详情 - 无人机智能租赁' }
      },
      {
        path: 'detections',
        name: 'AdminDetections',
        component: () => import('@/views/admin/Detections.vue'),
        meta: { title: 'AI检测记录 - 无人机智能租赁' }
      },
      {
        path: 'notifications',
        name: 'AdminNotifications',
        component: () => import('@/views/admin/Notifications.vue'),
        meta: { title: '系统通知 - 无人机智能租赁' }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/Logs.vue'),
        meta: { title: '操作日志 - 无人机智能租赁' }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/Settings.vue'),
        meta: { title: '系统设置 - 无人机智能租赁' }
      }
    ]
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/Login.vue'),
    meta: { title: '管理员登录 - 无人机智能租赁' }
  },
  // 运营端路由
  {
    path: '/operator',
    name: 'OperatorLayout',
    component: () => import('@/views/operator/Layout.vue'),
    meta: { requiresAuth: true, role: 'operator' },
    children: [
      {
        path: '',
        name: 'OperatorDashboard',
        component: () => import('@/views/operator/Index.vue'),
        meta: { title: '运营后台 - 无人机智能租赁' }
      },
      {
        path: 'vehicles',
        name: 'OperatorVehicles',
        component: () => import('@/views/operator/Vehicles.vue'),
        meta: { title: '无人机管理 - 无人机智能租赁' }
      },
      {
        path: 'orders',
        name: 'OperatorOrders',
        component: () => import('@/views/operator/Orders.vue'),
        meta: { title: '订单管理 - 无人机智能租赁' }
      },
      {
        path: 'order/:id',
        name: 'OperatorOrderDetail',
        component: () => import('@/views/operator/OrderDetail.vue'),
        meta: { title: '订单详情 - 无人机智能租赁' }
      },
      {
        path: 'repairs',
        name: 'OperatorRepairs',
        component: () => import('@/views/operator/Repairs.vue'),
        meta: { title: '故障报修 - 无人机智能租赁' }
      }
    ]
  },
  {
    path: '/operator/login',
    name: 'OperatorLogin',
    component: () => import('@/views/operator/Login.vue'),
    meta: { title: '运营员登录 - 无人机智能租赁' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '无人机智能租赁系统'

  const token = localStorage.getItem('token')
  const userRole = localStorage.getItem('userRole')

  // 如果是登录页面，直接放行
  if (to.path.includes('/login')) {
    next()
    return
  }

  if (to.meta.requiresAuth && !token) {
    // 需要登录但未登录 - 根据目标路由的角色跳转到对应的登录页
    if (to.meta.role === 'admin') {
      next('/admin/login')
    } else if (to.meta.role === 'operator') {
      next('/operator/login')
    } else {
      next('/user/login')
    }
  } else if (to.meta.role && to.meta.role !== userRole) {
    // 角色不匹配 - 已登录但角色不对，跳转到对应的登录页
    if (to.meta.role === 'admin') {
      next('/admin/login')
    } else if (to.meta.role === 'operator') {
      next('/operator/login')
    } else {
      next('/user/login')
    }
  } else {
    next()
  }
})

export default router
