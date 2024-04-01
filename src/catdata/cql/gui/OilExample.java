package catdata.cql.gui;

import catdata.cql.gui.WarehouseUI.Example;

public class OilExample extends Example {

	@Override
	public String getName() {
		return "MASP";
	}
	
	@Override
	public String getOptions() {
		return "options timeout = 300\r\n"
				+ "	prover_simplify_max = 512\r\n"
				+ " require_consistency=false\r\n"
			 + "dont_validate_unsafe=true\r\n"

				;
	}

	@Override
	public String getSources() {
		return """
typeside Ty = literal {
	external_types
		Integer -> "java.lang.Integer"
		Float -> "java.math.BigDecimal"
		String -> "java.lang.String"
	external_parsers 
		Float -> "x => java.math.BigDecimal.valueOf(java.lang.Double.parseDouble(x))"
		Integer -> "x => java.lang.Integer.parseInt(x)"
		String -> "x => x"
	external_functions
		"+" : Float,Float->Float = "(x, y) => x.add(y)"	
		"*" : Float,Float->Float = "(x, y) => x.multiply(y)"	
		"-" : Float,Float->Float = "(x, y) => x.subtract(y)"	
		"/" : Float,Float->Float = "(x, y) => x.divide(y)"	
		"MAX" : Float,Float->Float = "(x, y) => x.max(y)"
		"MIN" : Float,Float->Float = "(x, y) => x.min(y)"		
	equations
		
}		


				schema Brandon = literal : Ty {
					entity
						"Header Info"
					attributes
						"Operator Name" "Well Name" "API Number" "Type of Well" "Rig Name" "Field"  : String
						"Water Depth" "Water Density" "RKB Height" "RKB-ML" : Float

					entity
						"Interval Info"
					foreign_keys
						Well : "Header Info"
						"OH Depth Yielding Highest MASP - RKB TVD" "Liner Top Depth" "Planned Section Total Depth" : "FG-PP Inputs"
					attributes
						"Interval No." "Interval Type" "Interval Name" "Preventer Type" : String
						"Hole Size" "Downhole Mud Weight" "Frac Gradient"
						"Preventer Size" "Wellhead Rating" "Annular Rating"
						"BOP Rating" "Annular Test Pressure" "BOP Test Pressure"
						"Mud Weight Casing Run In Hole (Surface)" "Mud Weight Casing Run In Hole (Downhole)"
						"Downhole Mud Test Weight" "Casing Test Pressure" "Safety Margin Applied to MAWP"
						"Downhole Formation Test Pressure" "Max Mud Weight at OH Depth (Downhole)"
						"Planned Wellhead or Hanger Depth"
							"Pore Pressure at OH Depth"
						"Max Mud Weight at OH Depth (Downhole)"	"Frac Gradient at Deepest Shoe" : Float

					entity
						"Casing Section"
					foreign_keys
						Interval : "Interval Info"
						"Total Vertical Depth" : "FG-PP Inputs"
					attributes
					 		"Casing Weight"	"Measured Depth" "Burst Rating" : Float
						"Casing Size" "Section No."	"Casing Grade"		"Collapse Rating"	 : String #todo!

					entity
						"Zone of Interest"
					foreign_keys
						"Casing Section" : "Casing Section"
						"RKB TVD" : "FG-PP Inputs"
					attributes
						"Zone Name" : String
						"Backup Pore Pressure"  : Float

					entity
						"MASP Calc. Step 1"
					foreign_keys
						"Casing Section" : "Casing Section"
						"RKB TVD" : "FG-PP Inputs"	 "Interval" : "Interval Info"
						"Zone Name" : "Zone of Interest"
					attributes
					"70% Burst (not corrected)"	"70% Burst (corrected)"	"De-Rated Percent"
						"EMW - Backup"	"Corrected Hydrostatic" : Float


					entity
						"MASP Calc. Step 2a"
					foreign_keys
						Interval : "Interval Info"		Well : "Header Info"
						"TVD Deepest OH"
						"TVD Shoe" :  "FG-PP Inputs"
					attributes
						"Mud Ratio"	"Gas Ratio" "Gas Gradient" "Mud TVD (BHP)"	"Gas TVD (BHP)"
						"Pressure at Deepest OH Depth" "SW Hydrostatic" "Frac Pressure at Deepest Shoe"
						"MASP BHP"	"MASP Shoe"	 "Mud Hydrostatic (BHP)"	"Gas Hydrostatic (BHP)"
						"Interface TVD (BHP)" "Mud TVD (Shoe)" "Gas TVD (Shoe)"	"Mud Hydrostatic (Shoe)"
						"Gas Hydrostatic (Shoe)" "Minimum MASP" : Float

					entity
						"MASP Calc. Step 2b"
					foreign_keys
						"Reference MASP": "MASP Calc. Step 2a"  Interval : "Interval Info"
						"Well" : "Header Info"
					attributes
						"MAWP" "MAWP Surface" "MAWP Surf + 500" : Float

					entity
						"Pressure Test 1"
					foreign_keys
						Interval : "Interval Info"
					attributes
						"Test Pressure" : Float

					entity
						"FG-PP Inputs"
					foreign_keys
						Well : "Header Info"
					attributes
						"RKB TVD" "Mid Pore Pressure" "Fracture Gradient" : Float

					observation_equations
						forall x:"Header Info".  "RKB-ML"(x) = ("Water Depth"(x) "+" "RKB Height"(x))

						forall x:"MASP Calc. Step 1". "70% Burst (corrected)"(x) =
						     ("70% Burst (not corrected)"(x) "-" "Corrected Hydrostatic"(x))

						forall x:"MASP Calc. Step 1". "70% Burst (not corrected)"(x) =
						    ("Burst Rating"("Casing Section"(x)) "*" "De-Rated Percent"(x))

						forall x:"MASP Calc. Step 1". "EMW - Backup"(x) =
						    ("Downhole Mud Weight"("Interval"(x)) "-" "Backup Pore Pressure"("Zone Name"(x)))

					    forall x:"MASP Calc. Step 1". "Corrected Hydrostatic"(x) =
						  (".052" "*" ("RKB TVD"("RKB TVD"(x)) "*" "EMW - Backup"(x)))

						forall x:"MASP Calc. Step 2a". "MASP BHP"(x) =
						  ((("Pressure at Deepest OH Depth"(x) "-" "SW Hydrostatic"(x)) "-" "Gas Hydrostatic (BHP)"(x)) "-" "Mud Hydrostatic (BHP)"(x))

						forall x:"MASP Calc. Step 2a". "MASP Shoe"(x) =
							((("Frac Pressure at Deepest Shoe"(x) "-" "Mud Hydrostatic (Shoe)"(x) ) "-" "Gas Hydrostatic (Shoe)"(x) ) "-" "SW Hydrostatic"(x))

						forall x:"MASP Calc. Step 2a". "Mud Hydrostatic (BHP)"(x) =
						  ((".052" "*" "Mud TVD (BHP)"(x)) "*" "Max Mud Weight at OH Depth (Downhole)"(Interval(x)))

						forall x:"MASP Calc. Step 2a". "Gas Hydrostatic (BHP)"(x) =
						  ("Gas Gradient"(x) "*" "Gas TVD (BHP)"(x))

						forall x:"MASP Calc. Step 2a". "Gas Hydrostatic (BHP)"(x) =
						  ("Gas Gradient"(x) "*" "Gas TVD (BHP)"(x))

						forall x:"MASP Calc. Step 2a". "SW Hydrostatic"(x) =
						  ("0.052" "*" ("Water Depth"("Well"(x)) "*" "Water Density"("Well"(x))))

						forall x:"MASP Calc. Step 2a". "Pressure at Deepest OH Depth"(x)
						 = (("RKB TVD"("TVD Deepest OH"(x)) "*" "0.052") "*" "Pore Pressure at OH Depth"("Interval"(x)))

						forall x:"MASP Calc. Step 2a". "Interface TVD (BHP)"(x) = ("RKB-ML"("Well"(x)) "+" ("Gas Ratio"(x) "*" ("RKB TVD"("TVD Deepest OH"(x)) "-" "RKB-ML"("Well"(x)))))
						forall x:"MASP Calc. Step 2a". "Mud TVD (BHP)"(x) = (("RKB TVD"("TVD Deepest OH"(x)) "-" "RKB-ML"("Well"(x))) "*" "Mud Ratio"(x))
						forall x:"MASP Calc. Step 2a". "Gas TVD (BHP)"(x) = (("RKB TVD"("TVD Deepest OH"(x)) "-" "RKB-ML"("Well"(x))) "*" "Gas Ratio"(x))
						forall x:"MASP Calc. Step 2a". "Frac Pressure at Deepest Shoe"(x) = (("RKB TVD"("TVD Shoe"(x)) "*" "0.052") "*" "Frac Gradient at Deepest Shoe"("Interval"(x)))

						forall x:"MASP Calc. Step 2a". "Mud TVD (Shoe)"(x) = MAX(0,(("RKB TVD"("TVD Shoe"(x)) "-" "Gas TVD (Shoe)"(x)) "-" "RKB-ML"("Well"(x))))
						forall x:"MASP Calc. Step 2a". "Gas TVD (Shoe)"(x) = MIN(("RKB TVD"("TVD Shoe"(x)) "-" "RKB-ML"("Well"(x))),("Interface TVD (BHP)"(x) "-" "RKB-ML"("Well"(x))))
						forall x:"MASP Calc. Step 2a". "Minimum MASP"(x) = MIN("MASP BHP"(x),"MASP Shoe"(x))

						forall x:"MASP Calc. Step 2b". "MAWP"(x) = ("Minimum MASP"("Reference MASP"(x)) "+" "SW Hydrostatic"("Reference MASP"(x)))
						forall x:"MASP Calc. Step 2b". "MAWP Surface"(x) = ("MAWP"(x) "-" (("0.052" "*" "RKB-ML"("Well"(x))) "*" "Downhole Mud Weight"("Interval"(x))))
						forall x:"MASP Calc. Step 2b". "MAWP Surf + 500"(x) = ("MAWP Surface"(x) "+" 500)



				}
				
				constraints BrandonConstraints = literal : Brandon {
					forall x:"MASP Calc. Step 1" -> where x."70% Burst (corrected)" = "-"("*"(x."De-Rated Percent", x."Casing Section"."Burst Rating"), "*"(x."RKB TVD"."RKB TVD", "*"(".052"@Float, "-"(x."Interval"."Downhole Mud Weight",x."Zone Name"."Backup Pore Pressure"))))
 forall x:"MASP Calc. Step 2a"  -> where x."MASP Shoe" = (x."Frac Pressure at Deepest Shoe" "-" (x."Mud Hydrostatic (Shoe)" "+" (x."Gas Hydrostatic (Shoe)" "+" x."SW Hydrostatic")))
	
		 forall x: "MASP Calc. Step 2a" -> where x."MASP BHP" = (x."Pressure at Deepest OH Depth" "-" (x."Mud Hydrostatic (BHP)" "+" (x."Gas Hydrostatic (BHP)" "+" x."SW Hydrostatic")))

				}
				
				schema James = literal : Ty {
					entity "Well Data Key"
					attributes
					Block Well WellBore "Field/Prospect" : String
					WD "RKB-ML" : Float

					entity "Casing Section Key"
					foreign_keys "Well Data Key" : "Well Data Key"
					attributes "Casing Section / Nominal Casing - Size" : String

					entity "Casing Key"
					foreign_keys "Top Casing Section / Nominal Casing - Size" : "Casing Section Key"
					"Item of Interest #" : "Item of Inteerest Key"
					"PPFP Key" :  "Pore Pressure Frac Pressure Key"
					attributes
					"Casing Type - Enter \\"Long String\\", \\"Liner\\" or \\"Tieback\\"" : String
					"Mud Weight Casing Run In (Downhole,)"
					"Mud Weight Casing Run In (Surface,)"
					"Hole Section TD - RKB TVD (Ft)"
					"Max Pore Pressure at Hole Section TD (ppg)" : Float



					entity "Item of Inteerest Key"
					foreign_keys
						"Top Casing Section / Nominal Casing - Size" : "Casing Section Key"
						"PPFP Key" : "Pore Pressure Frac Pressure Key"
						"CBT Key" : "Casing Burst Key"
						"Liner Hangoff Key" : "Liner Hangoff Key"
					attributes
						"Item of Interest #" "Item of Interest Casing Section - Weight/Grade" : String
						"Item of Interest Depth - RKB TVD (Ft)"
						"Item of Interest Casing Section - Size"
						"Item of Interest - Burst Rating (psi)"
						"Item of Interest Backup Pore Pressure (ppg)" : Float


					entity "Shoe Track Key"
					foreign_keys
						"Item of Interest #" : "Item of Inteerest Key"
						"Top Casing Section / Nominal Casing - Size" : "Casing Section Key"
					attributes
						"Shoe Frac Gradient (ppg)" "Planned FIT (Downhole,ppg)"
						"Planned FIT (Surface,ppg)" : Float




					entity "Liner Hangoff Key"
					foreign_keys
						 "Top Casing Section / Nominal Casing - Size" : "Casing Section Key"
					attributes
						"Will a liner be hung off inside this string? (Yes/No)" : String
						"Mud Weight Used to Test 22 Casing RKB-ML (ppg)"
						"Mud Weight Used to Test Casing/Liner (Downhole,ppg)"
						"Mud Weight Used to Test Casing/Liner (Surface,ppg)"
						"Mud Weight Used to Perform FIT/LOT (Downhole,ppg)"
						"Mud Weight Used to Perform FIT/LOT (Surface,ppg)"
						"Safety Margin Applied to MAWPsurf for calculating Ptest1" : Float



					entity "Exposed Shoe Key"
					foreign_keys
					"PPFP Key" :  "Pore Pressure Frac Pressure Key"
						 "Top Casing Section / Nominal Casing - Size" : "Casing Section Key"
					attributes
						"Deepest Exposed Shoe Below This Shoe - RKB TVD (Ft)"
						"Frac Gradient at Deepest Exposed Shoe (ppg)" : Float



					entity "OH Key"
					foreign_keys "PPFP Key" :  "Pore Pressure Frac Pressure Key"
						"Top Casing Section / Nominal Casing - Size" :  "Casing Section Key"
					attributes
						"Open Hole Depth yielding highest MASP- RKB TVD (Ft)"
						"Pore Pressure at OH Depth (ppg)"
						"Max Mud Weight at OH Depth (Downhole,ppg)"
						"Max Mud Weight at OH Depth (Surface,ppg)"
						"Kick Fluid Gradient - Gas or Oil (psi/ft)"
						"Kick Fluid Gradient if Gas per BSEE (psi/ft)" : Float




					entity "Burst Calculation Key"
					foreign_keys
						"Item of Interest" : "Item of Inteerest Key"
					attributes
						"Material Utilization Factor"
						"Burst Rating"
						"DHEMW"
						"Backside EMW"
						"Constant"
						"TVD"
						"Burst Rating Corrected for MW & Backside" : Float


					entity "Mud Gradient Key"
					foreign_keys "Casing Section Key" : "Casing Section Key"
					attributes
					  "Mud Fraction" "Gas Fraction" "TVD Interface" : Float


					entity "MASP Open Hole Key"
					foreign_keys
						"Casing Section Key" : "Casing Section Key"
						"PPFP Key" : "Pore Pressure Frac Pressure Key"
						"OH Key" : "OH Key"
						"Mud Gradient Key" : "Mud Gradient Key"
					attributes
					"Open Hole Depth yielding highest MASP- RKB TVD (Ft)"
					"Pore Pressure at OH Depth (ppg)" "Constant" "TVDmud" "MW" "TVDHC"
					"HC Grad." "Sw Hydrostatic" "MASP Open Hole" : Float



					entity "MASP Shoe Key"
					foreign_keys
					"Casing Section Key" : "Casing Section Key"
					"PPFP Key" : "Pore Pressure Frac Pressure Key"
					"OH Key" : "OH Key"
					"Exposed Shoe Key" : "Exposed Shoe Key"
					attributes
					"Deepest Exposed Shoe Below This Shoe - RKB TVD (Ft)"
					"Frac Gradient at Deepest Exposed Shoe (ppg)"
					"Constant" "TVDmud" "MW" "TVDHC" "HC Grad."
					"Sw Hydrostatic" "MASP Shoe" : Float

					entity "MASP Key"
					foreign_keys
						"Casing Section Key" : "Casing Section Key"
						"MASP Open Hole Key" : "MASP Open Hole Key"
						"MASP Shoe Key" : "MASP Shoe Key"
					attributes "MASP Openhole" "MASP Shoe" "MASP" : Float

					entity "Casing Burst Key"
					attributes
					"Weight/Grade" Comment : String
					Size "Lowest Value Burst" "Lowest Value Collapse"
					"Pipe Body (for 123S) Burst" "Pipe Body (for 123S) Collapse"  : Float

					entity "Pore Pressure Frac Pressure Key"
					attributes "RKB TVD"	"Pore Pressure_mid (ppg)" "Fracture Gradient (ppg)"
					"Salt FG (Using OBG + 1000 psi (ppg)"	: Float


					observation_equations
					#1
						forall x:"Casing Key".  x."Max Pore Pressure at Hole Section TD (ppg)" = x."PPFP Key"."Pore Pressure_mid (ppg)"

						#2
						forall x:"Item of Inteerest Key". x."Item of Interest - Burst Rating (psi)" = x."CBT Key"."Lowest Value Burst"

				#3
						forall x:"Item of Inteerest Key". x."Item of Interest Backup Pore Pressure (ppg)" = x."PPFP Key"."Pore Pressure_mid (ppg)"

				#4
						forall x:"OH Key".  x."Pore Pressure at OH Depth (ppg)" = x."PPFP Key"."Pore Pressure_mid (ppg)"

				#5 tbd
				#		forall x:"OH Key".  x."Pore Pressure at OH Depth (ppg)" = x."PPFP Key"."Pore Pressure_mid (ppg)"

				#6
						forall x:"Burst Calculation Key".  x."Burst Rating" = x."Item of Interest"."Item of Interest - Burst Rating (psi)"

				#7
						forall x:"Burst Calculation Key".  x."Backside EMW" = x."Item of Interest"."Item of Interest Backup Pore Pressure (ppg)"
				#8
						forall x:"Burst Calculation Key".  x."TVD" = x."Item of Interest"."Item of Interest Depth - RKB TVD (Ft)"
				#9
						forall x:"Burst Calculation Key".  x."Burst Rating Corrected for MW & Backside"
							= ((x."Material Utilization Factor" "*" x."Burst Rating") "-" (((x.DHEMW "-" x."Backside EMW") "*" x.Constant) "*" x.TVD))
				#10
						forall x:"MASP Open Hole Key".  x."MASP Open Hole"
						= "-"("-"("-"((x."Open Hole Depth yielding highest MASP- RKB TVD (Ft)" "*" (x."Pore Pressure at OH Depth (ppg)" "*" x.Constant)),
						((x.TVDmud "*" x.MW) "*" x.Constant) ), (x.TVDHC "*" x."HC Grad.")), x."Sw Hydrostatic")
				#11
						forall x:"MASP Open Hole Key".  x."Pore Pressure at OH Depth (ppg)" =
						 x."OH Key"."Pore Pressure at OH Depth (ppg)"
				#12
						forall x:"MASP Open Hole Key".  x."MW" =
						 x."OH Key"."Max Mud Weight at OH Depth (Downhole,ppg)"
					#13
						forall x:"MASP Open Hole Key".  x."HC Grad." =
						 x."OH Key"."Kick Fluid Gradient if Gas per BSEE (psi/ft)"
				#14
						 forall x:"MASP Open Hole Key".  x."TVDmud" = MAX(0, "*"("-"(x."Open Hole Depth yielding highest MASP- RKB TVD (Ft)",
					 x."Casing Section Key"."Well Data Key"."RKB-ML"), x."Mud Gradient Key"."Mud Fraction"))
				#15
						 forall x:"MASP Open Hole Key".  x."TVDHC" = MAX(0, "*"("-"(x."Open Hole Depth yielding highest MASP- RKB TVD (Ft)",
					 x."Casing Section Key"."Well Data Key"."RKB-ML"), x."Mud Gradient Key"."Gas Fraction"))
				#16
						forall x:"MASP Shoe Key".  x."MASP Shoe"
						= "-"("-"("-"((x."Deepest Exposed Shoe Below This Shoe - RKB TVD (Ft)" "*" (x."Frac Gradient at Deepest Exposed Shoe (ppg)" "*" x.Constant)),
						((x.TVDmud "*" x.MW) "*" x.Constant) ), (x.TVDHC "*" x."HC Grad.")), x."Sw Hydrostatic")

				#18 6/30 changed from point "OH Key"."Pore Pressure at OH Depth (ppg)"
						forall x:"MASP Shoe Key".  x."MW"
						= x."OH Key"."Max Mud Weight at OH Depth (Downhole,ppg)"
				#19
						forall x:"MASP Shoe Key".  x."Frac Gradient at Deepest Exposed Shoe (ppg)"
						= x."Exposed Shoe Key"."Frac Gradient at Deepest Exposed Shoe (ppg)"
				#20
						forall x:"MASP Key". x.MASP = MIN(x."MASP Openhole", x."MASP Shoe")
				#21
						forall x:"MASP Key". x."MASP Openhole" = x."MASP Open Hole Key"."MASP Open Hole"
				#22
						forall x:"MASP Key". x."MASP Shoe" = x."MASP Shoe Key"."MASP Shoe"

				#23
						forall x:"Exposed Shoe Key". x."Frac Gradient at Deepest Exposed Shoe (ppg)"=x."PPFP Key"."Salt FG (Using OBG + 1000 psi (ppg)"


						#references h$54 - not path eq.
						#forall x:"Burst Calculation Key".  x."DHEMW" = x."Item of Interest"."Liner Hangoff Key"."Mud Weight Used to Test 22 Casing RKB-ML (ppg)"
}

				instance BrandonInstance = literal : Brandon {
					generators
						ABC1 : "Header Info"
						DEF1 DEF2 DEF3 DEF4 DEF5 : "Interval Info"
						GHI1 GHI2 GHI3 GHI4 GHI5 GHI6 GHI7 GHI8 GHI9 GHI10 GHI11 GHI12 GHI13 GHI14 GHI15 GHI16
						GHI17 GHI18 GHI19 GHI20 GHI21 GHI22 GHI23 GHI24 GHI25 GHI26 GHI27 GHI28 GHI29 : "Casing Section"
						"JKL1" : "Zone of Interest"
						"JKL2" : "Zone of Interest"
						"JKL3" : "Zone of Interest"
						"JKL4" : "Zone of Interest"
						"JKL5" : "Zone of Interest"
						"JKL6" : "Zone of Interest"
						"JKL7" : "Zone of Interest"
						"JKL8" : "Zone of Interest"
						"JKL9" : "Zone of Interest"
						"JKL10" : "Zone of Interest"
						"JKL11" : "Zone of Interest"
						"JKL12" : "Zone of Interest"
						"JKL13" : "Zone of Interest"
						"JKL14" : "Zone of Interest"
						"JKL15" : "Zone of Interest"
						"JKL16" : "Zone of Interest"
						"JKL17" : "Zone of Interest"
						"JKL18" : "Zone of Interest"
						"JKL19" : "Zone of Interest"
						"JKL20" : "Zone of Interest"
						"JKL21" : "Zone of Interest"
						"JKL22" : "Zone of Interest"
						"JKL23" : "Zone of Interest"
						"JKL24" : "Zone of Interest"
						"JKL25" : "Zone of Interest"
						"JKL26" : "Zone of Interest"
						"JKL27" : "Zone of Interest"
						"JKL28" : "Zone of Interest"
						"JKL29" : "Zone of Interest"
						"JKL30" : "Zone of Interest"

						"MNO1" : "MASP Calc. Step 1"
						"MNO2" : "MASP Calc. Step 1"
						"MNO3" : "MASP Calc. Step 1"
						"MNO4" : "MASP Calc. Step 1"
						"MNO5" : "MASP Calc. Step 1"
						"MNO6" : "MASP Calc. Step 1"
						"MNO7" : "MASP Calc. Step 1"
						"MNO8" : "MASP Calc. Step 1"
						"MNO9" : "MASP Calc. Step 1"
						"MNO10" : "MASP Calc. Step 1"
						"MNO11" : "MASP Calc. Step 1"
						"MNO12" : "MASP Calc. Step 1"
						"MNO13" : "MASP Calc. Step 1"
						"MNO14" : "MASP Calc. Step 1"
						"MNO15" : "MASP Calc. Step 1"
						"MNO16" : "MASP Calc. Step 1"
						"MNO17" : "MASP Calc. Step 1"
						"MNO18" : "MASP Calc. Step 1"
						"MNO19" : "MASP Calc. Step 1"
						"MNO20" : "MASP Calc. Step 1"
						"MNO21" : "MASP Calc. Step 1"
						"MNO22" : "MASP Calc. Step 1"
						"MNO23" : "MASP Calc. Step 1"
						"MNO24" : "MASP Calc. Step 1"
						"MNO25" : "MASP Calc. Step 1"
						"MNO26" : "MASP Calc. Step 1"
						"MNO27" : "MASP Calc. Step 1"
						"MNO28" : "MASP Calc. Step 1"
						"MNO29" : "MASP Calc. Step 1"
						"MNO30" : "MASP Calc. Step 1"

						"PQR1" : "MASP Calc. Step 2a"
						"PQR2" : "MASP Calc. Step 2a"
						"PQR3" : "MASP Calc. Step 2a"
						"PQR4" : "MASP Calc. Step 2a"
						"PQR5" : "MASP Calc. Step 2a"

						"STU1" : "MASP Calc. Step 2b"
						"STU2" : "MASP Calc. Step 2b"
						"STU3" : "MASP Calc. Step 2b"
						"STU4" : "MASP Calc. Step 2b"
						"STU5" : "MASP Calc. Step 2b"

						YZ3354 YZ1539 YZ3450 YZ3355 YZ3655 YZ3454
						YZ3940 YZ3146 YZ4263 YZ3357 YZ3886 YZ210
						YZ54 YZ3988 YZ1299 YZ3848 YZ133 YZ3650
						YZ699 YZ1 YZ3358 YZ3460 YZ56 YZ3648
						YZ3549 YZ2 YZ3352 YZ3551 YZ3360
						YZ113 YZ57 YZ2265 YZ3441 YZ3361
						YZ4102 YZ3998 YZ3157 YZ3356 YZ3858
						YZ3359 YZ55 YZ3657 YZ117 YZ3658 YZ208
						YZ119 YZ719 YZ3946 YZ4245 YZ3459 YZ4264 YZ4265  : "FG-PP Inputs"

					equations
						ABC1."Operator Name" = "Chevron"
						ABC1."Well Name" = "IS001 STOOBP00"
						ABC1."API Number" = "123456789"
						ABC1."Type of Well" = "Development"
						ABC1."Rig Name" = "Valaris DS-18"
						ABC1."Water Depth"  = "6936"
						ABC1."Water Density" = "8.6"
						ABC1."RKB Height" = "81"
						ABC1."Field" = "St. Malo"
						ABC1."RKB-ML" = "7017"




				"DEF1"."Interval No." = "1.00000000"
				"DEF1"."Interval Type" = "Casing"
				"DEF1"."Interval Name" = "Surface"
				"DEF1"."Hole Size" = "26.00000000"
				"DEF1"."Downhole Mud Weight" = "8.60000000"
				"DEF1"."Frac Gradient" = "13.20000000"
				"DEF1"."Preventer Type" = "Blowout"
				"DEF1"."Preventer Size" = "18.75000000"
				"DEF1"."Wellhead Rating" = "15000.00000000"
				"DEF1"."Annular Rating" = "10000.00000000"
				"DEF1"."BOP Rating" = "15000.00000000"
				"DEF1"."Annular Test Pressure" = "6800.00000000"
				"DEF1"."BOP Test Pressure" = "11800.00000000"
				"DEF1"."Mud Weight Casing Run In Hole (Surface)" = "12.50000000"
				"DEF1"."Mud Weight Casing Run In Hole (Downhole)" = "12.50000000"
				"DEF1"."Downhole Mud Test Weight" = "8.60000000"
				"DEF1"."Casing Test Pressure" = "3100.00000000"
				"DEF1"."Safety Margin Applied to MAWP" = "500.00000000"
				"DEF1"."Downhole Formation Test Pressure" = "13.20000000"
				"DEF1"."Max Mud Weight at OH Depth (Downhole)" = "12.10000000"
				"DEF1"."Planned Wellhead or Hanger Depth" = "7004.00000000"
				"DEF1"."Planned Section Total Depth" = "YZ719"
				"DEF1"."OH Depth Yielding Highest MASP - RKB TVD" = "YZ3459"
				"DEF1"."Pore Pressure at OH Depth" = "12.00800000"
				"DEF1"."Max Mud Weight at OH Depth (Downhole)" = "12.10000000"
				"DEF1"."Frac Gradient at Deepest Shoe" = "13.20900000"
				"DEF1"."Well" = "ABC1"

				"DEF2"."Interval No." = "2.00000000"
				"DEF2"."Interval Type" = "Liner"
				"DEF2"."Interval Name" = "Intermediate"
				"DEF2"."Hole Size" = "21.00000000"
				"DEF2"."Downhole Mud Weight" = "12.10000000"
				"DEF2"."Frac Gradient" = "14.40000000"
				"DEF2"."Liner Top Depth" = "YZ56"
				"DEF2"."Preventer Type" = "Blowout"
				"DEF2"."Preventer Size" = "18.75000000"
				"DEF2"."Wellhead Rating" = "15000.00000000"
				"DEF2"."Annular Rating" = "10000.00000000"
				"DEF2"."BOP Rating" = "15000.00000000"
				"DEF2"."Annular Test Pressure" = "5600.00000000"
				"DEF2"."BOP Test Pressure" = "9100.00000000"
				"DEF2"."Mud Weight Casing Run In Hole (Surface)" = "11.90000000"
				"DEF2"."Mud Weight Casing Run In Hole (Downhole)" = "12.10000000"
				"DEF2"."Downhole Mud Test Weight" = "12.10000000"
				"DEF2"."Casing Test Pressure" = "5300.00000000"
				"DEF2"."Safety Margin Applied to MAWP" = "500.00000000"
				"DEF2"."Downhole Formation Test Pressure" = "14.40000000"
				"DEF2"."Max Mud Weight at OH Depth (Downhole)" = "13.60000000"
				"DEF2"."Planned Wellhead or Hanger Depth" = "7287.00000000"
				"DEF2"."Planned Section Total Depth" = "YZ3460"
				"DEF2"."OH Depth Yielding Highest MASP - RKB TVD" = "YZ3886"
				"DEF2"."Pore Pressure at OH Depth" = "13.97300000"
				"DEF2"."Max Mud Weight at OH Depth (Downhole)" = "13.60000000"
				"DEF2"."Frac Gradient at Deepest Shoe" = "15.74800000"
				"DEF2"."Well" = "ABC1"
				"DEF3"."Interval No." = "3.00000000"
				"DEF3"."Interval Type" = "Liner"
				"DEF3"."Interval Name" = "Intermediate"
				"DEF3"."Hole Size" = "16.50000000"
				"DEF3"."Downhole Mud Weight" = "14.10000000"
				"DEF3"."Frac Gradient" = "15.59000000"
				"DEF3"."Liner Top Depth" = "YZ3352"
				"DEF3"."Preventer Type" = "Blowout"
				"DEF3"."Preventer Size" = "18.75000000"
				"DEF3"."Wellhead Rating" = "15000.00000000"
				"DEF3"."Annular Rating" = "10000.00000000"
				"DEF3"."BOP Rating" = "15000.00000000"
				"DEF3"."Annular Test Pressure" = "4800.00000000"
				"DEF3"."BOP Test Pressure" = "8300.00000000"
				"DEF3"."Mud Weight Casing Run In Hole (Surface)" = "13.90000000"
				"DEF3"."Mud Weight Casing Run In Hole (Downhole)" = "14.10000000"
				"DEF3"."Downhole Mud Test Weight" = "14.10000000"
				"DEF3"."Casing Test Pressure" = "2700.00000000"
				"DEF3"."Safety Margin Applied to MAWP" = "500.00000000"
				"DEF3"."Downhole Formation Test Pressure" = "14.40000000"
				"DEF3"."Max Mud Weight at OH Depth (Downhole)" = "13.60000000"
				"DEF3"."Planned Wellhead or Hanger Depth" = "23766.00000000"
				"DEF3"."Planned Section Total Depth" = "YZ3946"
				"DEF3"."OH Depth Yielding Highest MASP - RKB TVD" = "YZ3940"
				"DEF3"."Pore Pressure at OH Depth" = "13.87900000"
				"DEF3"."Max Mud Weight at OH Depth (Downhole)" = "13.60000000"
				"DEF3"."Frac Gradient at Deepest Shoe" = "15.74800000"
				"DEF3"."Well" = "ABC1"
				"DEF4"."Interval No." = "4.00000000"
				"DEF4"."Interval Type" = "Liner"
				"DEF4"."Interval Name" = "Production"
				"DEF4"."Hole Size" = "12.25000000"
				"DEF4"."Downhole Mud Weight" = "13.40000000"
				"DEF4"."Frac Gradient" = "15.75000000"
				"DEF4"."Liner Top Depth" = "YZ3650"
				"DEF4"."Preventer Type" = "Blowout"
				"DEF4"."Preventer Size" = "18.75000000"
				"DEF4"."Wellhead Rating" = "15000.00000000"
				"DEF4"."Annular Rating" = "10000.00000000"
				"DEF4"."BOP Rating" = "15000.00000000"
				"DEF4"."Annular Test Pressure" = "4900.00000000"
				"DEF4"."BOP Test Pressure" = "8300.00000000"
				"DEF4"."Mud Weight Casing Run In Hole (Surface)" = "13.20000000"
				"DEF4"."Mud Weight Casing Run In Hole (Downhole)" = "13.40000000"
				"DEF4"."Downhole Mud Test Weight" = "13.40000000"
				"DEF4"."Casing Test Pressure" = "3600.00000000"
				"DEF4"."Safety Margin Applied to MAWP" = "500.00000000"
				"DEF4"."Max Mud Weight at OH Depth (Downhole)" = "13.60000000"
				"DEF4"."Planned Wellhead or Hanger Depth" = "25251.00000000"
				"DEF4"."Planned Section Total Depth" = "YZ4263"
				"DEF4"."OH Depth Yielding Highest MASP - RKB TVD" = "YZ4263"
				"DEF4"."Pore Pressure at OH Depth" = "13.45900000"
				"DEF4"."Max Mud Weight at OH Depth (Downhole)" = "13.60000000"
				"DEF4"."Frac Gradient at Deepest Shoe" = "15.74800000"
				"DEF4"."Well" = "ABC1"
				"DEF5"."Interval No." = "5.00000000"
				"DEF5"."Interval Type" = "Casing"
				"DEF5"."Interval Name" = "Tieback"
				"DEF5"."Hole Size" = "12.25000000"
				"DEF5"."Downhole Mud Weight" = "13.90000000"
				"DEF5"."Preventer Type" = "Blowout"
				"DEF5"."Preventer Size" = "18.75000000"
				"DEF5"."Wellhead Rating" = "15000.00000000"
				"DEF5"."Annular Rating" = "10000.00000000"
				"DEF5"."BOP Rating" = "15000.00000000"
				"DEF5"."Annular Test Pressure" = "4900.00000000"
				"DEF5"."BOP Test Pressure" = "8300.00000000"
				"DEF5"."Mud Weight Casing Run In Hole (Surface)" = "13.70000000"
				"DEF5"."Mud Weight Casing Run In Hole (Downhole)" = "13.90000000"
				"DEF5"."Downhole Mud Test Weight" = "13.90000000"
				"DEF5"."Casing Test Pressure" = "7200.00000000"
				"DEF5"."Safety Margin Applied to MAWP" = "500.00000000"
				"DEF5"."Max Mud Weight at OH Depth (Downhole)" = "13.90000000"
				"DEF5"."Planned Wellhead or Hanger Depth" = "7007.00000000"
				"DEF5"."Planned Section Total Depth" = "YZ4245"
				"DEF5"."OH Depth Yielding Highest MASP - RKB TVD" = "YZ4263"
				"DEF5"."Pore Pressure at OH Depth" = "13.45900000"
				"DEF5"."Max Mud Weight at OH Depth (Downhole)" = "13.90000000"
				"DEF5"."Frac Gradient at Deepest Shoe" = "15.74800000"
				"DEF5"."Well" = "ABC1"




				"GHI1"."Section No." = "1.00000000"
				"GHI1"."Casing Size" = "23\\""@String
				"GHI1"."Casing Grade" = "23x2 HPWH Extension Jt"
				"GHI1"."Burst Rating" = "13722.00000000"
				"GHI1"."Collapse Rating" = "12545.00000000"
				"GHI1"."Measured Depth" = "7015.00000000"
				"GHI1"."Total Vertical Depth" = "YZ2"
				"GHI1"."Interval" = "DEF1"
				"GHI2"."Section No." = "2.00000000"
				"GHI2"."Casing Size" = "23\\""@String
				"GHI2"."Casing Weight" = "453.04000000"
				"GHI2"."Casing Grade" = "X-80Q (H-100DM/QT)"
				"GHI2"."Burst Rating" = "13890.00000000"
				"GHI2"."Collapse Rating" = "4000.00000000"
				"GHI2"."Measured Depth" = "7276.00000000"
				"GHI2"."Total Vertical Depth" = "YZ54"
				"GHI2"."Interval" = "DEF1"
				"GHI3"."Section No." = "3.00000000"
				"GHI3"."Casing Size" = "23\\""@String
				"GHI3"."Casing Grade" = "16\\" 12.5K SA Upper"@String
				"GHI3"."Burst Rating" = "15600.00000000"
				"GHI3"."Collapse Rating" = "14200.00000000"
				"GHI3"."Measured Depth" = "7279.00000000"
				"GHI3"."Total Vertical Depth" = "YZ54"
				"GHI3"."Interval" = "DEF1"
				"GHI4"."Section No." = "4.00000000"
				"GHI4"."Casing Size" = "22\\""@String
				"GHI4"."Casing Grade" = "16\\" 12.5K SA Lower"@String
				"GHI4"."Burst Rating" = "7540.00000000"
				"GHI4"."Collapse Rating" = "7200.00000000"
				"GHI4"."Measured Depth" = "7282.00000000"
				"GHI4"."Total Vertical Depth" = "YZ55"
				"GHI4"."Interval" = "DEF1"
				"GHI5"."Section No." = "5.00000000"
				"GHI5"."Casing Size" = "22\\""@String
				"GHI5"."Casing Weight" = "224.21000000"
				"GHI5"."Casing Grade" = "X-80 (S-90DM/QT-CR)"
				"GHI5"."Burst Rating" = "6364.00000000"
				"GHI5"."Collapse Rating" = "3873.00000000"
				"GHI5"."Measured Depth" = "7600.00000000"
				"GHI5"."Total Vertical Depth" = "YZ119"
				"GHI5"."Interval" = "DEF1"
				"GHI6"."Section No." = "6.00000000"
				"GHI6"."Casing Size" = "22\\""@String
				"GHI6"."Casing Grade" = "4275psi Burst Disk"
				"GHI6"."Burst Rating" = "4275.00000000"
				"GHI6"."Collapse Rating" = "3873.00000000"
				"GHI6"."Measured Depth" = "7670.00000000"
				"GHI6"."Total Vertical Depth" = "YZ113"
				"GHI6"."Interval" = "DEF1"
				"GHI7"."Section No." = "7.00000000"
				"GHI7"."Casing Size" = "22\\""@String
				"GHI7"."Casing Weight" = "224.21000000"
				"GHI7"."Casing Grade" = "X-80 (S-90DM/QT-CR)"
				"GHI7"."Burst Rating" = "6364.00000000"
				"GHI7"."Collapse Rating" = "3873.00000000"
				"GHI7"."Measured Depth" = "10500.00000000"
				"GHI7"."Total Vertical Depth" = "YZ699"
				"GHI7"."Interval" = "DEF1"
				"GHI8"."Section No." = "1.00000000"
				"GHI8"."Casing Size" = "16\\""@String
				"GHI8"."Casing Grade" = "Dril-Quip CsgHgr 16.25\\" WLSF"@String
				"GHI8"."Burst Rating" = "12350.00000000"
				"GHI8"."Collapse Rating" = "5000.00000000"
				"GHI8"."Measured Depth" = "7293.00000000"
				"GHI8"."Total Vertical Depth" = "YZ57"
				"GHI8"."Interval" = "DEF2"
				"GHI9"."Section No." = "2.00000000"
				"GHI9"."Casing Size" = "16.25\\""@String
				"GHI9"."Casing Weight" = "136.04000000"
				"GHI9"."Casing Grade" = "Q-125 HP"
				"GHI9"."Burst Rating" = "12350.00000000"
				"GHI9"."Collapse Rating" = "7680.00000000"
				"GHI9"."Measured Depth" = "7593.00000000"
				"GHI9"."Total Vertical Depth" = "YZ117"
				"GHI9"."Interval" = "DEF2"
				"GHI10"."Section No." = "3.00000000"
				"GHI10"."Casing Size" = "16.04\\""@String
				"GHI10"."Casing Weight" = "109.61000000"
				"GHI10"."Casing Grade" = "Q-125 ICY"
				"GHI10"."Burst Rating" = "10100.00000000"
				"GHI10"."Collapse Rating" = "4800.00000000"
				"GHI10"."Measured Depth" = "14700.00000000"
				"GHI10"."Total Vertical Depth" = "YZ1539"
				"GHI10"."Interval" = "DEF2"
				"GHI11"."Section No." = "4.00000000"
				"GHI11"."Casing Size" = "16.15\\""@String
				"GHI11"."Casing Weight" = "119.23000000"
				"GHI11"."Casing Grade" = "Q-125 XHP"
				"GHI11"."Burst Rating" = "10880.00000000"
				"GHI11"."Collapse Rating" = "6120.00000000"
				"GHI11"."Measured Depth" = "23842.00000000"
				"GHI11"."Total Vertical Depth" = "YZ3352"
				"GHI11"."Interval" = "DEF2"
				"GHI12"."Section No." = "5.00000000"
				"GHI12"."Casing Size" = "16.04\\""@String
				"GHI12"."Casing Weight" = "109.61000000"
				"GHI12"."Casing Grade" = "Innovex Centramax Sub"
				"GHI12"."Burst Rating" = "10100.00000000"
				"GHI12"."Collapse Rating" = "4670.00000000"
				"GHI12"."Measured Depth" = "24298.00000000"
				"GHI12"."Total Vertical Depth" = "YZ3441"
				"GHI12"."Interval" = "DEF2"
				"GHI13"."Section No." = "6.00000000"
				"GHI13"."Casing Size" = "16.04\\""@String
				"GHI13"."Casing Weight" = "109.61000000"
				"GHI13"."Casing Grade" = "Q-125 ICY"
				"GHI13"."Burst Rating" = "10100.00000000"
				"GHI13"."Collapse Rating" = "4800.00000000"
				"GHI13"."Measured Depth" = "24342.00000000"
				"GHI13"."Total Vertical Depth" = "YZ3450"
				"GHI13"."Interval" = "DEF2"
				"GHI14"."Section No." = "1.00000000"
				"GHI14"."Casing Size" = "14\\""@String
				"GHI14"."Casing Weight" = "125.58000000"
				"GHI14"."Casing Grade" = "14x16 VF LH,125.58# SLSF Box (5-element set in 16.15\\" 109#)"@String
				"GHI14"."Burst Rating" = "10328.00000000"
				"GHI14"."Collapse Rating" = "9928.00000000"
				"GHI14"."Measured Depth" = "23856.00000000"
				"GHI14"."Total Vertical Depth" = "YZ3355"
				"GHI14"."Interval" = "DEF3"
				"GHI15"."Section No." = "2.00000000"
				"GHI15"."Casing Size" = "14\\""@String
				"GHI15"."Casing Weight" = "115.53000000"
				"GHI15"."Casing Grade" = "VM125 HYHC WLSF"
				"GHI15"."Burst Rating" = "14760.00000000"
				"GHI15"."Collapse Rating" = "12500.00000000"
				"GHI15"."Measured Depth" = "26847.00000000"
				"GHI15"."Total Vertical Depth" = "YZ3940"
				"GHI15"."Interval" = "DEF3"
				"GHI16"."Section No." = "1.00000000"
				"GHI16"."Casing Size" = "10-1/8\\""@String
				"GHI16"."Casing Grade" = "11-7/8x13-3/8 VF XG Liner Hanger"
				"GHI16"."Burst Rating" = "9070.00000000"
				"GHI16"."Collapse Rating" = "8730.00000000"
				"GHI16"."Measured Depth" = "25407.00000000"
				"GHI16"."Total Vertical Depth" = "YZ3658"
				"GHI16"."Interval" = "DEF4"
				"GHI17"."Section No." = "2.00000000"
				"GHI17"."Casing Size" = "10-1/8\\""@String
				"GHI17"."Casing Weight" = "75.90000000"
				"GHI17"."Casing Grade" = "TN Q125-ICY WLSF"
				"GHI17"."Burst Rating" = "18190.00000000"
				"GHI17"."Collapse Rating" = "16370.00000000"
				"GHI17"."Measured Depth" = "26380.00000000"
				"GHI17"."Total Vertical Depth" = "YZ3848"
				"GHI17"."Interval" = "DEF4"
				"GHI18"."Section No." = "3.00000000"
				"GHI18"."Casing Size" = "10-1/8\\""@String
				"GHI18"."Casing Grade" = "145K Cflex"
				"GHI18"."Burst Rating" = "14370.00000000"
				"GHI18"."Collapse Rating" = "13741.00000000"
				"GHI18"."Measured Depth" = "26430.00000000"
				"GHI18"."Total Vertical Depth" = "YZ3858"
				"GHI18"."Interval" = "DEF4"
				"GHI19"."Section No." = "4.00000000"
				"GHI19"."Casing Size" = "10-1/8\\""@String
				"GHI19"."Casing Weight" = "75.90000000"
				"GHI19"."Casing Grade" = "TN Q125-ICY WLSF"
				"GHI19"."Burst Rating" = "18190.00000000"
				"GHI19"."Collapse Rating" = "16370.00000000"
				"GHI19"."Measured Depth" = "27093.00000000"
				"GHI19"."Total Vertical Depth" = "YZ3988"
				"GHI19"."Interval" = "DEF4"
				"GHI20"."Section No." = "5.00000000"
				"GHI20"."Casing Size" = "10-1/8\\""@String
				"GHI20"."Casing Grade" = "145K Cflex"
				"GHI20"."Burst Rating" = "14370.00000000"
				"GHI20"."Collapse Rating" = "13741.00000000"
				"GHI20"."Measured Depth" = "27143.00000000"
				"GHI20"."Total Vertical Depth" = "YZ3998"
				"GHI20"."Interval" = "DEF4"
				"GHI21"."Section No." = "6.00000000"
				"GHI21"."Casing Size" = "10-1/8\\""@String
				"GHI21"."Casing Weight" = "75.90000000"
				"GHI21"."Casing Grade" = "TN Q125-ICY WLSF"
				"GHI21"."Burst Rating" = "18190.00000000"
				"GHI21"."Collapse Rating" = "16370.00000000"
				"GHI21"."Measured Depth" = "28502.00000000"
				"GHI21"."Total Vertical Depth" = "YZ4263"
				"GHI21"."Interval" = "DEF4"
				"GHI22"."Section No." = "1.00000000"
				"GHI22"."Casing Size" = "10-3/4\\""@String
				"GHI22"."Casing Weight" = "85.30000000"
				"GHI22"."Casing Grade" = "VM125SS VAM CSG HGR"
				"GHI22"."Burst Rating" = "15000.00000000"
				"GHI22"."Collapse Rating" = "10000.00000000"
				"GHI22"."Measured Depth" = "7008.00000000"
				"GHI22"."Total Vertical Depth" = "YZ1"
				"GHI22"."Interval" = "DEF5"
				"GHI23"."Section No." = "2.00000000"
				"GHI23"."Casing Size" = "10-3/4\\""@String
				"GHI23"."Casing Weight" = "85.30000000"
				"GHI23"."Casing Grade" = "VM125SS SLIJ-II-KT"
				"GHI23"."Burst Rating" = "16680.00000000"
				"GHI23"."Collapse Rating" = "15530.00000000"
				"GHI23"."Measured Depth" = "7028.00000000"
				"GHI23"."Total Vertical Depth" = "YZ4263"
				"GHI23"."Interval" = "DEF5"
				"GHI24"."Section No." = "3.00000000"
				"GHI24"."Casing Size" = "10-3/4\\""@String
				"GHI24"."Casing Weight" = "85.30000000"
				"GHI24"."Casing Grade" = "VM125SS Wedge 624"
				"GHI24"."Burst Rating" = "16680.00000000"
				"GHI24"."Collapse Rating" = "15530.00000000"
				"GHI24"."Measured Depth" = "8048.00000000"
				"GHI24"."Total Vertical Depth" = "YZ208"
				"GHI24"."Interval" = "DEF5"
				"GHI25"."Section No." = "4.00000000"
				"GHI25"."Casing Size" = "10-3/4\\""@String
				"GHI25"."Casing Weight" = "85.30000000"
				"GHI25"."Casing Grade" = "Q125 ICY Wedge 623RW"
				"GHI25"."Burst Rating" = "18020.00000000"
				"GHI25"."Collapse Rating" = "17250.00000000"
				"GHI25"."Measured Depth" = "13500.00000000"
				"GHI25"."Total Vertical Depth" = "YZ1299"
				"GHI25"."Interval" = "DEF5"
				"GHI26"."Section No." = "5.00000000"
				"GHI26"."Casing Size" = "9-7/8\\""@String
				"GHI26"."Casing Weight" = "65.10000000"
				"GHI26"."Casing Grade" = "Q125 ICY Wedge 623RW"
				"GHI26"."Burst Rating" = "15550.00000000"
				"GHI26"."Collapse Rating" = "13900.00000000"
				"GHI26"."Measured Depth" = "22842.00000000"
				"GHI26"."Total Vertical Depth" = "YZ3157"
				"GHI26"."Interval" = "DEF5"
				"GHI27"."Section No." = "6.00000000"
				"GHI27"."Casing Size" = "10.175\\""@String
				"GHI27"."Casing Weight" = "81.00000000"
				"GHI27"."Casing Grade" = "Q125X HP SLIJ-II"
				"GHI27"."Burst Rating" = "18870.00000000"
				"GHI27"."Collapse Rating" = "18870.00000000"
				"GHI27"."Measured Depth" = "24857.00000000"
				"GHI27"."Total Vertical Depth" = "YZ3551"
				"GHI27"."Interval" = "DEF5"
				"GHI28"."Section No." = "7.00000000"
				"GHI28"."Casing Size" = "10-1/8\\""@String
				"GHI28"."Casing Weight" = "75.90000000"
				"GHI28"."Casing Grade" = "Q125 ICY WLSF"
				"GHI28"."Burst Rating" = "18190.00000000"
				"GHI28"."Collapse Rating" = "16370.00000000"
				"GHI28"."Measured Depth" = "25357.00000000"
				"GHI28"."Total Vertical Depth" = "YZ3648"
				"GHI28"."Interval" = "DEF5"
				"GHI29"."Section No." = "8.00000000"
				"GHI29"."Casing Size" = "10-1/8\\""@String
				"GHI29"."Casing Weight" = "75.90000000"
				"GHI29"."Casing Grade" = "10.125 x 8.520 ID C140 Prod TB Seal Assy 102855894"
				"GHI29"."Burst Rating" = "15580.00000000"
				"GHI29"."Collapse Rating" = "14820.00000000"
				"GHI29"."Measured Depth" = "25400.00000000"
				"GHI29"."Total Vertical Depth" = "YZ3657"
				"GHI29"."Interval" = "DEF5"

				"JKL1"."Zone Name" = "Top Section Backup Pore Pressure (ppg)"
				"JKL1"."Backup Pore Pressure" = "8.60000000"
				"JKL1"."RKB TVD" = "YZ1"
				"JKL1"."Casing Section" = "GHI1"
				"JKL2"."Zone Name" = "Zone of Interest 1 Backup Pore Pressure (ppg)"
				"JKL2"."Backup Pore Pressure" = "8.60000000"
				"JKL2"."RKB TVD" = "YZ56"
				"JKL2"."Casing Section" = "GHI2"
				"JKL3"."Zone Name" = "Zone of Interest 2 Backup Pore Pressure (ppg)"
				"JKL3"."Backup Pore Pressure" = "8.60000000"
				"JKL3"."RKB TVD" = "YZ133"
				"JKL3"."Casing Section" = "GHI3"
				"JKL4"."Zone Name" = "Zone of Interest 3 Backup Pore Pressure (ppg)"
				"JKL4"."Backup Pore Pressure" = "8.66600000"
				"JKL4"."RKB TVD" = "YZ133"
				"JKL4"."Casing Section" = "GHI6"
				"JKL5"."Zone Name" = "Zone of Interest 4 Backup Pore Pressure (ppg)"
				"JKL5"."Backup Pore Pressure" = "8.66600000"
				"JKL5"."RKB TVD" = "YZ133"
				"JKL5"."Casing Section" = "GHI5"
				"JKL6"."Zone Name" = "Bottom Section Backup Pore Pressure (ppg)"
				"JKL6"."Backup Pore Pressure" = "10.23800000"
				"JKL6"."RKB TVD" = "YZ699"
				"JKL6"."Casing Section" = "GHI5"
				"JKL7"."Zone Name" = "Top Section Backup Pore Pressure (ppg)"
				"JKL7"."Backup Pore Pressure" = "8.60000000"
				"JKL7"."RKB TVD" = "YZ56"
				"JKL7"."Casing Section" = "GHI8"
				"JKL8"."Zone Name" = "Zone of Interest 1 Backup Pore Pressure (ppg)"
				"JKL8"."Backup Pore Pressure" = "8.60000000"
				"JKL8"."RKB TVD" = "YZ57"
				"JKL8"."Casing Section" = "GHI9"
				"JKL9"."Zone Name" = "Zone of Interest 2 Backup Pore Pressure (ppg)"
				"JKL9"."Backup Pore Pressure" = "11.99200000"
				"JKL9"."RKB TVD" = "YZ1539"
				"JKL9"."Casing Section" = "GHI10"
				"JKL10"."Zone Name" = "Zone of Interest 3 Backup Pore Pressure (ppg)"
				"JKL10"."Backup Pore Pressure" = "9.38800000"
				"JKL10"."RKB TVD" = "YZ2265"
				"JKL10"."Casing Section" = "GHI11"
				"JKL11"."Zone Name" = "Zone of Interest 4 Backup Pore Pressure (ppg)"
				"JKL11"."Backup Pore Pressure" = "11.46200000"
				"JKL11"."RKB TVD" = "YZ3354"
				"JKL11"."Casing Section" = "GHI9"
				"JKL12"."Zone Name" = "Zone of Interest 5 Backup Pore Pressure (ppg)"
				"JKL12"."Backup Pore Pressure" = "11.98200000"
				"JKL12"."RKB TVD" = "YZ3454"
				"JKL12"."Casing Section" = "GHI12"
				"JKL13"."Zone Name" = "Bottom Section Backup Pore Pressure (ppg)"
				"JKL13"."Backup Pore Pressure" = "11.96100000"
				"JKL13"."RKB TVD" = "YZ3450"
				"JKL13"."Casing Section" = "GHI13"
				"JKL14"."Zone Name" = "Top Section Backup Pore Pressure (ppg)"
				"JKL14"."Backup Pore Pressure" = "11.46200000"
				"JKL14"."RKB TVD" = "YZ3352"
				"JKL14"."Casing Section" = "GHI14"
				"JKL15"."Zone Name" = "Zone of Interest 1 Backup Pore Pressure (ppg)"
				"JKL15"."Backup Pore Pressure" = "11.47600000"
				"JKL15"."RKB TVD" = "YZ3355"
				"JKL15"."Casing Section" = "GHI15"
				"JKL16"."Zone Name" = "Bottom Section Backup Pore Pressure (ppg)"
				"JKL16"."Backup Pore Pressure" = "13.88000000"
				"JKL16"."RKB TVD" = "YZ3356"
				"JKL16"."Casing Section" = "GHI15"
				"JKL17"."Zone Name" = "Top Section Backup Pore Pressure (ppg)"
				"JKL17"."Backup Pore Pressure" = "13.41000000"
				"JKL17"."RKB TVD" = "YZ3357"
				"JKL17"."Casing Section" = "GHI16"
				"JKL18"."Zone Name" = "Zone of Interest 1 Backup Pore Pressure (ppg)"
				"JKL18"."Backup Pore Pressure" = "13.40800000"
				"JKL18"."RKB TVD" = "YZ3358"
				"JKL18"."Casing Section" = "GHI17"
				"JKL19"."Zone Name" = "Zone of Interest 2 Backup Pore Pressure (ppg)"
				"JKL19"."Backup Pore Pressure" = "13.93400000"
				"JKL19"."RKB TVD" = "YZ3359"
				"JKL19"."Casing Section" = "GHI18"
				"JKL20"."Zone Name" = "Zone of Interest 3 Backup Pore Pressure (ppg)"
				"JKL20"."Backup Pore Pressure" = "13.94700000"
				"JKL20"."RKB TVD" = "YZ3360"
				"JKL20"."Casing Section" = "GHI19"
				"JKL21"."Zone Name" = "Zone of Interest 4 Backup Pore Pressure (ppg)"
				"JKL21"."Backup Pore Pressure" = "12.93700000"
				"JKL21"."RKB TVD" = "YZ3361"
				"JKL21"."Casing Section" = "GHI20"
				"JKL22"."Zone Name" = "Zone of Interest 5 Backup Pore Pressure (ppg)"
				"JKL22"."Backup Pore Pressure" = "7.85000000"
				"JKL22"."RKB TVD" = "YZ4102"
				"JKL22"."Casing Section" = "GHI21"
				"JKL23"."Zone Name" = "Bottom Section Backup Pore Pressure (ppg)"
				"JKL23"."Backup Pore Pressure" = "13.45900000"
				"JKL23"."RKB TVD" = "YZ4263"
				"JKL23"."Casing Section" = "GHI21"
				"JKL24"."Zone Name" = "Top Section Backup Pore Pressure (ppg)"
				"JKL24"."Backup Pore Pressure" = "8.60000000"
				"JKL24"."RKB TVD" = "YZ1"
				"JKL24"."Casing Section" = "GHI22"
				"JKL25"."Zone Name" = "Zone of Interest 1 Backup Pore Pressure (ppg)"
				"JKL25"."Backup Pore Pressure" = "8.60000000"
				"JKL25"."RKB TVD" = "YZ2"
				"JKL25"."Casing Section" = "GHI24"
				"JKL26"."Zone Name" = "Zone of Interest 2 Backup Pore Pressure (ppg)"
				"JKL26"."Backup Pore Pressure" = "8.79400000"
				"JKL26"."RKB TVD" = "YZ210"
				"JKL26"."Casing Section" = "GHI24"
				"JKL27"."Zone Name" = "Zone of Interest 3 Backup Pore Pressure (ppg)"
				"JKL27"."Backup Pore Pressure" = "11.60200000"
				"JKL27"."RKB TVD" = "YZ1299"
				"JKL27"."Casing Section" = "GHI25"
				"JKL28"."Zone Name" = "Zone of Interest 4 Backup Pore Pressure (ppg)"
				"JKL28"."Backup Pore Pressure" = "10.71700000"
				"JKL28"."RKB TVD" = "YZ3146"
				"JKL28"."Casing Section" = "GHI26"
				"JKL29"."Zone Name" = "Zone of Interest 5 Backup Pore Pressure (ppg)"
				"JKL29"."Backup Pore Pressure" = "12.87900000"
				"JKL29"."RKB TVD" = "YZ3549"
				"JKL29"."Casing Section" = "GHI27"
				"JKL30"."Zone Name" = "Bottom Section Backup Pore Pressure (ppg)"
				"JKL30"."Backup Pore Pressure" = "13.40200000"
				"JKL30"."RKB TVD" = "YZ3655"
				"JKL30"."Casing Section" = "GHI29"



				"MNO1"."De-Rated Percent" = "0.70000000"
				"MNO1"."70% Burst (not corrected)" = "9605.40000000"
				"MNO1"."70% Burst (corrected)" = "9605.40000000"
				"MNO1"."Casing Section" = "GHI1"
				"MNO1"."Zone Name" = "JKL1"
				"MNO1"."Interval" = "DEF1"
				"MNO1"."RKB TVD" = "YZ1"
				"MNO1"."EMW - Backup" = "0.00000000"
				"MNO1"."Corrected Hydrostatic" = "0.00000000"
				"MNO2"."De-Rated Percent" = "0.70000000"
				"MNO2"."70% Burst (not corrected)" = "9723.00000000"
				"MNO2"."70% Burst (corrected)" = "9723.00000000"
				"MNO2"."Casing Section" = "GHI2"
				"MNO2"."Zone Name" = "JKL2"
				"MNO2"."Interval" = "DEF1"
				"MNO2"."RKB TVD" = "YZ56"
				"MNO2"."EMW - Backup" = "0.00000000"
				"MNO2"."Corrected Hydrostatic" = "0.00000000"
				"MNO3"."De-Rated Percent" = "0.70000000"
				"MNO3"."70% Burst (not corrected)" = "10920.00000000"
				"MNO3"."70% Burst (corrected)" = "10920.00000000"
				"MNO3"."Casing Section" = "GHI3"
				"MNO3"."Zone Name" = "JKL3"
				"MNO3"."Interval" = "DEF1"
				"MNO3"."RKB TVD" = "YZ56"
				"MNO3"."EMW - Backup" = "0.00000000"
				"MNO3"."Corrected Hydrostatic" = "0.00000000"
				"MNO4"."De-Rated Percent" = "0.70000000"
				"MNO4"."70% Burst (not corrected)" = "2992.50000000"
				"MNO4"."70% Burst (corrected)" = "3018.82344"
				"MNO4"."Casing Section" = "GHI6"
				"MNO4"."Zone Name" = "JKL4"
				"MNO4"."Interval" = "DEF1"
				"MNO4"."RKB TVD" = "YZ4264"
				"MNO4"."EMW - Backup" = "-0.06600000"
				"MNO4"."Corrected Hydrostatic" = "-26.32344000"
				"MNO5"."De-Rated Percent" = "0.70000000"
				"MNO5"."70% Burst (not corrected)" = "4454.80000000"
				"MNO5"."70% Burst (corrected)" = "4481.12344" #"4480.73136000"
				"MNO5"."Casing Section" = "GHI5"
				"MNO5"."Zone Name" = "JKL5"
				"MNO5"."Interval" = "DEF1"
				"MNO5"."RKB TVD" = "YZ4264"
				"MNO5"."EMW - Backup" = "-0.06600000"
				"MNO5"."Corrected Hydrostatic" = "-26.32344" #  -25.93136000"
				"MNO6"."De-Rated Percent" = "0.70000000"
				"MNO6"."70% Burst (not corrected)" = "4454.80000000"
				"MNO6"."70% Burst (corrected)" = "5349.31835200"
				"MNO6"."Casing Section" = "GHI5"
				"MNO6"."Zone Name" = "JKL6"
				"MNO6"."Interval" = "DEF1"
				"MNO6"."RKB TVD" = "YZ699"
				"MNO6"."EMW - Backup" = "-1.63800000"
				"MNO6"."Corrected Hydrostatic" = "-894.51835200"
				"MNO7"."De-Rated Percent" = "0.70000000"
				"MNO7"."70% Burst (not corrected)" = "8645.00000000"
				"MNO7"."70% Burst (corrected)" = "7318.76600000"
				"MNO7"."Casing Section" = "GHI8"
				"MNO7"."Zone Name" = "JKL7"
				"MNO7"."Interval" = "DEF2"
				"MNO7"."RKB TVD" = "YZ56"
				"MNO7"."EMW - Backup" = "3.50000000"
				"MNO7"."Corrected Hydrostatic" = "1326.23400000"
				"MNO8"."De-Rated Percent" = "0.70000000"
				"MNO8"."70% Burst (not corrected)" = "8645.00000000"
				"MNO8"."70% Burst (corrected)" = "7317.85600000"
				"MNO8"."Casing Section" = "GHI9"
				"MNO8"."Zone Name" = "JKL8"
				"MNO8"."Interval" = "DEF2"
				"MNO8"."RKB TVD" = "YZ57"
				"MNO8"."EMW - Backup" = "3.50000000"
				"MNO8"."Corrected Hydrostatic" = "1327.14400000"
				"MNO9"."De-Rated Percent" = "0.70000000"
				"MNO9"."70% Burst (not corrected)" = "7070.00000000"
				"MNO9"."70% Burst (corrected)" = "6987.43356800"
				"MNO9"."Casing Section" = "GHI10"
				"MNO9"."Zone Name" = "JKL9"
				"MNO9"."Interval" = "DEF2"
				"MNO9"."RKB TVD" = "YZ1539"
				"MNO9"."EMW - Backup" = "0.10800000"
				"MNO9"."Corrected Hydrostatic" = "82.56643200"
				"MNO10"."De-Rated Percent" = "0.70000000"
				"MNO10"."70% Burst (not corrected)" = "7616.00000000"
				"MNO10"."70% Burst (corrected)" = "5030.74803200"
				"MNO10"."Casing Section" = "GHI11"
				"MNO10"."Zone Name" = "JKL10"
				"MNO10"."Interval" = "DEF2"
				"MNO10"."RKB TVD" = "YZ2265"
				"MNO10"."EMW - Backup" = "2.71200000"
				"MNO10"."Corrected Hydrostatic" = "2585.25196800"
				"MNO11"."De-Rated Percent" = "0.70000000"
				"MNO11"."70% Burst (not corrected)" = "8645.00000000"
				"MNO11"."70% Burst (corrected)" = "7856.17424800"
				"MNO11"."Casing Section" = "GHI9"
				"MNO11"."Zone Name" = "JKL11"
				"MNO11"."Interval" = "DEF2"
				"MNO11"."RKB TVD" = "YZ3354"
				"MNO11"."EMW - Backup" = "0.63800000"
				"MNO11"."Corrected Hydrostatic" = "788.82575200"
				"MNO12"."De-Rated Percent" = "0.70000000"
				"MNO12"."70% Burst (not corrected)" = "7070.00000000"
				"MNO12"."70% Burst (corrected)" = "6921.03632800"
				"MNO12"."Casing Section" = "GHI12"
				"MNO12"."Zone Name" = "JKL12"
				"MNO12"."Interval" = "DEF2"
				"MNO12"."RKB TVD" = "YZ3454"
				"MNO12"."EMW - Backup" = "0.11800000"
				"MNO12"."Corrected Hydrostatic" = "148.96367200"
				"MNO13"."De-Rated Percent" = "0.70000000"
				"MNO13"."70% Burst (not corrected)" = "7070.00000000"
				"MNO13"."70% Burst (corrected)" = "6894.67040400"
				"MNO13"."Casing Section" = "GHI13"
				"MNO13"."Zone Name" = "JKL13"
				"MNO13"."Interval" = "DEF2"
				"MNO13"."RKB TVD" = "YZ3450"
				"MNO13"."EMW - Backup" = "0.13900000"
				"MNO13"."Corrected Hydrostatic" = "175.32959600"
				"MNO14"."De-Rated Percent" = "0.70000000"
				"MNO14"."70% Burst (not corrected)" = "7229.60000000"
				"MNO14"."70% Burst (corrected)" = "3969.33800800"
				"MNO14"."Casing Section" = "GHI14"
				"MNO14"."Zone Name" = "JKL14"
				"MNO14"."Interval" = "DEF3"
				"MNO14"."RKB TVD" = "YZ3352"
				"MNO14"."EMW - Backup" = "2.63800000"
				"MNO14"."Corrected Hydrostatic" = "3260.26199200"
				"MNO15"."De-Rated Percent" = "0.70000000"
				"MNO15"."70% Burst (not corrected)" = "10332.00000000"
				"MNO15"."70% Burst (corrected)" = "7086.99366400"
				"MNO15"."Casing Section" = "GHI15"
				"MNO15"."Zone Name" = "JKL15"
				"MNO15"."Interval" = "DEF3"
				"MNO15"."RKB TVD" = "YZ3355"
				"MNO15"."EMW - Backup" = "2.62400000"
				"MNO15"."Corrected Hydrostatic" = "3245.00633600"
				"MNO16"."De-Rated Percent" = "0.70000000"
				"MNO16"."70% Burst (not corrected)" = "10332.00000000"
				"MNO16"."70% Burst (corrected)" = "10059.87672000"
				"MNO16"."Casing Section" = "GHI15"
				"MNO16"."Zone Name" = "JKL16"
				"MNO16"."Interval" = "DEF3"
				"MNO16"."RKB TVD" = "YZ3356"
				"MNO16"."EMW - Backup" = "0.22000000"
				"MNO16"."Corrected Hydrostatic" = "272.12328000"
				"MNO17"."De-Rated Percent" = "0.70000000"
				"MNO17"."70% Burst (not corrected)" = "6349.00000000"
				"MNO17"."70% Burst (corrected)" = "6361.37184000"
				"MNO17"."Casing Section" = "GHI16"
				"MNO17"."Zone Name" = "JKL17"
				"MNO17"."Interval" = "DEF4"
				"MNO17"."RKB TVD" = "YZ3357"
				"MNO17"."EMW - Backup" = "-0.01000000"
				"MNO17"."Corrected Hydrostatic" = "-12.37184000"
				"MNO18"."De-Rated Percent" = "0.70000000"
				"MNO18"."70% Burst (not corrected)" = "12733.00000000"
				"MNO18"."70% Burst (corrected)" = "12742.89955200"
				"MNO18"."Casing Section" = "GHI17"
				"MNO18"."Zone Name" = "JKL18"
				"MNO18"."Interval" = "DEF4"
				"MNO18"."RKB TVD" = "YZ3358"
				"MNO18"."EMW - Backup" = "-0.00800000"
				"MNO18"."Corrected Hydrostatic" = "-9.89955200"
				"MNO19"."De-Rated Percent" = "0.70000000"
				"MNO19"."70% Burst (not corrected)" = "10059.00000000"
				"MNO19"."70% Burst (corrected)" = "10719.93393600"
				"MNO19"."Casing Section" = "GHI18"
				"MNO19"."Zone Name" = "JKL19"
				"MNO19"."Interval" = "DEF4"
				"MNO19"."RKB TVD" = "YZ3359"
				"MNO19"."EMW - Backup" = "-0.53400000"
				"MNO19"."Corrected Hydrostatic" = "-660.93393600"
				"MNO20"."De-Rated Percent" = "0.70000000"
				"MNO20"."70% Burst (not corrected)" = "12733.00000000"
				"MNO20"."70% Burst (corrected)" = "13410.16630800"
				"MNO20"."Casing Section" = "GHI19"
				"MNO20"."Zone Name" = "JKL20"
				"MNO20"."Interval" = "DEF4"
				"MNO20"."RKB TVD" = "YZ3360"
				"MNO20"."EMW - Backup" = "-0.54700000"
				"MNO20"."Corrected Hydrostatic" = "-677.16630800"
				"MNO21"."De-Rated Percent" = "0.70000000"
				"MNO21"."70% Burst (not corrected)" = "10059.00000000"
				"MNO21"."70% Burst (corrected)" = "9485.70228800"
				"MNO21"."Casing Section" = "GHI20"
				"MNO21"."Zone Name" = "JKL21"
				"MNO21"."Interval" = "DEF4"
				"MNO21"."RKB TVD" = "YZ3361"
				"MNO21"."EMW - Backup" = "0.46300000"
				"MNO21"."Corrected Hydrostatic" = "573.29771200"
				"MNO22"."De-Rated Percent" = "0.70000000"
				"MNO22"."70% Burst (not corrected)" = "12733.00000000"
				"MNO22"."70% Burst (corrected)" = "4791.59380000"
				"MNO22"."Casing Section" = "GHI21"
				"MNO22"."Zone Name" = "JKL22"
				"MNO22"."Interval" = "DEF4"
				"MNO22"."RKB TVD" = "YZ4102"
				"MNO22"."EMW - Backup" = "5.55000000"
				"MNO22"."Corrected Hydrostatic" = "7941.40620000"
				"MNO23"."De-Rated Percent" = "0.70000000"
				"MNO23"."70% Burst (not corrected)" = "12733.00000000"
				"MNO23"."70% Burst (corrected)" = "12819.89189600"
				"MNO23"."Casing Section" = "GHI21"
				"MNO23"."Zone Name" = "JKL23"
				"MNO23"."Interval" = "DEF4"
				"MNO23"."RKB TVD" = "YZ4263"
				"MNO23"."EMW - Backup" = "-0.05900000"
				"MNO23"."Corrected Hydrostatic" = "-86.89189600"
				"MNO24"."De-Rated Percent" = "0.70000000"
				"MNO24"."70% Burst (not corrected)" = "10500.00000000"
				"MNO24"."70% Burst (corrected)" = "8569.69760000"
				"MNO24"."Casing Section" = "GHI22"
				"MNO24"."Zone Name" = "JKL24"
				"MNO24"."Interval" = "DEF5"
				"MNO24"."RKB TVD" = "YZ1"
				"MNO24"."EMW - Backup" = "5.30000000"
				"MNO24"."Corrected Hydrostatic" = "1930.30240000"
				"MNO25"."De-Rated Percent" = "0.70000000"
				"MNO25"."70% Burst (not corrected)" = "11676.00000000"
				"MNO25"."70% Burst (corrected)" = "9742.11480000"
				"MNO25"."Casing Section" = "GHI24"
				"MNO25"."Zone Name" = "JKL25"
				"MNO25"."Interval" = "DEF5"
				"MNO25"."RKB TVD" = "YZ2"
				"MNO25"."EMW - Backup" = "5.30000000"
				"MNO25"."Corrected Hydrostatic" = "1933.88520000"
				"MNO26"."De-Rated Percent" = "0.70000000"
				"MNO26"."70% Burst (not corrected)" = "11676.00000000"
				"MNO26"."70% Burst (corrected)" = "9536.76981600"
				"MNO26"."Casing Section" = "GHI24"
				"MNO26"."Zone Name" = "JKL26"
				"MNO26"."Interval" = "DEF5"
				"MNO26"."RKB TVD" = "YZ210"
				"MNO26"."EMW - Backup" = "5.10600000"
				"MNO26"."Corrected Hydrostatic" = "2139.23018400"
				"MNO27"."De-Rated Percent" = "0.70000000"
				"MNO27"."70% Burst (not corrected)" = "12614.00000000"
				"MNO27"."70% Burst (corrected)" = "11000.56500800"
				"MNO27"."Casing Section" = "GHI25"
				"MNO27"."Zone Name" = "JKL27"
				"MNO27"."Interval" = "DEF5"
				"MNO27"."RKB TVD" = "YZ1299"
				"MNO27"."EMW - Backup" = "2.29800000"
				"MNO27"."Corrected Hydrostatic" = "1613.43499200"
				"MNO28"."De-Rated Percent" = "0.70000000"
				"MNO28"."70% Burst (not corrected)" = "10885.00000000"
				"MNO28"."70% Burst (corrected)" = "7121.66270800"
				"MNO28"."Casing Section" = "GHI26"
				"MNO28"."Zone Name" = "JKL28"
				"MNO28"."Interval" = "DEF5"
				"MNO28"."RKB TVD" = "YZ3146"
				"MNO28"."EMW - Backup" = "3.18300000"
				"MNO28"."Corrected Hydrostatic" = "3763.33729200"
				"MNO29"."De-Rated Percent" = "0.70000000"
				"MNO29"."70% Burst (not corrected)" = "13209.00000000"
				"MNO29"."70% Burst (corrected)" = "11894.86681600"
				"MNO29"."Casing Section" = "GHI27"
				"MNO29"."Zone Name" = "JKL29"
				"MNO29"."Interval" = "DEF5"
				"MNO29"."RKB TVD" = "YZ3549"
				"MNO29"."EMW - Backup" = "1.02100000"
				"MNO29"."Corrected Hydrostatic" = "1314.13318400"
				"MNO30"."De-Rated Percent" = "0.70000000"
				"MNO30"."70% Burst (not corrected)" = "10906.00000000"
				"MNO30"."70% Burst (corrected)" = "10251.29732800"
				"MNO30"."Casing Section" = "GHI29"
				"MNO30"."Zone Name" = "JKL30"
				"MNO30"."Interval" = "DEF5"
				"MNO30"."RKB TVD" = "YZ3655"
				"MNO30"."EMW - Backup" = "0.49800000"
				"MNO30"."Corrected Hydrostatic" = "654.70267200"


				"PQR1"."MASP BHP" = "5339.71651200"
				"PQR1"."MASP Shoe" = "3588.958536" #changed
				"PQR1"."Mud Ratio" = "0.50000000"
				"PQR1"."Gas Ratio" = "0.50000000"
				"PQR1"."Gas Gradient" = "0.15000000"
				"PQR1"."TVD Deepest OH" = "YZ3460"
				"PQR1"."TVD Shoe" = "YZ699"
				"PQR1"."Interval" = "DEF1"
				"PQR1"."Mud Hydrostatic (BHP)" = "5439.43400000"
				"PQR1"."Gas Hydrostatic (BHP)" = "1296.75000000"
				"PQR1"."SW Hydrostatic" = "3101.77920000"
				"PQR1"."Pressure at Deepest OH Depth" = "15177.67971200"
				"PQR1"."Well" = "ABC1"
				"PQR1"."Interface TVD (BHP)" = "15662.00000000"
				"PQR1"."Mud TVD (BHP)" = "8645.00000000"
				"PQR1"."Gas TVD (BHP)" = "8645.00000000"
				"PQR1"."Frac Pressure at Deepest Shoe" = "7213.487736" #7213.487736
				"PQR1"."Mud TVD (Shoe)" = "0.00000000"
				"PQR1"."Gas TVD (Shoe)" = "3485.00000000"
				"PQR1"."Mud Hydrostatic (Shoe)" = "0.00000000"
				"PQR1"."Gas Hydrostatic (Shoe)" = "522.75000000"
				"PQR1"."Minimum MASP" = "3588.958536" #changed
				"PQR2"."MASP BHP" = "7783.82725200"
				"PQR2"."MASP Shoe" = "10434.50931200"
				"PQR2"."Mud Ratio" = "0.50000000"
				"PQR2"."Gas Ratio" = "0.50000000"
				"PQR2"."Gas Gradient" = "0.15000000"
				"PQR2"."TVD Deepest OH" = "YZ3886"
				"PQR2"."TVD Shoe" = "YZ4263"
				"PQR2"."Interval" = "DEF2"
				"PQR2"."Mud Hydrostatic (BHP)" = "6866.91200000"
				"PQR2"."Gas Hydrostatic (BHP)" = "1456.50000000"
				"PQR2"."SW Hydrostatic" = "3101.77920000"
				"PQR2"."Pressure at Deepest OH Depth" = "19209.01845200"
				"PQR2"."Well" = "ABC1"
				"PQR2"."Interface TVD (BHP)" = "16727.00000000"
				"PQR2"."Mud TVD (BHP)" = "9710.00000000"
				"PQR2"."Gas TVD (BHP)" = "9710.00000000"
				"PQR2"."Frac Pressure at Deepest Shoe" = "23192.77251200"
				"PQR2"."Mud TVD (Shoe)" = "11595.00000000"
				"PQR2"."Gas TVD (Shoe)" = "9710.00000000"
				"PQR2"."Mud Hydrostatic (Shoe)" = "8199.98400000"
				"PQR2"."Gas Hydrostatic (Shoe)" = "1456.50000000"
				"PQR2"."Minimum MASP" = "7783.82725200"
				"PQR3"."MASP BHP" = "7733.74235600"
				"PQR3"."MASP Shoe" = "10509.73131200"
				"PQR3"."Mud Ratio" = "0.50000000"
				"PQR3"."Gas Ratio" = "0.50000000"
				"PQR3"."Gas Gradient" = "0.15000000"
				"PQR3"."TVD Deepest OH" = "YZ3940"
				"PQR3"."TVD Shoe" = "YZ4263"
				"PQR3"."Interval" = "DEF3"
				"PQR3"."Mud Hydrostatic (BHP)" = "6962.38400000"
				"PQR3"."Gas Hydrostatic (BHP)" = "1476.75000000"
				"PQR3"."SW Hydrostatic" = "3101.77920000"
				"PQR3"."Pressure at Deepest OH Depth" = "19274.65555600"
				"PQR3"."Well" = "ABC1"
				"PQR3"."Interface TVD (BHP)" = "16862.00000000"
				"PQR3"."Mud TVD (BHP)" = "9845.00000000"
				"PQR3"."Gas TVD (BHP)" = "9845.00000000"
				"PQR3"."Frac Pressure at Deepest Shoe" = "23192.77251200"
				"PQR3"."Mud TVD (Shoe)" = "11460.00000000"
				"PQR3"."Gas TVD (Shoe)" = "9845.00000000"
				"PQR3"."Mud Hydrostatic (Shoe)" = "8104.51200000"
				"PQR3"."Gas Hydrostatic (Shoe)" = "1476.75000000"
				"PQR3"."Minimum MASP" = "7733.74235600"
				"PQR4"."MASP BHP" = "7588.55929600"
				"PQR4"."MASP Shoe" = "10959.67031200"
				"PQR4"."Mud Ratio" = "0.50000000"
				"PQR4"."Gas Ratio" = "0.50000000"
				"PQR4"."Gas Gradient" = "0.15000000"
				"PQR4"."TVD Deepest OH" = "YZ4263"
				"PQR4"."TVD Shoe" = "YZ4263"
				"PQR4"."Interval" = "DEF4"
				"PQR4"."Mud Hydrostatic (BHP)" = "7533.44800000"
				"PQR4"."Gas Hydrostatic (BHP)" = "1597.87500000"
				"PQR4"."SW Hydrostatic" = "3101.77920000"
				"PQR4"."Pressure at Deepest OH Depth" = "19821.66149600"
				"PQR4"."Well" = "ABC1"
				"PQR4"."Interface TVD (BHP)" = "17669.50000000"
				"PQR4"."Mud TVD (BHP)" = "10652.50000000"
				"PQR4"."Gas TVD (BHP)" = "10652.50000000"
				"PQR4"."Frac Pressure at Deepest Shoe" = "23192.77251200"
				"PQR4"."Mud TVD (Shoe)" = "10652.50000000"
				"PQR4"."Gas TVD (Shoe)" = "10652.50000000"
				"PQR4"."Mud Hydrostatic (Shoe)" = "7533.44800000"
				"PQR4"."Gas Hydrostatic (Shoe)" = "1597.87500000"
				"PQR4"."Minimum MASP" = "7588.55929600"
				"PQR5"."MASP BHP" = "9028.77729600"
				"PQR5"."MASP Shoe" = "12399.88831200"
				"PQR5"."Mud Ratio" = "0.00000000"
				"PQR5"."Gas Ratio" = "1.00000000"
				"PQR5"."Gas Gradient" = "0.36100000"
				"PQR5"."TVD Deepest OH" = "YZ4263"
				"PQR5"."TVD Shoe" = "YZ4263"
				"PQR5"."Interval" = "DEF5"
				"PQR5"."Mud Hydrostatic (BHP)" = "0.00000000"
				"PQR5"."Gas Hydrostatic (BHP)" = "7691.10500000"
				"PQR5"."SW Hydrostatic" = "3101.77920000"
				"PQR5"."Pressure at Deepest OH Depth" = "19821.66149600"
				"PQR5"."Well" = "ABC1"
				"PQR5"."Interface TVD (BHP)" = "28322.00000000"
				"PQR5"."Mud TVD (BHP)" = "0.00000000"
				"PQR5"."Gas TVD (BHP)" = "21305.00000000"
				"PQR5"."Frac Pressure at Deepest Shoe" = "23192.77251200"
				"PQR5"."Mud TVD (Shoe)" = "0.00000000"
				"PQR5"."Gas TVD (Shoe)" = "21305.00000000"
				"PQR5"."Mud Hydrostatic (Shoe)" = "0.00000000"
				"PQR5"."Gas Hydrostatic (Shoe)" = "7691.10500000"
				"PQR5"."Minimum MASP" = "9028.77729600"





				"STU1"."MAWP" = "6690.737736"
				"STU1"."MAWP Surface" = "3552.735336"
				"STU1"."Reference MASP" = "PQR1"
				"STU1"."Well" = "ABC1"
				"STU1"."Interval" = "DEF1"
				"STU1"."MAWP Surf + 500" = "4052.735336"
				"STU2"."MAWP" = "10885.60645200"
				"STU2"."MAWP Surface" = "6470.51005200"
				"STU2"."Reference MASP" = "PQR2"
				"STU2"."Well" = "ABC1"
				"STU2"."Interval" = "DEF2"
				"STU2"."MAWP Surf + 500" = "6970.51005200"
				"STU3"."MAWP" = "10835.52155600"
				"STU3"."MAWP Surface" = "5690.65715600"
				"STU3"."Reference MASP" = "PQR3"
				"STU3"."Well" = "ABC1"
				"STU3"."Interval" = "DEF3"
				"STU3"."MAWP Surf + 500" = "6190.65715600"
				"STU4"."MAWP" = "10690.33849600"
				"STU4"."MAWP Surface" = "5800.89289600"
				"STU4"."Reference MASP" = "PQR4"
				"STU4"."Well" = "ABC1"
				"STU4"."Interval" = "DEF4"
				"STU4"."MAWP Surf + 500" = "6300.89289600"
				"STU5"."MAWP" = "12130.55649600"
				"STU5"."MAWP Surface" = "7058.66889600"
				"STU5"."Reference MASP" = "PQR5"
				"STU5"."Well" = "ABC1"
				"STU5"."Interval" = "DEF5"
				"STU5"."MAWP Surf + 500" = "7558.66889600"





						"YZ1"."RKB TVD" = "7004"
						"YZ1"."Mid Pore Pressure" = "8.6"
						"YZ1"."Fracture Gradient" = "8.5"
						"YZ1"."Well" = "ABC1"
						"YZ2"."RKB TVD" = "7017"
						"YZ2"."Mid Pore Pressure" = "8.6"
						"YZ2"."Fracture Gradient" = "8.5"
						"YZ2"."Well" = "ABC1"
						"YZ54"."RKB TVD" = "7277"
						"YZ54"."Mid Pore Pressure" = "8.6"
						"YZ54"."Fracture Gradient" = "8.707"
						"YZ54"."Well" = "ABC1"
						"YZ55"."RKB TVD" = "7282"
						"YZ55"."Mid Pore Pressure" = "8.6"
						"YZ55"."Fracture Gradient" = "8.711"
						"YZ55"."Well" = "ABC1"
						"YZ56"."RKB TVD" = "7287"
						"YZ56"."Mid Pore Pressure" = "8.6"
						"YZ56"."Fracture Gradient" = "8.715"
						"YZ56"."Well" = "ABC1"
						"YZ57"."RKB TVD" = "7292"
						"YZ57"."Mid Pore Pressure" = "8.6"
						"YZ57"."Fracture Gradient" = "8.719"
						"YZ57"."Well" = "ABC1"
						"YZ113"."RKB TVD" = "7572"
						"YZ113"."Mid Pore Pressure" = "8.633"
						"YZ113"."Fracture Gradient" = "8.944"
						"YZ113"."Well" = "ABC1"
						"YZ117"."RKB TVD" = "7592"
						"YZ117"."Mid Pore Pressure" = "8.64"
						"YZ117"."Fracture Gradient" = "8.961"
						"YZ117"."Well" = "ABC1"
						"YZ208"."RKB TVD" = "8047"
						"YZ208"."Mid Pore Pressure" = "8.791"
						"YZ208"."Fracture Gradient" = "9.333"
						"YZ208"."Well" = "ABC1"
						"YZ210"."RKB TVD" = "8057"
						"YZ210"."Mid Pore Pressure" = "8.794"
						"YZ210"."Fracture Gradient" = "9.341"
						"YZ210"."Well" = "ABC1"
						"YZ699"."RKB TVD" = "10502"
						"YZ699"."Mid Pore Pressure" = "10.238" #10.238
						"YZ699"."Fracture Gradient" = "13.209"
						"YZ699"."Well" = "ABC1"
						"YZ1299"."RKB TVD" = "13502"
						"YZ1299"."Mid Pore Pressure" = "11.604"
						"YZ1299"."Fracture Gradient" = "14.317"
						"YZ1299"."Well" = "ABC1"
						"YZ1539"."RKB TVD" = "14702"
						"YZ1539"."Mid Pore Pressure" = "11.993"
						"YZ1539"."Fracture Gradient" = "14.634"
						"YZ1539"."Well" = "ABC1"
						"YZ2265"."RKB TVD" = "18332"
						"YZ2265"."Mid Pore Pressure" = "9.388"
						"YZ2265"."Fracture Gradient" = "12.756"
						"YZ2265"."Well" = "ABC1"
						"YZ3146"."RKB TVD" = "22737"
						"YZ3146"."Mid Pore Pressure" = "10.693"
						"YZ3146"."Fracture Gradient" = "13.875"
						"YZ3146"."Well" = "ABC1"
						"YZ3352"."RKB TVD" = "23767"
						"YZ3352"."Mid Pore Pressure" = "11.467"
						"YZ3352"."Fracture Gradient" = "14.291"
						"YZ3352"."Well" = "ABC1"
						"YZ3354"."RKB TVD" = "23777"
						"YZ3354"."Mid Pore Pressure" = "11.476"
						"YZ3354"."Fracture Gradient" = "14.292"
						"YZ3354"."Well" = "ABC1"
						"YZ3355"."RKB TVD" = "23782"
						"YZ3355"."Mid Pore Pressure" = "11.48"
						"YZ3355"."Fracture Gradient" = "14.293"
						"YZ3355"."Well" = "ABC1"
						"YZ3356"."RKB TVD" = "23787"
						"YZ3356"."Mid Pore Pressure" = "11.485"
						"YZ3356"."Fracture Gradient" = "14.294"
						"YZ3356"."Well" = "ABC1"
						"YZ3357"."RKB TVD" = "23792"
						"YZ3357"."Mid Pore Pressure" = "11.489"
						"YZ3357"."Fracture Gradient" = "14.295"
						"YZ3357"."Well" = "ABC1"
						"YZ3358"."RKB TVD" = "23797"
						"YZ3358"."Mid Pore Pressure" = "11.494"
						"YZ3358"."Fracture Gradient" = "14.296"
						"YZ3358"."Well" = "ABC1"
						"YZ3359"."RKB TVD" = "23802"
						"YZ3359"."Mid Pore Pressure" = "11.498"
						"YZ3359"."Fracture Gradient" = "14.297"
						"YZ3359"."Well" = "ABC1"
						"YZ3360"."RKB TVD" = "23807"
						"YZ3360"."Mid Pore Pressure" = "11.503"
						"YZ3360"."Fracture Gradient" = "14.298"
						"YZ3360"."Well" = "ABC1"
						"YZ3361"."RKB TVD" = "23812"
						"YZ3361"."Mid Pore Pressure" = "11.507"
						"YZ3361"."Fracture Gradient" = "14.299"
						"YZ3361"."Well" = "ABC1"
						"YZ3441"."RKB TVD" = "24212"
						"YZ3441"."Mid Pore Pressure" = "11.913"
						"YZ3441"."Fracture Gradient" = "14.389"
						"YZ3441"."Well" = "ABC1"
						"YZ3450"."RKB TVD" = "24257"
						"YZ3450"."Mid Pore Pressure" = "11.961"
						"YZ3450"."Fracture Gradient" = "14.403"
						"YZ3450"."Well" = "ABC1"
						"YZ3454"."RKB TVD" = "24277"
						"YZ3454"."Mid Pore Pressure" = "11.982"
						"YZ3454"."Fracture Gradient" = "14.436"
						"YZ3454"."Well" = "ABC1"
						"YZ3460"."RKB TVD" = "24307"
						"YZ3460"."Mid Pore Pressure" = "12.014"
						"YZ3460"."Fracture Gradient" = "14.486"
						"YZ3460"."Well" = "ABC1"
						"YZ3549"."RKB TVD" = "24752"
						"YZ3549"."Mid Pore Pressure" = "12.89"
						"YZ3549"."Fracture Gradient" = "15.079"
						"YZ3549"."Well" = "ABC1"
						"YZ3551"."RKB TVD" = "24762"
						"YZ3551"."Mid Pore Pressure" = "12.913"
						"YZ3551"."Fracture Gradient" = "15.085"
						"YZ3551"."Well" = "ABC1"
						"YZ3648"."RKB TVD" = "25247"
						"YZ3648"."Mid Pore Pressure" = "13.41"
						"YZ3648"."Fracture Gradient" = "15.273"
						"YZ3648"."Well" = "ABC1"
						"YZ3650"."RKB TVD" = "25257"
						"YZ3650"."Mid Pore Pressure" = "13.408"
						"YZ3650"."Fracture Gradient" = "15.274"
						"YZ3650"."Well" = "ABC1"
						"YZ3655"."RKB TVD" = "25282"
						"YZ3655"."Mid Pore Pressure" = "13.402"
						"YZ3655"."Fracture Gradient" = "15.277"
						"YZ3655"."Well" = "ABC1"
						"YZ3657"."RKB TVD" = "25292"
						"YZ3657"."Mid Pore Pressure" = "13.4"
						"YZ3657"."Fracture Gradient" = "15.279"
						"YZ3657"."Well" = "ABC1"
						"YZ3658"."RKB TVD" = "25297"
						"YZ3658"."Mid Pore Pressure" = "13.399"
						"YZ3658"."Fracture Gradient" = "15.279"
						"YZ3658"."Well" = "ABC1"
						"YZ3848"."RKB TVD" = "26247"
						"YZ3848"."Mid Pore Pressure" = "13.934"
						"YZ3848"."Fracture Gradient" = "15.443"
						"YZ3848"."Well" = "ABC1"
						"YZ3858"."RKB TVD" = "26297"
						"YZ3858"."Mid Pore Pressure" = "13.948"
						"YZ3858"."Fracture Gradient" = "15.449"
						"YZ3858"."Well" = "ABC1"
						"YZ3886"."RKB TVD" = "26437"
						"YZ3886"."Mid Pore Pressure" = "13.973"
						"YZ3886"."Fracture Gradient" = "15.465"
						"YZ3886"."Well" = "ABC1"
						"YZ3940"."RKB TVD" = "26707"
						"YZ3940"."Mid Pore Pressure" = "13.879"
						"YZ3940"."Fracture Gradient" = "15.592"
						"YZ3940"."Well" = "ABC1"
						"YZ3988"."RKB TVD" = "26947"
						"YZ3988"."Mid Pore Pressure" = "8.764"
						"YZ3988"."Fracture Gradient" = "15.62"
						"YZ3988"."Well" = "ABC1"
						"YZ3998"."RKB TVD" = "26997"
						"YZ3998"."Mid Pore Pressure" = "10.962"
						"YZ3998"."Fracture Gradient" = "15.067"
						"YZ3998"."Well" = "ABC1"

						"YZ119"."RKB TVD"	= 7602
						"YZ119"."Mid Pore Pressure" = "8.643"
						"YZ119"."Fracture Gradient" = "8.969"
						"YZ119"."Well" = "ABC1"

						"YZ4263"."RKB TVD" = "28322"
						"YZ4263"."Mid Pore Pressure" = "13.459"
						"YZ4263"."Fracture Gradient" = "15.748"
						"YZ4263"."Well" = "ABC1"

						"YZ3157"."RKB TVD" = "22792"
						"YZ3157"."Mid Pore Pressure" = "10.723"
						"YZ3157"."Fracture Gradient" = "13.885"
						"YZ3157"."Well" = "ABC1"

						#change?
						"YZ133"."RKB TVD" = "7672"
						"YZ133"."Mid Pore Pressure" = "8.666"
						"YZ133"."Fracture Gradient" = "9.026"
						"YZ133"."Well" = "ABC1"

						"YZ4102"."RKB TVD" = "27517"
						"YZ4102"."Mid Pore Pressure" = "7.85"
						"YZ4102"."Fracture Gradient" = "14.71"
						"YZ4102"."Well" = "ABC1"

						"YZ719"."RKB TVD" = "10602"
						"YZ719"."Mid Pore Pressure" = "10.298"
						"YZ719"."Fracture Gradient" = "13.256"
						"YZ719"."Well" = "ABC1"

						"YZ3946"."RKB TVD" = "26737"
						"YZ3946"."Mid Pore Pressure" = "13.875"
						"YZ3946"."Fracture Gradient" = "15.608"
						"YZ3946"."Well" = "ABC1"


						"YZ4245"."RKB TVD" = "28232"
						"YZ4245"."Mid Pore Pressure" = "13.48"
						"YZ4245"."Fracture Gradient" = "15.739"
						"YZ4245"."Well" = "ABC1"

						"YZ3459"."RKB TVD" = "24302"
						"YZ3459"."Mid Pore Pressure" = "12.08"
						"YZ3459"."Fracture Gradient" = "14.478"
						"YZ3459"."Well" = "ABC1"

						YZ4264."RKB TVD" = "7670"
				 		"YZ4264"."Mid Pore Pressure" = "8.666"
						#"YZ4393"."Fracture Gradient" = "14.478"
						"YZ4264"."Well" = "ABC1"

						#YZ4265."RKB TVD" = "10502"
						#YZ4265."Mid Pore Pressure" = "10.238" #10.24
				 		#YZ4265."Well" = "ABC1"

						DEF1."Liner Top Depth".Well = ABC1 #added
						DEF5."Liner Top Depth".Well = ABC1 #added
				}

								constraints JamesConstraints = literal : James {
								forall x:"Burst Calculation Key" -> where x."Burst Rating Corrected for MW & Backside" = "-"("*"(x."Material Utilization Factor", x."Item of Interest"."Item of Interest - Burst Rating (psi)"), "*"(x.TVD, "*"(".052"@Float, "-"(x.DHEMW,x."Backside EMW"))))

forall x:"MASP Key"  -> where x."MASP Open Hole Key"."MASP Open Hole" = ("*"(x."MASP Open Hole Key"."Pore Pressure at OH Depth (ppg)",
 		"*"(x."MASP Open Hole Key"."Constant", x."MASP Open Hole Key"."Open Hole Depth yielding highest MASP- RKB TVD (Ft)"))
		 "-" ("*"("*"(x."MASP Open Hole Key"."MW",  x."MASP Open Hole Key"."TVDmud"), x."MASP Open Hole Key"."Constant") "+" ("*"(x."MASP Open Hole Key"."HC Grad.", x."MASP Open Hole Key"."TVDHC") "+" x."MASP Open Hole Key"."Sw Hydrostatic")))

	
 
	forall x:"MASP Key"  -> where x."MASP Shoe Key"."MASP Shoe" = ("*"("*"(x."MASP Shoe Key"."Deepest Exposed Shoe Below This Shoe - RKB TVD (Ft)", x."MASP Shoe Key"."Frac Gradient at Deepest Exposed Shoe (ppg)"), x."MASP Shoe Key"."Constant") "-" ("*"("*"(x."MASP Shoe Key"."TVDmud", x."MASP Shoe Key"."MW"), x."MASP Shoe Key"."Constant") "+" ("*"(x."MASP Shoe Key"."HC Grad.", x."MASP Shoe Key"."TVDHC") "+" x."MASP Shoe Key"."Sw Hydrostatic")))
	
								}

instance JamesInstance = literal : James {
	generators
	
"WD1" : "Well Data Key"
"WD2" : "Well Data Key"
S1 : "Casing Section Key"
S2 : "Casing Section Key"
"CD1" : "Casing Key"
"IoI1" : "Item of Inteerest Key"
"IoI2" : "Item of Inteerest Key"
"IoI3" : "Item of Inteerest Key"
"IoI4" : "Item of Inteerest Key"
"IoI5" : "Item of Inteerest Key"
"IoI6" : "Item of Inteerest Key"
"ST1" : "Shoe Track Key"
"LHoff1" : "Liner Hangoff Key"
"ES1" : "Exposed Shoe Key"
"OH1" : "OH Key"
"BC1" : "Burst Calculation Key"
"BC2" : "Burst Calculation Key"
"BC3" : "Burst Calculation Key"
"BC4" : "Burst Calculation Key"
"BC5" : "Burst Calculation Key"
"BC6" : "Burst Calculation Key"
"MG1" : "Mud Gradient Key"
"MASPOH1" : "MASP Open Hole Key"
"MASPSH1" : "MASP Shoe Key"
"MASP1" : "MASP Key"

CBT5 CBT9 CBT72 CBT73 CBT77 : "Casing Burst Key"
PPFP1 PPFP133 PPFP699 PPFP56 PPFP3459 PPFP3460 PPFP713 : "Pore Pressure Frac Pressure Key"

	equations

	PPFP1."RKB TVD" = 7004
	PPFP1."Pore Pressure_mid (ppg)" = "8.6"
	PPFP1."Fracture Gradient (ppg)" = "8.5"
	PPFP1."Salt FG (Using OBG + 1000 psi (ppg)" = "8.5"

	PPFP56."RKB TVD" = 7287
	PPFP56."Pore Pressure_mid (ppg)" = "8.6"
	PPFP56."Fracture Gradient (ppg)" = "8.715"
	PPFP56."Salt FG (Using OBG + 1000 psi (ppg)" = "8.715"
	
	PPFP133."RKB TVD" = 7672
	PPFP133."Pore Pressure_mid (ppg)" = "8.666"
	PPFP133."Fracture Gradient (ppg)" = "9.026"
	PPFP133."Salt FG (Using OBG + 1000 psi (ppg)" = "9.026"

	PPFP699."RKB TVD" = 10502
	PPFP699."Pore Pressure_mid (ppg)" = "10.238" #todo
	PPFP699."Fracture Gradient (ppg)" = "-999.25"
	PPFP699."Salt FG (Using OBG + 1000 psi (ppg)" = "13.209"

	PPFP713."RKB TVD" = 10572
	PPFP713."Pore Pressure_mid (ppg)" = "10.281"
	PPFP713."Fracture Gradient (ppg)" = "-999.25"
	PPFP713."Salt FG (Using OBG + 1000 psi (ppg)" = "13.24"

	# change 6/30
	PPFP3459."RKB TVD" = 24302
	PPFP3459."Pore Pressure_mid (ppg)" = "12.008"
	PPFP3459."Fracture Gradient (ppg)" = "14.478"
	PPFP3459."Salt FG (Using OBG + 1000 psi (ppg)" = "14.478"

	PPFP3460."RKB TVD" = 24307
	PPFP3460."Pore Pressure_mid (ppg)" = "12.014"
	PPFP3460."Fracture Gradient (ppg)" = "14.486"
	PPFP3460."Salt FG (Using OBG + 1000 psi (ppg)" = "14.486"


	CBT5.Size = "23"
	CBT5."Weight/Grade" = "453.04ppf X-80Q (H-100DM/QT)"
	CBT5."Lowest Value Burst" = "13890"
	CBT5."Lowest Value Collapse" = "4000"
	CBT5."Pipe Body (for 123S) Burst" = "13890"
	CBT5."Pipe Body (for 123S) Collapse" = "12690"
	CBT5."Comment" = "Formerly 448.41 ppf X-80"

	CBT9.Size = "22"
	CBT9."Weight/Grade" = "224.21ppf X-80 (S-90DM/QT-CR)"
	CBT9."Lowest Value Burst" = "6364"
	CBT9."Lowest Value Collapse" = "3873"
	CBT9."Pipe Body (for 123S) Burst" = "6364"
	CBT9."Pipe Body (for 123S) Collapse" = "3873"
	CBT9."Comment" = "Dual Metal-to-Metal, Quick Thread-Collapse Resistant"

	CBT9.Size = "22"
	CBT9."Weight/Grade" = "224.21ppf X-80 (S-90DM/QT-CR)"
	CBT9."Lowest Value Burst" = "6364"
	CBT9."Lowest Value Collapse" = "3873"
	CBT9."Pipe Body (for 123S) Burst" = "6364"
	CBT9."Pipe Body (for 123S) Collapse" = "3873"
	CBT9."Comment" = "Dual Metal-to-Metal, Quick Thread-Collapse Resistant"

	CBT72.Size = "23"
	CBT72."Weight/Grade" = "23x2 HPWH Extension Jt"
	CBT72."Lowest Value Burst" = "13722"
	CBT72."Lowest Value Collapse" = "12545"
	CBT72."Pipe Body (for 123S) Burst" = "13722"
	CBT72."Pipe Body (for 123S) Collapse" = "12545"
	CBT72."Comment" = "Casing Hanger, Supplemental Adapter and Burst Disk Ratings"

	CBT73.Size = "23"
	CBT73."Weight/Grade" = "16\\" 12.5K SA Upper"
	CBT73."Lowest Value Burst" = "15600"
	CBT73."Lowest Value Collapse" = "14200"
	CBT73."Pipe Body (for 123S) Burst" = "15600"
	CBT73."Pipe Body (for 123S) Collapse" = "14200"
	CBT73."Comment" = "From TDS 2-413383-02 Rev B, Dated 12/02/15 (prior to hgr install)"

	CBT77.Size = "22"
	CBT77."Weight/Grade" = "4275psi Burst Disk"
	CBT77."Lowest Value Burst" = "4275"
	CBT77."Lowest Value Collapse" = "3873"
#	CBT77."Pipe Body (for 123S) Burst" = "15600"
#	CBT77."Pipe Body (for 123S) Collapse" = "14200"
	CBT77."Comment" = "Burst Disk Use ONLY - 4000psi - 5% = 3800psi min. burst value"


	S1."Well Data Key" = WD1
	S2."Well Data Key" = WD2
	S1."Casing Section / Nominal Casing - Size" = "Top 22 Surface Csg"
	S2."Casing Section / Nominal Casing - Size" = "16\\""

WD1.Block = "Walker Ridge 678 OCS-G-21245"
WD1.Well = "IS001"
WD1.WellBore	="ST00BP00"
WD1."Field/Prospect" = "St.Malo"
WD1.WD = "6936"	
WD1."RKB-ML" = "7017"


"CD1"."Top Casing Section / Nominal Casing - Size" = "S1"
"CD1"."Item of Interest #" = "IoI1"
"CD1"."PPFP Key" = "PPFP713"
"CD1"."Casing Type - Enter \\"Long String\\", \\"Liner\\" or \\"Tieback\\"" = "Long String"
"CD1"."Mud Weight Casing Run In (Downhole,)" = "12.50"
"CD1"."Mud Weight Casing Run In (Surface,)" = "12.50"
"CD1"."Hole Section TD - RKB TVD (Ft)" = "10600"
"CD1"."Max Pore Pressure at Hole Section TD (ppg)" = "10.281"

"IoI1"."Top Casing Section / Nominal Casing - Size" = "S1"
"IoI1"."Item of Interest #" = "Top 22 Surface Csg"
"IoI1"."PPFP Key" = "PPFP1"
"IoI1"."CBT Key" = "CBT72"
"IoI1"."Item of Interest Depth - RKB TVD (Ft)" = "7004"
"IoI1"."Item of Interest Casing Section - Size" = "23"
"IoI1"."Item of Interest Casing Section - Weight/Grade" = "23x2 HPWH Extension Jt"
"IoI1"."Item of Interest - Burst Rating (psi)" = "13722"
"IoI1"."Item of Interest Backup Pore Pressure (ppg)" = "8.60"
"IoI1"."Liner Hangoff Key" = "LHoff1"
"IoI2"."Top Casing Section / Nominal Casing - Size" = "S1"
"IoI2"."Item of Interest #" = "Zone of Interest 1"
"IoI2"."PPFP Key" = "PPFP56"
"IoI2"."CBT Key" = "CBT5"
"IoI2"."Item of Interest Depth - RKB TVD (Ft)" = "7287"
"IoI2"."Item of Interest Casing Section - Size" = "23"
"IoI2"."Item of Interest Casing Section - Weight/Grade" = "453.04ppf X-80Q (H-100DM/QT)"
"IoI2"."Item of Interest - Burst Rating (psi)" = "13890"
"IoI2"."Item of Interest Backup Pore Pressure (ppg)" = "8.60"
"IoI2"."Liner Hangoff Key" = "LHoff1"
"IoI3"."Top Casing Section / Nominal Casing - Size" = "S1"
"IoI3"."Item of Interest #" = "Zone of Interest 2"
"IoI3"."PPFP Key" = "PPFP56"
"IoI3"."CBT Key" = "CBT73"
"IoI3"."Item of Interest Depth - RKB TVD (Ft)" = "7287"
"IoI3"."Item of Interest Casing Section - Size" = "23"
"IoI3"."Item of Interest Casing Section - Weight/Grade" = "16\\" 12.5K SA Upper"
"IoI3"."Item of Interest - Burst Rating (psi)" = "15600"
"IoI3"."Item of Interest Backup Pore Pressure (ppg)" = "8.60"
"IoI3"."Liner Hangoff Key" = "LHoff1"
"IoI4"."Top Casing Section / Nominal Casing - Size" = "S1"
"IoI4"."Item of Interest #" = "Zone of Interest 3"
"IoI4"."PPFP Key" = "PPFP133"
"IoI4"."CBT Key" = "CBT77"
"IoI4"."Item of Interest Depth - RKB TVD (Ft)" = "7670"
"IoI4"."Item of Interest Casing Section - Size" = "22"
"IoI4"."Item of Interest Casing Section - Weight/Grade" = "4275psi Burst Disk"
"IoI4"."Item of Interest - Burst Rating (psi)" = "4275"
"IoI4"."Item of Interest Backup Pore Pressure (ppg)" = "8.666"
"IoI4"."Liner Hangoff Key" = "LHoff1"
"IoI5"."Top Casing Section / Nominal Casing - Size" = "S1"
"IoI5"."Item of Interest #" = "Zone of Interest 4"
"IoI5"."PPFP Key" = "PPFP133"
"IoI5"."CBT Key" = "CBT9"
"IoI5"."Item of Interest Depth - RKB TVD (Ft)" = "7670"
"IoI5"."Item of Interest Casing Section - Size" = "22"
"IoI5"."Item of Interest Casing Section - Weight/Grade" = "224.21ppf X-80 (S-90DM/QT-CR)"
"IoI5"."Item of Interest - Burst Rating (psi)" = "6364"
"IoI5"."Item of Interest Backup Pore Pressure (ppg)" = "8.666"
"IoI5"."Liner Hangoff Key" = "LHoff1"
"IoI6"."Top Casing Section / Nominal Casing - Size" = "S1"
"IoI6"."Item of Interest #" = "Btm of Casing/Shoe"
"IoI6"."PPFP Key" = "PPFP699"
"IoI6"."CBT Key" = "CBT9"
"IoI6"."Item of Interest Depth - RKB TVD (Ft)" = "10502" #data cleaning
"IoI6"."Item of Interest Casing Section - Size" = "22"
"IoI6"."Item of Interest Casing Section - Weight/Grade" = "224.21ppf X-80 (S-90DM/QT-CR)"
"IoI6"."Item of Interest - Burst Rating (psi)" = "6364"
"IoI6"."Item of Interest Backup Pore Pressure (ppg)" = "10.238" #10.238
"IoI6"."Liner Hangoff Key" = "LHoff1"

"ST1"."Top Casing Section / Nominal Casing - Size" = "S1"
"ST1"."Item of Interest #" = "IoI6"
"ST1"."Shoe Frac Gradient (ppg)" = "13.207"
"ST1"."Planned FIT (Downhole,ppg)" = "13.20"
"ST1"."Planned FIT (Surface,ppg)" = "13.00"

"LHoff1"."Top Casing Section / Nominal Casing - Size" = "S1"
"LHoff1"."Will a liner be hung off inside this string? (Yes/No)" = "Yes"
"LHoff1"."Mud Weight Used to Test 22 Casing RKB-ML (ppg)" = "8.60"
"LHoff1"."Mud Weight Used to Test Casing/Liner (Downhole,ppg)" = "8.60"
"LHoff1"."Mud Weight Used to Test Casing/Liner (Surface,ppg)" = "8.60"
"LHoff1"."Mud Weight Used to Perform FIT/LOT (Downhole,ppg)" = "11.30"
"LHoff1"."Mud Weight Used to Perform FIT/LOT (Surface,ppg)" = "11.10"
"LHoff1"."Safety Margin Applied to MAWPsurf for calculating Ptest1" = "500"

"ES1"."Top Casing Section / Nominal Casing - Size" = "S1"
"ES1"."PPFP Key" = "PPFP699"
"ES1"."Deepest Exposed Shoe Below This Shoe - RKB TVD (Ft)" = "10502"
"ES1"."Frac Gradient at Deepest Exposed Shoe (ppg)" = "13.209"

"OH1"."Top Casing Section / Nominal Casing - Size" = "S1"
"OH1"."PPFP Key" = "PPFP3459" #6/30 change
"OH1"."Open Hole Depth yielding highest MASP- RKB TVD (Ft)" = "24307" #changed 7/1
"OH1"."Pore Pressure at OH Depth (ppg)" = "12.008"
"OH1"."Max Mud Weight at OH Depth (Downhole,ppg)" = "12.1" #change
"OH1"."Max Mud Weight at OH Depth (Surface,ppg)" = "11.90"
"OH1"."Kick Fluid Gradient - Gas or Oil (psi/ft)" = "0.150"
"OH1"."Kick Fluid Gradient if Gas per BSEE (psi/ft)" = "0.150"

"BC1"."Item of Interest" = "IoI1"
"BC1"."Material Utilization Factor" = "0.7"
"BC1"."Burst Rating" = "13722"
"BC1"."DHEMW" = "8.6"
"BC1"."Backside EMW" = "8.6"
"BC1"."Constant" = "0.052"
"BC1"."TVD" = "7004"
"BC1"."Burst Rating Corrected for MW & Backside" = "9605.4"
"BC2"."Item of Interest" = "IoI2"
"BC2"."Material Utilization Factor" = "0.7"
"BC2"."Burst Rating" = "13890"
"BC2"."DHEMW" = "8.6"
"BC2"."Backside EMW" = "8.6"
"BC2"."Constant" = "0.052"
"BC2"."TVD" = "7287"
"BC2"."Burst Rating Corrected for MW & Backside" = "9723"
"BC3"."Item of Interest" = "IoI3"
"BC3"."Material Utilization Factor" = "0.7"
"BC3"."Burst Rating" = "15600"
"BC3"."DHEMW" = "8.6"
"BC3"."Backside EMW" = "8.6"
"BC3"."Constant" = "0.052"
"BC3"."TVD" = "7287"
"BC3"."Burst Rating Corrected for MW & Backside" = "10920"
"BC4"."Item of Interest" = "IoI4"
"BC4"."Material Utilization Factor" = "0.7"
"BC4"."Burst Rating" = "4275"
"BC4"."DHEMW" = "8.6"
"BC4"."Backside EMW" = "8.666"
"BC4"."Constant" = "0.052"
"BC4"."TVD" = "7670"
"BC4"."Burst Rating Corrected for MW & Backside" = "3018.82344"
"BC5"."Item of Interest" = "IoI5"
"BC5"."Material Utilization Factor" = "0.7"
"BC5"."Burst Rating" = "6364"
"BC5"."DHEMW" = "8.6"
"BC5"."Backside EMW" = "8.666"
"BC5"."Constant" = "0.052"
"BC5"."TVD" = "7670"
"BC5"."Burst Rating Corrected for MW & Backside" = "4481.12344"
"BC6"."Item of Interest" = "IoI6"
"BC6"."Material Utilization Factor" = "0.7"
"BC6"."Burst Rating" = "6364"
"BC6"."DHEMW" = "8.6"
"BC6"."Backside EMW" = "10.238"
"BC6"."Constant" = "0.052"
"BC6"."TVD" = "10502"
"BC6"."Burst Rating Corrected for MW & Backside" = "5349.318352"

"MG1"."Casing Section Key" = "S1"
"MG1"."Mud Fraction" = "0.5"
"MG1"."Gas Fraction" = "0.5"
"MG1"."TVD Interface" = "15661.5"

"MASPOH1"."Casing Section Key" = "S1"
"MASPOH1"."PPFP Key" = "PPFP3460" #no change 6/30
"MASPOH1"."Open Hole Depth yielding highest MASP- RKB TVD (Ft)" = "24307" #changed 
"MASPOH1"."Pore Pressure at OH Depth (ppg)" = "12.008"
"MASPOH1"."Constant" = "0.052"
"MASPOH1"."TVDmud" = "8645"
"MASPOH1"."MW" = "12.1"
"MASPOH1"."TVDHC" = "8645"
"MASPOH1"."HC Grad." = "0.15"
"MASPOH1"."Sw Hydrostatic" = "3101.7792"
"MASPOH1"."MASP Open Hole" = "5339.716512" #change 6/30
"MASPOH1"."OH Key" = OH1
"MASPOH1"."Mud Gradient Key" = MG1



"MASPSH1"."Casing Section Key" = "S1"
"MASPSH1"."PPFP Key" = "PPFP699"
"MASPSH1"."Deepest Exposed Shoe Below This Shoe - RKB TVD (Ft)" = "10502" #change
"MASPSH1"."Frac Gradient at Deepest Exposed Shoe (ppg)" = "13.209"
"MASPSH1"."Constant" = "0.052"
"MASPSH1"."TVDmud" = "00"
"MASPSH1"."MW" = "12.1"
"MASPSH1"."TVDHC" = "3485"
"MASPSH1"."HC Grad." = "0.15"
"MASPSH1"."Sw Hydrostatic" = "3101.7792"
"MASPSH1"."MASP Shoe" = "3588.958536"
"MASPSH1"."OH Key" = OH1
"MASPSH1"."Exposed Shoe Key" = ES1

"MASP1"."Casing Section Key" = "S1"
"MASP1"."MASP Openhole" = "5339.716512" #changed
"MASP1"."MASP Shoe" = "3588.958536"
"MASP1"."MASP" = "3588.958536"
"MASP1"."MASP Open Hole Key" = "MASPOH1"
"MASP1"."MASP Shoe Key" = "MASPSH1"

}


								""";
	}

