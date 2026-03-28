package com.drone.rental.web.controller;

import com.drone.rental.service.service.DroneRecommendationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 推荐引擎控制器
 */
@Controller
public class RecommendationController {

    @Autowired
    private DroneRecommendationService recommendationService;

    /**
     * 智能推荐页面
     */
    @GetMapping("/user/recommendation")
    public String recommendationPage(HttpSession session, Model model) {
        // 检查登录状态
        if (session.getAttribute("user") == null) {
            return "redirect:/user/login";
        }
        model.addAttribute("user", session.getAttribute("user"));
        return "user/recommendation";
    }

    // ==================== API 接口 ====================

    /**
     * 获取所有可用场景
     */
    @GetMapping("/user/api/recommendation/scenarios")
    @ResponseBody
    public ApiResult<List<Map<String, Object>>> getScenarios() {
        List<Map<String, Object>> scenarios = recommendationService.getAvailableScenarios();
        return ApiResult.success(scenarios);
    }

    /**
     * 根据场景推荐无人机
     */
    @GetMapping("/user/api/recommendation/by-scenario")
    @ResponseBody
    public ApiResult<List<Map<String, Object>>> recommendByScenario(
            @RequestParam String scenario,
            @RequestParam(required = false, defaultValue = "60") Integer duration) {
        List<Map<String, Object>> recommendations = recommendationService.recommendByScenario(scenario, duration);
        return ApiResult.success(recommendations);
    }

    /**
     * 根据预算推荐无人机
     */
    @GetMapping("/user/api/recommendation/by-budget")
    @ResponseBody
    public ApiResult<List<Map<String, Object>>> recommendByBudget(
            @RequestParam(required = false) Double minBudget,
            @RequestParam(required = false) Double maxBudget) {
        List<Map<String, Object>> recommendations = recommendationService.recommendByBudget(minBudget, maxBudget);
        return ApiResult.success(recommendations);
    }

    /**
     * 智能推荐（综合）
     */
    @PostMapping("/user/api/recommendation/smart")
    @ResponseBody
    public ApiResult<Map<String, Object>> smartRecommend(
            @RequestBody Map<String, Object> params,
            HttpSession session) {
        // 获取用户ID
        Object userObj = session.getAttribute("user");
        if (userObj == null) {
            return ApiResult.fail("请先登录");
        }

        // 假设session中的user有getId方法，实际需要根据SysUser结构调整
        Long userId = 1L; // 临时值，实际应从session获取

        String scenario = (String) params.get("scenario");
        Double budget = params.get("budget") != null ?
                Double.valueOf(params.get("budget").toString()) : null;
        Integer duration = params.get("duration") != null ?
                Integer.valueOf(params.get("duration").toString()) : 60;

        Map<String, Object> result = recommendationService.smartRecommend(userId, scenario, budget, duration);

        // 保存推荐历史
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recommendations = (List<Map<String, Object>>) result.get("recommendations");
        recommendationService.saveRecommendationHistory(userId, scenario, budget, duration, recommendations);

        return ApiResult.success(result);
    }

    /**
     * 获取用户推荐历史
     */
    @GetMapping("/user/api/recommendation/history")
    @ResponseBody
    public ApiResult<List<Map<String, Object>>> getRecommendationHistory(HttpSession session) {
        Long userId = 1L; // 临时值，实际应从session获取
        List<Map<String, Object>> history = recommendationService.getUserRecommendationHistory(userId);
        return ApiResult.success(history);
    }
}
