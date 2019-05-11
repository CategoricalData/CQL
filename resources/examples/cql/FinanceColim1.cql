////////////////////////////////////////////////////////////////////////
//
// Example courtesy of Jee Chung 
//
////////////////////////////////////////////////////////////////////////
// Uses the quotient technique to automatically compute the schema and
// instance colimits
////////////////////////////////////////////////////////////////////////

typeside Ty = literal {
	java_types	
		Integer = "java.lang.Integer"
		String = "java.lang.String"
		Double = "java.lang.Double"
		Boolean = "java.lang.String" 
		Date = "java.lang.String"
//		Date = "java.util.Date" alternative type for dates
	java_constants
		Integer = "return java.lang.Integer.parseInt(input[0]);"
		String = "return input[0]"
		Double = "return java.lang.Double.parseDouble(input[0])"
		Boolean = "return input[0]" 
		Date = "return input[0]"
//		Date = "return (new java.text.SimpleDateFormat(\"m/d/yyyy\").parse(input[0]));"
	java_functions
		keygen : Integer,Integer,Integer -> Integer = "return (2^input[0] * 3^input[1] * 5^input[2])"
}

////////////////////////////////////////////////////////////////////////
// Input schemas and instances separately
////////////////////////////////////////////////////////////////////////

// Compile time input
schema Client = literal : Ty {
	entities
		client
	attributes
		client_id : client -> Integer
		client_name : client -> String
		client_description : client -> String
} 

// Runtime input
instance ClientInstance = literal : Client {
     generators 
     	cc1 cc2 cc3 : client   	
     multi_equations //alternative, less verbose syntax
     	client_id -> {cc1 "1", cc2 "2", cc3 "3"}
     	client_name -> {cc1 Tom, cc2 Dick, cc3 Harry}
     	client_description -> {cc1 "Tom Client", cc2 "Dick Client", cc3 "Harry Client"}
} 

////////////////////////////////////////////////////////////////////////

// Compile time input
schema Portfolio = literal : Ty {
	entities
		strategy 
		portfolio
	foreign_keys			
		portfolio_strategy : portfolio -> strategy
		portfolio_parent_portfolio : portfolio -> portfolio //added
	path_equations
		portfolio.portfolio_parent_portfolio.portfolio_parent_portfolio = portfolio.portfolio_parent_portfolio //added required
	attributes			
		strategy_id : strategy -> Integer
		strategy_name : strategy -> String
		strategy_description : strategy -> String
		portfolio_id : portfolio -> Integer
		portfolio_name : portfolio -> String
		portfolio_description : portfolio -> String
		portfolio_client_client_id : portfolio -> Integer
		portfolio_strategy_strategy_id : portfolio -> Integer
		portfolio_parent_portfolio_portfolio_id : portfolio -> Integer //added 
	observation_equations
		forall p. p.portfolio_strategy.strategy_id = p.portfolio_strategy_strategy_id
		forall p. p.portfolio_parent_portfolio.portfolio_id = p.portfolio_parent_portfolio_portfolio_id //added
} 

// Runtime input
instance PortfolioInstance = literal : Portfolio {
     generators 
     	ps1 ps2 ps3 : strategy //not having ps2 here is really weird because it means this instance has nulls in it 
     	pp1 pp2 pp3 : portfolio
     equations
		pp1.portfolio_parent_portfolio_portfolio_id = "1" pp1.portfolio_parent_portfolio = pp1 //pp1 is parent of itself
		pp2.portfolio_parent_portfolio_portfolio_id = "2" pp2.portfolio_parent_portfolio = pp2 //pp2 is parent of itself
		pp3.portfolio_parent_portfolio_portfolio_id = "2" pp3.portfolio_parent_portfolio = pp2 //pp3's parent is pp2
     
		ps1.strategy_id = "1" ps1.strategy_name = Strat1 ps1.strategy_description = Strategy1 
		ps2.strategy_id = "2" ps2.strategy_name = Strat2 ps2.strategy_description = Strategy2 
		ps3.strategy_id = "3" ps3.strategy_name = Strat3 ps3.strategy_description = Strategy3 
		pp1.portfolio_id = "1" pp1.portfolio_name = Port1 pp1.portfolio_description = Portfolio1 pp1.portfolio_client_client_id = "1" pp1.portfolio_strategy_strategy_id = "2" pp1.portfolio_strategy = ps2
		pp2.portfolio_id = "2" pp2.portfolio_name = Port2 pp2.portfolio_description = Portfolio2 pp2.portfolio_client_client_id = "2" pp2.portfolio_strategy_strategy_id = "2" pp2.portfolio_strategy = ps2
		pp3.portfolio_id = "3" pp3.portfolio_name = Port3 pp3.portfolio_description = Portfolio3 pp3.portfolio_client_client_id = "2" pp3.portfolio_strategy_strategy_id = "1" pp3.portfolio_strategy = ps1
} 

////////////////////////////////////////////////////////////////////////

// Compile time input		
schema Ref = literal : Ty {
	entities
		country 
		currency 
		asset 
		strategy
	foreign_keys
		currency_country : currency -> country
		asset_strategy : asset -> strategy
	attributes	
		country_id : country -> Integer
		country_code : country -> String
		country_name : country -> String
		asset_id : asset -> Integer
		asset_name : asset -> String
		asset_description : asset -> String
		asset_strategy_strategy_id : asset -> Integer
		strategy_id : strategy -> Integer
		strategy_name : strategy -> String
		strategy_description : strategy -> String
		currency_id : currency -> Integer
		currency_code : currency -> String
		currency_name : currency -> String
		currency_country_country_id : currency -> Integer
	observation_equations	
		forall c. c.currency_country.country_id = c.currency_country_country_id
		forall a. a.asset_strategy.strategy_id = a.asset_strategy_strategy_id
} 	