	@Override
	public String getLinks() {
		return """
			entity_isomorphisms
		bck : James."Burst Calculation Key" -> Brandon."MASP Calc. Step 1" 
		mk  : James."MASP Key" -> Brandon."MASP Calc. Step 2a"
		 
	equations
 		eq1: forall x:James."Burst Calculation Key",   x.Constant=".052"@Float
	
		eq2: forall bc:Brandon."MASP Calc. Step 1",  bc."Casing Section" =  bc."Zone Name"."Casing Section"
		eq3: forall bc:Brandon."MASP Calc. Step 1",  bc."Casing Section".Interval = bc.Interval
		eq4: forall bc:Brandon."MASP Calc. Step 1",	 bc.Interval.Well = bc."RKB TVD".Well
		eq5: forall bc:Brandon."MASP Calc. Step 1",	 bc."Casing Section"."Total Vertical Depth".Well = bc."RKB TVD".Well
		eq6: forall bc:Brandon."MASP Calc. Step 1", 	bc.Interval."Planned Section Total Depth".Well = bc."Zone Name"."RKB TVD".Well

		eq7: forall x:Brandon."Interval Info",  x."Liner Top Depth".Well = x."OH Depth Yielding Highest MASP - RKB TVD".Well
		eq8: forall x:Brandon."Interval Info",  x."Liner Top Depth".Well = x."Planned Section Total Depth".Well
		eq9: forall x:Brandon."Interval Info",  x."Liner Top Depth".Well = x.Well


		eq10: forall x:Brandon."MASP Calc. Step 2a",	 x.Well = x."TVD Shoe".Well
		eq11: forall x:Brandon."MASP Calc. Step 2a",	 x.Well = x."TVD Deepest OH".Well
		eq12: forall x:Brandon."MASP Calc. Step 2a",	 x.Well = x."Interval"."Planned Section Total Depth".Well

		eq13: forall x:James."Item of Inteerest Key", x."Liner Hangoff Key"."Top Casing Section / Nominal Casing - Size" = x."Top Casing Section / Nominal Casing - Size"
 	
 		eq14: forall x:James."MASP Key", x."Casing Section Key" = x."MASP Open Hole Key"."Casing Section Key"
 		eq15: forall x:James."MASP Key", x."Casing Section Key" = x."MASP Open Hole Key"."Mud Gradient Key"."Casing Section Key"
 		eq16: forall x:James."MASP Key", x."Casing Section Key" = x."MASP Open Hole Key"."OH Key"."Top Casing Section / Nominal Casing - Size"
 		eq17: forall x:James."MASP Key", x."MASP Open Hole Key"."Mud Gradient Key"."Casing Section Key" = x."MASP Shoe Key"."Casing Section Key"
 		eq18: forall x:James."MASP Key",	x."MASP Shoe Key"."Exposed Shoe Key"."Top Casing Section / Nominal Casing - Size" = x."MASP Shoe Key"."Casing Section Key"
 		eq19: forall x:James."MASP Key",	x."MASP Open Hole Key"."OH Key"."Top Casing Section / Nominal Casing - Size"  = x."MASP Shoe Key"."Casing Section Key"
	
	constraints

     r1: forall x:"Brandon_MASP Calc. Step 1" -> where x."70% Burst (corrected)" = x.bck_inv."Burst Rating Corrected for MW & Backside" 
                                         x."Interval"."Downhole Mud Weight" = x.bck_inv."DHEMW" 
										 x."Casing Section"."Burst Rating" = x.bck_inv."Item of Interest"."Item of Interest - Burst Rating (psi)"
									     x."Zone Name"."Backup Pore Pressure" = x.bck_inv."Backside EMW" #ppore
   										 x."RKB TVD"."RKB TVD" = x.bck_inv.TVD #tvd
   									     x."De-Rated Percent"  = x.bck_inv."Material Utilization Factor" #seventy

 
	r2: forall x y: "Brandon_MASP Calc. Step 1" where x."70% Burst (corrected)" = y.bck_inv."Burst Rating Corrected for MW & Backside" -> where x = y
	 
	r3: forall x y: "Brandon_MASP Calc. Step 2a" where x."MASP BHP" = y.mk_inv."MASP Openhole" -> where x = y


	r4: forall x:"Brandon_MASP Calc. Step 2a" -> where x."Mud Hydrostatic (BHP)" = "*"("*"(x.mk_inv."MASP Open Hole Key"."MW",  x.mk_inv."MASP Open Hole Key"."TVDmud"), x.mk_inv."MASP Open Hole Key"."Constant")
											x."Gas Hydrostatic (BHP)" = "*"(x.mk_inv."MASP Open Hole Key"."HC Grad.", x.mk_inv."MASP Open Hole Key"."TVDHC")  
 											x."SW Hydrostatic" = x.mk_inv."MASP Open Hole Key"."Sw Hydrostatic"
											x."MASP BHP"  = x.mk_inv."MASP Open Hole Key"."MASP Open Hole"
 											x."Pressure at Deepest OH Depth" = "*"(x.mk_inv."MASP Open Hole Key"."Pore Pressure at OH Depth (ppg)",
 												"*"(x.mk_inv."MASP Open Hole Key"."Constant", x.mk_inv."MASP Open Hole Key"."Open Hole Depth yielding highest MASP- RKB TVD (Ft)")) 

	r5: forall x:"Brandon_MASP Calc. Step 2a" -> where x."Mud Hydrostatic (Shoe)" = "*"("*"(x.mk_inv."MASP Shoe Key"."TVDmud", x.mk_inv."MASP Shoe Key"."MW"), x.mk_inv."MASP Shoe Key"."Constant") 
											x."Gas Hydrostatic (Shoe)" = "*"(x.mk_inv."MASP Shoe Key"."HC Grad.", x.mk_inv."MASP Shoe Key"."TVDHC")
											x."SW Hydrostatic" =  x.mk_inv."MASP Shoe Key"."Sw Hydrostatic"
											x."MASP Shoe"  = x.mk_inv."MASP Shoe Key"."MASP Shoe"
											x."Frac Pressure at Deepest Shoe" = "*"("*"(x.mk_inv."MASP Shoe Key"."Deepest Exposed Shoe Below This Shoe - RKB TVD (Ft)", x.mk_inv."MASP Shoe Key"."Frac Gradient at Deepest Exposed Shoe (ppg)"), x.mk_inv."MASP Shoe Key"."Constant")

	
 

								""";
	}

	@Override
	public String getTargets() {
		// TODO Auto-generated method stub
		return "";
	}

}