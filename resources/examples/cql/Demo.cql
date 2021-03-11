# Universal Warehousing Demo

# user defined functions, in Java
typeside Type = literal {
	imports
		sql
	external_functions
		toUpper : String -> String  = "x => (x.isPresent() ? java.util.Optional.of(x.get().toUpperCase()) : java.util.Optional.empty())"
}

###########################################################################
# input schemas

schema AmphibianSchema = literal : Type {
	entities
		Amphibian Animal
	foreign_keys
		toAnimal : Amphibian -> Animal
	attributes
	     species : Animal -> String
}

schema LandSchema = literal : Type {
	imports
		AmphibianSchema
	entities
		LandAnimal
	foreign_keys
		isA : Amphibian  -> LandAnimal
		isA : LandAnimal -> Animal
	path_equations
		Amphibian.isA.isA = Amphibian.toAnimal
}

schema WaterSchema = literal : Type {
	imports
		AmphibianSchema
	entities
		WaterAnimal
	foreign_keys
		isA : Amphibian  -> WaterAnimal
		isA : WaterAnimal -> Animal
	path_equations
		Amphibian.isA.isA = Amphibian.toAnimal
}

###########################################################################
#input instances

instance LandInstance = import_csv "http://categoricaldata.net/demo/LandDB/" : LandSchema

instance WaterInstance = import_csv "http://categoricaldata.net/demo/WaterDB/" : WaterSchema

###########################################################################
#compute the canonical schema colimit, then enhances its semantics

schema_colimit UniversalWarehouseSchema = quotient LandSchema + WaterSchema : Type {
	entity_equations
		LandSchema.Animal = WaterSchema.Animal
		LandSchema.Amphibian = WaterSchema.Amphibian
	path_equations
		Amphibian.LandSchema_Amphibian_toAnimal = Amphibian.WaterSchema_Amphibian_toAnimal
}

schema_colimit ModifiedWarehouseSchema = modify UniversalWarehouseSchema {
	rename entities
		LandAnimal -> Land
		WaterAnimal -> Water
	rename foreign_keys
		Amphibian.LandSchema_Amphibian_isA -> land_is
		Amphibian.WaterSchema_Amphibian_isA -> water_is
		Amphibian.WaterSchema_Amphibian_toAnimal -> redundantW
		Amphibian.LandSchema_Amphibian_toAnimal -> redundantL
		#Water.WaterAnimal_isA -> isA
		#Land.LandAnimal_isA -> isA
	rename attributes
		Animal.LandSchema_Animal_species  -> land_species
		Animal.WaterSchema_Animal_species -> water_species
	remove foreign_keys
		Amphibian.redundantL -> land_is	. isA
		Amphibian.redundantW -> water_is. isA
}

################################################################################################
# migrate the data onto the warehouse schema

schema  WarehouseSchema  = getSchema  ModifiedWarehouseSchema
mapping LandToWarehouse  = getMapping ModifiedWarehouseSchema LandSchema
mapping WaterToWarehouse = getMapping ModifiedWarehouseSchema WaterSchema

instance LandInstanceForward  = sigma LandToWarehouse LandInstance
instance WaterInstanceForward = sigma WaterToWarehouse WaterInstance

instance UnmergedWarehouse = coproduct LandInstanceForward + WaterInstanceForward : WarehouseSchema

################################################################################################
# merge duplicates

instance Warehouse = quotient_query UnmergedWarehouse {
	entity Amphibian -> {from a:Amphibian b:Amphibian where toUpper(a.land_is.isA.land_species) = toUpper(b.water_is.isA.water_species)}

options
	quotient_use_chase = false
}

################################################################################################
# export the warehouse to SQL
command storeWH = export_jdbc_instance Warehouse "" ""

#view exported SQL instance
command view1 = exec_jdbc ""  {
	"SELECT * FROM \"Animal\""
	"SELECT * FROM \"Amphibian\""
	"SELECT * FROM \"Land\""
	"SELECT * FROM \"Water\""

"DROP TABLE \"Amphibian\""
"DROP TABLE \"Land\""
"DROP TABLE \"Water\""
	"DROP TABLE \"Animal\"" #drop tables in case we want to run the demo again
}

################################################################################################
# Application 0 : View warehouse as graph

schema WarehouseAsGraph = pivot Warehouse

################################################################################################
# Application 1 : Project (round-trip) the warehouse back onto the land schema

instance RoundTripLand = delta LandToWarehouse Warehouse

transform RoundTripLandFn = unit LandToWarehouse LandInstance

################################################################################################
# Application 2 : Project further onto the Amphibians schema with a query

query LandToAmphibians = literal : LandSchema -> AmphibianSchema {
	entity Amphibian -> {from amp:Amphibian
	                     #where amp.toAnimal.species = "frog"
						 foreign_keys toAnimal -> {anim -> amp.toAnimal}}

	entity Animal -> {from anim:Animal
	                  #where anim.species = "frog"
				      attributes species -> anim.species}
}

instance RoundTripAmphibians = eval LandToAmphibians RoundTripLand

################################################################################################
# Application 3 : Check and/or repair additional rules

constraints AllFksInjective = literal : WarehouseSchema {
	forall a1 a2 : Amphibian where a1.land_is  = a2.land_is  -> where a1 = a2
	forall a1 a2 : Amphibian where a1.water_is = a2.water_is -> where a1 = a2
	forall l1 l2 : Land      where l1.isA      = l2.isA      -> where l1 = l2
	forall w1 w2 : Water     where w1.isA      = w2.isA      -> where w1 = w2
}

command cmd = check AllFksInjective Warehouse

instance ANonInjectiveWarehouse = literal : WarehouseSchema {
	generators
		frog1 frog2 : Amphibian
		frog : Land
	equations
		frog1.land_is = frog
		frog2.land_is = frog
}

instance RepairedInjectiveWarehouse = chase AllFksInjective ANonInjectiveWarehouse
