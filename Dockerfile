# Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the built jar file
# Note: You need to run 'mvn clean package' first to generate the jar
COPY target/im-system-1.0-SNAPSHOT.jar app.jar

# Expose ports
# 10085: Web/SpringBoot
# 10086: Netty TCP
# 10087: Netty WebSocket
EXPOSE 10085 10086 10087

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
