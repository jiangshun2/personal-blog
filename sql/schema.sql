-- ============================================================
-- 个人博客系统 - 数据库建表脚本
-- 使用前：先执行本文件，再执行 data.sql
-- 约定：
--   1. 字符集统一 utf8mb4（支持 emoji 和中文）
--   2. 表名统一加 blog_ 前缀，避免与 MySQL 关键字冲突（如 comment）
--   3. 业务表统一含 deleted 逻辑删除字段（0=正常，1=已删除）
-- ============================================================

CREATE DATABASE IF NOT EXISTS blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE blog;

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS blog_user;
CREATE TABLE blog_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
  password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密后存储）',
  nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 文章分类表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS blog_category;
CREATE TABLE blog_category (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name        VARCHAR(50)  NOT NULL COMMENT '分类名称',
  description VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类表';

-- ------------------------------------------------------------
-- 文章标签表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS blog_tag;
CREATE TABLE blog_tag (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  name        VARCHAR(50) NOT NULL COMMENT '标签名称',
  create_time DATETIME    DEFAULT NULL COMMENT '创建时间',
  update_time DATETIME    DEFAULT NULL COMMENT '更新时间',
  deleted     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签表';

-- ------------------------------------------------------------
-- 文章表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS blog_article;
CREATE TABLE blog_article (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title       VARCHAR(200) NOT NULL COMMENT '标题',
  summary     VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  content     LONGTEXT     COMMENT '正文（Markdown 格式）',
  cover       VARCHAR(255) DEFAULT NULL COMMENT '封面图地址',
  author_id   BIGINT       NOT NULL COMMENT '作者ID',
  category_id BIGINT       DEFAULT NULL COMMENT '分类ID',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=草稿，1=已发布',
  view_count  BIGINT       NOT NULL DEFAULT 0 COMMENT '浏览量',
  is_top      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶：0=否，1=是',
  create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
  deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_author (author_id),
  KEY idx_category (category_id),
  KEY idx_status_top (status, is_top)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- ------------------------------------------------------------
-- 文章-标签关联表（多对多）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS blog_article_tag;
CREATE TABLE blog_article_tag (
  id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  article_id BIGINT NOT NULL COMMENT '文章ID',
  tag_id     BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_article_tag (article_id, tag_id),
  KEY idx_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章-标签关联表';

-- ------------------------------------------------------------
-- 评论表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS blog_comment;
CREATE TABLE blog_comment (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  article_id  BIGINT        NOT NULL COMMENT '文章ID',
  user_id     BIGINT        DEFAULT NULL COMMENT '评论用户ID（游客可为空）',
  parent_id   BIGINT        DEFAULT NULL COMMENT '父评论ID（NULL 表示一级评论）',
  content     VARCHAR(1000) NOT NULL COMMENT '评论内容',
  nickname    VARCHAR(50)   DEFAULT NULL COMMENT '游客昵称',
  email       VARCHAR(100)  DEFAULT NULL COMMENT '游客邮箱',
  status      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0=隐藏，1=显示',
  create_time DATETIME      DEFAULT NULL COMMENT '创建时间',
  deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ------------------------------------------------------------
-- 站点设置表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS blog_site_config;
CREATE TABLE blog_site_config (
  id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  config_key   VARCHAR(50)   NOT NULL COMMENT '配置键',
  config_value VARCHAR(1000) DEFAULT NULL COMMENT '配置值',
  description  VARCHAR(255)  DEFAULT NULL COMMENT '配置说明',
  update_time  DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点设置表';
