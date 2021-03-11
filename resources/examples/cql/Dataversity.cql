html { (* "
<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">
 <html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">
 <body>
" *) }

html { (* "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://categoricaldata.net/css/simple.css\" />" *) }

md { (* "
---

## Type Side

Generally the typeside defines the way CQL kinds connect to a computational environment. This
particular typeside is limited to a string type used to display human readable labels and
descriptions. More generally, CQL type sides can be much more complex supporting such things as
functions that can be called in line to enhance the definition of schemas as well as queries. Such
considerations are beyond the scope of this demonstration.
" *) }

typeside tString = literal {
    external_types
        String -> "java.lang.String"
    external_parsers
        String -> "x => x"
	}

md { (* "
---

## Spans

A span is a shape (graph) LeftFoot <- Head -> RightFoot. In the context of a schema where the
objects are entities and the arrows are foreign keys, a span implements a many to many relationship.
In these two examples we are modeling first the relationship between medications and patients and
second the relationship between adverse events and patients. These two schemas illustrate how CQL
implements the two typs of arrows, foreign keys (entity -> entity) and attributes (entity -> type).
" *) }

schema sPatientMed = literal : tString {
	entities
		Patient PatientMed Medication
	foreign_keys
		PM_Patient : PatientMed -> Patient
		PM_Med : PatientMed -> Medication
	attributes
		PM_Dosage : PatientMed -> String
		PM_Patient_Name : Patient -> String
		PM_Med_Type : Medication -> String
}
schema sPatientAE = literal : tString {
	entities
		Patient PatientAE AdvrsEvnt
	foreign_keys
		PA_Patient : PatientAE -> Patient
		PA_AE : PatientAE -> AdvrsEvnt
	attributes
		PA_Date_Reported : PatientAE -> String
		PA_Patient_Name : Patient -> String
		PA_PT : AdvrsEvnt -> String
}

md { (* "
---

## Bridge

Schema with a master-detail relationship that connects a constrolled vocabulary for causal
categories to patient data that can be used to join to the two spans above.
" *) }
schema sCausality = literal : tString {
	entities
		PAM Causality
	foreign_keys
		AE_Causality : PAM -> Causality
	attributes
		AM_Ptnt_Name : PAM -> String
		AM_Med   : PAM -> String
		AM_PT  : PAM -> String
		Causal_Cat : Causality -> String
}


md { (* "
---

## Instance

Instances for each of the three schemas above. CQL instances differ from SQL instances in that
primary keys which provide a unique identifier for a record are replaced with generators. A
generator plays a more active role than an identifier they serve to generate records even in the
absense of data. This happens through the process of saturation. During the process of saturation
if data is missing (either foreign key values or literals) a typed null value is added that is
typed by the generator and the column where the value occures.
" *) }

instance iPatientMed = literal : sPatientMed {
	generators
		p1m p2m  : Patient
		m1 m2 m3  : Medication
		pm1 pm2 pm3 : PatientMed
	multi_equations
		PM_Dosage -> {pm1 "300mg", pm2 "20mg", pm3 "10mg"}
		PM_Patient_Name -> {p1m John, p2m Jane}
		PM_Med_Type -> {m1 Asprin, m2 Lisinopril, m3 Atorvastatin}
		PM_Med   -> {pm1 m1, pm2 m2, pm3 m3}
		PM_Patient -> {pm1 p1m, pm2 p1m, pm3 p2m}
}

instance iPatientAE = literal : sPatientAE {
	generators
	  p1a p2a : Patient
	  a1 a2 a3 a4 a5 a6 a7 a8 : AdvrsEvnt
	  pa1 pa2 pa3 pa4 pa5 pa6 : PatientAE
	multi_equations
		PA_Date_Reported -> {pa1 "01/20/2018", pa2 "01/20/2018", pa3 "01/20/2018", pa4 "01/10/2018", pa5 "01/10/2018", pa6 "01/10/2018"}
		PA_Patient_Name  -> {p1a John, p2a Jane}
		PA_PT -> {a1 rash, a2 gi_ulceration, a3 cramping, a4 dizziness,
					a5 cough, a6 fatigue, a7 joint_pain, a8 nausea}
		PA_Patient  -> {pa1 p1a, pa2 p1a, pa3 p1a, pa4 p2a, pa5 p2a, pa6 p2a}
		PA_AE   -> {pa1 a1, pa2 a2, pa3 a3, pa4 a4, pa5 a6, pa6 a7}
}

instance iCausality = literal : sCausality {
	generators
	  amp1 amp2 amp3 amp4 amp5 amp6 amp7 amp8 amp9 : PAM
	  c1 c2 c3 c4 c5 c6 : Causality
	multi_equations
		AM_Ptnt_Name -> {amp1 John, amp2 John, amp3 John,
							amp4 John, amp5 John, amp6 John,
							amp7 Jane, amp8 Jane, amp9 Jane}
		AM_PT   -> {amp1 rash, amp2 gi_ulceration, amp3 cramping,
						amp4 rash, amp5 gi_ulceration, amp6 cramping,
						amp7 dizziness, amp8 fatigue, amp9 joint_pain}
		AM_Med  -> {amp1 Asprin, amp2 Asprin, amp3 Asprin,
						amp4 Lisinopril, amp5 Lisinopril, amp6 Lisinopril,
						amp7 Atorvastatin, amp8 Atorvastatin, amp9 Atorvastatin}
		Causal_Cat -> {c1 Certain, c2 Probable_Likely, c3 Possible,
							c4 Unlikely, c5 Conditional_Unclassified, c6 Unassessable_Unclassifiable}
		AE_Causality -> {amp1 c3, amp2 c2, amp3 c3,
							amp4 c4, amp5 c4, amp6 c4,
							amp7 c3, amp8 c2, amp9 c2}
}

md { (* "
---

## Colimit

Construct the schema colimit by first adding the three schemas disjointly and then taking the
quotient. Taking the quotient means that some combination of entities and foreign keys are declared
to be equal. Intuitively the colimit is a construction that assembles a schema from a set of input
schemas that overlap according to the quotient. Entities and foreign keys declared equal define the
overlap.
" *) }
schema_colimit PAM_Assembly = quotient
	sPatientMed + sPatientAE + sCausality : tString {
    entity_equations
      sPatientMed.Patient = sPatientAE.Patient
}

md { (* "
---

## Deconstructing the colimit

This block of code is added for pedigogical reasons. The colimit is an object composed of a schema
together with a set of mappings, one associated with each of the source schemas. We provide each
such feature with a name for clarity in the remaining blocks of code.
" *) }
schema sPAM_Assembly = getSchema PAM_Assembly
mapping mPMtoPAM = getMapping PAM_Assembly sPatientMed
mapping mPAtoPAM = getMapping PAM_Assembly sPatientAE
mapping mCtoPAM = getMapping PAM_Assembly sCausality

md { (* "
---

## Migrating data using sigma

Having assembled the desired target schema we must now move the instance data associated with the
source schemas. Sigma is a CQL kind that moves instance data through the mappings created by the
colimit. The syntax takes one such schema mapping together with an instance associated with the
source schema and produces an instance in the target schema.
" *) }

instance iPMinPAM_Asmbly = sigma mPMtoPAM iPatientMed
instance iPAinPAM_Asmbly = sigma mPAtoPAM iPatientAE
instance iCinPAM_Asmbly = sigma mCtoPAM iCausality

md { (* "
---

## Adding instances

The coproduct construction is also sometimes called a sum. The relational analog is union distinct.
The effect of this construction is to disjointly combine the sepperate instances produced by sigma
in the previous step into a single instance.
" *) }
instance iPAM_Asmbly_Sum = coproduct iPMinPAM_Asmbly + iPAinPAM_Asmbly + iCinPAM_Asmbly : sPAM_Assembly


md { (* "
---

## Merging data

As in the case of the schema merge produced by the colimit above we now need to identify records
that are duplicates of one another and merge those records. The quotient query provides a mechanism
for identifying duplicates based on matching attribute values identified using the query syntax.
In this case we are assuming the program name is a natural key for programs. The quotient query is
analogous to the colimit quotient used above for merging schemas at the entity level.
" *) }
instance iPAM_Asmbly = quotient_query iPAM_Asmbly_Sum {
	entity
		Patient -> {
			from
				p1:Patient p2:Patient
			where
				p1.PA_Patient_Name = p2.PM_Patient_Name}
}

md { (* "
---

## Adding constraints

Constraints can be added using the query construction. The process begins with the definition of
the target schema that has the desired constraints.
" *) }

schema sPAM_Norm = literal : tString {
	entities
		Medication PatientMed Patient PatientAE
		AdverseEvent PAM Causality
	foreign_keys
		PAM_PM : PAM -> PatientMed
		PAM_PA : PAM -> PatientAE
		PM_Patient : PatientMed -> Patient
		PA_Patient : PatientAE -> Patient
		PM_Med : PatientMed -> Medication
		PA_AE : PatientAE -> AdverseEvent
		PAM_C : PAM -> Causality
	path_equations
		PAM.PAM_PM.PM_Patient = PAM.PAM_PA.PA_Patient
	attributes
		Dosage : PatientMed -> String
		Date_Reported : PatientAE -> String
		Med_Type : Medication -> String
		Patient_Name : Patient -> String
		Preferred_Term : AdverseEvent -> String
		Causal_Cat : Causality -> String
}

md { (* "
---

## CQL query

A CQL query differs from a relational query in that the result of the query is a schema rather than
a single table. As such, CQL can verify that the query definition is 'structure preserving'. This
means that the movement of data from the source schema to the target schema is done in a way that
ensures the data integrety of the source schema is faithfully enforced by the query mapping. This
verification of structure preservation takes place when the query is compiled.

A broad view of the CQL query construction reveals that a CQL query generalizes a SQL query in that
each entity block is analogous to a SQL query. That is, the assembly of a single target entity
is analogous to a SQL query but for the addition of the foreign keys block. The foreign keys block
provides a mechanism (described below) for construction of outgoing foreign keys. Such foreign keys
are in fact columns of the entity being defined and so CQL extends SQL by knitting together multiple
target entities with foreign keys. In this way a CQL query preserves the structure of the source
schema in constructing the target schema.

Another feature provided by the query construction is the creation of foreign keys. In this case
we are constructing foreign keys from the PAM entity to PatientAE and PatientMed entities. This is
implemented using a strategy that is equivalent to the SQL join. A join is an equationally defined
subset of a Cartesian product. In this specific case, we equate the three attribute values of the
PAM entity, each a path from the PAM entity to the string type, with a path starting from the target
entity and ending with the string type containing a matching attribute value. To this we add a third
equation that enforces the commutitive diagram defined in the target schema.

The population of the foreign key column involves assigning the target generator of the target
entity blocks where the two target entities are constructed (PatientAE and PatientMed) to the
corresponding target generator in the source entity block (PAM). For the mapping of existing foreign
key column (AE_Causality) we assign the target entity generator from the block where that entity is
constructed.

The remaining blocks are identity mappings.
" *) }

query qPAM_Norm = literal : sPAM_Assembly -> sPAM_Norm {
	entity
		PAM -> {
			from
				pam : PAM
				pama : PatientAE
				pamm : PatientMed
			where
				pam.AM_Ptnt_Name = pama.PA_Patient.PA_Patient_Name
				pam.AM_Med  = pamm.PM_Med.PM_Med_Type
				pam.AM_PT   = pama.PA_AE.PA_PT
				pamm.PM_Patient = pama.PA_Patient # What happens if you leave this out?
			foreign_keys
				# PAM_PM : AEMedOverPtnt -> PatientMed
				PAM_PM -> {pm -> pamm}
				# PAM_PA : AEMedOverPtnt -> PatientAE
				PAM_PA -> {pa -> pama}
				# PAM_C : AEMedOverPtnt -> Causality
				PAM_C -> {c -> pam.AE_Causality}
		}
	# All of the entities below are mapped one to one from the source to the target.
	entity
		PatientMed -> {
			from
				pm : PatientMed
			attributes
				Dosage -> pm.PM_Dosage
			foreign_keys
				# PM_Patient : PatientMed -> Patient
				PM_Patient -> {p -> pm.PM_Patient}
				# PM_Med : PatientMed -> Medication
				PM_Med -> {m -> pm.PM_Med}
		}
	entity
		PatientAE -> {
			from
				pa : PatientAE
			attributes
				Date_Reported -> pa.PA_Date_Reported
			foreign_keys
				# PA_Patient : PatientAE -> Patient
				PA_Patient -> {p -> pa.PA_Patient}
				# PA_AE : PatientAE -> Medication
				PA_AE -> {a -> pa.PA_AE}
		}
	entity
		AdverseEvent -> {
			from
				a : AdvrsEvnt
			attributes
				Preferred_Term -> a.PA_PT
		}
	entity
		Patient -> {
			from
				p : Patient
			attributes
				# Since we have forced the two source attributes to be the same using the
				# quotient query we can pick either one.
				Patient_Name -> p.PA_Patient_Name
		}
	entity
		Medication -> {
			from
				m : Medication
			attributes
				Med_Type -> m.PM_Med_Type
		}
	entity
		Causality -> {
			from
				c : Causality
			attributes
				Causal_Cat -> c.Causal_Cat
		}
}

md { (* "
---

## Migrate data

The CQL query construction generalizes the sigma data migration construction as follows. Sigma is
functional in that it is many to one. That is, sigma migrates data from many source entities into
a single target entity. The query construction acts like a generalized relation which can map many
source entities into many target entities. The movement of an instance is emplemented by the
application of the eval keyword. Eval takes a query and applies it to an instance of the source
schema and produces an instance of the target schema.
" *) }

instance iPAM_Norm = eval qPAM_Norm iPAM_Asmbly

md { (* "
---

## Denormalization using a CQL query

Our next application of a CQL query illustrates how we can construct a bus architecture where we
have collapsed the normalized schema above into a set of star schemas with conforming dimensions.
This effecively constructs a bus architecture using a single query. The schema below defines the
target we wish to construct consisting of three facts and three dimensions. The PAM facts reference
all three dimensions while the PatientAE and PatientMed facts reference the patient dimension along
with the AdverseEvent and Medication dimensions respectively.
" *) }

schema sPAMDWH = literal : tString {
	entities
		F_PAM F_PatientMed F_PatientAE D_Patient D_Medication D_AdverseEvent
	foreign_keys
		PAM_Medication : F_PAM -> D_Medication
		PAM_Patient : F_PAM -> D_Patient
		PAM_AdverseEvent : F_PAM -> D_AdverseEvent
		PM_Patient : F_PatientMed -> D_Patient
		PM_Medication : F_PatientMed -> D_Medication
		PA_AdverseEvent : F_PatientAE -> D_AdverseEvent
		PA_Patient : F_PatientAE -> D_Patient
	attributes
		# Facts
		Causal_Cat : F_PAM -> String
		Dosage : F_PatientMed -> String
		Date_Reported : F_PatientAE -> String
		# Context provided by dimensions
		Patient_Name : D_Patient -> String
		Med_Type : D_Medication -> String
		Preferred_Term : D_AdverseEvent -> String
}

md { (* "
---

Query implementing the structuring preserving map from the normalized schema to the bus
architecture of the schema above.
" *) }

query qPAMDWH = literal : sPAM_Norm -> sPAMDWH {
	entity
		F_PAM -> {
			from
				pam : PAM
			attributes
				Causal_Cat -> pam.PAM_C.Causal_Cat
			foreign_keys
				# PAM_Patient : F_PAM -> D_Patient
				# Question: What generator should be assign to the foreign key column PAM_Patient?
				# Answer: The generator p obtained by following the path from AEMedOverPtnt to PatientAE
				#         to Patient. Due to the commutitive square pam.PAM_PM.PM_Patient would have
				#				 worked equally well.
				PAM_Patient -> {p -> pam.PAM_PA.PA_Patient}
				# PAM_AdverseEvent : F_PAM -> D_AdverseEvent
				PAM_AdverseEvent -> {a -> pam.PAM_PA.PA_AE}
				# PAM_Medication : F_PAM -> D_Medication
				PAM_Medication -> {m -> pam.PAM_PM.PM_Med}
		}
	entity
		F_PatientMed -> {
			from
				pm : PatientMed
			attributes
				Dosage -> pm.Dosage
			foreign_keys
				# PM_Patient : F_PatientMed -> D_Patient
				PM_Patient -> {p -> pm.PM_Patient}
				# PM_Medication : F_PatientMed -> D_Medication
				PM_Medication -> {m -> pm.PM_Med}
		}
	entity
		F_PatientAE -> {
			from
				pa : PatientAE
			attributes
				Date_Reported -> pa.Date_Reported
			foreign_keys
				# PA_Patient : F_PatientAE -> D_Program
				PA_Patient -> {p -> pa.PA_Patient}
				# PA_AdverseEvent : F_PatientAE -> D_AdverseEvent
				PA_AdverseEvent -> {a -> pa.PA_AE}
		}
	entity
		D_AdverseEvent -> {
			from
				a : AdverseEvent
			attributes
				Preferred_Term -> a.Preferred_Term
		}
	entity
		D_Patient -> {
			from
				p : Patient
			attributes
				Patient_Name -> p.Patient_Name
		}
	entity
		D_Medication -> {
			from
				m : Medication
			attributes
				Med_Type -> m.Med_Type
		}
}

# Move the instance data.
instance iPAM_DWH = eval qPAMDWH iPAM_Norm


md { (* "
---

## Further denormalization using a CQL query

This final query illustrates how one can further denormalize the above data warehouse schema into
a set of reports each of which could be exported to an Excel spreadsheet.
" *) }

schema sPAM_Rpt = literal : tString {
	entities
		PAM_Rpt PM_Rpt PA_Rpt
	attributes
		# PM_Rpt
		Dosage : PM_Rpt -> String
		Patient_Name : PM_Rpt -> String
		Medication : PM_Rpt -> String
		# PA_Rpt
		Date_Reported : PA_Rpt -> String
		Patient_Name : PA_Rpt -> String
		AE_Preferred_Term : PA_Rpt -> String
		# PAM_Rpt
		Causal_Cat : PAM_Rpt -> String
		Patient_Name : PAM_Rpt -> String
		Medication : PAM_Rpt -> String
		AE_Preferred_Term : PAM_Rpt -> String
}

query qDWHRpt = literal : sPAMDWH -> sPAM_Rpt {
	entity
		PAM_Rpt -> {
			from
				pam : F_PAM
			attributes
				Causal_Cat -> pam.Causal_Cat
				Patient_Name -> pam.PAM_Patient.Patient_Name
				Medication -> pam.PAM_Medication.Med_Type
				AE_Preferred_Term -> pam.PAM_AdverseEvent.Preferred_Term
		}
	entity
		PM_Rpt -> {
			from
				m : F_PatientMed
			attributes
				Dosage -> m.Dosage
				Patient_Name -> m.PM_Patient.Patient_Name
				Medication -> m.PM_Medication.Med_Type
		}
	entity
		PA_Rpt -> {
			from
				a : F_PatientAE
			attributes
				Date_Reported -> a.Date_Reported
				Patient_Name -> a.PA_Patient.Patient_Name
				AE_Preferred_Term -> a.PA_AdverseEvent.Preferred_Term
		}
}

instance iPAM_Rpt_1 = eval qDWHRpt iPAM_DWH


md { (* "
---

## CQL queries compose

We close with a demonstration of how queries can be considered as arrows connecting schemas and hence
be composed into paths that are also queries. This means that the sequence of queries above can be
composed into a single query that takes you from the colimit schema to the reporting schema above.
" *) }

query qOneShot = [[qPAM_Norm;qPAMDWH];qDWHRpt]
instance iPAM_Rpt_2 = eval qOneShot iPAM_Asmbly
