package com.drone.rental.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.dao.entity.KnowledgeDoc;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService extends IService<KnowledgeDoc> {

    /**
     * 为文本生成向量嵌入
     */
    List<Double> generateEmbedding(String text);

    /**
     * 添加文档到知识库
     */
    void addDocument(String filePath, String fileType, String content);

    /**
     * 添加文档到知识库（带行号）
     */
    void addDocument(String filePath, String fileType, String content, Integer lineStart, Integer lineEnd, int chunkIndex);

    /**
     * 根据文件路径删除文档
     */
    void deleteByFilePath(String filePath);

    /**
     * 相似度搜索
     */
    List<KnowledgeDoc> searchBySimilarity(String query, List<String> fileTypes, int limit);

    /**
     * 关键词搜索（备选方案）
     */
    List<KnowledgeDoc> searchByKeyword(String keyword, List<String> fileTypes, int limit);

    /**
     * 索引项目文件
     */
    void indexProjectFiles(String projectPath);

    /**
     * 重建知识库索引
     */
    void rebuildIndex();

    /**
     * 获取知识库统计信息
     */
    KnowledgeBaseStats getStats();

    /**
     * 知识库统计信息
     */
    class KnowledgeBaseStats {
        private int totalDocs;
        private int javaDocs;
        private int vueDocs;
        private int sqlDocs;
        private int mdDocs;
        private int otherDocs;

        public KnowledgeBaseStats(int totalDocs, int javaDocs, int vueDocs, int sqlDocs, int mdDocs, int otherDocs) {
            this.totalDocs = totalDocs;
            this.javaDocs = javaDocs;
            this.vueDocs = vueDocs;
            this.sqlDocs = sqlDocs;
            this.mdDocs = mdDocs;
            this.otherDocs = otherDocs;
        }

        public int getTotalDocs() { return totalDocs; }
        public int getJavaDocs() { return javaDocs; }
        public int getVueDocs() { return vueDocs; }
        public int getSqlDocs() { return sqlDocs; }
        public int getMdDocs() { return mdDocs; }
        public int getOtherDocs() { return otherDocs; }
    }
}
