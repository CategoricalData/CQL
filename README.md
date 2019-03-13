Categorical Data IDE
====

About
-----

An IDE for the functorial query and data migration languages AQL, FQL, FPQL, FQLPP, MPL, OPL, and EASIK.

For more information, please see:
- [CategoricalData AQL page](categoricaldata.net/aql.html)
- [Categorical Informatics](http://catinf.com/), where these query languages are being commercialized
- [mailing list](https://groups.google.com/forum/#!forum/categoricaldata)

Build and installation
----------------------

### Pre-compiled binary jar file:

http://categoricaldata.net/aql.jar

### Eclipse
	
For best results, compile using the [Eclipse IDE](https://eclipse.org/jdt/).

### Gradle

    git clone https://github.com/CategoricalData/fql.git
    cd fql
    gradle run --args="-p combinator"

Editor Support
--------------

- AQL mode for Emacs https://github.com/epost/aql-mode

In order to prepare the configuration files for an editor.

    gradle idea
    
or
   
    gradle eclipse
    
The open the project from the appropriate IDE.

License
-------

### Categorical Data IDE (BSD License)

Copyright (c) 2012-2017 Ryan Wisnesky.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the author nor the names of its contributors may
      be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Contains binary distributions of the following libraries:

jparsec (Apache License),
JUNG (BSD License),
RSyntaxTextArea and autocomplete (BSD License),
Commons Generics, CSV, and Exec (Apache License),
OpenCSV (Apache license)
JFlex (BSD License),
H2 (Mozilla Public License or Eclipse Public License),
JGraph (BSD License)
txtmark (Apache)
commons-lang-3 (Apache)
Google Guava (Apache)
Gnu Trove (LGPL)

And a source distribution of:

EASIK (BSD license)

Copyright statements for these projects are included below.

### Trove (LGPL)

Copyright Rob Eden, Johan Parent, Jeff Randall, and Eric D. Friedman

### H2 (Mozilla Public License or Eclipse Public License)

This software contains unmodified binary redistributions for
H2 database engine (http://www.h2database.com/),
which is dual licensed and available under the MPL 2.0
(Mozilla Public License) or under the EPL 1.0 (Eclipse Public License).
An original copy of the license agreement can be found at:
http://www.h2database.com/html/license.html

### OpenCSV (Apache 2.0 License)

See http://opencsv.sourceforge.net/index.html for copyright information.

### txtmark (Apache 2.0 License)

Copyright (C) 2011-2015 René Jeschke

### JParsec (Apache 2.0 License)

Copyright (c) The Codehaus

### Apache Commons Collections Generics and CSV and Exec and Lang-3 (Apache 2.0 License)

Copyright (c) The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

### JGraph (BSD License)
Copyright (c) JGraph Ltd

### JUNG - Java Universal Network/Graph Framework (BSD License)
Copyright (c) Joshua O'Madadhain, Danyel Fisher, and Scott White

### RSyntaxTextArea (and autocomplete, rstaui libraries) (BSD License)
Copyright (c) Robert Futrell

### JFlex (BSD License)
Copyright (C) Gerwin Klein, Steve Rowe, Regis Decamp

### EASIK (BSD License) 
Copyright (c) Robert Rosebrugh, Rob Fletcher (2005), Kevin Green (2006), Vera Ranieri(2006), Jason Rhinelander (2008-09), Andrew Wood (2008-09), Christian Fiddick (2012), Sarah VanderLaan (2013) and Federico Mora (2014).

### JSONP
JSON Processing project is the open source reference implementation of JSR 353 - Java API for JSON Processing.

[JSONP license](http://jsonp.java.net/license.html)
