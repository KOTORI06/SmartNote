<template>
  <div class="share-manager">
    <!-- 1. 权限状态展示卡片 -->
    <n-card size="small" title="分享与权限" style="margin-bottom: 20px;">
      <template #header-extra>
        <n-button v-if="isOwner" type="primary" size="small" @click="showShareModal = true">
          管理分享
        </n-button>
      </template>
      
      <n-descriptions label-placement="left" bordered :column="1">
        <n-descriptions-item label="我的权限">
          <n-tag :type="permissionTagType">
            {{ permissionText }}
          </n-tag>
        </n-descriptions-item>

         <!-- ✅ 新增：如果是所有者且已公开，显示公开链接 -->
        <n-descriptions-item label="公开链接" v-if="isOwner && isPublic">
          <n-space style="width: 100%">
            <n-input 
              :value="publicUrl" 
              readonly 
              size="small"
              style="flex: 1"
            />
            <n-button size="small" @click="copyPublicUrl">
              复制
            </n-button>
          </n-space>
          <div style="font-size: 12px; color: #999; margin-top: 4px;">
            任何人拥有此链接即可查看
          </div>
        </n-descriptions-item>

        
        <n-descriptions-item label="分享状态" v-if="isOwner">
          {{ shareStatusText }}
        </n-descriptions-item>
      </n-descriptions>
    </n-card>

    <!-- 2. 管理分享模态框 (仅所有者可见) -->
    <n-modal v-model:show="showShareModal" preset="card" title="管理分享权限" style="width: 600px;">
      <n-tabs type="line" animated>
        <!-- Tab 1: 添加新分享 -->
        <n-tab-pane name="add" tab="添加分享">
          <n-form ref="shareFormRef" :model="shareForm" label-placement="left" label-width="80">
            
            <!-- 选择分享对象类型 -->
            <n-form-item label="分享对象" path="granteeType">
              <n-radio-group v-model:value="shareForm.granteeType" name="radiogroup">
                <n-space>
                  <n-radio :value="2">所有人 (公开)</n-radio>
                  <n-radio :value="1">指定好友</n-radio>
                  <!-- 如果有分组功能，可以开启下面这行 -->
                  <!-- <n-radio :value="2">好友分组</n-radio> -->
                </n-space>
              </n-radio-group>
            </n-form-item>

            <!-- 如果选择了指定好友，显示好友选择器 -->
            <n-form-item v-if="shareForm.granteeType === 1" label="选择好友" path="granteeId">
              <n-select
                v-model:value="shareForm.granteeId"
                :options="friendOptions"
                placeholder="请选择要分享的好友"
                filterable
              />
            </n-form-item>

            <!-- 选择权限类型 -->
            <n-form-item label="权限类型" path="permissionType">
              <n-radio-group v-model:value="shareForm.permissionType" name="permgroup">
                <n-space>
                  <n-radio :value="1">可查看 (只读)</n-radio>
                  <n-radio :value="2">可编辑 (读写)</n-radio>
                </n-space>
              </n-radio-group>
            </n-form-item>
          </n-form>
          
          <div style="margin-top: 20px; text-align: right;">
            <n-button type="primary" @click="handleAddShare" :loading="submitting">确认分享</n-button>
          </div>
        </n-tab-pane>

        <!-- Tab 2: 已分享列表 -->
        <n-tab-pane name="list" tab="已分享列表">
          <n-spin :show="loadingShares">
            <n-empty v-if="shareList.length === 0" description="暂无分享记录" />
            <n-list v-else bordered>
              <n-list-item v-for="item in shareList" :key="item.id">
                <template #suffix>
                  <n-popconfirm @positive-click="handleDeleteShare(item.id)">
                    <template #trigger>
                      <n-button size="tiny" type="error" ghost>移除</n-button>
                    </template>
                    确定移除该分享权限吗？
                  </n-popconfirm>
                </template>
                <n-thing :title="getGranteeName(item)" :description="getPermissionDesc(item.permissionType)" />
              </n-list-item>
            </n-list>
          </n-spin>
        </n-tab-pane>
      </n-tabs>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { checkPermission, updateSharePermission, getNoteShares, deleteSharePermission } from '../api/share'
import { getFriends } from '../api/friend'
import { useUserStore } from '../stores/user'

const props = defineProps({
  noteId: {
    type: [Number, String],
    required: true
  },
  ownerId: {
    type: [Number, String],
    required: true
  }
})

const message = useMessage()
const userStore = useUserStore()

// --- 状态定义 ---
const permissionInfo = ref(null) // { hasPermission, permissionType }
const showShareModal = ref(false)
const loadingShares = ref(false)
const submitting = ref(false)
const shareList = ref([])
const friends = ref([])

// ✅ 新增：专门用于权限检查的 loading 状态
const checkingPermission = ref(false)

// 分享表单
const shareFormRef = ref(null)
const shareForm = ref({
  granteeType: 2, // 默认所有人
  granteeId: null,
  permissionType: 1 // 默认只读
})

// --- 计算属性 ---
const isOwner = computed(() => {
  return userStore.userInfo?.id === Number(props.ownerId)
})

const permissionText = computed(() => {
  if (!permissionInfo.value) return '加载中...'
  if (!permissionInfo.value.hasPermission) return '无权限'
  return permissionInfo.value.permissionType === 2 ? '可编辑' : '可查看'
})

