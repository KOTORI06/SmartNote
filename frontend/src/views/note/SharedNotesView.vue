<template>
  <div class="shared-notes-container">
    <!-- 左侧：分享给我的笔记列表 -->
    <div class="note-list">
      <div class="list-header">
        <h3>📩 分享给我的</h3>
        <n-input 
          v-model:value="searchText" 
          placeholder="搜索标题..." 
          clearable
          @keyup.enter="handleSearch"
        />
      </div>

      <div class="notes-scroll-container" @scroll="handleScroll">
        <n-spin :show="loading">
          <div v-for="note in notes" :key="note.noteId" 
               class="note-item"
               :class="{ active: currentNote?.noteId === note.noteId }"
               @click="selectNote(note)">
            <div class="note-title">{{ note.title || '无标题' }}</div>
            <div class="note-owner">来自: {{ note.ownerName || '未知用户' }}</div>
            <div class="note-preview">{{ note.contentPreview || '无内容预览' }}</div>
            <div class="note-meta">
              <n-tag size="tiny" :type="note.permissionType === 2 ? 'success' : 'info'">
                {{ note.permissionType === 2 ? '可编辑' : '仅查看' }}
              </n-tag>
              <span class="time">{{ formatTime(note.shareTime) }}</span>
            </div>
          </div>
          
          <div v-if="loadingMore" class="loading-more">加载中...</div>
          <div v-else-if="!hasMore && notes.length > 0" class="no-more">没有更多了</div>
          <div v-if="!loading && notes.length === 0" class="empty-list">
            <n-empty description="暂无分享笔记" />
          </div>
        </n-spin>
      </div>
    </div>

    <!-- 右侧：笔记详情展示/编辑 -->
    <div class="note-detail">
      <template v-if="currentNote">
        <div class="detail-header">
          <div class="header-left">
            <!-- 标题：如果是可编辑状态，显示输入框，否则显示文本 -->
            <n-input 
              v-if="canEdit" 
              v-model:value="editableTitle" 
              placeholder="笔记标题" 
              size="large"
              :bordered="false"
              class="title-input"
            />
            <h2 v-else class="title-text">{{ currentNote.title }}</h2>
            
            <div class="meta-info">
              <n-tag :type="canEdit ? 'success' : 'info'">
                当前权限: {{ canEdit ? '可编辑' : '仅查看' }}
              </n-tag>
              <span class="owner-name">所有者: {{ currentNote.ownerName }}</span>
            </div>
          </div>

          <div class="header-actions">
            <n-button v-if="canEdit" type="primary" @click="handleSaveEdit" :loading="saving">
              保存修改
            </n-button>
            <n-button v-else type="default" disabled>
              只读模式
            </n-button>
          </div>
        </div>

        <n-divider style="margin: 10px 0;" />

        <div class="detail-content">
          <!-- 内容：如果是可编辑状态，显示 Textarea，否则显示预格式化文本 -->
          <n-input 
            v-if="canEdit"
            v-model:value="editableContent" 
            type="textarea" 
            :autosize="{ minRows: 20 }"
            placeholder="笔记内容..."
          />
          <div v-else class="read-only-content">
            {{ currentNote.content || '无内容' }}
          </div>
        </div>
      </template>

      <div v-else class="empty-detail">
        <n-empty description="点击左侧笔记查看详情" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { getSharedNotesToMe } from '../../api/share'
import { updateNote } from '../../api/share' 
import { formatTime } from '../../utils'
// ✅ 正确的 Pinia 导入方式
import { useUserStore } from '@/stores/user'
import { getNoteDetail } from '../../api/note' // ✅ 1. 引入详情接口

const message = useMessage()
const userStore = useUserStore() // ✅ 提前初始化 store，方便多处使用


// --- 状态定义 ---
const loading = ref(false)
const loadingMore = ref(false)
const notes = ref([])
const currentNote = ref(null)
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)
const saving = ref(false)
const detailLoading = ref(false) // ✅ 新增：详情加载状态

// 编辑状态
const editableTitle = ref('')
const editableContent = ref('')

// --- 计算属性 ---
const canEdit = computed(() => {
  return currentNote.value && currentNote.value.permissionType === 2
})

// --- 方法 ---

