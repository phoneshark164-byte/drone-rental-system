package com.drone.rental.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.rental.dao.entity.DroneOrder;
import com.drone.rental.dao.entity.DroneRepair;
import com.drone.rental.dao.entity.DroneVehicle;
import com.drone.rental.dao.entity.SysOperator;
import com.drone.rental.service.service.DroneOrderService;
import com.drone.rental.service.service.DroneRepairService;
import com.drone.rental.service.service.DroneVehicleService;
import com.drone.rental.service.service.SysOperatorService;
import com.drone.rental.common.constant.RedisKey;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运营方控制器
 */
@Controller
public class OperatorController {

    @Autowired
    private SysOperatorService operatorService;

    @Autowired
    private DroneVehicleService vehicleService;

    @Autowired
    private DroneOrderService orderService;

    @Autowired
    private DroneRepairService repairService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 登录页面
     */
    @GetMapping("/operator/login")
    public String loginPage() {
        return "operator/login";
    }

    /**
     * 登录处理
     */
    @PostMapping("/operator/doLogin")
    public String login(@RequestParam String username,
                       @RequestParam String password,
                       @RequestParam(required = false) String captchaKey,
                       @RequestParam(required = false) String captchaCode,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        try {
            // 验证验证码
            if (!verifyCaptcha(captchaKey, captchaCode, session)) {
                redirectAttributes.addFlashAttribute("error", "验证码错误或已过期");
                return "redirect:/operator/login";
            }

            SysOperator operator = operatorService.login(username, password);
            session.setAttribute("operator", operator);
            return "redirect:/operator/index";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/operator/login";
        }
    }

    /**
     * 验证验证码
     */
    private boolean verifyCaptcha(String captchaKey, String userInputCode, HttpSession session) {
        if (captchaKey == null || userInputCode == null || userInputCode.trim().isEmpty()) {
            return false;
        }

        String storedCode;
        if (stringRedisTemplate != null) {
            String redisKey = RedisKey.CAPTCHA_CODE + captchaKey;
            storedCode = stringRedisTemplate.opsForValue().get(redisKey);
            // 验证后删除验证码（一次性使用）
            if (storedCode != null) {
                stringRedisTemplate.delete(redisKey);
            }
        } else {
            // Redis不可用时使用session
            String sessionKey = (String) session.getAttribute("captchaKey");
            if (!captchaKey.equals(sessionKey)) {
                return false;
            }
            storedCode = (String) session.getAttribute("captchaCode");
            session.removeAttribute("captchaCode");
            session.removeAttribute("captchaKey");
        }

        return storedCode != null && storedCode.equals(userInputCode.toLowerCase().trim());
    }

    /**
     * 运营后台首页
     */
    @GetMapping("/operator/index")
    public String index(HttpSession session, Model model) {
        SysOperator operator = (SysOperator) session.getAttribute("operator");
        model.addAttribute("operator", operator);

        // 统计数据
        model.addAttribute("vehicleCount", vehicleService.count());
        model.addAttribute("orderCount", orderService.count());
        model.addAttribute("repairCount", repairService.count());

        return "operator/index";
    }

    /**
     * 无人机管理
     */
    @GetMapping("/operator/vehicles")
    public String vehicles(@RequestParam(defaultValue = "1") Long page,
                          @RequestParam(required = false) String keyword,
                          Model model) {
        IPage<DroneVehicle> vehiclePage = vehicleService.selectPage(new Page<>(page, 10), keyword, null);
        model.addAttribute("vehicles", vehiclePage.getRecords());
        model.addAttribute("total", vehiclePage.getTotal());
        model.addAttribute("current", vehiclePage.getCurrent());
        model.addAttribute("pages", vehiclePage.getPages());
        model.addAttribute("keyword", keyword);
        return "operator/vehicles";
    }

    /**
     * 订单管理
     */
    @GetMapping("/operator/orders")
    public String orders(@RequestParam(defaultValue = "1") Long page,
                        Model model) {
        IPage<DroneOrder> orderPage = orderService.selectPage(new Page<>(page, 10), null, null);
        model.addAttribute("orders", orderPage.getRecords());
        model.addAttribute("total", orderPage.getTotal());
        model.addAttribute("current", orderPage.getCurrent());
        model.addAttribute("pages", orderPage.getPages());
        return "operator/orders";
    }

