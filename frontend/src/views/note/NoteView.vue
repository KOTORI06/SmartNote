<template>
  <div class="note-container">
    <!-- 左侧列表 -->
    <div class="note-list">
      <!-- 头部：两行布局 -->
      <div class="list-header">
        
        <!-- 第一行：筛选 + 搜索 -->
        <div class="search-row">
          <n-radio-group v-model:value="searchType" name="searchType" size="small">
            <n-radio-button value="title">标题</n-radio-button>
            <n-radio-button value="tag">标签</n-radio-button>
          </n-radio-group>

          <div class="search-input-area">
            <!-- 标题搜索 -->
            <n-input 
              v-if="searchType === 'title'"
              v-model:value="searchText" 
              placeholder="搜索笔记标题" 
              @keyup.enter="handleSearch" 
              clearable
            />
            
            <!-- 标签搜索 -->
            <n-select
              v-else
              v-model:value="selectedTagId"
              :options="tagOptions"
              placeholder="选择标签筛选"
              clearable
              filterable
              @update:value="handleTagSelect"
              :loading="loadingTags"
            />
          </div>

          <!-- 搜索按钮 -->
          <n-button v-if="searchType === 'title'" type="primary" size="small" @click="handleSearch">
            🔍
          </n-button>
        </div>

        <!-- 第二行：排序 + 新建 -->
        <div class="action-row">
          <div class="sort-wrapper">
            <span class="sort-label">排序：</span>
            <n-select 
              v-model:value="sortOrder" 
              :options="sortOptions"
              style="width: 120px"
              size="small"
              @update:value="handleSortChange"
            />
          </div>
          
          <n-button type="success" size="small" @click="handleCreate">+ 新建</n-button>
        </div>
      </div>

      <!-- 滚动容器 -->
      <div class="notes-scroll-container" @scroll="handleScroll">
        <n-spin :show="loading">
          <div class="spin-content-wrapper">
            <div v-for="note in notes" :key="note.id" 
                 class="note-item"
                 :class="{ active: currentNote?.id === note.id }"
                 @click="selectNote(note)">
              <div class="note-title">{{ note.title || '无标题' }}</div>
              
              <!-- 标签展示 -->
              <div v-if="note.tags && note.tags.length" class="note-tags">
                <n-tag v-for="tag in note.tags.slice(0, 2)" :key="tag" size="tiny" type="info" style="margin-right: 4px">
                  {{ tag }}
                </n-tag>
                <span v-if="note.tags.length > 2" class="more-tags">+{{ note.tags.length - 2 }}</span>
              </div>

              <div class="note-preview">{{ note.content?.substring(0, 50) || '无内容' }}</div>
              <div class="note-time">{{ formatTime(note.updateTime) }}</div>
            </div>

            <div v-if="loadingMore" class="loading-more">
              <n-spin size="small" />
              <span>加载中...</span>
            </div>
            <div v-else-if="!hasMore && notes.length > 0" class="no-more">
              没有更多了
            </div>
            <div v-if="!loading && notes.length === 0" class="empty-list">
              <n-empty description="暂无笔记" />
            </div>
          </div>
        </n-spin>
      </div>
    </div>

    <!-- 右侧编辑器 -->
    <div class="note-editor">
      <template v-if="currentNote">
        <div class="editor-header">
          <!-- 标题输入 -->
          <n-input v-model:value="currentNote.title" 
                   placeholder="笔记标题" 
                   size="large"
                   :bordered="false" 
                   class="title-input"/>
          
          <!-- 标签管理区域 -->
          <div class="tags-input-wrapper">
            <!-- 自动完成输入框 (可选，如果你更喜欢用下拉菜单添加标签，可以注释掉这个) -->
            <!-- 
            <n-auto-complete
              v-model:value="newTagInput"
              :options="filteredTagOptions"
              placeholder="输入标签 (支持搜索或新建)"
              @select="handleSelectTag"
              @keydown.enter.prevent="handleCreateNewTag"
              clearable
            /> 
            -->
            
            <div class="tags-management-area">
              <div class="selected-tags-list">
                <n-tag 
                  v-for="(tag, index) in currentNote.tags" 
                  :key="index" 
                  size="small" 
                  closable 
                  @close="removeTagByIndex(index)"
                  type="info"
                >
                  {{ tag }}
                </n-tag>
                
                  <!-- ✅ 正确：包裹在 n-dropdown中，触发 handleDropdownSelect -->
