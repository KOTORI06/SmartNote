<template>
  <n-layout has-sider class="layout">
    <n-layout-sider bordered class="sidebar" :native-scrollbar="false">
      <div class="sidebar-header">
        <h2>SmartNote</h2>
      </div>
      
      <!-- 菜单区域 -->
      <n-menu 
        v-model:value="activeMenu" 
        :options="menuOptions" 
        @update:value="handleMenuClick" 
      />

      <!-- ✅ 新增：底部用户信息区域 -->
      <div class="sidebar-footer">
        <div class="user-info" @click="goToProfile">
          <n-avatar 
            :src="userStore.userInfo.avatarUrl" 
            size="small"
            fallback-src="https://07akioni.oss-cn-beijing.aliyuncs.com/07akioni.jpeg"
          >
            {{ userStore.userInfo?.username?.charAt(0) || 'U' }}
          </n-avatar>
          <span class="username">{{ userStore.userInfo?.username || '未登录' }}</span>
        </div>
        <n-button 
          text 
          type="error" 
          class="logout-btn" 
          @click="handleLogout"
        >
          退出登录
        </n-button>
      </div>
    </n-layout-sider>
    
    <n-layout class="main">
      <router-view />
    </n-layout>
  </n-layout>
</template>

<script setup>
import { ref, h, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NIcon, useDialog, useMessage } from 'naive-ui'
import { 
  ChatbubbleEllipsesOutline, 
  DocumentTextOutline, 
  PeopleOutline, 
  SparklesOutline,
  PersonOutline, // ✅ 新增：个人中心图标
  TimeOutline, // ✅ 1. 在这里导入 TimeOutline 图标
  MailOutline, // ✅ 替换为 MailOutline (这是一个标准存在的图标)
   TrashOutline // ✅ 1. 导入垃圾桶图标
} from '@vicons/ionicons5'
import { useUserStore } from '../stores/user' // ✅ 引入 Store

const router = useRouter()
const route = useRoute()
const dialog = useDialog()
const message = useMessage()
const userStore = useUserStore()

// 根据当前路由高亮菜单
const activeMenu = computed(() => route.name)

const renderIcon = (icon) => () => h(NIcon, null, { default: () => h(icon) })

const menuOptions = [
  { label: '聊天', key: 'Chat', icon: renderIcon(ChatbubbleEllipsesOutline) },
  { label: '笔记', key: 'Note', icon: renderIcon(DocumentTextOutline) },
    { 
    label: '回收站', 
    key: 'RecycleBin', // ⚠️ 确保这个 key 与路由配置中的 name 一致
    icon: renderIcon(TrashOutline) 
  },
  // ✅ 2. 在这里添加“浏览历史”菜单项
  { 
    label: '浏览历史', 
    key: 'NoteHistory', // 确保这个 key 和你路由配置中的 name 一致
    icon: renderIcon(TimeOutline) 
  },
  { label: '好友', key: 'Friend', icon: renderIcon(PeopleOutline) },
  { label: 'AI助手', key: 'AI', icon: renderIcon(SparklesOutline) },
  { 
  label: '分享给我的', 
  key: 'SharedNotes', 
  icon: renderIcon(MailOutline) // 记得导入图标
  }
  // ✅ 可选：也可以把个人中心放在菜单里，如果放在底部则不需要这行
  // { label: '个人中心', key: 'Profile', icon: renderIcon(PersonOutline) }
]

const handleMenuClick = (key) => {
  router.push({ name: key })
}

// ✅ 跳转到个人中心
const goToProfile = () => {
  router.push({ name: 'Profile' })
}

// ✅ 登出逻辑
const handleLogout = () => {
  dialog.warning({
    title: '提示',
    content: '确定要退出登录吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      // 1. 清除 Store 状态 (Token, UserInfo)
      userStore.logout()
      
      // 2. 提示成功
      message.success('已退出登录')
      
      // 3. 跳转到登录页
      router.push('/login')
    }
  })
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.sidebar {
  width: 220px; /* 稍微加宽一点以容纳用户信息 */
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #f0f0f0;
}

/* ✅ 新增：底部区域样式 */
.sidebar-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 15px;
  border-top: 1px solid #f0f0f0;
  background-color: #fff; /* 确保背景不透明 */
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.username {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
}

.logout-btn {
  justify-content: flex-start;
  padding-left: 0;
  font-size: 13px;
}

.main {
  padding: 0; /* 通常主内容区由子组件控制 padding，或者保持默认 */
  height: 100%;
  overflow: hidden; /* 防止出现双重滚动条 */
}
</style>