    /**
     * 报修管理
     */
    @GetMapping("/operator/repairs")
    public String repairs(@RequestParam(defaultValue = "1") Long page,
                         Model model) {
        IPage<DroneRepair> repairPage = repairService.page(new Page<>(page, 10));
        model.addAttribute("repairs", repairPage.getRecords());
        model.addAttribute("total", repairPage.getTotal());
        model.addAttribute("current", repairPage.getCurrent());
        model.addAttribute("pages", repairPage.getPages());
        return "operator/repairs";
    }

    /**
     * 订单详情
     */
    @GetMapping("/operator/order/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        DroneOrder order = orderService.getById(id);
        model.addAttribute("order", order);
        if (order != null && order.getVehicleId() != null) {
            model.addAttribute("vehicle", vehicleService.getById(order.getVehicleId()));
        }
        return "operator/order-detail";
    }

    /**
     * 退出登录
     */
    @PostMapping("/operator/logout")
    public String logout(HttpSession session) {
        // 销毁session，彻底清除所有登录状态，防止会话重用
        session.invalidate();
        return "redirect:/operator/login";
    }

    // ==================== API接口 ====================

    /**
     * API - 运营方登录
     */
    @PostMapping("/operator/api/login")
    @ResponseBody
    public ApiResult<Map<String, Object>> loginApi(@RequestBody Map<String, String> params,
                                                    HttpSession session) {
        try {
            String username = params.get("username");
            String password = params.get("password");
            String captchaKey = params.get("captchaKey");
            String captchaCode = params.get("captchaCode");

            // 验证验证码
            if (!verifyCaptcha(captchaKey, captchaCode, session)) {
                return ApiResult.fail("验证码错误或已过期");
            }

            SysOperator operator = operatorService.login(username, password);
            session.setAttribute("operator", operator);

            Map<String, Object> result = new HashMap<>();
            result.put("id", operator.getId());
            result.put("username", operator.getUsername());
            result.put("operatorName", operator.getOperatorName());

            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * API - 运营方退出
     */
    @PostMapping("/operator/api/logout")
    @ResponseBody
    public ApiResult<Void> logoutApi(HttpSession session) {
        // 销毁session，彻底清除所有登录状态，防止会话重用
        session.invalidate();
        return ApiResult.success();
    }

    /**
     * 获取当前登录的运营方信息
     */
    @GetMapping("/operator/api/me")
    @ResponseBody
    public ApiResult<Map<String, Object>> getCurrentOperator(HttpSession session) {
        SysOperator operator = (SysOperator) session.getAttribute("operator");
        if (operator == null) {
            return ApiResult.unauthorized();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", operator.getId());
        result.put("username", operator.getUsername());
        result.put("operatorName", operator.getOperatorName());

        return ApiResult.success(result);
    }

    /**
     * 获取无人机列表（用于地图显示，只返回已上架的）
     */
    @GetMapping("/operator/api/vehicles")
    @ResponseBody
    public ApiResult<List<DroneVehicle>> getVehicles(HttpSession session) {
        // 返回所有无人机（不仅限于已上架的），运营方需要管理所有无人机
        List<DroneVehicle> vehicles = vehicleService.list();
        return ApiResult.success(vehicles);
    }

    /**
     * 获取无人机地图数据（只返回已上架且有坐标的）
     */
    @GetMapping("/operator/api/vehicles/map")
    @ResponseBody
    public ApiResult<List<Map<String, Object>>> getVehiclesForMap(HttpSession session) {
        List<DroneVehicle> vehicles = vehicleService.lambdaQuery()
                .isNotNull(DroneVehicle::getLatitude)
                .isNotNull(DroneVehicle::getLongitude)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (DroneVehicle v : vehicles) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getId());
            map.put("vehicleNo", v.getVehicleNo());
            map.put("brand", v.getBrand());
            map.put("model", v.getModel());
            map.put("batteryLevel", v.getBatteryLevel());
            map.put("status", v.getStatus());
            map.put("lat", v.getLatitude());
            map.put("lng", v.getLongitude());
            map.put("location", v.getLocationDetail());
            map.put("imageUrl", v.getImageUrl());
            // 添加充电相关数据
            map.put("chargingStartTime", v.getChargingStartTime());
            map.put("startBatteryLevel", v.getStartBatteryLevel());
            result.add(map);
        }
        return ApiResult.success(result);
    }

    /**
     * 获取无人机详情
     */
    @GetMapping("/operator/api/vehicle/{id}")
    @ResponseBody
    public ApiResult<DroneVehicle> getVehicle(@PathVariable Long id) {
        return ApiResult.success(vehicleService.getById(id));
    }

    /**
     * 添加无人机
     */
    @PostMapping("/operator/api/vehicle")
    @ResponseBody
    public ApiResult<Void> createVehicle(@RequestBody Map<String, Object> params, HttpSession session) {
        try {
            SysOperator operator = (SysOperator) session.getAttribute("operator");

            DroneVehicle vehicle = new DroneVehicle();
            vehicle.setVehicleNo((String) params.get("vehicleNo"));
            vehicle.setBrand((String) params.get("brand"));
            vehicle.setModel((String) params.get("model"));
            vehicle.setStatus(Integer.valueOf(params.get("status").toString()));
            vehicle.setBatteryLevel(Integer.valueOf(params.get("batteryLevel").toString()));
            vehicle.setLocationDetail((String) params.get("locationDetail"));
            vehicle.setImageUrl((String) params.get("imageUrl"));

            // 设置坐标
            Object lat = params.get("latitude");
            Object lng = params.get("longitude");
            if (lat != null && lng != null) {
                vehicle.setLatitude(new BigDecimal(lat.toString()));
                vehicle.setLongitude(new BigDecimal(lng.toString()));
            }

            // 设置飞行时长
            Object flightHours = params.get("flightHours");
            if (flightHours != null) {
                vehicle.setFlightHours(new BigDecimal(flightHours.toString()));
            }

            // 关联运营方
            if (operator != null) {
                vehicle.setOperatorId(operator.getId());
            }

            vehicle.setIsListed(1); // 默认上架

            vehicleService.save(vehicle);
            return ApiResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 更新无人机
     */
    @PutMapping("/operator/api/vehicle/{id}")
    @ResponseBody
    public ApiResult<Void> updateVehicle(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            DroneVehicle vehicle = vehicleService.getById(id);
            if (vehicle == null) {
                return ApiResult.fail("无人机不存在");
            }

            if (params.containsKey("brand")) {
                vehicle.setBrand((String) params.get("brand"));
            }
            if (params.containsKey("model")) {
                vehicle.setModel((String) params.get("model"));
            }
            if (params.containsKey("status")) {
                vehicle.setStatus(Integer.valueOf(params.get("status").toString()));
            }
            if (params.containsKey("batteryLevel")) {
                vehicle.setBatteryLevel(Integer.valueOf(params.get("batteryLevel").toString()));
            }
            if (params.containsKey("locationDetail")) {
                vehicle.setLocationDetail((String) params.get("locationDetail"));
            }
            if (params.containsKey("imageUrl")) {
                vehicle.setImageUrl((String) params.get("imageUrl"));
            }

            vehicleService.updateById(vehicle);
            return ApiResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 修改无人机状态
     */
    @PostMapping("/operator/api/vehicle/{id}/status")
    @ResponseBody
    public ApiResult<Void> updateVehicleStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            DroneVehicle vehicle = vehicleService.getById(id);
            if (vehicle == null) {
                return ApiResult.fail("无人机不存在");
            }

            // 如果状态变为充电中，记录充电开始时间
            if (status == 3 && vehicle.getStatus() != 3) {
                vehicle.setChargingStartTime(LocalDateTime.now());
                vehicle.setStartBatteryLevel(vehicle.getBatteryLevel());
            }
            // 如果从充电状态变为其他状态，计算并保存当前充到的电量
            else if (vehicle.getStatus() == 3 && status != 3) {
                // 计算充电进度，更新实际电量
                if (vehicle.getChargingStartTime() != null && vehicle.getStartBatteryLevel() != null) {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime startTime = vehicle.getChargingStartTime();

                    // 计算已充电时间（分钟）
                    long minutesCharged = java.time.Duration.between(startTime, now).toMinutes();

                    // 假设2小时（120分钟）从起始电量充到100%
                    int startBattery = vehicle.getStartBatteryLevel();
                    int batteryNeeded = 100 - startBattery;
                    long totalMinutesNeeded = (long) ((batteryNeeded / 100.0) * 120);

                    // 计算当前电量
                    int currentBattery = startBattery;
                    if (totalMinutesNeeded > 0) {
                        double progress = Math.min(1.0, (double) minutesCharged / totalMinutesNeeded);
                        currentBattery = startBattery + (int) ((100 - startBattery) * progress);
                    }
                    currentBattery = Math.min(100, currentBattery);

                    // 更新实际电量
                    vehicle.setBatteryLevel(currentBattery);
                }

                // 清除充电相关数据
                vehicle.setChargingStartTime(null);
                vehicle.setStartBatteryLevel(null);
            }

            vehicle.setStatus(status);
            vehicleService.updateById(vehicle);
            return ApiResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 提交报修
     */
    @PostMapping("/operator/api/repair")
    @ResponseBody
    public ApiResult<Void> createRepair(@RequestBody Map<String, Object> params, HttpSession session) {
        try {
            SysOperator operator = (SysOperator) session.getAttribute("operator");

            DroneRepair repair = new DroneRepair();
            // 生成报修单号：BX + 年月日 + 4位随机数
            String repairNo = "BX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                             (int)(Math.random() * 10000);
            repair.setRepairNo(repairNo);

            repair.setVehicleId(Long.valueOf(params.get("vehicleId").toString()));
            repair.setVehicleNo((String) params.get("vehicleNo"));
            repair.setFaultType((String) params.get("faultType"));
            repair.setFaultDescription((String) params.get("faultDescription"));
            repair.setReporterPhone((String) params.get("reporterPhone"));

            if (operator != null) {
                repair.setReporterId(operator.getId());
                repair.setReporterType("OPERATOR");
                repair.setReporterName(operator.getOperatorName());
            }

            repair.setStatus(0); // 待处理

            repairService.save(repair);

            // 同时将无人机状态设为维修中
            vehicleService.updateStatus(repair.getVehicleId(), 4);

            return ApiResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 开始处理报修
     */
    @PostMapping("/operator/api/repair/{id}/start")
    @ResponseBody
    public ApiResult<Void> startRepair(@PathVariable Long id, HttpSession session) {
        try {
            SysOperator operator = (SysOperator) session.getAttribute("operator");

            DroneRepair repair = repairService.getById(id);
            if (repair == null) {
                return ApiResult.fail("报修记录不存在");
            }

            repair.setStatus(1); // 处理中
            repair.setHandlerId(operator.getId());
            repair.setHandlerName(operator.getOperatorName());
            repair.setHandleTime(LocalDateTime.now());
            repairService.updateById(repair);

            return ApiResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 完成维修
     */
    @PostMapping("/operator/api/repair/{id}/complete")
    @ResponseBody
    public ApiResult<Void> completeRepair(@PathVariable Long id, @RequestParam String handleResult) {
        try {
            DroneRepair repair = repairService.getById(id);
            if (repair == null) {
                return ApiResult.fail("报修记录不存在");
            }

            repair.setStatus(2); // 已完成
            repair.setHandleResult(handleResult);
            repairService.updateById(repair);

            // 同时将无人机状态改为可用
            vehicleService.updateStatus(repair.getVehicleId(), 1);

            return ApiResult.success();
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 获取仪表板统计数据
     */
    @GetMapping("/operator/api/dashboard/stats")
    @ResponseBody
    public ApiResult<Map<String, Object>> getDashboardStats(HttpSession session) {
        Map<String, Object> stats = new HashMap<>();

        // 获取当前登录的运营方
        SysOperator operator = (SysOperator) session.getAttribute("operator");

        if (operator != null) {
            // 只统计该运营方管理的无人机
            long vehicleCount = vehicleService.lambdaQuery()
                    .eq(DroneVehicle::getOperatorId, operator.getId())
                    .count();
            stats.put("vehicleCount", vehicleCount);

            // 统计该运营方相关的订单（通过其管理的无人机）
            List<Long> vehicleIds = vehicleService.lambdaQuery()
                    .eq(DroneVehicle::getOperatorId, operator.getId())
                    .list()
                    .stream()
                    .map(DroneVehicle::getId)
                    .collect(Collectors.toList());

            long orderCount = 0;
            if (!vehicleIds.isEmpty()) {
                orderCount = orderService.lambdaQuery()
                        .in(DroneOrder::getVehicleId, vehicleIds)
                        .count();
            }
            stats.put("orderCount", orderCount);

            // 统计该运营方提交的报修
            long repairCount = repairService.lambdaQuery()
                    .eq(DroneRepair::getReporterId, operator.getId())
                    .count();
            stats.put("repairCount", repairCount);
        } else {
            // 如果没有登录运营方，返回0
            stats.put("vehicleCount", 0);
            stats.put("orderCount", 0);
            stats.put("repairCount", 0);
        }

        return ApiResult.success(stats);
    }

    /**
     * 获取订单列表
     */
    @GetMapping("/operator/api/order/list")
    @ResponseBody
    public ApiResult<List<Map<String, Object>>> getOrderList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpSession session) {
        SysOperator operator = (SysOperator) session.getAttribute("operator");

        if (operator != null) {
            // 获取该运营方管理的无人机ID列表
            List<Long> vehicleIds = vehicleService.lambdaQuery()
                    .eq(DroneVehicle::getOperatorId, operator.getId())
                    .list()
                    .stream()
                    .map(DroneVehicle::getId)
                    .collect(Collectors.toList());

            if (!vehicleIds.isEmpty()) {
                var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneOrder>()
                        .in(DroneOrder::getVehicleId, vehicleIds);

                if (status != null) {
                    queryWrapper.eq(DroneOrder::getStatus, status);
                }

                queryWrapper.orderByDesc(DroneOrder::getCreateTime);
                List<DroneOrder> orders = orderService.list(queryWrapper);

                // 转换为前端需要的格式
                List<Map<String, Object>> result = new ArrayList<>();
                for (DroneOrder order : orders) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", order.getId());
                    map.put("orderNo", order.getOrderNo());
                    map.put("userId", order.getUserId());
                    map.put("vehicleId", order.getVehicleId());
                    map.put("droneNo", order.getVehicleNo());
                    map.put("startTime", order.getStartTime());
                    map.put("endTime", order.getEndTime());
                    map.put("duration", order.getPlannedDuration());
                    map.put("amount", order.getAmount() != null ? order.getAmount() : "0.00");
                    map.put("status", order.getStatus());

                    // 简化处理：用户名显示为"用户ID"
                    map.put("username", "用户" + order.getUserId());

                    // 获取无人机型号
                    DroneVehicle vehicle = vehicleService.getById(order.getVehicleId());
                    if (vehicle != null) {
                        map.put("droneModel", vehicle.getBrand() + " " + vehicle.getModel());
                    } else {
                        map.put("droneModel", "未知型号");
                    }

                    // 状态文本
                    String statusText = switch (order.getStatus()) {
                        case 0 -> "待支付";
                        case 1 -> "已支付";
                        case 2 -> "使用中";
                        case 3 -> "已完成";
                        case 4 -> "已取消";
                        default -> "未知";
                    };
                    map.put("statusText", statusText);

                    result.add(map);
                }
                return ApiResult.success(result);
            }
        }

        return ApiResult.success(new ArrayList<>());
    }

    /**
     * 获取报修列表
     */
    @GetMapping("/operator/api/repair/list")
    @ResponseBody
    public ApiResult<List<Map<String, Object>>> getRepairList(HttpSession session) {
        SysOperator operator = (SysOperator) session.getAttribute("operator");

        if (operator != null) {
            List<DroneRepair> repairs = repairService.lambdaQuery()
                    .orderByDesc(DroneRepair::getCreateTime)
                    .list();

            // 转换为前端需要的格式
            List<Map<String, Object>> result = new ArrayList<>();
            for (DroneRepair repair : repairs) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", repair.getId());
                map.put("repairNo", repair.getRepairNo());
                map.put("droneNo", repair.getVehicleNo());
                map.put("vehicleId", repair.getVehicleId());
                map.put("faultType", repair.getFaultType());
                map.put("faultDescription", repair.getFaultDescription());
                map.put("reporterId", repair.getReporterId());
                map.put("reporterName", repair.getReporterName());
                map.put("reporterType", repair.getReporterType());
                map.put("reporterPhone", repair.getReporterPhone());
                map.put("status", repair.getStatus());
                map.put("handlerId", repair.getHandlerId());
                map.put("handlerName", repair.getHandlerName());
                map.put("handleTime", repair.getHandleTime());
                map.put("handleResult", repair.getHandleResult());
                map.put("createTime", repair.getCreateTime());

                // 获取无人机详细信息
                DroneVehicle vehicle = vehicleService.getById(repair.getVehicleId());
                if (vehicle != null) {
                    map.put("brand", vehicle.getBrand());
                    map.put("model", vehicle.getModel());
                    map.put("imageUrl", vehicle.getImageUrl());
                } else {
                    map.put("brand", "未知");
                    map.put("model", "未知");
                    map.put("imageUrl", "/img/train/0001.jpg");
                }

                result.add(map);
            }
            return ApiResult.success(result);
        }

        return ApiResult.success(new ArrayList<>());
    }
}
