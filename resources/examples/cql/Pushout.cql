typeside Type = literal {
	java_types	
		dom = "java.lang.Object"
	java_constants
		dom = "return input[0]"
}

///////////////////////////////////////////////////////////////////////////

schema Span = literal : Type {
	entities
		A B C
 	foreign_keys
		f: A -> B
		g: A -> C
	attributes
		C_att : C -> dom
		B_att : B -> dom	
} 

schema Square = literal : Type {
	imports
		Span
	entities
		D
 	foreign_keys
		f2: B -> D
		g2: C -> D
	path_equations
		D.f.f2=D.g.g2
	attributes
		D_attB : D -> dom
		D_attC : D -> dom	
} 

instance J = literal : Span {
	generators
		a : A
		b1 b2 : B
		c1 c2 : C
	equations
		b1.B_att = "b1"@dom
		b2.B_att = "b2"@dom
		c1.C_att = "c1"@dom
		c2.C_att = "c2"@dom
		a.f = b1 
		a.g = c1
} 

////////////////////////////////////////////////////////////////////////////////////////////////////
//pushouts, using queries

query pushout = literal : Square -> Span {
 
 		
entity	B -> {from b:B 
		 attributes B_att -> b.f2.D_attB}

entity	C -> {from c:C 
		 attributes C_att -> c.g2.D_attC}

entity	A -> {from a:A
			foreign_keys		 	   
			f -> {b -> a.f}
			g -> {c -> a.g}} 

}

instance I = coeval pushout J

transform t = unit_query pushout J
