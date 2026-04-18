import request from '../utils/request'

export const login = (data) => request.post('/users/login', data)
export const register = (data) => request.post('/users/register', data)


// ✅ 新增：获取当前用户信息
export const getCurrentUser = () => {
  return request({
    url: '/users/me',
    method: 'get'
  })
}

/**
 * 更新用户信息 (用户名、座右铭等)
 */
export const updateUserInfo = (data) => {
  return request({
    url: '/users/me', // 或者 /api/users/update，取决于后端具体路径，openapi中是 PUT /api/users/me?userId=xxx
    method: 'put',
    data
  })
}

/**
 * 上传头像
 */
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/users/avatar',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    data: formData
  })
}

/**
 * 修改密码
 */
export const updatePassword = (data) => { // ✅ 注意：这里是 updatePassword，不是 updatePassword
  return request({
    url: '/users/password',
    method: 'put',
    data
  })
}