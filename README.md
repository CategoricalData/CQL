Categorical Query Language IDE
====

About
-----

An IDE for the categorical query and data migration/integration language, CQL.  Also, we maintain EASIK here.

For more information, please see:
- [Academic CQL page](categoricaldata.net/cql.html)
- [Commercial CQL page](http://categorical.info/)

Build and installation
----------------------

### Pre-compiled binary jar file:

http://categoricaldata.net/cql.jar

### Eclipse
	
For best results, compile using the [Eclipse IDE](https://eclipse.org/jdt/).

### Gradle

    git clone https://github.com/CategoricalData/fql.git
    cd fql
    gradle run --args="-p combinator"

Related projects:
--------------

- CQL mode for Emacs: https://github.com/epost/aql-mode

- Statebox CQL Haskell implementation (coming soon)

License
-------

### Categorical Data IDE (AGPL 3 license)

Copyright (c) 2015+ Categorical Informatics.  All rights reserved.

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
