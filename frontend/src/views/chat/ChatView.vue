<template>
  <div class="im-container">
    <!-- ✅ 左侧：会话列表侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h3 class="sidebar-title">消息</h3>
        <n-button text circle size="small" @click="openCreateModal" title="新建会话">
          <template #icon><n-icon><AddOutline /></n-icon></template>
        </n-button>
      </div>
      
      <div class="search-box">
        <n-input 
          v-model:value="searchText" 
          placeholder="搜索会话" 
          size="small" 
          round 
          clearable
        >
          <template #prefix><n-icon><SearchOutline /></n-icon></template>
        </n-input>
      </div>

      <div class="conversation-list" ref="listRef">
        <n-spin :show="loading">
          <div 
            v-for="item in filteredConversations" 
            :key="item.id" 
            class="conversation-item" 
            :class="{ active: currentConversation?.id === item.id }"
            @click="selectConversation(item)"
          >
            <n-avatar :size="48" round :src="item.avatar" class="item-avatar">
              {{ item.name?.charAt(0) }}
            </n-avatar>
            <div class="item-content">
              <div class="item-top">
                <span class="item-name">{{ item.name }}</span>
                <span class="item-time">{{ formatTimeShort(item.lastMessageTime) }}</span>
              </div>
              <div class="item-bottom">
                <span class="item-last-msg">{{ item.lastMessageContent || '暂无消息' }}</span>
                <!-- 如果有未读消息，可以显示红点 -->
                <!-- <n-badge :value="item.unreadCount" :max="99" /> -->
              </div>
            </div>
          </div>
          
          <n-empty v-if="!loading && filteredConversations.length === 0" description="暂无会话" style="margin-top: 20px"/>
        </n-spin>
      </div>
    </div>

    <!-- ✅ 右侧：聊天主窗口 -->
    <div class="chat-window">
      <template v-if="currentConversation">
        <!-- 1. 顶部标题栏 -->
        <div class="chat-header">
          <div class="header-info">
            <h3>{{ currentConversation.name }}</h3>
            <span class="header-subtitle" v-if="currentConversation.type === 2">
              {{ currentConversation.memberCount }}人
            </span>
          </div>

          <div class="header-actions">
    <!-- ✅ 新增：删除/清空按钮 -->
    <n-popconfirm 
      @positive-click="handleClearMessages"
      title="确定要清空会话吗？聊天记录将无法恢复。"
      positive-text="清空"
      negative-text="取消"
    >
      <template #trigger>
        <n-button text size="small" title="删除会话">
          <template #icon><n-icon><TrashOutline /></n-icon></template>
        </n-button>
      </template>
    </n-popconfirm>
          <n-button text size="small" @click="handleMoreOptions">
            <template #icon><n-icon><EllipsisHorizontalOutline /></n-icon></template>
          </n-button>
        </div>
        </div>

        <!-- 2. 消息列表区域 (可滚动) -->
        <div class="message-area" ref="messageListRef">
          <div v-for="msg in messages" :key="msg.id" 
               class="message-row"
               :class="{ 'message-row-self': msg.senderId === userInfo.id }">
            
            <!-- 头像 -->
            <n-avatar 
              :size="40" 
              round 
              :src="msg.senderId === userInfo.id ? userInfo.avatarUrl : msg.senderAvatar"
              class="msg-avatar"
            >
              {{ (msg.senderId === userInfo.id ? userInfo.username : msg.senderName)?.charAt(0) }}
            </n-avatar>

            <!-- 消息内容气泡 -->
            <div class="message-bubble-wrapper">
              <div class="sender-name" v-if="msg.senderId !== userInfo.id && currentConversation.type === 2">
                {{ msg.senderName }}
              </div>
              <div class="message-bubble">
                <div class="message-text" v-html="formatMessage(msg.content)"></div>
              </div>
              <div class="message-meta">
                <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
              </div>
            </div>
          </div>
          
          <!-- 空状态提示 -->
          <div v-if="messages.length === 0" class="empty-history">
            <n-empty description="开始新的对话吧" />
          </div>
        </div>

        <!-- 3. 底部输入区域 -->
        <div class="input-area">
          <n-input 
            v-model:value="inputMessage" 
            type="textarea" 
            :autosize="{ minRows: 2, maxRows: 6 }"
            placeholder="按 Enter 发送，Shift + Enter 换行"
            @keydown.enter.prevent.exact="handleSendMessage"
            @keydown.enter.shift.exact="inputMessage += '\n'"
            class="chat-input"
          />
          <div class="input-toolbar">
            <div class="toolbar-left">
              <!-- 这里可以加表情、文件上传等按钮 -->
              <n-tooltip trigger="hover">
                <template #trigger>
                  <n-button text size="large"><template #icon><n-icon><HappyOutline /></n-icon></template></n-button>
                </template>
                表情
              </n-tooltip>
            </div>
            <div class="toolbar-right">
              <n-button 
                type="primary" 
                :disabled="!inputMessage.trim()" 
                :loading="sending"
                @click="handleSendMessage"
              >
                发送
              </n-button>
            </div>
          </div>
        </div>
      </template>

      <!-- 未选中会话时的占位图 -->
      <div v-else class="empty-state">
        <div class="empty-content">
          <n-icon size="80" color="#ccc"><ChatbubbleEllipsesOutline /></n-icon>
          <p>选择一个会话开始聊天</p>
        </div>
      </div>
    </div>

    <!-- ✅ 新建会话 Modal (保持原有逻辑) -->
    <n-modal v-model:show="showCreateModal" preset="card" title="新建会话" style="width: 600px">
      <n-tabs v-model:value="createType" type="line" animated>
        <n-tab-pane name="private" tab="私聊">
          <div class="friend-select-container">
            <n-input v-model:value="privateSearchKey" placeholder="搜索好友..." style="margin-bottom: 10px" />
            <div class="friend-list-scroll">
              <div v-for="friend in filteredPrivateFriends" :key="friend.id" class="friend-row">
                <div class="friend-info">
                  <n-avatar :size="32" round :src="friend.avatarUrl">{{ friend.username?.charAt(0) }}</n-avatar>
                  <span class="friend-name">{{ friend.username }}</span>
                </div>
                <n-button size="small" type="primary" :loading="creatingPrivate" @click="handleCreatePrivate(friend.id)">发起聊天</n-button>
              </div>
              <n-empty v-if="filteredPrivateFriends.length === 0" description="暂无好友" style="margin-top: 20px"/>
            </div>
          </div>
        </n-tab-pane>
        <n-tab-pane name="group" tab="群聊">
          <n-form label-placement="left" label-width="80">
            <n-form-item label="群名称"><n-input v-model:value="groupForm.name" placeholder="请输入群聊名称" /></n-form-item>
          </n-form>
          <div class="group-member-section">
            <div class="section-title">选择成员 ({{ selectedGroupMembers.length }})</div>
            <n-input v-model:value="groupSearchKey" placeholder="搜索好友..." style="margin-bottom: 10px" />
            <div class="friend-list-scroll">
              <div v-for="friend in filteredGroupFriends" :key="friend.id" class="friend-row">
                <n-checkbox :checked="selectedGroupMembers.includes(friend.id)" @update:checked="(checked) => toggleGroupMember(friend.id, checked)">
                  <div class="checkbox-content">
                    <n-avatar :size="32" round :src="friend.avatarUrl">{{ friend.username?.charAt(0) }}</n-avatar>
                    <span class="friend-name">{{ friend.username }}</span>
                  </div>
                </n-checkbox>
              </div>
              <n-empty v-if="filteredGroupFriends.length === 0" description="暂无好友" style="margin-top: 20px"/>
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" :disabled="createType === 'group' && (!groupForm.name || selectedGroupMembers.length === 0)" :loading="creatingGroup" @click="handleCreateGroup">创建</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { 
  AddOutline, SearchOutline, EllipsisHorizontalOutline, 
  ChatbubbleEllipsesOutline, HappyOutline ,
  TrashOutline // ✅ 1. 这里必须添加 TrashOutline

} from '@vicons/ionicons5'
import { getConversations, getMessages, createPrivateChat, createGroupChat ,clearMessages} from '../../api/chat'
import { getFriends } from '../../api/friend'
import { useUserStore } from '../../stores/user'
import { formatTime } from '../../utils'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const message = useMessage()
const userStore = useUserStore()
const userInfo = userStore.userInfo

