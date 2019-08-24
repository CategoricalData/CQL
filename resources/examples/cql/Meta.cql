
schema cat = literal : empty {
	entities
		Ob Mor Pair
	foreign_keys
		id: Ob -> Mor
		dom: Mor -> Ob
		cod: Mor -> Ob
		Fst: Pair -> Mor
		Snd: Pair -> Mor
		comp: Pair -> Mor
	path_equations
		Ob.id.dom = Ob
		Ob.id.cod = Ob
		Pair.Fst.dom = Pair.comp.dom
		Pair.Fst.cod = Pair.Snd.dom
		Pair.Snd.cod = Pair.comp.cod
	options
} 

constraints theEDs = literal : cat {
//every composable pair gives a Pair
	forall f g:Mor
	where f.cod=g.dom ->
 	exists unique fg:Pair
 	where fg.Fst=f fg.Snd=g
//left unitality
	forall fid:Pair
	where fid.Fst.cod.id = fid.Snd
	->
	where fid.comp = fid.Fst
//right unitality
	forall idf:Pair
	where idf.Snd.dom.id = idf.Fst
	->
	where idf.comp = idf.Snd
//associativity
 	forall fg gh fgThenh fThengh: Pair 
 	where 
 		fg.Snd = gh.Fst 
 		fgThenh.Fst = fg.comp
 		fgThenh.Snd = gh.Snd
 		fThengh.Fst = fg.Fst
 		fThengh.Snd = gh.comp
 	->
 	where fgThenh.comp = fThengh.comp
}

// You can make similar constraints on CQLschema.
schema CQLschema = literal : empty {
	imports 
		cat
	entities
		StringAtt IntAtt
	foreign_keys
		on: StringAtt -> Ob
		on: IntAtt -> Ob
}

instance mysch = literal : CQLschema  {
	generators
		c d: Ob
		f : Mor
		name: StringAtt
	equations
		f.dom=c f.cod=d
		name.on=c		
}

typeside Ty = literal { 
	java_types
		String = "java.lang.String"
		Integer = "java.lang.Integer"
	java_constants
		String = "return input[0]"
		Integer = "return java.lang.Integer.parseInt(input[0])"
	java_functions
		plus : Integer,Integer -> Integer = "return (input[0] + input[1]).intValue()"
}
schema CQLinstance = literal : Ty {
	imports CQLschema
	entities
		recordID
		StringCell
		IntCell
		fkCell
	foreign_keys
		col: IntCell -> IntAtt    col: StringCell -> StringAtt
		row: IntCell -> recordID  row: StringCell -> recordID
		inTable: recordID -> Ob
		col: fkCell	-> Mor
		row : fkCell -> recordID
		pointsTo: fkCell -> recordID
	path_equations
		//col.on = row.inTable // StringCell
		//col.on = row.inTable // IntCell
		fkCell.col.dom = fkCell.row.inTable // fkCell
		//col.cod = row.pointsTo // fkCell	
	attributes
		value : StringCell -> String
		value : IntCell -> Integer
	
}

//add all constraints for categories + ...
constraints moreEDs = literal : CQLinstance {
	imports theEDs
//string fields are nonempty and atomic (you can get rid of one or the other if you choose)
	forall r:recordID sa:StringAtt
	where r.inTable=sa.on ->
 	exists unique value:StringCell
 	where value.col=sa value.row=r
//int fields are nonempty and atomic
	forall r:recordID sa:IntAtt
	where r.inTable=sa.on ->
 	exists unique value:IntCell
 	where value.col=sa value.row=r
// fk fields are nonempty and atomic 	
	forall r:recordID m:Mor
	where r.inTable=m.dom ->
 	exists unique value:fkCell
 	where value.col=m value.row=r
}

/*
For given CQL schemas X and Y, there may be no CQL schema MapXY such that MapXY-Inst ~ CQL mappings X -> Y.  Rather, the mappings X -> Y are isomorphic to the transforms X* => Y*, where X* and Y* are the Schema instances associated to X and Y. 

But, there is an CQL schema Mapping := CQLSchema * Arrow such that Mapping-Inst ~ all CQL mappings of any type.

Question: are the set of CQL transforms I => J given by the transforms I* => J*, where I* and J* are the Instance instances associated to I and J?

Question: Is there an CQL schema Transform := CQLInstance * Arrow such that TransformInst ~ all CQL transforms of any type.  Or should be an instance?  Or nothing at all?
*/
