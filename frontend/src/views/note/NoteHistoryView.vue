<template>
  <div class="history-container">
    <div class="history-list">
      <!-- 头部 -->
      <div class="list-header">
        <div class="header-title">
          <n-icon size="18" color="#666" style="margin-right: 8px;">
            <TimeOutline />
          </n-icon>
          <h3>浏览历史</h3>
        </div>
        <div class="header-actions">
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button text size="small" @click="loadHistory(true)" :loading="loading">
                <template #icon><n-icon><RefreshOutline /></n-icon></template>
              </n-button>
            </template>
            刷新
          </n-tooltip>
          
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button 
                text 
                size="small" 
                @click="handleClearHistory" 
                :disabled="historyList.length === 0 || loading"
                style="color: #ff4d4f;"
              >
                <template #icon><n-icon><TrashOutline /></n-icon></template>
              </n-button>
            </template>
            清空历史
          </n-tooltip>
        </div>
      </div>

      <!-- 滚动容器 -->
      <div class="history-scroll-container" @scroll="handleScroll">
        <n-spin :show="loading && historyList.length === 0">
          <div v-if="historyList.length === 0 && !loading" class="empty-state">
            <n-empty description="暂无浏览记录" size="small">
              <template #icon>
                <n-icon><DocumentTextOutline /></n-icon>
              </template>
            </n-empty>
          </div>

          <transition-group name="list" tag="div">
            <div 
              v-for="item in historyList" 
              :key="item.noteId" 
              class="history-item"
              @click="goToNote(item)"
            >
              <div class="item-content">
                <div class="item-title">{{ item.title || '无标题笔记' }}</div>
                <div class="item-preview">{{ item.contentPreview || '无预览内容' }}</div>
              </div>
              
              <div class="item-meta">
                <span class="meta-time">{{ formatTime(item.viewTime) }}</span>
                <n-tag v-if="item.hasUpdate" size="tiny" type="warning" variant="dot">已更新</n-tag>
              </div>
            </div>
          </transition-group>

          <div v-if="loadingMore" class="loading-more">
            <n-spin size="small" />
            <span>加载中...</span>
          </div>
          <div v-else-if="!hasMore && historyList.length > 0" class="no-more">
            没有更多了
          </div>
        </n-spin>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import { getNoteHistory } from '../../api/note'
// 假设 utils 中有 formatTime，如果没有，请使用 date-fns 或 dayjs
import { formatTime } from '../../utils' 
import { 
  RefreshOutline, 
  TrashOutline, 
  TimeOutline, 
  DocumentTextOutline 
} from '@vicons/ionicons5'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const loadingMore = ref(false)
const historyList = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)
const total = ref(0)

// 加载历史记录
const loadHistory = async (refresh = false) => {
  if (refresh) {
    currentPage.value = 1
    historyList.value = []
    hasMore.value = true
  }

  if (!hasMore.value && !refresh) return

  if (refresh) loading.value = true
  else loadingMore.value = true

  try {
    // 注意：这里假设后端接口 /api/notes/history 存在且返回分页数据
    // 如果后端接口不同，请修改 note.js 中的对应方法
    const res = await getNoteHistory({
      page: currentPage.value,
      size: pageSize.value
    })

    // 兼容不同的响应结构
    let records = []
    let totalCount = 0
    
    if (res.data) {
      // 情况1: 标准分页结构 { records: [], total: 0 }
      if (Array.isArray(res.data.records)) {
        records = res.data.records
        totalCount = res.data.total || 0
      } 
      // 情况2: 直接返回数组
      else if (Array.isArray(res.data)) {
        records = res.data
        totalCount = res.data.length
      }
      // 情况3: data 中包含 list 字段
      else if (res.data.list && Array.isArray(res.data.list)) {
        records = res.data.list
        totalCount = res.data.total || res.data.list.length
      }
    }

    if (refresh) {
      historyList.value = records
    } else {
      // 去重合并：防止网络重试或边界条件导致重复
      const existingIds = new Set(historyList.value.map(h => h.noteId))
      const newRecords = records.filter(r => !existingIds.has(r.noteId))
      historyList.value = [...historyList.value, ...newRecords]
    }

    total.value = totalCount
    // 判断是否还有更多数据
    hasMore.value = historyList.value.length < totalCount
    
    if (refresh) currentPage.value = 1
    else if (records.length > 0) currentPage.value++

  } catch (error) {
    console.error('加载历史失败', error)
    message.error('加载浏览历史失败，请稍后重试')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 清空历史
const handleClearHistory = () => {
  dialog.warning({
    title: '确认清空',
    content: '确定要清空所有浏览历史吗？此操作不可恢复。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 假设有一个清空历史的 API，如果没有，需要后端补充
        // await clearNoteHistory() 
        message.info('后端暂未提供清空接口，仅前端清除展示')
        historyList.value = []
        hasMore.value = false
      } catch (e) {
        message.error('清空失败')
      }
    }
  })
}

// 滚动加载更多
const handleScroll = (e) => {
  const { scrollTop, scrollHeight, clientHeight } = e.target
  // 距离底部 10px 时触发加载
  if (!loadingMore.value && hasMore.value && (scrollHeight - scrollTop - clientHeight < 10)) {
    loadHistory()
  }
}

// 点击跳转回笔记详情页
const goToNote = (item) => {
  if (!item.noteId) return
  
  // 路由跳转
  router.push({ 
    name: 'Note', 
    query: { id: item.noteId } 
  }).catch(err => {
    // 忽略重复导航的错误
    if (err.name !== 'NavigationDuplicated') {
      console.error('跳转失败:', err)
      message.error('跳转笔记失败')
    }
  })
}

onMounted(() => {
  loadHistory(true)
})
</script>

<style scoped>
.history-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.history-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.list-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
  flex-shrink: 0;
}

.header-title {
  display: flex;
  align-items: center;
}

.header-title h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 4px;
}

.history-scroll-container {
  flex: 1;
  overflow-y: auto;
  /* 自定义滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: #ccc transparent;
}

.history-scroll-container::-webkit-scrollbar {
  width: 6px;
}

.history-scroll-container::-webkit-scrollbar-thumb {
  background-color: #ccc;
  border-radius: 3px;
}

.history-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.history-item:hover {
  background: #f0f7ff;
}

.history-item:active {
  background: #e6f4ff;
}

.item-content {
  margin-bottom: 8px;
}

.item-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.item-preview {
  font-size: 12px;
  color: #888;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 36px; /* 固定高度防止抖动 */
}

.item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta-time {
  font-size: 11px;
  color: #999;
}

.empty-state {
  padding: 40px 0;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  color: #999;
}

.loading-more, .no-more {
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: #999;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  background: #fafafa;
}

/* 列表动画 */
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}
</style>