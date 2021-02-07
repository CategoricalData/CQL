
apg_typeside Ty = literal {
	types
		Str -> "java.lang.String"  "x => x"
		Nat -> "java.lang.Integer" "parseInt"
	functions
		append : Str,Str -> Str = "(x, y) => x.concat(y)"
}

////////////////////////////////////////////////////////////

apg_schema VertexSch = literal : Ty {
	labels
		User -> ()
		Trip -> ()
}
apg_instance Vertex = literal : VertexSch {
	elements
		u1 : User -> ()
		t1 : Trip -> ()
}

////////////////////////////////////////////////////////////

apg_schema EdgeSch = literal : Ty {
	imports
		VertexSch
	labels
		driver -> (Trip:label Trip * User:label User)
		rider  -> (Trip:label Trip * User:label User)
}
apg_instance Edge = literal : EdgeSch {
	imports
		Vertex
	elements
		u2 : User   -> ()
		d1 : driver -> (Trip:t1 , User:u1)
		r1 : rider  -> (Trip:t1 , User:u2)
}

////////////////////////////////////////////////////////////

apg_schema PropertySch = literal : Ty {
	labels
		User -> ()
		name -> (User:label User * name:base Str)
}
apg_instance Property = literal : PropertySch {
	elements
		u1 : User -> ()
		n1 : name -> (User:u1, name:"Arthur Dent")
		n2 : name -> (User:u1, name:"Arthur P. Dent")
}

////////////////////////////////////////////////////////////

apg_schema AliasSch = literal : Ty {
	labels
		Lat  -> base Nat
		Long -> base Nat
}
apg_instance Alias = literal : AliasSch {
	elements
		d1 : Lat  -> 4
		d2 : Long -> 5
}

////////////////////////////////////////////////////////////

apg_schema HyperElementsSch = literal : Ty {
	labels
		User -> ()
		Trip -> (user1:label User * user2:label User * event1:<place:label PlaceEvent + null:()> * event2:<place:label PlaceEvent + null:()>)
		PlaceEvent -> (place:label Place * event:label UnixTimeSeconds)
		Place -> ()
		UnixTimeSeconds -> base Nat
}
apg_instance HyperElements = literal : HyperElementsSch {
	elements
		u1 : User -> ()
		u2 : User -> ()
		u3 : User -> ()
		t1 : Trip -> (user1:u1 , user2:u2 , event1:<place:e1>, event2: <place:e2>)
		t2 : Trip -> (user1:u1 , user2:u3 , event1:<place:e3>, event2: <null :()>)
		e1 : PlaceEvent -> (place:p1 , event:s1)
		e2 : PlaceEvent -> (place:p2 , event:s2)
		e3 : PlaceEvent -> (place:p2 , event:s3)
		e4 : PlaceEvent -> (place:p3 , event:s4)
		p1 : Place -> ()
		p2 : Place -> ()
		p3 : Place -> ()
		s1 : UnixTimeSeconds -> 1
		s2 : UnixTimeSeconds -> 1
		s3 : UnixTimeSeconds -> 4
		s4 : UnixTimeSeconds -> 3
}

///////////////////////////////////////////////////////////////////

apg_schema MSch = literal : Ty {
	labels
		PlateNumber -> (id: base Str * region_id: base Str * region_lp: base Str)
}
apg_instance M = literal : MSch {
	elements
		p1 : PlateNumber -> (id: US, region_id: CA, region_lp: "6trj244")
		p2 : PlateNumber -> (id: MX, region_id: BC, region_lp: "ahd4102")
		q1 : PlateNumber -> (id: US, region_id: CA, region_lp: "6trj244")
		q2 : PlateNumber -> (id: MX, region_id: SN, region_lp: "vuk1775")
}

apg_instance N = literal : MSch {
	elements
		p1q1 : PlateNumber -> (id: US, region_id: CA, region_lp: "6trj244")
}

apg_morphism j = literal : N -> M {
	labels
		PlateNumber -> PlateNumber
	elements
		p1q1 -> p1
}

apg_morphism k = literal : N -> M {
	labels
		PlateNumber -> PlateNumber
	elements
		p1q1 -> p2
}

apg_instance CoEqjk = coequalize j k
apg_morphism CoEqjkt = coequalize j k
apg_morphism CoEqjkt_u = coequalize_u j k CoEqjkt //id

