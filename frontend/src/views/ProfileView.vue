<template>
  <div class="profile-container">
    <n-card title="个人中心" style="max-width: 600px; margin: 40px auto;">
      <template #header-extra>
        <n-space>
          <!-- ✅ 新增：修改密码按钮 -->
          <n-button type="primary" secondary @click="showPasswordModal = true">
            修改密码
          </n-button>
        <n-button type="error" secondary @click="handleLogout">
          退出登录
        </n-button>
        </n-space>
      </template>

      <!-- 加载状态 -->
      <div v-if="loading" style="text-align: center; padding: 20px;">
        <n-spin size="large" />
        <p>加载中...</p>
      </div>

      <div v-else>
        <div class="avatar-section">
          <n-upload
            :custom-request="handleUploadAvatar"
            :show-file-list="false"
            accept="image/*"
          >
            <n-tooltip trigger="hover">
              <template #trigger>
                <div class="avatar-wrapper">
                  <!-- ✅ 核心修复：使用 v-if 确保只有有 URL 时才渲染 img 标签 -->
                  <!-- ✅ 核心修复：使用 :key 强制 Vue 重新创建 DOM 节点 -->
                  <n-avatar 
                    v-if="userInfo.avatarUrl"
                    :key="userInfo.avatarUrl"
                    :src="userInfo.avatarUrl" 
                    size="large" 
                    class="user-avatar"
                  />
                  
                  <!-- ✅ 核心修复：如果没有 URL，或者图片加载失败，显示默认字母 -->
                  <n-avatar 
                    v-else
                    size="large" 
                    class="user-avatar"
                  >
                    {{ userInfo.username?.charAt(0) || 'U' }}
                  </n-avatar>

                  <!-- 遮罩层 -->
                  <div class="avatar-mask">
                    <n-icon><CloudUploadOutline /></n-icon>
                    <span>更换头像</span>
                  </div>
                </div>
              </template>
              点击上传新头像
            </n-tooltip>
          </n-upload>
        </div>

        <n-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-placement="left"
          label-width="80"
          require-mark-placement="right-hanging"
          style="margin-top: 20px;"
        >
          <n-form-item label="用户名" path="username">
            <n-input 
              v-model:value="formData.username" 
              placeholder="请输入用户名" 
              :disabled="!isEditing"
            />
          </n-form-item>

          <n-form-item label="邮箱">
            <n-input :value="userInfo.email || '未设置'" disabled />
          </n-form-item>

          <n-form-item label="手机号">
            <n-input :value="userInfo.phone || '未设置'" disabled />
          </n-form-item>

          <n-form-item label="座右铭" path="motto">
            <n-input 
              v-model:value="formData.motto" 
              type="textarea" 
              placeholder="写一句你的座右铭吧..." 
              :disabled="!isEditing"
              :autosize="{ minRows: 2, maxRows: 4 }"
            />
          </n-form-item>

          <n-form-item label="注册时间">
            <span>{{ formatTime(userInfo.createTime) }}</span>
          </n-form-item>

          <n-form-item>
            <n-space justify="end">
              <n-button v-if="!isEditing" type="primary" @click="startEdit">
                编辑资料
              </n-button>
              <template v-else>
                <n-button @click="cancelEdit">取消</n-button>
                <n-button type="primary" @click="saveInfo" :loading="saving">
                  保存
                </n-button>
              </template>
            </n-space>
          </n-form-item>
        </n-form>
      </div>
    </n-card>
     <!-- ✅ 新增：修改密码模态框 -->
    <n-modal v-model:show="showPasswordModal" preset="dialog" title="修改密码">
      <n-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-placement="left" label-width="80">
        <n-form-item label="旧密码" path="oldPassword">
          <n-input v-model:value="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password-on="click" />
        </n-form-item>
        <n-form-item label="新密码" path="newPassword">
          <n-input v-model:value="passwordForm.newPassword" type="password" placeholder="8-20位，含字母和数字" show-password-on="click" />
        </n-form-item>
        <n-form-item label="确认密码" path="confirmPassword">
          <n-input v-model:value="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password-on="click" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space justify="end">
          <n-button @click="showPasswordModal = false">取消</n-button>
          <n-button type="primary" @click="handleUpdatePassword" :loading="updatingPassword">确定</n-button>
        </n-space>
      </template>
    </n-modal>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { CloudUploadOutline } from '@vicons/ionicons5'
import { useUserStore } from '../stores/user'
// 确保这个路径下有对应的导出函数
import { getCurrentUser, updateUserInfo, uploadAvatar , updatePassword} from '../api/user'
import { formatTime } from '../utils'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

const loading = ref(true) // 新增加载状态
const isEditing = ref(false)
const saving = ref(false)
const formRef = ref(null)

// ✅ 2. 修改密码相关状态
const showPasswordModal = ref(false)
const updatingPassword = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// ✅ 3. 修改密码验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@#$%^&+=]{8,20}$/, message: '长度8-20位，必须包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value) => {
        return value === passwordForm.newPassword
      },
      message: '两次输入的密码不一致',
      trigger: 'input'
    }
  ]
}



let originalData = {}

const userInfo = ref({
  id: null,
  username: '',
  email: '',
  phone: '',
  avatarUrl: '',
  motto: '',
  createTime: ''
})

