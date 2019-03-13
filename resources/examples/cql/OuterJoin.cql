//Outer joins using java's Optional class

/*
This is not an outer join per se (it returns A*B rows, rather than A+B rows, 
when A and B don't join at all), but is similar in spirit.  The pedantic version follows.
*/

typeside Ty = literal {
	java_types
		String = "java.lang.String"
		Nat = "java.lang.Integer"
		Bool = "java.lang.Boolean"
		NullableNat = "java.util.Optional"
	java_constants
		String = "return input[0]"
		Nat = "return java.lang.Integer.parseInt(input[0])"
		Bool = "return java.lang.Boolean.parseBool(input[0])"
		NullableNat = "return java.lang.Integer.parseInt(input[0])"
	java_functions
		null : -> NullableNat = "return java.util.Optional.empty()"
		inNat : Nat -> NullableNat = "return java.util.Optional.of(input[0])"
		eqNat : Nat, Nat -> Bool = "return (input[0].equals(input[1]))"
		ifNat : Bool, NullableNat, NullableNat -> NullableNat = 
		  "if (input[0]) { return input[1]; } else { return input[2]; }"
}		  

schema S = literal : Ty {
	entities
		A B
	attributes
		Aname : A -> String
		Bname : B -> String
		Aid : A -> Nat
		Bid : B -> Nat
}

schema T = literal : Ty {
	entities
		C
	attributes
		CAname : C -> String
		CBname : C -> String
		Cid : C -> NullableNat
} 

query OuterJoin = literal : S -> T {
 	entity 
	 	C  -> {from a:A b:B
 			  attributes Cid -> ifNat(eqNat(a.Aid, b.Bid), inNat(a.Aid), null)
 			         CAname -> a.Aname
 		     	    CBname -> b.Bname
 		  	} 
}  

instance I = literal : S {
	generators
		a1 a2 : A
		b2 b3 : B
	equations
		a1.Aname = alice a1.Aid = "1"
		a2.Aname = bob a2.Aid = "2"
		b2.Bname = charlie b2.Bid = "2"
		b3.Bname = dave b3.Bid = "3"
} 

instance J = eval OuterJoin I

////////////////////////////////////////////////////////////////////////////////////////

schema Sx = literal : sql {
	entities
		Left Right Middle
	foreign_keys
		f: Left -> Middle g: Right -> Middle
	attributes
		attL: Left -> Varchar
		attR: Right -> Varchar
		attM: Middle -> Varchar
} 

instance Ix = literal : Sx {
	generators
		l1 l2 l3 l4: Left
		r1 r2: Right
		m1: Middle
	equations
		l1.f=l2.f l2.f=r1.g l3.f=r2.g
		l1.attL="Bob" l1.f.attM="5"
		l2.attL="Carl" l3.attL="Alice" 
		l3.f.attM="10" r1.attR="Happy"
		r2.attR="Sad" m1.attM="7"
		l4.attL="Dave" l4.f.attM="6"
} 

//Let's do a left outer join

//Step 1: query, remembering the join and the Left

//Step 1a: make the appropriate schema

schema Helper = literal : sql {
	entities
		Joiner Left
	foreign_keys
		h: Joiner->Left
	attributes
		attL: Left->Varchar
		attR: Joiner-> Varchar
		attM: Joiner -> Varchar
} 

//Step 1b: query it

query findJoin = literal : Sx -> Helper {
 entity
  Joiner -> {
  	from l:Left r:Right 
	where 
		l.f=r.g 
	attributes
		attM -> l.f.attM
		attR -> r.attR 
	foreign_keys
		h -> {l -> l } 
  }
 entity
  Left -> {
	from l:Left
	attributes attL->l.attL
  }
  }

//Step 2: add it all up

//Step 2a: make the landing schema

schema Landing = literal : sql {
	entities
		Result
	attributes
		attL: Result->Varchar
		attM: Result->Varchar
		attR: Result->Varchar
} 

//Step 2b: make a mapping from helper to landing

mapping AddItUp = literal : Helper -> Landing {
	entity
		Joiner -> Result
	foreign_keys
		h -> identity
	attributes
		attM -> attM
		attR -> attR
	entity
		Left -> Result
	attributes
		attL -> attL

}

//Step 3: run

instance Joined = eval findJoin Ix

instance OuterJoinx = sigma AddItUp Joined
