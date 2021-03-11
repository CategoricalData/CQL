typeside Type = literal {
	external_types
		dom -> "java.lang.Object"
	external_parsers
		dom -> "x => x"
}

#####################################/

schema CoSpan = literal : Type {
	entities
		B C D
 	foreign_keys
		f: B -> D
		g: C -> D
	attributes
		B_att : B -> dom
		C_att : C -> dom
}

schema Square = literal : Type {
	imports
		CoSpan
	entities
		A
 	foreign_keys
		f2: A -> B
		g2: A -> C
	path_equations
		A.f2.f=A.g2.g
	attributes
		A_attB : A -> dom
		A_attC : A -> dom
}

instance I = literal : CoSpan {
	generators
		b1 b2 : B
		c1 c2 : C
		d1 d2 d3 : D
	equations
		b1.B_att = "b1"@dom
		b2.B_att = "b2"@dom
		c1.C_att = "c1"@dom
		c2.C_att = "c2"@dom
		b1.f = d1
		b2.f = d2
		c1.g = d1
		c1.g = d3
}

##################################################
#pullbacks, using queries

query pullback = literal : CoSpan -> Square {


entity	D -> {from d:D}

entity	B -> {from b:B
		 attributes B_att -> b.B_att
		 foreign_keys f -> {d -> b.f}}

entity	C -> {from c:C
		 attributes C_att -> c.C_att
		 foreign_keys g -> {d -> c.g}}

entity	A -> {from b0:B c0:C
		 where b0.f = c0.g
		 attributes A_attB -> b0.B_att
		 	   A_attC -> c0.C_att
		  foreign_keys
		  f2 -> {b -> b0}
		  g2 -> {c -> c0}
		 }


}

instance J = eval pullback I

transform t = counit_query pullback I

##################################################
#pullbacks, using constraints (EDs) and the chase

instance K = literal : Square {
	imports
		I
}

constraints pullbackED = literal : Square {
	forall b:B c:C
	where b.f = c.g
	->
	exists a:A
	where a.f2 = b  a.g2 = c
		b.B_att = a.A_attB
		c.C_att = a.A_attC
}

instance L = chase pullbackED K
