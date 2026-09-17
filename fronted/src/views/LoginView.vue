<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { userLogin } from '@/api/user'
import { setLoginUser } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const error = ref('')
const form = reactive({
  userAccount: '',
  userPassword: '',
})

async function onSubmit() {
  error.value = ''
  if (form.userAccount.length < 4) {
    error.value = '账号至少 4 位'
    return
  }
  if (form.userPassword.length < 8) {
    error.value = '密码至少 8 位'
    return
  }
  loading.value = true
  try {
    const user = await userLogin({ ...form })
    setLoginUser(user)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-shell">
    <section class="auth-card">
      <div class="brand" style="padding: 0 0 8px">
        <div class="brand-mark">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M8 14c0-3 2-5 4-7 2 2 4 4 4 7a4 4 0 1 1-8 0Z" fill="currentColor" />
          </svg>
        </div>
        <div>
          <div class="brand-title">落笔</div>
          <div class="brand-sub">LUO BI</div>
        </div>
      </div>
      <p class="kicker">FROM A SPARK TO A STORY</p>
      <h1>点亮你的下一篇。</h1>
      <p class="lede">登录后进入创作台。登录态写在 Session 里，刷新也还在。</p>
      <p v-if="error" class="alert">{{ error }}</p>
      <form @submit.prevent="onSubmit">
        <div class="field">
          <label for="account">账号</label>
          <input id="account" v-model.trim="form.userAccount" class="soft-input" autocomplete="username" />
        </div>
        <div class="field">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="form.userPassword"
            class="soft-input"
            type="password"
            autocomplete="current-password"
          />
        </div>
        <button class="generate" :disabled="loading">{{ loading ? '登录中…' : '进入落笔' }}</button>
      </form>
      <p class="hint">还没有账号？<router-link to="/register">去注册</router-link></p>
    </section>
  </main>
</template>
