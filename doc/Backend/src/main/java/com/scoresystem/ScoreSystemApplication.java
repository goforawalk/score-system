package com.scoresystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 评分系统应用程序入口
 */
@SpringBootApplication
public class ScoreSystemApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ScoreSystemApplication.class, args);
        
        // 添加应用关闭钩子，确保线程池正确关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("应用正在关闭，正在清理资源...");
            try {
                // 关闭Spring上下文
                context.close();
                System.out.println("Spring上下文已关闭");
            } catch (Exception e) {
                System.err.println("关闭Spring上下文时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }
    
    // 所有CORS配置已移至CorsConfig类
} 