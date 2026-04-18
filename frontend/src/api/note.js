import request from '../utils/request'

export const getNotes = (params) => request.get('/notes', { params })
export const getNoteDetail = (id) => request.get(`/notes/${id}`)
export const createNote = (data) => request.post('/notes', data)
export const updateNote = (id, data) => request.put(`/notes/${id}`, data)
export const deleteNote = (id) => request.delete(`/notes/${id}`)
export const getTags = (params) => request.get('/notes/tags', { params })
export const createTag = (data) => request.post('/notes/tags', data)
export const deleteTag = (id) => request.delete(`/notes/tags/${id}`)
export const getHistory = (params) => request.get('/notes/history', { params })

export const getNoteHistory = (params) => {
  return request({
    url: '/notes/history',
    method: 'get',
    params
  })
}


/**
 * 获取已删除笔记列表（回收站）
 */
export const getDeletedNotes = (params) => {
  return request({
    url: '/notes/deleted',
    method: 'get',
    params
  })
}

/**
 * 复原笔记
 */
export const restoreNote = (id) => {
  return request({
    url: `/notes/${id}/restore`,
    method: 'put'
  })
}