// --- 状态定义 ---
const loading = ref(false)
const sending = ref(false)
const conversations = ref([])
const currentConversation = ref(null)
const messages = ref([])
const inputMessage = ref('')
const searchText = ref('')
const messageListRef = ref(null)
let ws = null

// 新建会话相关
const showCreateModal = ref(false)
const createType = ref('private')
const privateSearchKey = ref('')
const allFriends = ref([])
const creatingPrivate = ref(false)
const groupSearchKey = ref('')
const groupForm = ref({ name: '' })
const selectedGroupMembers = ref([])
const creatingGroup = ref(false)

// --- 计算属性 ---
const filteredConversations = computed(() => {
  if (!searchText.value) return conversations.value
  return conversations.value.filter(c => c.name.includes(searchText.value))
})

const filteredPrivateFriends = computed(() => {
  if (!privateSearchKey.value) return allFriends.value
  return allFriends.value.filter(f => f.username.includes(privateSearchKey.value))
})

const filteredGroupFriends = computed(() => {
  if (!groupSearchKey.value) return allFriends.value
  return allFriends.value.filter(f => f.username.includes(groupSearchKey.value))
})

// --- 工具函数 ---
const formatMessage = (text) => {
  if (!text) return ''
  return DOMPurify.sanitize(marked.parse(text, { breaks: true }))
}

