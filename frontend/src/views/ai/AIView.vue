<template>
  <div class="ai-container">
    <!-- ✅ 新增：左侧会话列表侧边栏 -->
    <div class="session-sidebar">
      <div class="sidebar-header">
        <h3>会话记录</h3>
        <n-button text size="small" @click="createNewSession" title="新建会话">
          <template #icon><n-icon><AddOutline /></n-icon></template>
        </n-button>
      </div>
      
      <div class="session-list" ref="sessionListRef">
        <div 
          v-for="session in sessionList" 
          :key="session.id"
          class="session-item"
          :class="{ active: currentSessionId === session.id }"
          @click="switchSession(session.id)"
        >
          <div class="session-info">
            <n-icon :component="ChatbubbleEllipsesOutline" />
            <!-- ✅ 修改：条件渲染名称或输入框 -->
      <template v-if="editingSessionId === session.id">
        <n-input 
          v-model:value="editingSessionName" 
          size="tiny" 
          autofocus
          @keyup.enter="confirmRename(session.id)"
          @blur="confirmRename(session.id)"
          @click.stop
        />
      </template>
      <template v-else>
        <span class="session-name" @dblclick.stop="startRename(session)">
          {{ session.sessionName || '未命名会话' }}
        </span>
      </template>
    </div>

    <div class="session-actions">
      <!-- ✅ 新增：编辑按钮 (仅在悬停且非编辑状态下显示) -->
      <n-button 
        v-if="editingSessionId !== session.id"
        text 
        size="tiny" 
        class="edit-btn"
        @click.stop="startRename(session)"
        title="重命名"
      >
        <n-icon><CreateOutline /></n-icon>
      </n-button>

      <n-button 
        text 
        size="tiny" 
        class="delete-btn"
        @click.stop="deleteSession(session.id)"
      >
        <n-icon><CloseOutline /></n-icon>
      </n-button>
    </div>
  </div>
        
        <div v-if="sessionList.length === 0" class="empty-sessions">
          暂无会话记录
        </div>
      </div>
    </div>

    <!-- 右侧主内容区 -->
    <div class="ai-main-content">
      <div class="ai-header">
        <h2>{{ currentSessionName || 'AI 助手' }}</h2>
        <n-button text @click="clearCurrentChat">
          <template #icon><n-icon><TrashOutline /></n-icon></template>
          清空当前对话
        </n-button>
      </div>

      <!-- ✅ 使用 Tabs 区分模式 (仅在选中会话时显示) -->
      <n-tabs v-if="currentSessionId" type="line" animated v-model:value="activeTab" class="ai-tabs">
        
        <!-- Tab 1: 普通智能对话 -->
        <n-tab-pane name="chat" tab="💬 智能对话">
          <div class="tab-content chat-layout">
            <div class="message-list" ref="chatMessagesRef">
              <div v-for="(msg, index) in chatMessages" :key="index" 
                   class="message-item"
                   :class="{ 'message-user': msg.role === 'user' }">
                <n-avatar v-if="msg.role === 'assistant'" :size="40" round style="background: #18a058">AI</n-avatar>
                <div class="message-bubble">
                  <div class="message-text" v-html="formatMessage(msg.content)"></div>
                </div>
                <n-avatar v-if="msg.role === 'user'" :size="40" round>{{ userInfo.username?.charAt(0) || 'U' }}</n-avatar>
              </div>
              
              <div v-if="chatLoading" class="message-item">
                <n-avatar :size="40" round style="background: #18a058">AI</n-avatar>
                <div class="message-bubble">
                  <n-spin size="small" />
                </div>
              </div>
            </div>

            <div class="input-area">
              <n-input v-model:value="chatInput" 
                       type="textarea" 
                       :autosize="{ minRows: 1, maxRows: 5 }"
                       placeholder="输入消息..."
                       @keydown.enter.prevent="sendChatMessage" />
              <n-button type="primary" :loading="chatLoading" @click="sendChatMessage">
                发送
              </n-button>
            </div>
          </div>
        </n-tab-pane>

        <!-- Tab 2: PDF 文档总结 & 追问 -->
        <n-tab-pane name="pdf" tab="📄 PDF 总结">
          <div class="tab-content pdf-layout">
            
            <!-- 1. 上传区域 -->
            <div class="upload-section">
              <n-upload
                :show-file-list="false"
                :custom-request="handlePdfUpload"
                accept=".pdf"
                :disabled="pdfLoading"
              >
                <n-button type="info" dashed block size="large">
                  <template #icon><n-icon><CloudUploadOutline /></n-icon></template>
                  {{ pdfLoading ? '正在分析中...' : '点击上传 PDF 文件' }}
                </n-button>
              </n-upload>
              <p class="upload-tip">支持 .pdf 格式，文件大小建议不超过 10MB</p>
            </div>

            <!-- 2. 分析结果展示区 (可滚动) -->
            <div class="result-section" ref="pdfResultRef">
              <div v-if="!pdfResult && !pdfLoading" class="empty-state">
                <n-empty description="上传 PDF 后，AI 将在此处生成总结" />
              </div>

              <div v-else class="markdown-body">
                <div v-if="pdfLoading" class="loading-indicator">
                  <n-spin size="medium" />
                  <span>AI 正在阅读并总结文档...</span>
                </div>
                
                <!-- 实时流式显示结果 -->
                <div class="result-content" v-html="formatMessage(pdfResult)"></div>
              </div>
            </div>

            <!-- 3. PDF 专属对话区 -->
            <div v-if="pdfResult && !pdfLoading" class="pdf-chat-area">
              <n-divider dashed style="margin: 10px 0;">基于文档提问</n-divider>
              
              <div class="pdf-message-list" ref="pdfChatRef">
                 <div v-for="(msg, index) in pdfChatMessages" :key="index" 
                   class="message-item"
                   :class="{ 'message-user': msg.role === 'user' }">
                    <n-avatar v-if="msg.role === 'assistant'" :size="32" round style="background: #18a058; font-size: 12px">AI</n-avatar>
                    <div class="message-bubble">
                      <div class="message-text" v-html="formatMessage(msg.content)"></div>
                    </div>
                    <n-avatar v-if="msg.role === 'user'" :size="32" round>{{ userInfo.username?.charAt(0) || 'U' }}</n-avatar>
                  </div>
                  <div v-if="pdfChatLoading" class="message-item">
                     <n-avatar :size="32" round style="background: #18a058">AI</n-avatar>
                     <div class="message-bubble"><n-spin size="small" /></div>
                  </div>
              </div>

              <div class="input-area">
                <n-input v-model:value="pdfChatInput" 
                         type="textarea" 
                         :autosize="{ minRows: 1, maxRows: 4 }"
                         placeholder="基于刚才的总结，问我任何问题..."
                         @keydown.enter.prevent="sendPdfChatMessage" />
                <n-button type="primary" :loading="pdfChatLoading" @click="sendPdfChatMessage">
                  追问
                </n-button>
              </div>
            </div>

          </div>
        </n-tab-pane>

      </n-tabs>
      
      <!-- 未选中会话时的提示 -->
      <div v-else class="no-session-selected">
        <n-empty description="请选择或创建一个会话开始聊天" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { 
  DocumentTextOutline, 
  CloudUploadOutline, 
  TrashOutline,
  AddOutline,
  CloseOutline,
  ChatbubbleEllipsesOutline,
  CreateOutline // ✅ 新增
} from '@vicons/ionicons5'
// 引入 API
import { createSession, getSessions, getSessionMessages, deleteSession as apiDeleteSession , renameSession , clearSessionMessages } from '../../api/ai'
import { useUserStore } from '../../stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const userInfo = userStore.userInfo

