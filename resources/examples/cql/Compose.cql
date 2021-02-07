options
	dont_validate_unsafe = true
	require_consistency=false

typeside ty = literal {
	imports sql
	external_functions
		plus : Double,Double -> Double = "(x, y) => x + y"
		neg : Double -> Double = "x => -x"
}

schema S = literal : ty {
	entities
		s s0
	foreign_keys
		ss : s0 -> s
	attributes
		att : s -> Double

}

schema T = literal : ty {
	entities
		t1 t2 t3
	foreign_keys
		f : t1 -> t3
		g : t2 -> t3
		l : t1 -> t1
		h : t2 -> t1
	path_equations
		t1 = t1.l
	attributes
		att1 : t1 -> Double
		att2 : t2 -> Double

}

schema U = literal : ty {
	entities
		u11 u12 u21 u22 u3 u3x
	foreign_keys
		f1 : u11 -> u3
		f2 : u12 -> u3
		f1x: u11 -> u3x
		f2x: u12 -> u3x
		h  : u21 -> u11
		l  : u11 -> u11
		g1 : u21 -> u3
		g2 : u22 -> u3
		h2 : u21 -> u11
		h3 : u22 -> u12

	path_equations
		u11.l.l = u11.l

	attributes
		att1 : u11 -> Double
		att2 : u21 -> Double
		att2x: u12 -> Double
		att3 : u3 -> Double

}

query qUT = literal : U -> T {
	entity
		t1 -> {from u11:u11 u12:u12
			   where u11.l.l.f1 = u12.f2   neg(u11.l.l.f1.att3) = neg(u12.f2.att3)
			   attributes att1 -> plus(u12.att2x, u11.att1)
			   foreign_keys f -> {u3 -> u11.f1  u3x -> u12.f2x}
			  	 l -> {u11 -> u11  u12 -> u12}
			   }

	entity
		t2 -> {from u21:u21 u22:u22
			   where u21.g1 = u22.g2  u21.h2.l.l.f1 = u22.h3.f2
			   attributes att2 -> plus(u21.att2, 1)
			   foreign_keys g -> {u3 -> u21.g1  u3x -> u21.h.f1x}
							h -> {u11 -> u21.h2  u12 -> u22.h3}
			   }

	entity
		t3 -> {from u3:u3 u3x:u3x}



}

query qTS = literal : T -> S {
	entity
		s -> {from t1:t1 t2:t2
		      where t1.f = t2.g       t1.att1=t2.att2
		attributes att -> plus(t2.att2,t1.l.att1)}
	entity
		s0 -> { from x:t2
		          where x.h.f = x.g     x.h.att1=x.att2
	foreign_keys
		ss -> {t1 -> x.h t2 -> x }	}
}
query qUS = [qUT ; qTS]

/////////////////

instance I = literal : U {
	generators
		u11:u11 u12:u12 u21:u21 u22:u22 u3:u3 u3x:u3x
	equations
		u11.att1 = 1
		u21.att2 = 1
		u12.att2x = 1
		u3.att3 = 1

		u11.f1 = u3
		u12.f2 = u3
		u11.f1x = u3x
		u12.f2x = u3x
		u21.h = u11
		u11.l = u11
		u21.g1 = u3
		u22.g2 = u3
		u21.h2 = u11
		u22.h3 = u12
}

instance J = eval qTS (eval qUT I)
instance K = eval qUS I

transform fr = frozen qTS lambda x:s0. x.ss.att : Double

/////////////////////////////////////////////////

typeside Ty = literal {
	types
		Bool
	constants
		true false : Bool
	functions
		and : Bool, Bool -> Bool
	equations
		and(true, true) = true
		and(true, false)= false
		and(false,false)= false
		and(false,true) = false
}

schema SS = literal : Ty {
	entities
		E F
	foreign_keys
		f : E -> F
	attributes
		att : E -> Bool
}

instance II = literal : SS {
	generators
		e : E
	equations
		e.att=true
}

query QQ = literal : SS -> SS {
	entity
		E -> {from t:Bool x:E t2:Bool
		 where x.att=t
		attributes att -> and(x.att,t) foreign_keys f -> {y->x s->and(x.att,t) t3->t2} }

	entity
		F -> {from s:Bool y:E t3:Bool}
}

instance JJ = eval QQ II

instance KK = coeval QQ II

//transform t = unit_query Q I   INFINTE

transform t2 = counit_query QQ II

transform t3 = coeval QQ (identity II)

query QQQQ = [QQ;QQ]

instance A = eval QQQQ II
instance B = eval QQ eval QQ II
instance A2 = coeval QQQQ II
instance B2 = coeval QQ coeval QQ II
