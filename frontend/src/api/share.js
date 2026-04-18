// src/api/share.js
import request from '../utils/request'

/**
 * 检查当前用户对笔记的访问权限
 */
export const checkPermission = (noteId) => {
  return request.get(`/shares/check-permission/${noteId}`)
}

/**
 * 创建或更新分享权限
 * @param {Object} data - { noteId, granteeType, granteeId, permissionType }
 * granteeType: 1-用户, 2-好友分组, 3-所有人
 * permissionType: 1-可查看, 2-可编辑
 */
export const updateSharePermission = (data) => {
  return request.post('/shares', data)
}

/**
 * 获取笔记的分享权限列表 (仅所有者可见)
 */
export const getNoteShares = (noteId) => {
  return request.get(`/shares/notes/${noteId}`)
}

/**
 * 删除分享权限
 */
export const deleteSharePermission = (permissionId) => {
  return request.delete(`/shares/${permissionId}`)
}


/**
 * 获取分享给我的笔记列表
 */
export const getSharedNotesToMe = (params) => {
  return request.get('/shares/shared-to-me', { params })
}

/**
 * 获取公开笔记详情 (如果需要支持公开链接访问)
 */
export const getPublicNote = (noteId) => {
  return request.get(`/shares/public/${noteId}`)
}


// ✅ 确保路径有 /api，且支持 params 传入 userId
export const updateNote = (id, data, params = {}) => {
  return request.patch(`/notes/${id}`, data)
}

