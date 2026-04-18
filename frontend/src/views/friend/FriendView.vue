<template>
  <div class="friend-container">
    <div class="friend-sidebar">
      <n-tabs v-model:value="activeTab" type="line">
        
        <!-- Tab 1: 好友列表 -->
        <n-tab-pane name="friends" tab="好友列表">
          <div class="list-header">
            <n-button type="primary" size="small" @click="showAddModal = true">
              + 添加好友
            </n-button>
          </div>
          
          <n-spin :show="loading">
            <div v-if="filteredFriends.length === 0" class="empty-tip">
              该分组下暂无好友
            </div>
            <div v-for="friend in friends" :key="friend.id" 
                 class="friend-item"
                 :class="{ active: selectedFriend?.id === friend.id }"
                 @click="selectFriend(friend)">
              <n-avatar :size="40" round :src="friend.avatarUrl">
                {{ friend.username?.charAt(0) }}
              </n-avatar>
              <div class="friend-info">
                <div class="friend-name">{{ friend.username }}</div>
                <!-- 显示所属分组名称 -->
                <div class="friend-group-tag">
                  {{ getGroupNameById(friend.groupId) }}
                </div>
              </div>
            </div>
          </n-spin>
        </n-tab-pane>

        <!-- Tab 2: 好友申请 (保持不变) -->
        <n-tab-pane name="requests" tab="好友申请">
           <n-spin :show="requestLoading">
            <div v-for="req in requests" :key="req.id" class="request-item">
              <n-avatar :size="40" round>{{ req.username?.charAt(0) }}</n-avatar>
              <div class="request-info">
                <div class="request-name">{{ req.username }}</div>
                <div class="request-remark">{{ req.applyRemark }}</div>
              </div>
              <n-space>
                <n-button size="small" type="primary" @click="handleRequest(req.id, 1)">同意</n-button>
                <n-button size="small" type="error" @click="handleRequest(req.id, 2)">拒绝</n-button>
              </n-space>
            </div>
          </n-spin>
        </n-tab-pane>

        <!-- Tab 3: 分组管理 -->
        <n-tab-pane name="groups" tab="分组管理">
          <div class="list-header">
            <n-button type="primary" size="small" @click="showGroupModal = true">
              + 新建分组
            </n-button>
          </div>
          
          <div 
            v-for="group in allGroups" 
            :key="group.id" 
            class="group-item"
            :class="{ active: currentGroupId === group.id }"
            @click="handleGroupClick(group.id)"
          >
            <span>{{ group.name }}</span>
            <span class="group-count">({{ getFriendCountByGroup(group.id) }})</span>
            
            <n-button 
              v-if="group.id !== 1" 
              size="tiny" 
              type="error" 
              secondary
              @click.stop="deleteGroup(group.id)"
            >
              删除
            </n-button>
          </div>
        </n-tab-pane>
      </n-tabs>
    </div>

    <!-- 右侧详情区域 -->
    <div class="friend-detail">
      <template v-if="selectedFriend">
        <n-card title="好友详情">
          <template #header-extra>
             <n-tag :type="selectedFriend.status === 1 ? 'success' : 'default'">
               {{ selectedFriend.status === 1 ? '在线' : '离线' }}
             </n-tag>
          </template>
          
          <div class="detail-content">
            <div class="avatar-section">
               <n-avatar :size="80" round :src="selectedFriend.avatarUrl">
                 {{ selectedFriend.username?.charAt(0) }}
               </n-avatar>
               <h3>{{ selectedFriend.username }}</h3>
               <p class="username">@{{ selectedFriend.username }}</p>
            </div>

            <!-- ✅ 修改：去掉了备注，只展示基本信息 -->
            <n-descriptions label-placement="left" bordered :column="1">
              <n-descriptions-item label="用户ID">
                {{ selectedFriend.id }}
              </n-descriptions-item>
              <n-descriptions-item label="邮箱">
                {{ selectedFriend.email || '未公开' }}
              </n-descriptions-item>
              <n-descriptions-item label="手机号">
                {{ selectedFriend.phone || '未公开' }}
              </n-descriptions-item>
              <n-descriptions-item label="所属分组">
                <!-- ✅ 直接使用后端返回的 groupName，如果为空则显示默认值 -->
  {{ selectedFriend.groupName || '默认分组' }}
              </n-descriptions-item>
              <n-descriptions-item label="座右铭">
                {{ selectedFriend.motto || '这个人很懒，什么都没写' }}
              </n-descriptions-item>
              <n-descriptions-item label="成为好友时间">
                {{ selectedFriend.friendSince || '-' }}
              </n-descriptions-item>
            </n-descriptions>

            <!-- ✅ 修改：编辑表单只保留分组选择 -->
            <n-divider dashed>修改分组</n-divider>
            <n-form label-placement="left" label-width="80">
              <n-form-item label="分组">
                <n-select 
                  v-model:value="editForm.groupId" 
                  :options="groupSelectOptions" 
                  placeholder="选择分组"
                />
              </n-form-item>
            </n-form>
          </div>

          <template #footer>
            <n-space justify="end">
              <n-button type="primary" @click="saveFriendInfo" :loading="saving">保存分组</n-button>
              <n-button type="error" secondary @click="deleteFriend">删除好友</n-button>
            </n-space>
          </template>
        </n-card>
      </template>
      <n-empty v-else description="选择一个好友查看详情" style="margin-top: 100px" />
    </div>

    <!-- Modal: 添加好友 (保持不变) -->
     <n-modal v-model:show="showAddModal" preset="dialog" title="添加好友">
      <div style="margin-bottom: 15px;">
        <n-input-group>
          <n-input 
            v-model:value="searchKeyword" 
            placeholder="输入用户名、手机号或邮箱" 
            @keyup.enter="handleSearch"
          />
          <n-button type="primary" @click="handleSearch" :loading="searching">
            搜索
          </n-button>
        </n-input-group>
      </div>
      <div class="search-results" style="max-height: 300px; overflow-y: auto;">
        <n-empty v-if="!hasSearched" description="请输入关键词搜索好友" />
        <n-empty v-else-if="searchResults.length === 0" description="未找到相关用户" />
        <div v-else v-for="user in searchResults" :key="user.id" class="search-item">
          <div class="user-info">
            <n-avatar :size="40" round :src="user.avatarUrl">
              {{ user.username?.charAt(0) }}
            </n-avatar>
            <div class="user-detail">
              <div class="user-name">{{ user.username }}</div>
              <div class="user-contact">{{ user.email || user.phone }}</div>
            </div>
          </div>
          <n-button 
            size="small" 
            type="primary" 
            :disabled="user.isFriend" 
            @click="openRequestModal(user)"
          >
            {{ user.isFriend ? '已是好友' : '添加' }}
          </n-button>
        </div>
      </div>
    </n-modal>

    <!-- Modal: 发送申请 (保持不变) -->
    <n-modal v-model:show="showRequestModal" preset="dialog" title="发送好友申请">
      <n-form ref="requestFormRef" :model="requestForm">
        <n-form-item label="申请备注" path="applyRemark">
          <n-input 
            v-model:value="requestForm.applyRemark" 
            type="textarea" 
            placeholder="你好，我是..." 
            :autosize="{ minRows: 2, maxRows: 4 }"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space justify="end">
          <n-button @click="showRequestModal = false">取消</n-button>
          <n-button type="primary" @click="confirmSendRequest" :loading="sendingRequest">发送</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- Modal: 新建分组 (保持不变) -->
    <n-modal v-model:show="showGroupModal">
      <n-card title="新建分组" style="width: 400px">
        <n-form ref="groupFormRef" :model="groupForm">
          <n-form-item label="分组名称" path="groupName">
            <n-input v-model:value="groupForm.groupName" />
          </n-form-item>
        </n-form>
        <template #footer>
          <n-button @click="showGroupModal = false">取消</n-button>
          <n-button type="primary" @click="handleCreateGroup">创建</n-button>
        </template>
      </n-card>
    </n-modal>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { useUserStore } from '../../stores/user' 

