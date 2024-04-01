package catdata.cql.gui;

import catdata.cql.gui.WarehouseUI.Example;

public class PharmaExample extends Example {

		@Override
		public String getName() {
			return "Pharma";
		}

		@Override
		public String getSources() {
			return """
					
schema S0 = literal : sql {
	entities
		O P T
	attributes
		l r : O -> String
		l r : P -> String
		l r : T -> String	
}	
instance S0Instance = literal : S0 {
	generators
		bp wt : T 
		p : P
	equations
		bp.l = BP bp.r = BP
		wt.l = Wt wt.r = Wt
		p.l = Peter p.r = Pete
}
constraints S0Constraints = literal : S0 { }

schema S1 = literal : sql {
	entities 
		Observation Person Gender Type 
	attributes	
     	f: Observation -> String #person
		h: Person -> String #gender
		g: Observation -> String #obstype

		id: Observation -> String
	    name: Person -> String
	    id: Gender -> String
	    id: Type -> String
} 
constraints S1Constraints = literal : S1 {
	forall x y : Observation where x.id = y.id -> where x = y
	forall x y : Person where x.name = y.name -> where x = y
	forall x y : Gender where x.id = y.id -> where x = y
	forall x y : Type where x.id = y.id -> where x = y

	forall x:Observation -> exists y:Person where x.f=y.name
	forall x:Observation -> exists y:Type where x.g=y.id
	forall x:Person -> exists y:Gender where x.h=y.id
}
instance S1Instance = literal : S1 {
	generators
		one two three : Observation
		Peter Paul : Person
	    M F :Gender
	    BloodPressure BodyWeight HeartRate: Type
	equations
		Peter.name = Peter Paul.name = Paul
		M.id = M F.id = F
		BloodPressure.id = BP
		BodyWeight.id = Wt
		HeartRate.id = HR
		one.f = Peter two.f = Peter three.f = Paul
		Peter.h = M Paul.h = M
		one.g = BP two.g = Wt three.g = HR
		one.id = o5 two.id = o6 three.id = o7
} 
command c1 = check S1Constraints S1Instance

schema S2 = literal : sql {
	entities 
		Observation Person Method Type
	foreign_keys 
		
	attributes
	    nombre: Person -> String
	    id: Type -> String
	    id: Method -> String
	    id: Observation -> String

	    F : Observation -> String #patient
		g1: Observation -> String #method
		g2: Method -> String #type
}
constraints S2Constraints = literal : S2 {
	forall x y : Observation where x.id = y.id -> where x = y
	forall x y : Person where x.nombre = y.nombre -> where x = y
	forall x y : Method where x.id = y.id -> where x = y
	forall x y : Type where x.id = y.id -> where x = y

	forall x:Observation -> exists y:Person where x.F=y.nombre
	forall x:Observation -> exists y:Method where x.g1=y.id
	forall x:Method -> exists y:Type where x.g2=y.id
}
instance S2Instance = literal : S2 {
	generators
		o1 o2 o3 o4 : Observation
		Pete Jane : Person
		m1 m2 m3 m4 : Method
		BP Wt : Type
	equations
	     Pete.nombre = Pete Jane.nombre = Jane
	     #Pete.id = 54321 Jane.id = 11111
	     BP.id = BP Wt.id = Wt
	     o1.F = Pete o2.F = Pete o3.F = Jane o4.F = Jane
	     o1.g1 = m1 o2.g1 = m2 o3.g1 = m3 o4.g1 = m1
	     m1.g2 = BP m2.g2 = BP m3.g2 = Wt m4.g2 = Wt
	     o1.id=o1 o2.id=o2 o3.id=o3 o4.id=o4
	     m1.id=m1 m2.id=m2 m3.id=m3 m4.id=m4
} 
command c2 = check S2Constraints S2Instance

										""";
		}

		@Override
		public String getLinks() {
			return """
					entity_isomorphisms
				linkO : S1.Observation -> S2.Observation
		linkP : S1.Person -> S2.Person
		linkT : S1.Type -> S2.Type
	equations
		oEq: forall x:S1.Observation, x.id = x.linkO.id
		tEq: forall x:S1.Type, x.id = x.linkT.id
		
		constraints
		fRule: forall x:S1_Observation xf:S1_Person xf2:S2_Person
  where x.f = xf.name  x.linkO.F = xf2.nombre  ->  where xf.linkP = xf2   

  gRule: forall x:S1_Observation xg:S1_Type m:S2_Method t:S2_Type   
  where x.g = xg.id  x.linkO.g1=m.id  m.g2=t.id -> where xg.linkT = t

  prefixRule: forall x : S1_Person y : S2_Person o : S0_P
  where o.l=x.name  o.r=y.nombre -> where x.linkP = y 										""";
		}

		@Override
		public String getTargets() {
			return "";
		}

	}