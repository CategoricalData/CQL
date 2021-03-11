#one = two should not be provable
typeside EmptySortsCheck = literal {
	types
 		Void
 		nat
	constants
 		one two : nat
 	equations
	 	 forall x:Void. one = two
	options
		prover = completion
#		completion_precedence = "one two"
}

typeside Group = literal {
	types
		S
	constants
		e : S
	functions
		I : S -> S
		o : S,S -> S
	equations
 		forall x. (e o x) = x
 		forall x. (I(x) o x) = e
 		forall x, y, z. ((x o y) o z) = (x o (y o z))
 	options
		prover = completion
#		completion_precedence = "e o I"
}

typeside LR = literal {
	types
		S
	constants
		e : S
	functions
		I : S -> S
		o : S,S -> S
	equations
 		forall x. o(e,x) = x
		forall x. o(x,I(x)) = e
		forall x y z. o(o(x,y),z)=o(x,o(y,z))
 	options
		prover = completion
#		completion_precedence = "e o I"
}

typeside RL = literal {
	types
		S
	constants
		e : S
	functions
		I : S -> S
		o : S,S -> S
	equations
 		forall x. o(x,e) = x
		forall x. o(I(x),x) = e
		forall x, y, z. o(o(x,y),z)=o(x,o(y,z))
 	options
		prover = completion
#		completion_precedence = "e o I"
}

typeside Arith = literal {
	types
		N
	constants
		zero : N
	functions
		succ : N -> N
		plus : N,N -> N
		times : N,N -> N
	equations
		forall x. plus(zero, x) = x
		forall x, y. plus(succ(x),y) = succ(plus(x,y))
		forall x. times(zero, x) = zero
		forall x, y. times(succ(x),y) = plus(y,times(x,y))
	options
		prover = completion
#		completion_precedence = "zero succ plus times"
}

#try ((a o b) o (c o d)) = ((a o c) o (b o d)) and
#forall p q r s. ((p o q) o (r o s)) = ((p o r) o (q o s))
typeside Entropic = literal {
	 types
		S
	constants
		a b c d : S
	 functions
		o : S,S -> S
	 equations
	 	forall x, y, z, w. o(o(x,y),o(z,w)) = o(o(x,z),o(y,w))
		forall x, y. o(o(x,y),x) = x
	options
		prover = completion
		completion_precedence = "o a b c d"
}

typeside ACUIN = literal {
	types
		S
	constants
		e : S
	functions
		n : S -> S
		o : S,S -> S
	equations
		forall x, y, z. o(o(x,y),z) = o(x,o(y,z))
		forall x, y. o(x,y) = o(y,x)
		forall x. o(x, e) = x
#		forall x. o(x, x) = x
	options
		prover = completion
		completion_precedence = "e n o"
#		completion_syntactic_ac = true
#		timeout = 10
}

typeside DistributiveLattice = literal {
	types
		DL
	constants
		t f : DL
	functions
		n : DL, DL -> DL
		u : DL, DL -> DL
	equations
		 forall x. (x n t)=x
		 forall x y. (y n x) = (x n y)
		 forall x y z. (x n (y n z)) = ((x n y) n z)
		 forall x. (x n f)=f
		 forall x. (x u f)=x
		 forall x y. (y u x) = (x u y)
		 forall x y z. (x u (y u z)) = ((x u y) u z)
		 forall x. (x u t) = t
		 #forall x y z. (x n (y u z)) = ((x n y) u (x n z))
	options
		prover=completion
		completion_precedence = "t f n u"
}

# still too hard
#
#typeside BooleanRing = literal {
#	types
#		S
#	constants
#		zero one : S
#	functions
#		plus times : S,S -> S
#	equations
#		forall x y z.  plus(plus(x,y),z) = plus(x,plus(y,z))
#		forall x y z.  times(times(x,y),z) = times(x,times(y,z))
#		forall x y. plus(x,y) = plus(y,x)
#		forall x y. times(x,y) = times(y,x)
#		forall x y, z. plus(x,plus(y,z)) = plus(y,plus(x,z))
#		forall x y, z. times(x,times(y,z)) = times(y,times(x,z))
#		forall x y, z. times(x,plus(y,z)) = plus(times(x,y),times(x,z))
#		forall x y, z. times(plus(x,y),z) = plus(times(x,z),times(y,z))
#		forall x. plus(zero,x) = x
#		forall x. plus(x,zero) = x
#		forall x. times(one,x) = x
#		forall x. times(x,one) = x
#		forall x. times(zero,x) = zero
#		forall x. times(x,zero) = zero
#		forall x. plus(x,x) = zero
#		forall x. times(x,x) = x
#		forall x y. plus(x,plus(x,y)) = y
#		forall x y. times(x,times(x,y)) = times(x,y)
#	options
#		prover = completion
#		completion_precedence = "zero one plus times"
#}

