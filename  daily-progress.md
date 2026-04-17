# SmartNote 开发进度记录

## 2026-04-06

### 完成内容
- [x] 配置 MyBatis-Plus 框架（版本 3.5.15，适配 Spring Boot 4）
- [x] 配置驼峰命名自动转换（map-underscore-to-camel-case）
- [x] 创建8个数据库表
- [x] 生成 8 个实体类：
  - User（用户信息表）
  - Note（笔记核心信息表）
  - Tag（标签定义表）
  - NoteTag（笔记与标签关联表）
  - NoteViewHistory（笔记浏览历史表）
  - FriendRelation（好友关系表）
  - NotePermission（笔记分享权限表）
  - AiAnalysis（AI 分析记录表）
- [x] 创建统一结果响应体 Result<T>
- [x] 创建全局异常处理器 GlobalExceptionHandler
- [x] 创建自定义业务异常 BusinessException

### 技术要点
- **MyBatis-Plus**：
  - Spring Boot 4 需使用 `mybatis-plus-spring-boot4-starter` 3.5.13+ 版本
  - 实体类注解：`@TableName`、`@TableId(type = IdType.AUTO)`、`@Data`
  - Mapper 继承 `BaseMapper<Entity>` 获得基础 CRUD 能力
  
- **统一响应格式**：
  - 提供 success()、error()、badRequest()、unauthorized() 等静态方法
  
- **全局异常处理**：
  - 使用 `@RestControllerAdvice` + `@ExceptionHandler`
  - 捕获 BusinessException、ValidationException、NullPointerException 等
  - 统一返回 Result 格式错误信息

- **参数校验**：
  - 需要引入 `spring-boot-starter-validation` 依赖
  - 使用 `@Valid` 或 `@Validated` 触发校验
  - 常用注解：@NotNull、@NotBlank、@Size、@Email、@Pattern

### 遇到的问题
- **MyBatis-Plus 版本兼容性**：初始使用 3.5.9 版本导致启动失败，Spring Boot 4.0.5 需要 3.5.13+ 版本，最终升级到 3.5.15 解决

## 2026-04-07

### 完成内容
- [x] 设计5个controller层的所有接口（暂时能想到的要用到的所有方法）（没实现逻辑业务，只是占位）
  - UserController
    - 用户注册接口 POST(api/users/register)
    - 用户登录接口 POST(api/users/login)
    - 获取当前用户信息接口 GET(api/users/me)
    - 更新用户信息接口 PUT(api/users/me)
    - 上传头像接口 POST(api/users/avatar)
    - 修改密码接口 PUT(api/users/password)
    - 搜索用户接口 GET(api/users/search)
  - NoteController
    - 创建笔记接口 POST(api/notes)
    - 获取笔记列表接口 GET(api/notes)
    - 获取笔记详情接口 GET(api/notes/{id})
    - 更新笔记接口 PUT(api/notes/{id})
    - 删除笔记接口 DELETE(api/notes/{id})
    - 获取笔记浏览历史接口 GET(api/notes/history)
    - 添加/删除笔记标签接口 POST(api/notes/{id}/tags)
    - 编辑共享笔记接口 PATCH(api/notes/{id})
    - 创建笔记新标签接口 POST(api/notes/tags)
    - 删除笔记标签接口 DELETE(api/notes/tags/{id})
    - 获取笔记标签列表接口 GET(api/notes/tags)
  - ShareController
    - 创建/更新笔记分享权限接口 POST(api/shares)
    - 获取笔记分享权限列表接口 GET(api/shares/notes/{noteId})
    - 获取分享给我的笔记接口 GET(api/shares/shared-to-me)
    - 删除笔记分享权限接口 DELETE(api/shares/{permissionId})
    - 检查否有权限访问笔记接口 GET(api/shares/check-permission/{noteId})
    - 获取公开的笔记内容接口 GET(api/shares/public/{noteId})
  - friendController
    - 发送好友请求接口 POST(api/friends/requests)
    - 处理好友请求接口 POST(api/friends/requests/{requestId})
    - 获取收到的好友请求列表接口 GET(api/friends/requests/received)
    - 获取好友列表接口 GET(api/friends)
    - 更新好友分组接口 PATCH(api/friends/{friendId}/group)
    - 删除好友接口 DELETE(api/friends/{friendId})
    - 创建好友分组接口 POST(api/friends/groups)
    - 获取好友分组列表接口 GET(api/friends/groups)
    - 删除好友分组接口 DELETE(api/friends/groups/{groupId})
    - 获取好友详情接口 GET(api/friends/{friendId})
  - ChatController
    - 创建私聊接口 POST(api/chat/private)
    - 创建群聊接口 POST(api/chat/group)
    - 获取聊天列表接口 GET(api/chat/conversations)
    - 获取聊天记录接口 GET(api/chat/conversations/{conversationId}/messages)
    - 清空聊天记录接口 DELETE(api/chat/conversations/{conversationId}/messages)
