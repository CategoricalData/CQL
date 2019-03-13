
schema S = literal : sql {
	entities
		O P T
 	foreign_keys
		f: O -> P
		g: O -> T
} 

schema S1 = literal : sql {
	entities 
		Observation Person Gender ObsType
	foreign_keys 
		f: Observation -> Person
		h: Person -> Gender
		g: Observation -> ObsType
	attributes	
	    att: Person -> String
	    att: Gender -> String
	    att: ObsType -> String
} 

schema S2 = literal : sql {
	entities 
		Observation Patient Method Type
	foreign_keys 
		f : Observation -> Patient
		g1: Observation -> Method
		g2: Method -> Type
	attributes
	    att: Patient -> String
	    att: Type -> String
}

mapping F1 = literal : S -> S1 {
	entity
		O -> Observation
	foreign_keys 
		f -> f
		g -> g
	entity
		P -> Person
	
	entity
		T -> ObsType
} 

mapping F2 = literal : S -> S2 {
	entity
		O -> Observation
	foreign_keys 
		f -> f
		g -> g1.g2
		
	entity	
		P -> Patient
	
	entity
		T -> Type
} 

///////////////////////////////////////////////////////////////////////////

//Pushout schema, written out explicitly here by hand
schema T = literal : sql {
	entities
		O P G T M
	foreign_keys 
		f : O -> P
		h : P -> G		
		g1: O -> M 
		g2: M -> T
	attributes	
		G_att : G -> String
		T_att1 T_att2: T -> String
		P_att1 P_att2: P -> String
}

mapping G1 = literal : S1 -> T {
	entity
		Observation -> O
	foreign_keys 
		f -> f 
		g -> g1.g2
	entity		
		Person -> P
	foreign_keys 
		h -> h
	attributes
		att  ->  P_att1
	entity
		ObsType -> T
	attributes
		att ->  T_att1
	entity	
		Gender -> G
	attributes
		att  ->  G_att
} 

mapping G2 = literal : S2 -> T {
	entity
		Observation -> O
	foreign_keys 
		f  -> f
		g1 -> g1
	entity	
		Patient -> P
	attributes
		att -> P_att2
	entity	
		Method -> M
	foreign_keys 
		g2 -> g2
	entity	
		Type -> T
	attributes
		att ->  T_att2	
} 

///////////////////////////////////////////////////////////////////////////

instance I1 = literal : S1 {
	generators
		one two three : Observation
		Peter Paul : Person
	    M F :Gender
	    BloodPressure BodyWeight HeartRate: ObsType
	equations
		Peter.att = Peter@String Paul.att = Paul@String
		M.att = M@String F.att = F@String
		BloodPressure.att = BloodPressure@String
		BodyWeight.att = BodyWeight@String
		HeartRate.att = HeartRate@String
		one.f = Peter two.f = Peter three.f = Paul
		Peter.h = M Paul.h = M
		one.g = BloodPressure two.g = BodyWeight three.g = HeartRate
} 

instance I2 = literal : S2 {
	generators
		o1 o2 o3 o4 : Observation
		Pete Jane : Patient
		m1 m2 m3 m4 : Method
		BP Wt : Type
	equations
	     Pete.att = Pete@String Jane.att = Jane@String
	     BP.att = BloodPressure@String Wt.att = BodyWeight@String
	     o1.f = Pete o2.f = Pete o3.f = Jane o4.f = Jane
	     o1.g1 = m1 o2.g1 = m2 o3.g1 = m3 o4.g1 = m1
	     m1.g2 = BP m2.g2 = BP m3.g2 = Wt m4.g2 = Wt
} 

instance I = literal : S {
	generators 
	  	P : P 
	  	B W : T
} 

transform a1 = literal : sigma F1 I -> I1 {
	generators 
		P -> Peter
		B -> BloodPressure
		W -> BodyWeight
} 

transform a2 = literal : sigma F2 I -> I2 {
	generators
		P -> Pete
		B -> BP 
		W-> Wt
} 

///////////////////////////////////////////////////////////////////////////

transform b1 = sigma G1 a1
transform b2 = sigma G2 a2

graph Span = literal {
	nodes
		A B C
	edges
		f : A -> B
		g : A -> C
}

instance answer = colimit Span T {
	nodes
		A -> dom_t b1
		B -> cod_t b1
		C -> cod_t b2
	edges
		f -> b1
		g -> b2
}
