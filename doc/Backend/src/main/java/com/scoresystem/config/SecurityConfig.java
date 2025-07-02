package com.scoresystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

/**
 * 安全配置类
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * 生产环境安全配置
     */
    @Configuration
    @Profile("!test")
    public static class ProductionSecurityConfig extends WebSecurityConfigurerAdapter {
        
        private final CorsFilter corsFilter;
        
        public ProductionSecurityConfig(CorsFilter corsFilter) {
            this.corsFilter = corsFilter;
        }
        
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                // 添加CORS过滤器到安全链的最前面
                .addFilterBefore(corsFilter, ChannelProcessingFilter.class)
                // 配置CORS
                .cors().configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(Collections.singletonList("*"));
                    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    return config;
                })
                .and()
                .csrf().disable()
                .authorizeRequests()
                // 允许未认证访问的端点
                .antMatchers("/auth/login").permitAll()
                // 允许访问项目列表和其他公共API
                .antMatchers("/projects", "/projects/**").permitAll()
                .antMatchers("/users", "/users/**").permitAll()
                .antMatchers("/tasks","/tasks/**", "/tasks/active").permitAll()
                .antMatchers("/statistics/**").permitAll()
             // 允许访问评分API
                .antMatchers("/scores", "/scores/**").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated();
        }
    }
    
    /**
     * 测试环境安全配置
     */
    @Configuration
    @Profile("test")
    public static class TestSecurityConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .cors().configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(Collections.singletonList("*"));
                    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    return config;
                })
                .and()
                .csrf().disable()
                .authorizeRequests()
                .anyRequest().permitAll();
        }
    }
} 