// --- 状态管理 ---
const activeTab = ref('chat') 

// 1. 会话列表相关
const sessionList = ref([])
const currentSessionId = ref(null)
const sessionListRef = ref(null)

// 2. 聊天相关状态
const chatMessages = ref([]) // 初始为空，加载会话后填充
const chatInput = ref('')
const chatLoading = ref(false)
const chatMessagesRef = ref(null)

// 3. PDF 相关状态
const pdfResult = ref('') 
const pdfLoading = ref(false)
const pdfResultRef = ref(null)
const pdfChatMessages = ref([])
const pdfChatInput = ref('')
const pdfChatLoading = ref(false)
const pdfChatRef = ref(null)

// 用于控制重命名输入框的显示
const editingSessionId = ref(null)
const editingSessionName = ref('')

// 开始重命名
const startRename = (session) => {
  editingSessionId.value = session.id
  editingSessionName.value = session.sessionName
}

// AIView.vue 中的 confirmRename 函数无需大改，保持原样即可：
const confirmRename = async (sessionId) => {
  if (!editingSessionName.value.trim()) {
    message.warning('会话名称不能为空')
    return
  }

  try {
    // ✅ 这里传递的是字符串，配合上面修改后的 API 函数，Axios 会自动序列化为 JSON
    await renameSession(sessionId, editingSessionName.value)
    
    // 更新本地列表
    const session = sessionList.value.find(s => s.id === sessionId)
    if (session) {
      session.sessionName = editingSessionName.value
    }
    
    message.success('重命名成功')
    editingSessionId.value = null // 退出编辑模式
  } catch (error) {
    console.error(error)
    message.error('重命名失败: ' + (error.message || '未知错误'))
  }
}

