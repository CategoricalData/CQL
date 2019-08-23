
apg_typeside Ty = literal {
	types 
		Str -> "java.lang.String"  "return input[0]"
		Nat -> "java.lang.Integer" "return java.lang.Integer.parseInt(input[0])"
}

apg_instance Vertex = literal : Ty {
	labels
		User -> ()
		Trip -> ()
	elements
		u1 : User -> ()
		t1 : Trip -> ()
}

apg_instance Edge = literal : Ty {
	imports
		Vertex
	labels
		driver -> (Trip:label Trip * User:label User)
		rider  -> (Trip:label Trip * User:label User)
	elements
		u2 : User   -> ()
		d1 : driver -> (Trip:t1 , User:u1)
		r1 : rider  -> (Trip:t1 , User:u2)
}

apg_instance Property = literal : Ty {
	labels
		User -> ()
		name -> (User:label User * name:base Str)
	elements
		u1 : User -> ()
		n1 : name -> (User:u1, name:"Arthur Dent")
		n2 : name -> (User:u1, name:"Arthur P. Dent")
}

apg_instance Alias = literal : Ty {
	labels
		Lat  -> base Nat
		Long -> base Nat
	elements
		d1 : Lat  -> 4
		d2 : Long -> 5
}

apg_instance HyperElements = literal : Ty {
	labels
		User -> ()
		Trip -> (user1:label User * user2:label User * event1:<place:label PlaceEvent + null:()> * event2:<place:label PlaceEvent + null:()>) 
		PlaceEvent -> (place:label Place * event:label UnixTimeSeconds) 
		Place -> ()
		UnixTimeSeconds -> base Nat
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

//////////////////


apg_instance G = literal : Ty {
	labels
		L3 -> base Str
		L4 -> label L4
		L5 -> (name: base Str * age: base Nat)
		L6 -> <name: base Str + age: base Nat>
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

apg_instance i1 = empty Ty
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


apg_instance G0 = literal : Ty {
	labels
		L -> base Str
}
apg_instance H0 = literal : Ty {
	labels
		L -> base Nat
}
apg_morphism m0 = literal : G0 -> H0 {
	labels
		L -> L
}


apg_instance G2 = literal : Ty {
	labels
		L -> (a: base Str * b: base Nat)
	elements
		e:L -> (a:hi, b:0)	
}
apg_instance H2 = literal : Ty {
	labels
		L -> (b: base Str * a: base Nat)
	elements
		e:L -> (b:hi, a:0)		
}
apg_morphism m2 = literal : G2 -> H2 {
	labels
		L -> L
	elements
		e -> e

			
}

