/*
This example is recommended for anyone dealing with SQL data and is built-in to the IDE as QuickSQL. 
It imports a cloud-based MySQL database about employees and departments into CQL, 
demonstrates basic CQL operations, and then exports the data back into the MySQL database. 
Accessing any SQL database management system in CQL requires adding the vendor's JDBC driver to the 
java classpath when running CQL; this example requires downloading MySQL connector-j version 5.1.47 
and running CQL from the terminal in a directory containing both jar files 
(on Windows, replace : by ;):

java -cp "cql.jar:mysql-connector-java-5.1.47.jar" catdata.ide.IDE
*/

//put your JDBC configuration options at the top of the file.
//In this case, we are using a public MySQL DB that uses back-ticks for quotes. 
options
	jdbc_default_string = "jdbc:mysql://mysql.catinf.com:3306/aql_tutorial?user=catinfcom&password=FDiUY!pr&serverTimezone=America/New_York"
	jdbc_quote_char = "`"
	always_reload = true 

//Because this quickstart is using a public repo, for hygiene reasons we also drop these tables 
//if they currently exist, and always reload (see above).

command Prep = exec_jdbc "" {
	"START TRANSACTION"
	"SET FOREIGN_KEY_CHECKS=0"
	"DROP TABLE IF EXISTS OutEmployee"
	"DROP TABLE IF EXISTS OutDepartment"
	"SET FOREIGN_KEY_CHECKS=1"	
	"COMMIT"
}
		
//We start by viewing a public MySQL database

command ViewSrc = exec_jdbc "" {
	"SELECT * FROM Employee"
	"SELECT * FROM Department"	
}

//We next import that database into CQL.  
//The only difference between the SQL and CQL tables is that in CQL we can see 
//globally unique row numbers and references."

instance EmpDept = import_jdbc_all "" 

//We can examine the imported SQL schema, although we will not discuss it further here. 

schema EmpDeptSch = schemaOf EmpDept

//To export our imported instance back to SQL, we choose a prefix (here, 'Out').  


//We export the data using 'row' is the name of the column for CQL's row numbers
command Export = export_jdbc_instance EmpDept "" "Out" {
	options
		id_column_name = "row"
} 

//And now we can see the exported data in SQL.

command ViewDst = exec_jdbc "" {
	"SELECT * FROM OutEmployee"
	"SELECT * FROM OutDepartment"	
}

//Note that in its fully generality as described above, round tripping is verbose,
//leading to arguably redundant information.  See the JDBCSQL example for an import/export
//process that removes these redundancies.