// Runtime input
instance RefInstance = literal : Ref {
     generators 
     	rcty1 rcty2 rcty3 rcty4 rcty5 rcty6 : country 
		rcur1 rcur2 rcur3 : currency 
		ra1 ra2 ra3 ra4 ra5 ra6 ra7 ra8 ra9 ra10 : asset 
		rs1 rs2 rs3 rs4 rs5 : strategy
     equations	
 		rcty1.country_id = "1" rcty1.country_code = AU rcty1.country_name = Australia 
 		rcty2.country_id = "2" rcty2.country_code = CH rcty2.country_name = Switzerland 
 		rcty3.country_id = "3" rcty3.country_code = CN rcty3.country_name = China 
 		rcty4.country_id = "4" rcty4.country_code = EU rcty4.country_name = "European Union" 
 		rcty5.country_id = "5" rcty5.country_code = JP rcty5.country_name = Japan 
 		rcty6.country_id = "6" rcty6.country_code = US rcty6.country_name = "United States of America" 
 		rcur1.currency_id = "1" rcur1.currency_code = USD rcur1.currency_name = "US Dollar" rcur1.currency_country_country_id  = "6" rcur1.currency_country = rcty6 
		rcur2.currency_id = "2" rcur2.currency_code = EUR rcur2.currency_name = "Euro" rcur2.currency_country_country_id  = "4" rcur2.currency_country = rcty4 
		rcur3.currency_id = "3" rcur3.currency_code = JPY rcur3.currency_name = "Japanese Yen" rcur3.currency_country_country_id  = "5" rcur3.currency_country = rcty5 	 		 		
		ra1.asset_id = "1" ra1.asset_name = A1 ra1.asset_description = Asset1 ra1.asset_strategy_strategy_id = "1" ra1.asset_strategy = rs1 
		ra2.asset_id = "2" ra2.asset_name = A2 ra2.asset_description = Asset2 ra2.asset_strategy_strategy_id = "3" ra2.asset_strategy = rs3
		ra3.asset_id = "3" ra3.asset_name = A3 ra3.asset_description = Asset3 ra3.asset_strategy_strategy_id = "2" ra3.asset_strategy = rs2		 
		ra4.asset_id = "4" ra4.asset_name = A4 ra4.asset_description = Asset4 ra4.asset_strategy_strategy_id = "4" ra4.asset_strategy = rs4
		ra5.asset_id = "5" ra5.asset_name = A5 ra5.asset_description = Asset5 ra5.asset_strategy_strategy_id = "5" ra5.asset_strategy = rs5
		ra6.asset_id = "6" ra6.asset_name = A6 ra6.asset_description = Asset6 ra6.asset_strategy_strategy_id = "4" ra6.asset_strategy = rs4
		ra7.asset_id = "7" ra7.asset_name = A7 ra7.asset_description = Asset7 ra7.asset_strategy_strategy_id = "4" ra7.asset_strategy = rs4
		ra8.asset_id = "8" ra8.asset_name = A8 ra8.asset_description = Asset8 ra8.asset_strategy_strategy_id = "5" ra8.asset_strategy = rs5
		ra9.asset_id = "9" ra9.asset_name = A9 ra9.asset_description = Asset9 ra9.asset_strategy_strategy_id = "3" ra9.asset_strategy = rs3
		ra10.asset_id = "10" ra10.asset_name = A10 ra10.asset_description = Asset10 ra10.asset_strategy_strategy_id = "1" ra10.asset_strategy = rs1
		rs1.strategy_id = "1" rs1.strategy_name = Strat1 rs1.strategy_description = Strategy1 
		rs2.strategy_id = "2" rs2.strategy_name = Strat2 rs2.strategy_description = Strategy2 
		rs3.strategy_id = "3" rs3.strategy_name = Strat3 rs3.strategy_description = Strategy3 
		rs4.strategy_id = "4" rs4.strategy_name = Strat4 rs4.strategy_description = Strategy4 
		rs5.strategy_id = "5" rs5.strategy_name = Strat5 rs5.strategy_description = Strategy5
} 

////////////////////////////////////////////////////////////////////////

// Compile time input		
schema Trans = literal : Ty {
	entities
		asset 
		currency 
		transaction
	foreign_keys
		transaction_asset : transaction -> asset
		transaction_currency : transaction -> currency
	attributes	
		asset_id : asset -> Integer
		asset_name : asset -> String
		asset_description : asset -> String
		currency_id : currency -> Integer
		currency_code : currency -> String
		currency_name : currency -> String
		transaction_id : transaction -> Integer
		transaction_date : transaction -> Date	// added as a new column
		transaction_asset_asset_id : transaction -> Integer
		transaction_portfolio_portfolio_id : transaction -> Integer
		transaction_buy_sell_ind : transaction -> Boolean
		transaction_quantity : transaction -> Integer
		transaction_price : transaction -> Double
		transaction_currency_currency_id : transaction -> Integer
	observation_equations	
		forall t. t.transaction_asset.asset_id = t.transaction_asset_asset_id
		forall t. t.transaction_currency.currency_id = t.transaction_currency_currency_id
} 

