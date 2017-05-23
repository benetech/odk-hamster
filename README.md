# Open Data Kit Hamster

This project is a slimmed-down version of the ODK Tables sync web service first made available in ODK Aggregate.  This project is being developed by Benetech and incorporates code from [ODK Aggregate](https://github.com/opendatakit/aggregate) and features contributed by [SolDevelo](http://www.soldevelo.com/).

ODK Hamster is a web service that sits in front of a (Postgres) database and accepts form data from the [ODK Survey Android application.](https://github.com/benetech/ODKSurveyServices)

For the original OpenDataKit project, see:
https://github.com/opendatakit/aggregate

# Get Started

* [Run the Hamster web service.](RUN.md)
* [Run the service in a Docker container.](DOCKER.md)
* [Can I run it on Heroku?](HEROKU.md)
* [How should I edit it in an IDE?](ECLIPSE.md)

# Why "Hamster"?

- Hamsters are small.
- Hamsters collect things.  (In this case, data.)
- "Hamster" won't be confused with any other Open Data Kit or Benetech projects.

There was a ["Project Hamster"](https://projecthamster.wordpress.com/) which was a personal time tracking tool, but it's not a functional competitor and it is a defunct project.  So, "ODK Hamster".

# What are the differences between ODK Aggregate and ODK Hamster?
1. There is no GUI on ODK Hamster.  It is a web service.
2. ODK Hamster currently works with:
  * Digest Authentication
  * Postgres
  * Tomcat
3. ODK Hamster is a Spring Boot application.
4. ODK Hamster is a Jersey web service.
5. ODK Hamster uses Spring Java Configuration files
6. Some other stuff was removed from ODK Hamster:
  * Support for Google Application Engine
  * Dependence on Google Web Toolkit
  * Dependence on Apache Wink
  * Support for other databases
  * Support for other authentication methods
  * Explicit handling of HTTP/HTTPS
  * Bitrock Installer
  * Eclipse-specific projects and configuration

# Why did you remove so much stuff?

We have specific goals for ODK Hamster:

1. Use Open Source technologies where possible.
2. Release ODK Hamster in a Docker container.
3. Rely on a proxy between ODK Hamster and the internet to provide HTTPS.
4. Use ODK Hamster as a RESTful web service.
5. Create a new GUI, separate from the ODK Hamster web service, which does not include any of the ODK 1.0 GUI that appeared in Aggregate.
6. Simplify ODK Hamster to the point that one person or a small team can maintain and add features to it.
7. Set up the project to facilitate continuous improvement by avoiding reliance on GUI tools for configuration or other manual intervention.

We may re-add some of the features removed in the future.  But until those features are requested, we're operating under the [YAGNI](https://en.wikipedia.org/wiki/You_aren%27t_gonna_need_it) principle.  This way we'll have less to maintain as we enhance ODK Hamster.
