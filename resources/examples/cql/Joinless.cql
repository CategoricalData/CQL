typeside Ty = literal { 
	java_types
		String = "java.lang.String"
	java_constants
		String = "return input[0]"
}

schema Schools = literal : Ty {
	entities 
		Person
		School
		Dept
	foreign_keys
	     instituteOf : Person -> School 
	     deptOf      : Person -> Dept
	     biggestDept : School -> Dept
	attributes
		lastName    : Person -> String
	     schoolName  : School -> String
	     deptName    : Dept   -> String
}

instance BostonSchools = literal : Schools {
	generators 
		ryan david adam greg gregory jason : Person
	     harvard mit : School
	     math cs : Dept
	multi_equations 
		lastName -> {ryan Wisnesky, david Spivak, adam Chlipala, greg Morrisett, gregory Malecha, jason Gross}
		schoolName -> {harvard Harvard, mit MIT}
		deptName -> {math Mathematics, cs CompSci}
		instituteOf -> {ryan harvard, david mit, adam mit, greg harvard, gregory harvard, jason mit}
		deptOf -> {ryan math, david math, adam cs, greg cs, gregory cs, jason cs}
		biggestDept -> {harvard math, mit cs}
}

schema Person = literal : Ty {
	entities 
		Person
	attributes
		lastName   : Person -> String
		schoolName : Person -> String
}

//Find all the people whose school's biggest department is Mathematics
query BiggestDeptIsMathQuery = literal : Schools -> Person {
	entity
		Person -> {from   p:Person
				 where  p.instituteOf.biggestDept.deptName = Mathematics
				 attributes lastName -> p.lastName 
			  		   schoolName -> p.instituteOf.schoolName}
} 

//simpler, dynamic version of the above query
query BiggestDeptIsMathQuery_simple = simple : Schools  {
	from   p:Person
	where  p.instituteOf.biggestDept.deptName = Mathematics
    attributes lastName -> p.lastName 
    schoolName -> p.instituteOf.schoolName
} 

instance BiggestDeptIsMathInBoston = eval BiggestDeptIsMathQuery BostonSchools

