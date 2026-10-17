-- 已有第一版MySQL数据库的升级说明；应用启动时会自动按列检查补充，通常无需手动执行。
-- 手动执行前请备份，并仅在对应列不存在时执行ADD COLUMN，避免重复添加。
USE campus_lost_found;
ALTER TABLE claim ADD COLUMN publisher_status VARCHAR(16) NOT NULL DEFAULT 'pending';
ALTER TABLE claim ADD COLUMN publisher_reply VARCHAR(500);
ALTER TABLE claim ADD COLUMN publisher_confirmed_at TIMESTAMP NULL;
-- 旧版已处理申请没有招领人确认记录，明确标记历史来源，不伪造确认时间。
UPDATE claim SET publisher_status='legacy' WHERE status<>'pending';
UPDATE app_user SET role='user' WHERE role='student';
ALTER TABLE app_user ALTER COLUMN role SET DEFAULT 'user';
