@echo off
echo 🔧 LoopBuy后端服务启动调试脚本
echo =====================================

echo.
echo 📊 1. 检查环境...
echo Java版本:
java -version
if %errorlevel% neq 0 (
    echo ❌ Java未安装或未配置PATH
    pause
    exit /b 1
)

echo.
echo Maven版本:
mvn -version
if %errorlevel% neq 0 (
    echo ❌ Maven未安装或未配置PATH
    pause
    exit /b 1
)

echo.
echo 🗄️ 2. 检查数据库连接...
mysql -u root -p123456 -e "SELECT 1" 2>nul
if %errorlevel% neq 0 (
    echo ❌ 数据库连接失败，请检查MySQL服务和密码
    echo 尝试启动MySQL服务...
    net start mysql80
)

echo.
echo 🧹 3. 清理旧进程...
taskkill /f /im java.exe 2>nul
echo Java进程已清理

echo.
echo 📁 4. 进入后端目录...
cd /d "%~dp0backend"
if not exist "pom.xml" (
    echo ❌ 未找到pom.xml文件，请确认在正确目录
    pause
    exit /b 1
)

echo.
echo 🧽 5. 清理项目...
mvn clean
if %errorlevel% neq 0 (
    echo ❌ Maven清理失败
    pause
    exit /b 1
)

echo.
echo 🔨 6. 编译项目...
mvn compile
if %errorlevel% neq 0 (
    echo ❌ Maven编译失败
    pause
    exit /b 1
)

echo.
echo 📦 7. 复制依赖...
mvn dependency:copy-dependencies
if %errorlevel% neq 0 (
    echo ❌ 依赖复制失败
    pause
    exit /b 1
)

echo.
echo 🚀 8. 启动服务器...
echo 启动命令: java -cp "target/classes;target/dependency/*" com.shiwu.Application
echo.
java -cp "target/classes;target/dependency/*" com.shiwu.Application

pause
