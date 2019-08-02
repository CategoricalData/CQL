//Algebraic property graphs, joint with Josh at Uber
options 
	prover = completion
	second_prover = completion
	require_consistency=false

typeside B = literal {
	types
		T 
		V 
	constants
		String Double : T
		hello world "3.14" "2.78" : V 
	functions
		tau : V -> T
	equations
		hello .tau = String
		world .tau = String
		"3.14".tau = Double
		"2.78".tau = Double		
}

typeside A_B = literal {
	imports
		B
	constants
		0 1   : T
		"()"  : V
	functions	
		"*" "+" : T,T -> T
		","     : V,V -> V
		inl inr : V,T -> V
	equations
		tau("()") = 1
		forall v u. tau((v "," u)) = (tau(v) "*" tau(u))
		forall v t. tau(inl (v,t)) = (tau(v) "+"     t )	
		forall v t. tau(inr (v,t)) = (t      "+" tau(v))		
}

schema preAPG = literal : A_B {
	entities
		E L	
	foreign_keys
		"lambda"        : E -> L
	attributes	
		"sigma" kappa   : L -> T
		phi     kappa   : E -> V	
}

constraints APG = literal : preAPG {
	forall e:E  -> where e.kappa.tau = e."lambda".kappa	
	forall e:E  -> where e.phi.tau   = e."lambda"."sigma"
	forall e1 e2:E where e1.kappa = e2.kappa -> where e1 = e2
	forall l1 l2:L where l1.kappa = l2.kappa -> where l1 = l2
}

/////////////////////////////////////////////////////////////////////////////////////

//Example 1 - Vertex

instance G1 = literal : preAPG {
	generators
		Person Project : L
		v1 v2 : E
	equations
		Person ."sigma" = 1	
		Project."sigma" = 1
		v1."lambda" = Person
		v2."lambda" = Project
		v1.phi = "()"
		v2.phi = "()"
		v1.kappa.tau = Person.kappa
		v2.kappa.tau = Project.kappa
}
command check1 = check APG G1

//schema   G1_S = pivot G1
//mapping  G1_M = pivot G1
//instance G1_I = pivot G1

/////////////////////////////////////////////////////////////////////////////////////

//Example 2 - Edge

instance G2 = literal : preAPG {
	generators
		Person Project created knows : L
		v1 v2 v3 e1 e2 : E
	equations
		Person ."sigma" = 1	
		Project."sigma" = 1
		knows."sigma"   = "*"(Person.kappa, Project.kappa)
		created."sigma" = "*"(Person.kappa, Person .kappa)
		
		v1."lambda" = Person
		v2."lambda" = Person
		v3."lambda" = Project
		e1."lambda" = knows
		e2."lambda" = created
		
		v1.phi = "()"
		v2.phi = "()"
		v3.phi = "()"
		e1.phi = (v1.kappa "," v3.kappa)
		e2.phi = (v1.kappa "," v2.kappa)

		v1.kappa.tau = Person.kappa
		v2.kappa.tau = Person.kappa
		v3.kappa.tau = Project.kappa
		e1.kappa.tau = knows.kappa
		e2.kappa.tau = created.kappa
}
command check2 = check APG G2

schema   G2_S = pivot G2
mapping  G2_M = pivot G2
instance G2_I = pivot G2


/////////////////////////////////////////////////////////////////////////////////////

//Example 3 - Vertex Property

instance G3 = literal : preAPG {
	generators
		Person name : L
		e p1 p2 : E
	equations
		Person."sigma" = 1	
		name  ."sigma" = (Person.kappa "*" String)
		
		p1."lambda" = name
		p2."lambda" = name
		e ."lambda" = Person
		
		e .phi = "()"
		p1.phi = (e.kappa "," hello)
		p2.phi = (e.kappa "," world)

		e .kappa.tau = Person.kappa
		p1.kappa.tau = name.kappa
		p2.kappa.tau = name.kappa
}
command check3 = check APG G3
//schema   G3_S = pivot G3
//mapping  G3_M = pivot G3
//instance G3_I = pivot G3


/////////////////////////////////////////////////////////////////////////////////////

//Example 4 - Edge Property

instance G4 = literal : preAPG {
	generators
		Person knows weight : L
		v1 v2 e p : E
		
	equations
		Person."sigma" = 1	
		knows ."sigma" = (Person.kappa "*" Person.kappa)
		weight."sigma" = (knows.kappa "*" Double)
		
		v1."lambda" = Person
		v2."lambda" = Person
		e ."lambda" = knows
		p ."lambda" = weight
		
		v1.phi = "()"
		v2.phi = "()"
		e .phi = (v1.kappa "," v2.kappa)
		p .phi = (e .kappa "," "3.14"  )

		v1.kappa.tau = Person.kappa
		v2.kappa.tau = Person.kappa
		e .kappa.tau = knows.kappa
		p .kappa.tau = weight.kappa
}
command check4 = check APG G4

//schema   G4_S = pivot G4
//mapping  G4_M = pivot G4
//instance G4_I = pivot G4
/////////////////////////////////////////////////////////////////////////////////////

//Example 5 - Type Aliases

instance G5 = literal : preAPG {
	generators
		Degrees : L
		10 20 30 : E
		
	equations
		Degrees."sigma" = Double	
		
		10."lambda" = Degrees
		20."lambda" = Degrees
		30."lambda" = Degrees
		
		10.phi = "3.14"
		20.phi = "2.78"
		30.phi = "2.78"		

		10.kappa.tau = Degrees.kappa
		20.kappa.tau = Degrees.kappa
		30.kappa.tau = Degrees.kappa
}
command check5 = check APG G5
//schema   G5_S = pivot G5
//mapping  G5_M = pivot G5
//instance G5_I = pivot G5

/////////////////////////////////////////////////////////////////////////////////////

//Example 6 - Complex Objects

instance G6 = literal : preAPG {
	generators
		X : L
		Z : E
	equations
		X. "sigma" = "*"(X.kappa, X.kappa)	
		Z."lambda" = X
		Z.phi = ","(Z.kappa, Z.kappa)	
		Z.kappa.tau = X.kappa	
		//Z.phi = "pair1"(Z.idOfE)				
}
command check6 = check APG G6

//schema   G6_S = pivot G6
//mapping  G6_M = pivot G6
//instance G6_I = pivot G6





