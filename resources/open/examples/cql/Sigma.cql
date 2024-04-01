typeside Type = literal {
	types 
		String
	constants
		gecko frog human cow horse dolphin fish : String
}


schema C = literal : Type {
	entity
		Amphibian
	foreign_keys
		IsAL: LandAnimal
		IsAW: WaterAnimal
	attributes	
		attA: String 
	
	entities 
		LandAnimal
		WaterAnimal
	attributes
		attL: LandAnimal -> String 
		attW: WaterAnimal -> String
} 

instance I = literal : C {
	generators 
		a1 a2 : Amphibian
		l1 l2 l3 l4 l5 : LandAnimal
		w1 w2 w3 w4 : WaterAnimal
	equations
		 attA(a1) = gecko attA(a2) = frog
		 attL(l1) = gecko attL(l2) = frog 
		 attL(l3) = human attL(l4) = cow 
		 attL(l5) = horse attW(w1) = fish 
		 attW(w2) = gecko attW(w3) = frog 
		 attW(w4) = dolphin IsAL(a1) = l1 
		 IsAL(a2) = l2 IsAW(a1) = w2 IsAW(a2) = w3
} 

schema D = literal : Type {
	entities 
		yAmphibian
		yLandAnimal
		yWaterAnimal
		yAnimal
	foreign_keys
		yIsAL:yAmphibian->yLandAnimal
		yIsAW:yAmphibian->yWaterAnimal
		yIsALL:yLandAnimal->yAnimal
		yIsAWW:yWaterAnimal->yAnimal
	path_equations
		yAmphibian.yIsAL.yIsALL = yAmphibian.yIsAW.yIsAWW
	attributes
		yattA:yAmphibian->String 
		yattL:yLandAnimal->String 
		yattW:yWaterAnimal->String
} 

mapping F = literal : C -> D {
	entity
		Amphibian->yAmphibian
	foreign_keys
		IsAL -> yIsAL
		IsAW -> yIsAW
	attributes
		attA -> yattA

	entity
		LandAnimal->yLandAnimal
	attributes
		attL -> yattL

	entity
		WaterAnimal->yWaterAnimal
	attributes
		attW -> yattW
} 

instance J = sigma F I

instance I1 = literal : C {
	generators 
		xa1 : Amphibian
		xl1 xl2 xl3 xl4 : LandAnimal
		xw1 xw2 xw3: WaterAnimal
	equations
		attL(xl1) = gecko attL(xl2) = frog
		attL(xl3) = human attL(xl4) = cow
		attW(xw1) = fish attW(xw2) = gecko
		attW(xw3) = frog IsAL(xa1) = xl1
		IsAW(xa1) = xw2 attA(xa1) = gecko
} 

transform t = literal : I1 -> I {
	generators 
		xa1 -> a1
		xl1 -> l1
		xl2 -> l2
		xl3 -> l3
		xl4 -> l4
		xw1 -> w1
		xw2 -> w2
		xw3 -> w3
} 

transform u = sigma F t

instance K = delta F J

transform v = unit F I

###############################
#this particular migration can also be done as a colimit as follows.

schema Animal = literal : Type {
  entities
    A
  attributes
    name : A -> String
}

instance i_land = literal : Animal {
  generators
	la1 la2 la3 la4 la5 : A
  multi_equations
	name -> {la1 gecko, la2 frog, la3 human, la4 cow, la5 horse}  
}

instance i_water = literal : Animal {
  generators
	wa1 wa2 wa3 wa4 : A
  multi_equations
	name -> {wa1 fish, wa2 gecko, wa3 frog, wa4 dolphin} 
}

instance i_amphibian = literal : Animal {
  generators
	aa1 aa2 : A
  multi_equations
	name -> {aa1 gecko, aa2 frog}  
}

transform t_amphland = literal : i_amphibian -> i_land {
  generators
	aa1 -> la1 # match amphibian gecko to land gecko
	aa2 -> la2 # match amphibian frog to land frog
}

transform t_amphwater = literal : i_amphibian -> i_water {
  generators
	aa1 -> wa2 # match amphibian gecko to water gecko
	aa2 -> wa3 # match amphibian frog to water frog
}

graph Span = literal {
  nodes
	Amp Lan Wat # Amphibian LandAnimal WaterAnimal
  edges
	AmpLan : Amp -> Lan 
	AmpWat : Amp -> Wat 
}

instance i_animalmerge = colimit Span Animal {
  nodes
    Amp -> i_amphibian
    Lan -> i_land
    Wat -> i_water
  edges
    AmpLan -> t_amphland
    AmpWat -> t_amphwater
}