const formData = reactive({
  username: '',
  motto: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '长度在 4 到 20 个字符', trigger: 'blur' }
  ],
  motto: [
    { max: 200, message: '座右铭不能超过 200 个字符', trigger: 'blur' }
  ]
}

// 加载用户信息
// ProfileView.vue
const loadUserInfo = async () => {
  loading.value = true
  try {
    console.log('🚀 开始获取用户信息...')
    const res = await getCurrentUser()
    console.log('📥 API 原始响应:', res)
    
    let userData = null

    // 🔍 关键调试：尝试多种可能的数据结构，确保拿到 avatarUrl
    if (res?.data?.code === 200 && res?.data?.data) {
      // 情况 1: 标准 Axios 响应，后端返回 { code, data: { user... } }
      userData = res.data.data
    } else if (res?.code === 200 && res?.data) {
      // 情况 2: 拦截器已解包，res 直接是 { code, data: { user... } }
      userData = res.data
    } else if (res?.id) {
      // 情况 3: 直接返回用户对象
      userData = res
    }

    if (userData) {
      console.log('✅ 解析到的用户数据:', userData)
      console.log('🖼️ 头像 URL 值:', userData.avatarUrl) // 🔍 重点看这里是否有值

      // ✅ 显式赋值所有字段，确保没有遗漏
      userInfo.value = {
        id: userData.id,
        username: userData.username,
        email: userData.email,
        phone: userData.phone,
        avatarUrl: userData.avatarUrl || '', // ✅ 确保 avatarUrl 被赋值
        motto: userData.motto || '',
        createTime: userData.createTime
      }
      
      // 同步更新 Store
      userStore.setUserInfo(userInfo.value)
      
      // 初始化表单
      formData.username = userData.username
      formData.motto = userData.motto || ''
    } else {
      console.error('❌ 无法从响应中提取用户数据', res)
      message.warning('未获取到用户数据')
    }
  } catch (error) {
    console.error('💥 获取用户信息失败', error)
    message.error('获取用户信息失败，请检查登录状态或网络连接')
  } finally {
    loading.value = false
  }
}

// ✅ 4. 处理修改密码
const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (errors) => {
    if (errors) return
    
    updatingPassword.value = true
    try {
      await updatePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      
      message.success('密码修改成功，请重新登录')
      showPasswordModal.value = false
      
      // 修改密码后通常要求重新登录，因为 Token 可能失效或为了安全
      setTimeout(() => {
        userStore.logout()
        router.push('/login')
      }, 1500)
      
    } catch (error) {
      console.error('修改密码失败', error)
      message.error(error.response?.data?.message || '修改密码失败，请检查旧密码是否正确')
    } finally {
      updatingPassword.value = false
    }
  })
}


// 开始编辑
const startEdit = () => {
  originalData = { ...formData }
  isEditing.value = true
}

// 取消编辑
const cancelEdit = () => {
  formData.username = originalData.username
  formData.motto = originalData.motto
  isEditing.value = false
}

// 保存信息
const saveInfo = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (errors) => {
    if (errors) return
    
    saving.value = true
    try {
      const payload = {
        username: formData.username,
        motto: formData.motto
      }
      
      await updateUserInfo(payload)
      
      message.success('保存成功')
      
      // 更新本地显示
      userInfo.value.username = formData.username
      userInfo.value.motto = formData.motto
      
      // 更新 Store
      userStore.setUserInfo({
        ...userStore.userInfo,
        username: formData.username,
        motto: formData.motto
      })
      
      isEditing.value = false
    } catch (error) {
      console.error('保存失败', error)
      message.error(error.response?.data?.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

// 上传头像
const handleUploadAvatar = async ({ file, onFinish, onError }) => {
  try {
    const res = await uploadAvatar(file.file)
    console.log('头像上传响应:', res)
    
    // 注意：这里假设后端返回的 data 直接就是 URL 字符串
    // 如果后端返回的是 { code: 200, data: { url: '...' } }，请改为 res.data.url
    const newAvatarUrl = res.data 
    
    if (newAvatarUrl) {
      userInfo.value.avatarUrl = newAvatarUrl
      userStore.setUserInfo({
        ...userStore.userInfo,
        avatarUrl: newAvatarUrl
      })
      message.success('头像上传成功')
      onFinish()
    } else {
      throw new Error('未获取到新头像地址')
    }
  } catch (error) {
    console.error('上传失败', error)
    message.error('头像上传失败')
    onError()
  }
}

// 登出
const handleLogout = () => {
  userStore.logout()
  message.success('已退出登录')
  router.push('/login')
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  background-color: #e0e0e0; /* 灰色背景，方便看清区域 */
}

/* ✅ 关键：强制头像图片样式 */
.user-avatar {
  width: 80px !important;
  height: 80px !important;
  display: block;
  object-fit: cover;
  z-index: 1; /* 图片层级 */
  position: relative; /* 确保 z-index 生效 */
}

/* ✅ 关键：遮罩层样式 */
.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
  z-index: 2; /* 遮罩层级高于图片，但默认透明 */
  pointer-events: none; /* ✅ 必须：让点击事件穿透遮罩，触发下方的 n-upload */
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.avatar-mask span {
  font-size: 12px;
  margin-top: 4px;
}
</style>