// Runtime input
instance TransInstance = literal : Trans {
     generators 
		ta1 ta3 ta5 ta7 ta9 : asset 
		tc1 tc2 tc3 : currency 
		tt1 tt2 tt3 tt4 tt5 tt6 : transaction
     equations
		ta1.asset_id = "1" ta1.asset_name = A1 ta1.asset_description = Asset1 
		ta3.asset_id = "3" ta3.asset_name = A3 ta3.asset_description = Asset3 
		ta5.asset_id = "5" ta5.asset_name = A5 ta5.asset_description = Asset5 
		ta7.asset_id = "7" ta7.asset_name = A7 ta7.asset_description = Asset7 
		ta9.asset_id = "9" ta9.asset_name = A9 ta9.asset_description = Asset9 
		tc1.currency_id = "1" tc1.currency_code = USD tc1.currency_name = "US Dollar" 
		tc2.currency_id = "2" tc2.currency_code = EUR tc2.currency_name = Euro 
		tc3.currency_id = "3" tc3.currency_code = JPY tc3.currency_name = "Japanese Yen" 
		tt1.transaction_id = "1" tt1.transaction_asset_asset_id = "1" tt1.transaction_asset = ta1 tt1.transaction_portfolio_portfolio_id = "1" tt1.transaction_buy_sell_ind = buy tt1.transaction_quantity = "200" tt1.transaction_price = "5.51" tt1.transaction_currency_currency_id  = "2" tt1.transaction_currency = tc2 tt1.transaction_date = "1/1/2011"
		tt2.transaction_id = "2" tt2.transaction_asset_asset_id = "1" tt2.transaction_asset = ta1 tt2.transaction_portfolio_portfolio_id = "1" tt2.transaction_buy_sell_ind = sell tt2.transaction_quantity = "100" tt2.transaction_price = "5.5" tt2.transaction_currency_currency_id  = "2" tt2.transaction_currency = tc2 tt2.transaction_date = "6/1/2011"
		tt3.transaction_id = "3" tt3.transaction_asset_asset_id = "3" tt3.transaction_asset = ta3 tt3.transaction_portfolio_portfolio_id = "2" tt3.transaction_buy_sell_ind = buy tt3.transaction_quantity = "150" tt3.transaction_price = "2.5" tt3.transaction_currency_currency_id  = "1" tt3.transaction_currency = tc1 tt3.transaction_date = "9/1/2013"
		tt4.transaction_id = "4" tt4.transaction_asset_asset_id = "3" tt4.transaction_asset = ta3 tt4.transaction_portfolio_portfolio_id = "2" tt4.transaction_buy_sell_ind = sell tt4.transaction_quantity = "150" tt4.transaction_price = "3.5" tt4.transaction_currency_currency_id  = "1" tt4.transaction_currency = tc1 tt4.transaction_date = "3/1/2014"
		tt5.transaction_id = "5" tt5.transaction_asset_asset_id = "5" tt5.transaction_asset = ta5 tt5.transaction_portfolio_portfolio_id = "3" tt5.transaction_buy_sell_ind = buy tt5.transaction_quantity = "200" tt5.transaction_price = "1.6" tt5.transaction_currency_currency_id  = "3" tt5.transaction_currency = tc3 tt5.transaction_date = "2/1/2013"
		tt6.transaction_id = "6" tt6.transaction_asset_asset_id = "5" tt6.transaction_asset = ta5 tt6.transaction_portfolio_portfolio_id = "3" tt6.transaction_buy_sell_ind = sell tt6.transaction_quantity = "100" tt6.transaction_price = "1.5" tt6.transaction_currency_currency_id  = "3" tt6.transaction_currency = tc3 tt6.transaction_date = "7/1/2013"
} 

////////////////////////////////////////////////////////////////////////

// Compile time input
schema HoldPos = literal : Ty {
	entities
		client 
		holding 
		position
	foreign_keys	
		holding_client : holding -> client
		position_client : position -> client
	attributes	
		client_no : client -> Integer	// new name
		client_nm : client -> String	// new name
		client_desc : client -> String	// new name
		holding_id : holding -> Integer
		holding_client_client_no : holding -> Integer	// new name
		holding_portfolio_portfolio_id : holding -> Integer
		holding_asset_asset_id : holding -> Integer
		holding_quantity : holding -> Integer
		holding_purchase_date : holding -> Date
		holding_purchase_price : holding -> Double
		holding_currency_currency_code : holding -> String	// switched from _id to _code
		holding_begin_date : holding -> Date	// new attribute
		holding_end_date : holding -> Date		// new attribute
		position_id : position -> Integer
		position_client_client_no : position -> Integer	// new name
		position_asset_asset_id : position -> Integer
		position_quantity : position -> Integer
		position_current_value : position -> Integer
		position_current_value_currency_currency_code : position -> String	// switched from _id to _code
		position_cost_basis : position -> Integer
		position_cost_basis_currency_currency_code : position -> String	// switched from _id to _code
	observation_equations	
		forall h. h.holding_client.client_no = h.holding_client_client_no
		forall p. p.position_client.client_no = p.position_client_client_no
} 

// Runtime input
instance HoldPosInstance = literal : HoldPos {
     generators 
  		hpc1 hpc2 : client 
		hph1 hph2 hph3 hph4 hph5 : holding 
		hpp1 hpp2 : position
     equations
		// below lines were also updated to reflect the new column names in HoldPos.client
		hpc1.client_no  = "1" hpc1.client_nm = Tom hpc1.client_desc = "Tom Client" 
		hpc2.client_no  = "2" hpc2.client_nm = Dick hpc2.client_desc = "Dick Client" 

		// inserted two holding instances with IDs 2 & 5 ID values for all instances except 1 were adjusted
		hph1.holding_id = "1" hph1.holding_client_client_no = "1" hph1.holding_client = hpc1 hph1.holding_portfolio_portfolio_id = "1" hph1.holding_asset_asset_id = "1" hph1.holding_quantity = "200" hph1.holding_purchase_date = "1/1/2011" hph1.holding_purchase_price = "5.51" hph1.holding_currency_currency_code = "EUR" hph1.holding_begin_date = "1/1/2011" hph1.holding_end_date = "6/1/2011"
		hph2.holding_id = "2" hph2.holding_client_client_no = "1" hph2.holding_client = hpc1 hph2.holding_portfolio_portfolio_id = "1" hph2.holding_asset_asset_id = "1" hph2.holding_quantity = "100" hph2.holding_purchase_date = "1/1/2011" hph2.holding_purchase_price = "5.51" hph2.holding_currency_currency_code = "EUR" hph2.holding_begin_date = "6/1/2011"
		hph3.holding_id = "3" hph3.holding_client_client_no = "2" hph3.holding_client = hpc2 hph3.holding_portfolio_portfolio_id = "3" hph3.holding_asset_asset_id = "3" hph3.holding_quantity = "150" hph3.holding_purchase_date = "9/1/2013" hph3.holding_purchase_price = "2.5"  hph3.holding_currency_currency_code = "USD" hph3.holding_begin_date = "9/1/2013" hph3.holding_end_date = "3/1/2014"
		hph4.holding_id = "4" hph4.holding_client_client_no = "2" hph4.holding_client = hpc2 hph4.holding_portfolio_portfolio_id = "3" hph4.holding_asset_asset_id = "5" hph4.holding_quantity = "200" hph4.holding_purchase_date = "2/1/2013" hph4.holding_purchase_price = "1.6"  hph4.holding_currency_currency_code = "JPY" hph4.holding_begin_date = "2/1/2013" hph4.holding_end_date = "7/1/2013"
		hph5.holding_id = "5" hph5.holding_client_client_no = "2" hph5.holding_client = hpc2 hph5.holding_portfolio_portfolio_id = "3" hph5.holding_asset_asset_id = "5" hph5.holding_quantity = "100" hph5.holding_purchase_date = "2/1/2013" hph5.holding_purchase_price = "1.6"  hph5.holding_currency_currency_code = "JPY" hph5.holding_begin_date = "7/1/2013"

		hpp1.position_id = "1" hpp1.position_client_client_no = "1" hpp1.position_client = hpc1 hpp1.position_asset_asset_id = "1" hpp1.position_quantity = "100" hpp1.position_current_value = "550" hpp1.position_current_value_currency_currency_code = "EUR" hpp1.position_cost_basis = "551" hpp1.position_cost_basis_currency_currency_code = "EUR"		
		hpp2.position_id = "2" hpp2.position_client_client_no = "2" hpp2.position_client = hpc2 hpp2.position_asset_asset_id = "5" hpp2.position_quantity = "100" hpp2.position_current_value = "170" hpp2.position_current_value_currency_currency_code = "JPY" hpp2.position_cost_basis = "160" hpp2.position_cost_basis_currency_currency_code = "JPY"
} 

