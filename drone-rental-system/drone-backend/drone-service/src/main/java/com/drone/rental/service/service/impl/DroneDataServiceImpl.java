package com.drone.rental.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.dao.entity.*;
import com.drone.rental.dao.mapper.DroneRepairMapper;
import com.drone.rental.dao.mapper.DroneVehicleMapper;
import com.drone.rental.dao.mapper.SysUserMapper;
import com.drone.rental.dao.mapper.DroneDetectionMapper;
import com.drone.rental.dao.mapper.DroneOrderMapper;
import com.drone.rental.dao.mapper.DroneSystemConfigMapper;
import com.drone.rental.service.service.DroneDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 无人机数据查询服务实现
 */
@Service
public class DroneDataServiceImpl extends ServiceImpl<DroneVehicleMapper, DroneVehicle>
        implements DroneDataService {

    private static final Logger log = LoggerFactory.getLogger(DroneDataServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private DroneRepairMapper droneRepairMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DroneDetectionMapper droneDetectionMapper;

    @Autowired
    private DroneOrderMapper droneOrderMapper;

    @Autowired
    private DroneSystemConfigMapper configMapper;

    // ==================== 通用查询方法 ====================

    @Override
    public List<DroneVehicle> getByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return lambdaQuery()
                .like(DroneVehicle::getModel, name)
                .or()
                .like(DroneVehicle::getBrand, name)
                .orderByAsc(DroneVehicle::getId)
                .list();
    }

    @Override
    public List<DroneVehicle> getAvailableDrones() {
        return lambdaQuery()
                .eq(DroneVehicle::getStatus, 1)
                .eq(DroneVehicle::getIsListed, 1)
                .orderByAsc(DroneVehicle::getId)
                .list();
    }

    @Override
    public Map<String, Object> getDroneLocation(Long id) {
        DroneVehicle drone = getById(id);
        if (drone == null) {
            return Collections.singletonMap("found", false);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("found", true);
        result.put("id", drone.getId());
        result.put("vehicleNo", drone.getVehicleNo());
        result.put("brand", drone.getBrand());
        result.put("model", drone.getModel());
        result.put("status", getStatusText(drone.getStatus()));
        result.put("latitude", drone.getLatitude());
        result.put("longitude", drone.getLongitude());
        result.put("locationDetail", drone.getLocationDetail());
        result.put("batteryLevel", drone.getBatteryLevel());
        result.put("isCharging", drone.getChargingStartTime() != null);

        return result;
    }

    @Override
    public Map<String, Object> getDroneLocationByNo(String vehicleNo) {
        DroneVehicle drone = lambdaQuery()
                .eq(DroneVehicle::getVehicleNo, vehicleNo)
                .one();

        if (drone == null) {
            return Collections.singletonMap("found", false);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("found", true);
        result.put("id", drone.getId());
        result.put("vehicleNo", drone.getVehicleNo());
        result.put("brand", drone.getBrand());
        result.put("model", drone.getModel());
        result.put("status", getStatusText(drone.getStatus()));
        result.put("latitude", drone.getLatitude());
        result.put("longitude", drone.getLongitude());
        result.put("locationDetail", drone.getLocationDetail());
        result.put("batteryLevel", drone.getBatteryLevel());
        result.put("isCharging", drone.getChargingStartTime() != null);

        return result;
    }

    @Override
    public List<DroneVehicle> getByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        // 价格是全局配置，这里返回所有可用的无人机
        return lambdaQuery()
                .eq(DroneVehicle::getStatus, 1)
                .eq(DroneVehicle::getIsListed, 1)
                .orderByAsc(DroneVehicle::getId)
                .list();
    }

    @Override
    public Map<String, Object> getModelStatistics() {
        List<DroneVehicle> allDrones = list();

        Map<String, Integer> modelCount = new HashMap<>();
        Map<String, Integer> modelAvailable = new HashMap<>();

        for (DroneVehicle drone : allDrones) {
            String model = drone.getModel();
            if (model == null || model.isEmpty()) {
                model = "未知型号";
            }

            modelCount.put(model, modelCount.getOrDefault(model, 0) + 1);

            if (drone.getStatus() != null && drone.getStatus() == 1) {
                modelAvailable.put(model, modelAvailable.getOrDefault(model, 0) + 1);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("modelCount", modelCount);
        result.put("modelAvailable", modelAvailable);
        result.put("totalModels", modelCount.size());
        return result;
    }

    @Override
    public Map<String, Object> getStatusStatistics() {
        List<DroneVehicle> allDrones = list();

        int available = 0;
        int rented = 0;
        int maintenance = 0;
        int charging = 0;

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

        Map<String, Object> result = new HashMap<>();
        result.put("available", available);
        result.put("rented", rented);
        result.put("maintenance", maintenance);
        result.put("charging", charging);
        result.put("total", allDrones.size());
        return result;
    }

    @Override
    public String answerDataQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "抱歉，我没有理解您的问题。";
        }

        String q = question.toLowerCase();

        try {
            // 价格查询
            if (q.contains("多少钱") || q.contains("价格") || q.contains("费用")) {
                return handlePriceQuestion(question);
            }

            // 位置查询
            if (q.contains("在哪") || q.contains("位置") || q.contains("在哪里")) {
                return handleLocationQuestion(question);
            }

            // 可用无人机查询（包含"店里"、"店内"等场景）
            if (q.contains("可租") || q.contains("可用") || q.contains("有哪些无人机") ||
                q.contains("店里") || q.contains("店内") || q.contains("我们店")) {
                return handleAvailableQuestion(question);
            }

            // 状态/数量查询（包含多种表达方式）
            if (q.contains("状态") || q.contains("多少台") || q.contains("多少架") ||
                q.contains("几架") || q.contains("几台") || q.contains("有几")) {
                return handleStatusQuestion(question);
            }

            // 型号查询
            if (q.contains("型号") || q.contains("什么型号")) {
                return handleModelQuestion(question);
            }

            return "抱歉，我暂时无法回答这类数据问题。您可以询问关于价格、位置、可用无人机等问题。";

        } catch (Exception e) {
            log.error("处理数据查询问题失败: {}", question, e);
            return "查询数据时出现错误，请稍后再试。";
        }
    }

    @Override
    public String answerUserDataQuestion(String question, Long userId) {
        if (question == null || question.trim().isEmpty()) {
            return "抱歉，我没有理解您的问题。";
        }

        if (userId == null) {
            return "请先登录后再查询个人信息。";
        }

        String q = question.toLowerCase();

        try {
            // 用户余额查询
            if (q.contains("余额") || q.contains("钱包") || (q.contains("多少钱") && q.contains("账户"))) {
                return handleUserBalanceQuestion(userId);
            }

            // 用户报修查询
            if (q.contains("报修") || q.contains("维修") || q.contains("故障")) {
                return handleUserRepairQuestion(userId, question);
            }

            // 用户订单查询
            if (q.contains("订单") || q.contains("租赁") || q.contains("租过")) {
                return handleUserOrderQuestion(userId, question);
            }

            // 损伤检测查询
            if (q.contains("损伤") || q.contains("检测") || q.contains("损坏")) {
                return handleUserDetectionQuestion(userId, question);
            }

            // 智能推荐查询
            if (q.contains("推荐") || q.contains("建议")) {
                return handleRecommendationQuestion(question);
            }

            // 如果不是用户特定问题，尝试通用查询
            return answerDataQuestion(question);

        } catch (Exception e) {
            log.error("处理用户数据查询问题失败: userId={}, question={}", userId, question, e);
            return "查询数据时出现错误，请稍后再试。";
        }
    }

    // ==================== 用户相关查询处理 ====================

    private String handleUserBalanceQuestion(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return "未找到用户信息。";
        }

        StringBuilder sb = new StringBuilder("您的账户信息：\n\n");
        sb.append(String.format("- 余额：¥%s\n", user.getBalance() != null ? user.getBalance() : "0.00"));
        sb.append(String.format("- 押金：¥%s\n", user.getDeposit() != null ? user.getDeposit() : "0.00"));
        sb.append(String.format("- 用户名：%s\n", user.getUsername()));
        if (user.getRealName() != null) {
            sb.append(String.format("- 真实姓名：%s\n", user.getRealName()));
        }
        if (user.getPhone() != null) {
            sb.append(String.format("- 手机号：%s\n", user.getPhone()));
        }

        return sb.toString();
    }

    private String handleUserRepairQuestion(Long userId, String question) {
        List<Map<String, Object>> repairs = getUserRepairs(userId);

        if (repairs.isEmpty()) {
            return "您目前没有报修记录。";
        }

        // 检查是否询问统计
        if (question.contains("多少") || question.contains("统计") || question.contains("几个")) {
            Map<String, Object> stats = getUserRepairStats(userId);
            StringBuilder sb = new StringBuilder("您的报修统计：\n\n");
            sb.append(String.format("- 总报修次数：%d 次\n", stats.get("total")));
            sb.append(String.format("- 待处理：%d 次\n", stats.get("pending")));
            sb.append(String.format("- 处理中：%d 次\n", stats.get("processing")));
            sb.append(String.format("- 已完成：%d 次\n", stats.get("completed")));
            return sb.toString();
        }

        // 返回报修列表
        StringBuilder sb = new StringBuilder(String.format("您共有 %d 条报修记录：\n\n", repairs.size()));

        int limit = Math.min(repairs.size(), 5);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> repair = repairs.get(i);
            sb.append(String.format("**%s** (%s)\n", repair.get("repairNo"), repair.get("faultType")));
            sb.append(String.format("- 无人机：%s\n", repair.get("vehicleNo")));
            sb.append(String.format("- 故障描述：%s\n", repair.get("faultDescription")));
            sb.append(String.format("- 状态：%s\n", repair.get("status")));
            sb.append(String.format("- 报修时间：%s\n", repair.get("createTime")));
            sb.append("\n");
        }

        if (repairs.size() > 5) {
            sb.append(String.format("... 还有 %d 条记录\n", repairs.size() - 5));
        }

        return sb.toString();
    }

    private String handleUserOrderQuestion(Long userId, String question) {
        List<Map<String, Object>> orders = getUserOrders(userId);

        if (orders.isEmpty()) {
            return "您目前没有订单记录。";
        }

        // 检查是否询问统计
        if (question.contains("多少") || question.contains("统计") || question.contains("几个")) {
            Map<String, Object> stats = getUserOrderStats(userId);
            StringBuilder sb = new StringBuilder("您的订单统计：\n\n");
            sb.append(String.format("- 总订单数：%d 个\n", stats.get("total")));
            sb.append(String.format("- 待支付：%d 个\n", stats.get("unpaid")));
            sb.append(String.format("- 进行中：%d 个\n", stats.get("active")));
            sb.append(String.format("- 已完成：%d 个\n", stats.get("completed")));
            sb.append(String.format("- 已取消：%d 个\n", stats.get("cancelled")));
            sb.append(String.format("- 累计消费：¥%s\n", stats.get("totalAmount")));
            return sb.toString();
        }

        // 检查是否询问进行中的订单
        if (question.contains("进行中") || question.contains("当前") || question.contains("正在")) {
            List<Map<String, Object>> activeOrders = orders.stream()
                    .filter(o -> "进行中".equals(o.get("status")) || "租赁中".equals(o.get("status")) || "已支付".equals(o.get("status")))
                    .collect(Collectors.toList());

            if (activeOrders.isEmpty()) {
                return "您目前没有进行中的订单。";
            }

            StringBuilder sb = new StringBuilder("您当前进行中的订单：\n\n");
            for (Map<String, Object> order : activeOrders) {
                sb.append(formatOrderInfo(order)).append("\n");
            }
            return sb.toString();
        }

        // 返回最近的订单
        StringBuilder sb = new StringBuilder("您最近的订单记录：\n\n");

        int limit = Math.min(orders.size(), 5);
        for (int i = 0; i < limit; i++) {
            sb.append(formatOrderInfo(orders.get(i))).append("\n");
        }

        if (orders.size() > 5) {
            sb.append(String.format("\n... 还有 %d 个历史订单\n", orders.size() - 5));
        }

        return sb.toString();
    }

    private String handleUserDetectionQuestion(Long userId, String question) {
        List<Map<String, Object>> detections = getUserDetections(userId);

        if (detections.isEmpty()) {
            return "您目前没有损伤检测记录。";
        }

        StringBuilder sb = new StringBuilder(String.format("您共有 %d 条损伤检测记录：\n\n", detections.size()));

        int limit = Math.min(detections.size(), 5);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> detection = detections.get(i);
            sb.append(String.format("**检测编号：%s**\n", detection.get("detectionNo")));
            sb.append(String.format("- 无人机：%s\n", detection.get("vehicleNo")));
            sb.append(String.format("- 损伤数量：%s 处\n", detection.get("damageCount")));
            sb.append(String.format("- 严重程度：%s\n", detection.get("overallSeverity")));
            sb.append(String.format("- 责任认定：%s\n", detection.get("responsibility")));
            sb.append(String.format("- 检测时间：%s\n", detection.get("createTime")));
            sb.append("\n");
        }

        if (detections.size() > 5) {
            sb.append(String.format("... 还有 %d 条记录\n", detections.size() - 5));
        }

        return sb.toString();
    }

    private String handleRecommendationQuestion(String question) {
        List<Map<String, Object>> recommendations;

        // 场景推荐
        if (question.contains("航拍") || question.contains("摄影") || question.contains("拍照")) {
            recommendations = getRecommendationsByScenario("航拍摄影");
            return formatRecommendations(recommendations, "航拍摄影");
        }
        if (question.contains("农业") || question.contains("植保") || question.contains("喷洒")) {
            recommendations = getRecommendationsByScenario("农业植保");
            return formatRecommendations(recommendations, "农业植保");
        }
        if (question.contains("巡检") || question.contains("勘探") || question.contains("检测")) {
            recommendations = getRecommendationsByScenario("巡检勘探");
            return formatRecommendations(recommendations, "巡检勘探");
        }
        if (question.contains("新手") || question.contains("入门") || question.contains("学习")) {
            recommendations = getRecommendationsByScenario("新手入门");
            return formatRecommendations(recommendations, "新手入门");
        }

        // 默认返回可用无人机
        recommendations = getAvailableDrones().stream().limit(5).map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("brand", d.getBrand());
            map.put("model", d.getModel());
            map.put("vehicleNo", d.getVehicleNo());
            map.put("batteryLevel", d.getBatteryLevel());
            return map;
        }).collect(Collectors.toList());

        return formatRecommendations(recommendations, "推荐");
    }

    // ==================== 数据查询方法实现 ====================

    @Override
    public List<Map<String, Object>> getUserRepairs(Long userId) {
        List<DroneRepair> repairs = droneRepairMapper.selectList(
                new LambdaQueryWrapper<DroneRepair>()
                        .eq(DroneRepair::getReporterId, userId)
                        .orderByDesc(DroneRepair::getCreateTime)
        );

        return repairs.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("repairNo", r.getRepairNo());
            map.put("vehicleNo", r.getVehicleNo());
            map.put("faultType", r.getFaultType());
            map.put("faultDescription", r.getFaultDescription());
            map.put("status", getRepairStatusText(r.getStatus()));
            map.put("createTime", r.getCreateTime() != null ?
                    r.getCreateTime().format(DATE_FORMATTER) : "");
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getUserRepairStats(Long userId) {
        List<DroneRepair> repairs = droneRepairMapper.selectList(
                new LambdaQueryWrapper<DroneRepair>()
                        .eq(DroneRepair::getReporterId, userId)
        );

        int pending = 0, processing = 0, completed = 0;

        for (DroneRepair r : repairs) {
            Integer status = r.getStatus();
            if (status == null) continue;
            if (status == 0) pending++;
            else if (status == 1) processing++;
            else if (status == 2) completed++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", repairs.size());
        result.put("pending", pending);
        result.put("processing", processing);
        result.put("completed", completed);
        return result;
    }

    @Override
    public List<Map<String, Object>> getUserOrders(Long userId) {
        List<DroneOrder> orders = droneOrderMapper.selectList(
                new LambdaQueryWrapper<DroneOrder>()
                        .eq(DroneOrder::getUserId, userId)
                        .orderByDesc(DroneOrder::getCreateTime)
        );

        return orders.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("orderNo", o.getOrderNo());
            map.put("vehicleNo", o.getVehicleNo());
            map.put("startTime", o.getStartTime() != null ?
                    o.getStartTime().format(DATE_FORMATTER) : "");
            map.put("endTime", o.getEndTime() != null ?
                    o.getEndTime().format(DATE_FORMATTER) : "");
            map.put("amount", calculateOrderAmount(o));
            map.put("status", getOrderStatusText(o.getStatus()));
            map.put("createTime", o.getCreateTime() != null ?
                    o.getCreateTime().format(DATE_FORMATTER) : "");
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getUserOrderStats(Long userId) {
        List<DroneOrder> orders = droneOrderMapper.selectList(
                new LambdaQueryWrapper<DroneOrder>()
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
                totalAmount = totalAmount.add(calculateOrderAmount(o));
            }
            else if (status == 4) cancelled++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", orders.size());
        result.put("unpaid", unpaid);
        result.put("active", active);
        result.put("completed", completed);
        result.put("cancelled", cancelled);
        result.put("totalAmount", totalAmount);
        return result;
    }

    @Override
    public Map<String, Object> getUserBalance(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Collections.singletonMap("found", false);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("found", true);
        result.put("balance", user.getBalance());
        result.put("deposit", user.getDeposit());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("phone", user.getPhone());
        return result;
    }

    @Override
    public List<Map<String, Object>> getUserDetections(Long userId) {
        List<DroneDetection> detections = droneDetectionMapper.selectList(
                new LambdaQueryWrapper<DroneDetection>()
                        .eq(DroneDetection::getUserId, userId)
                        .orderByDesc(DroneDetection::getCreateTime)
        );

        return detections.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("detectionNo", d.getDetectionNo());
            map.put("vehicleNo", d.getVehicleNo());
            map.put("damageCount", d.getDamageCount());
            map.put("overallSeverity", d.getOverallSeverity());
            map.put("responsibility", d.getResponsibility());
            map.put("createTime", d.getCreateTime() != null ?
                    d.getCreateTime().format(DATE_FORMATTER) : "");
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getUserRecommendations(Long userId) {
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getRecommendationsByScenario(String scenario) {
        LambdaQueryWrapper<DroneVehicle> wrapper = new LambdaQueryWrapper<DroneVehicle>()
                .eq(DroneVehicle::getStatus, 1)
                .eq(DroneVehicle::getIsListed, 1)
                .orderByAsc(DroneVehicle::getId)
                .last("LIMIT 5");

        List<DroneVehicle> drones = list(wrapper);

        return drones.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("brand", d.getBrand());
            map.put("model", d.getModel());
            map.put("vehicleNo", d.getVehicleNo());
            map.put("batteryLevel", d.getBatteryLevel());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRecommendationsByBudget(BigDecimal budget) {
        List<DroneVehicle> drones = lambdaQuery()
                .eq(DroneVehicle::getStatus, 1)
                .eq(DroneVehicle::getIsListed, 1)
                .orderByAsc(DroneVehicle::getId)
                .list();

        return drones.stream().limit(5).map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("brand", d.getBrand());
            map.put("model", d.getModel());
            map.put("vehicleNo", d.getVehicleNo());
            map.put("batteryLevel", d.getBatteryLevel());
            return map;
        }).collect(Collectors.toList());
    }

    // ==================== 问题处理方法 ====================

    private String handlePriceQuestion(String question) {
        // 获取全局价格配置
        BigDecimal hourlyPrice = getConfigValue("rental.price.per.hour", new BigDecimal("20.00"));
        BigDecimal dailyPrice = hourlyPrice.multiply(new BigDecimal(8)); // 按8小时计算日租金

        StringBuilder sb = new StringBuilder("当前租赁价格：\n\n");
        sb.append(String.format("- 时租金：¥%s/小时\n", hourlyPrice));
        sb.append(String.format("- 日租金：¥%s/天（按8小时计算）\n", dailyPrice));

        // 尝试查找特定无人机
        String[] keywords = question.split("[\\s，。？！、]+");
        for (String kw : keywords) {
            if (kw.isEmpty()) continue;

            List<DroneVehicle> drones = lambdaQuery()
                    .like(DroneVehicle::getVehicleNo, kw.toUpperCase())
                    .or()
                    .like(DroneVehicle::getModel, kw)
                    .list();

            if (!drones.isEmpty()) {
                sb.append("\n符合条件的无人机：\n\n");
                for (DroneVehicle d : drones) {
                    sb.append(String.format("- **%s %s** (%s)\n",
                            d.getBrand() != null ? d.getBrand() : "",
                            d.getModel() != null ? d.getModel() : "",
                            d.getVehicleNo()));
                    sb.append(String.format("  状态：%s\n", getStatusText(d.getStatus())));
                    sb.append(String.format("  电量：%d%%\n", d.getBatteryLevel() != null ? d.getBatteryLevel() : 0));
                }
                break;
            }
        }

        return sb.toString();
    }

    private String handleLocationQuestion(String question) {
        // 尝试查找特定无人机
        String[] keywords = question.split("[\\s，。？！、]+");
        for (String kw : keywords) {
            if (kw.isEmpty()) continue;

            if (kw.matches(".*[A-Za-z]\\d+.*")) {
                Map<String, Object> location = getDroneLocationByNo(kw.toUpperCase());
                if ((Boolean) location.get("found")) {
                    return formatLocationResult(location);
                }
            }

            List<DroneVehicle> drones = lambdaQuery()
                    .like(DroneVehicle::getModel, kw)
                    .list();
            if (!drones.isEmpty()) {
                DroneVehicle d = drones.get(0);
                Map<String, Object> location = getDroneLocation(d.getId());
                return formatLocationResult(location);
            }
        }

        // 返回所有可用无人机的位置
        List<DroneVehicle> available = getAvailableDrones();
        if (available.isEmpty()) {
            return "目前没有可租赁的无人机。";
        }

        StringBuilder sb = new StringBuilder("当前可租赁无人机的位置信息：\n\n");
        for (DroneVehicle d : available) {
            sb.append(String.format("- **%s %s** (%s)\n",
                    d.getBrand() != null ? d.getBrand() : "",
                    d.getModel() != null ? d.getModel() : "",
                    d.getVehicleNo()));
            if (d.getLocationDetail() != null && !d.getLocationDetail().isEmpty()) {
                sb.append(String.format("  位置：%s\n", d.getLocationDetail()));
            } else if (d.getLatitude() != null && d.getLongitude() != null) {
                sb.append(String.format("  坐标：(%.6f, %.6f)\n", d.getLatitude(), d.getLongitude()));
            } else {
                sb.append("  位置：未设置\n");
            }
            sb.append(String.format("  电量：%d%%\n", d.getBatteryLevel() != null ? d.getBatteryLevel() : 0));
        }
        return sb.toString();
    }

    private String handleAvailableQuestion(String question) {
        List<DroneVehicle> available = getAvailableDrones();

        if (available.isEmpty()) {
            return "目前没有可租赁的无人机。";
        }

        BigDecimal hourlyPrice = getConfigValue("rental.price.per.hour", new BigDecimal("20.00"));

        StringBuilder sb = new StringBuilder(String.format("目前共有 **%d** 架无人机可租赁：\n\n", available.size()));

        for (DroneVehicle d : available) {
            sb.append(String.format("- **%s %s** (%s)\n",
                    d.getBrand() != null ? d.getBrand() : "",
                    d.getModel() != null ? d.getModel() : "",
                    d.getVehicleNo()));
            sb.append(String.format("  价格：¥%s/小时\n", hourlyPrice));
            sb.append(String.format("  电量：%d%%\n", d.getBatteryLevel() != null ? d.getBatteryLevel() : 0));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String handleStatusQuestion(String question) {
        Map<String, Object> stats = getStatusStatistics();

        StringBuilder sb = new StringBuilder("当前无人机状态统计：\n\n");
        sb.append(String.format("- 可租赁：%d 架\n", stats.get("available")));
        sb.append(String.format("- 租赁中：%d 架\n", stats.get("rented")));
        sb.append(String.format("- 维护中：%d 架\n", stats.get("maintenance")));
        sb.append(String.format("- 充电中：%d 架\n", stats.get("charging")));
        sb.append(String.format("- **总计：%d 架**\n", stats.get("total")));

        return sb.toString();
    }

    private String handleModelQuestion(String question) {
        Map<String, Object> stats = getModelStatistics();

        @SuppressWarnings("unchecked")
        Map<String, Integer> modelCount = (Map<String, Integer>) stats.get("modelCount");
        @SuppressWarnings("unchecked")
        Map<String, Integer> modelAvailable = (Map<String, Integer>) stats.get("modelAvailable");

        StringBuilder sb = new StringBuilder(String.format("当前共有 **%d** 种无人机型号：\n\n", stats.get("totalModels")));

        for (Map.Entry<String, Integer> entry : modelCount.entrySet()) {
            String model = entry.getKey();
            int total = entry.getValue();
            int available = modelAvailable.getOrDefault(model, 0);

            sb.append(String.format("- **%s**：共 %d 架，可租赁 %d 架\n", model, total, available));
        }

        return sb.toString();
    }

    // ==================== 辅助方法 ====================

    private String formatLocationResult(Map<String, Object> location) {
        if (!(Boolean) location.get("found")) {
            return "未找到该无人机。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**%s %s** (%s)\n",
                location.get("brand"), location.get("model"), location.get("vehicleNo")));
        sb.append(String.format("当前状态：%s\n", location.get("status")));

        Object loc = location.get("locationDetail");
        if (loc != null && !((String) loc).isEmpty()) {
            sb.append(String.format("当前位置：%s\n", loc));
        } else {
            Object lat = location.get("latitude");
            Object lon = location.get("longitude");
            if (lat != null && lon != null) {
                sb.append(String.format("当前位置坐标：(%.6f, %.6f)\n", lat, lon));
            } else {
                sb.append("当前位置：未设置\n");
            }
        }

        sb.append(String.format("当前电量：%s%%\n", location.get("batteryLevel")));

        Object isCharging = location.get("isCharging");
        if (isCharging != null && (Boolean) isCharging) {
            sb.append("状态：正在充电中\n");
        }

        return sb.toString();
    }

    private String formatOrderInfo(Map<String, Object> order) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**订单号：%s**\n", order.get("orderNo")));
        sb.append(String.format("- 无人机：%s\n", order.get("vehicleNo")));
        sb.append(String.format("- 开始时间：%s\n", order.get("startTime")));
        sb.append(String.format("- 结束时间：%s\n", order.get("endTime")));
        sb.append(String.format("- 金额：¥%s\n", order.get("amount")));
        sb.append(String.format("- 状态：%s\n", order.get("status")));
        return sb.toString();
    }

    private String formatRecommendations(List<Map<String, Object>> recommendations, String title) {
        if (recommendations.isEmpty()) {
            return "暂无相关推荐。";
        }

        BigDecimal hourlyPrice = getConfigValue("rental.price.per.hour", new BigDecimal("20.00"));

        StringBuilder sb = new StringBuilder(String.format("根据您的需求（%s），为您推荐以下无人机：\n\n", title));

        for (int i = 0; i < recommendations.size(); i++) {
            Map<String, Object> drone = recommendations.get(i);
            sb.append(String.format("%d. **%s %s** (%s)\n", i + 1,
                    drone.get("brand"), drone.get("model"), drone.get("vehicleNo")));
            sb.append(String.format("   价格：¥%s/小时\n", hourlyPrice));
            sb.append(String.format("   电量：%d%%\n\n", drone.get("batteryLevel")));
        }

        return sb.toString();
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

    private String getRepairStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待处理";
            case 1: return "处理中";
            case 2: return "已完成";
            default: return "未知";
        }
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

    private BigDecimal calculateOrderAmount(DroneOrder order) {
        if (order.getAmount() != null) {
            return order.getAmount();
        }

        // 根据时长计算金额
        if (order.getStartTime() != null && order.getEndTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(order.getStartTime(), order.getEndTime());
            BigDecimal hourlyPrice = getConfigValue("rental.price.per.hour", new BigDecimal("20.00"));
            BigDecimal perMinutePrice = getConfigValue("rental.price.per.minute", new BigDecimal("0.50"));
            return perMinutePrice.multiply(new BigDecimal(minutes));
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal getConfigValue(String key, BigDecimal defaultValue) {
        try {
            DroneSystemConfig config = configMapper.selectOne(
                    new LambdaQueryWrapper<DroneSystemConfig>()
                            .eq(DroneSystemConfig::getConfigKey, key)
            );
            if (config != null && config.getConfigValue() != null) {
                return new BigDecimal(config.getConfigValue());
            }
        } catch (Exception e) {
            log.debug("获取配置失败: {}", key);
        }
        return defaultValue;
    }
}
