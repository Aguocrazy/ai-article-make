<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { addUser, deleteUser, listUserVOByPage } from '@/api/user'
import type { UserVO } from '@/types/user'

const error = ref('')
const loading = ref(false)
const users = ref<UserVO[]>([])
const total = ref(0)
const query = reactive({
  current: 1,
  pageSize: 10,
  userAccount: '',
  userName: '',
})
const createForm = reactive({
  userAccount: '',
  userPassword: '',
  userName: '',
  userRole: 'user',
})

async function loadUsers() {
  loading.value = true
  error.value = ''
  try {
    const page = await listUserVOByPage({
      current: query.current,
      pageSize: query.pageSize,
      userAccount: query.userAccount || undefined,
      userName: query.userName || undefined,
    })
    users.value = page.records ?? []
    total.value = page.totalRow ?? 0
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载用户失败'
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  error.value = ''
  try {
    await addUser({ ...createForm })
    createForm.userAccount = ''
    createForm.userPassword = ''
    createForm.userName = ''
    createForm.userRole = 'user'
    query.current = 1
    await loadUsers()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建失败'
  }
}

async function onDelete(user: UserVO) {
  if (!confirm(`确认删除账号 ${user.userAccount}？`)) {
    return
  }
  error.value = ''
  try {
    await deleteUser(user.id)
    await loadUsers()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '删除失败'
  }
}

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <section class="card users-panel">
    <h2>用户管理</h2>
    <p v-if="error" class="alert">{{ error }}</p>
    <form class="create-grid" @submit.prevent="onCreate">
      <input v-model.trim="createForm.userAccount" class="soft-input" placeholder="新账号" required />
      <input v-model="createForm.userPassword" class="soft-input" type="password" placeholder="初始密码" required />
      <input v-model.trim="createForm.userName" class="soft-input" placeholder="昵称（可选）" />
      <select v-model="createForm.userRole" class="soft-input">
        <option value="user">普通用户</option>
        <option value="admin">管理员</option>
      </select>
      <button class="btn-new" type="submit">创建用户</button>
    </form>
    <div class="toolbar">
      <input v-model.trim="query.userAccount" class="soft-input" placeholder="按账号筛选" />
      <input v-model.trim="query.userName" class="soft-input" placeholder="按昵称筛选" />
      <button class="btn-new" type="button" @click="query.current = 1; loadUsers()">查询</button>
    </div>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>账号</th>
          <th>昵称</th>
          <th>角色</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in users" :key="user.id">
          <td>{{ user.id }}</td>
          <td>{{ user.userAccount }}</td>
          <td>{{ user.userName }}</td>
          <td>{{ user.userRole }}</td>
          <td>
            <button class="danger" type="button" @click="onDelete(user)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-if="!loading && users.length === 0" class="empty">没有匹配的用户</p>
    <div class="pager">
      <span>共 {{ total }} 人</span>
      <div>
        <button class="btn-new" :disabled="query.current <= 1" @click="query.current -= 1; loadUsers()">
          上一页
        </button>
        <button
          class="btn-new"
          :disabled="query.current * query.pageSize >= total"
          @click="query.current += 1; loadUsers()"
        >
          下一页
        </button>
      </div>
    </div>
  </section>
</template>
