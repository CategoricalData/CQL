
/*
This example is recommended for anyone dealing with SQL data and is built-in to the IDE as QuickSQL. 
It imports a cloud-based MySQL database about employees and departments into CQL, 
demonstrates basic CQL operations, and then exports the data to a local SQL database. 
Accessing any SQL database management system in CQL requires adding the vendor's JDBC driver to the 
java classpath when running CQL; this example requires downloading MySQL connector-j version 5.1.47 
and running CQL from the terminal in a directory containing both jar files 
(on Windows, replace : by ;):

java -cp "cql.jar:mysql-connector-java-5.1.47.jar" catdata.ide.IDE
*/

//can help with debugging, forces re-execution of all expressions on each run
//options
//	always_reload = true 
		
//We start by viewing a public MySQL database

command ViewSrc = exec_jdbc "jdbc:mysql://mysql.categoricaldata.net:3306/aql_tutorial?user=catinfcom&password=FDiUY!pr&serverTimezone=America/New_York" {
	"SELECT * FROM Employee"
	"SELECT * FROM Department"	
}

//We next import that database into CQL in a quick and dirty (best effort) way.  
//In this case, the only difference between the SQL and CQL tables is that in CQL we can see 
//globally unique row numbers and references.  In general, however, SQL databases
//need to be imported explicitly - see the JDBCSQL example.
//In this case, we are using a public MySQL DB that uses back-ticks for quotes. 
instance EmpDept = import_jdbc_all "jdbc:mysql://mysql.categoricaldata.net:3306/aql_tutorial?user=catinfcom&password=FDiUY!pr&serverTimezone=America/New_York" {
	options jdbc_quote_char = "`"
    allow_sql_import_all_unsafe=true
}

//We can examine the imported SQL schema, although we will not discuss it further here. 

schema EmpDeptSch = schemaOf EmpDept

//To export our imported instance back to SQL, we choose a prefix (here, 'Out').  
//We export the data using 'row' is the name of the column for CQL's row numbers.
//Note that we export to CQL's built-in SQL Database (see JDBCSQL example for more details.)
//We clean up in case a previous run left tables around.
command CleanUp = exec_jdbc "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1" {
	"SET FOREIGN_KEY_CHECKS=0"
	"DROP TABLE IF EXISTS \"OutEmployee\""
	"DROP TABLE IF EXISTS \"OutDepartment\""
	"SET FOREIGN_KEY_CHECKS=1"	
}
command Export = export_jdbc_instance EmpDept "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1" "Out" {
	options
		id_column_name = "row"
} 

//And now we can see the exported data in SQL.

command ViewDst = exec_jdbc "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1" {
	"SELECT * FROM \"OutEmployee\""
	"SELECT * FROM \"OutDepartment\""	
} 

//Note that in its fully generality as described above, round tripping is verbose,
//leading to arguably redundant information.  See the JDBCSQL example for an import/export
//process that removes these redundancies.