<n-dropdown 
  trigger="click" 
  :options="dropdownTagOptions" 
  @select="handleDropdownSelect"
>
  <n-button text size="tiny" class="add-tag-btn">
    添加标签
  </n-button>
</n-dropdown>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <n-space>
            <n-button type="primary" @click="handleSave">保存</n-button>
            <n-button type="error" @click="handleDelete">删除</n-button>
            <n-button type="info" @click="showAiDrawer = true">🤖 AI分析</n-button>
          </n-space>
        </div>

        <!-- 内容编辑区 -->
        <div class="editor-content">
          <n-input v-model:value="currentNote.content" 
                   type="textarea" 
                   :autosize="{ minRows: 20 }"
                   placeholder="开始写作..." />
        </div>

        <!-- ✅ 2. 内嵌分享管理组件 -->
        <!-- 只有当选中笔记时才显示 -->
        <div class="share-section">
          <ShareManager 
          :key="currentNote?.id"
            :note-id="currentNote.id" 
            :owner-id="currentNote.userId" 
          />
        </div>
      </template>
      
      <!-- 空状态 -->
      <div v-else class="empty-note">
        <n-empty description="选择一个笔记或创建新笔记" />
      </div>
    </div>

    <!-- AI 分析抽屉 -->
    <n-drawer v-model:show="showAiDrawer" :width="600" placement="right">
      <n-drawer-content title="AI 笔记分析">
        <div class="ai-analysis-panel">
          <n-form>
            <n-form-item label="分析类型">
              <n-select 
                v-model:value="analysisType" 
                :options="analysisTypeOptions"
              />
            </n-form-item>
            <n-form-item label="自定义问题（可选）">
              <n-input 
                v-model:value="customPrompt" 
                type="textarea" 
                placeholder="输入你想让AI分析的特定问题..."
                :autosize="{ minRows: 3 }"
              />
            </n-form-item>
            <n-form-item>
              <n-space>
                <n-button type="primary" @click="handleAnalyze" :loading="analyzing">
                  开始分析
                </n-button>
                <n-button v-if="lastAnalysisResult" @click="showLastAnalysis = true">
                  查看上次分析结果
                </n-button>
              </n-space>
            </n-form-item>
          </n-form>

          <n-divider />

          <div v-if="analyzing" class="streaming-result">
            <n-spin size="small" />
            <div class="stream-text">{{ streamingText }}</div>
          </div>

          <div v-else-if="currentAnalysisResult" class="analysis-result">
            <n-card title="分析结果">
              <div class="result-content" v-html="formattedResult"></div>
            </n-card>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>

    <!-- 历史分析模态框 -->
    <n-modal v-model:show="showLastAnalysis" preset="card" title="上次分析结果" style="width: 700px">
      <div v-if="lastAnalysisResult" class="last-analysis-content">
        <n-tag :type="getAnalysisTypeTag(lastAnalysisResult.analysisType)" style="margin-bottom: 10px">
          {{ getAnalysisTypeName(lastAnalysisResult.analysisType) }}
        </n-tag>
        <div class="result-text" v-html="formatMarkdown(lastAnalysisResult.analysisContent)"></div>
        <div class="analysis-time">分析时间: {{ formatTime(lastAnalysisResult.createTime) }}</div>
      </div>
      <n-empty v-else description="暂无历史分析记录" />
    </n-modal>
    <!-- ✅ 新增：手动创建的标签弹窗 -->
    <n-modal v-model:show="showCreateTagModal" preset="dialog" title="创建新标签">
      <n-input 
        v-model:value="newTagManualName" 
        placeholder="请输入标签名称" 
        @keydown.enter="confirmCreateTag"
      />
      <template #action>
        <n-space>
          <n-button @click="showCreateTagModal = false">取消</n-button>
          <n-button type="primary" @click="confirmCreateTag" :loading="creatingTag">
            创建
          </n-button>
        </n-space>
      </template>
    </n-modal>    
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue' // ✅ 添加 watch
import { useMessage, useDialog } from 'naive-ui'
import { getNotes, getNoteDetail, createNote, updateNote, deleteNote, getTags, createTag } from '../../api/note'
import { formatTime } from '../../utils'
// ✅ 1. 导入 ShareManager 组件
import ShareManager from '../../components/ShareManager.vue' 
import { useRouter, useRoute } from 'vue-router' // ✅ 1. 引入 useRoute


