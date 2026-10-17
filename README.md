# 校园寻物 · 校园失物招领管理系统

按照提供的 PPT、讲稿补充材料与 web.zip 课程资料实现。已补齐 JWT 角色鉴权、招领人确认、时间范围筛选，以及公告用途、表关联和时间条目。详见 docs/07-补充材料对照与修改说明.md。

## 一、最快查看效果

已经提供完整打包程序，不需要先安装前端依赖。

1. 确保 JDK 8 已安装，终端执行 java -version 能看到版本。
2. 双击项目根目录 start-demo.cmd，保持窗口运行。
3. 浏览器访问 http://localhost:8081 。
4. 演示管理员：admin / Admin12345；演示学生：20260001 / Student123。也可以自行注册新学生。
5. 演示数据独立保存在 backend/data，不连接或修改本机 MySQL。停止请在启动窗口按 Ctrl+C。

演示模式首次创建少量演示物品和公告；正式 MySQL 模式不插入演示物品。演示账号仅用于本地查看。

## 二、课程指定环境与 IDEA 开发

- JDK 8；Spring Boot 2.7.14；Maven 3；内嵌 Tomcat 9，可打包 WAR。
- MySQL 8.0 / MySQL Workbench 8.0。
- Node.js 14.21.3；Vue 2.7.16；Vue Router 3；Element UI 2.15.14；Vue CLI 5。
- IDE：IntelliJ IDEA。需要 Vue 编辑支持时安装 Vue 插件。

在 IDEA 中 File → Open，选择本项目根目录（含 pom.xml 的 web 文件夹），按 Maven 工程导入。

1. File → Project Structure → Project SDK 选择 JDK 1.8，语言级别选择 8。
2. Settings → Build Tools → Maven 选择 Maven 3。项目内 .mvn/settings.xml 提供阿里镜像；.mvn/maven.config 将依赖缓存放在项目下的 .maven-repository，避免修改全局设置。IDEA 若未自动读取，可手动指定这两项。
3. 等待 Maven 导入完成；运行 .run 中 Backend-Demo 配置即可启动。若 IDEA 没识别共享配置，直接右键 Application.java → Run，并设置参数 --spring.profiles.active=demo，工作目录设为 backend。
4. 前端开发：在 IDEA Terminal 中 cd frontend，首次 npm ci，然后 npm run serve。访问 http://localhost:8080；/api 代理到后端 8081。
5. 已打包版本访问 8081；修改 Vue 后在 8080 看开发效果，或重新执行 build.cmd 更新 8081 的静态页面。

## 三、切换正式 MySQL 8

1. 在 Workbench 中运行 database/init-mysql.sql。此脚本创建数据库与表，不删除已有数据；运行后确认数据库名为 campus_lost_found。
2. 在 Windows CMD 中切换到本项目根目录，设置真实连接信息（下面为示例，需替换密码）：

   set DB_USERNAME=root
   set DB_PASSWORD=你的 MySQL 密码
   set ADMIN_PASSWORD=你设置的管理员初始密码
   start-mysql.cmd

也可以在 IDEA 的 Backend-MySQL 配置 → Environment variables 填入同样的变量。ADMIN_PASSWORD 至少 8 位，只在首次没有管理员时用于创建 admin，之后更改该变量不会重置已有密码。管理员初始联系手机号为 13900000000。

数据库服务器或端口不同时，设置 DB_URL（完整 JDBC URL，含 serverTimezone=Asia/Shanghai）。默认地址 localhost:3306。不要把数据库密码写进源代码或提交文件。

正式 MySQL 未连接时会报连接错误；演示模式不会自动替代正式模式。MySQL 密码未知时可先使用 start-demo.cmd。自动集成测试使用 H2；2026-10-17 另在本机独立 MySQL 8.0.18 实例（13306端口）验证建表、演示SQL重复导入、JWT登录、认领审核与公告关联，原3306实例未修改。

## 四、完整构建与部署

运行根目录 build.cmd：npm ci → 前端生产构建 → 拷贝静态页面 → Maven 测试和 WAR 打包 → release/campus-lost-found.war。

源码没有更改时，可直接使用已提供的 release 包。build.cmd 需要 Maven/npm 能联网下载依赖。前端完整依赖锁定在 package-lock.json。

