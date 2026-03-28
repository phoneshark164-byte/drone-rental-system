package com.drone.rental.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.rental.dao.entity.DroneOrder;
import com.drone.rental.dao.entity.DroneVehicle;
import com.drone.rental.dao.entity.DroneBanner;
import com.drone.rental.dao.entity.DroneRepair;
import com.drone.rental.dao.entity.DroneDetection;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.dao.entity.SysNotification;
import com.drone.rental.service.service.DroneOrderService;
import com.drone.rental.service.service.DroneVehicleService;
import com.drone.rental.service.service.DroneBannerService;
import com.drone.rental.service.service.DroneRepairService;
import com.drone.rental.service.service.DroneDetectionService;
import com.drone.rental.service.service.SysUserService;
import com.drone.rental.service.service.SysNotificationService;
import com.drone.rental.service.service.UserRechargeRecordService;
import com.drone.rental.service.service.UserPaymentRecordService;
import com.drone.rental.service.service.OrderReviewService;
import com.drone.rental.dao.entity.UserRechargeRecord;
import com.drone.rental.dao.entity.UserPaymentRecord;
import com.drone.rental.dao.entity.OrderReview;
import com.drone.rental.common.constant.RedisKey;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Controller
public class UserController {

    @Autowired
    private DroneVehicleService vehicleService;

    @Autowired
    private DroneOrderService orderService;

    @Autowired
    private SysUserService userService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DroneBannerService bannerService;

    @Autowired
    private DroneRepairService repairService;

    @Autowired
    private DroneDetectionService detectionService;

    @Autowired
    private UserRechargeRecordService rechargeRecordService;

    @Autowired
    private UserPaymentRecordService paymentRecordService;

    @Autowired
    private SysNotificationService notificationService;

    @Autowired
    private OrderReviewService reviewService;

    /**
     * 登录页面
     */
    @GetMapping("/user/login")
    public String loginPage() {
        return "user/login";
    }

    /**
     * 注册页面
     */
    @GetMapping("/user/register")
    public String registerPage() {
        return "user/register";
    }

    /**
     * 用户首页
     */
    @GetMapping({"/user/index", "/user"})
    public String index(HttpSession session, Model model) {
        SysUser user = (SysUser) session.getAttribute("user");
        model.addAttribute("user", user);
        // 如果已登录，查询附近可用无人机
        if (user != null) {
            IPage<DroneVehicle> page = vehicleService.selectNearbyAvailable(1L, 10L, 39.0842, 117.2009, 5.0);
            model.addAttribute("vehicles", page.getRecords());
        }
        return "user/index";
    }