const route = useRoute() // ✅ 2. 获取 route 对象


// ✅ 新增：搜索相关状态
const searchType = ref('title') // 'title' | 'tag'
const selectedTagId = ref(null) // 选中的标签ID
const tagOptions = ref([]) // 标签下拉选项 { label: '名字', value: id }

const showTagSelector = ref(false)
const loadingTags = ref(false)
const availableTags = ref([])
const selectedTagIds = ref([])
const newTagName = ref('')

// ✅ 新增：标签自动完成相关状态
const newTagInput = ref('') // 当前输入框的值
const filteredTagOptions = ref([]) // 过滤后的推荐标签


const isLoadingPage = ref(false)

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const notes = ref([])
const currentNote = ref(null)
const searchText = ref('')
const sortOrder = ref('desc')
const currentPage = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)
const loadingMore = ref(false)


const showAiDrawer = ref(false)
const analyzing = ref(false)
const streamingText = ref('')
const currentAnalysisResult = ref('')
const showLastAnalysis = ref(false)

const analysisType = ref('SUMMARY')
const customPrompt = ref('')

// ✅ 2. 优化 dropdownTagOptions 计算属性
const dropdownTagOptions = computed(() => {
  if (!currentNote.value) return []
  
  // 1. 过滤出当前笔记尚未拥有的标签
  const existingOptions = availableTags.value
    .filter(t => {
      // 确保 tagName 是字符串
      const tagName = String(t.name || '')
      // 检查当前笔记是否已包含该标签名
      return !currentNote.value.tags.includes(tagName)
    })
    .map(t => ({
      label: t.name,
      key: t.id, // key 必须是唯一的，这里用 ID
      isExisting: true,
      originalName: t.name
    }))

  // 2. 添加一个特殊的“新建标签”选项
  return [
    ...existingOptions,
    {
      label: '✨ 创建新标签...',
      key: 'CREATE_NEW',
      isExisting: false
    }
  ]
})

// ✅ 3. 修复 handleDropdownSelect，确保异步操作正确执行
// ✅ 新增：手动弹窗相关状态
const showCreateTagModal = ref(false)
const newTagManualName = ref('')
const creatingTag = ref(false)

// ✅ 修改：handleDropdownSelect 改为触发手动弹窗
const handleDropdownSelect = async (key, option) => {
  if (key === 'CREATE_NEW') {
    // 重置输入框并显示弹窗
    newTagManualName.value = ''
    showCreateTagModal.value = true
  } else if (option && option.originalName) {
    // 选择现有标签逻辑不变
    if (!currentNote.value.tags.includes(option.originalName)) {
      currentNote.value.tags.push(option.originalName)
      message.success(`已添加: ${option.originalName}`)
    }
  }
}

// ✅ 新增：确认创建标签的逻辑
const confirmCreateTag = async () => {
  const newName = newTagManualName.value.trim()
  
  if (!newName) {
    message.warning('标签名称不能为空')
    return
  }
  
  if (currentNote.value.tags.includes(newName)) {
    message.warning('该标签已存在于当前笔记中')
    return
  }

  creatingTag.value = true
  try {
    const res = await createTag({ name: newName })
    const newTagObj = res.data || res // 兼容结构
    
    // 更新全局库
    availableTags.value.push(newTagObj)
    // 添加到当前笔记
    currentNote.value.tags.push(newTagObj.name)
    
    message.success('创建并添加成功')
    showCreateTagModal.value = false // 关闭弹窗
  } catch (e) {
    console.error(e)
    message.error('创建失败')
  } finally {
    creatingTag.value = false
  }
}



