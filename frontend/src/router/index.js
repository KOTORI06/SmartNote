import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import NoteView from '../views/note/NoteView.vue'
import ProfileView from '../views/ProfileView.vue' // 导入新页面
import SharedNotesView from '../views/note/SharedNotesView.vue'


const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },

 // ✅ 新增：公开笔记路由 (独立页面，无 Layout)
  {
    path: '/public/note/:id',
    name: 'PublicNote',
    component: () => import('@/views/note/PublicNoteView.vue'),
    meta: { 
      title: '公开笔记',
      requiresAuth: false // 明确标记不需要登录
    }
  },

  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    redirect: '/chat',
    children: [
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/chat/ChatView.vue')
      },
      {
        path: 'note',
        name: 'Note',
        component: () => import('../views/note/NoteView.vue')
      },
      {
        path: 'friend',
        name: 'Friend',
        component: () => import('../views/friend/FriendView.vue')
      },
      {
        path: 'ai',
        name: 'AI',
        component: () => import('../views/ai/AIView.vue')
      },
      // ✅ 新增：个人中心路由
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/ProfileView.vue')
      },
      {
        path: '/shared-notes',
        name: 'SharedNotes',
        component: SharedNotesView,
        meta: { requiresAuth: true }
      },
      {
        path: '/history',
        name: 'NoteHistory',
        component: () => import('@/views/note/NoteHistoryView.vue'),
        meta: { title: '浏览历史' }
      },
      {
    path: '/recycle-bin',
    name: 'RecycleBin',
    component: () => import('@/views/note/RecycleBin.vue'),
    meta: { 
      title: '回收站',
      requiresAuth: true // 如果需要登录
    }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
