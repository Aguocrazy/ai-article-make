<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  articleStreamUrl,
  createArticle,
  type ArticleSseEvent,
} from '@/api/article'
import { drafts, saveDraft, type Draft } from '@/stores/studio'

const STAGE_LABELS: Record<string, string> = {
  AGENT1_COMPLETE: '标题生成完成',
  AGENT2_STREAMING: '大纲流式输出',
  AGENT2_COMPLETE: '大纲生成完成',
  AGENT3_STREAMING: '正文流式输出',
  AGENT3_COMPLETE: '正文生成完成',
  AGENT4_COMPLETE: '配图需求分析完成',
  IMAGE_COMPLETE: '单张配图完成',
  AGENT5_COMPLETE: '配图生成完成',
  MERGE_COMPLETE: '图文合成完成',
  ALL_COMPLETE: '全部完成',
}

let eventSource: EventSource | null = null
let streamSettled = false

const TOPICS = ['AI 与工作', '阅读习惯']
const TYPES = ['深度解读', '教程指南', '观点评论', '故事叙述']
const TONES = ['专业严谨', '轻松亲切', '犀利直接', '温暖治愈']
const LENGTHS = [
  { label: '短篇', words: 500 },
  { label: '中篇', words: 1000 },
  { label: '长篇', words: 2000 },
]

const route = useRoute()
const router = useRouter()
const tab = ref<'preview' | 'markdown'>('preview')
const generating = ref(false)
const showExtra = ref(false)
const preview = ref('')
const errorMessage = ref('')
const progressMessage = ref('')
const form = reactive({
  topic: '',
  type: '深度解读',
  tone: '专业严谨',
  length: 1000,
  audience: '',
  extra: '',
})

const count = computed(() => form.topic.length)

function applyDraft(draft: Draft) {
  form.topic = draft.topic
  form.type = draft.type
  form.tone = draft.tone
  form.length = draft.length
  form.audience = draft.audience
  form.extra = draft.extra
  preview.value = draft.markdown
  showExtra.value = Boolean(draft.extra)
}

watch(
  () => route.query.draft,
  (id) => {
    if (typeof id !== 'string') {
      return
    }
    const found = drafts.value.find((item) => item.id === id)
    if (found) {
      applyDraft(found)
    }
  },
  { immediate: true },
)

function closeEventSource() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

function resetForm() {
  closeEventSource()
  streamSettled = true
  generating.value = false
  errorMessage.value = ''
  progressMessage.value = ''
  form.topic = ''
  form.type = '深度解读'
  form.tone = '专业严谨'
  form.length = 1000
  form.audience = ''
  form.extra = ''
  preview.value = ''
  showExtra.value = false
  const query = { ...route.query }
  delete query.draft
  void router.replace({ query })
}

function fillTopic(text: string) {
  form.topic = `例如：${text === 'AI 与工作' ? 'AI 如何改变我们的工作方式' : '如何把阅读变成长期习惯'}`
}

function parseSseEvent<T>(event: Event): ArticleSseEvent<T> | null {
  try {
    return JSON.parse((event as MessageEvent).data) as ArticleSseEvent<T>
  } catch {
    return null
  }
}

function persistDraft() {
  const id = typeof route.query.draft === 'string' ? route.query.draft : crypto.randomUUID()
  saveDraft({
    id,
    title: form.topic.slice(0, 18),
    topic: form.topic,
    type: form.type,
    tone: form.tone,
    length: form.length,
    audience: form.audience,
    extra: form.extra,
    markdown: preview.value,
    updatedAt: Date.now(),
  })
  if (route.query.draft !== id) {
    void router.replace({ query: { ...route.query, draft: id } })
  }
}

function finishSuccessfully() {
  if (streamSettled) {
    return
  }
  streamSettled = true
  closeEventSource()
  progressMessage.value = STAGE_LABELS.ALL_COMPLETE ?? '全部完成'
  persistDraft()
  generating.value = false
}

function finishWithError(messageOrEvent: unknown) {
  if (streamSettled) {
    return
  }
  streamSettled = true
  closeEventSource()
  generating.value = false
  if (typeof messageOrEvent === 'string') {
    errorMessage.value = messageOrEvent
    return
  }
  const payload = parseSseEvent<unknown>(messageOrEvent as Event)
  if (payload?.data != null) {
    errorMessage.value = String(payload.data)
    return
  }
  errorMessage.value = '文章生成失败'
}

