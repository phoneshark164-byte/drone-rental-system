package com.drone.rental.service.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.KnowledgeDoc;
import com.drone.rental.dao.mapper.KnowledgeDocMapper;
import com.drone.rental.service.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 知识库服务实现
 */
@Service
@SuppressWarnings("unchecked")
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeDocMapper, KnowledgeDoc>
        implements KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.embedding.url:}")
    private String embeddingUrl;

    @Value("${ai.embedding.dimension:768}")
    private int embeddingDimension;

    @Value("${knowledge.base.project-path:.}")
    private String projectPath;

    private final RestTemplate restTemplate = new RestTemplate();

    // Redis缓存前缀
    private static final String CACHE_PREFIX = "kb:embedding:";
    private static final String CACHE_PREFIX_SEARCH = "kb:search:";
    private static final long CACHE_TTL_HOURS = 24;

    @Override
    public List<Double> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 检查Redis缓存
        String cacheKey = getCacheKey(text);
        if (redisTemplate != null) {
            try {
                if (redisTemplate.hasKey(cacheKey)) {
                    String cached = (String) redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null) {
                        return JSON.parseArray(cached, Double.class);
                    }
                }
            } catch (Exception e) {
                // Redis不可用，忽略缓存
                log.debug("Redis缓存不可用，将重新生成embedding: {}", e.getMessage());
            }
        }

        List<Double> embedding;

        // 如果配置了embedding API，调用API
        if (embeddingUrl != null && !embeddingUrl.isEmpty()) {
            embedding = callEmbeddingApi(text);
        } else {
            // 使用简单的哈希伪嵌入（仅用于演示，生产环境应使用真实embedding）
            embedding = generatePseudoEmbedding(text);
        }

        // 缓存结果（如果Redis可用）
        if (redisTemplate != null && !embedding.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(embedding),
                        CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                // Redis缓存失败，不影响主流程
                log.debug("Redis缓存写入失败: {}", e.getMessage());
            }
        }

        return embedding;
    }

    /**
     * 调用Embedding API
     */
    private List<Double> callEmbeddingApi(String text) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("input", text);
            body.put("model", "text-embedding-ada-002");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + aiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    embeddingUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                if (data != null && !data.isEmpty()) {
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    return embedding;
                }
            }
        } catch (Exception e) {
            log.warn("调用Embedding API失败，使用伪嵌入: {}", e.getMessage());
        }
        return generatePseudoEmbedding(text);
    }

    /**
     * 生成伪嵌入向量（基于哈希，仅用于演示）
     */
    private List<Double> generatePseudoEmbedding(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));

            List<Double> embedding = new ArrayList<>();
            for (int i = 0; i < embeddingDimension; i++) {
                // 将哈希值转换为浮点数
                int byteIndex = i % hash.length;
                int value = hash[byteIndex] & 0xFF;
                // 归一化到 [-1, 1]
                embedding.add((value - 128) / 128.0);
            }
            return embedding;
        } catch (Exception e) {
            log.error("生成伪嵌入失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void addDocument(String filePath, String fileType, String content) {
        addDocument(filePath, fileType, content, null, null, 0);
    }

    @Override
    public void addDocument(String filePath, String fileType, String content,
                           Integer lineStart, Integer lineEnd, int chunkIndex) {
        try {
            // 生成嵌入
            List<Double> embedding = generateEmbedding(content);

            if (embedding.isEmpty()) {
                log.warn("文档 {} 嵌入生成失败，跳过", filePath);
                return;
            }

            // 检查是否已存在
            KnowledgeDoc existing = baseMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeDoc>()
                            .eq(KnowledgeDoc::getFilePath, filePath)
                            .eq(KnowledgeDoc::getChunkIndex, chunkIndex)
            );

            KnowledgeDoc doc = new KnowledgeDoc();
            doc.setFilePath(filePath);
            doc.setFileType(fileType);
            doc.setContent(content);
            doc.setEmbedding(JSON.toJSONString(embedding));
            doc.setChunkIndex(chunkIndex);
            doc.setLineStart(lineStart);
            doc.setLineEnd(lineEnd);

            if (existing != null) {
                doc.setId(existing.getId());
                baseMapper.updateById(doc);
            } else {
                baseMapper.insert(doc);
            }

            log.debug("添加文档成功: {} (chunk: {})", filePath, chunkIndex);

        } catch (Exception e) {
            log.error("添加文档失败: {}", filePath, e);
        }
    }

    @Override
    public void deleteByFilePath(String filePath) {
        baseMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeDoc>()
                        .eq(KnowledgeDoc::getFilePath, filePath)
        );

        // 清除相关缓存
        if (redisTemplate != null) {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null) {
                redisTemplate.delete(keys);
            }
        }
    }

    @Override
    public List<KnowledgeDoc> searchBySimilarity(String query, List<String> fileTypes, int limit) {
        // 生成查询向量
        List<Double> queryEmbedding = generateEmbedding(query);

        if (queryEmbedding.isEmpty()) {
            return Collections.emptyList();
        }

        // 检查搜索缓存
        String cacheKey = CACHE_PREFIX_SEARCH + query.hashCode();
        if (redisTemplate != null && redisTemplate.hasKey(cacheKey)) {
            List<Long> cachedIds = (List<Long>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedIds != null && !cachedIds.isEmpty()) {
                return baseMapper.selectBatchIds(cachedIds);
            }
        }

        // 执行相似度搜索
        List<KnowledgeDoc> results = baseMapper.searchBySimilarity(
                JSON.toJSONString(queryEmbedding),
                fileTypes,
                limit
        );

        // 缓存结果
        if (redisTemplate != null && !results.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (KnowledgeDoc doc : results) {
                ids.add(doc.getId());
            }
            redisTemplate.opsForValue().set(cacheKey, ids, 1, TimeUnit.HOURS);
        }

        return results;
    }

    @Override
    public List<KnowledgeDoc> searchByKeyword(String keyword, List<String> fileTypes, int limit) {
        return baseMapper.searchByKeyword(keyword, fileTypes, limit);
    }

    @Override
    public void indexProjectFiles(String projectPath) {
        log.info("开始索引项目文件: {}", projectPath);

        Path rootPath = Paths.get(projectPath).toAbsolutePath().normalize();
        log.info("解析后的绝对路径: {}", rootPath);

        if (!Files.exists(rootPath)) {
            log.error("项目路径不存在: {}", rootPath);
            return;
        }

        int indexedCount = 0;

        try {
            // 遍历Java源文件
            indexJavaFiles(rootPath);
            // 遍历Vue文件
            indexVueFiles(rootPath);
            // 遍历SQL文件
            indexSqlFiles(rootPath);
            // 遍历Markdown文件
            indexMarkdownFiles(rootPath);

            indexedCount = baseMapper.countDocs();
            log.info("索引完成，共索引 {} 个文档", indexedCount);

        } catch (Exception e) {
            log.error("索引项目文件失败", e);
        }
    }

    /**
     * 索引Java文件
     */
    private void indexJavaFiles(Path rootPath) throws Exception {
        // rootPath 已经是 drone-backend 目录，直接扫描其中的所有子模块
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p, StandardCharsets.UTF_8);
                            String relativePath = rootPath.relativize(p).toString();

                            // 如果文件太大，分块处理
                            if (content.length() > 5000) {
                                indexLargeFile(p, relativePath, "java", content);
                            } else {
                                addDocument(relativePath, "java", content);
                            }
                        } catch (Exception e) {
                            log.error("索引Java文件失败: {}", p, e);
                        }
                    });
        }
    }

    /**
     * 索引Vue文件
     */
    private void indexVueFiles(Path rootPath) throws Exception {
        // Vue项目在 drone-backend 的同级目录
        Path vuePath = rootPath.getParent().resolve("drone-web");
        if (!Files.exists(vuePath)) {
            return;
        }

        // 使用final变量以便在lambda中引用
        final Path finalVuePath = vuePath;

        try (Stream<Path> paths = Files.walk(finalVuePath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".vue"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p, StandardCharsets.UTF_8);
                            String relativePath = "drone-web/" + finalVuePath.relativize(p).toString();

                            if (content.length() > 5000) {
                                indexLargeFile(p, relativePath, "vue", content);
                            } else {
                                addDocument(relativePath, "vue", content);
                            }
                        } catch (Exception e) {
                            log.error("索引Vue文件失败: {}", p, e);
                        }
                    });

        }
    }

    /**
     * 索引SQL文件
     */
    private void indexSqlFiles(Path rootPath) throws Exception {
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p, StandardCharsets.UTF_8);
                            String relativePath = rootPath.relativize(p).toString();
                            addDocument(relativePath, "sql", content);
                        } catch (Exception e) {
                            log.error("索引SQL文件失败: {}", p, e);
                        }
                    });
        }
    }

    /**
     * 索引Markdown文件
     */
    private void indexMarkdownFiles(Path rootPath) throws Exception {
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p, StandardCharsets.UTF_8);
                            String relativePath = rootPath.relativize(p).toString();
                            addDocument(relativePath, "md", content);
                        } catch (Exception e) {
                            log.error("索引Markdown文件失败: {}", p, e);
                        }
                    });
        }
    }

    /**
     * 索引大文件（分块处理）
     */
    private void indexLargeFile(Path filePath, String relativePath, String fileType, String content) {
        try {
            String[] lines = content.split("\n");
            int chunkSize = 200; // 每块200行
            int chunkIndex = 0;

            for (int i = 0; i < lines.length; i += chunkSize) {
                int end = Math.min(i + chunkSize, lines.length);
                StringBuilder chunk = new StringBuilder();

                for (int j = i; j < end; j++) {
                    chunk.append(lines[j]).append("\n");
                }

                addDocument(relativePath, fileType, chunk.toString(), i + 1, end, chunkIndex);
                chunkIndex++;
            }
        } catch (Exception e) {
            log.error("索引大文件失败: {}", filePath, e);
        }
    }

    @Override
    public void rebuildIndex() {
        log.info("开始重建知识库索引");

        // 清空现有索引
        baseMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeDoc>()
        );

        // 清除缓存
        if (redisTemplate != null) {
            Set<String> keys = redisTemplate.keys("kb:*");
            if (keys != null) {
                redisTemplate.delete(keys);
            }
        }

        // 重新索引
        indexProjectFiles(projectPath);

        log.info("知识库索引重建完成");
    }

    @Override
    public KnowledgeBaseStats getStats() {
        List<KnowledgeDoc> allDocs = baseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>()
        );

        int java = 0, vue = 0, sql = 0, md = 0, other = 0;

        for (KnowledgeDoc doc : allDocs) {
            String type = doc.getFileType();
            switch (type) {
                case "java": java++; break;
                case "vue": vue++; break;
                case "sql": sql++; break;
                case "md": md++; break;
                default: other++; break;
            }
        }

        return new KnowledgeBaseStats(allDocs.size(), java, vue, sql, md, other);
    }

    /**
     * 生成缓存键
     */
    private String getCacheKey(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return CACHE_PREFIX + sb.toString();
        } catch (Exception e) {
            return CACHE_PREFIX + text.hashCode();
        }
    }
}