- [x] 创建每个表对应的mapper
- [x] 配置日志logback.xml
- [x] 将所有可能用到的文件和包都创建出来，构建基本项目结构
- [x] 创建密码加密相关工具类PasswordUtil
- [x] 创建JWT令牌相关工具类JwtUtil,JWT验证拦截器JwtInterceptor
- [x] 创建用户上下文工具类UserContext
- [x] 创建格式校验正则表达式常量类RegexConstant
### 技术要点
- **密码安全**：
  - 使用 BCrypt 算法加密密码，strength=10（1024次迭代）
  - 自动加盐防止彩虹表攻击，每次加密同一密码生成不同密文
  - 时序安全验证防止 timing attack

- **JWT认证机制**：
  - Token有效期7天，载荷存储userId和username
  - HS256签名算法，密钥长度256位
  - 支持Token刷新和即将过期检测

- **用户上下文获取**：
  - 通过 RequestContextHolder 获取当前请求
  - JwtInterceptor 验证Token后将userId存入request attribute
  - Service层通过 UserContext.getCurrentUserId() 静态方法获取

- **@RequestAttribute参数注入**：
  - 从拦截器存入request的属性中获取userId，避免在每个接口手动解析JWT
  - 参数名需与拦截器中 `request.setAttribute("userId", userId)` 的key一致
  - 若不一致需显式指定：`@RequestAttribute("userId") Long uid`

- **@Validated类级别校验**：
  - Controller类添加 `@Validated` 注解，支持方法参数的校验（如 `@NotBlank`）
  - DTO对象使用 `@Valid` 触发嵌套校验，确保请求数据合法性
  - 校验失败由全局异常处理器捕获，返回统一错误信息

- **RESTful API设计规范**：
  - GET：查询资源（列表、详情、历史记录）
  - POST：创建资源（注册、创建笔记、发送好友申请）
  - PUT：完整更新资源（更新用户信息、修改密码）
  - PATCH：部分更新资源（编辑共享笔记、更新好友分组）
  - DELETE：删除资源（删除笔记、删除好友、删除权限）

- **分页参数默认值设置**：
  - 使用 `@RequestParam(defaultValue = "1")` 设置页码默认值
  - 使用 `@RequestParam(defaultValue = "20")` 设置每页大小默认值
  - 避免前端未传参时出现null导致异常

- **可选参数处理**：
  - 使用 `@RequestParam(required = false)` 标记非必填参数
  - Service层通过 `if (param != null)` 判断是否添加查询条件
  - 支持灵活的多条件组合查询（标题、标签、文件夹等）

- **文件上传接口设计**：
  - 使用 `@RequestParam("file") MultipartFile file` 接收文件
  - 在Service层进行文件大小、类型校验
  - 返回文件访问URL而非文件对象本身

- **路径变量与请求体混合使用**：
  - `@PathVariable` 获取URL路径中的资源ID（如 `/notes/{id}`）
  - `@RequestBody` 接收JSON格式的请求体数据
  - 两者可在同一接口中配合使用

- **流式响应特殊处理**：
  - SSE接口使用 `produces = "text/event-stream"` 声明响应类型
  - 返回 `SseEmitter` 对象而非 `Result`，绕过统一响应封装
  - 设置超时时间防止长任务中断（PDF分析120秒，普通分析60秒）

- **日志记录规范**：
  - 每个接口入口记录关键参数（userId、业务ID、操作类型）
  - 使用SLF4J的占位符 `{}` 避免字符串拼接，提升性能
  - 敏感信息（如密码）不记录到日志中