// ✅ 导入 API
import { 
  getFriends, 
  getFriendDetail, 
  getFriendRequests, 
  sendFriendRequest, 
  handleFriendRequest, 
  getGroups, 
  createGroup, 
  deleteGroup as deleteGroupApi,
  searchUsers,
  updateFriendGroup ,
   deleteFriend as deleteFriendApi // 重命名避免与本地函数名冲突
} from '../../api/friend'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()

// --- 状态定义 ---
const activeTab = ref('friends')
const loading = ref(false)
const requestLoading = ref(false)
const saving = ref(false)

const friends = ref([]) 
const requests = ref([])
const groups = ref([]) 

const selectedFriend = ref(null)
const editForm = ref({ groupId: null })

const currentGroupId = ref(1) 

// 搜索相关
const showAddModal = ref(false)
const searching = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const hasSearched = ref(false)

// 申请相关
const showRequestModal = ref(false)
const sendingRequest = ref(false)
const requestFormRef = ref(null)
const requestForm = ref({ friendId: null, applyRemark: '' })

// 分组创建相关
const showGroupModal = ref(false)
const groupFormRef = ref(null)
const groupForm = ref({ groupName: '' })

// --- 计算属性 ---


const allGroups = computed(() => {
  const defaultGroup = { id: 1, name: '默认分组' }
  // ✅ 修复：确保从后端获取的 group 对象中正确提取名称，兼容 groupName 或 name 字段
  const customGroups = groups.value.map(g => ({
    id: g.id,
    name: g.groupName || g.name || '未命名分组' // 优先使用 groupName
  })).filter(g => g.id !== 1)
  
  return [defaultGroup, ...customGroups]
})

