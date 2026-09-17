import { computed, reactive } from 'vue'
import { getLoginUser, userLogout } from '@/api/user'
import type { LoginUserVO } from '@/types/user'

const state = reactive<{
  user: LoginUserVO | null
  ready: boolean
}>({
  user: null,
  ready: false,
})

export const currentUser = computed(() => state.user)
export const isReady = computed(() => state.ready)
export const isLoggedIn = computed(() => state.user != null)
export const isAdmin = computed(() => state.user?.userRole === 'admin')

export function setLoginUser(user: LoginUserVO | null) {
  state.user = user
  state.ready = true
}

export async function fetchLoginUser() {
  try {
    state.user = await getLoginUser()
  } catch {
    state.user = null
  } finally {
    state.ready = true
  }
}

export async function logout() {
  await userLogout()
  state.user = null
}
