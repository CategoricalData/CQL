/*
Problem 1:

Find a sub-database (i.e., populate fresh Employee and Department tables,
including constraints) where

 1) every employee works in the same department as their manager
 2) every department's secretary works in that department

Schema:

DROP TABLE IF EXISTS Employee;
DROP TABLE IF EXISTS Department;

CREATE TABLE Employee(
id INT PRIMARY KEY,
name VARCHAR(255),
manager INT,
worksIn INT
);

CREATE TABLE Department(
id INT PRIMARY KEY,
name VARCHAR(255),
secretary INT,
);

ALTER TABLE Employee ADD CONSTRAINT e1
FOREIGN KEY (manager) REFERENCES Employee (id);

ALTER TABLE Employee ADD CONSTRAINT e2
FOREIGN KEY (worksIn) REFERENCES Department (id);

ALTER TABLE Department ADD CONSTRAINT d1
FOREIGN KEY (secretary) REFERENCES Employee (id);

Example input DB:

INSERT INTO Employee VALUES
(101, 'Alan', 103, 10),
(102, 'Camille', 102, 2),
(103, 'Andrey', 103, 10);

INSERT INTO Department VALUES
(10, 'Applied Math', 101),
(2, 'Pure Math', 102);
*/


////////////////////////////////////////////////////////////////////////////////////////////////////
// Example schema and instance about employees

typeside Ty = literal {
	external_types
		String -> "java.lang.String"
		Int -> "java.lang.Integer"
	external_parsers
		String -> "x => x"
		Int -> "parseInt"
}

schema S = literal : Ty {
	entities
		Employee
		Department
	foreign_keys
		manager   : Employee -> Employee
		worksIn   : Employee -> Department
		secretary : Department -> Employee
 	attributes
  		first 	: Employee -> String
     	name 	: Department -> String
}

schema T = literal : Ty {
	imports
		S
	path_equations
		Employee.manager.worksIn = Employee.worksIn
  		Department.secretary.worksIn = Department
}

/* Error in query bad: Target equation v.manager.worksIn = v.worksIn not respected:
   transforms to v.manager.worksIn = v.worksIn, which is not provable in the sub-query for Employee
  *//*
query bad = literal : S -> T {
	entity Employee -> {from
		v : Employee
	attributes
		first -> v.first
	foreign_keys
		manager -> {v -> v.manager}
		worksIn -> {v -> v.worksIn}
	}

	entity Department -> {from
		v : Department
	attributes
		name -> v.name
	foreign_keys
		secretary -> {v -> v.secretary}
	}
} */

query good1 = literal : S -> T {
	entity Employee -> {from
		e : Employee
	where
		e.manager = e
		e.worksIn.secretary = e
	attributes
		first -> e.first
	foreign_keys
		manager -> {e -> e.manager}
		worksIn -> {d -> e.worksIn}
	}

	entity Department -> {from
		d : Department
	where
		d.secretary.worksIn = d
		d.secretary.manager = d.secretary
	attributes
		name -> d.name
	foreign_keys
		secretary -> {e -> d.secretary}
	}
}

instance I = literal : S {
	generators
		101 102 103 : Employee
		2 10 : Department
	multi_equations
		manager   -> {101 103, 102 102, 103 103}
		worksIn   -> {101 10, 102 2, 103 10}
		secretary -> {10 101, 2 102}
 		first 	-> {101 Alan, 102 Camille, 103 Audrey}
     	name 	-> {10 "Applied Math", 2 "Pure Math"}
}

instance J = eval good1 I