const permissionTagType = computed(() => {
  if (!permissionInfo.value || !permissionInfo.value.hasPermission) return 'default'
  return permissionInfo.value.permissionType === 2 ? 'success' : 'info'
})

const shareStatusText = computed(() => {
  // 简单统计
  const publicShare = shareList.value.some(s => s.granteeType === 2)
  if (publicShare) return '公开分享中'
  return `已分享给 ${shareList.value.length} 个对象`
})

const friendOptions = computed(() => {
  return friends.value.map(f => ({
    label: f.username,
    value: f.id
  }))
})

// --- 方法 ---

// ✅ 新增：判断是否公开 (granteeType === 3)
const isPublic = computed(() => {
  if (!shareList.value || shareList.value.length === 0) return false
  // 检查分享列表中是否有 granteeType 为 3 的记录
  return shareList.value.some(item => item.granteeType === 2)
})

// ✅ 修改 publicUrl 计算属性
const publicUrl = computed(() => {
  const baseUrl = window.location.origin // 例如 http://localhost:5173
  // 指向我们刚才创建的前端路由
  return `${baseUrl}/public/note/${props.noteId}`
})



// ✅ 2. 提取加载逻辑为一个独立函数
const loadPermissions = async () => {
  if (!props.noteId) return
  
  loading.value = true
  try {
    const res = await getSharePermissions(props.noteId)
    permissions.value = res.data || []
  } catch (error) {
    console.error('加载权限失败', error)
  } finally {
    loading.value = false
  }
}

// ✅ 3. 监听 noteId 变化
watch(
  () => props.noteId, 
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      // 当笔记ID变化时，重新加载权限
      loadPermissions()
    }
  },
  { immediate: false } // immediate: false 表示不在初始化时立即执行，因为 onMounted 会执行
)


// ✅ 1. 修正 loadPermission 函数
const loadPermission = async () => {
  if (!props.noteId) return
  
  checkingPermission.value = true // ✅ 开启 loading
  try {
    // 调用检查权限接口
    const res = await checkPermission(props.noteId)
    // 假设后端返回结构: { code: 200, data: { hasPermission: true, permissionType: 1 } }
    permissionInfo.value = res.data 
  } catch (error) {
    console.error('检查权限失败', error)
    permissionInfo.value = null
  } finally {
    checkingPermission.value = false // ✅ 关闭 loading
  }
}

// 2. 加载已分享列表
const loadShareList = async () => {
  if (!isOwner.value) return
  loadingShares.value = true
  try {
    const res = await getNoteShares(props.noteId)
    // 假设返回 res.data.records 或 res.data
    shareList.value = res.data.records || res.data || []
  } catch (error) {
    message.error('加载分享列表失败')
  } finally {
    loadingShares.value = false
  }
}


// ✅ 新增：复制链接功能
const copyPublicUrl = async () => {
  try {
    await navigator.clipboard.writeText(publicUrl.value)
    message.success('公开链接已复制到剪贴板')
  } catch (err) {
    message.error('复制失败，请手动复制')
  }
}


// 3. 加载好友列表 (用于选择器)
const loadFriends = async () => {
  try {
    const res = await getFriends({ page: 1, size: 100 })
    friends.value = res.data.records || res.data || []
  } catch (error) {
    console.error('加载好友失败', error)
  }
}

// 4. 添加/更新分享
const handleAddShare = async () => {
  if (shareForm.value.granteeType === 1 && !shareForm.value.granteeId) {
    message.warning('请选择一个好友')
    return
  }

  submitting.value = true
  try {
    await updateSharePermission({
      noteId: props.noteId,
      granteeType: shareForm.value.granteeType,
      granteeId: shareForm.value.granteeId,
      permissionType: shareForm.value.permissionType
    })
    
    message.success('分享设置成功')
    showShareModal.value = false
    loadShareList() // 刷新列表
    loadPermission() // 刷新当前权限（如果是分享给自己测试的话）
  } catch (error) {
    message.error(error.response?.data?.message || '分享设置失败')
  } finally {
    submitting.value = false
  }
}

// 5. 删除分享
const handleDeleteShare = async (permissionId) => {
  try {
    await deleteSharePermission(permissionId)
    message.success('已移除分享')
    loadShareList()
  } catch (error) {
    message.error('移除失败')
  }
}

// 辅助：获取被授权者名称
const getGranteeName = (item) => {
  if (item.granteeType === 2) return '所有人 (公开)'
  // 如果是用户，尝试从列表中找名字，或者直接显示 ID
  const friend = friends.value.find(f => f.id === item.granteeId)
  return friend ? friend.username : `用户 ID: ${item.granteeId}`
}

const getPermissionDesc = (type) => {
  return type === 2 ? '可编辑' : '可查看'
}

// --- 生命周期 ---
onMounted(() => {
  loadPermission()
  if (isOwner.value) {
    loadShareList()
    loadFriends()
  }
})

// 监听所有者变化，重新加载
watch(() => props.ownerId, () => {
  loadPermission()
  if (isOwner.value) {
    loadShareList()
    loadFriends()
  }
})
</script>

<style scoped>
.share-manager {
  margin-top: 20px;
}
</style>