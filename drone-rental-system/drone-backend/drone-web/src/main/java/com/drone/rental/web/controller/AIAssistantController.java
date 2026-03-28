package com.drone.rental.web.controller;

import com.drone.rental.dao.entity.KnowledgeDoc;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.service.service.DroneDataService;
import com.drone.rental.service.service.KnowledgeBaseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI多模态助手控制器 - Anthropic Claude + RAG知识库
 */
@RestController
public class AIAssistantController {

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.api.url:http://47.108.60.237:3300/api/v1/messages}")
    private String aiApiUrl;

    @Value("${ai.model:claude-3-5-sonnet-20241022}")
    private String aiModel;

    @Value("${ai.knowledge.enabled:true}")
    private boolean knowledgeEnabled;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired(required = false)
    private DroneDataService droneDataService;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * AI对话接口
     */
    @PostMapping("/user/api/assistant/chat")
    public ApiResult<Map<String, Object>> chat(
            @RequestBody Map<String, String> params,
            HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.fail("请先登录");
        }

        String message = params.get("message");
        String imageBase64 = params.get("image");

        try {
            String reply;

            // 优先检查是否是数据查询问题（价格、位置、可用性、用户信息等）
            if (droneDataService != null) {
                reply = droneDataService.answerUserDataQuestion(message, user.getId());

                // 如果数据服务没有返回有效答案，继续使用AI
                if (reply.contains("暂时无法回答") || reply.contains("查询数据时出现错误")) {
                    reply = callClaudeAI(message, imageBase64);
                }
            } else {
                // 使用AI回答（带知识库增强）
                reply = callClaudeAI(message, imageBase64);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("reply", reply);
            result.put("timestamp", System.currentTimeMillis());
            return ApiResult.success(result);

        } catch (Exception e) {
            e.printStackTrace();
            // 如果AI调用失败，返回智能回复
            String fallbackReply = getFallbackReply(message);
            Map<String, Object> result = new HashMap<>();
            result.put("reply", fallbackReply);
            result.put("timestamp", System.currentTimeMillis());
            return ApiResult.success(result);
        }
    }

    /**
     * 判断是否是数据查询问题
     */
    private boolean isDataQueryQuestion(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        String lower = message.toLowerCase();

        // 价格相关
        if (lower.contains("多少钱") || lower.contains("价格") || lower.contains("费用")) {
            return true;
        }

        // 位置相关
        if (lower.contains("在哪") || lower.contains("位置") || lower.contains("在哪里")) {
            return true;
        }

        // 可用性相关
        if (lower.contains("可租") || lower.contains("可用") || lower.contains("有哪些无人机")) {
            return true;
        }

        // 状态相关
        if (lower.contains("状态") && (lower.contains("多少") || lower.contains("几台"))) {
            return true;
        }

        // 型号相关
        if (lower.contains("型号") && (lower.contains("有哪些") || lower.contains("什么型号"))) {
            return true;
        }

        return false;
    }

    /**
     * 调用Claude AI模型（带RAG知识库增强）
     */
    private String callClaudeAI(String message, String imageBase64) {
        if (aiApiKey == null || aiApiKey.isEmpty()) {
            throw new RuntimeException("AI API密钥未配置");
        }

        // 获取知识库上下文（RAG）
        String knowledgeContext = retrieveKnowledgeContext(message);

        // 构建请求体
        Map<String, Object> requestBody = buildClaudeRequest(message, imageBase64, knowledgeContext);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", aiApiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 发送请求
        ResponseEntity<Map> response = restTemplate.exchange(
                aiApiUrl,
                HttpMethod.POST,
                entity,
                Map.class
        );

        // 解析响应
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return extractClaudeContent(response.getBody());
        }