////////////////////////////////////////////////////////////////////////
// Compute the canonical colimit schema using the quotient method
////////////////////////////////////////////////////////////////////////
schema_colimit Colimit = quotient Client + Ref + Trans + Portfolio + HoldPos : Ty {
	 entity_equations
		Client.client = HoldPos.client
		Ref.asset     = Trans.asset
		Ref.currency  = Trans.currency
		Ref.strategy  = Portfolio.strategy
	observation_equations
		forall x. x.Client_client_client_id          = x.HoldPos_client_client_no
		forall x. x.Client_client_client_name        = x.HoldPos_client_client_nm
		forall x. x.Client_client_client_description = x.HoldPos_client_client_desc

		forall x. x.Ref_strategy_strategy_id          = x.Portfolio_strategy_strategy_id
		forall x. x.Ref_strategy_strategy_name        = x.Portfolio_strategy_strategy_name
		forall x. x.Ref_strategy_strategy_description = x.Portfolio_strategy_strategy_description

		forall x. x.Ref_asset_asset_id          = x.Trans_asset_asset_id
		forall x. x.Ref_asset_asset_name        = x.Trans_asset_asset_name
		forall x. x.Ref_asset_asset_description = x.Trans_asset_asset_description

		forall x. x.Ref_currency_currency_id   = x.Trans_currency_currency_id
		forall x. x.Ref_currency_currency_code = x.Trans_currency_currency_code
		forall x. x.Ref_currency_currency_name = x.Trans_currency_currency_name
	options
		simplify_names=false	 
}

////////////////////////////////////////////////////////////////////////
// The instance coproduct
////////////////////////////////////////////////////////////////////////

instance ClientFwd = sigma (getMapping Colimit Client) ClientInstance
instance PortfolioFwd = sigma (getMapping Colimit Portfolio) PortfolioInstance
instance RefFwd = sigma (getMapping Colimit Ref) RefInstance 
instance TransFwd = sigma (getMapping Colimit Trans) TransInstance
instance HoldPosFwd = sigma (getMapping Colimit HoldPos) HoldPosInstance

instance CoProd = literal : getSchema Colimit {
	imports 
		ClientFwd PortfolioFwd  RefFwd  TransFwd  HoldPosFwd 
}

////////////////////////////////////////////////////////////////////////
// The quotient instance
////////////////////////////////////////////////////////////////////////
instance E = literal : getSchema Colimit {
	generators
		ec1 ec2 : Client_client__HoldPos_client
		ea1 ea3 ea5 ea7 ea9 : Ref_asset__Trans_asset
		ecur1 ecur2 ecur3 :  Ref_currency__Trans_currency
		es1 es2 es3 : Portfolio_strategy__Ref_strategy
}

////////////////////////////////////////////////////////////////////////
// The transforms
////////////////////////////////////////////////////////////////////////
transform a1 = literal : E -> CoProd {
	generators
		ec1 -> cc1
		ec2 -> cc2
		ea1 -> ra1
		ea3 -> ra3 
		ea5 -> ra5 
		ea7 -> ra7
		ea9 -> ra9
		ecur1 -> rcur1
		ecur2 -> rcur2
		ecur3 -> rcur3
		es1 -> rs1
		es2 -> rs2
		es3 -> rs3
}

transform a2 = literal : E -> CoProd {
	generators
		ec1 -> hpc1
		ec2 -> hpc2
		ea1 -> ta1
		ea3 -> ta3 
		ea5 -> ta5 
		ea7 -> ta7
		ea9 -> ta9
		ecur1 -> tc1
		ecur2 -> tc2
		ecur3 -> tc3
		es1 -> ps1
		es2 -> ps2
		es3 -> ps3
}

////////////////////////////////////////////////////////////////////////
// The final step
////////////////////////////////////////////////////////////////////////
instance ColimitInstance = coequalize a1 a2

////////////////////////////////////////////////////////////////////////
//Desired Target Schema translated from asset-mgmt-er.pdf
////////////////////////////////////////////////////////////////////////