const formatTimeShort = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  // 简单判断：如果是今天，只显示 HH:mm；否则显示 MM-DD
  if (date.toDateString() === now.toDateString()) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  return `${date.getMonth() + 1}-${date.getDate()}`
}

const scrollToBottom = (behavior = 'smooth') => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 监听消息变化自动滚动
watch(messages, () => {
  nextTick(() => scrollToBottom())
}, { deep: true })

// --- 业务逻辑 ---
const loadConversations = async () => {
  loading.value = true
  try {
    const res = await getConversations()
    let list = res.data || []
    
    // ✅ 核心修改：处理私聊名称
    // 假设后端返回的 conversation 对象结构如下：
    // { id, type, name, members: [{ userId, username, ... }] } 
    // 或者 { id, type, name, friendId, friendName } (取决于后端实现)
    
    // 如果后端返回的 name 已经是正确的（比如群聊名，或后端已处理好私聊名），则无需修改。
    // 如果后端返回的私聊 name 是空的或者是 "Private Chat"，我们需要手动替换。
    
    conversations.value = list.map(conv => {
      // 如果是私聊 (type === 1)
      if (conv.type === 1) {
        // 情况 A: 后端返回了 members 列表
        if (conv.members && Array.isArray(conv.members)) {
          const otherMember = conv.members.find(m => m.userId !== userInfo.id)
          if (otherMember) {
            return {
              ...conv,
              name: otherMember.username || otherMember.name || '未知用户',
              avatar: otherMember.avatarUrl || conv.avatar // 同时也更新头像
            }
          }
        }
        
        // 情况 B: 后端返回了 friendId 或 targetUserId，但未返回名字
        // 这时我们需要从 allFriends 缓存中查找
        // 注意：这要求 allFriends 已经加载。如果没加载，可能需要先加载好友列表
        if (conv.friendId || conv.targetUserId) {
           const fid = conv.friendId || conv.targetUserId
           const friend = allFriends.value.find(f => f.id === fid)
           if (friend) {
             return {
               ...conv,
               name: friend.username,
               avatar: friend.avatarUrl
             }
           }
        }
      }
      
      // 群聊或其他情况，保持原样
      return conv
    })
    
  } catch (error) {
    message.error('加载会话失败')
  } finally {
    loading.value = false
  }
}

