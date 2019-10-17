
typeside TyJava = literal { 
	java_types
		string = "java.lang.String"
		nat = "java.lang.Integer"
	java_constants
		string = "return input[0]"
		nat = "return java.lang.Integer.parseInt(input[0])"
	java_functions
		plus : nat,nat -> nat = "return (input[0] + input[1]).intValue()"
}

schema SJava = literal : TyJava {
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
  		first last	: Employee -> string
     	age			: Employee -> nat
     	cummulative_age: Employee -> nat
     	name 		: Department -> string
     observation_equations
     	forall e. cummulative_age(e) = plus(age(e), age(manager(e)))
     options
     	prover_simplify_max = 64	
}

instance IJava = literal : SJava {
	generators 
		a b c : Employee
		m s : Department
	equations 
		first(a) = Al
		first(b) = Bob  last(b) = Bo
		first(c) = Carl 
		name(m)  = Math name(s) = CS
		age(a) = age(c) 
		manager(a) = b manager(b) = b manager(c) = c
		worksIn(a) = m worksIn(b) = m worksIn(c) = s
		secretary(s) = c secretary(m) = b 
		secretary(worksIn(a)) = manager(a)
		worksIn(a) = worksIn(manager(a))
		age(a) = "2" 
		age(manager(a)) = "1"
	 options
     	prover_simplify_max = 64		
}

//basic query syntax
query Q = simple : SJava {
	from e:Employee
	where e.first = Al
	attributes deptName -> e.worksIn.name
	options
		simple_query_entity = Z
     	prover_simplify_max = 64
}

instance J = eval Q IJava

////////////////////////////////////////////////////////////////////////////////////////////////////
//demonstrates 'interpret as instance' option.   which interprets the instance as a model, 
//similar to JDBC / CSV import.  This behavior can be useful when writing down an 
//instance that is already saturated and for which one wants to check the schema constraints, 
//rather than impose them  

schema SS = literal : empty {
    entities
        Person
    foreign_keys
    	mother father : Person -> Person
    path_equations
        Person.mother = Person.father
}

//works; sets b = c.  This is normal ‘theory’ or ‘initial algebra' semantics
instance I1 = literal : SS {
        generators
                a b c : Person
        equations
                a.mother = b
                a.father = c

                b.mother = b b.father = b
                c.mother = c c.father = c

                //fails because b != c.  This is 'algebra' semantics
  		//options
          		//interpret_as_algebra = true
}          		

/////////////////////////////////////////////////////////////////

typeside Ty = literal { 
	types
 		string 
 		nat
	constants
 		Al Akin Bob Bo Carl Cork Dan Dunn Math CS : string
 		zero : nat
 	functions 		
	 	succ		: nat -> nat
	 	plus		: nat, nat -> nat
	equations  
	 	forall x. plus(zero, x) = x
	 	forall x, y. plus(succ(x),y) = succ(plus(x,y))
	options
		prover = completion
}

schema S = literal : Ty {
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
  	attributes
  		first last	: Employee -> string
     	age			: Employee -> nat
     	cummulative_age: Employee -> nat
     	name 		: Department -> string
     observation_equations
     	forall e. cummulative_age(e) = plus(age(e), age(manager(e)))
	options
		prover = completion
}

instance I = literal : S {
	generators 
		a b c : Employee
		m s : Department
	equations 
		first(a) = Al
		first(b) = Bob  last(b) = Bo
		first(c) = Carl 
		name(m)  = Math name(s) = CS
		age(a) = age(c) 
		manager(a) = b manager(b) = b manager(c) = c
		worksIn(a) = m worksIn(b) = m worksIn(c) = s
		secretary(s) = c secretary(m) = b 
		secretary(worksIn(a)) = manager(a)
		worksIn(a) = worksIn(manager(a))
		age(a) = zero.succ.succ 
		age(manager(a)) = zero.succ
	options
		prover = completion
		second_prover = completion
		completion_precedence = "a b c m s manager worksIn secretary"
} 