- **构造器注入最佳实践**：
  - 使用Lombok的 `@RequiredArgsConstructor` 自动生成构造函数
  - 依赖字段声明为 `private final`，确保不可变性
  - 相比 `@Autowired` 字段注入，更利于单元测试和代码可维护性

## 2026-04-08

### 完成内容
- [x] 完成用户相关功能接口的service层实现
- [x] 完成大部分笔记相关功能接口的service层实现
- [x] 创建用户相关功能接口,大部分笔记相关功能接口用到的用于接收前端请求数据的DTO
- [x] 创建用户相关功能接口,大部分笔记相关功能接口用到的用于返回前端用于展示的数据的VO
- [x] 为每个VO创建由实体类直接转换成VO的静态方法
- [x] 创建阿里云OSS存储服务工具类

### 技术要点
- **阿里云OSS文件存储**：
  - 使用日期分目录存储：`yyyy/MM/UUID.扩展名`，避免单目录文件过多
  - UUID生成唯一文件名防止冲突，保留原始文件扩展名
  - 采用 V4 签名版本（SignVersion.V4），比 V2 更安全
  - 从环境变量读取 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET，避免密钥硬编码

- **事务管理**：
  - `@Transactional(rollbackFor = Exception.class)` 确保所有异常都触发回滚
  - 默认只回滚 RuntimeException，扩展后包含检查型异常

- **LambdaQueryWrapper查询**：
  - 使用方法引用（如 `User::getEmail`）替代字符串字段名，编译期检查避免拼写错误
  - `.or()` 实现多条件 OR 查询（邮箱或手机号登录）
  - `.like()` 实现模糊搜索，`.orderByDesc()` 排序

- **权限控制策略**：
  - 所有者权限：直接通过 `note.getUserId().equals(userId)` 判断
  - 协作者权限：查询 NotePermission 表，检查 granteeId 和 permissionType
  - 共享笔记编辑隔离：所有者使用完整更新接口，协作者使用 editSharedNote 接口（职责分离）

- **逻辑删除处理**：
  - 常规查询自动过滤 is_deleted=1 的记录
  - 回收站功能使用自定义 SQL 绕过全局逻辑删除配置
  - 复原时直接更新 is_deleted=0，不依赖 MyBatis-Plus 的逻辑删除插件

- **标签管理策略**：
  - 新增标签：遍历 tagIds 批量插入 NoteTag 关联记录
  - 更新标签：先删除该笔记的所有关联，再重新插入，确保数据一致性
  - 删除标签：同时清理 NoteTag 关联记录和 Tag 主表记录

- **分页数据处理**：
  - 按标签查询时使用内存分页：先查全量数据 → 排序 → subList 截取
  - Entity 转 VO 时关联查询标签和 AI 摘要，使用 Stream.map() 转换
  - 手动构建 Page<VO> 对象，保留 total、current、size 信息

## 2026-04-09

### 完成内容
- [x] 完成好友相关功能接口的service层实现
- [x] 完成剩下的笔记相关功能接口的service层实现
- [x] 创建好友相关功能接口,部分笔记相关功能接口用到的用于接收前端请求数据的DTO
- [x] 创建好友相关功能接口,部分笔记相关功能接口用到的用于返回前端用于展示的数据的VO
- [x] 为每个VO创建由实体类直接转换成VO的静态方法
- [x] 创建了新表friend_group,创建了新表的实体类Group
- [x] 创建了好友关系状态常量类FriendStatus

### 技术要点
- **好友关系双向记录策略**：
  - 发送申请时只创建单条记录（userId → friendId，status=0）
  - 同意好友后创建双向记录（A→B 和 B→A，status=1），确保双方都能看到好友
  - 拒绝时保持单条记录（status=2），不创建反向记录

- **好友状态管理**：
  - status=0：待处理（申请已发送）
  - status=1：已是好友（双向记录）
  - status=2：已拒绝（单条记录）
  - status=3：已删除（软删除，保留记录可恢复）
  - 重新添加被拒绝/删除的好友时，复用原有记录并重置 status=0

- **分组管理机制**：
  - 默认分组 ID="1"，前端硬编码展示，数据库中不创建实体记录
  - 删除分组时，将该分组下所有好友移至默认分组（groupId="1"）
  - 更新分组前验证分组归属权，防止越权操作

