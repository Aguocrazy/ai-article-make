<script setup lang="ts">
import { useRouter } from 'vue-router'
import { drafts, removeDraft } from '@/stores/studio'

const router = useRouter()

function openDraft(id: string) {
  void router.push({ name: 'write', query: { draft: id } })
}

function formatTime(value: number) {
  return new Date(value).toLocaleString()
}
</script>

<template>
  <section class="card users-panel">
    <h2>我的草稿</h2>
    <p class="lede">草稿先存在这台电脑上。生成接口接上之后，会改成跟账号走。</p>
    <p v-if="drafts.length === 0" class="empty">还没有草稿。去文章创作里写一个主题吧。</p>
    <table v-else>
      <thead>
        <tr>
          <th>标题</th>
          <th>类型</th>
          <th>更新时间</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in drafts" :key="item.id">
          <td>{{ item.title }}</td>
          <td>{{ item.type }}</td>
          <td>{{ formatTime(item.updatedAt) }}</td>
          <td>
            <button class="extra-link" type="button" @click="openDraft(item.id)">继续写</button>
            &nbsp;
            <button class="danger" type="button" @click="removeDraft(item.id)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
