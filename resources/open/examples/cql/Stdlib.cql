typeside Exp = literal {
	types
		Bool Nat NatOpt NatList
	constants
		t f : Bool
		z : Nat
		none : NatOpt
		nil : NatList
	functions
		not : Bool -> Bool
		and : Bool, Bool -> Bool
		or : Bool, Bool -> Bool

		s : Nat -> Nat
		plus : Nat, Nat -> Nat
		#times : Nat, Nat -> Nat
		if : Bool, Nat, Nat -> Nat
		eq : Nat, Nat -> Bool
		ne : Nat, Nat -> Bool
		lt : Nat, Nat -> Bool
		le : Nat, Nat -> Bool
		gt : Nat, Nat -> Bool
		ge : Nat, Nat -> Bool

		just : Nat -> NatOpt
		isNone : NatOpt -> Bool
		orDef : NatOpt, Nat -> Nat

		cons : Nat, NatList -> NatList
		head : NatList -> NatOpt
		tail : NatList -> NatList
		at : NatList, Nat -> NatOpt
		length : NatList -> Nat
		rangeDesc : Nat -> NatList
	equations
		not(t) = f
		not(f) = t
		forall x. not(not(x)) = x

		forall x. (x and f) = f
		forall x. (x and t) = x
		forall x. (x and not(x)) = f
		forall x y. (x and y) = (y and x)
		#forall x y z. (x and (y and z)) = ((x and y) and z)

		forall x. (x or f) = x
		forall x. (x or t) = t
		forall x. (x or not(x)) = t
		forall x y. (x or y) = (y or x)
		#forall x y z. (x or (y or z)) = ((x or y) or z)

		forall x. (x plus z) = x
		forall x. (z plus x) = x
		forall x y. (s(x) plus y) = s((x plus y))
		forall x y. (x plus s(y)) = s((x plus y))

		#forall x. (x times z) = z
		#forall x. (z times x) = z
		#forall x y. (s(x) times y) = (y plus (x times y))
		#forall x y. (x times s(y)) = (x plus (x times y))

		forall x y. if(t, x, y) = x
		forall x y. if(f, x, y) = y

		(z eq z) = t
		forall x. (z eq s(x)) = f
		forall x. (s(x) eq z) = f
		forall x y. (s(x) eq s(y)) = (x eq y)

		(z lt z) = f
		forall x. (z lt s(x)) = t
		forall x. (s(x) lt z) = f
		forall x y. (s(x) lt s(y)) = (x lt y)

		forall x y. (x ne y) = not((x eq y))
		forall x y. (x le y) = ((x lt y) or (x eq y))
		forall x y. (x gt y) = not((x le y))
		forall x y. (x ge y) = not((x lt y))

		isNone(none) = t
		forall x. isNone(just(x)) = f

		forall x. (none orDef x) = x
		forall x y. (just(x) orDef y) = x

		head(nil) = none
		forall x xs. head((x cons xs)) = just(x)

		tail(nil) = nil
		forall x xs. tail((x cons xs)) = xs

		forall x. (x at z) = head(x)
		forall xs i. (xs at s(i)) = (tail(xs) at i)

		length(nil) = z
		forall x xs. length((x cons xs)) = s(length(xs))

		rangeDesc(z) = nil
		forall x. rangeDesc(s(x)) = (x cons rangeDesc(x))
 	options
		prover = e
		timeout = 30
    triviality_check_best_effort = false
}

# example:
# if(isNone((rangeDesc(s(z)) at s(s(z)))), length((z cons (s(z) cons nil))), z)
# =
# if(((s(s(z)) ne s(z)) and (s(s(s(z))) ge s(s(z)))), (s(z) plus s(z)), s(z))
