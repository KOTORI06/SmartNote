<template>
  <div class="login-container">
    <n-card title="SmartNote" class="login-card">
      <n-tabs v-model:value="activeTab" type="line" animated>
        <n-tab-pane name="login" tab="登录">
          <n-form ref="loginFormRef" :model="loginForm" :rules="loginRules">
            <n-form-item path="account">
              <n-input v-model:value="loginForm.account" placeholder="邮箱/手机号" />
            </n-form-item>
            <n-form-item path="password">
              <n-input v-model:value="loginForm.password" type="password" placeholder="密码" show-password-on="click" />
            </n-form-item>
            <n-button type="primary" block @click="handleLogin">登录</n-button>
          </n-form>
        </n-tab-pane>
        <n-tab-pane name="register" tab="注册">
          <n-form ref="registerFormRef" :model="registerForm" :rules="registerRules">
            <n-form-item path="username">
              <n-input v-model:value="registerForm.username" placeholder="用户名" />
            </n-form-item>
            <n-form-item path="email">
              <n-input v-model:value="registerForm.email" placeholder="邮箱" />
            </n-form-item>
            <n-form-item path="phone">
              <n-input v-model:value="registerForm.phone" placeholder="手机号" />
            </n-form-item>
            <n-form-item path="password">
              <n-input 
                v-model:value="registerForm.password" 
                type="password" 
                placeholder="密码 (8-20位,含字母和数字)" 
                show-password-on="click"
              />
            </n-form-item>
            
            <!-- ✅ 新增：确认密码输入框 -->
            <n-form-item path="confirmPassword">
              <n-input 
                v-model:value="registerForm.confirmPassword" 
                type="password" 
                placeholder="请再次输入密码" 
                show-password-on="click"
              />
            </n-form-item>

            <n-button type="primary" block @click="handleRegister">注册</n-button>
          </n-form>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { login, register } from '../api/user'
import { useUserStore } from '../stores/user'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

const activeTab = ref('login')
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = ref({ account: '', password: '' })

// ✅ 修改：注册表单增加 confirmPassword 字段
const registerForm = ref({ 
  username: '', 
  email: '', 
  phone: '', 
  password: '', 
  confirmPassword: '' 
})

const loginRules = {
  account: [{ required: true, message: '请输入邮箱或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// ✅ 新增：自定义校验函数，检查两次密码是否一致
const validateConfirmPassword = (rule, value) => {
  if (!value) {
    return new Error('请再次输入密码')
  } else if (value !== registerForm.value.password) {
    return new Error('两次输入的密码不一致')
  }
  return true
}

// ✅ 修改：注册规则增加 confirmPassword 校验，并优化其他字段的正则校验以匹配 OpenAPI
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]{4,20}$/, message: '用户名4-20位，支持中文、字母、数字和下划线', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur', 'change'] }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@#$%^&+=]{8,20}$/, message: '密码需8-20位，包含字母和数字', trigger: 'blur' }
  ],
  // ✅ 新增：确认密码规则
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  await loginFormRef.value.validate()
  try {
    const res = await login(loginForm.value)
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data)
    message.success('登录成功')
    router.push('/')
  } catch (error) {
    message.error(error.response?.data?.message || '登录失败')
  }
}

const handleRegister = async () => {
  // 1. 先进行前端表单校验（包括密码一致性）
  await registerFormRef.value.validate()
  
  try {
    // 2. 构造发送给后端的数据（OpenAPI 定义只需要 username, email, phone, password）
    const payload = {
      username: registerForm.value.username,
      email: registerForm.value.email,
      phone: registerForm.value.phone,
      password: registerForm.value.password
      // 注意：不要发送 confirmPassword 给后端
    }

    await register(payload)
    
    message.success('注册成功，请登录')
    activeTab.value = 'login'
    
    // 可选：清空注册表单
    registerForm.value = { username: '', email: '', phone: '', password: '', confirmPassword: '' }
  } catch (error) {
    message.error(error.response?.data?.message || '注册失败')
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
}
</style>