import { formatDistanceToNow, format } from 'date-fns'
import { zhCN } from 'date-fns/locale'

export const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return formatDistanceToNow(date, { locale: zhCN, addSuffix: true })
  if (diff < 86400000) return format(date, 'HH:mm')
  return format(date, 'yyyy-MM-dd')
}

export const getUserInfo = () => {
  const user = localStorage.getItem('user')
  return user ? JSON.parse(user) : null
}

export const setUserInfo = (user) => {
  localStorage.setItem('user', JSON.stringify(user))
}
