package com.scoresystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * JdbcTemplate配置类
 * 用于在测试中使用JdbcTemplate
 */
@Configuration
public class JdbcTemplateConfig {

    /**
     * 创建JdbcTemplate Bean
     * 
     * @param dataSource 数据源
     * @return JdbcTemplate实例
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
