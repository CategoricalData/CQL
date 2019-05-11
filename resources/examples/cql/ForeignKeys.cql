typeside Ty = empty

schema University = literal : Ty {
	entities
		Professor Student Department
 	foreign_keys
		worksIn    : Professor -> Department
		majoringIn : Student -> Department	
} 

schema AdvisorMatches = literal : Ty {
	imports
		University
	entities
		Match
 	foreign_keys
		studentOf : Match -> Student
		professorOf : Match -> Professor
	path_equations
		Match.studentOf.majoringIn = Match.professorOf.worksIn
} 

query findMatches = literal : University -> AdvisorMatches {
 entity
	Department -> {from dept:Department}
entity		
	Professor -> {from prof:Professor foreign_keys worksIn -> {dept -> prof.worksIn}}
entity
	Student -> {from stu:Student foreign_keys majoringIn -> {dept -> stu.majoringIn}}


/* When 
 * 
 *   where p.worksIn = s.majoringIn 
 *   
 * is commented out the query is rejected: 
 *   
 *   Error in query findMatches: 
 *     Target equation v.studentOf.majoringIn = v.professorOf.worksIn not respected: 
 *       transforms to s.majoringIn = p.worksIn, which is not provable in the sub-query for Match.
 */entity  Match -> {from p:Professor s:Student 
		     where p.worksIn = s.majoringIn
		     foreign_keys professorOf -> {prof -> p}		
			studentOf -> {stu -> s}
		     } 
	
	

	
/* Disabling the equation validator will cause a runtime error when this query is executed:
 * 
 *   Error in instance MatchesForUnivX : Algebra does not satisfy equation  
 *     forall v. v.studentOf.majoringIn = v.professorOf.worksIn 
 *       on ID <Match p:[Euler],s:[Kleene]>
 */ /* options
	    dont_validate_unsafe=true */

} 

instance UniversityOfX = literal : University {
	generators
		Gauss Church Euler : Professor
		Riemann Turing Kleene : Student
		math cs : Department
	multi_equations
		worksIn -> {Gauss math, Church cs, Euler math}
		majoringIn -> {Riemann math, Turing cs, Kleene cs}		
}

instance MatchesForUnivX = eval findMatches UniversityOfX
