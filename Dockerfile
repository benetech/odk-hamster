FROM openjdk:8-jdk

# Control Java heap and metaspace sizes
ENV MIN_HEAP 256m
ENV MAX_HEAP 1024m
ENV MAX_METASPACE 128m

ENV JAVA_OPTS -server -Xms$MIN_HEAP -Xmx$MAX_HEAP -XX:MaxMetaspaceSize=$MAX_METASPACE -XX:+UseG1GC

# This Dockerfile runs an insecure instance of ODK 2.0 (Hamster) Server on the default Tomcat port
# It is intended to be installed behind an SSL proxy

MAINTAINER Benetech <cadenh@benetech.org>

ENV SPRING_DATASOURCE_URL='jdbc:postgresql://192.168.86.113/hamster_db?autoDeserialize=true' \
    SPRING_DATASOURCE_USERNAME='hamster_db' \
    SPRING_DATASOURCE_PASSWORD='hamster_db' \
    JDBC_SCHEMA='hamster_db' 
    
VOLUME /tmp
COPY ./target/classes/static /tmp/static
ADD ./target/odk-hamster*.jar odk-hamster.jar
RUN sh -c 'touch /odk-hamster.jar'
    
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Dspring.datasource.url=$SPRING_DATASOURCE_URL -Dspring.datasource.username=$SPRING_DATASOURCE_USERNAME -Dspring.datasource.password=$SPRING_DATASOURCE_PASSWORD -Djdbc.schema=$JDBC_SCHEMA -jar /odk-hamster.jar" ]
    
EXPOSE 8080