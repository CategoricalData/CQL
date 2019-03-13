//This file demonstrates some properties of data type conversion and inversion
//with delta and sigma.

typeside Ty = literal {
	types
		String
		Integer
	constants
		50 : Integer
	functions
		int_to_str : Integer -> String
		str_to_int : String -> Integer
	equations
		//forall x. int_to_str(str_to_int(x)) = x
		forall x. str_to_int(int_to_str(x)) = x
}

schema S = literal : Ty {
	entities
		S_E
	attributes
		S_att_str : S_E -> String
	}

schema T = literal : Ty {
	entities
		T_E
	attributes
		T_att_int : T_E -> Integer
}

mapping F = literal : S -> T {
	entity
		S_E -> T_E
	attributes
		S_att_str -> T_att_int . int_to_str
}

instance I = literal : T {
	generators
		g : T_E
	equations
		g.T_att_int = 50
}

instance J = delta F I // works fine

instance K = sigma F J { // works fine
	options
		require_consistency = false
}
/* K is isomorphic to this instance:
instance K2 = literal : T {
	generators
		g : T_E

	equations
		g.T_att.int_to_str = 50.int_to_str

	options
		require_consistency = false
}

CQL can't prove that K doesn't prove any additional string or int equations that
the typeside does, hence the need to set require_consistency = false.

Note that K |- g.T_att_int = 50, but only because of the equation on the typeside.
*/

