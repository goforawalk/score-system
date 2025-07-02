package com.scoresystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 数据库切换配置
 * 用于在测试环境中切换不同的数据库
 */
@Configuration
public class DatabaseSwitchConfig {

    /**
     * H2数据库配置
     * 在spring.profiles.active=h2-test时激活
     */
    @Configuration
    @Profile("h2-test")
    public static class H2DatabaseConfig {
        public H2DatabaseConfig() {
            System.out.println("启用H2内存数据库配置");
        }
    }

    /**
     * SQL Server数据库配置
     * 在spring.profiles.active=sqlserver时激活
     */
    @Configuration
    @Profile("sqlserver")
    public static class SqlServerDatabaseConfig {
        public SqlServerDatabaseConfig() {
            System.out.println("启用SQL Server数据库配置");
        }
    }
} 