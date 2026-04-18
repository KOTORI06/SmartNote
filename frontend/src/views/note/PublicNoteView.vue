<template>
  <div class="public-note-container">
    <n-spin :show="loading">
      <div v-if="note" class="note-content-wrapper">
        <!-- 头部 -->
        <div class="note-header">
          <h1 class="note-title">{{ note.title }}</h1>
          <div class="note-meta">
            <span class="meta-item">作者: {{ note.ownerName || '未知用户' }}</span>
            <span class="meta-item">更新于: {{ formatTime(note.updateTime) }}</span>
          </div>
        </div>

        <n-divider />

        <!-- 内容区域 -->
        <div class="note-body">
          <pre class="note-text">{{ note.content }}</pre>
        </div>
        
        <!-- 底部提示 -->
        <div class="note-footer">
          <n-text depth="3">本文档由 SmartNote 公开分享</n-text>
          <n-button text type="primary" @click="goToHome">返回首页</n-button>
        </div>
      </div>

      <n-empty v-else-if="!loading && !hasError" description="未找到该公开笔记" />
      <n-result v-else-if="hasError" status="404" title="访问失败" :description="errorMsg" />
    </n-spin>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicNote } from '@/api/share' // 你需要在 api/share.js 中添加这个函数
import { formatTime } from '@/utils'

const route = useRoute()
const router = useRouter()

const note = ref(null)
const loading = ref(false)
const hasError = ref(false)
const errorMsg = ref('')

// 获取公开笔记详情
const loadPublicNote = async () => {
  const noteId = route.params.id
  if (!noteId) {
    hasError.value = true
    errorMsg.value = '无效的笔记链接'
    return
  }

  loading.value = true
  try {
    // 调用专门获取公开笔记的接口
    const res = await getPublicNote(noteId)
    note.value = res.data
  } catch (error) {
    console.error('加载公开笔记失败', error)
    hasError.value = true
    errorMsg.value = error.response?.data?.message || '笔记不存在或已取消公开'
  } finally {
    loading.value = false
  }
}

const goToHome = () => {
  router.push('/')
}

onMounted(() => {
  loadPublicNote()
})
</script>

<style scoped>
.public-note-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
  min-height: 100vh;
  background-color: #fff;
}

.note-title {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.note-meta {
  display: flex;
  gap: 20px;
  color: #666;
  font-size: 14px;
}

.note-body {
  margin-top: 20px;
}

.note-text {
  white-space: pre-wrap; /* 保留换行和空格 */
  word-wrap: break-word;
  font-family: inherit;
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  margin: 0;
}

.note-footer {
  margin-top: 50px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>