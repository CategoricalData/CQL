
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


///////////////////////////////////////////////////////////////////////////
//compute the canonical schema colimit

schema_colimit ColimAuto = quotient S1 + S2 : sql {
	entity_equations
		S1.Observation = S2.Observation
		S1.Person = S2.Patient
		S1.ObsType = S2.Type
	path_equations
		Observation.S1_Observation_f = Observation.S2_Observation_f
		Observation.g = Observation.g1.g2
	options
		simplify_names = true	
}


//just for fun
schema_colimit Coproduct = coproduct S1 + S2 : sql 

///////////////////////////////////////////////////////////////////////////
//use a different, less verbose colimit constructed manually

schema_colimit Colim = modify ColimAuto {
	rename entities
		Observation -> O
//		Patient__Person -> P  done because of shorted prefix!
		ObsType__Type -> T
		Gender -> G
		Method -> M
 	rename foreign_keys
		O.S1_Observation_f -> f
	rename attributes
		T.ObsType_att  -> T_att1
		//G.Gender_att  -> G_att
		P.Person_att -> P_att1
		T.Type_att  -> T_att2
		P.Patient_att -> P_att2
	remove foreign_keys	
		O.S2_Observation_f  -> f
		O.g  -> g1.g2 
	/* e.g. remove attributes
		B_att -> lambda x. x.T_att1 */
}


///////////////////////////////////////////////////////////////////////////

instance I1 = literal : S1 {
	generators
		one two three : Observation
		Peter Paul : Person
	    	M F :Gender
	    	BloodPressure BodyWeight HeartRate: ObsType
	equations
		Peter.att = xPeter Paul.att = xPaul
		M.att = xM F.att = xF
		BloodPressure.att = xBloodPressure
		BodyWeight.att = xBodyWeight
		HeartRate.att = xHeartRate
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
	     Pete.att = xPete Jane.att = xJane
	     BP.att = xBloodPressure Wt.att = xBodyWeight
	     o1.f = Pete o2.f = Pete o3.f = Jane o4.f = Jane
	     o1.g1 = m1 o2.g1 = m2 o3.g1 = m3 o4.g1 = m1
	     m1.g2 = BP m2.g2 = BP m3.g2 = Wt m4.g2 = Wt
} 

instance I1Fwd = sigma (getMapping Colim S1) I1 
instance I2Fwd = sigma (getMapping Colim S2) I2 
instance CoProd = literal : getSchema Colim {
	imports I1Fwd I2Fwd
}

instance E = literal : getSchema Colim {
	generators 
	  	P : P 
	  	B W : T
} 

transform a1 = literal : E -> CoProd {
		generators 
			P -> Peter
			B -> BloodPressure
			W -> BodyWeight
} 

transform a2 = literal : E -> CoProd {
		generators
			P -> Pete
			B -> BP 
			W -> Wt
} 

instance answer = coequalize a1 a2 
