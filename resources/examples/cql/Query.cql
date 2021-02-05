typeside Ty = literal {
	external_types
		String -> "java.lang.String"
		Bool -> "java.lang.Boolean"
	external_parsers
		String -> "x => x"
		Bool -> "x => java.lang.Boolean.parseBoolean(x)"
}

schema Source = literal : Ty {
	entities
		Man Woman
	attributes
		fav_book_m : Man -> String
		fav_book_w : Woman -> String
		name_m : Man -> String
		name_w : Woman -> String
		paying : Man -> Bool
}

schema Target = literal : Ty {
	entities
		Male GoodMatch PayingGoodMatch
	foreign_keys
		is_a : PayingGoodMatch -> GoodMatch
		for_man : GoodMatch -> Male
	attributes
		man_name : Male -> String
		woman_name : GoodMatch -> String
}

query Q = literal : Source -> Target {
 entity
	GoodMatch -> {
		from m:Man w:Woman
		where fav_book_m(m) = fav_book_w(w)
		attributes woman_name -> name_w(w)
		foreign_keys for_man -> {man -> m}
	}

 entity
	Male -> {
		from man:Man
		attributes man_name -> name_m(man)
	}

 entity
	PayingGoodMatch -> {
		from man:Man woman:Woman
		where fav_book_m(man) = fav_book_w(woman)
			 paying(man) = true
		foreign_keys is_a -> {m -> man w -> woman}
	}


}

instance I = literal : Source {
	generators
		a d e g : Woman
		b c f h : Man
	multi_equations
		paying -> {b true, c false, f true, h true}
		name_m -> {b bob, c charlie, f frank, h henry}
		name_w -> {a alice, d doris, e ellie, g gina}
		fav_book_m -> {b book1, c book1, f book2, h book3}
		fav_book_w -> {a book1, d book2, e book2, g book4}
}

instance J = eval Q I

instance I0 = literal : Source {
	generators
		a0 d0 e0 : Woman
		b0 c0 f0 : Man
	multi_equations
		paying -> {b0 true, c0 false, f0 true}
		name_m -> {b0 bob, c0 charlie, f0 frank}
		name_w -> {a0 alice, d0 doris, e0 ellie}
		fav_book_m -> {b0 book1, c0 book1, f0 book2}
		fav_book_w -> {a0 book1, d0 book2, e0 book2}
}

transform h = literal : I0 -> I {
	generators
		a0 -> a d0 -> d e0 -> e b0 -> b c0 -> c f0 -> f
}

transform k = eval Q h

transform kk = coeval Q k
