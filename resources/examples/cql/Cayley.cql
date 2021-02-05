schema Dihedral2 = literal : empty {
	entities
		G 
	foreign_keys
		r s : G -> G
	path_equations	
		G.r.r = G
		G.s.s = G
		G.s.r.s = G.r
}

schema Quaternions = literal : empty {
	entities
		G 
	foreign_keys
		x y x0 y0 : G -> G
	path_equations	
		G.x.x0=G
		G.x0.x=G
		G.y.y0=G
		G.y0.y=G
		G.x.x.x.x = G
		G.x.x = G.y.y
		G.y0.x.y = G.x0
}

schema Cyclic4 = literal : empty {
	entities
		G 
	foreign_keys
		f : G -> G
	path_equations
		G.f.f.f.f=G
		
}

schema FreeMonoid = literal : empty {
	entities
		G 
	foreign_keys
		a b : G -> G		
}