// ✅ 修改：清空聊天记录逻辑 (而不是删除会话)
const handleClearMessages = async () => {
  if (!currentConversation.value) return
  
  try {
    // 1. 调用后端接口清空消息
    await clearMessages(currentConversation.value.id)
    
    // 2. 前端清空当前聊天窗口的消息
    messages.value = []
    
    // 3. 更新左侧会话列表中的“最后一条消息”预览
    const convIndex = conversations.value.findIndex(c => c.id === currentConversation.value.id)
    if (convIndex !== -1) {
      // 使用展开运算符创建新对象以触发 Vue 响应式更新
      conversations.value[convIndex] = {
        ...conversations.value[convIndex],
        lastMessageContent: '', // 清空预览
        lastMessageTime: null   // 可选：清空时间或保持原样
      }
    }
    
    message.success('聊天记录已清空')
    
  } catch (error) {
    console.error(error)
    message.error('清空失败: ' + (error.message || '网络错误'))
  }
}


const selectConversation = async (conv) => {
  if (currentConversation.value?.id === conv.id) return
  currentConversation.value = conv
  messages.value = []
  
  try {
    const res = await getMessages(conv.id, { page: 1, size: 50 })
    const list = res.data?.records || res.data || []
    messages.value = list.reverse()
    nextTick(() => scrollToBottom('auto'))
  } catch (error) {
    message.error('加载消息失败')
  }
}

const handleSendMessage = () => {
  if (!inputMessage.value.trim() || !ws || ws.readyState !== WebSocket.OPEN) {
    if(!ws || ws.readyState !== WebSocket.OPEN) message.warning('连接已断开')
    return
  }
  
  const content = inputMessage.value
  inputMessage.value = ''
  
  const msgData = {
    conversationId: currentConversation.value.id,
    messageType: 1,
    content: content
  }
  
  ws.send(JSON.stringify(msgData))
  
  // 乐观更新 UI
  messages.value.push({
    id: Date.now(),
    senderId: userInfo.id,
    senderName: userInfo.username,
    content: content,
    createTime: new Date().toISOString()
  })
}

const connectWebSocket = () => {
  const token = localStorage.getItem('token')
  const wsUrl = `ws://localhost:8080/ws/chat/${userInfo.id}?token=${token}`
  ws = new WebSocket(wsUrl)
  
  ws.onopen = () => console.log('WS Connected')
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'chat' || data.type === 'offline_message') {
        if (currentConversation.value && data.conversationId === currentConversation.value.id) {
          messages.value.push({
            id: data.messageId,
            senderId: data.senderId,
            senderName: data.senderName,
            content: data.content,
            createTime: data.createdAt
          })
        } else {
          loadConversations() // 更新侧边栏预览
        }
      }
    } catch (e) { console.error(e) }
  }
  ws.onclose = () => console.log('WS Closed')
}

// --- 新建会话逻辑 (保持不变) ---
const openCreateModal = async () => {
  showCreateModal.value = true
  createType.value = 'private'
  groupForm.value.name = ''
  selectedGroupMembers.value = []
  if (allFriends.value.length === 0) {
    try {
      const res = await getFriends({ userId: userInfo.id, page: 1, size: 100 })
      allFriends.value = res.data?.records || res.data || []
    } catch (error) { message.error('加载好友失败') }
  }
}

const handleCreatePrivate = async (friendId) => {
  creatingPrivate.value = true
  try {
    const res = await createPrivateChat(friendId)
    message.success('创建成功')
    showCreateModal.value = false
    await loadConversations()
    if (res.data) {
       const target = conversations.value.find(c => c.id === res.data)
       if (target) selectConversation(target)
    }
  } catch (e) { message.error('创建失败') }
  finally { creatingPrivate.value = false }
}

const toggleGroupMember = (id, checked) => {
  if (checked) selectedGroupMembers.value.push(id)
  else selectedGroupMembers.value = selectedGroupMembers.value.filter(i => i !== id)
}

const handleCreateGroup = async () => {
  if (!groupForm.value.name || selectedGroupMembers.value.length === 0) return
  creatingGroup.value = true
  try {
    const res = await createGroupChat({ 
      name: groupForm.value.name, 
      memberIds: selectedGroupMembers.value, 
      type: 2 
    })
    message.success('群聊创建成功')
    showCreateModal.value = false
    await loadConversations()
    if (res.data) {
       const target = conversations.value.find(c => c.id === res.data)
       if (target) selectConversation(target)
    }
  } catch (e) { message.error('创建失败') }
  finally { creatingGroup.value = false }
}

