# About The Dockerfile

The included [Dockerfile](Dockerfile) is to run an insecure instance which will reside behind an SSL proxy.  SSL proxy not included.

The [Dockerfile](Dockerfile) is in the root directory.

The following environment variables need to be overridden.

Environment variables to override (with sample values):
+ SPRING\_DATASOURCE\_URL='jdbc:postgresql://192.168.1.113/odk_db?autoDeserialize=true'
+ SPRING\_DATASOURCE\_USERNAME='odk_db'
+ SPRING\_DATASOURCE\_PASSWORD='odk_db'
+ JDBC\_SCHEMA='odk_db'

A database must be created matching the above environment variables.  An SQL script that can be used to create this database is here:

[src/main/resources/create_database.sql](src/main/resources/create_database.sql)

Make sure that the environment variable values match those in the SQL file.

The Tomcat instance uses the default port 8080.

## Building and running in a Docker container

Build the [Dockerfile](Dockerfile) in the project root directory with tag odk_ws
```shell
docker build -t odk_ws .
```
Install and start Docker container with:
+ Name ```odk_webservice```.
+ From image ```odk_ws```.
+ Forwarding the Tomcat port
+ Overriding the database environment variables.

```shell
docker run -d -i -t --name odk_webservice -p 8080:8080  -e "SPRING_DATASOURCE_URL=jdbc:postgresql://192.168.1.113/odk_unit?autoDeserialize=true" -e "SPRING_DATASOURCE_USERNAME=my_db" -e "SPRING_DATASOURCE_PASSWORD=my_db" -e "JDBC_SCHEMA=my_db" pstop
```
Tail the log:
```shell
docker logs -f odk_webservice
```
Stop and delete the image and container. (This can be helpful if you realize you need to do some additional configuration on your Docker container and you would like to start over from the `docker build` command.
```shell
docker stop odk_webservice;docker rm odk_webservice;docker rmi odk_ws
```

