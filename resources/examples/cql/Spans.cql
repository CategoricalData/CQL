schema S = literal : rdf {
	entities
		A B
	foreign_keys
		f : A -> B	
	attributes
		attA : A -> Dom
		attB : B -> Dom	
}
schema T = literal : rdf {
	entities
		W X Y Z
	foreign_keys
		g : W -> X 		h : X -> Y 		i : Y -> Z	
	attributes
		attW : W -> Dom	
		attX : X -> Dom	
		attY : Y -> Dom	
		attZ : Z -> Dom	
}
mapping M = literal : S -> T {
	entity
		A -> W
	foreign_keys
		f -> g.h.i
	attributes
		attA -> attW	
	entity
		B -> Z	
	attributes
		attB -> attZ		
}

instance I = literal : rdf {
	generators
		r1 r2 r3 t0 t1 t2 t3 u1 u2 u3 u4: R
	equations
		r1.subject = "a1"	r1.predicate = "f"		r1.object = "b1"	
		r2.subject = "a2"	r2.predicate = "f"		r2.object = "b2"	
		r3.subject = "a2"	r3.predicate = "f"		r3.object = "b1"	

		u1.subject = "a1"	u1.predicate = "attA"	u1.object = "Alice"
		u2.subject = "a2"	u2.predicate = "attA"	u2.object = "Ann"
		u3.subject = "b1"	u3.predicate = "attB"	u3.object = "Bob"
		u4.subject = "b2"	u4.predicate = "attB"	u4.object = "Bill"
		
		t0.subject = "a1"	t0.object = "A"	
		t0.predicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
		t1.subject = "a2"	t1.object = "A"	
		t1.predicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
		t2.subject = "b1"	t2.object = "B"	
		t2.predicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
		t3.subject = "b2"	t3.object = "B"	
		t3.predicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
}

schema S0 = spanify S
schema T0 = spanify T

query Q1 = spanify S
query Q2 = spanify T

query Q = spanify_mapping M

instance J = eval Q1 I
instance K = coeval Q J
instance L = coeval Q2 K

////////////////////////////



schema X = literal : rdf {
	entities
		W 
	attributes
		attW : W -> Dom	
}
mapping F = literal : S -> T {
	entity
		A -> W
	foreign_keys
		f -> identity
	attributes
		attA -> attW	
	entity
		B -> W	
	attributes
		attB -> attW		
}

schema Y = spanify X

query U = spanify_mapping F

instance J2 = eval Q1 I
instance K2 = coeval U J2
instance L2 = coeval Q2 K2

