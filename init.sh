#!/bin/bash

# rideSketch 开发环境启动脚本

echo "=================================="
echo "rideSketch 开发环境启动"
echo "=================================="

# 检查Java版本
echo "[1/4] 检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java，请先安装JDK 17+"
    exit 1
fi

java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$java_version" -lt 17 ]; then
    echo "错误: 需要JDK 17+，当前版本: $java_version"
    exit 1
fi
echo "Java版本检查通过"

# 检查Maven

# 检查Node.js (前端)
echo "[3/4] 检查Node.js环境..."
if ! command -v node &> /dev/null; then
    echo "错误: 未找到Node.js，请先安装Node.js 18+"
    exit 1
fi

node_version=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$node_version" -lt 18 ]; then
    echo "错误: 需要Node.js 18+，当前版本: $node_version"
    exit 1
fi
echo "Node.js版本检查通过"

# 检查MySQL
echo "[4/4] 检查MySQL服务..."
if ! command -v mysql &> /dev/null; then
    echo "警告: 未找到mysql命令，假设MySQL已作为服务运行"
else
    echo "MySQL命令检查通过"
fi

echo ""
echo "=================================="
echo "环境检查完成，开始编译项目"
echo "=================================="

# 编译后端
echo ""
echo ">>> 编译Spring Boot后端..."
cd "$(dirname "$0")"
./mvnw clean compile -DskipTests

if [ $? -ne 0 ]; then
    echo "错误: 后端编译失败"
    exit 1
fi

echo ""
echo "=================================="
echo "编译成功！"
echo "=================================="
echo ""
echo "启动方式:"
echo "  后端: ./mvnw spring-boot:run"
echo "  前端: cd frontend && npm run dev"
echo ""
echo "注意事项:"
echo "  1. 确保MySQL服务已启动 (localhost:3306)"
echo "  2. 确保Redis服务已启动 (localhost:6379)"
echo "  3. 确保Ollama服务已启动 (localhost:11434)"
echo "  4. 请在高德开放平台申请API Key并配置"
echo ""