独立运行：在 backend 工作目录执行 java -jar ../release/campus-lost-found.war --spring.profiles.active=mysql。启动脚本已包含 UTF-8 和上海时区参数。

外部 Tomcat 9 部署：把 WAR 以 ROOT.war 部署到独立的 Tomcat webapps 根应用，设置上述 MySQL 环境变量；前端 API 使用 /api，需以根应用部署。开发不需要单独安装 Tomcat。Nginx 示例见 docs/05-部署维护.md。

## 五、已实现功能

- 游客浏览大厅、搜索详情和公告；登录后才可查看联系方式。
- 学号或手机号登录；注册检查手机号、学号和手机号重复，BCrypt 保存密码。
- 学生发布/编辑本人失物或招领；实际发生时间、发布/修改/完结时间分别记录。
- 中文关键词（支持空格分隔的多个词）、分类、地点、类型、状态、丢失/拾获时间区间筛选和分页。
- 招领物品提交特征说明；招领人在个人中心“收到的认领申请”确认后转交管理员审核。申请人可查看两步处理结果，待审可撤回。
- 管理员用户启停、物品维护、分类维护、公告维护、审核与统计。
- 审核通过事务内完结物品，驳回其余待审申请，防止同时认领成功。

## 六、公告栏专项修改

公告分为系统通知、招领提醒和认领结果。系统通知说明平台服务和安排；提醒指向具体待认领物品或类别；结果公告说明交接或认领结果，由管理员确认后发布，避免自动公开申请中的私密特征。

notice.publisher_id → app_user.id（发布管理员，必填）
notice.item_id → item.id（关联物品，可选）
notice.category_id → category.id（关联分类，可选）

提醒/结果至少关联物品或分类之一。前端显示发布人，可进入关联物品详情或按关联分类筛选。

新增 published_at（发布时间）、starts_at（生效时间）、expires_at（截止时间，可空）、created_at（创建时间）、updated_at（更新时间）。首页仅显示已启用且已发布、已生效、未过期的公告。后台可查看全部时间字段并编辑定时展示。物品删除时公告保留，item_id 置空；分类被引用时禁止直接删除，可停用。

## 七、目录

- backend/：Java Spring Boot 后端、数据库初始化与 18 项集成测试。
- frontend/：Vue2 + Element UI 前端源码及构建配置。
- database/：MySQL 初始化 SQL。
- release/：可直接执行或部署的 WAR。
- .run/：IDEA 共享运行配置。
- docs/：需求说明、数据库设计、接口设计、测试记录、部署维护、用户手册。

详见 docs 中各文档。为兼容课程指定环境采用其框架版本，定位为课程作业和本地演示。

## 八、讲稿补充后的使用变化

认领流程为提交申请 → 招领人确认 → 管理员审核。管理员登录后直接进入工作台。大厅默认展示进行中信息，可通过状态筛选查看历史完结记录；正常审核完结保留数据，不自动物理删除。

鉴权已改为 JWT，role 使用 user/admin；旧 student 角色自动升级。默认 JWT 密钥每次启动随机生成，重启后需重新登录；如需固定密钥，可设置 JWT_SECRET 为至少 32 随机字节的 Base64 值。HTTPS 部署设置 JWT_COOKIE_SECURE=true。BCrypt 默认 cost=10，可通过 BCRYPT_STRENGTH 调整。旧库确认字段在启动时自动补充，不需清库。

## 九、课程提交材料与 GitHub

先读 `先读我_文件打开方式.md` 和 `提交材料/00_工程文件导览.docx`。提交材料含六份正式Word文档和16页展示PPT。第8周提交程序设计文档（数据库设计作附件）；第12周提交测试报告、部署维护说明和用户手册。

目标仓库：https://github.com/Wenxi-Yang/campus-lost-found 。`.github/workflows` 提供自动构建和手动发布Pages介绍页，`deploy` 提供完整系统的服务器部署模板。上传不等于业务系统已上线；以Actions运行记录及实际业务URL为准。

MySQL新库执行 `database/init-mysql.sql`。`demo-data.sql` 仅供演示/隔离验收库，不能向正式库导入公开演示账号；`verify-database.sql` 为只读关联检查。真实验收结果见 `docs/evidence/mysql-web-result.json`。
