@echo off
setlocal
cd /d "%~dp0"
cd frontend
call npm ci
if errorlevel 1 exit /b 1
call npm run build
if errorlevel 1 exit /b 1
cd ..
node tools\copy-static.js
if errorlevel 1 exit /b 1
call mvn -B -ntp package
if errorlevel 1 exit /b 1
if not exist release mkdir release
copy /y backend\target\campus-lost-found.war release\campus-lost-found.war
echo Build completed. Run start-demo.cmd or start-mysql.cmd.
