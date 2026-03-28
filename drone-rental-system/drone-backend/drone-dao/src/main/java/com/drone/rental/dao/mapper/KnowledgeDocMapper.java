package com.drone.rental.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.dao.entity.KnowledgeDoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库文档Mapper
 */
@Mapper
public interface KnowledgeDocMapper extends BaseMapper<KnowledgeDoc> {

    /**
     * 余弦相似度搜索（使用SQL函数计算）
     * 注意：MySQL 8.0+ 支持JSON函数
     */
    @Select("<script>" +
            "SELECT id, file_path, file_type, content, chunk_index, line_start, line_end, " +
            "embedding, " +
            // 计算余弦相似度
            "(1 - (JSON_LENGTH(embedding) - " +
            "SUM(CAST(JSON_EXTRACT(embedding, CONCAT('$[', seq.seq, ']')) AS UNSIGNED) * " +
            "CAST(JSON_EXTRACT(#{queryEmbedding}, CONCAT('$[', seq.seq, ']')) AS UNSIGNED)) " +
            "/ (" +
            "SQRT(SUM(POW(CAST(JSON_EXTRACT(embedding, CONCAT('$[', seq.seq, ']')) AS UNSIGNED), 2))) * " +
            "SQRT(SUM(POW(CAST(JSON_EXTRACT(#{queryEmbedding}, CONCAT('$[', seq.seq, ']')) AS UNSIGNED), 2)))" +
            "))) AS similarity " +
            "FROM knowledge_doc, (" +
            "  SELECT 0 AS seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 " +
            "  UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 " +
            "  UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 " +
            ") seq " +
            "WHERE seq.seq &lt; JSON_LENGTH(embedding) " +
            "AND seq.seq &lt; JSON_LENGTH(#{queryEmbedding}) " +
            "<if test='fileTypes != null and fileTypes.size() > 0'>" +
            "AND file_type IN " +
            "<foreach item='type' collection='fileTypes' open='(' separator=',' close=')'>" +
            "#{type}" +
            "</foreach>" +
            "</if>" +
            "GROUP BY id " +
            "ORDER BY similarity DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<KnowledgeDoc> searchBySimilarity(@Param("queryEmbedding") String queryEmbedding,
                                          @Param("fileTypes") List<String> fileTypes,
                                          @Param("limit") int limit);

    /**
     * 简单的关键词搜索（备选方案，如果向量搜索太慢）
     */
    @Select("<script>" +
            "SELECT * FROM knowledge_doc " +
            "WHERE content LIKE CONCAT('%', #{keyword}, '%')" +
            "<if test='fileTypes != null and fileTypes.size() > 0'>" +
            "AND file_type IN " +
            "<foreach item='type' collection='fileTypes' open='(' separator=',' close=')'>" +
            "#{type}" +
            "</foreach>" +
            "</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<KnowledgeDoc> searchByKeyword(@Param("keyword") String keyword,
                                        @Param("fileTypes") List<String> fileTypes,
                                        @Param("limit") int limit);

    /**
     * 获取所有文件路径
     */
    @Select("SELECT DISTINCT file_path FROM knowledge_doc")
    List<String> getAllFilePaths();

    /**
     * 根据文件路径删除文档
     */
    @Select("DELETE FROM knowledge_doc WHERE file_path = #{filePath}")
    int deleteByFilePath(@Param("filePath") String filePath);

    /**
     * 统计文档数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_doc")
    int countDocs();
}
