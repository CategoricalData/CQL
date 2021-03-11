options
	always_reload = true

schema tp = tinkerpop

constraints tpc = tinkerpop

command c1 = spawn_bitsy #note: can only have one running bitsy at a time

command c2 = exec_tinkerpop {
"g.V().drop()"
"g.addV('root').property('data',9).as('root').
  addV('node').property('data',5).as('b').
  addV('node').property('data',2).as('c').
  addV('node').property('data',11).as('d').
  addV('node').property('data',15).as('e').
  addV('node').property('data',10).as('f').
  addV('node').property('data',1).as('g').
  addV('node').property('data',8).as('h').
  addV('node').property('data',22).as('i').
  addV('node').property('data',16).as('j').
  addE('left').property('data',16).from('root').to('b').
  addE('left').from('b').to('c').
  addE('right').from('root').to('d').
  addE('right').from('d').to('e').
  addE('right').from('e').to('i').
  addE('left').from('i').to('j').
  addE('left').from('d').to('f').
  addE('right').from('b').to('h').
  addE('left').from('c').to('g')"
}

instance g = import_tinkerpop_all

#########################
# Export


schema SJava = literal : rdf {
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
  		Employee.manager.manager = Employee.manager
  	attributes
  		first last age : Employee -> Dom
     	name 		: Department -> Dom
}

instance IJava = literal : SJava {
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

command c3 = exec_tinkerpop {
"g.V().drop()"
}

command c4 = export_tinkerpop_instance IJava

instance h = import_tinkerpop_all {
	options 
		tinkerpop_graph_name = g #without this option, CQL will re-use g, not knowing c4 mutated bitsy
}
