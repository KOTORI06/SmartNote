<template>
  <div class="recycle-bin-container">
    <n-card title="回收站" :bordered="false">
      <template #header-extra>
        <n-text depth="3">已删除的笔记将保留在这里，您可以随时复原。</n-text>
      </template>

      <!-- 加载状态 -->
      <n-spin :show="loading">
        
        <!-- 空状态 -->
        <n-empty 
          v-if="!loading && deletedNotes.length === 0" 
          description="回收站是空的" 
          style="margin-top: 50px"
        >
          <template #icon>
            <n-icon><TrashOutline /></n-icon>
          </template>
        </n-empty>

        <!-- 笔记列表 -->
        <div v-else class="note-list">
          <n-list hoverable clickable bordered>
            <n-list-item v-for="note in deletedNotes" :key="note.id">
              <template #prefix>
                <n-icon size="24" color="#999"><DocumentTextOutline /></n-icon>
              </template>
              
              <n-thing :title="note.title || '无标题笔记'">
                <template #description>
                  <n-space vertical size="small">
                    <n-text depth="3" style="font-size: 12px">
                      删除时间: {{ formatTime(note.updateTime) }}
                    </n-text>
                    <!-- 显示内容预览，如果有的话 -->
                    <n-text depth="3" truncate style="max-width: 600px">
                      {{ note.contentPreview || '无内容预览' }}
                    </n-text>
                  </n-space>
                </template>
                
                <template #action>
                  <n-space>
                    <n-button 
                      size="small" 
                      type="primary" 
                      secondary
                      @click="handleRestore(note.id)"
                      :loading="restoringId === note.id"
                    >
                      <template #icon><n-icon><RefreshOutline /></n-icon></template>
                      复原
                    </n-button>
                    
                    <!-- 如果有彻底删除接口，可以添加此按钮 -->
                    <!-- 
                    <n-popconfirm @positive-click="handlePermanentDelete(note.id)">
                      <template #trigger>
                        <n-button size="small" type="error" text>彻底删除</n-button>
                      </template>
                      确定要彻底删除吗？此操作不可恢复。
                    </n-popconfirm> 
                    -->
                  </n-space>
                </template>
              </n-thing>
            </n-list-item>
          </n-list>
        </div>

        <!-- 分页器 -->
        <div class="pagination-container" v-if="total > 0">
          <n-pagination
            v-model:page="currentPage"
            v-model:page-size="pageSize"
            :item-count="total"
            show-size-picker
            :page-sizes="[10, 20, 50]"
            @update:page="loadDeletedNotes"
            @update:page-size="handlePageSizeChange"
          />
        </div>

      </n-spin>
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { 
  TrashOutline, 
  DocumentTextOutline, 
  RefreshOutline 
} from '@vicons/ionicons5'
import { getDeletedNotes, restoreNote } from '@/api/note' // 确保路径正确
import { useUserStore } from '@/stores/user' // 假设你有 user store 获取 userId

const message = useMessage()
const userStore = useUserStore()

// 状态定义
const loading = ref(false)
const restoringId = ref(null) // 记录当前正在复原的笔记ID，用于按钮loading状态
const deletedNotes = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// 格式化时间工具函数
const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return new Date(timeStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 加载已删除笔记列表
const loadDeletedNotes = async () => {
  loading.value = true
  try {
    const res = await getDeletedNotes({
      page: currentPage.value,
      size: pageSize.value,
      userId: userStore.userInfo?.id // 根据后端要求传递 userId
    })
    
    // 兼容不同的返回结构，通常 Page 对象在 data 中
    const pageData = res.data || {}
    deletedNotes.value = pageData.records || []
    total.value = pageData.total || 0
    
  } catch (error) {
    console.error('加载回收站失败:', error)
    message.error('加载回收站失败')
  } finally {
    loading.value = false
  }
}

// 处理页码变化
const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1 // 重置到第一页
  loadDeletedNotes()
}

// 复原笔记
const handleRestore = async (id) => {
  restoringId.value = id
  try {
    await restoreNote(id)
    message.success('笔记已复原')
    
    // 重新加载列表
    // 如果当前页只有一条数据且不是第一页，可能需要回退一页，这里简单处理为重新加载当前页
    await loadDeletedNotes()
    
  } catch (error) {
    console.error('复原失败:', error)
    message.error('复原失败: ' + (error.message || '未知错误'))
  } finally {
    restoringId.value = null
  }
}

onMounted(() => {
  loadDeletedNotes()
})
</script>

<style scoped>
.recycle-bin-container {
  padding: 20px;
  height: 100%;
  background-color: #f5f7fa; /* 与主背景保持一致 */
}

.note-list {
  margin-bottom: 20px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>