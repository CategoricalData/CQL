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
RSyntaxTextArea, autocomplete and rstaui (BSD),
Commons CSV, Exec, and Collections4 (Apache),
H2 (EPL),
OpenCSV (Apache),
JGraph (BSD),
Javax JSON (CDDL) and json.org JSON (JSON),
txtmark (Apache),
GraalVM (MIT),
Commonmark (BSD),
commons-lang-3 (Apache),
Google Guava (Apache),
Gnu Trove (LGPL),
Picocli (Apache),
Apache HttpComponents and Jena (Apache)

And a source distribution of

EASIK (BSD)
