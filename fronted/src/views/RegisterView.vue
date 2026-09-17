<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userLogin, userRegister } from '@/api/user'
import { setLoginUser } from '@/stores/user'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

async function onSubmit() {
  error.value = ''
  if (form.userAccount.length < 4 || form.userAccount.length > 256) {
    error.value = '账号长度必须在 4 到 256 位之间'
    return
  }
  if (form.userPassword.length < 8 || form.userPassword.length > 512) {
    error.value = '密码长度必须在 8 到 512 位之间'
    return
  }
  if (form.userPassword !== form.checkPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    await userRegister({ ...form })
    const user = await userLogin({
      userAccount: form.userAccount,
      userPassword: form.userPassword,
    })
    setLoginUser(user)
    await router.replace('/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-shell">
    <section class="auth-card">
      <div class="brand" style="padding: 0 0 8px">
        <div class="brand-mark">✦</div>
        <div>
          <div class="brand-title">落笔</div>
          <div class="brand-sub">LUO BI</div>
        </div>
      </div>
      <p class="kicker">A NEW LAMP, A NEW PAGE</p>
      <h1>先把账号点亮。</h1>
      <p class="lede">账号 4～256 位，密码 8～512 位。注册成功后会直接进入创作台。</p>
      <p v-if="error" class="alert">{{ error }}</p>
      <form @submit.prevent="onSubmit">
        <div class="field">
          <label for="account">账号</label>
          <input id="account" v-model.trim="form.userAccount" class="soft-input" autocomplete="username" />
        </div>
        <div class="field">
          <label for="password">密码</label>
          <input id="password" v-model="form.userPassword" class="soft-input" type="password" autocomplete="new-password" />
        </div>
        <div class="field">
          <label for="check">确认密码</label>
          <input id="check" v-model="form.checkPassword" class="soft-input" type="password" autocomplete="new-password" />
        </div>
        <button class="generate" :disabled="loading">{{ loading ? '提交中…' : '注册并进入' }}</button>
      </form>
      <p class="hint">已有账号？<router-link to="/login">去登录</router-link></p>
    </section>
  </main>
</template>
