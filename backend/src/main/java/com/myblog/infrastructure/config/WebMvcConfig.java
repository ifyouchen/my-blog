package com.myblog.infrastructure.config;

import com.myblog.infrastructure.security.JwtAuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置。
 *
 * @author Codex
 * @since 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;
    private final String uploadDir;

    /**
     * 创建 Web MVC 配置。
     *
     * @param jwtAuthenticationInterceptor JWT 认证拦截器
     */
    public WebMvcConfig(JwtAuthenticationInterceptor jwtAuthenticationInterceptor,
                        @Value("${my-blog.upload-dir:uploads}") String uploadDir) {
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
        this.uploadDir = uploadDir;
    }

    /**
     * 注册接口拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/register",
                "/api/auth/register/email-code",
                "/api/auth/login",
                "/api/auth/password/forgot",
                "/api/auth/password/reset",
                "/api/health",
                "/api/uploads/files/**"
            );
    }

    /**
     * 注册跨域配置。
     *
     * @param registry 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(false)
            .maxAge(3600);
    }

    /**
     * 注册静态资源处理器，将上传文件目录映射为可访问的 URL 路径。
     *
     * @param registry 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 先将配置路径转为规范绝对路径，避免相对路径片段导致资源映射歧义。
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/api/uploads/files/**")
            .addResourceLocations(location, "classpath:/static/uploads/");
    }
}
