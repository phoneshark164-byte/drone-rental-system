-- 知识库文档表
CREATE TABLE IF NOT EXISTS `knowledge_doc` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_type` VARCHAR(50) NOT NULL COMMENT '文件类型：java/vue/sql/md/txt等',
    `content` MEDIUMTEXT NOT NULL COMMENT '文档内容',
    `embedding` JSON NOT NULL COMMENT '向量嵌入数据',
    `chunk_index` INT DEFAULT 0 COMMENT '分块索引（大文档会分块）',
    `line_start` INT DEFAULT NULL COMMENT '代码起始行号',
    `line_end` INT DEFAULT NULL COMMENT '代码结束行号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_chunk` (`file_path`, `chunk_index`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- 知识库查询缓存表（可选，也可以直接用Redis）
CREATE TABLE IF NOT EXISTS `knowledge_query_cache` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `query_text` VARCHAR(1000) NOT NULL COMMENT '查询文本',
    `query_embedding` JSON NOT NULL COMMENT '查询向量',
    `result_doc_ids` TEXT NOT NULL COMMENT '结果文档ID列表（逗号分隔）',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库查询缓存表';
