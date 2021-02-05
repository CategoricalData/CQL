/* see options button to disable spellcheck or outline */

//global options at start of file, see manual or options button
options
	timeout = 30

//////////// Kind comment

html { (* "some html" *) } //be sure to escape slashes and quotes
md { (* "some markdown" *) }

//////////// Kind graph

graph g1 = literal {
	nodes
		n1 n2
	edges
		f : n1 -> n2
}

//////////// Kind typeside

typeside ty1 = empty
typeside ty2 = sql
typeside ty3 = typesideOf (empty : ty1)
typeside ty4 = literal {
	imports //order of sections (imports, types, ...) matters, but sections can be omitted
		ty1
	types
		Nat
	constants
		zero : Nat
	functions
		succ : Nat -> Nat
		plus : Nat, Nat -> Nat
	external_types
	 	String -> "java.lang.String"
	external_parsers
	 	String -> "x => x"
	external_functions
	 	append : String, String -> String = "(x, y) => x.concat(y)"
	equations
		forall x. plus(zero, x) = x
	 	forall x, y. plus(succ(x),y) = succ(plus(x,y))
	 options
	 	prover = completion
}

//////////// Kind schema

schema s1 = empty : ty2
schema s2 = schemaOf (empty : s1)
schema s3 = literal : ty4 {
	imports
		s1
	entities
		Employee
		Department
	foreign_keys
		manager   : Employee -> Employee
		worksIn   : Employee -> Department
		secretary : Department -> Employee
	path_equations
		Employee.manager.manager = Employee.manager
		Employee.manager.worksIn = Employee.worksIn
  		Department.secretary.worksIn = Department
  	attributes
  		ename	: Employee -> String
     	age			: Employee -> Nat
     	cummulative_age: Employee -> Nat
     	dname 		: Department -> String
     observation_equations
     	forall e. cummulative_age(e) = plus(age(e), age(manager(e)))
	options
		prover = completion
}
schema ss = prefix s3 "hi"
// getSchema - see Tutorial\ TSP.aql for an example

//////////// Kind mapping

mapping m0 = include s1 s1
mapping m1 = identity s1
mapping m2 = [m1 ; m1]
mapping m3 = literal : s3 -> s3 {
	entity
		Employee -> Employee
	foreign_keys
		manager -> manager.manager
		worksIn -> worksIn
	attributes
		ename -> manager.ename
		age -> manager.age
		cummulative_age -> lambda e. (age(e) plus age(e))

	entity
		Department -> Department
	foreign_keys
		secretary -> secretary
	attributes
		dname -> dname

	options
		dont_validate_unsafe = true // this isn't a real mapping
}
//getMapping - see FinanceColim2
mapping ma = to_prefix s3 "hi"
mapping mb = from_prefix s3 "hi"

//////////// Kind query

query q1 = identity s1
query q0 = include s1 s1
query q2 = literal : s3 -> s3 {
	entity
		Employee -> {from e:Employee d:Department
		 		      where e.worksIn = d
		 		      attributes ename -> e.ename
		 		      	     age -> e.age
		 		      	     cummulative_age -> e.cummulative_age
		 		      foreign_keys
		 		      manager -> {e -> e.manager d -> d}
					worksIn -> {d -> e.worksIn}
   }
	entity	Department -> {from d:Department
						attributes dname -> d.dname
						foreign_keys secretary -> {d -> d e -> d.secretary} }
	options
		dont_validate_unsafe = true // this isn't a real query
}
query q3 = simple : s3 {
	from e:Employee
	where e.ename = bill
	attributes * mgrName -> e.manager.ename
	options
		prover = completion
}
//eval q4 ~ delta m3 and coeval q4 ~ sigma m3
query q4 = toQuery m3 { options dont_validate_unsafe = true } // this isn't a real query

//coeval q4 ~ delta (id s3) and eval q4 ~ pi (id s3)
query q5 = toCoQuery identity s2 {
	options
		prover = completion
		query_remove_redundancy = false //this increases the proving burden on this example
}
query q6 = [ identity s2 ; identity s2 ]
query q7 = fromCoSpan m0 m0
schema s4 = dom_q q1
schema s5 = cod_q q1
schema s6 = dom_m m1
schema s7 = cod_m m1
//import_jdbc_all - see JDBCSQL example

