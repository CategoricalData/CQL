
schema Rel = literal : empty {
	entities 
		Dom Row
	foreign_keys 
		p1 p2 : Row -> Dom
}

constraints PartialOrder = literal : Rel {
	forall d:Dom -> exists r:Row where r.p1 = d r.p2 = d #refl
	
	forall r1 r2:Row where r1.p1 = r2.p2 r1.p2 = r2.p1 -> where r1.p1 = r1.p2  #anti-sym
	
	forall r1 r2:Row where r1.p2 = r2.p1 -> 
	exists r3:Row where r3.p1 = r1.p1 r3.p2 = r2.p2 #trans
}

instance TwoGenerators = literal : Rel { 
	generators 
		0 1 : Dom 
}

#R(0,0) R(1,1)
instance FreePartialOrderOnTwoGenerators = chase PartialOrder TwoGenerators

	
