import { computed, reactive } from 'vue'

export interface Draft {
  id: string
  title: string
  topic: string
  type: string
  tone: string
  length: number
  audience: string
  extra: string
  markdown: string
  updatedAt: number
}

const KEY = 'luo-bi-drafts'

function read(): Draft[] {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as Draft[]) : []
  } catch {
    return []
  }
}

const state = reactive({
  drafts: read(),
})

function persist() {
  localStorage.setItem(KEY, JSON.stringify(state.drafts))
}

export const drafts = computed(() => state.drafts)
export const draftCount = computed(() => state.drafts.length)

export function saveDraft(draft: Draft) {
  const next = state.drafts.filter((item) => item.id !== draft.id)
  next.unshift(draft)
  state.drafts = next.slice(0, 20)
  persist()
}

export function removeDraft(id: string) {
  state.drafts = state.drafts.filter((item) => item.id !== id)
  persist()
}
