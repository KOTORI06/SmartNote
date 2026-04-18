import request from '../utils/request'

export const getFriends = (params) => request.get('/friends', { params })
export const getFriendDetail = (friendId) => request.get(`/friends/${friendId}`)
export const getFriendRequests = (params) => request.get('/friends/requests/received', { params })
export const sendFriendRequest = (data) => request.post('/friends/requests', data)
export const handleFriendRequest = (requestId, data) => request.put(`/friends/requests/${requestId}`, data)
export const updateFriendGroup = (friendId, data) => request.patch(`/friends/${friendId}/group`, data)
export const deleteFriend = (friendId) => request.delete(`/friends/${friendId}`)
export const getGroups = (params) => request.get('/friends/groups', { params })
export const createGroup = (data) => request.post('/friends/groups', data)
export const deleteGroup = (groupId) => request.delete(`/friends/groups/${groupId}`)
// ✅ 新增：搜索用户接口 (对应 openapi.yaml 中的 /api/users/search)
export const searchUsers = (keyword) => {
  return request.get('/users/search', {
    params: { keyword }
  })
}

/**
 * 删除好友分组
 * @param {number} groupId - 分组ID
 */
export const deleteGroupApi = (groupId) => {
  return request.delete(`/friends/groups/${groupId}`)
}


/**
 * 删除好友
 * @param {number} friendId - 好友ID
 */
export const deleteFriendApi = (friendId) => {
  return request.delete(`/friends/${friendId}`)
}

/**
 * ✅ 修改：更新好友分组 (严格符合 PATCH + 仅 groupId)
 * @param {number} friendId - 好友ID
 * @param {string|number} groupId - 分组ID
 */
export const updateFriendGroupPatch = (friendId, groupId) => {
  return request.patch(`/friends/${friendId}/group`, {
    groupId: groupId
  })
}