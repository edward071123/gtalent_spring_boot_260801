package student.ed.gtalent_spring_boot_260801.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import student.ed.gtalent_spring_boot_260801.interceptor.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // members 需要登入的 API 統一先經過 AuthInterceptor 檢查 token。
        // register/login/refresh/logout 是登入流程本身，所以不做攔截。
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/members/**")
                .excludePathPatterns(
                        "/members/register",
                        "/members/login",
                        "/members/refresh"
                );
    }
}