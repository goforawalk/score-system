package com.scoresystem.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * 测试数据源配置
 * 用于在测试环境中配置不同的数据源
 */
@Configuration
public class TestDataSourceConfig {

    /**
     * H2数据库数据源配置
     */
    @Configuration
    @Profile("h2-test")
    public static class H2DataSourceConfig {

        @Bean
        @Primary
        @ConfigurationProperties("spring.datasource")
        public DataSourceProperties dataSourceProperties() {
            return new DataSourceProperties();
        }

        @Bean
        @Primary
        @ConfigurationProperties("spring.datasource.hikari")
        public DataSource dataSource(DataSourceProperties properties) {
            return properties.initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
        }
    }

    /**
     * SQL Server数据库数据源配置
     */
    @Configuration
    @Profile("sqlserver")
    public static class SqlServerDataSourceConfig {

    @Bean
    @Primary
        @ConfigurationProperties("spring.datasource")
        public DataSourceProperties dataSourceProperties() {
            return new DataSourceProperties();
        }

        @Bean
        @Primary
        @ConfigurationProperties("spring.datasource.hikari")
        public DataSource dataSource(DataSourceProperties properties) {
            return properties.initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
        }
    }
} 