function setStage(type: string) {
  progressMessage.value = STAGE_LABELS[type] ?? type
}

function attachStreamListeners(source: EventSource) {
  source.addEventListener('AGENT1_COMPLETE', () => setStage('AGENT1_COMPLETE'))
  source.addEventListener('AGENT2_STREAMING', (event) => {
    const payload = parseSseEvent<string>(event)
    progressMessage.value = payload?.data
      ? `${STAGE_LABELS.AGENT2_STREAMING ?? '大纲流式输出'}：${String(payload.data)}`
      : (STAGE_LABELS.AGENT2_STREAMING ?? '大纲流式输出')
  })
  source.addEventListener('AGENT2_COMPLETE', () => setStage('AGENT2_COMPLETE'))
  source.addEventListener('AGENT3_STREAMING', (event) => {
    const payload = JSON.parse((event as MessageEvent).data) as ArticleSseEvent<string>
    preview.value += String(payload.data)
    setStage('AGENT3_STREAMING')
  })
  source.addEventListener('AGENT3_COMPLETE', () => setStage('AGENT3_COMPLETE'))
  source.addEventListener('AGENT4_COMPLETE', () => setStage('AGENT4_COMPLETE'))
  source.addEventListener('IMAGE_COMPLETE', () => setStage('IMAGE_COMPLETE'))
  source.addEventListener('AGENT5_COMPLETE', () => setStage('AGENT5_COMPLETE'))
  source.addEventListener('MERGE_COMPLETE', (event) => {
    const payload = JSON.parse((event as MessageEvent).data) as ArticleSseEvent<string>
    preview.value = String(payload.data)
    setStage('MERGE_COMPLETE')
  })
  source.addEventListener('ALL_COMPLETE', finishSuccessfully)
  source.addEventListener('ERROR', finishWithError)
  source.onerror = () => finishWithError('生成连接已断开，请稍后查看文章状态')
}

async function generate() {
  if (!form.topic.trim()) {
    return
  }
  closeEventSource()
  streamSettled = false
  errorMessage.value = ''
  progressMessage.value = '正在创建任务…'
  preview.value = ''
  generating.value = true
  tab.value = 'markdown'
  try {
    const { taskId } = await createArticle(form.topic.trim())
    if (streamSettled) {
      return
    }
    const source = new EventSource(articleStreamUrl(taskId), { withCredentials: true })
    eventSource = source
    attachStreamListeners(source)
  } catch (error) {
    finishWithError(error instanceof Error ? error.message : '创建任务失败')
  }
}

onBeforeUnmount(() => {
  closeEventSource()
})

async function copyMarkdown() {
  if (!preview.value) {
    return
  }
  await navigator.clipboard.writeText(preview.value)
}

