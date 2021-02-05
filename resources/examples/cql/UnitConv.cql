typeside Ty = literal {
	external_types
		in -> "java.lang.Double"
		cm -> "java.lang.Double"
	external_parsers
		in -> "x => java.lang.Double.parseDouble(x)"
		cm -> "x => java.lang.Double.parseDouble(x)"
	external_functions
		inToCm : in -> cm = "x => (2.54 * x)"
}

schema AmericanAirplane = literal : Ty {
	entities
		Wing
	attributes
		wingLength : Wing -> in
}

schema EuropeanAirplane = literal : Ty {
	entities
		Wing
	attributes
		wingLength : Wing -> cm
}

query AmericanToEuropean = literal : AmericanAirplane -> EuropeanAirplane {
	entity
		Wing -> {from w:Wing
			    attributes wingLength -> inToCm(w.wingLength)}
}

instance Boeing747 = literal : AmericanAirplane {
	generators
		left right : Wing
	equations
		left.wingLength = right.wingLength
		left.wingLength = "500.1"	//apparently 500 can't be a double in javascript, it must be an integer
}

instance Boeing747Metric = eval AmericanToEuropean Boeing747

/*
  Error in query AmericanToEuropean_disastrous_conversion: in attribute wingLength, expected sort of wingLength(w) has sort cm but wingLength(w) actually has sort in

query AmericanToEuropean_disastrous_conversion = literal : AmericanAirplane -> EuropeanAirplane {
	entities
		Wing -> {from w:Wing
			    return wingLength -> w.wingLength}
}
*/