//compile time input
schema Target = literal : Ty {
	entities
		currency //Trans Ref
		strategy //Portfolio Ref
		asset //Trans Ref
		transaction //Trans
		client //HoldPos
		position //HoldPos
		country //Ref
		portfolioholding //NEW
		portfolio //Portfolio 
		holding //HoldPos
	
	foreign_keys
		portfolio_strategy : portfolio -> strategy //Portfolio
		portfolio_parent_portfolio : portfolio -> portfolio //Added for cyclic FK on Portfolio.portfolio.  ryan also added the cyclic fk to 
		
		position_client : position -> client //HoldPos
		position_asset : position -> asset //NEW
		position_current_value_currency : position -> currency //NEW
		position_cost_basis_currency : position -> currency //NEW
		
		asset_strategy : asset -> strategy //Ref
		
		currency_country : currency -> country //Ref 
		
		transaction_currency : transaction -> currency //Trans 
		transaction_asset : transaction -> asset //Trans 
		transaction_portfolio : transaction -> portfolio //NEW

		holding_currency : holding -> currency //NEW
		// added below we might have missed in defining the "target" schema we had used in "er_pushout.opl"
		// ryan concurs.  todo: meaningless ID discussion 
		holding_asset : holding -> asset

		// below is supposed to be the analog of the "junction table" between client portfolio & holding
		// but in looking at the holding table above seems redundant. Why is that?
		portfolioholding_holding : portfolioholding -> holding //NEW
		portfolioholding_client : portfolioholding -> client //NEW 
		portfolioholding_portfolio : portfolioholding -> portfolio //NEW
	
	path_equations
		//todo: revisit scenario using parent . client = client rather than parent . parent = parent
		portfolio.portfolio_parent_portfolio.portfolio_parent_portfolio = portfolio.portfolio_parent_portfolio

	attributes
		currency_code : currency -> String //Trans Ref
		currency_name : currency -> String //Trans Ref
		currency_id : currency -> Integer //Ref
		currency_country_country_id : currency -> Integer //Ref

		country_code : country -> String //Ref
		country_name : country -> String //Ref
		country_id : country -> Integer //Ref		

		asset_id : asset -> Integer //Trans Ref
		asset_description : asset -> String //Trans Ref
		asset_name : asset -> String //Trans Ref
		asset_strategy_strategy_id : asset -> Integer //Ref
				
		strategy_name : strategy -> String //Ref Portfolio
		strategy_description : strategy -> String //Ref Portfolio
		strategy_id : strategy -> Integer //Ref Portfolio		

		portfolio_name : portfolio -> String //Portfolio
		portfolio_description : portfolio -> String //Portfolio
		portfolio_id : portfolio -> Integer //Portfolio
		portfolio_strategy_strategy_id : portfolio -> Integer //Portfolio
		portfolio_parent_portfolio_portfolio_id : portfolio -> Integer	//Portfolio ADDED
		
		position_quantity : position -> Integer //HoldPos
		position_current_value : position -> Integer //HoldPos
		position_cost_basis : position -> Integer //HoldPos
		position_id : position -> Integer //HoldPos
		position_client_client_id : position -> Integer //HoldPos
		position_asset_asset_id : position -> Integer //HoldPos
		position_current_value_currency_currency_id : position -> Integer // HoldPos 
		position_cost_basis_currency_currency_id : position -> Integer // HoldPos		

		holding_id : holding -> Integer //HoldPos				
		holding_asset_asset_id : holding -> Integer //HoldPos
		holding_currency_currency_id : holding -> Integer //HoldPos
		holding_purchase_price : holding -> Double //HoldPos
		holding_quantity : holding -> Integer //HoldPos //HoldPos
		holding_purchase_date : holding -> Date //HoldPos		
		holding_begin_date : holding -> Date //HoldPos new attribute
		holding_end_date : holding -> Date //HoldPos new attribute

		client_description : client -> String //HoldPos
		client_name : client -> String //HoldPos
		client_id : client -> Integer //HoldPos		

		transaction_id : transaction -> Integer //Trans
		transaction_asset_asset_id : transaction -> Integer //Trans
		transaction_buy_sell_ind : transaction -> Boolean //Trans
		transaction_quantity : transaction -> Integer //Trans
		transaction_price : transaction -> Double //Trans
		transaction_currency_currency_id : transaction -> Integer //Trans			
		transaction_portfolio_portfolio_id : transaction -> Integer //Trans
		transaction_date : transaction -> Date	//added as a new column

		portfolioholding_id : portfolioholding -> Integer //NEW todo: talk to Jee about this
		portfolioholding_holding_holding_id : portfolioholding -> Integer //NEW
		portfolioholding_client_client_id : portfolioholding -> Integer //NEW
		portfolioholding_portfolio_portfolio_id : portfolioholding -> Integer //NEW
	
	observation_equations
		forall p. p.portfolio_parent_portfolio.portfolio_id = p.portfolio_parent_portfolio_portfolio_id //added
		forall p. p.portfolio_strategy.strategy_id = p.portfolio_strategy_strategy_id
		forall a. a.asset_strategy.strategy_id = a.asset_strategy_strategy_id	
		forall p. p.position_client.client_id = p.position_client_client_id
		forall c. c.currency_country.country_id = c.currency_country_country_id
		forall t. t.transaction_currency.currency_id = t.transaction_currency_currency_id
		forall t. t.transaction_asset.asset_id = t.transaction_asset_asset_id
		forall p. p.portfolioholding_holding.holding_id = p.portfolioholding_holding_holding_id
		forall p. p.portfolioholding_client.client_id = p.portfolioholding_client_client_id	
		forall p. p.portfolioholding_portfolio.portfolio_id = p.portfolioholding_portfolio_portfolio_id					
		forall p. p.position_asset.asset_id = p.position_asset_asset_id
		forall h. h.holding_currency.currency_id = h.holding_currency_currency_id
		forall t. t.transaction_portfolio.portfolio_id = t.transaction_portfolio_portfolio_id
		forall p. p.position_current_value_currency.currency_id = p.position_current_value_currency_currency_id
		forall p. p.position_cost_basis_currency.currency_id = p.position_cost_basis_currency_currency_id	
}

////////////////////////////////////////////////////////////////////////
//An uber-flower to migrate from Colim to Target
////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////

