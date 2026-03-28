package com.drone.rental.web.config;

import com.drone.rental.web.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源处理
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        // 上传文件访问（支持/uploads和/api/uploads两种路径）
        registry.addResourceHandler("/uploads/**", "/api/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS跨域配置
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器 - 排除API接口
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/user/**", "/operator/**", "/admin/**")
                .excludePathPatterns(
                        // 登录相关页面
                        "/user", "/user/index", "/user/login", "/user/register", "/user/doLogin", "/user/doRegister", "/user/logout",
                        "/operator/login", "/operator/doLogin", "/operator/logout",
                        "/admin/login", "/admin/doLogin", "/admin/logout",
                        // 静态资源
                        "/static/**", "/css/**", "/js/**", "/images/**",
                        // API接口（不需要session认证，使用token认证）
                        "/admin/api/**",
                        "/operator/api/**",
                        "/user/api/**",
                        "/api/**",
                        "/upload",
                        "/uploads/**"
                );
    }
}
