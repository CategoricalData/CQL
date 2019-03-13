typeside Ty = literal { 
	java_types
		String = "java.lang.String"
	java_constants
		String = "return input[0]"
}

schema NormalizedSchema = literal : Ty {
	entities
		Male 
		Female
	foreign_keys
		mother : Male -> Female
  	attributes
  		female_name : Female -> String
  		male_name : Male -> String 
}

instance NormalizedData = literal : NormalizedSchema {
	generators
		Al Bob Charlie : Male
		Ellie Fran : Female
	equations
		Al.male_name = Albert 
		Al.mother = Ellie
		
		Bob.male_name = George
		Bob.mother = Ellie
		
		Charlie.male_name = Charles	
		Charlie.mother = Fran

		Ellie.female_name = Elaine
		Fran.female_name = Francine
}

schema DeNormalizedSchema = literal : Ty {
	imports
		NormalizedSchema
	attributes
		mother_name : Male -> String
  	observation_equations
  		forall m:Male. mother_name(m) = female_name(mother(m))
}

instance DeNormalizedData = literal : DeNormalizedSchema {
	imports
		NormalizedData
}
