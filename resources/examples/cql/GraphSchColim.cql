graph span = literal {
	nodes L X R
	edges f : L -> X   g : X -> R
}

typeside T = empty
schema S = empty : T
mapping F = identity S

schema_colimit C = literal span : T {
	nodes
		L -> S
		X -> S
		R -> S
	edges
		f -> F
		g -> F
}