// 简化后的计算属性
const filteredFriends = computed(() => {
  return friends.value || []
})

const groupSelectOptions = computed(() => {
  return allGroups.value.map(g => ({ label: g.name, value: g.id }))
})

// --- 方法 ---


const getGroupNameById = (id) => {
  // 查找时也转为数字
  const group = allGroups.value.find(g => g.id === Number(id))
  return group ? group.name : '未知分组'
}

const getFriendCountByGroup = (groupId) => {
  if (groupId === 1) {
    return friends.value.filter(f => !f.groupId || f.groupId === 1).length
  }
  return friends.value.filter(f => f.groupId === groupId).length
}

// ✅ 修复：加载好友列表，支持按分组ID过滤
const loadFriends = async (groupId = null) => {
  loading.value = true
  try {
    const userId = userStore.userInfo?.id
    if (!userId) {
      message.warning('用户未登录')
      return
    }
    
    // 构建请求参数
    const params = { 
      userId: userId, 
      page: 1, 
      size: 100 
    }
    
    // ✅ 关键：如果传入了 groupId，则加入请求参数，让后端进行过滤
    if (groupId && groupId !== 1) {
      params.groupId = String(groupId) // 后端定义 groupId 为 string
    }
    
    const res = await getFriends(params)
    
    let list = res.data?.records || res.data || []
    
    // 统一将 groupId 转为数字，方便前端后续逻辑（如下拉框选中）
    friends.value = list.map(f => ({
      ...f,
      groupId: f.groupId ? Number(f.groupId) : 1
    }))
    
  } catch (error) {
    console.error(error)
    message.error('加载好友列表失败')
  } finally {
    loading.value = false
  }
}

const selectFriend = async (friend) => {
  // 先显示基本信息，避免界面闪烁
  selectedFriend.value = { ...friend } 
  
  try {
    const res = await getFriendDetail(friend.id)
    const fullInfo = res.data
    
    // ✅ 后端返回的数据结构示例: { id: 1, username: "...", groupId: "2", groupName: "同事" }
    // 直接赋值，selectedFriend 现在就拥有了 groupName
    selectedFriend.value = fullInfo
    
    // ✅ 初始化编辑表单
    editForm.value = {
      groupId: selectedFriend.value.groupId
    }
  } catch (error) {
    console.error('获取好友详情失败', error)
    message.warning('无法获取完整详情，显示基本信息')
    editForm.value = {
      groupId: friend.groupId ? Number(friend.groupId) : 1
    }
  }
}

