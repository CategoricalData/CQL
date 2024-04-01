#Record Linkage

schema S = literal : sql {
	entities
		Element
	attributes
		name1 name2 : Element -> String	
		num1 num2 : Element -> String
}

instance I = literal : S {
	generators
		e1 e2 e3 x y z d : Element
	equations
		e1.name1 = "H"   e1.num1 = "1"
		e2.name1 = "He"  e2.num1 = "2"
		e3.name1 = "Li"  e3.num1 = "3"
		x.name2 = "Hydrogen"  x.num2 = "1"
		y.name2 = "Helium"    y.num2 = "2"
		z.name2 = "Lithium"   z.num2 = "3"
		d.name2 = "Beryllium" d.num2 = "4"	
}

instance J = quotient_query I {
	entity Element -> {from a:Element b:Element where a.num1 = b.num2}
	options
	quotient_use_chase = false
}

instance J2 = quotient_query I {
	entity Element -> {from a:Element b:Element where a.num1 = b.num2}
	options
	quotient_use_chase = true
}
