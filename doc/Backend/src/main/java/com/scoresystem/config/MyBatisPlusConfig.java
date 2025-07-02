package com.scoresystem.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * MyBatis-Plus配置类
 */
@Configuration
@EnableTransactionManagement
@MapperScan("com.scoresystem.repository")
public class MyBatisPlusConfig {
    
    /**
     * 配置MyBatis-Plus拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQL_SERVER));
        return interceptor;
    }
    
    /**
     * 自定义SqlSessionFactory配置
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        // 使用MyBatisPlus提供的SqlSessionFactory构建器
        com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean factoryBean = 
            new com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        // 设置MyBatis-Plus全局配置
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(false);
        
        // 注册JDBC类型处理器
        factoryBean.setGlobalConfig(globalConfig);
        
        // 创建SqlSessionFactory
        SqlSessionFactory sqlSessionFactory = factoryBean.getObject();
        
        // 获取类型处理器注册表
        if (sqlSessionFactory != null) {
            TypeHandlerRegistry typeHandlerRegistry = sqlSessionFactory.getConfiguration().getTypeHandlerRegistry();
            
            // 注册Date类型处理器
            typeHandlerRegistry.register(java.util.Date.class, org.apache.ibatis.type.DateTypeHandler.class);
        }
        
        return sqlSessionFactory;
    }
}