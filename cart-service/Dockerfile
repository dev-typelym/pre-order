#   베이스 이미지
FROM openjdk:21-jdk

#   JAR 파일 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

#   컨테이너 시작 시 실행 명령
ENTRYPOINT ["java","-jar","/app.jar"]