const handleGroupClick = (groupId) => {
  currentGroupId.value = groupId
  activeTab.value = 'friends' // 确保切回好友列表 Tab
  
  // ✅ 关键：点击分组时，重新加载该分组下的好友
  loadFriends(groupId)
  
  // 提示用户（可选）
  // message.info(`已切换到: ${getGroupNameById(groupId)}`)
}

// FriendView.vue 中的 saveFriendInfo

const saveFriendInfo = async () => {
  if (!selectedFriend.value) return
  
  const newGroupId = Number(editForm.value.groupId)
  const oldGroupId = Number(selectedFriend.value.groupId)

  if (newGroupId === oldGroupId) {
    message.info('分组未发生变化')
    return
  }

  saving.value = true
  try {
    // 1. 发送更新请求 (Body: { groupId: "2" })
    await updateFriendGroup(selectedFriend.value.id, { groupId: String(newGroupId) })
    
    message.success('分组更新成功')
    
    // 2. ✅ 关键：重新加载好友列表，确保左侧列表和过滤逻辑同步
    await loadFriends()
    
    // 3. ✅ 关键：重新获取当前好友的详情
    // 因为后端详情接口直接返回了最新的 groupName，这一步能保证右侧详情页显示完全正确
    const friendInList = friends.value.find(f => f.id === selectedFriend.value.id)
    if (friendInList) {
      await selectFriend(friendInList)
    } else {
      // 如果列表中找不到（极端情况），至少更新本地的 groupId
      selectedFriend.value.groupId = newGroupId
      editForm.value.groupId = newGroupId
    }
    
  } catch (error) {
    console.error('❌ 更新分组失败:', error)
    if (error.response) {
      message.error(`更新失败: ${error.response.data.message || '服务器错误'}`)
    } else {
      message.error('更新失败: ' + (error.message || '网络错误'))
    }
  } finally {
    saving.value = false
  }
}


