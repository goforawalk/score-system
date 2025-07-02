package com.scoresystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 评分系统应用程序入口
 */
@SpringBootApplication
public class ScoreSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScoreSystemApplication.class, args);
    }
    
    // 所有CORS配置已移至CorsConfig类
} 