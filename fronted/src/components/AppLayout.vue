<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { currentUser, isAdmin, logout } from '@/stores/user'
import { draftCount, drafts } from '@/stores/studio'

const route = useRoute()
const router = useRouter()
const demo = computed(() => Boolean(route.query.demo))

async function onLogout() {
  await logout()
  await router.replace('/login')
}

function exitDemo() {
  const query = { ...route.query }
  delete query.demo
  void router.replace({ query })
}

function toggleDemo() {
  if (demo.value) {
    exitDemo()
    return
  }
  void router.replace({ query: { ...route.query, demo: '1' } })
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && demo.value) {
    exitDemo()
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="studio">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark" aria-hidden="true">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M8 14c0-3 2-5 4-7 2 2 4 4 4 7a4 4 0 1 1-8 0Z" fill="currentColor" opacity=".9" />
            <path d="M9 18h6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            <path d="M12 4v2M7.5 6.5 9 8M16.5 6.5 15 8" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
          </svg>
        </div>
        <div>
          <div class="brand-title">落笔</div>
          <div class="brand-sub">LUO BI</div>
        </div>
      </div>

      <p class="nav-label">你的创作空间</p>
      <RouterLink class="nav-item" :class="{ 'is-active': route.name === 'write' }" to="/">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M5 20h14M7 16l9.5-9.5a1.5 1.5 0 0 1 2.1 2.1L9.1 18H7v-2Z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
        </svg>
        文章创作
        <span class="nav-badge">✦</span>
      </RouterLink>
      <RouterLink class="nav-item" :class="{ 'is-active': route.name === 'drafts' }" to="/drafts">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M7 4h7l4 4v12H7V4Z" stroke="currentColor" stroke-width="1.7" />
          <path d="M14 4v4h4" stroke="currentColor" stroke-width="1.7" />
        </svg>
        我的草稿
        <span class="nav-badge">{{ draftCount }}</span>
      </RouterLink>
      <RouterLink v-if="isAdmin" class="nav-item" :class="{ 'is-active': route.name === 'users' }" to="/users">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="8" r="3" stroke="currentColor" stroke-width="1.7" />
          <path d="M5 19c1.2-3 3.4-4.5 7-4.5S17.8 16 19 19" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
        </svg>
        用户管理
      </RouterLink>

      <div class="recent">
        <div class="recent-head">
          <span>最近创作</span>
          <span>仅本机</span>
        </div>
        <button
          v-for="item in drafts.slice(0, 4)"
          :key="item.id"
          class="recent-item"
          type="button"
          @click="router.push({ name: 'write', query: { draft: item.id } })"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M7 4h10v16H7z" stroke="currentColor" stroke-width="1.6" />
          </svg>
          <span>{{ item.title }}</span>
        </button>
        <p v-if="drafts.length === 0" class="recent-item" style="cursor: default">还没有草稿</p>
      </div>

      <div class="inspire-card">
        <div class="brand-mark" style="width: 28px; height: 28px; border-radius: 9px">✦</div>
        <h3>一点灵感，无限可能</h3>
        <p>不必等到灵感完美，先把脑海中的想法写下来。</p>
        <RouterLink to="/">寻找写作灵感 →</RouterLink>
      </div>
      <div class="logout-row">
        <button type="button" @click="onLogout">退出账号</button>
        <span>
          {{ currentUser?.userName || currentUser?.userAccount }}
          ·
          <a href="https://www.pexels.com" target="_blank" rel="noopener">Photos by Pexels</a>
        </span>
      </div>
    </aside>

    <div class="workspace">
      <div v-if="demo" class="esc-bar">
        若要退出全屏模式，请按住
        <kbd>esc</kbd>
      </div>
      <div class="crumb-bar">
        <div class="crumbs">工作空间 / <b>{{ route.meta.title }}</b></div>
        <div class="crumb-actions">
          <button class="pill" type="button" @click="toggleDemo">
            <span style="width: 16px; height: 10px; border-radius: 999px; background: #ead7a8; display: inline-block" />
            演示模式
          </button>
          <button class="icon-btn" type="button" title="帮助">?</button>
          <button class="icon-btn" type="button" title="落笔">笔</button>
        </div>
      </div>
      <router-view />
    </div>
  </div>
</template>
