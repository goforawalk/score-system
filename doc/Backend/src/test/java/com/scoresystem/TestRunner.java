package com.scoresystem;

/**
 * 测试运行器
 * 用于运行测试，支持H2和SQL Server两种数据库环境
 */
public class TestRunner {

    /**
     * 使用H2数据库运行所有测试
     */
    public static void runWithH2Database() {
        System.setProperty("spring.profiles.active", "h2-test");
        System.out.println("使用H2数据库运行测试");
    }

    /**
     * 使用SQL Server数据库运行所有测试
     */
    public static void runWithSqlServerDatabase() {
        System.setProperty("spring.profiles.active", "sqlserver");
        System.out.println("使用SQL Server数据库运行测试");
    }

    /**
     * 主方法，默认使用SQL Server数据库运行所有测试
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("h2")) {
            runWithH2Database();
        } else {
            runWithSqlServerDatabase();
        }
    }
}