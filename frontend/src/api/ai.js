import request from '../utils/request'

export const getSessions = () => request.get('/ai/sessions')
export const createSession = (data) => request.post('/ai/sessions', data)
export const getSessionMessages = (sessionId) => request.get(`/ai/sessions/${sessionId}/messages`)
export const deleteSession = (sessionId) => request.delete(`/ai/sessions/${sessionId}`)

export const clearSessionMessages = (sessionId) => request.delete(`/ai/sessions/${sessionId}/messages`)
export const streamChat = (data) => request.post('/ai/chat/completions', data)


// ✅ 修改这里：明确接收 sessionName 字符串，并在内部构造对象
export const renameSession = (sessionId, sessionName) => {
  return request.put(`/ai/sessions/${sessionId}`, {
    sessionName: sessionName
  })
}