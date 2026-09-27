import { get, post } from '@/request'
import type { PageResult } from '@/types/user'

export interface ArticleTask {
  taskId: string
}

export interface ArticleSseEvent<T = unknown> {
  taskId: string
  type: string
  data: T
  timestamp: number
}

export interface ArticleVO {
  id: string
  taskId: string
  topic: string
  articleType?: string
  writingTone?: string
  wordCount?: number
  audience?: string
  extraRequirement?: string
  mainTitle?: string
  subTitle?: string
  coverImage?: string
  status: string
  createTime?: string
  completedTime?: string
  updateTime?: string
}

export interface ArticleDetailVO extends ArticleVO {
  outline?: string
  content?: string
  fullContent?: string
  images?: string
  errorMessage?: string
}

export function createArticle(payload: {
  topic: string
  articleType: string
  writingTone: string
  wordCount: number
  audience: string
  extraRequirement: string
}) {
  return post<ArticleTask>('/article/create', payload)
}

export function listMyArticles(payload: {
  current?: number
  pageSize?: number
  topic?: string
  status?: string
}) {
  return post<PageResult<ArticleVO>>('/article/list/page/vo', payload)
}

export function getMyArticle(id: string) {
  return get<ArticleDetailVO>(`/article/get/${encodeURIComponent(id)}`)
}

export function deleteMyArticle(id: string) {
  return post<boolean>('/article/delete', { id })
}

export function articleStreamUrl(taskId: string) {
  return `/article/stream/${encodeURIComponent(taskId)}`
}
