
The antlr4 files are build by running the 
boot promote 

from catdata/aql/grammar

Once the java files have been built eclipse needs to 
know about them.
The easiest way is to make a symbolic link.

ln -s ../../../gen/catdata/aql/grammar/ ./src/catdata/aql/grammar

This will confuse maven though so the link needs to be removed before running 

mvn package