query ETL = literal : getSchema Colimit -> Target {
 entity
 
	//identity
	currency -> {from c:Ref_currency__Trans_currency
		attributes currency_code -> c.Ref_currency_currency_code
			currency_name -> c.Ref_currency_currency_name
			currency_id -> c.Ref_currency_currency_id
			currency_country_country_id -> c.Ref_currency_currency_country_country_id
foreign_keys
 	currency_country -> {c -> c.Ref_currency_currency_country} 		
	} 

	//identity
entity	strategy -> {from s:Portfolio_strategy__Ref_strategy
		attributes strategy_name -> s.Ref_strategy_strategy_name
			strategy_description -> s.Ref_strategy_strategy_description
			strategy_id -> s.Ref_strategy_strategy_id

	}

	//identity
entity	asset -> {from a:Ref_asset__Trans_asset
		attributes asset_id -> a.Ref_asset_asset_id
			asset_description -> a.Ref_asset_asset_description
			asset_name -> a.Ref_asset_asset_name
			asset_strategy_strategy_id -> a.Ref_asset_asset_strategy_strategy_id
foreign_keys
 	asset_strategy -> {s -> a.Ref_asset_asset_strategy} 		
		
	} 

	//identity
entity	client -> {from c:Client_client__HoldPos_client 
		attributes client_description -> c.Client_client_client_description 
			client_name -> c.Client_client_client_name
			client_id -> c.Client_client_client_id

	} 

	//identity
entity	country -> {from c:Ref_country
		attributes country_code -> c.Ref_country_country_code
			country_name -> c.Ref_country_country_name
			country_id -> c.Ref_country_country_id

	} 

	//not identity
entity	transaction -> {from t:Trans_transaction p:Portfolio_portfolio c:Client_client__HoldPos_client c1:Client_client__HoldPos_client
		where t.Trans_transaction_transaction_portfolio_portfolio_id = p.Portfolio_portfolio_portfolio_id
		attributes transaction_id -> t.Trans_transaction_transaction_id 
			transaction_asset_asset_id -> t.Trans_transaction_transaction_asset_asset_id 
			transaction_buy_sell_ind -> t.Trans_transaction_transaction_buy_sell_ind 
			transaction_quantity -> t.Trans_transaction_transaction_quantity
			transaction_price -> t.Trans_transaction_transaction_price 
			transaction_date -> t.Trans_transaction_transaction_date
			transaction_currency_currency_id -> t.Trans_transaction_transaction_currency_currency_id 			
			transaction_portfolio_portfolio_id -> t.Trans_transaction_transaction_portfolio_portfolio_id
foreign_keys
 transaction_currency -> {c -> t.Trans_transaction_transaction_currency} 
	transaction_asset -> {a -> t.Trans_transaction_transaction_asset} 
	transaction_portfolio -> {p -> p} 	//NEW	
		}

	//not identity
entity	position -> {from p:HoldPos_position a:Ref_asset__Trans_asset cv:Ref_currency__Trans_currency cb:Ref_currency__Trans_currency
		where p.HoldPos_position_position_asset_asset_id = a.Ref_asset_asset_id
			p.HoldPos_position_position_current_value_currency_currency_code = cv.Ref_currency_currency_code
			p.HoldPos_position_position_cost_basis_currency_currency_code = cb.Ref_currency_currency_code
		attributes position_quantity -> p.HoldPos_position_position_quantity
			position_current_value -> p.HoldPos_position_position_current_value
			position_cost_basis -> p.HoldPos_position_position_cost_basis
			position_id -> p.HoldPos_position_position_id 
			position_client_client_id -> p.HoldPos_position_position_client_client_no
			position_asset_asset_id -> p.HoldPos_position_position_asset_asset_id
			position_current_value_currency_currency_id -> cv.Ref_currency_currency_id
			position_cost_basis_currency_currency_id -> cb.Ref_currency_currency_id
 foreign_keys
 	position_client -> {c -> p.HoldPos_position_position_client} 
	position_asset -> {a -> a}  //NEW
	position_current_value_currency -> {c -> cv}  //NEW
	position_cost_basis_currency -> {c -> cb}  //NEW
	} 

	//not identity
	entity portfolio -> {from p:Portfolio_portfolio
		attributes portfolio_parent_portfolio_portfolio_id -> p.Portfolio_portfolio_portfolio_parent_portfolio_portfolio_id //added
			portfolio_name -> p.Portfolio_portfolio_portfolio_name
			portfolio_description -> p.Portfolio_portfolio_portfolio_description
			portfolio_id -> p.Portfolio_portfolio_portfolio_id
			portfolio_strategy_strategy_id -> p.Portfolio_portfolio_portfolio_strategy_strategy_id
 foreign_keys
 	portfolio_parent_portfolio -> {p -> p.Portfolio_portfolio_portfolio_parent_portfolio}  //added
	portfolio_strategy -> {s -> p.Portfolio_portfolio_portfolio_strategy} 
	} 

	//not identity
	entity holding -> {from h:HoldPos_holding c:Ref_currency__Trans_currency a:Ref_asset__Trans_asset
		where a.Ref_asset_asset_id = h.HoldPos_holding_holding_asset_asset_id
			h.HoldPos_holding_holding_currency_currency_code = c.Ref_currency_currency_code
		attributes holding_id -> h.HoldPos_holding_holding_id			
			holding_asset_asset_id -> h.HoldPos_holding_holding_asset_asset_id
			holding_currency_currency_id -> c.Ref_currency_currency_id 			
			holding_purchase_price -> h.HoldPos_holding_holding_purchase_price 
			holding_quantity -> h.HoldPos_holding_holding_quantity 
			holding_purchase_date -> h.HoldPos_holding_holding_purchase_date 
			holding_begin_date -> h.HoldPos_holding_holding_begin_date 
			holding_end_date -> h.HoldPos_holding_holding_end_date
			foreign_keys
 	holding_asset -> {a -> a} 
	holding_currency -> {c -> c}  //NEW
	
	} 
   
	//not identity
	// Q: In this particular query, I had to rename h.holding_client
	//    to h.HoldPos_holding_holding_client. I did not have to do that for
	//    the other FKs. Why? There is a line in the foreign_keys
	//    section that names portfolioholding_client to that.
entity	portfolioholding -> {from h:HoldPos_holding p:Portfolio_portfolio cur: Ref_currency__Trans_currency c1:Client_client__HoldPos_client a:Ref_asset__Trans_asset
		where a.Ref_asset_asset_id = h.HoldPos_holding_holding_asset_asset_id
			p.Portfolio_portfolio_portfolio_parent_portfolio.Portfolio_portfolio_portfolio_client_client_id = c1.Client_client_client_id
			c1.Client_client_client_id = h.HoldPos_holding_holding_client.Client_client_client_id
			h.HoldPos_holding_holding_currency_currency_code = cur.Ref_currency_currency_code
			p.Portfolio_portfolio_portfolio_client_client_id = h.HoldPos_holding_holding_client.Client_client_client_id
			h.HoldPos_holding_holding_portfolio_portfolio_id = p.Portfolio_portfolio_portfolio_id
		attributes portfolioholding_id -> keygen(h.HoldPos_holding_holding_id, h.HoldPos_holding_holding_client.Client_client_client_id, p.Portfolio_portfolio_portfolio_id) //added by ryan
			portfolioholding_holding_holding_id -> h.HoldPos_holding_holding_id
			portfolioholding_client_client_id -> c1.Client_client_client_id
			portfolioholding_portfolio_portfolio_id -> p.Portfolio_portfolio_portfolio_id
foreign_keys
 
	portfolioholding_holding -> {h -> h c -> cur a -> a}
	portfolioholding_client -> {c -> h.HoldPos_holding_holding_client}  
	portfolioholding_portfolio -> {p -> p} 	
	}

} 

