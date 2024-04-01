
schema C = literal : sql {
 entities 
	a1 a2 a3 b1 b2 c1 c2 c3 c4

 foreign_keys
	g1 : a1 -> b1 
	g2 : a2 -> b2 
	g3 : a3 -> b2
	h1 : a1 -> c1 
	h2 : a2 -> c2 
	h3 : a3 -> c4
	
 attributes
	a1_str : a1 -> Varchar
 	a2_str : a2 -> Varchar
 	a3_str : a3 -> Varchar
	
	b1_str : b1 -> Varchar
 	b2_str : b2 -> Varchar

 	c1_str : c1 -> Varchar
 	c2_str : c2 -> Varchar
 	c3_str : c3 -> Varchar
 	c4_str : c4 -> Varchar
}

schema D = literal : sql {
 entities 
 	A B C
 foreign_keys
 	G : A -> B 
 	H : A -> C
  attributes
	A_str : A -> Varchar
 	B_str : B -> Varchar
 	C_str : C -> Varchar	
}

mapping F = literal : C -> D {
  entity
  	a1 -> A 
  foreign_keys
  	h1 -> H 
  	g1 -> G 
  attributes
  	a1_str -> A_str 
  	
 entity
	a2 -> A 
 foreign_keys
	h2 -> H 
	g2 -> G 
 attributes
	a2_str -> A_str 

  entity
  	a3 -> A
  foreign_keys
	g3 -> G
  	h3 -> H
  attributes
  	a3_str -> A_str

  entity
  	b1 -> B
  attributes
  	b1_str -> B_str 

  entity
  	b2 -> B
  attributes
  	b2_str -> B_str

  entity
  	c1 -> C 
  attributes
  	c1_str -> C_str 

  entity
  	c2 -> C 
  attributes
  	c2_str -> C_str

  entity
  	c3 -> C 
  attributes
  	c3_str -> C_str 

  entity
  	c4 -> C
  attributes
  	c4_str -> C_str
} 

instance I = random : C {
	generators
		a1 -> 10
		a2 -> 10
		a3 -> 10
		b1 -> 10
		b2 -> 10
		c1 -> 10
		c2 -> 10
		c3 -> 10
		c4 -> 10
	#options
	#	random_seed = 2
}

instance J = sigma_chase F I

instance J2 = sigma F I
