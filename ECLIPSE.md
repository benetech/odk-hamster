# How should I edit this project in an IDE?

Some effort has been made into making this project independent of any IDE choice.  Here is how you would open this project in Eclipse, although you should be able to use IntelliJ IDEA or the IDE of your choice just as easily.

1. Create or open a new Eclipse workspace
2. Select from the menus File -> Import -> Maven -> Existing Maven Projects
3. Browse to the odk-hamster (or odk-hamsterball-java) directory you cloned from GitHub and accept all of the defaults.

You can add one or all of the following projects to the workspace:
* [odk-hamster](https://github.com/benetech/odk-hamster), the web service
* [odk-hamsterball-java](https://github.com/benetech/odk-hamsterball-java) the web client
* [odk-tables-api-hamster](https://github.com/benetech/odk-tables-api-hamster) shared object definitions used to marshal data in and out of web service calls
* [odk-hamster-digest-auth-client](https://github.com/benetech/odk-hamster-digest-auth-client) shared functionality to support Digest Authentication

You will probably only want the first two unless you need to make specific changes to digest authentication or web service data definitions.  The second two are included as dependencies by the Maven definitions of the client and web service projects.

If you plan to submit code, please also use one of the formatter definitions in the [developer](developer) folder.

1. Select from the menus Window -> Preferences -> Java -> Code Style -> Formatter
2. Select "Import" and select the [eclipse-java-google-style.xml](developer/eclipse-java-google-style.xml) file from the [developer](developer) folder.
3. Make sure that "GoogleStyle" is selected as the default formatting profile.
4. Apply and save your changes.