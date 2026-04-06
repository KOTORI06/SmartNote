# SmartNote 开发进度记录

## 2026-04-06

### 完成内容
- [x] 配置 MyBatis-Plus 框架（版本 3.5.15，适配 Spring Boot 4）
- [x] 配置驼峰命名自动转换（map-underscore-to-camel-case）
- [x] 生成 8 个实体类：
  - User（用户信息表）
  - Note（笔记核心信息表）
  - Tag（标签定义表）
  - NoteTag（笔记与标签关联表）
  - NoteViewHistory（笔记浏览历史表）
  - FriendRelation（好友关系表）
  - NotePermission（笔记分享权限表）
  - AiAnalysis（AI 分析记录表）
- [x] 创建统一结果响应体 Result<T>（util/Result.java）
- [x] 创建全局异常处理器 GlobalExceptionHandler（config/GlobalExceptionHandler.java）
- [x] 创建自定义业务异常 BusinessException（exception/BusinessException.java）

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
