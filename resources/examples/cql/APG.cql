options
	require_consistency=false
	prover = completion
	second_prover = completion 
	//prover = e
	//second_prover = e 

typeside B = literal {
	types
		T V
	constants
		String Double : T
		hello world "3.14" "2.78" : V                  
	functions
		typeOfV : V -> T	
	equations
		hello .typeOfV = String
		world .typeOfV = String
		"3.14".typeOfV = Double
		"2.78".typeOfV = Double	
}

typeside A = literal {
	imports
		B
	constants
		0 1 : T
		tt  : V
	functions	

		"*1" "+1" : T -> T
		pair1    : V -> V
		inj1 : V -> V
		
	
		"*2" "+2" : T,T -> T
		pair2    : V,V -> V
		inj2 : V,T -> V
//		...	
	equations
		typeOfV(tt) = 1
		
		forall v. typeOfV(pair1(v)) = "*1"(typeOfV(v))
		forall v. typeOfV(inj1 (v)) = "+1"(typeOfV(v))
		
		forall v v2. typeOfV(pair2(v,v2)) = "*2"(typeOfV(v),typeOfV(v2))
		forall v t . typeOfV(inj2 (v,t )) = "+2"(typeOfV(v),        t  )
}

schema APG = literal : A {
	entities
		E L	
	foreign_keys
		labelOfE : E -> L
	attributes	
		idOfL : L -> T
		idOfE : E -> V
		valueOfE : E -> V
		typeOfL  : L -> T
}

constraints C = literal : APG {
	forall e:E     where -> where e.idOfE.typeOfV = e.labelOfE.idOfL	
	forall e1 e2:E where e1.idOfE = e2.idOfE -> where e1 = e2
	forall l1 l2:L where l1.idOfL = l2.idOfL -> where l1 = l2
}

constraints CD = literal : APG {
	forall e:E where -> where e.valueOfE.typeOfV = e.labelOfE.typeOfL
}
/////////////////////////////////////////////////////////////////////////////////////

//Example 1 - Vertex

instance G1 = chase C literal : APG {
	generators
		Person Project : L
		v1 v2 : E
	equations
		Person .typeOfL = 1	
		Project.typeOfL = 1
		v1.labelOfE = Person
		v2.labelOfE = Project
		v1.valueOfE = tt
		v2.valueOfE = tt
}

schema   G1_S = pivot G1
mapping  G1_M = pivot G1
instance G1_I = pivot G1

command check1 = check CD G1
/////////////////////////////////////////////////////////////////////////////////////

//Example 2 - Edge

instance G2 = chase C literal : APG {
	generators
		Person Project created knows : L
		v1 v2 v3 e1 e2 : E
	equations
		Person .typeOfL = 1	
		Project.typeOfL = 1
		knows.typeOfL   = "*2"(Person.idOfL, Project.idOfL)
		created.typeOfL = "*2"(Person.idOfL, Person. idOfL)
		
		v1.labelOfE = Person
		v2.labelOfE = Person
		v3.labelOfE = Project
		e1.labelOfE = knows
		e2.labelOfE = created
		
		v1.valueOfE = tt
		v2.valueOfE = tt
		v3.valueOfE = tt
		e1.valueOfE = "pair2"(v1.idOfE, v3.idOfE)
		e2.valueOfE = "pair2"(v1.idOfE, v2.idOfE)
}

schema   G2_S = pivot G2
mapping  G2_M = pivot G2
instance G2_I = pivot G2

command check2 = check CD G2
/////////////////////////////////////////////////////////////////////////////////////

//Example 3 - Vertex Property

instance G3 = chase C literal : APG {
	generators
		Person name : L
		e p1 p2 : E
		
	equations
		Person.typeOfL = 1	
		name  .typeOfL = "*2"(Person.idOfL, String)
		
		p1.labelOfE = name
		p2.labelOfE = name
		e .labelOfE = Person
		
		e .valueOfE = tt
		p1.valueOfE = pair2(e.idOfE, hello)
		p2.valueOfE = pair2(e.idOfE, world)
}

schema   G3_S = pivot G3
mapping  G3_M = pivot G3
instance G3_I = pivot G3

command check3 = check CD G3
/////////////////////////////////////////////////////////////////////////////////////

//Example 4 - Edge Property

instance G4 = chase C literal : APG {
	generators
		Person knows weight : L
		v1 v2 e p : E
		
	equations
		Person.typeOfL = 1	
		knows .typeOfL = "*2"(Person.idOfL, Person.idOfL)
		weight.typeOfL = "*2"(knows.idOfL , Double)
		v1.labelOfE = Person
		v2.labelOfE = Person
		e .labelOfE = knows
		p .labelOfE = weight
		
		v1.valueOfE = tt
		v2.valueOfE = tt
		e .valueOfE = pair2(v1.idOfE, v2.idOfE)
		p .valueOfE = pair2(e .idOfE, "3.14"  )
}

schema   G4_S = pivot G4
mapping  G4_M = pivot G4
instance G4_I = pivot G4

command check4 = check CD G4
/////////////////////////////////////////////////////////////////////////////////////

//Example 5 - Type Aliases

instance G5 = chase C literal : APG {
	generators
		Degrees : L
		10 20 30 : E
		
	equations
		Degrees.typeOfL = Double	
		10.labelOfE = Degrees
		20.labelOfE = Degrees
		30.labelOfE = Degrees
		
		10.valueOfE = "3.14"
		20.valueOfE = "2.78"
		30.valueOfE = "2.78"		
}

schema   G5_S = pivot G5
mapping  G5_M = pivot G5
instance G5_I = pivot G5

command check5 = check CD G5
/////////////////////////////////////////////////////////////////////////////////////

//Example 6 - Complex Objects

instance G6 = chase C literal : APG {
	generators
		X : L
		Z : E
	equations
		X. typeOfL = "*2"(X.idOfL, X.idOfL)	
		Z.labelOfE = X
		Z.valueOfE = "pair2"(Z.idOfE, Z.idOfE)		
		//Z.valueOfE = "pair1"(Z.idOfE)				
}

schema   G6_S = pivot G6
mapping  G6_M = pivot G6
instance G6_I = pivot G6

command check6 = check CD G6



