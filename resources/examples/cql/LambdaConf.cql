
////////////////////////////////////////////////////////////////////////////////////////////////////
// Example schema and instance about employees

typeside Ty = literal {
	external_types
		String -> "java.lang.String"
		Int -> "java.lang.Integer"
	external_parsers
		String -> "x => x"
		Int -> "parseInt"
	external_functions
		plus : Int,Int -> Int = "(x, y) => x + y"
}

schema EmpSchema = literal : Ty {
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
//  	Employee.manager.manager = Employee.manager
  	attributes
  		first last	: Employee -> String
     	name 		: Department -> String
}

instance EmpInstance = literal : EmpSchema {
	generators
		101 102 103 : Employee
		q10 x02 : Department
	equations
		manager(101) = 103  worksIn(101) = q10  first(101) = Al    last(101) = Akin
		manager(102) = 102  worksIn(102) = x02  first(102) = Bob   last(102) = Bo
		manager(103) = 103  worksIn(103) = q10  first(103) = Carl  last(103) = Cork

		secretary(q10) = 101  name(q10) = Math
		secretary(x02) = 102  name(x02) = CS
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// example sigma/delta data migrations

schema S = literal : Ty {
	entities
		N1 N2
	foreign_keys
		f : N1 -> N2 //
	attributes
		name : N1 -> String
		salary : N1 -> Int
		age : N2 -> Int
}

schema T = literal : Ty {
	entities
		N
	attributes
		name : N -> String
		salary : N -> Int
		age : N -> Int
}

mapping F = literal : S -> T {
	entity
		N1 -> N
	foreign_keys
		f -> identity //
	attributes
		name -> name
		salary -> salary

	entity
		N2 -> N
	attributes
		age -> age
}

instance I = literal : S {
	generators
		1 2 3 : N1
		4 5 6 : N2
	equations
		name(1) = Alice  salary(1) = 100
		name(2) = Bob    salary(2) = 250
		name(3) = Sue    salary(3) = 300

		age(4) = 20  age(5) = 20  age(6) = 30

		f(1) = 4  f(2) = 5  f(3) = 6 //
}

instance sigmaFI = sigma F I
instance deltaFsigmaFI = delta F sigmaFI
transform unitSigmaFDeltaF = unit F I //is identity

query Q = literal : T -> S {
	entity
		N1 -> {from n1:N attributes name -> n1.name salary -> n1.salary
		foreign_keys f -> {n2 -> n1}}
	entity	N2 -> {from n2:N attributes age -> n2.age}

}

instance coevalQI = coeval Q I
instance evalcoevalQI = eval Q coevalQI

////////////////////////////////////////////////////////////////////////////////////////////////////
// select-from-where syntax to find the name of every manager's department

schema DeptNameSchema = literal : Ty {
	entities
		Manager
	attributes
		deptName : Manager -> String
}

query Query = literal : EmpSchema -> DeptNameSchema {
	entity
		Manager -> {from e:Employee
					attributes deptName -> e.manager.worksIn.name}
}

instance QueryResult = eval Query EmpInstance //has bag semantics

instance QueryResultDistinct = distinct QueryResult //has set semantics

////////////////////////////////////////////////////////////////////////////////////////////////////
// example richer constraint - name is a key for department

constraints NameIsDeptKey = literal : EmpSchema {
	forall d1 d2 : Department
	where name(d1) = name(d2)
	->
	where d1 = d2
}

command CheckNameIsDeptKey = check NameIsDeptKey EmpInstance //the constraint holds

////////////////////////////////////////////////////////////////////////////////////////////////////
// see 'Pharma Colim' example for schema and data integration
