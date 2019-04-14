
//create some example data in a local temporary sql database. (close delay keeps H2 database alive across connections)
command load = exec_jdbc "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1" { 
"
DROP TABLE IF EXISTS Employee;
DROP TABLE IF EXISTS Department;" //multiple statements can be used, but many SQL engines (such as H2, CQL's internal engine) will only print the output of the first statement, and some require ?allowMultiQueries=true in the jdbc string 

"
CREATE TABLE Employee(
 id INT PRIMARY KEY,
 name VARCHAR(255),
 manager INT, 
 worksIn INT
)"

//a bug in the library for CQL's code editor prevents the correct highlighting of 
//multi-line quotes.  Starting the text on the line after the initial quote helps readability.
"
CREATE TABLE Department(
 id INT PRIMARY KEY,
 name VARCHAR(255),
 secretary INT,
)"
 
"
INSERT INTO Employee VALUES 
 (101, 'Alan', 103, 10), 
 (102, 'Camille', 102, 2), 
 (103, 'Andrey', 103, 10)"

"
INSERT INTO Department VALUES
 (10, 'Applied Math', 101),
 (2, 'Pure Math', 102)"

"
ALTER TABLE Employee ADD CONSTRAINT e1
 FOREIGN KEY (manager) REFERENCES Employee (id)"

"
ALTER TABLE Employee ADD CONSTRAINT e2 
 FOREIGN KEY (worksIn) REFERENCES Department (id)"

"
ALTER TABLE Department ADD CONSTRAINT d1
 FOREIGN KEY (secretary) REFERENCES Employee (id)"

// options always_reload = true //true forces pragmas to not be cached between runs (i.e., always run)
}  
 
/* When the JDBC class name and string are left blank, CQL uses the value of the option
    jdbc_default_string.  These can be set globally by placing,
   at the beginning of the file, for example:
   
  options
   
    jdbc_default_string = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1"
 */
//look at the example data
command view0 = exec_jdbc "" {  //see note above!!!
"SELECT * FROM Department"
"SELECT * FROM Employee"
}

////////////////////////////////////////////////////////////////////////////////////////////////////////

schema S = literal : sql {
	entities
		Employee 
		Department
	foreign_keys
		manager   : Employee -> Employee
		worksIn   : Employee -> Department
		secretary : Department -> Employee
	path_equations 
		Employee.manager.worksIn = Employee.worksIn
  		Department.secretary.worksIn = Department
  		Employee.manager.manager = Employee.manager
  	attributes
  		first last	: Employee -> Varchar
     	age			: Employee -> Integer
     	name 		: Department -> Varchar
 }

//import an instance by providing queries for each entity
instance J = import_jdbc ""  : S {
//use name as first name
  //use null as last name
  //use id as age	
	Employee -> "SELECT id, manager, worksIn, name AS first, NULL as last, id AS age FROM Employee"
	Department -> "SELECT id, secretary, name FROM Department"

	options
		//aql by default prepends the entity to each imported ID, to get uniqueness across entities.
		//to import the IDs verbatim, set the option below to false
		prepend_entity_on_ids = false
}



//import a transform by providing queries for each entity
transform Jid = import_jdbc ""  : J -> J {
	Employee -> "SELECT id, id FROM Employee"
	Department -> "SELECT id, id FROM Department"

	options
		prepend_entity_on_ids = false
}

//export the instance to SQL
command store1 = export_jdbc_instance J ""  "Exported"
{ options start_ids_at = 100 }

//view exported SQL instance
command view1 = exec_jdbc ""  { 
	"SELECT * FROM \"ExportedEmployee\""
	"SELECT * FROM \"ExportedDepartment\""
}

//export the transform to SQL
command store2 = export_jdbc_transform Jid ""  "ExportedTrans" 
 { options start_ids_at = 100 } //src
 { options start_ids_at = 100 } //dst

//view the exported SQL transform
command view2 = exec_jdbc ""  { 
	"SELECT * FROM ExportedTransEmployee"
	"SELECT * FROM ExportedTransDepartment"
}

//to import all the data in a SQL DB at once onto an auto-generated schema, use the following:
instance import_all_inst = import_jdbc_all "" 
//the schema can be obtained as
schema import_all_schema = schemaOf import_all_inst
//or
schema import_all_schema_2 = import_jdbc_all "" 


////////////////////////////////////////////////////////////
//
//execute command line actions as follows
//pragma cmdline1 = exec_cmdline  { 
//"ls -ltr"
//"echo hi"
//}
//
//execute actions as follows
//pragma js1 = exec_js  { 
//"javax.swing.JOptionPane.showMessageDialog(null, \"hello1\")"
//"javax.swing.JOptionPane.showMessageDialog(null, \"hello2\")"
//}

//store query evaluation as a view

query Q = literal : S -> S {
	entity
		Employee -> 
		{from e:Employee d:Department
		 where e.worksIn = d
		 attributes first -> e.manager.first 
		        last -> d.name 
		        age -> e.age
		        foreign_keys manager -> {e -> e.manager
		            d -> e.manager.worksIn}
		worksIn -> {d -> e.worksIn}
		}
		
		entity Department -> {from d:Department 
		               attributes name -> d.name
		               foreign_keys secretary -> {e -> d.secretary 
		              d -> d}}
		
	
		
}

command store_id_query = export_jdbc_query Q "" "Exported" "View"

command view_view = exec_jdbc ""  { 
	"SELECT * FROM \"ViewEmployee\""
	"SELECT * FROM \"ViewDepartment\""
	"DROP VIEW \"ViewEmployee\""
	"DROP VIEW \"ViewDepartment\"" //clean up is important when re-running the program
}