// 取消重命名
const cancelRename = () => {
  editingSessionId.value = null
  editingSessionName.value = ''
}

// 计算当前会话名称
const currentSessionName = computed(() => {
  const session = sessionList.value.find(s => s.id === currentSessionId.value)
  return session ? session.sessionName : ''
})

// --- 会话管理逻辑 ---

// 1. 获取会话列表
const loadSessions = async () => {
  try {
    const res = await getSessions()
    // 兼容不同的返回结构
    sessionList.value = Array.isArray(res.data) ? res.data : (res.data?.records || [])
    
    // 如果列表不为空且当前没有选中会话，默认选中第一个
    if (sessionList.value.length > 0 && !currentSessionId.value) {
      switchSession(sessionList.value[0].id)
    }
  } catch (error) {
    console.error(error)
    message.error('加载会话列表失败')
  }
}

// 2. 创建新会话
const createNewSession = async () => {
  try {
    const res = await createSession({ sessionName: '新会话' })
    const newSession = res.data
    sessionList.value.unshift(newSession) // 添加到列表头部
    switchSession(newSession.id) // 自动切换
    message.success('新会话已创建')
  } catch (error) {
    message.error('创建会话失败')
  }
}

// 3. 切换会话
const switchSession = async (id) => {
  if (currentSessionId.value === id) return
  
  currentSessionId.value = id
  activeTab.value = 'chat' // 切换会话时重置为聊天 Tab
  
  // 清空 PDF 状态
  pdfResult.value = ''
  pdfChatMessages.value = []
  
  // 加载该会话的历史消息
  await loadSessionHistory(id)
}

// 4. 加载会话历史消息
const loadSessionHistory = async (sessionId) => {
  chatMessages.value = [] // 先清空
  try {
    const res = await getSessionMessages(sessionId)
    // 假设后端返回的是数组 [{ role: 'user', content: '...' }, ...]
    const history = Array.isArray(res.data) ? res.data : []
    
    // 转换格式以匹配前端显示
    chatMessages.value = history.map(msg => ({
      role: msg.role, // 'user' or 'assistant'
      content: msg.content
    }))
    
    await nextTick()
    scrollToBottom(chatMessagesRef)
  } catch (error) {
    console.error(error)
    message.error('加载历史记录失败')
  }
}

