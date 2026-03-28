package com.drone.rental.web.controller;

import com.drone.rental.dao.entity.KnowledgeDoc;
import com.drone.rental.dao.mapper.KnowledgeDocMapper;
import com.drone.rental.service.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 */
@RestController
@RequestMapping("/admin/api/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeDocMapper knowledgeDocMapper;

    @Value("${knowledge.base.project-path:.}")
    private String projectPath;

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> getStats() {
        KnowledgeBaseService.KnowledgeBaseStats stats = knowledgeBaseService.getStats();

        Map<String, Object> result = new HashMap<>();
        result.put("totalDocs", stats.getTotalDocs());
        result.put("javaDocs", stats.getJavaDocs());
        result.put("vueDocs", stats.getVueDocs());
        result.put("sqlDocs", stats.getSqlDocs());
        result.put("mdDocs", stats.getMdDocs());
        result.put("otherDocs", stats.getOtherDocs());

        return ApiResult.success(result);
    }

    /**
     * 索引项目文件
     */
    @PostMapping("/index")
    public ApiResult<String> indexProject(@RequestParam(required = false) String path) {
        String indexPath = (path != null && !path.isEmpty()) ? path : projectPath;

        new Thread(() -> {
            try {
                knowledgeBaseService.indexProjectFiles(indexPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return ApiResult.success("索引任务已启动，正在后台处理...");
    }

    /**
     * 重建索引
     */
    @PostMapping("/rebuild")
    public ApiResult<String> rebuildIndex() {
        new Thread(() -> {
            try {
                knowledgeBaseService.rebuildIndex();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return ApiResult.success("索引重建任务已启动，正在后台处理...");
    }

    /**
     * 搜索知识库
     */
    @GetMapping("/search")
    public ApiResult<List<KnowledgeDoc>> search(
            @RequestParam String query,
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "5") int limit) {

        List<String> fileTypes = null;
        if (fileType != null && !fileType.isEmpty()) {
            fileTypes = List.of(fileType.split(","));
        }

        List<KnowledgeDoc> results = knowledgeBaseService.searchBySimilarity(query, fileTypes, limit);
        return ApiResult.success(results);
    }

    /**
     * 添加单个文档
     */
    @PostMapping("/add")
    public ApiResult<String> addDocument(@RequestBody Map<String, String> params) {
        String filePath = params.get("filePath");
        String fileType = params.get("fileType");
        String content = params.get("content");

        if (filePath == null || fileType == null || content == null) {
            return ApiResult.fail("参数不完整");
        }

        knowledgeBaseService.addDocument(filePath, fileType, content);
        return ApiResult.success("文档添加成功");
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/delete")
    public ApiResult<String> deleteDocument(@RequestParam String filePath) {
        knowledgeBaseService.deleteByFilePath(filePath);
        return ApiResult.success("文档删除成功");
    }

    /**
     * 获取所有文档列表
     */
    @GetMapping("/list")
    public ApiResult<List<String>> listDocuments() {
        List<String> filePaths = knowledgeDocMapper.getAllFilePaths();
        return ApiResult.success(filePaths);
    }
}