    /**
     * 无人机详情
     */
    @GetMapping("/user/vehicle/{id}")
    public String vehicleDetail(@PathVariable Long id, Model model, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login";
        }
        DroneVehicle vehicle = vehicleService.getById(id);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("user", user);
        return "user/vehicle-detail";
    }

    /**
     * 我的订单
     */
    @GetMapping("/user/orders")
    public String myOrders(@RequestParam(defaultValue = "1") Long page,
                           @RequestParam(required = false) Integer status,
                           HttpSession session, Model model) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login";
        }
        IPage<DroneOrder> orderPage = orderService.selectUserPage(new Page<>(page, 10), user.getId(), status);
        model.addAttribute("orders", orderPage.getRecords());
        model.addAttribute("total", orderPage.getTotal());
        model.addAttribute("current", orderPage.getCurrent());
        model.addAttribute("pages", orderPage.getPages());
        model.addAttribute("status", status);
        model.addAttribute("user", user);
        return "user/orders";
    }

    /**
     * 订单详情
     */
    @GetMapping("/user/order/{id}")
    public String orderDetail(@PathVariable Long id, Model model, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login";
        }
        DroneOrder order = orderService.getById(id);
        model.addAttribute("order", order);
        model.addAttribute("vehicle", vehicleService.getById(order.getVehicleId()));
        model.addAttribute("user", user);
        return "user/order-detail";
    }

    /**
     * 个人中心
     */
    @GetMapping("/user/profile")
    public String profile(HttpSession session, Model model) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login";
        }
        // 从数据库重新查询用户以获取完整信息（包括注册时间）
        SysUser fullUser = userService.getById(user.getId());
        model.addAttribute("user", fullUser);
        return "user/profile";
    }

    // ==================== API接口 ====================

    /**
     * 创建订单
     */
    @PostMapping("/user/api/order/create")
    @ResponseBody
    public ApiResult<DroneOrder> createOrder(@RequestParam Long vehicleId,
                                          @RequestParam Integer plannedDuration,
                                          HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }
        try {
            DroneOrder order = orderService.createOrder(user.getId(), vehicleId, plannedDuration,
                    39.0842, 117.2009, "天津理工大学");
            return ApiResult.success(order);
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 支付订单
     */
    @PostMapping("/user/api/order/{id}/pay")
    @ResponseBody
    public ApiResult<Void> payOrder(@PathVariable Long id) {
        try {
            orderService.payOrder(id);
            return ApiResult.success();
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 开始使用
     */
    @PostMapping("/user/api/order/{id}/start")
    @ResponseBody
    public ApiResult<Void> startUse(@PathVariable Long id) {
        try {
            orderService.startUse(id);
            return ApiResult.success();
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 结束使用
     */
    @PostMapping("/user/api/order/{id}/end")
    @ResponseBody
    public ApiResult<Void> endUse(@PathVariable Long id,
                                   @RequestParam(required = false) Double latitude,
                                   @RequestParam(required = false) Double longitude,
                                   @RequestParam(required = false) String location) {
        try {
            orderService.endUse(id,
                    latitude != null ? latitude : 39.0842,
                    longitude != null ? longitude : 117.2009,
                    location != null ? location : "天津理工大学");
            return ApiResult.success();
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/user/api/order/{id}/cancel")
    @ResponseBody
    public ApiResult<Void> cancelOrder(@PathVariable Long id, @RequestParam String reason) {
        try {
            orderService.cancelOrder(id, reason);
            return ApiResult.success();
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/user/api/profile")
    @ResponseBody
    public ApiResult<Void> updateProfile(@RequestParam String realName,
                                         @RequestParam String phone,
                                         HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }
        user.setRealName(realName);
        user.setPhone(phone);
        userService.updateById(user);
        session.setAttribute("user", user);
        return ApiResult.success();
    }

    /**
     * 更新用户头像
     */
    @PostMapping("/user/api/profile/avatar")
    @ResponseBody
    public ApiResult<Void> updateAvatar(@RequestBody java.util.Map<String, String> body, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }
        String avatar = body.get("avatar");
        if (avatar != null) {
            user.setAvatar(avatar);
            userService.updateById(user);
            session.setAttribute("user", user);
        }
        return ApiResult.success();
    }

    /**
     * 缴纳押金
     */
    @PostMapping("/user/api/deposit/pay")
    @ResponseBody
    public ApiResult<Void> payDeposit(@RequestBody java.util.Map<String, Object> body, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        Double amount = ((Number) body.get("amount")).doubleValue();

        // 检查余额是否足够
        if (user.getBalance() == null || user.getBalance().compareTo(java.math.BigDecimal.valueOf(amount)) < 0) {
            return ApiResult.fail("余额不足");
        }

        // 扣除余额
        user.setBalance(user.getBalance().subtract(java.math.BigDecimal.valueOf(amount)));
        // 设置押金
        user.setDeposit(java.math.BigDecimal.valueOf(amount));

        userService.updateById(user);
        session.setAttribute("user", user);

        // 发送通知给管理员
        SysNotification notification = new SysNotification();
        notification.setTitle("用户缴纳押金");
        notification.setContent(String.format("用户 %s 刚刚缴纳了 ¥%.2f 押金", user.getUsername(), amount));
        notification.setType("success");
        notification.setReceiverType("admin");
        notification.setRelatedType("deposit");
        notification.setRelatedId(user.getId());
        notificationService.save(notification);

        return ApiResult.success();
    }

    /**
     * 获取用户订单统计
     */
    @GetMapping("/user/api/order/stats")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getOrderStats(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", orderService.lambdaQuery().eq(DroneOrder::getUserId, user.getId()).count());
        stats.put("completed", orderService.lambdaQuery().eq(DroneOrder::getUserId, user.getId())
                .eq(DroneOrder::getStatus, 3).count());
        stats.put("cancelled", orderService.lambdaQuery().eq(DroneOrder::getUserId, user.getId())
                .eq(DroneOrder::getStatus, 4).count());

        // 计算总飞行小时数（仅统计已完成的订单）
        java.util.List<DroneOrder> completedOrders = orderService.lambdaQuery()
                .eq(DroneOrder::getUserId, user.getId())
                .eq(DroneOrder::getStatus, 3)
                .list();
        int totalMinutes = 0;
        for (DroneOrder order : completedOrders) {
            if (order.getActualDuration() != null) {
                totalMinutes += order.getActualDuration();
            }
        }
        // 转换为小时，保留1位小数
        double totalHours = Math.round(totalMinutes / 60.0 * 10.0) / 10.0;
        stats.put("totalHours", totalHours);

        return ApiResult.success(stats);
    }

    /**
     * 获取最近订单
     */
    @GetMapping("/user/api/order/recent")
    @ResponseBody
    public ApiResult<java.util.List<DroneOrder>> getRecentOrders(
            @RequestParam(defaultValue = "5") Integer limit,
            HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }
        java.util.List<DroneOrder> orders = orderService.lambdaQuery()
                .eq(DroneOrder::getUserId, user.getId())
                .orderByDesc(DroneOrder::getCreateTime)
                .last("LIMIT " + limit)
                .list();
        return ApiResult.success(orders);
    }

    /**
     * 获取订单列表
     */
    @GetMapping("/user/api/order/list")
    @ResponseBody
    public ApiResult<java.util.List<java.util.Map<String, Object>>> getOrderList(
            @RequestParam(required = false) Integer status,
            HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneOrder>()
                .eq(DroneOrder::getUserId, user.getId());

        if (status != null) {
            queryWrapper.eq(DroneOrder::getStatus, status);
        }

        queryWrapper.orderByDesc(DroneOrder::getCreateTime);

        java.util.List<DroneOrder> orders = orderService.list(queryWrapper);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        for (DroneOrder order : orders) {
            DroneVehicle vehicle = vehicleService.getById(order.getVehicleId());
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("vehicleId", order.getVehicleId());
            map.put("droneNo", vehicle != null ? vehicle.getVehicleNo() : "");
            map.put("droneModel", vehicle != null ? vehicle.getBrand() + " " + vehicle.getModel() : "无人机");
            map.put("droneImage", vehicle != null ? vehicle.getImageUrl() : "/img/train/0001.jpg");
            map.put("startTime", order.getStartTime());
            map.put("plannedDuration", order.getPlannedDuration());
            map.put("actualDuration", order.getActualDuration());
            map.put("amount", order.getAmount());
            map.put("status", order.getStatus());
            map.put("createTime", order.getCreateTime());
            map.put("endTime", order.getEndTime());
            result.add(map);
        }

        return ApiResult.success(result);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/user/api/order/{id}")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getOrderDetail(@PathVariable Long id, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        DroneOrder order = orderService.getById(id);
        if (order == null) {
            return ApiResult.fail("订单不存在");
        }
        if (!order.getUserId().equals(user.getId())) {
            return ApiResult.fail("无权访问此订单");
        }

        DroneVehicle vehicle = vehicleService.getById(order.getVehicleId());
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("vehicleId", order.getVehicleId());
        map.put("droneNo", vehicle != null ? vehicle.getVehicleNo() : "");
        map.put("droneModel", vehicle != null ? vehicle.getBrand() + " " + vehicle.getModel() : "无人机");
        map.put("droneImage", vehicle != null ? vehicle.getImageUrl() : "/img/train/0001.jpg");
        map.put("startTime", order.getStartTime());
        map.put("endTime", order.getEndTime());
        map.put("plannedDuration", order.getPlannedDuration());
        map.put("actualDuration", order.getActualDuration());
        map.put("amount", order.getAmount());
        map.put("status", order.getStatus());
        map.put("createTime", order.getCreateTime());
        map.put("startLocation", order.getStartLocation());
        map.put("endLocation", order.getEndLocation());

        return ApiResult.success(map);
    }

    /**
     * 获取所有无人机位置信息（用于地图展示）
     */
    @GetMapping("/user/api/vehicles/map")
    @ResponseBody
    public ApiResult<java.util.List<java.util.Map<String, Object>>> getVehiclesForMap() {
        java.util.List<DroneVehicle> vehicles = vehicleService.lambdaQuery()
                .eq(DroneVehicle::getIsListed, 1)
                .isNotNull(DroneVehicle::getLatitude)
                .isNotNull(DroneVehicle::getLongitude)
                .list();

        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (DroneVehicle v : vehicles) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
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
            // 充电相关数据
            map.put("chargingStartTime", v.getChargingStartTime());
            map.put("startBatteryLevel", v.getStartBatteryLevel());
            result.add(map);
        }
        return ApiResult.success(result);
    }

    /**
     * 修改密码
     */
    @PostMapping("/user/api/password/change")
    @ResponseBody
    public ApiResult<Void> changePassword(@RequestBody java.util.Map<String, String> params,
                                          HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        // 验证旧密码
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            return ApiResult.fail("原密码错误");
        }

        // 更新新密码
        user.setPassword(PasswordUtil.encode(newPassword));
        userService.updateById(user);

        // 销毁session，强制用户重新登录
        session.invalidate();

        return ApiResult.success();
    }

    /**
     * 获取无人机详情
     */
    @GetMapping("/user/api/vehicle/{id}")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getVehicleDetail(@PathVariable Long id) {
        DroneVehicle vehicle = vehicleService.getById(id);
        if (vehicle == null) {
            return ApiResult.fail("无人机不存在");
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", vehicle.getId());
        result.put("droneNo", vehicle.getVehicleNo());
        result.put("vehicleNo", vehicle.getVehicleNo());
        result.put("brand", vehicle.getBrand());
        result.put("model", vehicle.getModel());
        result.put("color", vehicle.getColor());
        result.put("imageUrl", vehicle.getImageUrl());
        result.put("batteryLevel", vehicle.getBatteryLevel());
        result.put("latitude", vehicle.getLatitude());
        result.put("longitude", vehicle.getLongitude());
        result.put("locationDetail", vehicle.getLocationDetail());
        result.put("status", vehicle.getStatus());
        result.put("isListed", vehicle.getIsListed());
        result.put("flightHours", vehicle.getFlightHours() != null ? vehicle.getFlightHours() : 0);
        result.put("maxSpeed", 75);
        result.put("camera", "4K");
        result.put("pricePerMinute", 0.5);
        // 充电相关数据
        result.put("chargingStartTime", vehicle.getChargingStartTime());
        result.put("startBatteryLevel", vehicle.getStartBatteryLevel());

        return ApiResult.success(result);
    }

    /**
     * 获取无人机列表（只返回已上架的）
     */
    @GetMapping("/user/api/vehicles/list")
    @ResponseBody
    public ApiResult<java.util.List<java.util.Map<String, Object>>> getVehicleList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String brand) {

        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneVehicle>()
                .eq(DroneVehicle::getIsListed, 1); // 只返回已上架的

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(DroneVehicle::getBrand, keyword)
                    .or()
                    .like(DroneVehicle::getModel, keyword)
                    .or()
                    .like(DroneVehicle::getVehicleNo, keyword));
        }

        if (status != null) {
            queryWrapper.eq(DroneVehicle::getStatus, status);
        }

        if (brand != null && !brand.isEmpty()) {
            queryWrapper.eq(DroneVehicle::getBrand, brand);
        }

        java.util.List<DroneVehicle> vehicles = vehicleService.list(queryWrapper);

        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (DroneVehicle v : vehicles) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", v.getId());
            map.put("droneNo", v.getVehicleNo());
            map.put("brand", v.getBrand());
            map.put("model", v.getModel());
            map.put("imageUrl", v.getImageUrl());
            map.put("locationDetail", v.getLocationDetail());
            map.put("batteryLevel", v.getBatteryLevel());
            map.put("status", v.getStatus());
            // 默认值（实体类中没有这些字段）
            map.put("maxSpeed", 75);
            map.put("camera", "4K");
            map.put("pricePerMinute", 0.5);
            map.put("flightHours", v.getFlightHours() != null ? v.getFlightHours() : 0);
            map.put("chargingStartTime", v.getChargingStartTime());
            map.put("chargingDuration", null);
            map.put("startBatteryLevel", v.getStartBatteryLevel());
            result.add(map);
        }
        return ApiResult.success(result);
    }

    /**
     * 获取启用的轮播图列表
     */
    @GetMapping("/user/api/banner/list")
    @ResponseBody
    public ApiResult<java.util.List<DroneBanner>> getActiveBanners() {
        return ApiResult.success(bannerService.getActiveBanners());
    }

    /**
     * 用户提交报修
     */
    @PostMapping("/user/api/repair/create")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> createRepair(@RequestBody java.util.Map<String, Object> params,
                                                                  HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        try {
            Long vehicleId = Long.valueOf(params.get("vehicleId").toString());
            DroneVehicle vehicle = vehicleService.getById(vehicleId);
            if (vehicle == null) {
                return ApiResult.fail("无人机不存在");
            }

            DroneRepair repair = new DroneRepair();
            // 生成报修单号
            String repairNo = "BX" + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                    (int)(Math.random() * 10000);
            repair.setRepairNo(repairNo);
            repair.setVehicleId(vehicleId);
            repair.setVehicleNo(vehicle.getVehicleNo());
            repair.setFaultType((String) params.get("faultType"));
            repair.setFaultDescription((String) params.get("faultDescription"));
            repair.setReporterId(user.getId());
            repair.setReporterType("USER");
            repair.setReporterName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            repair.setReporterPhone(user.getPhone());
            repair.setStatus(0); // 待处理

            repairService.save(repair);

            // 将无人机状态设为维修中
            vehicleService.updateStatus(vehicleId, 4);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("repairNo", repairNo);
            result.put("id", repair.getId());
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 获取我的报修列表
     */
    @GetMapping("/user/api/repair/my")
    @ResponseBody
    public ApiResult<java.util.List<java.util.Map<String, Object>>> getMyRepairs(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        java.util.List<DroneRepair> repairs = repairService.lambdaQuery()
                .eq(DroneRepair::getReporterId, user.getId())
                .orderByDesc(DroneRepair::getCreateTime)
                .list();

        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (DroneRepair repair : repairs) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", repair.getId());
            map.put("repairNo", repair.getRepairNo());
            map.put("vehicleId", repair.getVehicleId());
            map.put("droneNo", repair.getVehicleNo());
            map.put("faultType", repair.getFaultType());
            map.put("faultDescription", repair.getFaultDescription());
            map.put("status", repair.getStatus());
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

    /**
     * 获取报修详情
     */
    @GetMapping("/user/api/repair/{id}")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getRepairDetail(@PathVariable Long id, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        DroneRepair repair = repairService.getById(id);
        if (repair == null) {
            return ApiResult.fail("报修记录不存在");
        }
        if (!repair.getReporterId().equals(user.getId())) {
            return ApiResult.fail("无权访问此报修记录");
        }

        DroneVehicle vehicle = vehicleService.getById(repair.getVehicleId());
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", repair.getId());
        map.put("repairNo", repair.getRepairNo());
        map.put("vehicleId", repair.getVehicleId());
        map.put("droneNo", repair.getVehicleNo());
        map.put("faultType", repair.getFaultType());
        map.put("faultDescription", repair.getFaultDescription());
        map.put("status", repair.getStatus());
        map.put("handlerId", repair.getHandlerId());
        map.put("handlerName", repair.getHandlerName());
        map.put("handleTime", repair.getHandleTime());
        map.put("handleResult", repair.getHandleResult());
        map.put("createTime", repair.getCreateTime());
        map.put("updateTime", repair.getUpdateTime());

        if (vehicle != null) {
            map.put("brand", vehicle.getBrand());
            map.put("model", vehicle.getModel());
            map.put("imageUrl", vehicle.getImageUrl());
        } else {
            map.put("brand", "未知");
            map.put("model", "未知");
            map.put("imageUrl", "/img/train/0001.jpg");
        }

        return ApiResult.success(map);
    }

    /**
     * AI 损伤检测 - 获取检测服务状态
     */
    @GetMapping("/user/api/detect/status")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getDetectionStatus() {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:5000/api/health"))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> result = mapper.readValue(response.body(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});

                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("available", true);
                data.put("service", result.get("service"));
                data.put("model_loaded", result.get("model_loaded"));

                return ApiResult.success(data);
            } else {
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("available", false);
                data.put("message", "检测服务未启动");
                return ApiResult.success(data);
            }
        } catch (Exception e) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("available", false);
            data.put("message", "检测服务未启动");
            return ApiResult.success(data);
        }
    }

    /**
     * AI 损伤检测 - 图片检测
     */
    @PostMapping("/user/api/detect/image")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> detectImageDamage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long orderId,
            HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        try {
            // 调用 Python 检测服务
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:5000/api/detect/image"))
                    .timeout(java.time.Duration.ofSeconds(30));

            // 构建multipart请求
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            requestBuilder.header("Content-Type", "multipart/form-data; boundary=" + boundary);

            byte[] fileBytes = file.getBytes();
            java.io.ByteArrayOutputStream fullBody = new java.io.ByteArrayOutputStream();

            // 添加文件部分
            fullBody.write(("--" + boundary + "\r\n").getBytes());
            fullBody.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" +
                    file.getOriginalFilename() + "\"\r\n").getBytes());
            fullBody.write(("Content-Type: " + file.getContentType() + "\r\n\r\n").getBytes());
            fullBody.write(fileBytes);
            fullBody.write("\r\n".getBytes());

            // 添加 vehicle_id 参数
            if (vehicleId != null) {
                fullBody.write(("--" + boundary + "\r\n").getBytes());
                fullBody.write("Content-Disposition: form-data; name=\"vehicle_id\"\r\n\r\n".getBytes());
                fullBody.write(vehicleId.toString().getBytes());
                fullBody.write("\r\n".getBytes());
            }

            // 添加 order_id 参数
            if (orderId != null) {
                fullBody.write(("--" + boundary + "\r\n").getBytes());
                fullBody.write("Content-Disposition: form-data; name=\"order_id\"\r\n\r\n".getBytes());
                fullBody.write(orderId.toString().getBytes());
                fullBody.write("\r\n".getBytes());
            }

            // 结束边界
            fullBody.write(("--" + boundary + "--\r\n").getBytes());

            java.net.http.HttpRequest request = requestBuilder
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(fullBody.toByteArray()))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 解析JSON响应
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> result = mapper.readValue(response.body(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});

                int code = (Integer) result.get("code");
                if (code == 200) {
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) result.get("data");

                    // 根据检测结果自动创建报修记录
                    java.util.Map<String, Object> damageAnalysis = (java.util.Map<String, Object>) data.get("damage_analysis");
                    String responsibility = (String) damageAnalysis.get("responsibility");
                    Integer damageCount = (Integer) damageAnalysis.get("damage_count");

                    DroneRepair repair = null;

                    // 如果检测到用户责任损伤且已选择关联无人机，自动创建报修记录
                    if (damageCount > 0 && responsibility.equals("user") && vehicleId != null) {
                        repair = new DroneRepair();
                        String repairNo = "BX" + java.time.LocalDateTime.now().format(
                                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                                (int)(Math.random() * 10000);
                        repair.setRepairNo(repairNo);
                        repair.setVehicleId(vehicleId);

                        DroneVehicle vehicle = vehicleService.getById(vehicleId);
                        if (vehicle != null) {
                            repair.setVehicleNo(vehicle.getVehicleNo());
                        }

                        repair.setReporterId(user.getId());
                        repair.setReporterType("USER");
                        repair.setReporterName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                        repair.setReporterPhone(user.getPhone());

                        // 设置故障类型和描述
                        @SuppressWarnings("unchecked")
                        java.util.List<java.util.Map<String, Object>> detectedDamages =
                                (java.util.List<java.util.Map<String, Object>>) damageAnalysis.get("detected_damages");
                        if (detectedDamages != null && !detectedDamages.isEmpty()) {
                            repair.setFaultType((String) detectedDamages.get(0).get("type"));
                            repair.setFaultDescription("AI检测到 " + damageCount + " 处损伤: " +
                                    damageAnalysis.get("responsibility_reason"));
                        }

                        repair.setStatus(0); // 待处理
                        repairService.save(repair);

                        // 将无人机状态设为维修中
                        vehicleService.updateStatus(vehicleId, 4);

                        data.put("auto_repair_id", repair.getId());
                        data.put("auto_repair_no", repairNo);
                    }

                    // 保存检测记录
                    DroneDetection detection = new DroneDetection();
                    String detectionNo = "DET" + java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                            (int)(Math.random() * 10000);
                    detection.setDetectionNo(detectionNo);
                    detection.setUserId(user.getId());
                    detection.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                    detection.setVehicleId(vehicleId);
                    if (vehicleId != null) {
                        DroneVehicle vehicle = vehicleService.getById(vehicleId);
                        if (vehicle != null) {
                            detection.setVehicleNo(vehicle.getVehicleNo());
                        }
                    }
                    detection.setOrderId(orderId);
                    detection.setImageUrl((String) data.get("original_image"));
                    detection.setResultImageUrl((String) data.get("image_path"));
                    detection.setDamageCount(damageCount);
                    detection.setOverallSeverity((String) damageAnalysis.get("overall_severity"));
                    detection.setResponsibility(responsibility);
                    detection.setResponsibilityReason((String) damageAnalysis.get("responsibility_reason"));
                    detection.setInferenceTime(new java.math.BigDecimal((String) data.get("inference_time")));
                    detection.setModelName((String) data.get("model_name"));
                    if (repair != null && repair.getId() != null) {
                        detection.setAutoRepairId(repair.getId());
                    }
                    detectionService.save(detection);

                    return ApiResult.success(data);
                } else {
                    return ApiResult.fail((String) result.get("message"));
                }
            } else {
                return ApiResult.fail("检测服务异常: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.fail("检测失败: " + e.getMessage());
        }
    }

    /**
     * AI 损伤检测 - 多图片批量检测
     */
    @PostMapping("/user/api/detect/images")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> detectMultipleImages(
            @RequestParam("files") org.springframework.web.multipart.MultipartFile[] files,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long orderId,
            HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        // 验证文件数量
        if (files == null || files.length == 0) {
            return ApiResult.fail("没有上传文件");
        }
        if (files.length > 6) {
            return ApiResult.fail("最多支持上传6张图片");
        }

        try {
            // 调用 Python 检测服务
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();

            // 构建multipart请求体
            java.io.ByteArrayOutputStream fullBody = new java.io.ByteArrayOutputStream();

            // 添加每个文件
            for (org.springframework.web.multipart.MultipartFile file : files) {
                if (file.isEmpty()) continue;

                byte[] fileBytes = file.getBytes();

                // 添加文件部分
                fullBody.write(("--" + boundary + "\r\n").getBytes());
                fullBody.write(("Content-Disposition: form-data; name=\"files\"; filename=\"" +
                        file.getOriginalFilename() + "\"\r\n").getBytes());
                fullBody.write(("Content-Type: " + file.getContentType() + "\r\n\r\n").getBytes());
                fullBody.write(fileBytes);
                fullBody.write("\r\n".getBytes());
            }

            // 添加 vehicle_id 参数
            if (vehicleId != null) {
                fullBody.write(("--" + boundary + "\r\n").getBytes());
                fullBody.write("Content-Disposition: form-data; name=\"vehicle_id\"\r\n\r\n".getBytes());
                fullBody.write(vehicleId.toString().getBytes());
                fullBody.write("\r\n".getBytes());
            }

            // 添加 order_id 参数
            if (orderId != null) {
                fullBody.write(("--" + boundary + "\r\n").getBytes());
                fullBody.write("Content-Disposition: form-data; name=\"order_id\"\r\n\r\n".getBytes());
                fullBody.write(orderId.toString().getBytes());
                fullBody.write("\r\n".getBytes());
            }

            // 结束边界
            fullBody.write(("--" + boundary + "--\r\n").getBytes());

            // 发送请求
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:5000/api/detect/images"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(java.time.Duration.ofSeconds(60))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(fullBody.toByteArray()))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> result = mapper.readValue(response.body(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});

                int code = (Integer) result.get("code");
                if (code == 200) {
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) result.get("data");

                    // 保存检测记录
                    java.util.Map<String, Object> damageAnalysis = (java.util.Map<String, Object>) data.get("damage_analysis");
                    String responsibility = (String) damageAnalysis.get("responsibility");
                    Integer damageCount = (Integer) damageAnalysis.get("damage_count");

                    DroneRepair repair = null;

                    // 如果检测到用户责任损伤且已选择关联无人机，自动创建报修记录
                    if (damageCount > 0 && responsibility.equals("user") && vehicleId != null) {
                        repair = new DroneRepair();
                        String repairNo = "BX" + java.time.LocalDateTime.now().format(
                                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                                (int)(Math.random() * 10000);
                        repair.setRepairNo(repairNo);
                        repair.setVehicleId(vehicleId);

                        DroneVehicle vehicle = vehicleService.getById(vehicleId);
                        if (vehicle != null) {
                            repair.setVehicleNo(vehicle.getVehicleNo());
                        }

                        repair.setReporterId(user.getId());
                        repair.setReporterType("USER");
                        repair.setReporterName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                        repair.setReporterPhone(user.getPhone());

                        @SuppressWarnings("unchecked")
                        java.util.List<java.util.Map<String, Object>> detectedDamages =
                                (java.util.List<java.util.Map<String, Object>>) data.get("total_detections");
                        if (detectedDamages != null && !detectedDamages.isEmpty()) {
                            repair.setFaultType((String) detectedDamages.get(0).get("type"));
                            repair.setFaultDescription("AI多角度检测到 " + damageCount + " 处损伤: " +
                                    damageAnalysis.get("responsibility_reason"));
                        }

                        repair.setStatus(0);
                        repairService.save(repair);

                        vehicleService.updateStatus(vehicleId, 4);

                        data.put("auto_repair_id", repair.getId());
                        data.put("auto_repair_no", repairNo);
                    }

                    // 保存检测记录（保存第一张图片作为主图，所有图片路径存储）
                    DroneDetection detection = new DroneDetection();
                    String detectionNo = "DET" + java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                            (int)(Math.random() * 10000);
                    detection.setDetectionNo(detectionNo);
                    detection.setUserId(user.getId());
                    detection.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                    detection.setVehicleId(vehicleId);
                    if (vehicleId != null) {
                        DroneVehicle vehicle = vehicleService.getById(vehicleId);
                        if (vehicle != null) {
                            detection.setVehicleNo(vehicle.getVehicleNo());
                        }
                    }
                    detection.setOrderId(orderId);

                    @SuppressWarnings("unchecked")
                    java.util.List<java.util.Map<String, Object>> imageResults =
                            (java.util.List<java.util.Map<String, Object>>) data.get("image_results");
                    if (imageResults != null && !imageResults.isEmpty()) {
                        detection.setImageUrl((String) imageResults.get(0).get("image_path"));
                        detection.setResultImageUrl((String) imageResults.get(0).get("result_path"));
                    }

                    detection.setDamageCount(damageCount);
                    detection.setOverallSeverity((String) damageAnalysis.get("overall_severity"));
                    detection.setResponsibility(responsibility);
                    detection.setResponsibilityReason((String) damageAnalysis.get("responsibility_reason"));
                    detection.setInferenceTime(new java.math.BigDecimal(
                            String.valueOf(data.get("total_inference_time"))));
                    detection.setModelName((String) data.get("model_name"));
                    if (repair != null && repair.getId() != null) {
                        detection.setAutoRepairId(repair.getId());
                    }
                    detectionService.save(detection);

                    return ApiResult.success(data);
                } else {
                    return ApiResult.fail((String) result.get("message"));
                }
            } else {
                return ApiResult.fail("检测服务异常: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.fail("检测失败: " + e.getMessage());
        }
    }

    /**
     * JSON API - 用户登录
     */
    @PostMapping("/user/api/login")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> loginApi(@RequestBody java.util.Map<String, String> params,
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

            SysUser user = userService.login(username, password);
            session.setAttribute("user", user);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", user.getId());
            result.put("username", user.getUsername());
            result.put("realName", user.getRealName());
            result.put("phone", user.getPhone());
            result.put("avatar", user.getAvatar());
            result.put("balance", user.getBalance());
            result.put("deposit", user.getDeposit());

            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
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
     * JSON API - 用户注册
     */
    @PostMapping("/user/api/register")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> registerApi(@RequestBody java.util.Map<String, String> params) {
        try {
            String username = params.get("username");
            String password = params.get("password");
            String confirmPassword = params.get("confirmPassword");
            String phone = params.get("phone");
            String realName = params.get("realName");

            if (username == null || username.isEmpty()) {
                return ApiResult.fail("用户名不能为空");
            }
            if (password == null || password.isEmpty()) {
                return ApiResult.fail("密码不能为空");
            }
            if (!password.equals(confirmPassword)) {
                return ApiResult.fail("两次密码不一致");
            }

            // 注册用户
            userService.register(username, password, phone, realName);

            // 查询新注册的用户
            SysUser user = userService.getByUsername(username);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", user.getId());
            result.put("username", user.getUsername());
            result.put("realName", user.getRealName());

            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * JSON API - 用户登出
     */
    @PostMapping("/user/api/logout")
    @ResponseBody
    public ApiResult<Void> logoutApi(HttpSession session) {
        // 销毁session，彻底清除所有登录状态
        session.invalidate();
        return ApiResult.success();
    }

    /**
     * JSON API - 获取当前登录用户信息
     */
    @GetMapping("/user/api/me")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getCurrentUser(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("phone", user.getPhone());
        result.put("balance", user.getBalance() != null ? user.getBalance() : java.math.BigDecimal.ZERO);

        return ApiResult.success(result);
    }

    // ==================== 余额支付相关API ====================

    /**
     * 获取用户余额信息
     */
    @GetMapping("/user/api/balance")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getBalance(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        // 从数据库重新查询最新余额
        SysUser latestUser = userService.getById(user.getId());
        if (latestUser == null) {
            return ApiResult.fail("用户不存在");
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("balance", latestUser.getBalance() != null ? latestUser.getBalance() : java.math.BigDecimal.ZERO);
        result.put("deposit", latestUser.getDeposit() != null ? latestUser.getDeposit() : java.math.BigDecimal.ZERO);

        return ApiResult.success(result);
    }

    /**
     * 创建充值订单
     */
    @PostMapping("/user/api/recharge/create")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> createRecharge(@RequestBody java.util.Map<String, Object> params,
                                                                     HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        try {
            java.math.BigDecimal amount = new java.math.BigDecimal(params.get("amount").toString());
            String paymentMethod = (String) params.getOrDefault("paymentMethod", "BALANCE");
            String remark = (String) params.getOrDefault("remark", "");

            UserRechargeRecord record = rechargeRecordService.createRecharge(
                    user.getId(), user.getUsername(), amount, paymentMethod, remark);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("rechargeNo", record.getRechargeNo());
            result.put("amount", record.getAmount());
            result.put("status", record.getStatus());

            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 充值支付（模拟支付成功）
     */
    @PostMapping("/user/api/recharge/pay")
    @ResponseBody
    public ApiResult<Void> payRecharge(@RequestBody java.util.Map<String, String> params,
                                       HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        try {
            String rechargeNo = params.get("rechargeNo");
            rechargeRecordService.rechargeSuccess(rechargeNo);

            // 更新session中的用户信息
            SysUser latestUser = userService.getById(user.getId());
            session.setAttribute("user", latestUser);

            return ApiResult.success();
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 余额支付订单
     */
    @PostMapping("/user/api/order/{id}/pay/balance")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> payOrderByBalance(@PathVariable Long id,
                                                                       HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        try {
            DroneOrder order = orderService.getById(id);
            if (order == null) {
                return ApiResult.fail("订单不存在");
            }
            if (!order.getUserId().equals(user.getId())) {
                return ApiResult.fail("无权操作此订单");
            }
            if (order.getStatus() != 0) {
                return ApiResult.fail("订单状态异常");
            }

            // 创建支付记录
            UserPaymentRecord paymentRecord = paymentRecordService.createPayment(
                    user.getId(), user.getUsername(), order.getId(), order.getOrderNo(), order.getAmount());

            // 执行余额支付
            paymentRecordService.payOrderByBalance(paymentRecord.getPaymentNo());

            // 更新session中的用户信息
            SysUser latestUser = userService.getById(user.getId());
            session.setAttribute("user", latestUser);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("paymentNo", paymentRecord.getPaymentNo());
            result.put("balanceAfter", latestUser.getBalance() != null ? latestUser.getBalance() : java.math.BigDecimal.ZERO);

            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 获取充值记录
     */
    @GetMapping("/user/api/recharge/list")
    @ResponseBody
    public ApiResult<java.util.List<UserRechargeRecord>> getRechargeList(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        java.util.List<UserRechargeRecord> records = rechargeRecordService.lambdaQuery()
                .eq(UserRechargeRecord::getUserId, user.getId())
                .orderByDesc(UserRechargeRecord::getCreateTime)
                .list();

        return ApiResult.success(records);
    }

    /**
     * 获取支付记录
     */
    @GetMapping("/user/api/payment/list")
    @ResponseBody
    public ApiResult<java.util.List<UserPaymentRecord>> getPaymentList(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        java.util.List<UserPaymentRecord> records = paymentRecordService.lambdaQuery()
                .eq(UserPaymentRecord::getUserId, user.getId())
                .orderByDesc(UserPaymentRecord::getCreateTime)
                .list();

        return ApiResult.success(records);
    }

    // ==================== 评价相关API ====================

    /**
     * 创建评价
     */
    @PostMapping("/user/api/review/create")
    @ResponseBody
    public ApiResult<OrderReview> createReview(
            @RequestParam Long orderId,
            @RequestParam Integer rating,
            @RequestParam String content,
            @RequestParam(required = false) String images,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Integer serviceRating,
            @RequestParam(required = false) Integer valueRating,
            HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        try {
            OrderReview review = reviewService.createReview(
                    orderId, user.getId(), rating, content, images, tags, serviceRating, valueRating);
            return ApiResult.success(review);
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 检查订单是否已评价
     */
    @GetMapping("/user/api/review/check/{orderId}")
    @ResponseBody
    public ApiResult<Boolean> checkReviewed(@PathVariable Long orderId, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        boolean reviewed = reviewService.isReviewed(orderId);
        return ApiResult.success(reviewed);
    }

    /**
     * 获取订单评价
     */
    @GetMapping("/user/api/review/order/{orderId}")
    @ResponseBody
    public ApiResult<OrderReview> getReviewByOrderId(@PathVariable Long orderId, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        OrderReview review = reviewService.getByOrderId(orderId);
        return ApiResult.success(review);
    }

    /**
     * 获取我的评价列表
     */
    @GetMapping("/user/api/review/my")
    @ResponseBody
    public ApiResult<IPage<OrderReview>> getMyReviews(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size,
            HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("user");
        if (user == null) {
            return ApiResult.unauthorized();
        }

        IPage<OrderReview> pageInfo = new Page<>(page, size);
        IPage<OrderReview> result = reviewService.selectUserPage(pageInfo, user.getId());
        return ApiResult.success(result);
    }

    /**
     * 获取无人机评价列表
     */
    @GetMapping("/user/api/review/vehicle/{vehicleId}")
    @ResponseBody
    public ApiResult<IPage<OrderReview>> getVehicleReviews(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {
        IPage<OrderReview> pageInfo = new Page<>(page, size);
        IPage<OrderReview> result = reviewService.selectVehiclePage(pageInfo, vehicleId);
        return ApiResult.success(result);
    }

    /**
     * 获取无人机评价统计
     */
    @GetMapping("/user/api/review/stats/{vehicleId}")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getReviewStats(@PathVariable Long vehicleId) {
        java.util.Map<String, Object> stats = reviewService.getReviewStats(vehicleId);
        return ApiResult.success(stats);
    }

    /**
     * 点赞评价
     */
    @PostMapping("/user/api/review/{id}/like")
    @ResponseBody
    public ApiResult<Void> likeReview(@PathVariable Long id) {
        try {
            reviewService.likeReview(id);
            return ApiResult.success();
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }
}