// ✅ 新增：获取当前用户的所有标签
const loadUserTags = async () => {
  loadingTags.value = true
  try {
    // 假设 getTags 不需要分页或者我们取第一页全部
    const res = await getTags({ page: 1, size: 100 }) 
    const tags = res.data.records || res.data || []
    
    // 转换为 n-select 需要的格式
    tagOptions.value = tags.map(t => ({
      label: t.name,
      value: t.id
    }))
  } catch (error) {
    console.error('加载标签失败', error)
  } finally {
    loadingTags.value = false
  }
}


// 监听输入变化，实时过滤可用标签
watch(newTagInput, (val) => {
  if (!val) {
    filteredTagOptions.value = []
    return
  }
  
  // 从 availableTags 中筛选包含输入值的标签
  // 假设 availableTags 是字符串数组 ['工作', '生活'] 或对象数组 [{name: '工作'}]
  // 这里我们统一处理为字符串比较
  const lowerVal = val.toLowerCase()
  
  // 如果 availableTags 还没加载，先尝试加载
  if (availableTags.value.length === 0 && !loadingTags.value) {
    loadAvailableTags()
  }

  filteredTagOptions.value = availableTags.value
    .map(t => typeof t === 'object' ? t.name : t) // 兼容对象或字符串
    .filter(t => t && t.toLowerCase().includes(lowerVal) && !currentNote.value.tags.includes(t))
    .slice(0, 5) // 最多显示5个建议
    .map(t => ({ label: t, value: t })) // 转换为 n-auto-complete 需要的格式
})

// 当用户从下拉列表选择一个现有标签时
const handleSelectTag = (value) => {
  if (value && !currentNote.value.tags.includes(value)) {
    currentNote.value.tags.push(value)
  }
  newTagInput.value = '' // 清空输入框
  filteredTagOptions.value = []
}

// 当用户直接按回车，且没有选择下拉项时，创建新标签
const handleCreateNewTag = () => {
  const val = newTagInput.value.trim()
  if (val && !currentNote.value.tags.includes(val)) {
    currentNote.value.tags.push(val)
    
    // 可选：如果这个新标签不在全局标签库中，可以异步添加到后端标签库
    if (!availableTags.value.some(t => (typeof t === 'object' ? t.name : t) === val)) {
       // 这里可以选择立即调用 createTag，或者只在保存笔记时隐式创建
       // createTag({ name: val }).then(...) 
    }
  }
  newTagInput.value = ''
  filteredTagOptions.value = []
}

// 删除某个标签
const removeTagByIndex = (index) => {
  currentNote.value.tags.splice(index, 1)
}



// 辅助函数：确保 tags 是字符串数组
const normalizeTags = (tags) => {
  if (!tags) return []
  
  // 情况1：后端返回的是对象数组 [{id: 1, name: '工作'}, ...]
  if (Array.isArray(tags) && tags.length > 0 && typeof tags[0] === 'object') {
    return tags.map(t => t.name).filter(Boolean)
  }
  
  // 情况2：后端返回的是字符串数组 ['工作', '生活'] (兼容旧数据或列表接口)
  if (Array.isArray(tags)) {
    return tags.filter(t => typeof t === 'string' && t.trim() !== '')
  }
  
  return []
}
// 修改 selectNote
const selectNote = async (note) => {
  try {
    const res = await getNoteDetail(note.id)
    console.log('后端返回的笔记详情:', res.data) // ✅ 调试：查看 tags 字段
    
    currentNote.value = { ...res.data }
    
    // ✅ 关键：标准化标签数据
    // 如果后端返回 tags: null 或 undefined，normalizeTags 会返回 []
    // 如果后端返回 tags: [{id:1, name:'A'}]，normalizeTags 会转换为 ['A']
    currentNote.value.tags = normalizeTags(currentNote.value.tags)
    
    console.log('标准化后的标签:', currentNote.value.tags) // ✅ 调试：查看转换结果

    currentAnalysisResult.value = ''
    streamingText.value = ''
  } catch (error) {
    message.error('加载笔记详情失败')
  }
}