instance Output = eval ETL ColimitInstance

////////////////////////////////////////////////////////////////////////
// Target schema A - cyclic FK version (on the Portfolio entity)
////////////////////////////////////////////////////////////////////////

schema CycTarget = literal : Ty {
	entities
		client
		country
		currency
		strategy
		asset
		portfolio
		transaction
		clientposition
		clientportfolioholding

	foreign_keys
		currency_country : currency -> country

		asset_strategy : asset -> strategy

		portfolio_strategy : portfolio -> strategy
		portfolio_parent_portfolio : portfolio -> portfolio // for cyclic FK on portfolio

		transaction_currency : transaction -> currency
		transaction_asset : transaction -> asset
		transaction_portfolio : transaction -> portfolio

		clientposition_client : clientposition -> client
		clientposition_asset : clientposition -> asset

		clientportfolioholding_asset : clientportfolioholding -> asset
		clientportfolioholding_client : clientportfolioholding -> client
		clientportfolioholding_portfolio : clientportfolioholding -> portfolio

	path_equations
		//added by ryan
		portfolio.portfolio_parent_portfolio.portfolio_parent_portfolio = portfolio.portfolio_parent_portfolio 

	attributes
		client_id : client -> Integer
		client_name : client -> String
		client_description : client -> String

		country_id : country -> Integer
		country_code : country -> String
		country_name : country -> String

		currency_id : currency -> Integer
		currency_code : currency -> String
		currency_name : currency -> String
		currency_country_country_id : currency -> Integer

		asset_id : asset -> Integer
		asset_name : asset -> String
		asset_description : asset -> String
		asset_strategy_strategy_id : asset -> Integer

		strategy_id : strategy -> Integer
		strategy_name : strategy -> String
		strategy_description : strategy -> String

		portfolio_id : portfolio -> Integer
		portfolio_name : portfolio -> String
		portfolio_description : portfolio -> String
		portfolio_strategy_strategy_id : portfolio -> Integer
		portfolio_parent_portfolio_portfolio_id : portfolio -> Integer // for cyclic FK on portfolio

		transaction_id : transaction -> Integer
		transaction_date : transaction -> Date
		transaction_portfolio_portfolio_id : transaction -> Integer
		transaction_portfolio_portfolio_name : transaction -> String // intentionally added for query convenience
		transaction_asset_asset_id : transaction -> Integer
		transaction_asset_asset_name : transaction -> String // intentionally added for query convenience
		transaction_buy_sell_ind : transaction -> Boolean
		transaction_quantity : transaction -> Integer
		transaction_price : transaction -> Double
		transaction_currency_currency_id : transaction -> Integer

		clientposition_id : clientposition -> Integer
		clientposition_begin_date : clientposition -> Date
		clientposition_end_date : clientposition -> Date
		clientposition_client_client_id : clientposition -> Integer
		clientposition_client_client_name : clientposition -> String // intentionally added for query convenience
		clientposition_asset_asset_id : clientposition -> Integer
		clientposition_asset_asset_name : clientposition -> String // intentionally added for query convenience
		clientposition_quantity : clientposition -> Integer
		clientposition_current_value : clientposition -> Integer
		clientposition_current_value_currency_currency_code : clientposition -> String // intentionally code instead of id for query convenience

		clientportfolioholding_id : clientportfolioholding -> Integer
		clientportfolioholding_begin_date : clientportfolioholding -> Date
		clientportfolioholding_end_date : clientportfolioholding -> Date
		clientportfolioholding_portfolio_portfolio_id : clientportfolioholding -> Integer
		clientportfolioholding_portfolio_portfolio_name : clientportfolioholding -> String // intentionally added for query convenience
		clientportfolioholding_client_client_id : clientportfolioholding -> Integer
		clientportfolioholding_client_client_name : clientportfolioholding -> String // intentionally added for query convenience
		clientportfolioholding_asset_asset_id : clientportfolioholding -> Integer
		clientportfolioholding_asset_asset_name : clientportfolioholding -> String // intentionally added for query convenience
		clientportfolioholding_quantity : clientportfolioholding -> Integer
		clientportfolioholding_purchase_date : clientportfolioholding -> Date
		clientportfolioholding_purchase_price : clientportfolioholding -> Double
		clientportfolioholding_currency_currency_code : clientportfolioholding -> String // intentionally code instead of id for query convenience
	
	observation_equations
		forall c. c.currency_country.country_id = c.currency_country_country_id

		forall a. a.asset_strategy.strategy_id = a.asset_strategy_strategy_id

		forall p. p.portfolio_strategy.strategy_id = p.portfolio_strategy_strategy_id
		forall p. p.portfolio_parent_portfolio.portfolio_id = p.portfolio_parent_portfolio_portfolio_id

		forall t. t.transaction_currency.currency_id = t.transaction_currency_currency_id
		forall t. t.transaction_asset.asset_id = t.transaction_asset_asset_id
		forall t. t.transaction_portfolio.portfolio_id = t.transaction_portfolio_portfolio_id

		forall p. p.clientposition_asset.asset_id = p.clientposition_asset_asset_id
		forall p. p.clientposition_client.client_id = p.clientposition_client_client_id

		forall p. p.clientportfolioholding_asset.asset_id = p.clientportfolioholding_asset_asset_id
		forall p. p.clientportfolioholding_client.client_id = p.clientportfolioholding_client_client_id
		forall p. p.clientportfolioholding_portfolio.portfolio_id = p.clientportfolioholding_portfolio_portfolio_id

} 

