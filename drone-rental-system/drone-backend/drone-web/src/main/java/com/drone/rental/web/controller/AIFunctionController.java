package com.drone.rental.web.controller;

import com.alibaba.fastjson2.JSON;
import com.drone.rental.dao.entity.*;
import com.drone.rental.dao.mapper.*;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.service.service.DroneDataService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI助手控制器 - 支持Function Calling函数调用
 */
@RestController
public class AIFunctionController {

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.api.url:http://47.108.60.237:3300/api/v1/messages}")
    private String aiApiUrl;

    @Value("${ai.model:claude-3-5-sonnet-20241022}")
    private String aiModel;

    @Autowired(required = false)
    private DroneDataService droneDataService;

    @Autowired
    private DroneVehicleMapper droneVehicleMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DroneOrderMapper droneOrderMapper;

    @Autowired
    private DroneRepairMapper droneRepairMapper;

    @Autowired
    private DroneDetectionMapper droneDetectionMapper;

    @Autowired
    private DroneSystemConfigMapper configMapper;

    @Autowired
    private RestTemplate restTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * AI对话接口 - 支持函数调用
     */
    @PostMapping("/user/api/assistant/chat2")
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
            // 第一轮：发送消息和可用工具给AI
            Map<String, Object> aiResponse = callAIWithTools(message, imageBase64, user.getId());

            // 调试：打印AI响应
            System.out.println("=== AI Response ===");
            System.out.println(JSON.toJSONString(aiResponse));

            // 检查AI是否想要调用工具
            List<Map<String, Object>> toolUseBlocks = extractToolUseBlocks(aiResponse);
            System.out.println("=== Tool Use Blocks: " + toolUseBlocks.size() + " ===");

            if (!toolUseBlocks.isEmpty()) {
                // 执行工具调用
                List<Map<String, Object>> toolResults = new ArrayList<>();
                for (Map<String, Object> toolUse : toolUseBlocks) {
                    String toolName = (String) toolUse.get("name");
                    String toolUseId = (String) toolUse.get("id");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> toolInputs = (Map<String, Object>) toolUse.get("input");

                    Object result = executeTool(toolName, toolInputs, user.getId());
                    toolResults.add(Map.of(
                        "tool_use_id", toolUseId,
                        "result", result
                    ));
                }

                // 第二轮：将工具结果发送回AI获取最终回复
                String finalReply = callAIWithToolResults(message, toolUseBlocks, toolResults);

                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("reply", finalReply);
                resultMap.put("timestamp", System.currentTimeMillis());
                return ApiResult.success(resultMap);
            }

            // AI直接回复，没有调用工具
            String reply = extractTextContent(aiResponse);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("reply", reply);
            resultMap.put("timestamp", System.currentTimeMillis());
            return ApiResult.success(resultMap);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> resultMap = new HashMap<>();

            String errorMessage = e.getMessage();
            String userFriendlyMessage = "抱歉，AI服务暂时不可用，请稍后再试。";

            // 根据错误信息提供更友好的提示
            if (errorMessage != null) {
                if (errorMessage.contains("E015") || errorMessage.contains("Internal server error")) {
                    if (imageBase64 != null && !imageBase64.isEmpty()) {
                        userFriendlyMessage = "抱歉，图片识别功能暂时不可用。请尝试：\n1. 只发送文字消息\n2. 稍后再试\n3. 联系管理员检查AI服务配置";
                    } else {
                        userFriendlyMessage = "抱歉，AI服务暂时不可用，请稍后再试。";
                    }
                } else if (errorMessage.contains("timeout") || errorMessage.contains("timed out")) {
                    userFriendlyMessage = "抱歉，AI服务响应超时，请稍后再试。";
                } else if (errorMessage.contains("429") || errorMessage.contains("rate limit")) {
                    userFriendlyMessage = "抱歉，AI服务请求过于频繁，请稍后再试。";
                } else if (errorMessage.contains("401") || errorMessage.contains("403")) {
                    userFriendlyMessage = "抱歉，AI服务认证失败，请联系管理员检查API配置。";
                }
            }

