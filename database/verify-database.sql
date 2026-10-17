-- 先在Workbench选择目标数据库，再执行；只读验证，不删除数据。
SELECT VERSION() mysql_version, DATABASE() current_database;
SELECT 'app_user' table_name,COUNT(*) row_count FROM app_user UNION ALL SELECT 'category',COUNT(*) FROM category UNION ALL SELECT 'item',COUNT(*) FROM item UNION ALL SELECT 'claim',COUNT(*) FROM claim UNION ALL SELECT 'notice',COUNT(*) FROM notice;
SELECT i.id orphan_item FROM item i LEFT JOIN app_user u ON u.id=i.user_id LEFT JOIN category c ON c.id=i.category_id WHERE u.id IS NULL OR c.id IS NULL;
SELECT item_id,COUNT(*) approved_count FROM claim WHERE status='approved' GROUP BY item_id HAVING COUNT(*)>1;
SELECT id invalid_notice_window FROM notice WHERE expires_at IS NOT NULL AND (expires_at<=starts_at OR expires_at<=published_at);
SELECT n.id,n.title,u.name publisher,i.title item_title,c.name category_name,n.published_at,n.starts_at,n.expires_at FROM notice n JOIN app_user u ON n.publisher_id=u.id LEFT JOIN item i ON n.item_id=i.id LEFT JOIN category c ON n.category_id=c.id;