query ETL_Cyc = literal : Target -> CycTarget {
 entity
 	
	client -> {from c:client
		 attributes client_id -> c.client_id
			client_name -> c.client_name
			client_description -> c.client_description
	} 

	entity country -> {from c:country
		 attributes country_id -> c.country_id
			country_code -> c.country_code
			country_name -> c.country_name
	} 

entity	currency -> {from c:currency
		 attributes currency_id -> c.currency_id
			currency_code -> c.currency_code
			currency_name -> c.currency_name
			currency_country_country_id -> c.currency_country_country_id
			foreign_keys
	currency_country -> {c -> c.currency_country} 
	
	} 

entity	asset -> {from a:asset
		 attributes asset_id -> a.asset_id
			asset_name -> a.asset_name
			asset_description -> a.asset_description
			asset_strategy_strategy_id -> a.asset_strategy_strategy_id
			foreign_keys
			asset_strategy -> {s -> a.asset_strategy} 
	
	} 

entity	strategy -> {from s:strategy
		 attributes strategy_id -> s.strategy_id
			strategy_name -> s.strategy_name
			strategy_description -> s.strategy_description
	}

entity	portfolio -> {from p:portfolio
		 attributes portfolio_id -> p.portfolio_id
			portfolio_name -> p.portfolio_name
			portfolio_description -> p.portfolio_description
			portfolio_strategy_strategy_id -> p.portfolio_strategy_strategy_id
			portfolio_parent_portfolio_portfolio_id -> p.portfolio_parent_portfolio_portfolio_id
			foreign_keys
			portfolio_parent_portfolio -> {p -> p.portfolio_parent_portfolio} 
	portfolio_strategy -> {s->p.portfolio_strategy} 
	
	} 

entity	transaction -> {from t:transaction
		 attributes transaction_id -> t.transaction_id
			transaction_date -> t.transaction_date
			transaction_portfolio_portfolio_id -> t.transaction_portfolio_portfolio_id
			transaction_portfolio_portfolio_name -> t.transaction_portfolio.portfolio_name
			transaction_asset_asset_id -> t.transaction_asset_asset_id
			transaction_asset_asset_name -> t.transaction_asset.asset_name
			transaction_buy_sell_ind -> t.transaction_buy_sell_ind
			transaction_quantity -> t.transaction_quantity
			transaction_price -> t.transaction_price
			transaction_currency_currency_id -> t.transaction_currency_currency_id
		foreign_keys
			transaction_currency -> {c -> t.transaction_currency} 
	transaction_asset -> {a -> t.transaction_asset} 
	transaction_portfolio -> {p -> t.transaction_portfolio} 
	
	} 

entity	clientposition -> {from p:position
		 attributes clientposition_id -> p.position_id
			clientposition_begin_date -> "1/1/2011" // fake this for now source doesn't have it
			clientposition_end_date -> "1/1/2011" // fake this for now source doesn't have it
			clientposition_client_client_id -> p.position_client.client_id
			clientposition_client_client_name -> p.position_client.client_name
			clientposition_asset_asset_id -> p.position_asset.asset_id
			clientposition_asset_asset_name -> p.position_asset.asset_name
			clientposition_quantity -> p.position_quantity
			clientposition_current_value -> p.position_current_value
			clientposition_current_value_currency_currency_code -> p.position_current_value_currency.currency_code
	 foreign_keys
			clientposition_client -> {c -> p.position_client} 
			clientposition_asset -> {a -> p.position_asset} 
	} 

entity	clientportfolioholding -> {from ph:portfolioholding
		 attributes clientportfolioholding_id -> ph.portfolioholding_holding.holding_id
			clientportfolioholding_begin_date -> ph.portfolioholding_holding.holding_begin_date
			clientportfolioholding_end_date -> ph.portfolioholding_holding.holding_end_date
			clientportfolioholding_asset_asset_id -> ph.portfolioholding_holding.holding_asset.asset_id
			clientportfolioholding_asset_asset_name -> ph.portfolioholding_holding.holding_asset.asset_name
			clientportfolioholding_purchase_price -> ph.portfolioholding_holding.holding_purchase_price
			clientportfolioholding_quantity -> ph.portfolioholding_holding.holding_quantity
			clientportfolioholding_purchase_date -> ph.portfolioholding_holding.holding_purchase_date
			clientportfolioholding_client_client_id -> ph.portfolioholding_client.client_id
			clientportfolioholding_client_client_name -> ph.portfolioholding_client.client_name
			clientportfolioholding_portfolio_portfolio_id -> ph.portfolioholding_portfolio.portfolio_id
			clientportfolioholding_portfolio_portfolio_name -> ph.portfolioholding_portfolio.portfolio_name
			clientportfolioholding_currency_currency_code -> ph.portfolioholding_holding.holding_currency.currency_code
		foreign_keys
			clientportfolioholding_client -> {c -> ph.portfolioholding_client} 
 			clientportfolioholding_asset -> {a -> ph.portfolioholding_holding.holding_asset} 
			clientportfolioholding_portfolio -> {p -> ph.portfolioholding_portfolio} 
	} 

 

} 

instance OutputCyc = eval ETL_Cyc Output 
 
instance OutputCycDistinct = distinct OutputCyc
