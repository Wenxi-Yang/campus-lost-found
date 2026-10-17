@echo off
setlocal
cd /d "%~dp0backend"
echo Campus Lost and Found DEMO: http://localhost:8081
echo Admin: admin / Admin12345
echo Student: 20260001 / Student123
java -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar ..\release\campus-lost-found.war --spring.profiles.active=demo
if errorlevel 1 pause
