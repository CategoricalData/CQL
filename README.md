Categorical Query Language
====

For more information, please see:
- <a href="http://categoricaldata.net" target="_blank">Community page</a>

Build and installation
----------------------

### Pre-built Binary

[Java 11 Jar File (14MB)](http://categoricaldata.net/cql.jar)

### Gradle

The recommended build method is [Gradle](https://gradle.org), invoked with the `gradlew` script. Useful tasks include:
- `run`: run the CQL IDE
- `shadowJar`: build the CQL IDE as a JAR in `build/libs`
- `tasks`: show all available tasks

### Eclipse

Quick build guide:
- create a new java project
- add the src/main/java folder as a source folder
- add resources as a separate source folder
- add lib/* to the classpath as external jar files
- run catdata.ide.IDE as the main class

[Detailed Build Guide](https://github.com/CategoricalData/CQL/wiki/detailed-Eclipse-build-instructions)

A gradle file is provided, but is not maintained.

Related projects:
--------------

- [Archived FQL code](https://github.com/CategoricalData/FQL)

License
-------

### Categorical Data IDE

AGPL 3 license for non-commercial use; contact us for commercial licenses.

Copyright (c) 2015+ Conexus AI, Inc.  All rights reserved.

https://www.gnu.org/licenses/agpl-3.0.en.html

Contains binary distributions of

jparsec (Apache),
JUNG (BSD),
RSyntaxTextArea and autocomplete (BSD),
Commons Generics, CSV, and Exec (Apache),
OpenCSV (Apache),
JFlex (BSD),
H2 (Mozilla Public License or Eclipse Public License),
JGraph (BSD),
txtmark (Apache),
commons-lang-3 (Apache),
Google Guava (Apache),
Gnu Trove (LGPL),

And a source distribution of

EASIK (BSD)
