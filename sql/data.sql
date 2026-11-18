-- ============================================================
-- 个人博客系统 - 初始化数据
-- 注意：管理员账号不在此脚本中创建，由后端启动时自动创建
--       （见 DataInitializer，账号 admin / 123456）
-- ============================================================
USE blog;

-- 分类
INSERT INTO blog_category (name, description, sort, create_time, update_time) VALUES
('前端开发', 'Vue、JavaScript、CSS 等前端技术', 1, NOW(), NOW()),
('后端开发', 'Java、Spring Boot、MyBatis-Plus 等', 2, NOW(), NOW()),
('数据库',   'MySQL、Redis 等数据存储',          3, NOW(), NOW()),
('运维部署', 'Docker、Nginx、Linux',             4, NOW(), NOW()),
('随笔',     '读书、生活与思考',                 5, NOW(), NOW());

-- 标签
INSERT INTO blog_tag (name, create_time, update_time) VALUES
('Java', NOW(), NOW()),
('SpringBoot', NOW(), NOW()),
('Vue', NOW(), NOW()),
('MySQL', NOW(), NOW()),
('Redis', NOW(), NOW()),
('Docker', NOW(), NOW());

-- 站点设置
INSERT INTO blog_site_config (config_key, config_value, description, update_time) VALUES
('site_name',        '我的个人博客',       '站点名称', NOW()),
('site_description', '记录学习与生活的地方', '站点描述', NOW()),
('site_avatar',      '',                  '站长头像地址', NOW()),
('github',           '',                  'GitHub 主页', NOW()),
('email',            '',                  '联系邮箱', NOW());
