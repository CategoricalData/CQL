options
	always_reload = true
	
schema S0 = literal : sql {
	entities
		Employee
		Person
	foreign_keys
		eAsP : Employee -> Person
	attributes
		ssn : Person -> Integer
		eId : Employee -> Integer
		eSsn  : Employee -> Integer
	observation_equations
		eSsn = eAsP . ssn 					
}

/* 
 *  Person.csv is the file
 *   pId
 *   0
 *   1
 *   2
 *   
 *   Employee.csv is the file
 *    eId,is
 *    10,0
 *    11,1
 *    12,2
 */
command createCsvData = exec_js {
	"Java.type(\"catdata.Util\").writeFile(\"pId\\n0\\n1\\n2\", \"Person.csv\")"
	"Java.type(\"catdata.Util\").writeFile(\"eId,is\\n10,0\\n11,1\\n12,2\", \"Employee.csv\")"
}

instance I0 = import_csv "." : S0 {
	Employee -> {Employee -> eId   eAsP -> is    eSsn -> is}
	//eId -> eId can be ommitted
	
	Person -> {Person -> pId  ssn -> pId}
}


command exportCsvData = export_csv_instance I0 "exported" 
command exportCsvData2 = export_csv_transform (identity I0) "exported_trans.csv" 


//////////////////////////////////////////////


/* 
 * We show how to take the Person.csv file
 *   ssn,nameOf,Likes
 *   0,a,"a"
 *   1,b,"a,b,d"
 *   2,c ""
 *   
 * and create an CQL instance with 3 option-valued attributes, one per list element.  
 */


typeside Ty = literal {
	java_types
		String = "java.lang.String"
		List = "java.util.List"
		Option = "java.util.Optional"
		Integer = "java.lang.Integer"
	java_constants
		Integer = "return java.lang.Integer.parseInt(input[0])"
		String = "return input[0]"
		List = "return java.util.Arrays.asList(input[0].split(\",\"))"
		Option = "throw new RuntimeException()" //don't need to parse option-valued types
	java_functions
		at : List, Integer -> Option = "return (input[1] < input[0].size() ? java.util.Optional.of(input[0].get(input[1])) : java.util.Optional.empty())" 				
		of : String -> Option = "return java.util.Optional.of(input[0])"
		get : Option -> String = "return input[0].get()"
}

schema S = literal : Ty {
	entities
		Person
	attributes
		name : Person -> String
		likes: Person -> List 
}

command createCsvData2 = exec_js {
	"Java.type(\"catdata.Util\").writeFile(\"ssn,nameOf,Likes\\n0,a,a\\n1,b,\\\"a,b,d\\\"\\n2,c,\\\"\\\"\\n\", \"Person.csv\")"
}

instance I = import_csv "." : S {
	Person -> {
		Person -> ssn
		name -> nameOf
		likes -> Likes}
}

schema T = literal : Ty {
	imports 
		S
	attributes
		likes0: Person -> Option 
		likes1: Person -> Option
		likes2: Person -> Option 
	observation_equations
		forall p. at(likes(p),0) = likes0(p)
		forall p. at(likes(p),1) = likes1(p)
		forall p. at(likes(p),2) = likes2(p)
}

//J = sigma_{inclusion mapping} I
instance J = literal : T {
	imports 
		I
}

//////////////////////////////////////////////////////////

//Migrating the java-valued optional attributes onto foreign keys falls outside
//of the safe java fragment.  But here is a non-java solution of the 
//essence of the migration.


typeside Ty2 = literal {
	types
		String
		Option
	constants
		a b c d e  : String
		none : Option
	functions
		some : String -> Option
//		get : 
//	equations	
}

schema S2 = literal : Ty2 {
	entities
		X
	attributes
		_id : X -> String
		att : X -> Option	
}

schema T2 = literal : Ty2 {
	imports
		S2
	foreign_keys
		f : X -> X
	path_equations
		X.f.f = X.f	//needed for finiteness of J2
}

constraints C = literal : T2 {
	forall x1 x2 : X
	where att(x1) = some(_id(x2))
->
	where x1.f = x2	

	//todo: why can't this ED be used to get rid of the path equation in T2?
	//forall x : X
	//->
	//where x.f.f = x.f 
}

instance I2 = literal : T2 {
	generators
		x1 x2 x3 x4: X
	equations
		x1._id = a  x1.att = some(b)    
		x2._id = b  x2.att = none      
		x3._id = c  x3.att = some(e)   
		x4._id = d  x4.att = some(d)
}

//set the foreign keys to be the join of att and _id
instance J2 = chase C I2 

