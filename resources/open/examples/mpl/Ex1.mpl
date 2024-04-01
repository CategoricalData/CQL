 T = theory {
	sorts
		A, B, C;
	symbols
		e : (A*C) -> (B*C), 
		f : A -> B,
		g : A -> C,
		h : (A*A) -> (B*C),
		i : I -> I;
	equations
		h = (f*g),
		(h*f) = (h*f),
		(rho1 A * id B) = (alpha1 A I B ; (id A * lambda1 B)),
		tr (id A * id B) = id A;
}
			
f = eval T f
h = eval T h
i = eval T i
fg = eval T (f*g)
idA = eval T id A
idAidA = eval T (id A ; id A)
tre = eval T tr e
			
S = theory {
	sorts
		X;
	symbols
		F : (X*X) -> (X*X),
		G : (X*X) -> (X*X);
	equations;
}
	
result = eval S tr ((((F * id X) ; alpha1 X X X); (id X * G)) ; (alpha2 X X X ; (sym X X * id X)))
	

