@echo off
setlocal
cd /d "%~dp0backend"
echo MySQL mode: execute database\init-mysql.sql in Workbench first.
echo Set DB_USERNAME, DB_PASSWORD and ADMIN_PASSWORD in your terminal before running.
echo http://localhost:8081
java -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -jar ..\release\campus-lost-found.war --spring.profiles.active=mysql
if errorlevel 1 pause