- **删除好友的软删除策略**：
  - 同时查询双向关系（A→B 或 B→A），将两条记录的 status 都改为 3
  - 不物理删除记录，保留历史数据，支持后续恢复或审计

- **权限校验规则**：
  - 处理好友申请时验证 `relation.getFriendId().equals(userId)`，确保只有接收方能处理
  - 更新分组时验证好友关系存在且 status=1，防止操作非好友
  - 删除分组时保护默认分组（groupId=1），抛出 BusinessException

- **VO转换与数据关联**：
  - 好友列表查询时关联 User 表获取好友信息，Group 表获取分组名称
  - 过滤 null VO（好友已被删除的情况），保证数据一致性
  - 手动构建 Page<VO> 对象，从 Page<Entity> 复制 total、current、size

### 遇到的问题
- **问题描述**：创建好友分组表时，我直接用了group做表名，后来使用报错了
  - **解决方案**：更改表名为friend_group
  - **原因分析**：group是关键字，MySQL不支持关键字作为表名

## 2026-04-10

### 完成内容
- [x] 完成分享相关功能接口的service层实现
- [x] 完成Ai相关功能接口的controller层的完全重构
- [x] 创建分享相关功能接口用到的用于接收前端请求数据的DTO
- [x] 创建分享相关功能接口用到的用于返回前端用于展示的数据的VO
- [x] 为每个VO创建由实体类直接转换成VO的静态方法
- [x] 创建分享状态常量类ShareType

### 技术要点
- **分享权限模型设计**：
  - granteeType=1：指定用户分享，granteeId 存储具体用户ID
  - granteeType=2：公开分享（所有人），granteeId 为 NULL
  - permissionType=1：查看权限，permissionType=2：编辑权限

- **权限查询优先级策略**：
  - 检查权限时使用 `.orderByDesc(NotePermission::getPermissionType)`，优先返回高权限记录
  - 同时匹配公开分享（granteeType=2）和指定用户分享（granteeId=userId）
  - 笔记所有者直接授予完全权限（permissionType=2），无需查询权限表

- **权限记录的幂等性处理**：
  - 创建/更新权限时先查询是否存在相同 granteeType + granteeId 的记录
  - 存在则更新 permissionType，不存在则创建新记录
  - 公开分享时 wrapper 添加 `.isNull(NotePermission::getGranteeId)` 条件

- **SSE流式响应机制**：
  - 使用 `SseEmitter` 实现服务器推送事件（Server-Sent Events）
  - PDF分析设置超时120秒（文件处理较慢），笔记分析60秒
  - produces = "text/event-stream" 声明响应类型为流式数据

- **智能路由对话架构**：
  - 统一入口 `/api/ai/chat/completions`，后端自动识别意图（笔记分析/闲聊/知识搜索）
  - 前端无需维护多个AI接口，降低耦合度
  - 流式返回实现打字机效果，提升用户体验

- **会话管理功能**：
  - 支持创建、重命名、删除会话，清空会话消息
  - 获取会话列表和历史消息，支持多轮对话上下文管理
  - 通过 @RequestAttribute 从拦截器获取 userId，确保会话归属权

- **文件类型校验**：
  - PDF上传时同时检查 Content-Type 和文件扩展名（双重验证）
  - 兼容 `application/pdf` 和 `.pdf` 后缀两种判断方式

- **常量类设计规范**：
  - ShareType 使用静态常量定义分享类型，避免魔法数字
  - 私有构造函数防止实例化，作为纯工具类使用
  - GRANTEE_TYPE_USER=1（指定用户），GRANTEE_TYPE_ALL=2（所有人）

## 2026-04-11

### 完成内容
- [x] 完成AI相关功能接口的service层实现
- [x] 创建AI相关功能接口用到的用于接收前端请求数据的DTO
- [x] 创建AI相关功能接口用到的用于返回前端用于展示的数据的VO
- [x] 为每个VO创建由实体类直接转换成VO的静态方法
- [x] 创建AI提示词常量类AiPromptConstant
- [x] 创建PDF解析工具类PdfUtils
- [x] 创建AI可调用的工具类AiTools
- [x] 创建ChatClient配置类AiConfig
- [x] 创建新表chat_session(会话记录表),创建新表的实体类ChatSession
- [x] 创建新表chat_message(会话消息表),创建新表的实体类ChatMessage

