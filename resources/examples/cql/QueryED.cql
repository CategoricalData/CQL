options
	always_reload = true
	
///////////////////////////////////////////////////////////////////////////

schema T = literal : sql {
	entities 
		T1 
		T2
	attributes
		id fk  : T1 -> String
		id     : T2 -> String
}

constraints D = literal : T {
	forall x y : T1 where x.id = y.id -> where x = y
	forall x y : T2 where x.id = y.id -> where x = y
	forall x   : T1                   -> exists y : T2 where x.fk = y.id
}

constraints D0 = literal : T { }

//command check_id = check_query (identity T) D0 D


///////////////////////////////////////////////////////////////////////////

schema S1 = literal : sql {
	entities
		S
	attributes
		ssn : S -> String	
}

constraints C1 = literal : S1 { }

query Q1 = literal : S1 -> T {
	entity T1 -> {from s:S
		attributes id -> s.ssn  fk -> s.ssn	
	}

	entity T2 -> {from s:S
		attributes id -> s.ssn
	}
}

//command check1 = check_query Q1 C1 D

instance I1 = literal : S1 {
	generators
		s : S
	equations
		s.ssn = "YYY-XXXX"	
}

instance J1 = eval Q1 I1

command check1_redundant = check D J1

///////////////////////////////////////////////////////////////////////////

schema S2 = literal : sql {
	entities 
		S1 
		S2
	attributes
		name  : S1 -> String
		name  : S2 -> String
}

constraints C2 = literal : S1 {
}

query Q2 = literal : S2 -> T {
	entity T1 -> {from s1:S1 s2:S2
		where s1.name = s2.name
		attributes id -> s1.name  fk -> s1.name	
	}

	entity T2 -> {from s1:S1 s2:S2
		where s1.name = s2.name
		attributes id -> s1.name  
	}
}

//command check2 = check_query Q2 C2 D

instance I2 = literal : S2 {
	generators
		s1 u1 : S1
		s2 u2 : S2
	equations
		s1.name = "Alice"
		s2.name = "Alice"
		u1.name = "Bill"
		u2.name = "Charlie"		
}

instance J2 = eval Q2 I2

command check2_redundant = check D J2

///////////////////////////////////////////////////////////////////////////

query Q3 = literal : T -> T {
	entity T1 -> {from t2:T2 
		attributes id -> t2.id  fk -> t2.id
	}

	entity T2 -> {from t2:T2
		attributes id -> t2.id  
	}
}

//command check3 = check_query Q3 D0 D

instance I3 = literal : T {
	generators
		t1 : T1
		t2 : T2
	equations
		t1.id = 0  t1.fk = 1  t2.id = 2	
}

instance J3 = eval Q3 I3

command check3_redundant = check D J3

//////////////////////////////////////////////////////////////////////////

query Q4 = literal : T -> T {
	entity T1 -> {from t1:T1 t2:T2
		where t2.id = t1.fk
		attributes id -> t1.id  fk -> t2.id	
	}

	entity T2 -> {from t2:T2
		attributes id -> t2.id  
	}
}

//command check4 = check_query Q4 D0 D

instance I4 = literal : T {
	generators
		t1 t1x : T1
		t2 : T2
		
	equations
		t1.id = 0  t1.fk = 2 t2.id = 2
		t1x.id = 1

}

instance J4 = eval Q4 I4

command check4_redundant = check D J4

/////////////////////////////////////////////////////////////////////////

query Q5 = literal : T -> T {
	entity T1 -> {from t1:T1 t2:T2
		attributes id -> t1.id  fk -> t2.id	
	}

	entity T2 -> {from t2:T2
		attributes id -> t2.id  
	}
}

//command check5 = check_query Q5 D0 D

instance J5 = eval Q5 I4

command check5_redundant = check D J5


