//https://suif.stanford.edu/~courses/cs243/lectures/l12-handout.pdf

typeside Domains = literal {
	external_types 
		V -> "java.lang.Object"
		H -> "java.lang.Object"
		F -> "java.lang.Object"
	external_parsers 
		V -> "x => x"
		F -> "x => x"
		H -> "x => x"
}

schema EDB = literal : Domains {
	entities
		vP0 assign store load 
	attributes
		v : vP0 -> V	h : vP0 -> H
		v1: assign -> V v2: assign -> V
		v1: store -> V  f:store -> F  v2: store->V	
		v1: load -> V   f:load -> F   v2: load->V	

}

schema IDB = literal : Domains {
	entities
		vP hP 
	attributes
		v : vP -> V	h : vP -> H
		h1: hP -> H   f:hP->F  h2:hP->H
}

schema DB = literal : Domains {
	imports EDB IDB
}

constraints Rules = literal : DB {
	forall x:vP0 -> exists y:vP where x.v = y.v  x.h = y.h

	forall a:assign x:vP where a.v2 = x.v -> 
	exists unique z:vP where a.v1 = z.v  x.h = z.h

	forall x:vP y:vP s:store where s.v1 = x.v  s.v2 = y.v -> 
	exists unique h:hP where h.h1 = x.h  h.h2 = y.h h.f = s.f

	forall l:load x:vP k:hP where l.v1 = x.v  l.f = k.f -> 
	exists unique y:vP where y.v = l.v2  y.h = k.h2 
}

instance I = literal : DB {
	generators
		0 1 2 : vP0
		3 : store
		4 : load
		5 6 7 8 9 A : assign
	equations
		0.v = "x" 0.h = "main@1"
		1.v = "y" 1.h = "main@2"
		2.v = "z" 2.h = "main@3"
		3.v1= "c" 3.f = "f" 3.v2 = "d"
		4.v1= "z" 4.f = "f" 4.v2 = "q"
		5.v1= "a" 5.v2= "x"
		6.v1= "b" 6.v2= "y"
		7.v1= "c" 7.v2= "z"
		8.v1= "d" 8.v2= "x"
		9.v1= "c" 9.v2= "a"
		A.v1= "d" A.v2= "b"
}

instance J = chase Rules I
 