// 5. 删除会话
const deleteSession = (id) => {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除这个会话吗？历史记录将无法恢复。',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await apiDeleteSession(id)
        // 从列表中移除
        sessionList.value = sessionList.value.filter(s => s.id !== id)
        
        // 如果删除的是当前会话，清空当前状态
        if (currentSessionId.value === id) {
          currentSessionId.value = null
          chatMessages.value = []
        }
        message.success('会话已删除')
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

// --- 聊天与流式请求逻辑 ---

const sendStreamRequest = async (url, payload, onMessage, onError, onFinish) => {
  const token = localStorage.getItem('token')
  
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })

    if (!response.ok) {
      throw new Error(`请求失败: ${response.status} ${response.statusText}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmedLine = line.trim()
        if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.substring(5).trim()
          if (data === '[DONE]' || data === '') continue
          onMessage(data)
        }
      }
    }
    onFinish()
  } catch (error) {
    console.error('Stream Error:', error)
    onError(error)
  }
}

// --- 1. 普通聊天逻辑 ---
const sendChatMessage = async () => {
  if (!chatInput.value.trim() || chatLoading.value || !currentSessionId.value) return
  
  const userMsg = chatInput.value
  chatMessages.value.push({ role: 'user', content: userMsg })
  chatMessages.value.push({ role: 'assistant', content: '' }) 
  chatInput.value = ''
  chatLoading.value = true
  
  await nextTick()
  scrollToBottom(chatMessagesRef)
  
  const url = `${import.meta.env.VITE_API_BASE_URL}/ai/chat/completions?sessionId=${currentSessionId.value}`
  
  await sendStreamRequest(
    url,
    { query: userMsg, sessionId: currentSessionId.value },
    (data) => {
      const lastIndex = chatMessages.value.length - 1
      chatMessages.value[lastIndex].content += data
      nextTick(() => scrollToBottom(chatMessagesRef))
    },
    (error) => {
      message.error('AI 回复失败: ' + error.message)
      chatLoading.value = false
    },
    () => {
      chatLoading.value = false
    }
  )
}

// --- PDF 上传与分析逻辑 (优化版：自动创建并跳转会话) ---
const handlePdfUpload = async ({ file, onFinish, onError }) => {
  if (pdfLoading.value) return

  pdfResult.value = '' 
  pdfChatMessages.value = [] 
  pdfLoading.value = true
  await nextTick()
  scrollToBottom(pdfResultRef)

  const token = localStorage.getItem('token')
  const url = `${import.meta.env.VITE_API_BASE_URL}/ai/files/pdf/analyze`

  try {
    const formData = new FormData()
    formData.append('file', file.file)

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    })

    if (!response.ok) {
      throw new Error(`上传失败: ${response.statusText}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let newSessionId = null // ⚠️ 用于存储后端返回的新会话ID

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmedLine = line.trim()
        if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.substring(5).trim()
          
          // ⚠️ 特殊约定：后端可以在流中发送一个特殊的标记来传递 SessionId
          // 例如: data: {"type": "session_created", "sessionId": 123}
          // 或者在后端实现中，直接通过响应头返回，或者在 [DONE] 之前返回
          
          if (data === '[DONE]') continue
          
          // 尝试解析是否为 JSON 元数据 (可选，取决于后端实现)
          try {
            const json = JSON.parse(data)
            if (json.sessionId) {
              newSessionId = json.sessionId
              continue // 跳过显示，只保存 ID
            }
          } catch (e) {
            // 如果不是 JSON，则是正常的总结文本
          }

          pdfResult.value += data
          await nextTick()
          scrollToBottom(pdfResultRef)
        }
      }
    }
    
    onFinish()
    message.success('PDF 分析完成')

    // ✅ 关键步骤：处理会话跳转
    if (newSessionId) {
      // 1. 刷新会话列表，确保新会话出现在左侧
      await loadSessions() 
      
      // 2. 自动切换到新会话
      await switchSession(newSessionId)
      
      // 3. 提示用户
      message.info('已为您创建专属文档会话，现在可以开始追问了')
    } else {
      // 如果后端没返回 sessionId，说明后端可能没实现这个功能
      // 此时只能保持现状，或者手动创建一个空会话让用户去问（但会丢失上下文）
      console.warn('后端未返回 SessionId，上下文可能无法关联')
    }

  } catch (error) {
    console.error(error)
    message.error('PDF 分析失败: ' + error.message)
    onError()
  } finally {
    pdfLoading.value = false
  }
}

// --- ✅ 2. PDF 追问逻辑 (已简化，去掉前端拼接) ---
const sendPdfChatMessage = async () => {
  if (!pdfChatInput.value.trim() || pdfChatLoading.value || !currentSessionId.value) return
  
  const userQuestion = pdfChatInput.value
  
  // ❌ 删除了之前的 enhancedQuery 拼接逻辑
  // ✅ 现在直接发送用户原始问题，后端会根据 sessionId 自动关联上下文

  pdfChatMessages.value.push({ role: 'user', content: userQuestion })
  pdfChatMessages.value.push({ role: 'assistant', content: '' })
  pdfChatInput.value = ''
  pdfChatLoading.value = true
  
  await nextTick()
  scrollToBottom(pdfChatRef)

  const url = `${import.meta.env.VITE_API_BASE_URL}/ai/chat/completions?sessionId=${currentSessionId.value}`

  await sendStreamRequest(
    url,
    { query: userQuestion, sessionId: currentSessionId.value }, // ✅ 只发送原始问题
    (data) => {
      const lastIndex = pdfChatMessages.value.length - 1
      pdfChatMessages.value[lastIndex].content += data
      nextTick(() => scrollToBottom(pdfChatRef))
    },
    (error) => {
      message.error('追问失败: ' + error.message)
      pdfChatLoading.value = false
    },
    () => {
      pdfChatLoading.value = false
    }
  )
}

