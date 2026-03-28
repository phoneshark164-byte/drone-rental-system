package com.drone.rental.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档实体
 */
@Data
@TableName("knowledge_doc")
public class KnowledgeDoc implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件类型：java/vue/sql/md/txt等
     */
    private String fileType;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 向量嵌入数据（JSON格式）
     */
    private String embedding;

    /**
     * 分块索引（大文档会分块）
     */
    private Integer chunkIndex;

    /**
     * 代码起始行号
     */
    private Integer lineStart;

    /**
     * 代码结束行号
     */
    private Integer lineEnd;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
