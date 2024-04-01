typeside cql_sorts = literal {
	types
		G T S I M Q H C P U O
		#graph, typeside, schema, instance, mapping, query, transform, constraints, pragma, schema-colimit, object
}

typeside cql_literals = literal {
	imports 
		cql_sorts

	functions	
		G_lit : O -> G
		T_lit : O -> T
		S_lit : O , T -> S
		I_lit : O , S -> I
		H_lit : O , I , I -> T
		M_lit : O , S , S -> M
		Q_lit : O , S , S -> Q
		C_lit : O , S -> C 
}

typeside cql_cat = literal {
	imports 
		cql_sorts

	functions
		empty_T :   -> T
		empty_S : T -> S
		empty_I : S -> I
		empty_C : S -> C

		id_M : S -> M
		id_Q : S -> Q
		id_H : I -> H
		
		schemaOf_I   : I -> S
		typesideOf_S : S -> T

		comp_M : M , M -> M
		comp_T : T , T -> T
		comp_Q : Q , Q -> Q

		dom_M cod_M : M -> S
		dom_Q cod_Q : Q -> S
		dom_T cod_T : I -> H

}

constraints cql_cat_constraints = literal : empty : cql_cat {
	
	forall s:S f:M where dom_M(f) = s -> where comp_M(id_M(s), f) = f #etc

}

typeside cql_io = literal {
	imports
		cql_sorts

	functions
		sql_T : -> T
		jdbc_I csv_I : O , S -> I
		jdbc_all_I : O -> I
		jdbc_all_S : O -> S
		jdbc_T csv_T : O , S -> T

		toJdbc_I toCsv_I : O , I -> P
		toJdbc_T toCsv_T : O , T -> P
		toJdbc_Q 
		
		execcmd execjs execsql : O -> P
}

typeside cql_fdm = literal {
	imports 
		cql_sorts

	functions
		from_Mapping : M , M -> Q
		toMapping1 toMapping2 : Q -> M

		delta_I sigma_I pi_I : M , I -> I
		eval_I  coeval_I     : Q , I -> I
	
		delta_T sigma_T pi_T  : M , T -> T
		eval_T  coeval_T      : Q , T -> T

		sd_unit sd_counit dp_unit dp_counit : M -> T
		ec_unit ec_counit : Q -> T
		
		anononymize_I cascade_delete_I distinct_I  : I -> I
		frozen_I : O , Q -> I
		coequalize_I : T , T -> I
		coproduct_I except_I : I , I -> I
		chase_I : I , C -> I
		quotient_I : I , C -> I
		
		random_I : O , S -> I
		
		except_T  : I , T -> T
		except_return_T  distinct_return_T : I -> T
		distinct_T : T -> T
		frozen_T : O , Q -> T
		coproduct_T : I , T -> T

		fromColim_S : U -> S
		fromColim_M : U , S -> M
		wrap_U : U , M , M -> U
		modify_U quo_U : O , U -> U

		satisfies_I consistent_I : O , I -> P
}

typeside cql_pivot = literal {
	imports 
		cql_sorts

	functions
		pivot_I : I -> I
		pivot_S : I -> S
		pivot_M : I -> M

}