// --- 通用工具 ---
// ✅ 2. 修改 clearCurrentChat 方法
const clearCurrentChat = async () => {
  if (!currentSessionId.value) {
    message.warning('请先选择一个会话')
    return
  }

  // 使用 Dialog 确认操作
  dialog.warning({
    title: '确认清空',
    content: activeTab.value === 'chat' 
      ? '确定要清空当前会话的所有聊天记录吗？此操作不可恢复。' 
      : '确定要清空当前 PDF 的问答记录吗？',
    positiveText: '清空',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        if (activeTab.value === 'chat') {
          // ✅ 调用后端接口清空普通聊天消息
          await clearSessionMessages(currentSessionId.value)
          
          // 清空前端状态
          chatMessages.value = []
          message.success('聊天记录已清空')
          
        } else {
          // PDF 模式通常没有持久的“清空”接口，除非你后端专门做了
          // 这里暂时只清空前端显示，如果需要持久化，需后端支持
          pdfResult.value = ''
          pdfChatMessages.value = []
          message.success('PDF 问答记录已清空')
        }
      } catch (error) {
        console.error(error)
        message.error('清空失败: ' + (error.message || '未知错误'))
      }
    }
  })
}

const scrollToBottom = (elRef) => {
  if (elRef && elRef.value) {
    elRef.value.scrollTop = elRef.value.scrollHeight
  }
}

const formatMessage = (text) => {
  if (!text) return ''
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.ai-container {
  display: flex;
  height: calc(100vh - 40px);
  background: #fff;
  overflow: hidden;
}

/* --- 左侧会话侧边栏 --- */
.session-sidebar {
  width: 260px;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  background: #f9f9f9;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e0e0e0;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  margin-bottom: 5px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  color: #666;
}

.session-item:hover {
  background: #e0e0e0;
}

.session-item.active {
  background: #e6f4ff;
  color: #1890ff;
  font-weight: 500;
}

.session-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0; /* 默认隐藏操作按钮 */
  transition: opacity 0.2s;
}

.session-info {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  flex: 1; /* 确保信息区域占据剩余空间 */
}

.edit-btn {
  color: #999;
}

.edit-btn:hover {
  color: #1890ff;
}


.session-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  cursor: pointer; /* 提示可双击编辑 */
}
.delete-btn {
  color: #999;
}

/* 编辑状态下的输入框样式调整 */
:deep(.n-input__input-el) {
  padding: 2px 4px;
  font-size: 13px;
}

.delete-btn:hover {
  color: #ff4d4f;
}

.session-item:hover .session-actions {
  opacity: 1; /* 悬停显示 */
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #ff4d4f;
}

.empty-sessions {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin-top: 20px;
}

/* --- 右侧主内容 --- */
.ai-main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0; /* 防止内容溢出 */
}

.ai-header {
  padding: 15px 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.no-session-selected {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

/* Tabs 样式调整 */
.ai-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

:deep(.n-tabs-pane-wrapper) {
  height: 100%;
}

:deep(.n-tab-pane) {
  height: 100%;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.tab-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* --- 聊天布局 --- */
.chat-layout {
  padding: 20px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 20px;
  padding-right: 5px;
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.message-user {
  flex-direction: row-reverse;
}
.message-bubble {
  max-width: 70%;
}
.message-text {
  background: #f0f0f0;
  padding: 12px 16px;
  border-radius: 12px;
  word-break: break-word;
  white-space: pre-wrap;
  line-height: 1.6;
}
.message-user .message-text {
  background: #1890ff;
  color: white;
}

.input-area {
  display: flex;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.input-area :deep(.n-input) {
  flex: 1;
}

/* --- PDF 布局 --- */
.pdf-layout {
  padding: 20px;
  background: #fafafa;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.upload-section {
  flex-shrink: 0;
  margin-bottom: 15px;
  text-align: center;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.result-section {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  overflow-y: auto;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  border: 1px solid #e0e0e0;
  margin-bottom: 15px;
  min-height: 200px;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  color: #18a058;
  font-weight: 500;
}

.result-content {
  line-height: 1.8;
  font-size: 15px;
  color: #333;
}

.pdf-chat-area {
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 15px;
  border: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  max-height: 40%;
}

.pdf-message-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 10px;
  padding-right: 5px;
}

.pdf-message-list .message-item {
  margin-bottom: 10px;
}
.pdf-message-list .message-bubble {
  max-width: 85%;
}
.pdf-message-list .message-text {
  padding: 8px 12px;
  font-size: 14px;
}


</style>