### 技术要点
- **Spring AI ChatClient配置**：
  - 创建两个Bean：`chatClient`（启用工具调用）和 `simpleChatClient`（纯对话）
  - 通过 `.defaultTools(aiTools)` 注册AI可调用的后端方法
  - AI模型根据用户意图自动决定是否调用工具函数

- **PDF文本提取处理**：
  - 使用 Apache PDFBox 库解析PDF文件（`PDDocument.load()`）
  - 文件大小限制10MB，防止内存溢出
  - 文本长度截断至20000字符，避免超出AI Token限制
  - 检测扫描版PDF（无文本内容），抛出友好提示

- **SSE流式响应实现**：
  - 使用 `doOnNext(chunk -> ...)` 实时接收AI返回的文本片段
  - 通过 `emitter.send(SseEmitter.event().name("message").data(chunk))` 推送给前端
  - `doOnComplete()` 中保存完整内容到数据库并发送完成事件
  - `doOnError()` 捕获异常并向前端发送错误信息

- **AI工具函数设计（@Tool注解）**：
  - 使用 `@Tool(name, description)` 标注可供AI调用的方法
  - `@ToolParam(description)` 为参数添加描述，帮助AI理解用途
  - 工具方法返回结构化文本而非JSON，便于AI理解和展示
  - 通过 ThreadLocal 传递 userId，解决工具方法签名不支持额外参数的问题

- **ThreadLocal用户上下文管理**：
  - AI调用工具前设置：`AiTools.setCurrentUser(userId)`
  - 工具方法内获取：`getCurrentUserId()` 从 ThreadLocal 读取
  - finally块清除：`AiTools.clearCurrentUser()` 防止内存泄漏
  - 确保多线程环境下用户隔离

- **智能意图识别机制**：
  - 先调用简单ChatClient识别意图（NOTE_ANALYSIS/KNOWLEDGE_SEARCH/CHAT）
  - 根据意图选择对应的角色提示词（Role Prompt）
  - 意图识别失败时降级为闲聊模式（默认CHAT）

- **Prompt模板化管理**：
  - 使用常量类 AiPromptConstant 统一管理所有提示词
  - 角色设定、分析指令、输出格式分离定义
  - PDF总结使用 Java Text Block（三引号）保持格式清晰
  - 通过 `String.format()` 动态注入变量内容

- **会话消息持久化策略**：
  - 用户提问后立即保存（role="user"）
  - AI回复完成后保存完整内容（role="assistant"）
  - 每次保存后更新会话的 updateTime，用于排序
  - 删除会话时物理删除关联消息，避免孤儿数据

- **历史消息上下文构建**：
  - 只保留最近10条消息，避免超出Token限制
  - 按时间升序排列，符合对话自然顺序
  - 拼接格式：`用户：xxx\n\n助手：xxx\n\n`
  - 在Prompt开头添加角色设定，中间插入历史对话，末尾追加当前问题

- **笔记分析幂等性处理**：
  - 查询是否存在相同类型的分析记录（noteId + analysisType）
  - 存在则更新（重新分析），不存在则插入（首次分析）
  - 使用 `.last("LIMIT 1")` 获取最新一条记录

- **资源管理与异常处理**：
  - PDF解析后在 finally 块关闭 PDDocument，释放内存
  - SSE连接中断时抛出 RuntimeException 终止流式处理
  - 区分 BusinessException（业务校验失败）和 Exception（系统错误）
  - 所有异常都通过 `sendError()` 通知前端并关闭连接


### 遇到的问题
- **问题描述**：目前AI工具类中，Spring ai新版本不识别@Tool注解的参数，导致工具方法无法调用
  - **试过的方法**：还未解决，目前试过3,4种配置工具类的方法，均无法解决，要降低版本的话害怕影响其他AI功能
  - **原因分析**：新版本Spring AI 2.0.0版本中，@Tool注解的参数无法识别，导致工具方法无法调用

## 2026-04-12

