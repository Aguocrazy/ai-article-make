<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { articleCount, articleTitle, articles, articlesError, articlesLoading, loadMyArticles } from '@/stores/articles'

const router = useRouter()

const STATUS_LABEL: Record<string, string> = {
  PENDING: '排队中',
  PROCESSING: '生成中',
  COMPLETED: '已完成',
  FAILED: '失败',
}

function openArticle(id: number) {
  void router.push({ name: 'write', query: { article: String(id) } })
}

function formatTime(value?: string) {
  if (!value) {
    return '—'
  }
  return new Date(value).toLocaleString()
}

onMounted(() => {
  void loadMyArticles()
})
</script>

<template>
  <section class="card users-panel">
    <h2>我的文章</h2>
    <p class="lede">保存在账号下，换设备登录后也能打开。共 {{ articleCount }} 篇。</p>
    <p v-if="articlesError" class="alert">{{ articlesError }}</p>
    <p v-else-if="articlesLoading" class="empty">正在加载…</p>
    <p v-else-if="articles.length === 0" class="empty">还没有文章。去文章创作里提交一个主题吧。</p>
    <table v-else>
      <thead>
        <tr>
          <th>标题</th>
          <th>状态</th>
          <th>更新时间</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in articles" :key="item.id">
          <td>{{ articleTitle(item) }}</td>
          <td>{{ STATUS_LABEL[item.status] || item.status }}</td>
          <td>{{ formatTime(item.updateTime || item.createTime) }}</td>
          <td>
            <button class="extra-link" type="button" @click="openArticle(item.id)">打开</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
