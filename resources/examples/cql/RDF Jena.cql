
schema S = literal : sql {
	entities
		Employee
		Department
	foreign_keys
		manager   : Employee -> Employee
		worksIn   : Employee -> Department
		secretary : Department -> Employee
	path_equations
		Employee.manager.worksIn = Employee.worksIn
  		Department.secretary.worksIn = Department
  	attributes
  		first last	: Employee -> String
     	age			: Employee -> Integer
     	name 		: Department -> String
}

instance I = literal : S {
	generators
		a b c : Employee
		m s : Department
	equations
		first(a) = Al
		first(b) = Bob  last(b) = Bo
		first(c) = Carl
		name(m)  = Math name(s) = CS
		age(a) = age(c)
		manager(a) = b manager(b) = b manager(c) = c
		worksIn(a) = m worksIn(b) = m worksIn(c) = s
		secretary(s) = c secretary(m) = b
		secretary(worksIn(a)) = manager(a)
		worksIn(a) = worksIn(manager(a))
		age(a) = "2"
		age(manager(a)) = "1"
}

command exportRdfData = export_rdf_instance_xml I "file.xml" {
	external_types
		Integer -> "http://www.w3.org/2001/XMLSchema#Integer" "x => x.toString()"
}

instance J = import_rdf_all "file.xml" //can import from .ttl too

instance K = spanify J
