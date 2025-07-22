@echo off
echo Starting LoopBuy Backend Server...
echo =====================================

cd /d "%~dp0backend"

echo Checking Java version...
java -version

echo.
echo Starting server...
java -cp "target\classes;target\dependency\*" com.shiwu.Application

pause
