# Can I run it on Heroku?

Open Data Kit Hamster and Open Data Kit Hamsterball Client have been [run successfully on Heroku using these instructions.](https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku)  
You will probably want to either fork the existing repositories, or clone them and set your Heroku project repositories as remote repositories where you can push your changes.

Note that the client project contains a [Heroku Procfile](https://github.com/benetech/odk-hamsterball-java/blob/master/Procfile), which you will have to update to point at your instance of the Hamster web service.

In case it is not clear, you will need 2 Heroku apps to run both the client and web service.  It would not be easy to run them both on one Heroku app.  For the web service, you will need a Postgres instance, for which you can use a Heroku Postgres Add-on.