function downloadMarkdown() {
  if (!preview.value) {
    return
  }
  const blob = new Blob([preview.value], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${form.topic.slice(0, 12) || 'draft'}.md`
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <section>
    <div class="hero-row">
      <div>
        <p class="kicker">FROM A SPARK TO A STORY</p>
        <h1>让每一个灵感，落笔成篇。</h1>
        <p>给落笔一个主题，把脑海中的想法，变成有条理的 Markdown 文章。</p>
      </div>
      <button class="btn-new" type="button" @click="resetForm">+ 新建文单</button>
    </div>

    <div class="stage">
      <aside class="card settings">
        <div class="card-title">
          <span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M5 20h14M7 16l9.5-9.5a1.5 1.5 0 0 1 2.1 2.1L9.1 18H7v-2Z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
            </svg>
            创作设定
          </span>
          <span class="dots">···</span>
        </div>

        <div class="field">
          <label>文章主题<span class="req">*</span></label>
          <textarea
            v-model="form.topic"
            maxlength="200"
            placeholder="你想写点什么？&#10;例如：AI 如何改变我们的工作方式"
          />
          <div class="counter">{{ count }}/200</div>
        </div>

        <div class="chips">
          <span style="align-self: center; color: var(--ink-3); font-size: 12px">试试</span>
          <button v-for="item in TOPICS" :key="item" class="chip" type="button" @click="fillTopic(item)">
            {{ item }} ✦
          </button>
        </div>

        <div class="field">
          <label>文章类型</label>
          <div class="select-wrap">
            <select v-model="form.type">
              <option v-for="item in TYPES" :key="item">{{ item }}</option>
            </select>
          </div>
        </div>

        <div class="field">
          <label>写作语气</label>
          <div class="select-wrap">
            <select v-model="form.tone">
              <option v-for="item in TONES" :key="item">{{ item }}</option>
            </select>
          </div>
        </div>

        <div class="field">
          <p class="field-label">文章篇幅</p>
          <div class="length-grid">
            <button
              v-for="item in LENGTHS"
              :key="item.words"
              class="length-btn"
              :class="{ 'is-active': form.length === item.words }"
              type="button"
              @click="form.length = item.words"
            >
              <b>{{ item.label }}</b>
              <small>约 {{ item.words.toLocaleString() }} 字</small>
            </button>
          </div>
        </div>

        <div class="field">
          <label>目标读者 <span class="opt">选填</span></label>
          <input v-model="form.audience" class="soft-input" placeholder="例如：职场人、技术爱好者" />
        </div>

        <button class="extra-link" type="button" @click="showExtra = !showExtra">
          {{ showExtra ? '− 收起写作要求' : '+ 补充写作要求' }}
          <span class="opt">选填</span>
        </button>
        <div v-if="showExtra" class="field" style="margin-top: 10px">
          <textarea v-model="form.extra" placeholder="结构、禁忌、必须出现的观点…" />
        </div>

        <button class="generate" type="button" :disabled="generating || !form.topic.trim()" @click="generate">
          {{ generating ? '生成中…' : '✦  开始生成文章' }}
        </button>
        <p v-if="progressMessage" class="gen-progress">{{ progressMessage }}</p>
        <p v-if="errorMessage" class="gen-error">{{ errorMessage }}</p>
      </aside>

      <section class="card preview">
        <div class="preview-head">
          <div class="tabs">
            <button class="tab" :class="{ 'is-active': tab === 'preview' }" type="button" @click="tab = 'preview'">
              文章预览
            </button>
            <button class="tab" :class="{ 'is-active': tab === 'markdown' }" type="button" @click="tab = 'markdown'">
              / Markdown
            </button>
          </div>
          <div class="preview-tools">
            <button class="icon-btn" type="button" title="复制" @click="copyMarkdown">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <rect x="8" y="8" width="10" height="12" rx="2" stroke="currentColor" stroke-width="1.6" />
                <path d="M6 16H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" stroke="currentColor" stroke-width="1.6" />
              </svg>
            </button>
            <button class="pill" type="button" @click="downloadMarkdown">↓ 下载 .md</button>
          </div>
        </div>

        <div class="preview-body">
          <div v-if="!preview" class="blank">
            <div class="blank-art">
              <svg width="72" height="72" viewBox="0 0 72 72" fill="none">
                <rect x="18" y="16" width="30" height="38" rx="3" fill="#f3ead6" stroke="#e2d3b0" />
                <rect x="24" y="22" width="28" height="38" rx="3" fill="#fbf7ee" stroke="#e2d3b0" />
                <path d="M32 34h12M32 40h9" stroke="#d7c49a" stroke-width="1.6" stroke-linecap="round" />
                <circle cx="48" cy="48" r="10" fill="#ead7a4" />
                <path d="M44 48h8M48 44v8" stroke="#8a6d30" stroke-width="1.6" stroke-linecap="round" />
              </svg>
            </div>
            <p class="kicker">A BLANK PAGE, A NEW POSSIBILITY</p>
            <h2>你的下一篇好文章，从这里开始</h2>
            <p>在左侧写下下一个主题，剩下的交给落笔。<br />从一个灵感，到一层结构清晰的 Markdown 文章。</p>
            <div class="steps">
              <span><i class="step-no">1</i>输入主题</span>
              <span><i class="step-no">2</i>设定风格</span>
              <span><i class="step-no">3</i>生成文章</span>
            </div>
          </div>
          <article v-else class="markdown">{{ preview }}</article>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.gen-progress,
.gen-error {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
}

.gen-progress {
  color: var(--gold-deep);
}

.gen-error {
  color: #9a4b3a;
}
</style>