const handleMoreOptions = () => {
  message.info('更多设置功能开发中...')
}

onMounted(() => {
  loadConversations()
  connectWebSocket()
})

onUnmounted(() => {
  if (ws) ws.close()
})
</script>

<style scoped>
/* ✅ 整体容器：Flex 布局，占满全屏 */
.im-container {
  display: flex;
  height: calc(100vh - 40px); /* 根据你项目的 Header 高度调整 */
  background-color: #f5f7fa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* --- 左侧侧边栏 --- */
.sidebar {
  width: 320px;
  background-color: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.search-box {
  padding: 10px 15px;
  background: #fff;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 5px 0;
}

/* 会话项样式 */
.conversation-item {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  cursor: pointer;
  transition: background 0.2s;
}

.conversation-item:hover {
  background-color: #f5f5f5;
}

.conversation-item.active {
  background-color: #e6f4ff;
}

.item-avatar {
  margin-right: 12px;
  flex-shrink: 0;
}

.item-content {
  flex: 1;
  overflow: hidden;
}

.item-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 4px;
}

.item-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}

.item-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-last-msg {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

/* --- 右侧聊天窗口 --- */
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa; /* 聊天背景稍灰，突出气泡 */
  position: relative;
}

/* 顶部 Header */
.chat-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  flex-shrink: 0;
}

.header-info h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.header-subtitle {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

/* 消息列表区域 */
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.empty-history {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}

/* 消息行 */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 80%;
}

.message-row-self {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
  cursor: pointer;
}

.message-bubble-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 100%;
}

.message-row-self .message-bubble-wrapper {
  align-items: flex-end;
}

.sender-name {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  position: relative;
}

/* 对方的消息 */
.message-row:not(.message-row-self) .message-bubble {
  background-color: #fff;
  color: #333;
  border-top-left-radius: 2px;
}

/* 自己的消息 */
.message-row-self .message-bubble {
  background-color: #0084ff; /* QQ/微信蓝 */
  color: #fff;
  border-top-right-radius: 2px;
}

/* Markdown 内容微调 */
.message-text :deep(p) { margin: 0 0 5px 0; }
.message-text :deep(p:last-child) { margin-bottom: 0; }
.message-text :deep(code) {
  background: rgba(0,0,0,0.05);
  padding: 2px 4px;
  border-radius: 3px;
  font-family: monospace;
}
.message-row-self .message-text :deep(code) {
  background: rgba(255,255,255,0.2);
}

.message-meta {
  font-size: 11px;
  color: #bbb;
  margin-top: 2px;
}
.message-row-self .message-meta {
  text-align: right;
}

/* 底部输入区 */
.input-area {
  background: #fff;
  border-top: 1px solid #e0e0e0;
  padding: 10px 20px 20px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-input :deep(.n-input__border) {
  border: none;
  box-shadow: none;
}
.chat-input :deep(.n-input__state-border) {
  display: none;
}

.input-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  gap: 5px;
}

/* 未选中会话的空状态 */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.empty-content {
  text-align: center;
  color: #999;
}

/* Modal 内部样式 (保持之前的) */
.friend-select-container, .group-member-section { display: flex; flex-direction: column; height: 400px; }
.section-title { font-size: 14px; font-weight: bold; margin-bottom: 10px; color: #333; }
.friend-list-scroll { flex: 1; overflow-y: auto; border: 1px solid #eee; border-radius: 4px; padding: 5px; }
.friend-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 10px; border-bottom: 1px solid #f5f5f5; }
.friend-row:hover { background-color: #fafafa; }
.friend-info { display: flex; align-items: center; gap: 10px; }
.friend-name { font-size: 14px; color: #333; }
.checkbox-content { display: flex; align-items: center; gap: 10px; }
:deep(.n-checkbox__label) { padding-left: 8px; }
</style>