///////////////////////////////////////////////////////////////////

apg_schema GSch = literal : Ty {
	labels
		L3 -> base Str
		L4 -> label L4
		L5 -> (name: base Str * age: base Nat)
		L6 -> <name: base Str + age: base Nat>
}
apg_instance G = literal : GSch {
	 elements
         e4 : L4 -> e4
         e3 : L3 -> "hello world"
         e5 : L5 -> (name:"bill", age: "35")
         e6 : L6 -> <age: "30">
}

apg_morphism h = literal : G -> G {
	labels
		L3->L3 L4->L4 L5->L5 L6->L6
	elements
		e4->e4 e3->e3 e5->e5 e6->e6
}

apg_instance i1 = empty GSch
apg_instance i2 = unit Ty
apg_instance i3 = <G + G>
apg_instance i4 = (G * G)
apg_instance i5 = equalize (identity G) (identity G)

apg_morphism t1 = empty G
apg_morphism t2 = unit G
apg_morphism t3 = fst G G
apg_morphism t4 = snd G G
apg_morphism t5 = inl G G
apg_morphism t6 = inr G G
apg_morphism t7 = (t3,t4)
apg_morphism t8 = <t5|t6>
apg_morphism t9 = identity G
apg_morphism t0 = [ t9 ; t9 ]
apg_morphism ta = equalize t9 t9
apg_morphism tb = equalize_u t9 t9 t9

apg_schema x = <VertexSch + VertexSch>
apg_schema y = (VertexSch * VertexSch)
apg_schema z = empty Ty
apg_schema w = unit Ty

/////////////////////////////////////////////////////////////////

apg_schema G0Sch = literal : Ty {
	labels
		L -> base Str
}
apg_instance G0 = empty G0Sch

apg_schema H0Sch = literal : Ty {
	labels
		L -> base Nat
}
apg_instance H0 = empty H0Sch

apg_morphism m0 = literal : G0 -> H0 {
	labels
		L -> L
}

apg_schema G2Sch = literal : Ty {
	labels
		L -> (a: base Str * b: base Nat)
}
apg_instance G2 = literal : G2Sch {
	elements
		e:L -> (a:hi, b:0)
}
apg_schema H2Sch = literal : Ty {
	labels
		L -> (b: base Str * a: base Nat)
}
apg_instance H2 = literal : H2Sch {
	elements
		e:L -> (b:hi, a:0)
}
apg_morphism m2 = literal : G2 -> H2 {
	labels
		L -> L
	elements
		e -> e
}

/////////////////////////////////////////////////////////////////

apg_schema T = literal : Ty {
	labels
		tLbl -> (age: base Nat * name: base Str)
}
apg_schema S = literal : Ty {
	labels
		sLbl -> (age: base Str * name: base Nat * ssn: base Nat)
}

// for mappings for now, terms must be fully type-annotated
apg_mapping F = literal : S -> T {
	labels
		sLbl -> lambda v: label tLbl. (ssn: 0@Nat, name: .age(!tLbl(v)), age: .name(!tLbl(v)))
}

apg_instance J = literal : T {
	elements
		e : tLbl -> (age: 40, name: Alice)
}

apg_instance I = delta F J

apg_instance J2= literal : T {
	elements
		e2 : tLbl -> (age: 40, name: Alice)
}

apg_morphism mt = literal : J -> J2 {
	labels
		tLbl -> tLbl
	elements
		e -> e2
}

apg_morphism dmt = delta F mt

//

apg_schema Tx = literal : Ty {
	labels
		tLbl -> <age: base Nat + name: base Str>
}
apg_schema Sx = literal : Ty {
	labels
		sLbl -> <AGE: base Nat + NAME: base Str>
}

apg_mapping Fx = literal : Sx -> Tx {
	labels
		sLbl -> lambda v: label tLbl.
			case !tLbl(v) where
				age  -> lambda x. <AGE: x> : <AGE: base Nat + NAME: base Str>
		 		name -> lambda x. <NAME: append(x,x)> : <AGE: base Nat + NAME: base Str>
			: <AGE: base Nat + NAME: base Str>
}

apg_instance Jx = literal : Tx {
	elements
		e : tLbl -> <age: 40>
		f : tLbl -> <name: alice>
}

apg_instance Ix = delta Fx Jx
