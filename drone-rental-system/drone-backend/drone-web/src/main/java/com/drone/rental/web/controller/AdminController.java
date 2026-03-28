package com.drone.rental.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.rental.dao.entity.SysAdmin;
import com.drone.rental.dao.entity.SysUser;
import com.drone.rental.dao.entity.DroneOrder;
import com.drone.rental.dao.entity.DroneVehicle;
import com.drone.rental.dao.entity.DroneBanner;
import com.drone.rental.dao.entity.DroneOperationLog;
import com.drone.rental.dao.entity.UserRechargeRecord;
import com.drone.rental.dao.entity.UserPaymentRecord;
import com.drone.rental.dao.entity.DroneDetection;
import com.drone.rental.dao.entity.OrderReview;
import com.drone.rental.common.result.ApiResult;
import com.drone.rental.service.service.SysUserService;
import com.drone.rental.service.service.DroneOrderService;
import com.drone.rental.service.service.DroneVehicleService;
import com.drone.rental.service.service.DroneBannerService;
import com.drone.rental.service.service.DroneSystemConfigService;
import com.drone.rental.service.service.UserRechargeRecordService;
import com.drone.rental.service.service.UserPaymentRecordService;
import com.drone.rental.service.service.DroneDetectionService;
import com.drone.rental.service.service.DroneRecommendationService;
import com.drone.rental.service.service.OrderReviewService;
import com.drone.rental.common.constant.RedisKey;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员控制器
 */
@Controller
public class AdminController {

    @Autowired
    private SysUserService userService;

    @Autowired
    private DroneVehicleService vehicleService;

    @Autowired
    private DroneOrderService orderService;

    @Autowired
    private DroneBannerService bannerService;

    @Autowired
    private com.drone.rental.service.service.DroneOperationLogService operationLogService;

    @Autowired
    private DroneSystemConfigService systemConfigService;

    @Autowired
    private UserRechargeRecordService rechargeRecordService;

    @Autowired
    private UserPaymentRecordService paymentRecordService;

    @Autowired
    private DroneDetectionService detectionService;

    @Autowired
    private DroneRecommendationService recommendationService;

