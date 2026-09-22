import { post } from '@/request'

export interface ArticleTask {
  taskId: string
}

export interface ArticleSseEvent<T = unknown> {
  taskId: string
  type: string
  data: T
  timestamp: number
}

export function createArticle(topic: string) {
  return post<ArticleTask>('/article/create', { topic })
}

export function articleStreamUrl(taskId: string) {
  return `/article/stream/${encodeURIComponent(taskId)}`
}