        throw new RuntimeException("AI请求失败");
    }

    /**
     * 从知识库检索相关上下文
     */
    private String retrieveKnowledgeContext(String query) {
        if (!knowledgeEnabled || knowledgeBaseService == null) {
            return "";
        }

        try {
            List<KnowledgeDoc> docs = new ArrayList<>();

            // 扩展搜索关键词
            List<String> searchKeywords = expandKeywords(query);

            // 判断是否是"有哪些"这类列举问题
            boolean isListQuestion = query.contains("有哪些") || query.contains("列表") ||
                                    query.contains("所有") || query.contains("Controller") ||
                                    query.contains("Service");

            if (isListQuestion) {
                // 对于列举问题，收集所有关键词的结果，去重
                Set<String> seenPaths = new HashSet<>();
                int limitPerKeyword = 5; // 每个关键词最多5个结果

                for (String keyword : searchKeywords) {
                    List<KnowledgeDoc> keywordDocs = knowledgeBaseService.searchByKeyword(keyword, null, limitPerKeyword);
                    if (keywordDocs != null) {
                        for (KnowledgeDoc doc : keywordDocs) {
                            // 按文件路径去重
                            String path = doc.getFilePath();
                            if (!seenPaths.contains(path)) {
                                seenPaths.add(path);
                                docs.add(doc);
                            }
                        }
                    }
                    // 限制总结果数量
                    if (docs.size() >= 20) {
                        break;
                    }
                }
            } else {
                // 对于具体问题，只使用第一个匹配的关键词
                for (String keyword : searchKeywords) {
                    docs = knowledgeBaseService.searchByKeyword(keyword, null, 5);
                    if (docs != null && !docs.isEmpty()) {
                        break;
                    }
                }
            }

            if (docs.isEmpty()) {
                return "";
            }

            StringBuilder context = new StringBuilder();
            context.append("\n\n=== 项目知识库相关信息 ===\n");

            for (KnowledgeDoc doc : docs) {
                context.append("\n【文件: ").append(doc.getFilePath()).append("】");
                if (doc.getLineStart() != null) {
                    context.append(" (第").append(doc.getLineStart()).append("-").append(doc.getLineEnd()).append("行)");
                }
                // 限制内容长度
                String content = doc.getContent();
                if (content.length() > 3000) {
                    content = content.substring(0, 3000) + "...";
                }
                context.append("\n").append(content).append("\n");
            }

            context.append("=== 知识库信息结束 ===\n");
            context.append("\n请根据以上项目代码和文档内容回答用户问题。分析代码时，请提取类名、方法名、属性等具体信息。\n");

            return context.toString();

        } catch (Exception e) {
            // 知识库检索失败不影响主流程
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 扩展搜索关键词
     */
    private List<String> expandKeywords(String query) {
        List<String> keywords = new ArrayList<>();
        keywords.add(query);

        // 添加相关的英文关键词
        if (query.contains("数据库") || query.contains("表")) {
            keywords.add("CREATE TABLE");
            keywords.add("drone_vehicle");
            keywords.add("sys_user");
        }
        if (query.contains("用户") && query.contains("登录")) {
            keywords.add("login");
            keywords.add("SysUserService");
            keywords.add("username");
        }
        if (query.contains("价格") || query.contains("多少钱")) {
            keywords.add("price");
            keywords.add("daily_rate");
            keywords.add("hourly_rate");
        }
        if (query.contains("无人机")) {
            keywords.add("DroneVehicle");
            keywords.add("drone");
        }
        if (query.contains("订单")) {
            keywords.add("DroneOrder");
            keywords.add("rental");
        }

        // 支付相关
        if (query.contains("支付") || query.contains("付款") || query.contains("结算")) {
            keywords.add("Payment");
            keywords.add("UserPaymentRecord");
            keywords.add("payOrderByBalance");
            keywords.add("paymentSuccess");
            keywords.add("paymentFail");
            keywords.add("createPayment");
        }

        // 充值相关
        if (query.contains("充值") || query.contains("余额")) {
            keywords.add("Recharge");
            keywords.add("UserRechargeRecord");
            keywords.add("createRecharge");
            keywords.add("rechargeSuccess");
            keywords.add("balance");
        }

        // 流程相关 - 添加具体的类名和方法
        if (query.contains("流程") || query.contains("如何") || query.contains("怎么")) {
            // 根据问题中的其他关键词确定搜索内容
            if (query.contains("支付") || query.contains("付款")) {
                keywords.add("UserPaymentRecordService");
                keywords.add("payment");
            }
            if (query.contains("订单")) {
                keywords.add("DroneOrderService");
                keywords.add("createOrder");
            }
            if (query.contains("登录")) {
                keywords.add("SysUserService");
                keywords.add("login");
            }
        }

        // Controller相关
        if (query.contains("Controller") || query.contains("控制器")) {
            keywords.add("@RestController");
            keywords.add("@Controller");
            keywords.add("public class");
        }

        // Service相关
        if (query.contains("Service") || query.contains("服务")) {
            keywords.add("interface");
            keywords.add("extends IService");
            keywords.add("public interface");
        }

        // 实体类相关
        if (query.contains("实体") || query.contains("Entity") || query.contains("类") && query.contains("属性")) {
            keywords.add("@TableName");
            keywords.add("private");
            keywords.add("public class");
        }

        // 通用"有哪些"类问题 - 添加多个可能的关键词
        if (query.contains("有哪些") || query.contains("列表") || query.contains("所有")) {
            // 按顺序尝试搜索不同的代码模式
            keywords.add("public class");
            keywords.add("public interface");
            keywords.add("@RestController");
            keywords.add("@Service");
            keywords.add("@TableName");
        }

        return keywords;
    }

    /**
     * 构建Claude AI请求
     */
    private Map<String, Object> buildClaudeRequest(String message, String imageBase64, String knowledgeContext) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiModel);
        body.put("max_tokens", 2048);

        // 构建用户消息内容
        List<Map<String, Object>> content = new ArrayList<>();

        // 如果有图片，添加图片
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image");

            Map<String, String> imageSource = new HashMap<>();
            imageSource.put("type", "base64");

            // 检测图片类型
            String mediaType = "image/jpeg";
            if (imageBase64.contains(",")) {
                String dataPrefix = imageBase64.split(",")[0].toLowerCase();
                if (dataPrefix.contains("png")) {
                    mediaType = "image/png";
                } else if (dataPrefix.contains("gif")) {
                    mediaType = "image/gif";
                } else if (dataPrefix.contains("webp")) {
                    mediaType = "image/webp";
                }
                // 提取纯base64数据
                imageBase64 = imageBase64.split(",")[1];
            }

            imageSource.put("media_type", mediaType);
            imageSource.put("data", imageBase64);

            imageContent.put("source", imageSource);
            content.add(imageContent);
        }

        // 添加文本（包含知识库上下文）
        String textMessage = (message != null && !message.isEmpty()) ? message : "请帮我分析这张图片。";
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");

        String fullPrompt = buildSystemPrompt();
        if (!knowledgeContext.isEmpty()) {
            fullPrompt += knowledgeContext;
            fullPrompt += "\n\n请根据以上项目知识库信息回答用户的问题。如果知识库中有相关代码或配置，请引用具体的文件路径和行号。";
        }
        fullPrompt += "\n\n用户问题：" + textMessage;

        textContent.put("text", fullPrompt);
        content.add(textContent);

        // 构建消息列表
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", content);
        messages.add(userMessage);

        body.put("messages", messages);

        return body;
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt() {
        return "你是一个无人机租赁系统的AI助手。你的职责包括：\n" +
                "1. 识别和分析无人机图片中的损伤情况\n" +
                "2. 回答关于无人机型号、参数、使用场景的问题\n" +
                "3. 提供无人机选择建议和使用指导\n" +
                "4. 解答租赁流程、押金、支付等问题\n" +
                "5. 回答各种技术问题，包括生成代码、数据结构等\n" +
                "6. 解答关于本项目代码、架构、实现细节的问题\n\n" +
                "【重要：回答代码相关问题时】\n" +
                "- 当用户询问Controller、Service、Entity等类时，请仔细分析提供的Java代码\n" +
                "- 从代码中提取类名、包路径、主要方法和属性\n" +
                "- 列举时使用格式：类名 (完整包路径)\n" +
                "- 如果是实体类，列出所有属性及其类型\n" +
                "- 如果是Controller，列出主要的API接口\n" +
                "- 如果是Service，列出主要方法\n" +
                "- 不要编造代码中不存在的内容\n\n" +
                "请用简洁、友好的语气回复，不要使用表情符号。";
    }

    /**
     * 从Claude AI响应中提取内容
     */
    @SuppressWarnings("unchecked")
    private String extractClaudeContent(Map<String, Object> response) {
        try {
            List<Map<String, Object>> content =
                    (List<Map<String, Object>>) response.get("content");

            if (content != null && !content.isEmpty()) {
                StringBuilder text = new StringBuilder();
                for (Map<String, Object> item : content) {
                    if ("text".equals(item.get("type"))) {
                        text.append(item.get("text"));
                    }
                }
                return text.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "抱歉，我无法理解回复内容。";
    }

    /**
     * 获取备用回复（当AI不可用时）
     */
    private String getFallbackReply(String message) {
        if (message == null || message.isEmpty()) {
            return "你好！我是AI助手，请问有什么可以帮助你的吗？";
        }

        String lowerMessage = message.toLowerCase();

        // 简单的关键词匹配回复
        if (lowerMessage.contains("如何选择") || lowerMessage.contains("推荐")) {
            return "**无人机选择建议**\n\n" +
                    "根据不同使用场景推荐：\n" +
                    "• 航拍摄影：推荐大疆 Mavic 系列，画质优秀，便携易携\n" +
                    "• 农业植保：推荐专业植保无人机，载重大，续航久\n" +
                    "• 巡检勘探：推荐工业级无人机，稳定可靠\n" +
                    "• 新手入门：推荐 mini 系列，操作简单，价格亲民\n\n" +
                    "你可以告诉我具体用途，我会给你更详细的建议！";
        }

        if (lowerMessage.contains("损坏") || lowerMessage.contains("故障") || lowerMessage.contains("报修")) {
            return "**故障处理指南**\n\n" +
                    "如果你的无人机出现故障：\n" +
                    "1. 先拍照记录损坏情况\n" +
                    "2. 在「我的报修」页面提交报修申请\n" +
                    "3. 我们会尽快安排维修人员处理\n" +
                    "4. 你也可以上传图片让我帮你分析损坏程度\n\n" +
                    "需要我帮你分析图片吗？";
        }

        if (lowerMessage.contains("押金") || lowerMessage.contains("退还")) {
            return "**押金说明**\n\n" +
                    "• 押金金额：¥100\n" +
                    "• 支付方式：从账户余额扣除\n" +
                    "• 退还条件：订单完成后自动退还\n" +
                    "• 退还方式：退回到账户余额\n\n" +
                    "温馨提示：押金会在你结束使用无人机并确认无误后自动退还！";
        }

        if (lowerMessage.contains("价格") || lowerMessage.contains("费用") || lowerMessage.contains("多少钱")) {
            return "**租赁价格**\n\n" +
                    "不同型号的无人机价格不同：\n" +
                    "• 入门级：¥30-50/小时\n" +
                    "• 进阶级：¥50-100/小时\n" +
                    "• 专业级：¥100-200/小时\n\n" +
                    "在无人机列表页面可以看到具体价格，新用户还有优惠哦！";
        }

        if (lowerMessage.contains("你好") || lowerMessage.contains("hi") || lowerMessage.contains("hello")) {
            return "你好！很高兴见到你！\n\n" +
                    "我是你的AI助手，可以帮你：\n" +
                    "• 识别和分析无人机图片\n" +
                    "• 推荐合适的无人机\n" +
                    "• 解答使用问题\n" +
                    "• 说明租赁流程\n" +
                    "• 回答技术问题，生成代码等\n\n" +
                    "有什么问题尽管问我吧！";
        }

        // 默认回复
        return "感谢你的提问！\n\n" +
                "我可以帮你：\n" +
                "• 上传无人机图片，帮你分析损伤情况\n" +
                "• 推荐适合的无人机型号\n" +
                "• 解答租赁和支付相关问题\n" +
                "• 提供使用建议和技巧\n" +
                "• 回答技术问题，包括生成代码、数据结构等\n\n" +
                "请告诉我你具体想了解什么？";
    }

    /**
     * 获取AI助手状态
     */
    @GetMapping("/user/api/assistant/status")
    public ApiResult<Map<String, Object>> getStatus() {
        Map<String, Object> result = new HashMap<>();
        boolean enabled = aiApiKey != null && !aiApiKey.isEmpty();
        result.put("enabled", enabled);
        result.put("model", enabled ? aiModel : "未配置");
        result.put("provider", "Anthropic Claude");
        result.put("knowledgeEnabled", knowledgeEnabled && knowledgeBaseService != null);
        return ApiResult.success(result);
    }
}