    @Autowired
    private OrderReviewService reviewService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 登录页面
     */
    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }

    /**
     * 登录处理
     */
    @PostMapping("/admin/doLogin")
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
                return "redirect:/admin/login";
            }

            SysAdmin admin = adminService.login(username, password);
            session.setAttribute("admin", admin);
            return "redirect:/admin/index";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/login";
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
     * JSON API - 管理员登录
     */
    @PostMapping("/admin/api/login")
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

            SysAdmin admin = adminService.login(username, password);
            session.setAttribute("admin", admin);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", admin.getId());
            result.put("username", admin.getUsername());
            result.put("adminName", admin.getAdminName());

            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    @Autowired
    private com.drone.rental.service.service.SysAdminService adminService;

    /**
     * 管理后台首页
     */
    @GetMapping("/admin/index")
    public String index(HttpSession session, Model model) {
        SysAdmin admin = (SysAdmin) session.getAttribute("admin");
        model.addAttribute("admin", admin);

        // 统计数据
        model.addAttribute("userCount", userService.count());
        model.addAttribute("vehicleCount", vehicleService.count());
        model.addAttribute("orderCount", orderService.count());

        // 无人机状态统计（用于图表）
        model.addAttribute("vehicleAvailableCount", vehicleService.lambdaQuery()
                .eq(DroneVehicle::getStatus, 1).count());
        model.addAttribute("vehicleInUseCount", vehicleService.lambdaQuery()
                .eq(DroneVehicle::getStatus, 2).count());
        model.addAttribute("vehicleRepairCount", vehicleService.lambdaQuery()
                .eq(DroneVehicle::getStatus, 3).count());
        model.addAttribute("vehicleOfflineCount", vehicleService.lambdaQuery()
                .eq(DroneVehicle::getStatus, 0).count());

        // 最近订单
        IPage<DroneOrder> recentOrders = orderService.selectPage(new Page<>(1, 5), null, null);
        model.addAttribute("recentOrders", recentOrders.getRecords());

        return "admin/index";
    }

    /**
     * 用户管理
     */
    @GetMapping("/admin/users")
    public String users(@RequestParam(defaultValue = "1") Long page,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        IPage<SysUser> userPage = userService.selectPage(new Page<>(page, 10), keyword);
        model.addAttribute("users", userPage.getRecords());
        model.addAttribute("total", userPage.getTotal());
        model.addAttribute("current", userPage.getCurrent());
        model.addAttribute("pages", userPage.getPages());
        model.addAttribute("keyword", keyword);
        return "admin/users";
    }

    /**
     * 无人机管理
     */
    @GetMapping("/admin/vehicles")
    public String vehicles(@RequestParam(defaultValue = "1") Long page,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status,
                          Model model) {
        IPage<DroneVehicle> vehiclePage = vehicleService.selectPage(new Page<>(page, 10), keyword, status);
        model.addAttribute("vehicles", vehiclePage.getRecords());
        model.addAttribute("total", vehiclePage.getTotal());
        model.addAttribute("current", vehiclePage.getCurrent());
        model.addAttribute("pages", vehiclePage.getPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/vehicles";
    }

    /**
     * 订单管理
     */
    @GetMapping("/admin/orders")
    public String orders(@RequestParam(defaultValue = "1") Long page,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Integer status,
                        Model model) {
        IPage<DroneOrder> orderPage = orderService.selectPage(new Page<>(page, 10), keyword, status);
        model.addAttribute("orders", orderPage.getRecords());
        model.addAttribute("total", orderPage.getTotal());
        model.addAttribute("current", orderPage.getCurrent());
        model.addAttribute("pages", orderPage.getPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/orders";
    }

    /**
     * 订单详情
     */
    @GetMapping("/admin/order/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        DroneOrder order = orderService.getById(id);
        model.addAttribute("order", order);
        model.addAttribute("vehicle", vehicleService.getById(order.getVehicleId()));
        return "admin/order-detail";
    }

    /**
     * 用户状态修改（HTML页面跳转）
     */
    @PostMapping("/admin/user/{id}/status")
    public String updateUserStatusPage(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return "redirect:/admin/users";
    }

    /**
     * 无人机状态修改
     */
    @PostMapping("/admin/vehicle/{id}/status")
    public String updateVehicleStatus(@PathVariable Long id, @RequestParam Integer status) {
        vehicleService.updateStatus(id, status);
        return "redirect:/admin/vehicles";
    }

    /**
     * 退出登录
     */
    @PostMapping("/admin/logout")
    public String logout(HttpSession session) {
        // 销毁session，彻底清除所有登录状态，防止会话重用
        session.invalidate();
        return "redirect:/admin/login";
    }

    /**
     * 轮播图管理页面
     */
    @GetMapping("/admin/banners")
    public String banners(@RequestParam(defaultValue = "1") Long page,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status,
                          Model model) {
        IPage<DroneBanner> bannerPage = bannerService.selectPage(new Page<>(page, 10), keyword, status);
        model.addAttribute("banners", bannerPage.getRecords());
        model.addAttribute("total", bannerPage.getTotal());
        model.addAttribute("current", bannerPage.getCurrent());
        model.addAttribute("pages", bannerPage.getPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/banners";
    }

    /**
     * 系统设置页面
     */
    @GetMapping("/admin/settings")
    public String settings(HttpSession session, Model model) {
        model.addAttribute("admin", session.getAttribute("admin"));
        return "admin/settings";
    }

    /**
     * 操作日志页面
     */
    @GetMapping("/admin/logs")
    public String logs(@RequestParam(defaultValue = "1") Long page,
                       @RequestParam(required = false) String actionType,
                       @RequestParam(required = false) String module,
                       @RequestParam(required = false) String operator,
                       @RequestParam(required = false) String date,
                       Model model) {
        // 这里可以添加日志查询逻辑，暂时返回空列表
        model.addAttribute("logs", new java.util.ArrayList<>());
        model.addAttribute("current", page);
        model.addAttribute("pages", 1);
        model.addAttribute("actionType", actionType);
        model.addAttribute("module", module);
        model.addAttribute("operator", operator);
        return "admin/logs";
    }

    // ==================== API接口 ====================

    /**
     * 获取用户详情
     */
    @GetMapping("/admin/api/user/{id}")
    @ResponseBody
    public ApiResult<SysUser> getUser(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return ApiResult.fail("用户不存在");
        }
        return ApiResult.success(user);
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/admin/api/user/list")
    @ResponseBody
    public ApiResult<java.util.List<SysUser>> getUserList(
            @RequestParam(required = false) String keyword) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword));
        }

        queryWrapper.orderByDesc(SysUser::getCreateTime);

        java.util.List<SysUser> users = userService.list(queryWrapper);
        return ApiResult.success(users);
    }

    /**
     * 添加用户
     */
    @PostMapping("/admin/api/user")
    @ResponseBody
    public ApiResult<Void> addUser(@RequestBody SysUser user) {
        // 检查用户名是否存在
        SysUser existUser = userService.getByUsername(user.getUsername());
        if (existUser != null) {
            return ApiResult.fail("用户名已存在");
        }
        // 加密密码
        user.setPassword(PasswordUtil.encode(user.getPassword()));
        user.setStatus(user.getStatus() != null ? user.getStatus() : 1);
        user.setDeposit(user.getDeposit() != null ? user.getDeposit() : BigDecimal.ZERO);
        userService.save(user);
        return ApiResult.success();
    }

    /**
     * 更新用户
     */
    @PutMapping("/admin/api/user/{id}")
    @ResponseBody
    public ApiResult<Void> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        SysUser existUser = userService.getById(id);
        if (existUser == null) {
            return ApiResult.fail("用户不存在");
        }
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordUtil.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userService.updateById(user);
        return ApiResult.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/admin/api/user/{id}")
    @ResponseBody
    public ApiResult<Void> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return ApiResult.success();
    }

    /**
     * 修改用户状态
     */
    @PostMapping("/admin/api/user/{id}/status")
    @ResponseBody
    public ApiResult<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return ApiResult.fail("用户不存在");
        }
        user.setStatus(status);
        userService.updateById(user);
        return ApiResult.success();
    }

    /**
     * 获取无人机列表 (必须在 {id} 路由之前，否则会被 {id} 匹配)
     */
    @GetMapping("/admin/api/vehicle/list")
    @ResponseBody
    public ApiResult<java.util.List<DroneVehicle>> getVehicleList() {
        return ApiResult.success(vehicleService.list());
    }

    /**
     * 获取无人机详情
     */
    @GetMapping("/admin/api/vehicle/{id}")
    @ResponseBody
    public ApiResult<DroneVehicle> getVehicle(@PathVariable Long id) {
        return ApiResult.success(vehicleService.getById(id));
    }

    /**
     * 添加无人机
     */
    @PostMapping("/admin/api/vehicle")
    @ResponseBody
    public ApiResult<Void> addVehicle(@RequestBody DroneVehicle vehicle) {
        vehicle.setStatus(vehicle.getStatus() != null ? vehicle.getStatus() : 1);
        vehicle.setBatteryLevel(vehicle.getBatteryLevel() != null ? vehicle.getBatteryLevel() : 100);
        vehicle.setIsListed(vehicle.getIsListed() != null ? vehicle.getIsListed() : 1); // 默认上架

        // 如果提供了位置详情，自动获取经纬度
        String locationDetail = vehicle.getLocationDetail();
        if (locationDetail != null && !locationDetail.isEmpty()) {
            double[] coords = AmapGeocodingUtil.geocode(locationDetail);
            if (coords != null) {
                vehicle.setLongitude(java.math.BigDecimal.valueOf(coords[0]));
                vehicle.setLatitude(java.math.BigDecimal.valueOf(coords[1]));
            } else {
                return ApiResult.fail("地理编码失败，无法获取该地址的经纬度，请检查地址是否正确");
            }
        }

        vehicleService.save(vehicle);
        return ApiResult.success();
    }

    /**
     * 更新无人机
     */
    @PutMapping("/admin/api/vehicle/{id}")
    @ResponseBody
    public ApiResult<Void> updateVehicle(@PathVariable Long id, @RequestBody DroneVehicle vehicle) {
        // 获取原无人机信息
        DroneVehicle existingVehicle = vehicleService.getById(id);
        if (existingVehicle == null) {
            return ApiResult.fail("无人机不存在");
        }

        vehicle.setId(id);

        // 如果位置详情发生变化，自动获取经纬度
        String newLocationDetail = vehicle.getLocationDetail();
        if (newLocationDetail != null && !newLocationDetail.isEmpty()) {
            // 检查位置详情是否发生变化
            if (!newLocationDetail.equals(existingVehicle.getLocationDetail())) {
                // 调用高德地图地理编码获取经纬度
                double[] coords = AmapGeocodingUtil.geocode(newLocationDetail);
                if (coords != null) {
                    vehicle.setLongitude(java.math.BigDecimal.valueOf(coords[0]));
                    vehicle.setLatitude(java.math.BigDecimal.valueOf(coords[1]));
                } else {
                    return ApiResult.fail("地理编码失败：无法获取地址\"" + newLocationDetail + "\"的经纬度，请检查地址格式是否正确（如：四川省成都市武侯区）");
                }
            }
        }

        vehicleService.updateById(vehicle);
        return ApiResult.success();
    }

    /**
     * 删除无人机
     */
    @DeleteMapping("/admin/api/vehicle/{id}")
    @ResponseBody
    public ApiResult<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.removeById(id);
        return ApiResult.success();
    }

    /**
     * 获取订单统计数据（近7天）
     */
    @GetMapping("/admin/api/order/stats")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getOrderStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        java.util.List<Integer> last7Days = new java.util.ArrayList<>();
        java.util.List<String> dayLabels = new java.util.ArrayList<>();
        java.util.List<String> weekDays = java.util.Arrays.asList("周日", "周一", "周二", "周三", "周四", "周五", "周六");

        // 获取近7天的订单统计
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.now().minusDays(i);
            java.time.LocalDateTime startOfDay = dateTime.toLocalDate().atStartOfDay();
            java.time.LocalDateTime endOfDay = dateTime.toLocalDate().atTime(23, 59, 59);

            long count = orderService.lambdaQuery()
                    .ge(DroneOrder::getCreateTime, startOfDay)
                    .le(DroneOrder::getCreateTime, endOfDay)
                    .count();

            last7Days.add((int) count);
            dayLabels.add(weekDays.get(dateTime.getDayOfWeek().getValue() % 7));
        }

        stats.put("data", last7Days);
        stats.put("labels", dayLabels);
        return ApiResult.success(stats);
    }

    /**
     * 获取订单列表
     */
    @GetMapping("/admin/api/order/list")
    @ResponseBody
    public ApiResult<java.util.List<java.util.Map<String, Object>>> getOrderList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneOrder> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        // 按订单号搜索
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(DroneOrder::getOrderNo, keyword);
        }

        // 按状态筛选
        if (status != null) {
            queryWrapper.eq(DroneOrder::getStatus, status);
        }

        queryWrapper.orderByDesc(DroneOrder::getCreateTime);

        java.util.List<DroneOrder> orders = orderService.list(queryWrapper);

        // 转换为包含用户名和无人机信息的列表
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (DroneOrder order : orders) {
            java.util.Map<String, Object> orderMap = new java.util.HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("orderNo", order.getOrderNo());
            orderMap.put("userId", order.getUserId());
            orderMap.put("vehicleId", order.getVehicleId());
            orderMap.put("vehicleNo", order.getVehicleNo());
            orderMap.put("startTime", order.getStartTime());
            orderMap.put("endTime", order.getEndTime());
            orderMap.put("plannedDuration", order.getPlannedDuration());
            orderMap.put("actualDuration", order.getActualDuration());
            orderMap.put("amount", order.getAmount());
            orderMap.put("status", order.getStatus());
            orderMap.put("payTime", order.getPayTime());
            orderMap.put("createTime", order.getCreateTime());

            // 获取用户名
            SysUser user = userService.getById(order.getUserId());
            orderMap.put("username", user != null ? user.getUsername() : "未知用户");

            // 获取无人机型号
            DroneVehicle vehicle = vehicleService.getById(order.getVehicleId());
            orderMap.put("droneModel", vehicle != null ? vehicle.getModel() : "未知无人机");
            orderMap.put("droneBrand", vehicle != null ? vehicle.getBrand() : "");

            result.add(orderMap);
        }

        return ApiResult.success(result);
    }

    /**
     * 获取仪表板统计数据
     */
    @GetMapping("/admin/api/dashboard/stats")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getDashboardStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        // 基础统计
        stats.put("userCount", userService.count());
        stats.put("vehicleCount", vehicleService.count());
        stats.put("orderCount", orderService.count());

        // 无人机状态统计
        long availableCount = vehicleService.lambdaQuery().eq(DroneVehicle::getStatus, 1).count();
        long inUseCount = vehicleService.lambdaQuery().eq(DroneVehicle::getStatus, 2).count();
        long chargingCount = vehicleService.lambdaQuery().eq(DroneVehicle::getStatus, 3).count();
        long faultCount = vehicleService.lambdaQuery().eq(DroneVehicle::getStatus, 0).count();
        long maintenanceCount = vehicleService.lambdaQuery().eq(DroneVehicle::getStatus, 4).count();

        // 本月收入统计（已完成订单）
        java.time.LocalDateTime startOfMonth = java.time.LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        java.math.BigDecimal monthlyIncome = orderService.lambdaQuery()
                .eq(DroneOrder::getStatus, 3)
                .ge(DroneOrder::getCreateTime, startOfMonth)
                .list()
                .stream()
                .map(DroneOrder::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        stats.put("monthlyIncome", monthlyIncome != null ? monthlyIncome : java.math.BigDecimal.ZERO);
        stats.put("vehicleAvailable", availableCount);
        stats.put("vehicleInUse", inUseCount);
        stats.put("vehicleCharging", chargingCount);
        stats.put("vehicleFault", faultCount);
        stats.put("vehicleMaintenance", maintenanceCount);

        return ApiResult.success(stats);
    }

    /**
     * 获取所有无人机位置信息（用于地图展示）
     */
    @GetMapping("/admin/api/vehicles/map")
    @ResponseBody
    public ApiResult<java.util.List<java.util.Map<String, Object>>> getVehiclesForMap() {
        java.util.List<DroneVehicle> vehicles = vehicleService.lambdaQuery()
                .eq(DroneVehicle::getIsListed, 1) // 只返回已上架的无人机
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
            result.add(map);
        }
        return ApiResult.success(result);
    }

    /**
     * 测试地理编码
     */
    @GetMapping("/admin/api/geocode/test")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> testGeocode(@RequestParam String address) {
        double[] coords = AmapGeocodingUtil.geocode(address);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (coords != null) {
            result.put("address", address);
            result.put("longitude", coords[0]);
            result.put("latitude", coords[1]);
            return ApiResult.success(result);
        } else {
            result.put("address", address);
            result.put("error", "地理编码失败");
            return ApiResult.fail("无法获取该地址的经纬度");
        }
    }

    /**
     * 获取最近订单列表
     */
    @GetMapping("/admin/api/orders/recent")
    @ResponseBody
    public ApiResult<java.util.List<java.util.Map<String, Object>>> getRecentOrders(
            @RequestParam(defaultValue = "5") Integer limit) {
        IPage<DroneOrder> orders = orderService.selectPage(new Page<>(1, limit), null, null);

        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (DroneOrder order : orders.getRecords()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("amount", order.getAmount());
            map.put("status", order.getStatus());
            map.put("createTime", order.getCreateTime());
            DroneVehicle vehicle = vehicleService.getById(order.getVehicleId());
            map.put("vehicleNo", vehicle != null ? vehicle.getVehicleNo() : "未知");
            SysUser user = userService.getById(order.getUserId());
            map.put("userName", user != null ? user.getRealName() : user != null ? user.getUsername() : "未知");
            result.add(map);
        }
        return ApiResult.success(result);
    }

    // ==================== 操作日志API接口 ====================

    /**
     * 获取操作日志列表
     */
    @GetMapping("/admin/api/log/list")
    @ResponseBody
    public ApiResult<java.util.List<DroneOperationLog>> getLogList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String operatorType,
            @RequestParam(required = false) Integer status) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneOperationLog> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        // 按关键词搜索（操作者名称或操作内容）
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(DroneOperationLog::getOperatorName, keyword)
                    .or()
                    .like(DroneOperationLog::getOperation, keyword));
        }

        // 按操作者类型筛选
        if (operatorType != null && !operatorType.isEmpty()) {
            queryWrapper.eq(DroneOperationLog::getOperatorType, operatorType);
        }

        // 按状态筛选
        if (status != null) {
            queryWrapper.eq(DroneOperationLog::getStatus, status);
        }

        queryWrapper.orderByDesc(DroneOperationLog::getCreateTime);

        java.util.List<DroneOperationLog> logs = operationLogService.list(queryWrapper);
        return ApiResult.success(logs);
    }

    /**
     * 清空操作日志
     */
    @DeleteMapping("/admin/api/log/clear")
    @ResponseBody
    public ApiResult<Void> clearLogs() {
        operationLogService.remove(null);
        return ApiResult.success();
    }

    /**
     * 删除单条操作日志
     */
    @DeleteMapping("/admin/api/log/{id}")
    @ResponseBody
    public ApiResult<Void> deleteLog(@PathVariable Long id) {
        operationLogService.removeById(id);
        return ApiResult.success();
    }

    // ==================== 轮播图API接口 ====================

    /**
     * 获取轮播图列表
     */
    @GetMapping("/admin/api/banner/list")
    @ResponseBody
    public ApiResult<IPage<DroneBanner>> getBannerList(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        IPage<DroneBanner> bannerPage = bannerService.selectPage(new Page<>(page, 1000), keyword, status);
        return ApiResult.success(bannerPage);
    }

    /**
     * 获取轮播图详情
     */
    @GetMapping("/admin/api/banner/{id}")
    @ResponseBody
    public ApiResult<DroneBanner> getBanner(@PathVariable Long id) {
        DroneBanner banner = bannerService.getById(id);
        if (banner == null) {
            return ApiResult.fail("轮播图不存在");
        }
        return ApiResult.success(banner);
    }

    /**
     * 新增轮播图
     */
    @PostMapping("/admin/api/banner")
    @ResponseBody
    public ApiResult<Void> addBanner(@RequestBody DroneBanner banner) {
        banner.setStatus(banner.getStatus() != null ? banner.getStatus() : 1);
        banner.setSortOrder(banner.getSortOrder() != null ? banner.getSortOrder() : 0);
        bannerService.save(banner);
        return ApiResult.success();
    }

    /**
     * 修改轮播图
     */
    @PutMapping("/admin/api/banner/{id}")
    @ResponseBody
    public ApiResult<Void> updateBanner(@PathVariable Long id, @RequestBody DroneBanner banner) {
        DroneBanner existBanner = bannerService.getById(id);
        if (existBanner == null) {
            return ApiResult.fail("轮播图不存在");
        }
        banner.setId(id);
        bannerService.updateById(banner);
        return ApiResult.success();
    }

    /**
     * 删除轮播图
     */
    @DeleteMapping("/admin/api/banner/{id}")
    @ResponseBody
    public ApiResult<Void> deleteBanner(@PathVariable Long id) {
        bannerService.removeById(id);
        return ApiResult.success();
    }

    /**
     * 修改轮播图状态
     */
    @PostMapping("/admin/api/banner/{id}/status")
    @ResponseBody
    public ApiResult<Void> updateBannerStatus(@PathVariable Long id, @RequestParam Integer status) {
        DroneBanner banner = bannerService.getById(id);
        if (banner == null) {
            return ApiResult.fail("轮播图不存在");
        }
        banner.setStatus(status);
        bannerService.updateById(banner);
        return ApiResult.success();
    }

    // ==================== 无人机上架/下架API接口 ====================

    /**
     * 上架无人机
     */
    @PostMapping("/admin/api/vehicle/{id}/list")
    @ResponseBody
    public ApiResult<Void> listVehicle(@PathVariable Long id) {
        DroneVehicle vehicle = vehicleService.getById(id);
        if (vehicle == null) {
            return ApiResult.fail("无人机不存在");
        }
        vehicle.setIsListed(1);
        vehicleService.updateById(vehicle);
        return ApiResult.success();
    }

    /**
     * 下架无人机
     */
    @PostMapping("/admin/api/vehicle/{id}/unlist")
    @ResponseBody
    public ApiResult<Void> unlistVehicle(@PathVariable Long id) {
        DroneVehicle vehicle = vehicleService.getById(id);
        if (vehicle == null) {
            return ApiResult.fail("无人机不存在");
        }
        vehicle.setIsListed(0);
        vehicleService.updateById(vehicle);
        return ApiResult.success();
    }

    // ==================== 系统设置 ====================

    /**
     * 获取所有系统配置
     */
    @GetMapping("/admin/api/config/list")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getSystemConfigs() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        // 租赁规则配置
        result.put("rental.price.per.hour", systemConfigService.getConfigValue("rental.price.per.hour", "20.00"));
        result.put("rental.price.per.minute", systemConfigService.getConfigValue("rental.price.per.minute", "0.50"));
        result.put("rental.deposit.amount", systemConfigService.getConfigValue("rental.deposit.amount", "500.00"));
        result.put("rental.max.duration", systemConfigService.getConfigValue("rental.max.duration", "480"));
        result.put("rental.min.duration", systemConfigService.getConfigValue("rental.min.duration", "30"));
        result.put("rental.cancel.minutes", systemConfigService.getConfigValue("rental.cancel.minutes", "30"));

        // 系统配置
        result.put("system.name", systemConfigService.getConfigValue("system.name", "无人机智能租赁系统"));
        result.put("system.deposit", systemConfigService.getConfigValue("system.deposit", "500"));
        result.put("system.deposit.required", systemConfigService.getConfigValue("system.deposit.required", "true"));

        // 地图配置
        result.put("map.api.key", systemConfigService.getConfigValue("map.api.key", ""));
        result.put("map.security.code", systemConfigService.getConfigValue("map.security.code", ""));

        return ApiResult.success(result);
    }

    /**
     * 更新系统配置
     */
    @PostMapping("/admin/api/config/update")
    @ResponseBody
    public ApiResult<Void> updateSystemConfigs(@RequestBody java.util.Map<String, String> configs) {
        systemConfigService.batchUpdateConfigs(configs);
        return ApiResult.success();
    }

    // ==================== 充值记录API ====================

    /**
     * 获取充值记录列表
     */
    @GetMapping("/admin/api/recharge/list")
    @ResponseBody
    public ApiResult<IPage<UserRechargeRecord>> getRechargeList(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword) {
        IPage<UserRechargeRecord> recordPage = rechargeRecordService.selectPage(
                new Page<>(page, 10), userId, keyword);
        return ApiResult.success(recordPage);
    }

    /**
     * 获取充值统计
     */
    @GetMapping("/admin/api/recharge/stats")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getRechargeStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        // 总充值金额
        java.math.BigDecimal totalAmount = rechargeRecordService.lambdaQuery()
                .eq(UserRechargeRecord::getStatus, 1)
                .list()
                .stream()
                .map(UserRechargeRecord::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // 今日充值金额
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        java.math.BigDecimal todayAmount = rechargeRecordService.lambdaQuery()
                .eq(UserRechargeRecord::getStatus, 1)
                .ge(UserRechargeRecord::getCreateTime, todayStart)
                .list()
                .stream()
                .map(UserRechargeRecord::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        stats.put("totalAmount", totalAmount != null ? totalAmount : java.math.BigDecimal.ZERO);
        stats.put("todayAmount", todayAmount != null ? todayAmount : java.math.BigDecimal.ZERO);
        stats.put("totalCount", rechargeRecordService.lambdaQuery().eq(UserRechargeRecord::getStatus, 1).count());

        return ApiResult.success(stats);
    }

    // ==================== 支付记录API ====================

    /**
     * 获取支付记录列表
     */
    @GetMapping("/admin/api/payment/list")
    @ResponseBody
    public ApiResult<IPage<UserPaymentRecord>> getPaymentList(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword) {
        IPage<UserPaymentRecord> recordPage = paymentRecordService.selectPage(
                new Page<>(page, 10), userId, keyword);
        return ApiResult.success(recordPage);
    }

    /**
     * 获取支付统计
     */
    @GetMapping("/admin/api/payment/stats")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getPaymentStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        // 总支付金额
        java.math.BigDecimal totalAmount = paymentRecordService.lambdaQuery()
                .eq(UserPaymentRecord::getStatus, 1)
                .list()
                .stream()
                .map(UserPaymentRecord::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // 今日支付金额
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        java.math.BigDecimal todayAmount = paymentRecordService.lambdaQuery()
                .eq(UserPaymentRecord::getStatus, 1)
                .ge(UserPaymentRecord::getCreateTime, todayStart)
                .list()
                .stream()
                .map(UserPaymentRecord::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        stats.put("totalAmount", totalAmount != null ? totalAmount : java.math.BigDecimal.ZERO);
        stats.put("todayAmount", todayAmount != null ? todayAmount : java.math.BigDecimal.ZERO);
        stats.put("totalCount", paymentRecordService.lambdaQuery().eq(UserPaymentRecord::getStatus, 1).count());

        return ApiResult.success(stats);
    }

    // ==================== AI检测记录API ====================

    /**
     * 获取检测记录列表
     */
    @GetMapping("/admin/api/detections")
    @ResponseBody
    public ApiResult<IPage<DroneDetection>> getDetectionList(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String responsibility) {
        Page<DroneDetection> pageParam = new Page<>(page, size);

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DroneDetection> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        // 关键词搜索（检测编号、用户名称）
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(DroneDetection::getDetectionNo, keyword)
                    .or()
                    .like(DroneDetection::getUserName, keyword));
        }

        queryWrapper.eq(userId != null, DroneDetection::getUserId, userId)
                .eq(vehicleId != null, DroneDetection::getVehicleId, vehicleId)
                .eq(responsibility != null && !responsibility.isEmpty(), DroneDetection::getResponsibility, responsibility)
                .orderByDesc(DroneDetection::getCreateTime);

        IPage<DroneDetection> result = detectionService.page(pageParam, queryWrapper);

        return ApiResult.success(result);
    }

    /**
     * 获取检测记录详情
     */
    @GetMapping("/admin/api/detection/{id}")
    @ResponseBody
    public ApiResult<DroneDetection> getDetectionDetail(@PathVariable Long id) {
        DroneDetection detection = detectionService.getById(id);
        if (detection == null) {
            return ApiResult.fail("检测记录不存在");
        }
        return ApiResult.success(detection);
    }

    /**
     * 获取检测统计
     */
    @GetMapping("/admin/api/detection/stats")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getDetectionStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        long totalCount = detectionService.count();
        long damageCount = detectionService.lambdaQuery()
                .gt(DroneDetection::getDamageCount, 0)
                .count();
        long userRespCount = detectionService.lambdaQuery()
                .eq(DroneDetection::getResponsibility, "user")
                .count();
        long operatorRespCount = detectionService.lambdaQuery()
                .eq(DroneDetection::getResponsibility, "operator")
                .count();

        stats.put("totalCount", totalCount);
        stats.put("damageCount", damageCount);
        stats.put("userRespCount", userRespCount);
        stats.put("operatorRespCount", operatorRespCount);
        stats.put("noDamageCount", totalCount - damageCount);

        return ApiResult.success(stats);
    }

    /**
     * 删除检测记录
     */
    @DeleteMapping("/admin/api/detection/{id}")
    @ResponseBody
    public ApiResult<String> deleteDetection(@PathVariable Long id) {
        DroneDetection detection = detectionService.getById(id);
        if (detection == null) {
            return ApiResult.fail("检测记录不存在");
        }
        boolean success = detectionService.removeById(id);
        if (success) {
            return ApiResult.success("删除成功");
        } else {
            return ApiResult.fail("删除失败");
        }
    }

    // ==================== 推荐引擎管理API ====================

    /**
     * 获取推荐统计
     */
    @GetMapping("/admin/api/recommendation/stats")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getRecommendationStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        // 场景分布统计
        java.util.List<java.util.Map<String, Object>> scenarios = recommendationService.getAvailableScenarios();
        stats.put("scenarioCount", scenarios.size());
        stats.put("scenarios", scenarios);

        return ApiResult.success(stats);
    }

    // ==================== 评价管理API ====================

    /**
     * 分页查询所有评价
     */
    @GetMapping("/admin/api/reviews")
    @ResponseBody
    public ApiResult<IPage<OrderReview>> getReviews(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        IPage<OrderReview> pageInfo = new Page<>(page, size);
        IPage<OrderReview> result = reviewService.selectPage(pageInfo, keyword, status);
        return ApiResult.success(result);
    }

    /**
     * 获取评价详情
     */
    @GetMapping("/admin/api/review/{id}")
    @ResponseBody
    public ApiResult<OrderReview> getReviewDetail(@PathVariable Long id) {
        OrderReview review = reviewService.getById(id);
        if (review == null) {
            return ApiResult.fail("评价不存在");
        }
        return ApiResult.success(review);
    }

    /**
     * 管理员回复评价
     */
    @PostMapping("/admin/api/review/{id}/reply")
    @ResponseBody
    public ApiResult<Void> replyReview(@PathVariable Long id, @RequestParam String reply) {
        try {
            reviewService.adminReply(id, reply);
            return ApiResult.success();
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 显示/隐藏评价
     */
    @PostMapping("/admin/api/review/{id}/status")
    @ResponseBody
    public ApiResult<Void> updateReviewStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            reviewService.updateStatus(id, status);
            return ApiResult.success();
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/admin/api/review/{id}")
    @ResponseBody
    public ApiResult<String> deleteReview(@PathVariable Long id) {
        try {
            reviewService.deleteReview(id);
            return ApiResult.success("删除成功");
        } catch (Exception e) {
            return ApiResult.fail(e.getMessage());
        }
    }

    /**
     * 获取评价统计
     */
    @GetMapping("/admin/api/review/stats")
    @ResponseBody
    public ApiResult<java.util.Map<String, Object>> getReviewStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        long totalCount = reviewService.count();
        long visibleCount = reviewService.lambdaQuery()
                .eq(OrderReview::getStatus, 1)
                .count();
        long hiddenCount = reviewService.lambdaQuery()
                .eq(OrderReview::getStatus, 0)
                .count();

        // 计算平均评分
        java.math.BigDecimal avgRating = java.math.BigDecimal.valueOf(
                reviewService.list().stream()
                        .filter(r -> r.getRating() != null)
                        .mapToInt(OrderReview::getRating)
                        .average()
                        .orElse(0.0)
        ).setScale(1, java.math.RoundingMode.HALF_UP);

        stats.put("totalCount", totalCount);
        stats.put("visibleCount", visibleCount);
        stats.put("hiddenCount", hiddenCount);
        stats.put("avgRating", avgRating);

        return ApiResult.success(stats);
    }
}
