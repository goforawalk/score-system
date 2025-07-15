@echo off
echo ========================================
echo 综合验证测试启动脚本
echo ========================================
echo.

REM 检查Node.js是否安装
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到Node.js，请先安装Node.js
    echo 下载地址: https://nodejs.org/
    pause
    exit /b 1
)

echo ✅ Node.js已安装
echo.

REM 检查后端服务是否运行
echo 检查后端服务状态...
curl -s http://localhost:8080/api/health >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠️ 警告: 后端服务可能未运行
    echo 请确保后端服务在 http://localhost:8080 运行
    echo.
    set /p continue="是否继续运行测试? (y/n): "
    if /i not "%continue%"=="y" (
        echo 测试已取消
        pause
        exit /b 1
    )
) else (
    echo ✅ 后端服务正在运行
)

echo.

REM 安装依赖（如果需要）
if not exist "node_modules\node-fetch" (
    echo 安装测试依赖...
    npm install node-fetch
    if %errorlevel% neq 0 (
        echo ❌ 依赖安装失败
        pause
        exit /b 1
    )
    echo ✅ 依赖安装完成
)

echo.

REM 运行综合验证测试
echo 🚀 开始运行综合验证测试...
echo ========================================
node run-verification.js

echo.
echo ========================================
echo 测试完成
echo ========================================
pause 