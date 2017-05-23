# Run Open Data Kit Hamster

Here's how you get Open Data Kit Hamster up and running locally.  You should do more configuration (such as choosing a legitimately secure database password) before using it in a production setting.

## Prerequisites
It is assumed you have working knowledge of Git, Maven, Tomcat, and Postgres.

* Postgres 9.4 or above
* Maven 3.3.3 or above
* Java 8

## First, install your database

Install Postgres 9.4 or above.

You can use an existing installation if you have one.

## Second, initialize your database

To use the default database values, enter the following in the psql client.

```
create database "hamster_dev";
select datname FROM pg_database where datistemplate = false;
create user "hamster_dev" with unencrypted password 'hamster_dev';
grant all privileges on database "hamster_dev" to "hamster_dev";
alter database "hamster_dev" owner to "odk_hamster";
\c "hamster_dev";
create schema "hamster_dev";
grant all privileges on schema "hamster_dev" to "hamster_dev";
```

If desired, run these commands again for **`hamster_unit`** so that you can run unit tests.

```
create database "hamster_unit";
select datname FROM pg_database where datistemplate = false;
create user "hamster_unit" with unencrypted password 'hamster_unit';
grant all privileges on database "hamster_unit" to "hamster_unit";
alter database "hamster_unit" owner to "odk_hamster";
\c "hamster_unit";
create schema "hamster_unit";
grant all privileges on schema "hamster_unit" to "hamster_unit";
```

##  Third, check out source code.

```shell
git clone git@github.com:benetech/odk-hamster.git
```

## Fourth, compile the code

From the odk-hamster directory, run:

```shell
mvn clean install -DskipTests
```

If you'd rather run unit tests, which take a few minutes longer, run:

```shell
mvn clean install
```

## Fifth, run the code

The code runs by default at port 8080, so you may want to disable any other services you're running that are using that port at this time.

Run the web client:
```shell
java -jar target/*.jar
```

Optionally, if you must run on another port, add the following line to `odk-hamster/src/main/resources/application-default.properties` where `9999` is a port you know to be free.
```
server.port=9999
```

## You're running!

The service will be launched by default at http://localhost:8080.  There should be a welcome page there with links to the Jersey WADL file and the Swagger UI.

You can use the Swagger UI to test the web services.  For most of the calls, you will need to be logged in.  There is an "Authorize" button in the upper right where you can enter your web service credentials.  The default login information is username: `admin`, password: `aggregate`.

## What's next?

Do you want to run this web service inside a Docker container? [Read the Docker container instructions.](DOCKER.md)

Do you want to see this project in Eclipse?  [Read the IDE instructions.](ECLIPSE.md)

Do you want to get the web client running?  [Read the instructions for running the web client.](https://github.com/benetech/odk-hamsterball-java/blob/master/RUN.md)