//////////// Kind instance
instance i1 = empty : s3
instance i0 = except (empty : s1) (empty : s1)
instance i2 = i1 //distinct i1
instance i3 = dom_t (identity i1)
instance i4 = cod_t (identity i1)
instance i5 = eval (identity s3) i1
instance i6 = coeval (identity s3) i1 { options prover = completion  second_prover = completion}
instance i7 = sigma (identity s3) i1 { options prover = completion second_prover = completion}
instance i8 = delta (identity s3) i1
instance i20 = coproduct i1 + i2 : s3 { options prover = completion }
instance i11 = coequalize (identity i1) (identity i1) { options prover = completion  second_prover = completion}
instance i12 = chase literal : s3 { } i3
instance i15 = anonymize i12
instance i16 = frozen q3 Q
instance i17 = pi (identity s2) (empty : s2) { options prover = completion second_prover = completion}
instance i18 = cascade_delete i1 : s3
//random -- see delta example
//import_jdbc -- see JDBC example
//import_jdbc_all -- see JDBC example
//quotient_jdbc -- see JDBC example
//quotient_csv -- see JDBC example (yes, JDBC example)
//import_csv -- See FinanceColim1 example
//quotient_query - see Link example

instance i13 = literal : s3 {
	generators
		e : Employee
	equations
		e.manager = e
		e.worksIn.secretary = e
	multi_equations
		ename -> {e Al, e.manager Al}
	options
		prover = completion
		second_prover = completion
}



//////////// Kind transform

transform ta = except_return (empty : s1) (empty : s1)
transform tb = except (identity (empty : s1)) (empty : s1)
transform t0 = include i7 i7
transform t1 = identity i1
transform t2 = [t1 ; t1]
transform t3 = distinct (identity empty : s1)
transform t3x= distinct_return empty : s1
transform t4 = eval (identity s3) t1
//the { options } blocks are optional
transform t5 = coeval (identity s3) t1 { options prover = completion second_prover = completion} { options prover = completion second_prover = completion}
transform t6 = sigma (identity s3) t1 { options prover = completion second_prover = completion} { options prover = completion second_prover = completion}
transform t6x= pi (identity s1) (identity (empty : s1)) { options prover = completion second_prover = completion} { options prover = completion second_prover = completion }
transform t7 = delta (identity s3) t1
transform t8 = unit (identity s3) i1 { options prover = completion second_prover = completion}
transform t9 = counit (identity s3) i1 { options prover = completion second_prover = completion}
transform t10= counit_query (identity s3) i1 { options prover = completion second_prover = completion}
//import_jdbc - see JDBC example
//import_csv - see Finance Colim example
transform t11= literal : i13 -> i13 {
	generators
		e -> e.manager
}
//frozen transform - see compose example

//////////// Kind constraints
constraints c0 = empty : s3
constraints c1 = literal : s3 {
	forall e:Employee d:Department
	where e.worksIn = d
	->
	exists e0:Employee
	where e0.ename = e.ename

	forall e:Employee
	->
	where e = e

	options prover = completion
	 prover_simplify_max = 64
}

//////////// Kind pragma

command p1 = check c1 i3 //check instance against constraints
command p2 = assert_consistent i3
command p3 = exec_cmdline {
//	"echo hello"
//	"echo world"
}
command p4 = exec_js {
	"1+2;"
	"2+3;"
}
query q8 = fromConstraints 0 c1
constraints cx = fromSchema s1

//check_query -- see Query ED example

//exec_jdbc - see JDBC example
//export_jdbc_instance - see JDBC example
//export_jdbc_query - see JDBC example
//export_jdbc_transform - see JDBC example

//export_instance_csv - see FinanceColim1 example
//export_transform_csv - see FinanceColim1 example


//////////// Kind colimit_schema

//quotient - see PharmaColim2
//coproduct - see PharmaColim2
//modify - see PharmaColim2
//colimit - unnecessarily verbose; recommend quotient instead
//wrap - see manual; recommend quotient instead
