-- rideSketch 数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ridesketch DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ridesketch;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `email` VARCHAR(100) COMMENT '邮箱',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 路线表
CREATE TABLE IF NOT EXISTS `route` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '路线ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '路线标题',
    `description` TEXT COMMENT '路线描述',
    `start_point` VARCHAR(200) COMMENT '起点',
    `end_point` VARCHAR(200) COMMENT '终点',
    `waypoints` TEXT COMMENT '途经点(JSON数组)',
    `route_path` TEXT COMMENT '路线轨迹(JSON)',
    `total_distance` DECIMAL(10,2) COMMENT '总距离(公里)',
    `estimated_time` INT COMMENT '预计时间(分钟)',
    `difficulty` TINYINT COMMENT '难度: 1-简单, 2-中等, 3-困难',
    `tags` VARCHAR(500) COMMENT '标签(JSON数组)',
    `likes` INT DEFAULT 0 COMMENT '点赞数',
    `views` INT DEFAULT 0 COMMENT '浏览数',
    `is_public` TINYINT DEFAULT 1 COMMENT '是否公开: 0-私有, 1-公开',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线表';

-- 点赞表
CREATE TABLE IF NOT EXISTS `like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `route_id` BIGINT NOT NULL COMMENT '路线ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_route` (`user_id`, `route_id`),
    KEY `idx_route_id` (`route_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `route_id` BIGINT NOT NULL COMMENT '路线ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_route_id` (`route_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- AI对话记录表
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT COMMENT '用户ID',
    `session_id` VARCHAR(100) COMMENT '会话ID',
    `user_message` TEXT NOT NULL COMMENT '用户消息',
    `ai_response` TEXT COMMENT 'AI响应',
    `route_id` BIGINT COMMENT '关联路线ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';
