package com.scoresystem.service;

import com.scoresystem.dto.ScoreSystemModels.ApiResponse;

public interface TestDataService {
    /**
     * 清空所有测试数据
     */
    void clearAllTestData();

    /**
     * 一键生成测试数据
     * @return 生成结果描述
     */
    String generateTestData();
} 