//In a relational data exchange setting, there are no function symbols or equations.
typeside Ty = literal {
	java_types
		dom = "java.lang.Object"
	java_constants
		dom = "return input[0]"
}


//////////////////////////////////////////////////////////////////////////

//Source relational schema. In a relational data exchange setting, source constraints are irrelevant
//and schemas have no foreign keys.
schema S = literal : Ty {
	entities
		SDeptEmp
	attributes
		SdeptId : SDeptEmp -> dom
		SmgrName : SDeptEmp -> dom
		SempId : SDeptEmp -> dom
} 

//target relational schema.  
schema T = literal : Ty {
	entities
		TDept TEmp
	attributes
		TempId : TEmp -> dom
		TwrksIn : TEmp -> dom
		TdeptId : TDept -> dom
		TmgrId : TDept -> dom
		TmgrName : TDept -> dom
} 

//To encode a relational data exchange setting in CQL, we must disjoint union the 
//source and target schemas together.
schema ST = literal : Ty {
	imports 
		S T
} 

//all EDs are expressed on the combined schema ST.  
constraints theEDs = literal : ST {
	//managers work in the departments they manage (stronger than FK)
	//Dept(d,m,n) -> Emp(m,d)
	forall d:TDept ->
 	exists e:TEmp 
 	where d.TdeptId = e.TwrksIn  d.TmgrId = e.TempId
 
	//every employee works in some department (FK)
	//Emp(e,d) -> exists M,N. Dept(d,M,N)
 	forall e:TEmp ->
	exists d:TDept
 	where d.TdeptId = e.TwrksIn	
 	
	//every DeptEmp is a department and an employee
	//DeptEmp(d,n,e) -> exists M. Dept(d, M, n) , Emp(e, d)
	forall de:SDeptEmp ->
	exists d:TDept e:TEmp
 	where de.SdeptId = d.TdeptId d.TdeptId = e.TwrksIn de.SdeptId = d.TdeptId de.SempId = e.TempId
} 

/////////////////////////////////////////////////////////////////////////

//test instance (on ST) DeptEmp(cs, alice, 1)
instance I = literal : ST {
	generators
		de:SDeptEmp
	equations
		de.SdeptId = cs de.SmgrName = alice de.SempId = "1"
}

//The three EDs are 'weakly acyclic'.  Thus we know any chase sequence will terminate in polynomial time.
//J = Dept(cs, null1, null2), Emp(1, cs), Emp(null2, cs)
//intuitively, null2 = alice's manager id, null3 = alice's manager's name 
instance J = chase theEDs I 

command JisOk1 = check theEDs J //verify the chased instance satisfies the EDs

//command InotOk = check theEDs I //the input instance does not satisfy the EDs

///////////////////////////////////////////////////////////////////////////
//primary key constraints

schema Sch = literal : Ty {
	entities
		E
	attributes
		att1 att2 att3 : E -> dom
} 

//says (att1,att2) is a primary key 
constraints PkForSch = literal : Sch {
	forall x y : E
	where x.att1 = y.att1  x.att2 = y.att3 
	->
	where x.att3 = y.att3
}

///////////////////////////////////////////////////////////////////////////
//see the pushout and pullback examples for more constraints

