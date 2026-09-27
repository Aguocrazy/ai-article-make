import { computed, reactive } from 'vue'
import { listMyArticles, type ArticleVO } from '@/api/article'

const state = reactive({
  items: [] as ArticleVO[],
  total: 0,
  loading: false,
  error: '',
})

export const articles = computed(() => state.items)
export const articleCount = computed(() => state.total)
export const articlesLoading = computed(() => state.loading)
export const articlesError = computed(() => state.error)

export async function loadMyArticles(pageSize = 20) {
  state.loading = true
  state.error = ''
  try {
    const page = await listMyArticles({ current: 1, pageSize })
    state.items = page.records ?? []
    state.total = page.totalRow ?? state.items.length
  } catch (error) {
    state.error = error instanceof Error ? error.message : '加载文章失败'
    state.items = []
    state.total = 0
  } finally {
    state.loading = false
  }
}

export function articleTitle(item: ArticleVO) {
  return item.mainTitle || item.topic
}