### 完成内容
- [x] 完成聊天相关功能接口的service层实现
- [x] 创建聊天相关功能接口用到的用于接收前端请求数据的DTO
- [x] 创建聊天相关功能接口用到的用于返回前端用于展示的数据的VO
- [x] 为每个VO创建由实体类直接转换成VO的静态方法
- [x] 创建新表chat_conversation(会话记录表),创建新表的实体类ChatConversation
- [x] 创建新表chat_conversation_member(会话成员表),创建新表的实体类ChatConversationMember
- [x] 创建新表chat_message_new(会话消息表),创建新表的实体类ChatMessageNew
- [x] 创建WebSocket实时聊天处理器ChatWebSocket
- [x] 创建WebConfig配置类
- [x] 用Apifox测试所有接口功能，修复逻辑错误
- [x] 用Apifox生成接口文档

### 技术要点
- **WebSocket端点配置**：
  - 使用 `@ServerEndpoint("/ws/chat/{userId}")` 声明WebSocket端点
  - 路径参数 `{userId}` 通过 `@PathParam` 提取，用于身份标识
  - WebSocket由Jakarta EE容器管理，不是Spring Bean，需特殊处理依赖注入

- **静态Mapper注入方案**：
  - WebSocket实例由容器创建，无法直接使用 `@Autowired` 字段注入
  - 通过 `@Autowired` 标注的setter方法手动注入Mapper到静态变量
  - Spring启动时调用setter，所有WebSocket实例共享同一Mapper引用

- **在线用户会话管理**：
  - 使用 `ConcurrentHashMap<Long, Session>` 存储在线用户（线程安全）
  - Key为用户ID，Value为WebSocket Session对象
  - static保证所有WebSocket实例共享同一个映射表

- **JWT Token认证机制**：
  - WebSocket握手后无法访问HTTP Header，Token通过URL查询参数传递
  - 从 `session.getQueryString()` 提取token参数
  - 验证Token有效性及userId匹配性，防止身份伪造

- **离线消息推送策略**：
  - 用户上线时查询所有未读消息（is_read=0）
  - 验证用户是否为会话成员后再推送（权限控制）
  - 推送后立即标记为已读，避免重复推送

- **消息广播机制**：
  - 发送消息时遍历会话所有成员，排除发送者本人
  - 使用 `session.getBasicRemote().sendText()` 同步发送
  - 检查 `session.isOpen()` 避免向断开连接发送

- **用户在线状态广播**：
  - 上线/下线时遍历 `onlineUsers` 通知其他用户
  - 前端据此更新好友列表的在线状态显示
  - 消息格式：`{type: "user_status", userId: xxx, isOnline: true/false}`

- **JSON序列化与消息协议**：
  - 使用Jackson ObjectMapper进行Java对象与JSON互转
  - 统一消息格式：`{type, messageId, conversationId, senderId, content, ...}`
  - 消息类型区分：chat（实时消息）、offline_message（离线消息）、error（错误）、user_status（状态）

- **异常处理与资源清理**：
  - `@OnError` 捕获网络异常、协议错误等
  - 关闭会话时使用 `CloseReason.NORMAL_CLOSURE` 正常关闭
  - 清理操作不抛出异常，避免干扰容器回收资源

- **权限校验规则**：
  - 发送消息前验证用户是否为会话成员（`isMemberOfConversation`）
  - 查询 `chat_conversation_member` 表确认 membership
  - 非成员发送消息时返回错误提示并拒绝处理

- **私聊会话去重策略**：
  - 创建私聊前先遍历所有未删除的私聊会话
  - 检查两个用户是否同时存在于某个会话的成员表中（`count == 2`）
  - 存在则返回已有会话ID，避免重复创建

- **群聊成员批量添加**：
  - 创建者自动加入会话
  - 遍历 `memberIds` 批量添加其他成员
  - 排除创建者本人（`!memberId.equals(userId)`），防止重复插入

- **会话列表最后消息预览**：
  - 查询用户参与的所有会话后，为每个会话补充额外信息
  - 使用 `.last("LIMIT 1")` 获取最新一条消息（按 `createdAt` 降序）
  - 设置 `lastMessageContent` 和 `lastMessageTime`，前端展示类似微信聊天列表

- **分页查询延迟关联优化**：
  - 调用自定义 Mapper 方法 `selectMessagesByConversation` 实现延迟关联
  - 先通过子查询获取符合条件的消息ID，再关联查询完整数据
  - 避免大偏移量分页时扫描大量无用数据，提升性能

