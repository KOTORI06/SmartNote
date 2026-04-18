import request from '../utils/request'

export const createPrivateChat = (friendId) => request.post('/chat/private', null, { params: { friendId } })
export const createGroupChat = (data) => request.post('/chat/group', data)
export const getConversations = () => request.get('/chat/conversations')
export const getMessages = (conversationId, params) => request.get(`/chat/conversations/${conversationId}/messages`, { params })
export const clearMessages = (conversationId) => request.delete(`/chat/conversations/${conversationId}/messages`)


/**
 * 获取好友列表 (用于创建聊天时选择好友)
 * 注意：这里复用 friend 模块的接口，或者如果 chat 模块有独立的好友接口请替换
 */
export const getFriendsForChat = (params) => {
  return request({
    url: '/api/friends',
    method: 'get',
    params
  })
}