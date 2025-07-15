#!/bin/bash

echo "========================================"
echo "综合验证测试启动脚本"
echo "========================================"
echo

# 检查Node.js是否安装
if ! command -v node &> /dev/null; then
    echo "❌ 错误: 未找到Node.js，请先安装Node.js"
    echo "下载地址: https://nodejs.org/"
    exit 1
fi

echo "✅ Node.js已安装"
echo

# 检查后端服务是否运行
echo "检查后端服务状态..."
if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
    echo "✅ 后端服务正在运行"
else
    echo "⚠️ 警告: 后端服务可能未运行"
    echo "请确保后端服务在 http://localhost:8080 运行"
    echo
    read -p "是否继续运行测试? (y/n): " continue
    if [[ ! $continue =~ ^[Yy]$ ]]; then
        echo "测试已取消"
        exit 1
    fi
fi

echo

# 安装依赖（如果需要）
if [ ! -d "node_modules/node-fetch" ]; then
    echo "安装测试依赖..."
    npm install node-fetch
    if [ $? -ne 0 ]; then
        echo "❌ 依赖安装失败"
        exit 1
    fi
    echo "✅ 依赖安装完成"
fi

echo

# 运行综合验证测试
echo "🚀 开始运行综合验证测试..."
echo "========================================"
node run-verification.js

echo
echo "========================================"
echo "测试完成"
echo "========================================" 