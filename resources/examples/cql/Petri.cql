
schema Net = literal : empty {
	entities
		Input Place Trans Output
	foreign_keys
		place : Input -> Place
	    trans : Input -> Trans
	    place : Output -> Place
	    trans : Output -> Trans
} 

/*
 * p1 ->t1-> p2 ->t2-> p3
 */
instance N = literal : Net {
	generators
		p1 p2 p3 : Place
		t1 t2 : Trans
		i1 i2 : Input
		o1 o2 : Output
	equations
		i1.place = p1
		i1.trans = t1
		o1.trans = t1
		o1.place = p2

		i2.place = p2
		i2.trans = t2
		o2.trans = t2
		o2.place = p3
		
	options
		interpret_as_algebra = true	
}

/*
 entities
	i1 i2 o1 o2 p1 p2 p3 t1 t2
foreign_keys
	trans : o2 -> t2
	place : o2 -> p3
	place : o1 -> p2
	trans : o1 -> t1
	trans : i2 -> t2
	place : i2 -> p2
	place : i1 -> p1
	trans : i1 -> t1
*/	
schema intN = pivot N 

mapping proj = pivot N // intN -> Net

constraints injectivity = literal : Net {
	forall x y : Input
	where x.place = y.place -> where x = y

	forall x y : Output
	where x.trans = y.trans -> where x = y
}

///////////////////////////////////////////////////////////////
//example: 

//this is an instance on intN and passes the injectivity check
instance I = literal : intN {
	generators
		token1time1 : p1
		token1time2 : p2
		token1time3 : p3
		fire12 : t1
		fire23 : t2	

	equations

	options
		interpret_as_algebra = true	
}

instance J = sigma proj I // have J = N

command validate1 = check injectivity J