- **会话成员权限校验**：
  - 查看消息、清空消息前调用 `checkMember()` 验证权限
  - 查询 `chat_conversation_member` 表确认用户是否为会话成员
  - 非成员抛出 `BusinessException("无权访问该会话")`

- **会话更新时间维护**：
  - 清空消息后调用 `updateConversationTime()` 更新 `updatedAt`
  - 会话列表按 `updatedAt` 降序排列，确保活跃会话排在前面
  - 新消息由 WebSocket 或触发器更新（Service层不处理）

- **发送者姓名填充**：
  - 消息VO转换时调用 `getUserName()` 查询发送者用户名
  - 用户不存在时返回"未知用户"，避免前端显示null
  - 每条消息单独查询，可优化为批量查询减少数据库交互

- **事务边界控制**：
  - 创建会话、添加成员在同一事务中（`@Transactional`）
  - 清空消息使用物理删除，不可恢复
  - 会话本身不删除，只清空消息记录




## 2026-04-13

### 完成内容
- [x] 添加笔记回收站功能接口，复原笔记功能接口
- [x] 开始写部分前端代码（登录注册页面，笔记页面）
- [x] 创建Spring Security安全配置类放行所有接口

### 技术要点
- **CSRF防护禁用**：
  - `.csrf(csrf -> csrf.disable())` 关闭CSRF保护
  - JWT认证场景下无需CSRF防护（无状态认证）

- **接口权限控制**：
  - `/api/users/register`、`/api/users/login`、`/api/shares/public/**` 公开访问
  - `.anyRequest().permitAll()` 其他接口暂时全部放行（后续可改为 `.authenticated()`）
  - 实际认证由 JwtInterceptor 拦截器处理，Security层只做基础配置

### 遇到的问题
- **问题描述**：预检请求访问跨域请求失败，跨域请求失败
  - **解决方案**：创建SecurityConfig配置类，放行所有请求，所有认证由JwtInterceptor处理，并添加@EnableWebSecurity注解启动
  - **原因分析**：Spring Security 默认开启 CSRF 保护，但 JWT 认证场景下无需 CSRF 保护\

## 2026-04-14

### 完成内容
- [x] 这天只搞了前端

### 技术要点

## 2026-04-15

### 完成内容
- [x] 前端代码编写
- [x] 后端对于使用逻辑删除的表的查询，更新方法使用的SQL语句改为自己编写，不使用MP框架自带方法

### 技术要点
- **自定义SQL绕过逻辑删除**：
  - MyBatis-Plus 全局配置逻辑删除后，`selectById()`、`updateById()` 等方法会自动添加 `is_deleted=0` 条件
  - 回收站功能需要查询已删除数据（`is_deleted=1`），必须使用自定义 SQL 方法
  - `selectDeletedNotes()`、`selectByIdIgnoreLogic()`、`restoreNoteById()` 均在 mapper 中手写 SQL，不使用 MP 自带方法

### 遇到的问题
- **问题描述**：复原逻辑删除笔记时，无法获取逻辑删除的笔记ID，无法更新逻辑删除的笔记
  - **解决方案**：手写SQL绕过逻辑删除，使用自定义SQL方法查询逻辑删除的笔记ID，并使用自定义SQL方法更新逻辑删除的笔记
  - **原因分析**：配置了全局逻辑删除，导致查询逻辑删除的笔记时，自动添加了 `is_deleted=0` 条件，无法获取逻辑删除的笔记ID

## 2026-04-16

### 完成内容
- [x] 完成全部前端代码
- [x] 优化消息的列表查询，采用延迟关联解决深分页查询的性能问题

### 技术要点
- **延迟关联优化深分页**：
  - 传统分页 `LIMIT 10000, 20` 会扫描前10020条数据再丢弃前10000条，性能极差
  - 延迟关联方案：先通过子查询只查ID（覆盖索引，无需回表），再用ID关联查询完整数据
  - SQL结构：`SELECT m.* FROM chat_message_new m INNER JOIN (SELECT id FROM ... LIMIT offset, size) t ON m.id = t.id`

- **覆盖索引加速子查询**：
  - 子查询只选择 `id` 字段，若 `conversation_id + created_at` 有联合索引，可直接从索引树获取数据
  - 避免回表查询完整行数据，减少I/O开销
  - 外层JOIN通过主键ID精确查找，效率远高于OFFSET跳过大量记录