// 1. 加载分享笔记列表
const loadSharedNotes = async (page = 1, append = false) => {
  if (!append) loading.value = true
  else loadingMore.value = true

  try {
    const res = await getSharedNotesToMe({
      page,
      size: pageSize.value,
      title: searchText.value || undefined
    })

    const newNotes = res.data.records || res.data || []
    
    if (append) {
      // 去重合并
      const existingIds = new Set(notes.value.map(n => n.noteId))
      const uniqueNew = newNotes.filter(n => !existingIds.has(n.noteId))
      notes.value = [...notes.value, ...uniqueNew]
    } else {
      notes.value = newNotes
    }

    hasMore.value = newNotes.length >= pageSize.value
    currentPage.value = page
  } catch (error) {
    message.error('加载分享笔记失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// ✅ 2. 修改 selectNote：支持异步获取详情
const selectNote = async (note) => {
  // 先显示基本信息，避免界面空白
  currentNote.value = { ...note }
  editableTitle.value = note.title
  
  // 判断是否有完整内容
  // 通常列表接口只返回 contentPreview。如果 content 为空或长度很短，视为需要加载详情
  const hasFullContent = note.content && note.content.length > (note.contentPreview?.length || 0)

  if (hasFullContent) {
    // 如果列表里已经有全文，直接使用
    editableContent.value = note.content
  } else {
    // ✅ 如果没有全文，显示加载状态并请求详情接口
    detailLoading.value = true
    editableContent.value = '正在加载完整内容...'
    
    try {
      const userId = userStore.userInfo?.id
      // 调用详情接口
      const res = await getNoteDetail(note.noteId, { userId: userId })
      
      if (res.data) {
        const fullNote = res.data
        
        // 更新当前选中项的完整数据
        currentNote.value = {
          ...currentNote.value,
          content: fullNote.content,
          // 如果详情接口返回了其他重要字段（如标签、文件夹），也可以在这里合并
        }
        
        // 更新编辑器内容
        editableContent.value = fullNote.content
        
        // ✅ 优化：同步更新列表中的该项，避免下次点击重复请求
        const index = notes.value.findIndex(n => n.noteId === note.noteId)
        if (index !== -1) {
          notes.value[index].content = fullNote.content
        }
      }
    } catch (error) {
      console.error('获取笔记详情失败', error)
      message.error('无法加载笔记完整内容')
      editableContent.value = '' // 加载失败则清空
    } finally {
      detailLoading.value = false
    }
  }
}

// 3. 保存编辑
const handleSaveEdit = async () => {
  if (!currentNote.value) return
  
  saving.value = true
  try {
    // ✅ 关键：获取当前用户ID
    const userStore = useUserStore() // ✅ 正确调用
    const userId = userStore.userInfo?.id
    
    // 调用更新笔记接口
    await updateNote(currentNote.value.noteId, {
      title: editableTitle.value,
      content: editableContent.value
    }) // 如果后端需要 userId 在 query param
    
    message.success('保存成功')
    
    // 更新本地列表
    const index = notes.value.findIndex(n => n.noteId === currentNote.value.noteId)
    if (index !== -1) {
      notes.value[index].title = editableTitle.value
      notes.value[index].contentPreview = editableContent.value.substring(0, 100)
    }
    
    currentNote.value.title = editableTitle.value
    currentNote.value.content = editableContent.value
    
  } catch (error) {
    console.error(error)
    message.error(error.response?.data?.message || '保存失败，请检查网络或权限')
  } finally {
    saving.value = false
  }
}

// 4. 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadSharedNotes(1, false)
}

// 5. 滚动加载
const handleScroll = (e) => {
  const { scrollTop, scrollHeight, clientHeight } = e.target
  if (hasMore.value && !loadingMore.value && (scrollHeight - scrollTop - clientHeight < 10)) {
    loadSharedNotes(currentPage.value + 1, true)
  }
}

onMounted(() => {
  loadSharedNotes()
})
</script>

<style scoped>
.shared-notes-container {
  display: flex;
  height: calc(100vh - 40px);
  gap: 20px;
}

/* 左侧列表样式 (复用 NoteView 的部分样式) */
.note-list {
  width: 300px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.list-header {
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.list-header h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
}

.notes-scroll-container {
  flex: 1;
  overflow-y: auto;
}

.note-item {
  padding: 15px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.note-item:hover {
  background: #f5f5f5;
}

.note-item.active {
  background: #e8f4ff;
  border-left: 4px solid #18a058;
}

.note-title {
  font-weight: bold;
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.note-owner {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.note-preview {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time {
  font-size: 11px;
  color: #bbb;
}

/* 右侧详情样式 */
.note-detail {
  flex: 1;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-header {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.header-left {
  flex: 1;
  margin-right: 20px;
}

.title-input {
  font-size: 24px;
  font-weight: bold;
  padding: 0;
  margin-bottom: 10px;
}

.title-text {
  font-size: 24px;
  margin: 0 0 10px 0;
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.owner-name {
  font-size: 13px;
  color: #666;
}

.detail-content {
  flex: 1;
  padding: 0 20px 20px 20px;
  overflow-y: auto;
}

.read-only-content {
  white-space: pre-wrap;
  line-height: 1.6;
  font-size: 15px;
  color: #333;
}

.empty-detail {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-more, .no-more, .empty-list {
  padding: 15px;
  text-align: center;
  color: #999;
  font-size: 13px;
}
</style>