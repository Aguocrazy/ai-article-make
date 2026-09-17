import { createRouter, createWebHistory } from 'vue-router'
import { fetchLoginUser, isAdmin, isLoggedIn, isReady } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true, title: '登录' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guest: true, title: '注册' },
    },
    {
      path: '/',
      component: () => import('@/components/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'write',
          component: () => import('@/views/WriteView.vue'),
          meta: { title: '文章创作' },
        },
        {
          path: 'drafts',
          name: 'drafts',
          component: () => import('@/views/DraftsView.vue'),
          meta: { title: '我的草稿' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UsersView.vue'),
          meta: { title: '用户管理', requiresAdmin: true },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  if (!isReady.value) {
    await fetchLoginUser()
  }
  if (to.meta.requiresAuth && !isLoggedIn.value) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && isLoggedIn.value) {
    return { name: 'write' }
  }
  if (to.meta.requiresAdmin && !isAdmin.value) {
    return { name: 'write' }
  }
  return true
})

export default router
