package cn.campus.lostfound;

import javax.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Bean
  public BCryptPasswordEncoder passwordEncoder(@Value("${app.bcrypt-strength:10}") int strength) {
    return new BCryptPasswordEncoder(strength);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
      .addInterceptor(
        new HandlerInterceptor() {
          public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object h) {
            String m = req.getMethod();
            if (
              !"GET".equals(m) &&
              !"HEAD".equals(m) &&
              !"OPTIONS".equals(m) &&
              !"campus".equals(req.getHeader("X-Requested-With"))
            ) throw new ApiError.Failure(403, "请求来源校验失败");
            return true;
          }
        }
      )
      .addPathPatterns("/api/**");
  }
}