const handleSave = async () => {
  if (!currentNote.value) return

  try {
    let finalTagIds = []
    
    // 1. 遍历当前选中的所有标签名称
    for (const tagName of currentNote.value.tags) {
      // 在已加载的全局标签库中查找
      const existingTag = availableTags.value.find(t => 
        (typeof t === 'object' ? t.name : t) === tagName
      )

      if (existingTag) {
        // 情况A：标签已存在，直接获取 ID
        finalTagIds.push(typeof existingTag === 'object' ? existingTag.id : existingTag)
      } else {
        // 情况B：标签是新输入的，需要先创建
        try {
          const res = await createTag({ name: tagName })
          // 假设 createTag 返回的数据结构包含 id 和 name
          const newTag = res.data 
          // 将新标签加入全局库，方便下次使用
          availableTags.value.push(newTag)
          finalTagIds.push(newTag.id)
        } catch (e) {
          console.error(`创建标签 "${tagName}" 失败`, e)
          message.warning(`标签 "${tagName}" 创建失败，已忽略`)
        }
      }
    }

    // 2. 构造符合 OpenAPI 标准的 Payload
    // 注意：只发送后端需要的字段，避免发送多余的前端状态
    const payload = {
      title: currentNote.value.title,
      content: currentNote.value.content,
      folderId: currentNote.value.folderId,
      tagIds: finalTagIds // ✅ 关键：发送 ID 数组
    }
    
    // 3. 调用更新接口
    await updateNote(currentNote.value.id, payload)
    
    // 4. ✅ 关键：同步更新左侧列表
    // 我们需要从 availableTags 中反查出名称，用于左侧列表展示
    const updatedTagNames = finalTagIds.map(id => {
      const t = availableTags.value.find(tag => 
        (typeof tag === 'object' ? tag.id : null) === id
      )
      return t ? (typeof t === 'object' ? t.name : t) : ''
    }).filter(Boolean)

    const index = notes.value.findIndex(n => n.id === currentNote.value.id)
    if (index !== -1) {
      notes.value.splice(index, 1, {
        ...notes.value[index],
        tags: updatedTagNames // 左侧列表存储名称数组，方便展示
      })
    } else {
      // 如果列表中找不到，重新加载第一页
      loadNotes(searchText.value.trim(), sortOrder.value, 1, false)
    }
    
    message.success('保存成功')
  } catch (error) {
    console.error(error)
    message.error('保存失败')
  }
}

// ✅ 1. 确保 loadAvailableTags 正确解析数据
const loadAvailableTags = async () => {
  loadingTags.value = true
  try {
    const res = await getTags({ page: 1, size: 100 }) // 增加 size 确保获取所有标签
    console.log('加载到的原始标签数据:', res.data) // ✅ 调试：查看后端返回结构
    
    // 兼容不同的后端返回结构
    let rawTags = []
    if (res.data && res.data.records) {
      rawTags = res.data.records
    } else if (Array.isArray(res.data)) {
      rawTags = res.data
    }

    // 确保存储的是对象数组 [{ id: 1, name: 'xx' }]
    availableTags.value = rawTags.map(t => ({
      id: t.id,
      name: t.name
    }))
    
    console.log('标准化后的可用标签:', availableTags.value)
  } catch (error) {
    console.error('加载标签失败', error)
    message.error('加载标签列表失败')
  } finally {
    loadingTags.value = false
  }
}


const handleCreateTag = async () => {
  if (!newTagName.value.trim()) {
    message.warning('请输入标签名称')
    return
  }
  
  try {
    const res = await createTag({ name: newTagName.value.trim() })
    availableTags.value.push(res.data)
    newTagName.value = ''
    message.success('标签创建成功')
  } catch (error) {
    message.error('标签创建失败')
  }
}

const handleUpdateTags = async () => {
  try {
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/notes/${currentNote.value.id}/tags`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        tagIds: selectedTagIds.value
      })
    })
    
    if (!response.ok) {
      throw new Error('更新标签失败')
    }
    
    const res = await response.json()
    currentNote.value.tags = res.data
    
    showTagSelector.value = false
    message.success('标签更新成功')
  } catch (error) {
    message.error('更新标签失败: ' + error.message)
  }
}

const removeTag = async (tagId) => {
  const updatedTagIds = selectedTagIds.value.filter(id => id !== tagId)
  selectedTagIds.value = updatedTagIds
  
  try {
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/notes/${currentNote.value.id}/tags`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        tagIds: updatedTagIds
      })
    })
    
    if (!response.ok) {
      throw new Error('移除标签失败')
    }
    
    const res = await response.json()
    currentNote.value.tags = res.data
    message.success('标签已移除')
  } catch (error) {
    message.error('移除标签失败')
    await selectNote(currentNote.value)
  }
}