            resultMap.put("reply", userFriendlyMessage);
            resultMap.put("timestamp", System.currentTimeMillis());
            return ApiResult.success(resultMap);
        }
    }

    /**
     * 调用AI，发送消息和可用工具
     */
    private Map<String, Object> callAIWithTools(String message, String imageBase64, Long userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiModel);
        body.put("max_tokens", 4096);

        // 系统提示词 - 独立于消息数组
        body.put("system", buildSystemPrompt());

        // 定义可用工具
        body.put("tools", buildToolDefinitions());

        // 构建用户消息内容
        List<Map<String, Object>> content = new ArrayList<>();

        // 添加图片（如果有）
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image");

            Map<String, String> imageSource = new HashMap<>();
            imageSource.put("type", "base64");

            String mediaType = "image/jpeg";
            String base64Data = imageBase64;
            if (imageBase64.contains(",")) {
                String dataPrefix = imageBase64.split(",")[0].toLowerCase();
                if (dataPrefix.contains("png")) {
                    mediaType = "image/png";
                } else if (dataPrefix.contains("gif")) {
                    mediaType = "image/gif";
                } else if (dataPrefix.contains("webp")) {
                    mediaType = "image/webp";
                }
                base64Data = imageBase64.split(",")[1];
            }

            imageSource.put("media_type", mediaType);
            imageSource.put("data", base64Data);
            imageContent.put("source", imageSource);
            content.add(imageContent);
        }

        // 添加文本消息
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", message != null ? message : "请帮我分析这张图片。");
        content.add(textContent);

        // 构建消息列表
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", content);
        messages.add(userMessage);

        body.put("messages", messages);

        // 调试：打印请求内容
        System.out.println("=== AI Request ===");
        System.out.println("URL: " + aiApiUrl);
        System.out.println("Model: " + aiModel);
        System.out.println("Request Body: " + JSON.toJSONString(body));

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", aiApiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    aiApiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

            throw new RuntimeException("AI请求失败，状态码: " + response.getStatusCode());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // 捕获API返回的错误详情
            String errorBody = e.getResponseBodyAsString();
            System.err.println("=== AI API Error ===");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Response: " + errorBody);

            // 尝试解析错误信息
            String errorMsg = "AI服务暂时不可用";
            if (errorBody != null && !errorBody.isEmpty()) {
                try {
                    Map<String, Object> errorMap = JSON.parseObject(errorBody, Map.class);
                    if (errorMap.containsKey("error")) {
                        Map<String, Object> errorDetail = (Map<String, Object>) errorMap.get("error");
                        if (errorDetail.containsKey("message")) {
                            errorMsg = "AI服务错误: " + errorDetail.get("message");
                            if (errorDetail.containsKey("code")) {
                                errorMsg += " (错误码: " + errorDetail.get("code") + ")";
                            }
                        }
                    }
                } catch (Exception ex) {
                    errorMsg = "AI服务错误: " + errorBody.substring(0, Math.min(200, errorBody.length()));
                }
            }

            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * 将工具结果发送回AI
     * @param originalMessage 原始用户消息
     * @param toolUseBlocks AI返回的tool_use块（需要包含在对话历史中）
     * @param toolResults 工具执行结果
     */
    private String callAIWithToolResults(String originalMessage, List<Map<String, Object>> toolUseBlocks, List<Map<String, Object>> toolResults) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiModel);
        body.put("max_tokens", 4096);

        // 系统提示词
        body.put("system", buildSystemPrompt());

        body.put("tools", buildToolDefinitions());

        // 构建消息历史
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. 原始用户消息
        Map<String, Object> originalUserMsg = new HashMap<>();
        originalUserMsg.put("role", "user");
        originalUserMsg.put("content", originalMessage);
        messages.add(originalUserMsg);

        // 2. Assistant的响应（包含tool_use块）
        Map<String, Object> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", toolUseBlocks);
        messages.add(assistantMsg);

        // 3. 工具结果（每个tool_result作为单独的user消息）
        for (Map<String, Object> result : toolResults) {
            Map<String, Object> toolResultMsg = new HashMap<>();
            toolResultMsg.put("role", "user");

            // 将结果格式化为易读的字符串
            String resultStr = formatToolResult(result.get("result"));

            toolResultMsg.put("content", Arrays.asList(Map.of(
                "type", "tool_result",
                "tool_use_id", result.get("tool_use_id"),
                "content", resultStr  // GLM使用content字段而不是result字段
            )));
            messages.add(toolResultMsg);
        }

        body.put("messages", messages);

        // 调试：打印工具结果
        System.out.println("=== Tool Results ===");
        for (Map<String, Object> result : toolResults) {
            System.out.println("Tool Use ID: " + result.get("tool_use_id"));
            System.out.println("Result: " + formatToolResult(result.get("result")));
        }

        // 调试：打印第二轮请求内容
        System.out.println("=== AI Request (with tool results) ===");
        System.out.println("Request Body: " + JSON.toJSONString(body));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", aiApiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                aiApiUrl,
                HttpMethod.POST,
                entity,
                Map.class
        );

        // 调试：打印第二轮AI响应
        System.out.println("=== AI Response (with tool results) ===");
        System.out.println("Response: " + JSON.toJSONString(response.getBody()));

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return extractTextContent(response.getBody());
        }

        return "抱歉，我无法处理查询结果。";
    }

    /**
     * 从AI响应中提取tool_use块
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractToolUseBlocks(Map<String, Object> response) {
        List<Map<String, Object>> toolUses = new ArrayList<>();

        try {
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content == null) return toolUses;

            for (Map<String, Object> block : content) {
                if ("tool_use".equals(block.get("type"))) {
                    toolUses.add(block);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return toolUses;
    }

    /**
     * 从AI响应中提取文本内容
     */
    @SuppressWarnings("unchecked")
    private String extractTextContent(Map<String, Object> response) {
        try {
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content == null) return "抱歉，我无法理解回复内容。";

            StringBuilder text = new StringBuilder();
            for (Map<String, Object> block : content) {
                if ("text".equals(block.get("type"))) {
                    text.append(block.get("text"));
                }
            }
            return text.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，我无法理解回复内容。";
        }
    }

    /**
     * 构建工具定义
     */
    private List<Map<String, Object>> buildToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // 工具1: 查询用户余额
        tools.add(Map.of(
            "name", "get_user_balance",
            "description", "获取用户的账户余额和押金信息",
            "input_schema", Map.of(
                "type", "object",
                "properties", new HashMap<>(),
                "required", List.of()
            )
        ));

        // 工具2: 查询用户订单
        tools.add(Map.of(
            "name", "get_user_orders",
            "description", "获取用户的订单列表，包括订单号、无人机编号、时间、金额和状态",
            "input_schema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "limit", Map.of(
                        "type", "integer",
                        "description", "返回的订单数量限制，默认5"
                    )
                )
            )
        ));

        // 工具3: 查询用户订单统计
        tools.add(Map.of(
            "name", "get_user_order_stats",
            "description", "获取用户的订单统计信息，包括总订单数、各状态订单数、累计消费",
            "input_schema", Map.of(
                "type", "object",
                "properties", new HashMap<>()
            )
        ));

        // 工具4: 查询用户报修记录
        tools.add(Map.of(
            "name", "get_user_repairs",
            "description", "获取用户的报修记录列表",
            "input_schema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "limit", Map.of(
                        "type", "integer",
                        "description", "返回的记录数量限制，默认5"
                    )
                )
            )
        ));

        // 工具5: 查询用户损伤检测记录
        tools.add(Map.of(
            "name", "get_user_detections",
            "description", "获取用户的损伤检测记录列表",
            "input_schema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "limit", Map.of(
                        "type", "integer",
                        "description", "返回的记录数量限制，默认5"
                    )
                )
            )
        ));

        // 工具6: 查询可租赁无人机列表
        tools.add(Map.of(
            "name", "get_available_drones",
            "description", "获取当前可租赁的无人机列表，包括品牌、型号、编号、电量、价格",
            "input_schema", Map.of(
                "type", "object",
                "properties", new HashMap<>()
            )
        ));

        // 工具7: 查询无人机位置
        tools.add(Map.of(
            "name", "get_drone_location",
            "description", "根据无人机编号查询当前位置和状态",
            "input_schema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "vehicle_no", Map.of(
                        "type", "string",
                        "description", "无人机编号，如DRONE-E005"
                    )
                ),
                "required", List.of("vehicle_no")
            )
        ));

        // 工具8: 查询无人机状态统计
        tools.add(Map.of(
            "name", "get_drone_stats",
            "description", "获取无人机的状态统计，包括可租赁、租赁中、维护中、充电中的数量",
            "input_schema", Map.of(
                "type", "object",
                "properties", new HashMap<>()
            )
        ));

        // 工具9: 查询价格
        tools.add(Map.of(
            "name", "get_pricing",
            "description", "获取当前的租赁价格配置，包括时租金和日租金",
            "input_schema", Map.of(
                "type", "object",
                "properties", new HashMap<>()
            )
        ));

        // 工具10: 查询特定无人机信息
        tools.add(Map.of(
            "name", "get_drone_info",
            "description", "根据无人机编号或型号查询无人机详细信息",
            "input_schema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "keyword", Map.of(
                        "type", "string",
                        "description", "无人机编号（如DRONE-E005）或型号（如Mavic）"
                    )
                )
            )
        ));

        return tools;
    }

    /**
     * 执行工具调用
     */
    private Object executeTool(String toolName, Map<String, Object> inputs, Long userId) {
        try {
            switch (toolName) {
                case "get_user_balance":
                    return getUserBalance(userId);

                case "get_user_orders":
                    int limit = inputs.containsKey("limit") ?
                            ((Number) inputs.get("limit")).intValue() : 5;
                    return getUserOrders(userId, limit);

                case "get_user_order_stats":
                    return getUserOrderStats(userId);

                case "get_user_repairs":
                    int repairLimit = inputs.containsKey("limit") ?
                            ((Number) inputs.get("limit")).intValue() : 5;
                    return getUserRepairs(userId, repairLimit);

                case "get_user_detections":
                    int detectionLimit = inputs.containsKey("limit") ?
                            ((Number) inputs.get("limit")).intValue() : 5;
                    return getUserDetections(userId, detectionLimit);

                case "get_available_drones":
                    return getAvailableDrones();

                case "get_drone_location":
                    String vehicleNo = (String) inputs.get("vehicle_no");
                    return getDroneLocation(vehicleNo);

                case "get_drone_stats":
                    return getDroneStats();

                case "get_pricing":
                    return getPricing();

                case "get_drone_info":
                    String keyword = (String) inputs.get("keyword");
                    return getDroneInfo(keyword);

                default:
                    return Map.of("error", "未知的工具: " + toolName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "工具执行失败: " + e.getMessage());
        }
    }

    // ==================== 工具实现方法 ====================

    private Map<String, Object> getUserBalance(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Map.of("error", "用户不存在");
        }
        return Map.of(
            "balance", user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO,
            "deposit", user.getDeposit() != null ? user.getDeposit() : BigDecimal.ZERO,
            "username", user.getUsername(),
            "realName", user.getRealName() != null ? user.getRealName() : "",
            "phone", user.getPhone() != null ? user.getPhone() : ""
        );
    }

    private List<Map<String, Object>> getUserOrders(Long userId, int limit) {
        List<DroneOrder> orders = droneOrderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneOrder>()
                        .eq(DroneOrder::getUserId, userId)
                        .orderByDesc(DroneOrder::getCreateTime)
                        .last("LIMIT " + limit)
        );

        return orders.stream().map(o -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orderNo", o.getOrderNo());
            map.put("vehicleNo", o.getVehicleNo());
            map.put("startTime", o.getStartTime() != null ? o.getStartTime().format(DATE_FORMATTER) : "");
            map.put("endTime", o.getEndTime() != null ? o.getEndTime().format(DATE_FORMATTER) : "");
            map.put("amount", o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO);
            map.put("status", getOrderStatusText(o.getStatus()));
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> getUserOrderStats(Long userId) {
        List<DroneOrder> orders = droneOrderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneOrder>()
                        .eq(DroneOrder::getUserId, userId)
        );

        int unpaid = 0, active = 0, completed = 0, cancelled = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (DroneOrder o : orders) {
            Integer status = o.getStatus();
            if (status == null) continue;
            if (status == 0) unpaid++;
            else if (status == 1 || status == 2) active++;
            else if (status == 3) {
                completed++;
                if (o.getAmount() != null) totalAmount = totalAmount.add(o.getAmount());
            }
            else if (status == 4) cancelled++;
        }

        return Map.of(
            "total", orders.size(),
            "unpaid", unpaid,
            "active", active,
            "completed", completed,
            "cancelled", cancelled,
            "totalAmount", totalAmount
        );
    }

    private List<Map<String, Object>> getUserRepairs(Long userId, int limit) {
        List<DroneRepair> repairs = droneRepairMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneRepair>()
                        .eq(DroneRepair::getReporterId, userId)
                        .orderByDesc(DroneRepair::getCreateTime)
                        .last("LIMIT " + limit)
        );

        return repairs.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("repairNo", r.getRepairNo());
            map.put("vehicleNo", r.getVehicleNo());
            map.put("faultType", r.getFaultType());
            map.put("faultDescription", r.getFaultDescription());
            map.put("status", getRepairStatusText(r.getStatus()));
            map.put("createTime", r.getCreateTime() != null ? r.getCreateTime().format(DATE_FORMATTER) : "");
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getUserDetections(Long userId, int limit) {
        List<DroneDetection> detections = droneDetectionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneDetection>()
                        .eq(DroneDetection::getUserId, userId)
                        .orderByDesc(DroneDetection::getCreateTime)
                        .last("LIMIT " + limit)
        );

        return detections.stream().map(d -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("detectionNo", d.getDetectionNo());
            map.put("vehicleNo", d.getVehicleNo());
            map.put("damageCount", d.getDamageCount());
            map.put("overallSeverity", d.getOverallSeverity());
            map.put("responsibility", d.getResponsibility());
            map.put("createTime", d.getCreateTime() != null ? d.getCreateTime().format(DATE_FORMATTER) : "");
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getAvailableDrones() {
        List<DroneVehicle> drones = droneVehicleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneVehicle>()
                        .eq(DroneVehicle::getStatus, 1)
                        .eq(DroneVehicle::getIsListed, 1)
        );

        BigDecimal hourlyPrice = getConfigValue("rental.price.per.hour", new BigDecimal("20.00"));

        return drones.stream().map(d -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("vehicleNo", d.getVehicleNo());
            map.put("brand", d.getBrand() != null ? d.getBrand() : "");
            map.put("model", d.getModel() != null ? d.getModel() : "");
            map.put("batteryLevel", d.getBatteryLevel());
            map.put("locationDetail", d.getLocationDetail() != null ? d.getLocationDetail() : "");
            map.put("hourlyPrice", hourlyPrice);
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> getDroneLocation(String vehicleNo) {
        DroneVehicle drone = droneVehicleMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneVehicle>()
                        .eq(DroneVehicle::getVehicleNo, vehicleNo)
        );

        if (drone == null) {
            return Map.of("found", false, "message", "未找到编号为 " + vehicleNo + " 的无人机");
        }

        return Map.of(
            "found", true,
            "vehicleNo", drone.getVehicleNo(),
            "brand", drone.getBrand() != null ? drone.getBrand() : "",
            "model", drone.getModel() != null ? drone.getModel() : "",
            "status", getStatusText(drone.getStatus()),
            "latitude", drone.getLatitude(),
            "longitude", drone.getLongitude(),
            "locationDetail", drone.getLocationDetail() != null ? drone.getLocationDetail() : "",
            "batteryLevel", drone.getBatteryLevel()
        );
    }

    private Map<String, Object> getDroneStats() {
        List<DroneVehicle> allDrones = droneVehicleMapper.selectList(null);

        int available = 0, rented = 0, maintenance = 0, charging = 0;

        for (DroneVehicle drone : allDrones) {
            Integer status = drone.getStatus();
            if (status == null) continue;
            switch (status) {
                case 1: available++; break;
                case 2: rented++; break;
                case 3: maintenance++; break;
                case 4: charging++; break;
            }
        }

        return Map.of(
            "available", available,
            "rented", rented,
            "maintenance", maintenance,
            "charging", charging,
            "total", allDrones.size()
        );
    }

    private Map<String, Object> getPricing() {
        BigDecimal hourlyPrice = getConfigValue("rental.price.per.hour", new BigDecimal("20.00"));
        BigDecimal dailyPrice = hourlyPrice.multiply(new BigDecimal(8));

        return Map.of(
            "hourlyPrice", hourlyPrice,
            "dailyPrice", dailyPrice,
            "deposit", getConfigValue("rental.deposit.amount", new BigDecimal("500.00"))
        );
    }

    private List<Map<String, Object>> getDroneInfo(String keyword) {
        List<DroneVehicle> drones = droneVehicleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneVehicle>()
                        .and(wrapper -> wrapper
                                .like(DroneVehicle::getVehicleNo, keyword)
                                .or()
                                .like(DroneVehicle::getModel, keyword)
                                .or()
                                .like(DroneVehicle::getBrand, keyword)
                        )
                        .last("LIMIT 5")
        );

        BigDecimal hourlyPrice = getConfigValue("rental.price.per.hour", new BigDecimal("20.00"));

        return drones.stream().map(d -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("vehicleNo", d.getVehicleNo());
            map.put("brand", d.getBrand() != null ? d.getBrand() : "");
            map.put("model", d.getModel() != null ? d.getModel() : "");
            map.put("status", getStatusText(d.getStatus()));
            map.put("batteryLevel", d.getBatteryLevel());
            map.put("locationDetail", d.getLocationDetail() != null ? d.getLocationDetail() : "");
            map.put("hourlyPrice", hourlyPrice);
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt() {
        return "你是一个无人机租赁系统的AI助手。你可以使用提供的工具来查询数据库中的真实数据。\n\n" +
                "重要规则：\n" +
                "1. 当用户询问关于他们账户、订单、余额等问题时，请调用相应的工具获取数据\n" +
                "2. 当用户询问无人机、价格、位置等问题时，请调用相应的工具获取数据\n" +
                "3. 获取数据后，用友好、简洁的中文回复用户\n" +
                "4. 不要编造数据，只使用工具返回的真实数据\n" +
                "5. 金额格式：使用¥符号，如¥190.00\n" +
                "6. 日期格式：使用 yyyy-MM-dd HH:mm 格式\n\n" +
                "可用工具：\n" +
                "- get_user_balance: 查询用户余额\n" +
                "- get_user_orders: 查询用户订单\n" +
                "- get_user_order_stats: 查询订单统计\n" +
                "- get_user_repairs: 查询报修记录\n" +
                "- get_user_detections: 查询损伤检测记录\n" +
                "- get_available_drones: 查询可租赁无人机\n" +
                "- get_drone_location: 查询无人机位置\n" +
                "- get_drone_stats: 查询无人机统计\n" +
                "- get_pricing: 查询价格配置\n" +
                "- get_drone_info: 查询特定无人机信息";
    }

    private String getOrderStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "租赁中";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知";
        }
    }

    private String getRepairStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待处理";
            case 1: return "处理中";
            case 2: return "已完成";
            default: return "未知";
        }
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "可租赁";
            case 2: return "租赁中";
            case 3: return "维护中";
            case 4: return "充电中";
            default: return "未知";
        }
    }

    private BigDecimal getConfigValue(String key, BigDecimal defaultValue) {
        try {
            DroneSystemConfig config = configMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneSystemConfig>()
                            .eq(DroneSystemConfig::getConfigKey, key)
            );
            if (config != null && config.getConfigValue() != null) {
                return new BigDecimal(config.getConfigValue());
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultValue;
    }

    /**
     * 格式化工具执行结果为易读的字符串
     */
    @SuppressWarnings("unchecked")
    private String formatToolResult(Object result) {
        if (result == null) {
            return "查询结果为空";
        }

        // 如果是Map，格式化为易读文本
        if (result instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) result;

            // 检查是否有错误
            if (map.containsKey("error")) {
                return "错误: " + map.get("error");
            }

            StringBuilder sb = new StringBuilder();

            // 处理用户余额结果
            if (map.containsKey("balance")) {
                sb.append("账户余额: ¥").append(map.get("balance")).append("\n");
                sb.append("押金: ¥").append(map.get("deposit")).append("\n");
                if (map.containsKey("username")) {
                    sb.append("用户名: ").append(map.get("username")).append("\n");
                }
                return sb.toString();
            }

            // 处理订单列表结果
            if (map.containsKey("orderNo")) {
                sb.append("订单号: ").append(map.get("orderNo")).append("\n");
                sb.append("无人机编号: ").append(map.get("vehicleNo")).append("\n");
                sb.append("状态: ").append(map.get("status")).append("\n");
                sb.append("金额: ¥").append(map.get("amount")).append("\n");
                return sb.toString();
            }

            // 处理订单统计结果
            if (map.containsKey("total")) {
                sb.append("订单统计:\n");
                sb.append("- 总订单数: ").append(map.get("total")).append("\n");
                sb.append("- 待支付: ").append(map.get("unpaid")).append("\n");
                sb.append("- 进行中: ").append(map.get("active")).append("\n");
                sb.append("- 已完成: ").append(map.get("completed")).append("\n");
                sb.append("- 已取消: ").append(map.get("cancelled")).append("\n");
                if (map.containsKey("totalAmount")) {
                    sb.append("- 累计消费: ¥").append(map.get("totalAmount")).append("\n");
                }
                return sb.toString();
            }

            // 处理无人机位置结果
            if (map.containsKey("found")) {
                if (Boolean.FALSE.equals(map.get("found"))) {
                    return String.valueOf(map.get("message"));
                }
                sb.append("无人机编号: ").append(map.get("vehicleNo")).append("\n");
                sb.append("型号: ").append(map.get("brand")).append(" ").append(map.get("model")).append("\n");
                sb.append("状态: ").append(map.get("status")).append("\n");
                sb.append("位置: ").append(map.get("locationDetail")).append("\n");
                sb.append("电量: ").append(map.get("batteryLevel")).append("%\n");
                return sb.toString();
            }

            // 处理无人机统计结果
            if (map.containsKey("available") && map.containsKey("total")) {
                sb.append("无人机统计:\n");
                sb.append("- 总数: ").append(map.get("total")).append("\n");
                sb.append("- 可租赁: ").append(map.get("available")).append("\n");
                sb.append("- 租赁中: ").append(map.get("rented")).append("\n");
                sb.append("- 维护中: ").append(map.get("maintenance")).append("\n");
                sb.append("- 充电中: ").append(map.get("charging")).append("\n");
                return sb.toString();
            }

            // 处理价格配置结果
            if (map.containsKey("hourlyPrice")) {
                sb.append("价格配置:\n");
                sb.append("- 时租金: ¥").append(map.get("hourlyPrice")).append("/小时\n");
                sb.append("- 日租金: ¥").append(map.get("dailyPrice")).append("/天\n");
                sb.append("- 押金: ¥").append(map.get("deposit")).append("\n");
                return sb.toString();
            }

            // 默认格式化为JSON字符串
            return JSON.toJSONString(map);
        }

        // 如果是List，格式化每一项
        if (result instanceof List) {
            List<?> list = (List<?>) result;
            if (list.isEmpty()) {
                return "暂无数据";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size() && i < 5; i++) {
                sb.append("[").append(i + 1).append("] ").append(formatToolResult(list.get(i))).append("\n");
            }
            if (list.size() > 5) {
                sb.append("... 还有 ").append(list.size() - 5).append(" 条记录\n");
            }
            return sb.toString();
        }

        return String.valueOf(result);
    }
}