const deleteFriend = () => {
  if (!selectedFriend.value) return
  
  dialog.warning({
    title: '确认删除',
    content: `确定要删除好友 "${selectedFriend.value.username}" 吗？此操作不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 1. 调用后端删除接口，只传入 friendId
        await deleteFriendApi(selectedFriend.value.id)
        
        message.success('好友已删除')
        
        // 2. 清空右侧详情展示
        selectedFriend.value = null
        
        // 3. 重新加载左侧好友列表
        // 保持当前的分组筛选状态
        await loadFriends(currentGroupId.value === 1 ? null : currentGroupId.value)
        
      } catch (error) {
        console.error('❌ 删除好友失败:', error)
        if (error.response) {
          message.error(`删除失败: ${error.response.data.message || '服务器错误'}`)
        } else {
          message.error('网络异常，删除失败')
        }
      }
    }
  })
}

const deleteGroup = (groupId) => {
  dialog.warning({
    title: '确认删除分组',
    content: '删除分组后，该分组下的好友将移入默认分组。',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteGroupApi(groupId)
        message.success('分组已删除')
        await loadGroups()
        await loadFriends() 
        
        if (currentGroupId.value === groupId) {
          currentGroupId.value = 1
        }
      } catch (error) {
        message.error('删除分组失败')
      }
    }
  })
}

// ✅ 修复：加载分组列表时传入 userId
const loadGroups = async () => {
  try {
    const userId = userStore.userInfo?.id
    if (!userId) return

    const res = await getGroups({ 
      userId: userId,
      page: 1, 
      size: 50 
    })
    groups.value = res.data?.records || res.data || []
  } catch (error) {
    console.error(error)
  }
}

const handleCreateGroup = async () => {
  // 创建分组通常也需要 userId，如果后端要求，需在 createGroup 中处理或在此处附加
  // 假设 createGroup 的 data 中不需要显式传 userId，而是通过 Token 或 Query Param
  // 如果 openapi.yaml 中 POST /api/friends/groups 需要 userId query param:
  const userId = userStore.userInfo?.id
  
  // 注意：目前的 createGroup 实现只发送 body。如果后端需要 userId 在 query 中，
  // 你可能需要临时修改请求或后端逻辑。这里暂按原逻辑，若失败请检查后端是否从 Token 获取 userId
  try {
    await createGroup(groupForm.value)
    message.success('创建成功')
    showGroupModal.value = false
    groupForm.value.groupName = ''
    await loadGroups()
  } catch (error) {
    message.error('创建失败')
  }
}

const handleSearch = async () => {
   if (!searchKeyword.value.trim()) return
   searching.value = true
   hasSearched.value = true
   try {
     const res = await searchUsers(searchKeyword.value)
     let users = res.data?.records || res.data || []
     searchResults.value = users.map(u => ({
       ...u,
       isFriend: friends.value.some(f => f.id === u.id)
     }))
   } catch (e) {
     message.error('搜索失败')
   } finally {
     searching.value = false
   }
}

const openRequestModal = (user) => {
  requestForm.value.friendId = user.id
  requestForm.value.applyRemark = `你好，我是${userStore.userInfo?.username}`
  showRequestModal.value = true
}

const confirmSendRequest = async () => {
   if(!requestForm.value.applyRemark) {
     message.warning('请填写备注')
     return
   }
   sendingRequest.value = true
   try {
     // sendFriendRequest 通常需要当前用户ID，后端可能从 Token 获取，或者需要在 data/body 中携带
     // 如果后端要求 query param userId，需修改 API 封装
     await sendFriendRequest(requestForm.value)
     message.success('申请已发送')
     showRequestModal.value = false
     showAddModal.value = false
   } catch(e) {
     message.error('发送失败')
   } finally {
     sendingRequest.value = false
   }
}

// 处理好友申请：同意或拒绝
const handleRequest = async (requestId, status) => {
  try {
    // 1. 调用后端 API 处理申请
    await handleFriendRequest(requestId, { status })

    // 2. 成功提示
    message.success(status === 1 ? '已同意好友申请' : '已拒绝好友申请')

    // 3. 从当前申请列表中移除该条目（无需重新加载整个列表）
    const index = requests.value.findIndex(req => req.id === requestId)
    if (index !== -1) {
      requests.value.splice(index, 1)
    }

    // 4. 如果是同意，则刷新好友列表（因为新增了一个好友）
    if (status === 1) {
      await loadFriends()
    }

  } catch (error) {
    console.error('❌ 处理好友申请失败:', error)
    if (error.response) {
      message.error(`操作失败: ${error.response.data.message || '服务器错误'}`)
    } else {
      message.error('网络异常，请稍后重试')
    }
  }
}

// ✅ 修复：加载申请列表时传入 userId
const loadRequests = async () => {
  requestLoading.value = true
  try {
    const userId = userStore.userInfo?.id
    if (!userId) return

    const res = await getFriendRequests({ 
      userId: userId,
      status: 0, 
      page: 1, 
      size: 50 
    })
    requests.value = res.data?.records || res.data
  } catch (error) {
    message.error('加载申请失败')
  } finally {
    requestLoading.value = false
  }
}




onMounted(() => {
  loadFriends()
  loadRequests()
  loadGroups()
})
</script>

<style scoped>
/* 样式保持不变 */
.friend-item.active {
  background-color: #e6f7ff;
  border-left: 4px solid #1890ff;
}

.group-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.group-item:hover {
  background: #f5f5f5;
}

.group-item.active {
  background: #e6f7ff;
  color: #1890ff;
  font-weight: bold;
}

.group-count {
  font-size: 12px;
  color: #999;
  margin-left: 5px;
}

.detail-content {
  padding: 10px 0;
}

.avatar-section {
  text-align: center;
  margin-bottom: 20px;
}

.username {
  color: #999;
  font-size: 14px;
  margin-top: 5px;
}

.friend-group-tag {
  font-size: 12px;
  color: #666;
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
  margin-top: 4px;
}
</style>