const openTagSelector = () => {
  loadAvailableTags()
  showTagSelector.value = true
}


const sortOptions = [
  { label: '最新在前', value: 'desc' },
  { label: '最早在前', value: 'asc' }
]

const analysisTypeOptions = [
  { label: '内容总结', value: 'SUMMARY' },
  { label: '标签推荐', value: 'TAGS' },
  { label: '关键词提取', value: 'KEY_POINTS' },
  { label: '自定义分析', value: 'CUSTOM' }
]

const lastAnalysisResult = computed(() => {
  return currentNote.value?.aiAnalyses || null
})

const formattedResult = computed(() => {
  return formatMarkdown(currentAnalysisResult.value)
})

// ✅ 修改：loadNotes 增加 tagId 参数，并处理标签格式
const loadNotes = async (title = '', order = 'desc', page = 1, append = false, tagId = null) => {
  if (!append) {
    loading.value = true
  } else {
    loadingMore.value = true
  }
  
  try {
    const res = await getNotes({ 
      page: page, 
      size: pageSize.value,
      title: title || undefined,
      order: order,
      tagId: tagId || undefined 
    })
    
    let newNotes = res.data.records || res.data
    
    // ✅ 关键步骤：标准化标签数据
    // 后端可能返回 [{id:1, name:'A'}]，前端列表展示需要 ['A']
    newNotes = newNotes.map(note => ({
      ...note,
      tags: normalizeTags(note.tags) // 调用下方的 normalizeTags 函数
    }))
    
    if (append) {
      const existingIds = new Set(notes.value.map(n => n.id))
      const uniqueNewNotes = newNotes.filter(n => !existingIds.has(n.id))
      if (uniqueNewNotes.length > 0) {
        notes.value = [...notes.value, ...uniqueNewNotes]
      }
    } else {
      notes.value = newNotes
    }
    
    hasMore.value = newNotes.length >= pageSize.value
    currentPage.value = page
  } catch (error) {
    message.error('加载笔记失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// ✅ 新增：处理标签选择
const handleTagSelect = (value) => {
  // 当用户选择了一个标签，立即重置页码并搜索
  currentPage.value = 1
  // 如果 value 为 null (清空)，则 tagId 传 undefined
  loadNotes('', sortOrder.value, 1, false, value)
}




// ✅ 修改：原有的 handleSearch 只处理标题搜索
const handleSearch = () => {
  if (searchType.value !== 'title') return
  
  currentPage.value = 1
  // 标题搜索时，tagId 传 null/undefined
  loadNotes(searchText.value.trim(), sortOrder.value, 1, false, null)
}

// ✅ 监听搜索类型切换，如果切到标签模式且没加载过标签，则加载
watch(searchType, (newVal) => {
  if (newVal === 'tag' && tagOptions.value.length === 0) {
    loadUserTags()
  }
  // 切换模式时，可以选择清空之前的搜索结果，或者保留
  // 这里建议清空输入框状态，避免混淆
  if (newVal === 'title') {
    selectedTagId.value = null
  } else {
    searchText.value = ''
  }
})

// ✅ 修改：切换排序时，也要带上当前的筛选条件
const handleSortChange = (value) => {
  currentPage.value = 1
  if (searchType.value === 'tag') {
    loadNotes('', value, 1, false, selectedTagId.value)
  } else {
    loadNotes(searchText.value.trim(), value, 1, false, null)
  }
}

const handleScroll = (e) => {
  const { scrollTop, scrollHeight, clientHeight } = e.target
  
  // 1. 必须确保不在加载中
  // 2. 必须还有更多数据
  // 3. 距离底部小于 10px (更精确)
  if (!isLoadingPage.value && hasMore.value && (scrollHeight - scrollTop - clientHeight < 10)) {
    isLoadingPage.value = true
    loadNotes(searchText.value.trim(), sortOrder.value, currentPage.value + 1, true).finally(() => {
      isLoadingPage.value = false
    })
  }
}

// 修改 handleCreate
const handleCreate = async () => {
  try {
    const res = await createNote({ title: '新笔记', content: '', tags: [] })
    currentNote.value = res.data
    // ✅ 关键：初始化标签为空数组
    currentNote.value.tags = normalizeTags(currentNote.value.tags)
    
    notes.value.unshift(currentNote.value)
    message.success('创建成功')
  } catch (error) {
    message.error('创建失败')
  }
}


const handleDelete = () => {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除这个笔记吗？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteNote(currentNote.value.id)
        const index = notes.value.findIndex(n => n.id === currentNote.value.id)
        if (index > -1) notes.value.splice(index, 1)
        currentNote.value = null
        message.success('删除成功')
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

const handleAnalyze = async () => {
  if (!currentNote.value) {
    message.warning('请先选择一篇笔记')
    return
  }

  analyzing.value = true
  streamingText.value = ''
  currentAnalysisResult.value = ''

  try {
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/ai/notes/${currentNote.value.id}/analysis/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        analysisType: analysisType.value,
        customPrompt: customPrompt.value || undefined
      })
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`分析请求失败: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmedLine = line.trim()
        
        if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.substring(5).trim()
          
          if (data === '[DONE]' || data.includes('分析完成')) {
            continue
          }
          
          if (data && !data.startsWith('{')) {
            streamingText.value += data
            currentAnalysisResult.value = streamingText.value
          }
        }
        
        if (trimmedLine.startsWith('event:')) {
          continue
        }
      }
    }

    message.success('分析完成')
    
    const res = await getNoteDetail(currentNote.value.id)
    currentNote.value = { ...res.data }
  } catch (error) {
    console.error('AI分析错误:', error)
    message.error('AI分析失败: ' + error.message)
  } finally {
    analyzing.value = false
  }
}

const formatMarkdown = (text) => {
  if (!text) return ''
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

const getAnalysisTypeName = (type) => {
  const names = {
    SUMMARY: '内容总结',
    TAGS: '标签推荐',
    KEY_POINTS: '关键词提取',
    CUSTOM: '自定义分析'
  }
  return names[type] || type
}

const getAnalysisTypeTag = (type) => {
  const types = {
    SUMMARY: 'info',
    TAGS: 'success',
    KEY_POINTS: 'warning',
    CUSTOM: 'default'
  }
  return types[type] || 'default'
}

onMounted(async () => {
  // 1. 先检查是否有路由传来的 ID
  const targetId = route.query.id
  
  if (targetId) {
    try {
      // 2. 直接加载该笔记详情
      const res = await getNoteDetail(targetId)
      const noteDetail = res.data
      
      // 3. 标准化标签
      noteDetail.tags = normalizeTags(noteDetail.tags)
      
      // 4. 设置为当前笔记
      currentNote.value = noteDetail
      
      // 5. 将其加入左侧列表（模拟它就在列表里）
      notes.value = [{
        id: noteDetail.id,
        title: noteDetail.title,
        content: noteDetail.content, // 列表可能需要 preview，这里简化处理
        updateTime: noteDetail.updateTime,
        tags: noteDetail.tags
      }]
      
      message.success('已从历史记录加载笔记')
    } catch (e) {
      message.error('加载历史笔记失败')
      // 失败则正常加载列表
      loadNotes()
      loadAvailableTags()
    }
  } else {
    // 没有 ID，正常加载列表
    loadNotes()
    loadAvailableTags()
  }
})

// ✅ 4. 确保 onMounted 时加载标签
onMounted(() => {
  loadNotes()
  loadAvailableTags() // 确保页面加载时获取标签
})
</script>

<style scoped>
.note-container {
  display: flex;
  height: calc(100vh - 40px);
  gap: 20px;
}

.note-list {
  width: 300px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%; /* 确保占满父容器高度 */
}


/* 第二行：操作栏 */
.action-row {
  display: flex;
  justify-content: space-between; /* 两端对齐 */
  align-items: center;
  width: 100%;
}


/* ✅ 新布局：头部垂直排列 */
.list-header {
  padding: 10px;
  display: flex;
  flex-direction: column; /* 关键：垂直布局 */
  gap: 10px;
  border-bottom: 1px solid #f0f0f0;
}


/* 第一行：搜索栏 */
.search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

/* 确保单选框不被压缩 */
.search-row :deep(.n-radio-group) {
  flex-shrink: 0;
}

/* ✅ 优化：标签管理区域样式 */
.tags-management-area {
  margin-right: 10px;
  flex: 1;
  min-width: 0;
}


.add-tag-btn:hover {
  color: #40a9ff;
  border-color: #40a9ff;
  background-color: #e6f7ff;
}

.add-tag-btn {
  color: #1890ff;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  padding: 0 8px;
  height: 24px;
  display: flex;
  align-items: center;
  transition: all 0.3s;
}

.selected-tags-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px; /* 增加间距 */
  min-height: 32px;
}

.sort-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

/* 可选：稍微调整单选按钮组的样式，防止它太宽挤压输入框 */
.list-header :deep(.n-radio-group) {
  flex-shrink: 0; /* 防止单选按钮组被压缩 */
}

/* 强制输入框和选择器填满容器 */
.search-input-area :deep(.n-input),
.search-input-area :deep(.n-select) {
  width: 100%;
}

/* 输入框区域：占据剩余空间 */
.search-input-area {
  flex: 1;
  min-width: 0; /* 防止溢出 */
}

/* 修改 .notes-scroll-container */
.notes-scroll-container {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  position: relative;
  /* ❌ 删除 contain: content; */
  /* ✅ 新增：平滑滚动，减少视觉跳动感 */
  scroll-behavior: smooth; 
}

/* 修改 n-spin 的深度样式 */
.notes-scroll-container :deep(.n-spin) {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: visible; /* ✅ 改回 visible，避免裁剪内容导致计算错误 */
}

/* ✅ 确保 spin-content 占满高度 */
.notes-scroll-container :deep(.n-spin-content) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.spin-content-wrapper {
  flex: 1; /* ✅ 改为 flex: 1，让它自动填充剩余空间 */
  display: flex;
  flex-direction: column;
  min-height: 100%; 
}

.note-item {
  padding: 15px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.note-item:hover {
  background: #f5f5f5;
}

.note-item.active {
  background: #e8f4ff;
}

.note-title {
  font-weight: bold;
  margin-bottom: 8px;
}

.note-preview {
  color: #666;
  font-size: 12px;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-time {
  font-size: 11px;
  color: #999;
}

.note-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.editor-header {
  padding: 15px 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.empty-note {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-analysis-panel {
  padding: 10px;
}

.streaming-result {
  margin-top: 20px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 8px;
}

.stream-text {
  margin-top: 10px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.analysis-result {
  margin-top: 20px;
}

.result-content {
  line-height: 1.8;
}

.last-analysis-content {
  padding: 10px;
}

.result-text {
  line-height: 1.8;
  margin: 15px 0;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
}

.analysis-time {
  font-size: 12px;
  color: #999;
  margin-top: 10px;
}

.loading-more,
.no-more {
  padding: 15px;
  text-align: center;
  color: #999;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.tags-section {
  padding: 10px 20px;
  border-bottom: 1px solid #e0e0e0;
  background: #fafafa;
}

.tag-selector {
  max-height: 400px;
}

.tag-list {
  max-height: 300px;
  overflow-y: auto;
}

/* ✅ 优化：编辑器头部布局，防止标签输入框挤压按钮 */
.editor-header {
  padding: 15px 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px; /* 增加间距 */
}


/* ✅ 新增：列表项标签样式 */
.note-tags {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 6px;
  align-items: center;
}

.more-tags {
  font-size: 10px;
  color: #999;
  margin-left: 2px;
}


.title-input {
  flex: 1; /* 让标题输入框占据剩余空间 */
  min-width: 100px;
}


.tags-input-wrapper {
  margin-right: 10px;
  min-width: 200px;
  flex-shrink: 1;
  display: flex;
  flex-direction: column; /* 让输入框和标签列表垂直排列 */
}

.selected-tags-list {
  display: flex;
  flex-wrap: wrap;
  margin-top: 5px;
  min-height: 24px; /* 防止没有标签时高度塌陷 */
}


.share-section {
  padding: 20px;
  border-top: 1px solid #e0e0e0;
  background-color: #fafafa; /* 稍微区分一下背景色 */
}
</style>
