FROM eclipse-temurin:8-jre
WORKDIR /app
COPY release/campus-lost-found.war /app/app.war
RUN mkdir -p /app/data
EXPOSE 8081
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-Duser.timezone=Asia/Shanghai","-jar","/app/app.war"]
