//Aggregation is not functorial
options
	allow_aggregation_unsafe = true

typeside Ty = literal {
	java_types	
		Double = "java.lang.Double"
	java_constants
	    //0.0 needed for Nashhorn error
		Double = "return 0.0 + java.lang.Double.parseDouble(input[0])"
	java_functions
		plus : Double,Double -> Double = "return 0.0 + input[0] + input[1]"
}

schema S = literal : Ty {
	entities 
		Emp Dept
	foreign_keys
		worksIn : Emp -> Dept		
	attributes
		salary : Emp -> Double
} 

schema T = literal : Ty {
	entities
		Dept
	attributes
		totalCost : Dept -> Double
} 

instance I = literal : S {
	generators 
		p1 p2 p3 p4 : Emp
		d1 d2 : Dept
	equations 
		p1.salary = "10" /*p2.salary = "1"*/ 
		p1.worksIn = d1 p2.worksIn = d1 

		p3.salary = "50" /*p2.salary = "1"*/ 
		p3.worksIn = d2 p4.worksIn = d2 

} 

query Q1 = literal : S -> T {
	entity Dept -> {from d:Dept
  			attributes totalCost -> from p:Emp
				where worksIn(p) = d
  	    		return salary(p)
  	    		aggregate 0 lambda arg1 arg2. plus(arg1,arg2)
	}
} 

instance J = eval Q1 I
