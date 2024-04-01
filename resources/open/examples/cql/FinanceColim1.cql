####################################
# Financial Data Warehousing

typeside Ty = literal {
	external_types
		Integer -> "java.lang.Integer"
		String -> "java.lang.String"
		Double -> "java.lang.Double"
		Boolean -> "java.lang.String"
		Date -> "java.lang.String"
	external_parsers
		Integer -> "parseInt;"
		String -> "x => x"
		Double -> "x => java.lang.Double.parseDouble(x)"
		Boolean -> "x => x"
		Date -> "x => x"
	external_functions
		keygen : Integer,Integer,Integer -> Integer = "(x, y, z) => (2^x * 3^y * 5^z)"
}

####################################

schema Client = literal : Ty {
	entities
		client
	attributes
		id : client -> Integer
		name description : client -> String
}

instance ClientInstance = literal : Client {
     generators
     	cc1 cc2 cc3 : client
     multi_equations
     	id -> {cc1 1, cc2 2, cc3 3}
     	name -> {cc1 Tom, cc2 Dick, cc3 Harry}
     	description -> {cc1 "Tom Client", cc2 "Dick Client", cc3 "Harry Client"}
}

####################################

schema Portfolio = literal : Ty {
	entities
		strategy
		portfolio
	foreign_keys
		strategy : portfolio -> strategy
		parent : portfolio -> portfolio
	path_equations
		portfolio.parent.parent = portfolio.parent
	attributes
		id : strategy -> Integer
		name description : strategy -> String
		id : portfolio -> Integer
		name description : portfolio -> String
		client_id strategy_id parent_id : portfolio -> Integer
	observation_equations
		forall p. p.strategy.id = p.strategy_id
		forall p. p.parent.id = p.parent_id
}

instance PortfolioInstance = literal : Portfolio {
    generators
     	ps1 ps2 ps3 : strategy
     	pp1 pp2 pp3 : portfolio
	multi_equations
		parent_id -> {pp1 1, pp2 2, pp3 2}
		parent -> {pp1 pp1, pp2 pp2, pp3 pp2}
		id -> {ps1 1, ps2 2, ps3 3, pp1 1, pp2 2, pp3 3}
		name -> {ps1 Strat1, ps2 Strat2, ps3 Strat3, pp1 Port1, pp2 Port2, pp3 Port3}
		description -> {ps1 Strategy1, ps2 Strategy2, ps3 Strategy3, pp1 Portfolio1, pp2 Portfolio2, pp3 Portfolio3}
		strategy_id -> {pp1 2, pp2 2, pp3 1}
		client_id -> {pp1 1, pp2 2, pp3 2}
		strategy -> {pp1 ps2, pp2 ps2, pp3 ps1}
}

####################################

schema Ref = literal : Ty {
	entities
		country
		currency
		asset
		strategy
	foreign_keys
		country : currency -> country
		strategy : asset -> strategy
	attributes
		id : country -> Integer
		code name : country -> String
		id strategy_id : asset -> Integer
		name description : asset -> String
		id : strategy -> Integer
		name description : strategy -> String
		id country_id : currency -> Integer
		code name : currency -> String
	observation_equations
		forall c. c.country.id = c.country_id
		forall a. a.strategy.id = a.strategy_id
}

instance RefInstance = literal : Ref {
     generators
     	rcty1 rcty2 rcty3 rcty4 rcty5 rcty6 : country
		rcur1 rcur2 rcur3 : currency
		ra1 ra2 ra3 ra4 ra5 ra6 ra7 ra8 ra9 ra10 : asset
		rs1 rs2 rs3 rs4 rs5 : strategy
	multi_equations
		country -> {rcur1 rcty6, rcur2 rcty4, rcur3 rcty5}
		country_id  -> {rcur1 6, rcur2 4, rcur3 5}
		strategy -> {ra1 rs1, ra2 rs3, ra3 rs2, ra4 rs4, ra5 rs5, ra6 rs4, ra7 rs4, ra8 rs5, ra9 rs3, ra10 rs1}
		strategy_id  -> {ra1 1, ra2 3, ra3 2, ra4 4, ra5 5, ra6 4, ra7 4, ra8 5, ra9 3, ra10 1}
		id  -> {rcty1 1, rcty2 2, rcty3 3, rcty4 4, rcty5 5, rcty6 6, rcur1 1, rcur2 2, rcur3 3, ra1 1, ra2 2, ra3 3, ra4 4, ra5 5, ra6 6, ra7 7, ra8 8, ra9 9, ra10 10, rs1 1, rs2 2, rs3 3, rs4 4, rs5 5}
		code  -> {rcty1 AU, rcty2 CH, rcty3 CN, rcty4 EU, rcty5 JP, rcty6 US, rcur1 USD, rcur2 EUR, rcur3 JPY}
		name  -> {ra1 A1, ra2 A2, ra3 A3, ra4 A4, ra5 A5, ra6 A6, ra7 A7, ra8 A8, ra9 A9, ra10 A10, rs1 Strat1, rs2 Strat2, rs3 Strat3, rs4 Strat4, rs5 Strat5, rcty1 Australia, rcty2 Switzerland, rcty3 China, rcty4 "European Union", rcty5 Japan, rcty6 "United States of America", rcur1 "US Dollar", rcur2 "Euro", rcur3 "Japanese Yen" }
		description  -> {ra1 Asset1, ra2 Asset2, ra3 Asset3, ra4 Asset4, ra5 Asset5, ra6 Asset6, ra7 Asset7, ra8 Asset8, ra9 Asset9, ra10 Asset10, rs1 Strategy1, rs2 Strategy2, rs3 Strategy3, rs4 Strategy4, rs5 Strategy5}
}

####################################

schema Trans = literal : Ty {
	entities
		asset
		currency
		transaction
	foreign_keys
		asset : transaction -> asset
		currency : transaction -> currency
	attributes
		id : asset -> Integer
		name description : asset -> String
		id : currency -> Integer
		code name : currency -> String
		id asset_id portfolio_id quantity currency_id : transaction -> Integer
		date  : transaction -> Date	# added as a new column
		buy_sell_ind : transaction -> Boolean
		price : transaction -> Double
	observation_equations
		forall t. t.asset.id = t.asset_id
		forall t. t.currency.id = t.currency_id
}

instance TransInstance = literal : Trans {
     generators
		ta1 ta3 ta5 ta7 ta9 : asset
		tc1 tc2 tc3 : currency
		tt1 tt2 tt3 tt4 tt5 tt6 : transaction
	multi_equations
		id -> {ta1 1, ta3 3, ta5 5, ta7 7, ta9 9, tc1 1, tc2 2, tc3 3, tt1 1, tt2 2, tt3 3, tt4 4, tt5 5, tt6 6}
		name -> {ta1 A1, ta3 A3, ta5 A5, ta7 A7, ta9 A9, tc1 "US Dollar", tc2 Euro, tc3 "Japanese Yen"}
		description -> {ta1 Asset1, ta3 Asset3, ta5 Asset5, ta7 Asset7, ta9 Asset9}
		code -> {tc1 USD, tc2 EUR, tc3 JPY}
		asset -> {tt1 ta1, tt2 ta1, tt3 ta3, tt4 ta3, tt5 ta5, tt6 ta5}
		asset_id -> {tt1 1, tt2 1, tt3 3, tt4 3, tt5 5, tt6 5}
		portfolio_id -> {tt1 1, tt2 1, tt3 2, tt4 2, tt5 3, tt6 3}
		buy_sell_ind -> {tt1 buy, tt2 sell, tt3 buy, tt4 sell, tt5 buy, tt6 sell}
		quantity -> {tt1 200, tt2 100, tt3 150, tt4 150, tt5 200, tt6 100}
		price -> {tt1 "5.51", tt2 "5.5", tt3 "2.5", tt4 "3.5", tt5 "1.6", tt6 "1.5"}
		date -> {tt1 "1/1/2011", tt2 "6/1/2011", tt3 "9/1/2013", tt4 "3/1/2014", tt5 "2/1/2013", tt6 "7/1/2013"}
		currency -> {tt1 tc2, tt2 tc2, tt3 tc1, tt4 tc1, tt5 tc3, tt6 tc3}
		currency_id -> {tt1 2, tt2 2, tt3 1, tt4 1, tt5 3, tt6 3}
}

####################################

schema HoldPos = literal : Ty {
	entities
		client
		holding
		position
	foreign_keys
		client : holding -> client
		client : position -> client
	attributes
		no : client -> Integer	# new name
		nm desc : client -> String	# new name
		id client_no portfolio_id asset_id quantity : holding -> Integer
		purchase_date begin_date end_date : holding -> Date
		purchase_price : holding -> Double
		currency_code : holding -> String	# switched from _id to _code
		id client_no asset_id quantity current_value cost_basis : position -> Integer	# new name
		current_value_currency_code cost_basis_currency_code : position -> String	# switched from _id to _code
	observation_equations
		forall h:holding. h.client.no = h.client_no
		forall p:position. p.client.no = p.client_no
}

instance HoldPosInstance = literal : HoldPos {
    generators
  		hpc1 hpc2 : client
		hph1 hph2 hph3 hph4 hph5 : holding
		hpp1 hpp2 : position
	multi_equations
		no -> {hpc1 1, hpc2 2}
		nm -> {hpc1 Tom, hpc2 Dick}
		desc -> {hpc1 "Tom Client", hpc2 "Dick Client"}
		id -> {hph1 1, hph2 2, hph3 3, hph4 4, hph5 5, hpp1 1, hpp2 2}
		client_no -> {hph1 1, hph2 1, hph3 2, hph4 2, hph5 2, hpp1 1, hpp2 2}
		client -> {hph1 hpc1, hph2 hpc1, hph3 hpc2, hph4 hpc2, hph5 hpc2, hpp1 hpc1, hpp2 hpc2}
		portfolio_id -> {hph1 1, hph2 1, hph3 3, hph4 3, hph5 3}
		asset_id -> {hph1 1, hph2 1, hph3 3, hph4 5, hph5 5}
		quantity -> {hph1 200, hph2 100, hph3 150, hph4 200, hph5 100, hpp1 100, hpp2 100}
		purchase_date -> {hph1 "1/1/2011", hph2 "1/1/2011", hph3 "9/1/2013", hph4 "2/1/2013", hph5 "2/1/2013"}
		begin_date -> {hph1 "1/1/2011", hph2 "6/1/2011", hph3 "9/1/2013", hph4 "2/1/2013", hph5 "7/1/2013"}
		end_date -> {hph1 "6/1/2011", hph3 "3/1/2014", hph4 "7/1/2013"}
		purchase_price -> {hph1 "5.51", hph2 "5.1", hph3 "2.5", hph4 "1.6", hph5 "1.6"}
		currency_code -> {hph1 EUR, hph2 EUR, hph3 JPY, hph4 USD, hph5 USD}
		asset_id -> {hpp1 1, hpp2 5}
		current_value -> {hpp1 550, hpp2 170}
		cost_basis -> {hpp1 551, hpp2 160}
		current_value_currency_code -> {hpp1 EUR, hpp2 JPY}
		cost_basis_currency_code -> {hpp1 EUR, hpp2 JPY}
}

####################################

schema_colimit Colimit = quotient Client + Ref + Trans + Portfolio + HoldPos : Ty {
	 entity_equations
		Client.client = HoldPos.client
		Ref.asset     = Trans.asset
		Ref.currency  = Trans.currency
		Ref.strategy  = Portfolio.strategy
	observation_equations
		forall x. x.Client_client_id          = x.HoldPos_client_no
		forall x. x.Client_client_name        = x.HoldPos_client_nm
		forall x. x.Client_client_description = x.HoldPos_client_desc

		forall x. x.Ref_strategy_id          = x.Portfolio_strategy_id
		forall x. x.Ref_strategy_name        = x.Portfolio_strategy_name
		forall x. x.Ref_strategy_description = x.Portfolio_strategy_description

		forall x. x.Ref_asset_id          = x.Trans_asset_id
		forall x. x.Ref_asset_name        = x.Trans_asset_name
		forall x. x.Ref_asset_description = x.Trans_asset_description

		forall x. x.Ref_currency_id   = x.Trans_currency_id
		forall x. x.Ref_currency_code = x.Trans_currency_code
		forall x. x.Ref_currency_name = x.Trans_currency_name
	options
		simplify_names=false
}

instance ClientFwd = sigma (getMapping Colimit Client) ClientInstance
instance PortfolioFwd = sigma (getMapping Colimit Portfolio) PortfolioInstance
instance RefFwd = sigma (getMapping Colimit Ref) RefInstance
instance TransFwd = sigma (getMapping Colimit Trans) TransInstance
instance HoldPosFwd = sigma (getMapping Colimit HoldPos) HoldPosInstance

instance CoProd = coproduct ClientFwd + PortfolioFwd + RefFwd + TransFwd + HoldPosFwd : getSchema Colimit

####################################

instance E = quotient_query CoProd {
	entity Client_client -> {
		from
			a:Client_client b:Client_client
		where
			a.Client_client_id = b.HoldPos_client_no
	}
	entity Ref_asset -> {
		from
			a:Ref_asset b:Ref_asset
		where
			a.Ref_asset_id = b.Trans_asset_id
	}
	entity Ref_currency -> {
		from
			a:Ref_currency b:Ref_currency
		where
			a.Ref_currency_id = b.Trans_currency_id
	}
	entity Ref_strategy -> {
		from
			a:Ref_strategy b:Ref_strategy
		where
			a.Portfolio_strategy_id = b.Ref_strategy_id
	}

	options
		quotient_use_chase = false
}

####################################

schema Target = literal : Ty {
	entities
		currency #Trans Ref
		strategy #Portfolio Ref
		asset #Trans Ref
		transaction #Trans
		client #HoldPos
		position #HoldPos
		country #Ref
		portfolioholding #NEW
		portfolio #Portfolio
		holding #HoldPos

	foreign_keys
		portfolio_strategy : portfolio -> strategy #Portfolio
		parent : portfolio -> portfolio

		position_client : position -> client #HoldPos
		position_asset : position -> asset #NEW
		position_current_value_currency : position -> currency #NEW
		position_cost_basis_currency : position -> currency #NEW

		asset_strategy : asset -> strategy #Ref

		currency_country : currency -> country #Ref

		transaction_currency : transaction -> currency #Trans
		transaction_asset : transaction -> asset #Trans
		transaction_portfolio : transaction -> portfolio #NEW

		holding_currency : holding -> currency #NEW
		holding_asset : holding -> asset

		portfolioholding_holding : portfolioholding -> holding #NEW
		portfolioholding_client : portfolioholding -> client #NEW
		portfolioholding_portfolio : portfolioholding -> portfolio #NEW

	path_equations
		portfolio.parent.parent = portfolio.parent

	attributes
		code : currency -> String #Trans Ref
		name : currency -> String #Trans Ref
		id : currency -> Integer #Ref
		country_id : currency -> Integer #Ref

		code : country -> String #Ref
		name : country -> String #Ref
		id : country -> Integer #Ref

		id : asset -> Integer #Trans Ref
		description : asset -> String #Trans Ref
		name : asset -> String #Trans Ref
		strategy_id : asset -> Integer #Ref

		name : strategy -> String #Ref Portfolio
		description : strategy -> String #Ref Portfolio
		id : strategy -> Integer #Ref Portfolio

		name : portfolio -> String #Portfolio
		description : portfolio -> String #Portfolio
		id : portfolio -> Integer #Portfolio
		strategy_id : portfolio -> Integer #Portfolio
		parent_portfolio_id : portfolio -> Integer	#Portfolio ADDED

		quantity : position -> Integer #HoldPos
		current_value : position -> Integer #HoldPos
		cost_basis : position -> Integer #HoldPos
		id : position -> Integer #HoldPos
		client_id : position -> Integer #HoldPos
		asset_id : position -> Integer #HoldPos
		current_value_currency_id : position -> Integer # HoldPos
		cost_basis_currency_id : position -> Integer # HoldPos

		id : holding -> Integer #HoldPos
		asset_id : holding -> Integer #HoldPos
		currency_id : holding -> Integer #HoldPos
		purchase_price : holding -> Double #HoldPos
		quantity : holding -> Integer #HoldPos #HoldPos
		purchase_date : holding -> Date #HoldPos
		begin_date : holding -> Date #HoldPos new attribute
		end_date : holding -> Date #HoldPos new attribute

		description : client -> String #HoldPos
		name : client -> String #HoldPos
		id : client -> Integer #HoldPos

		id : transaction -> Integer #Trans
		asset_id : transaction -> Integer #Trans
		buy_sell_ind : transaction -> Boolean #Trans
		quantity : transaction -> Integer #Trans
		price : transaction -> Double #Trans
		currency_id : transaction -> Integer #Trans
		portfolio_id : transaction -> Integer #Trans
		date : transaction -> Date	#added as a new column

		id : portfolioholding -> Integer #NEW todo: talk to Jee about this
		holding_id : portfolioholding -> Integer #NEW
		client_id : portfolioholding -> Integer #NEW
		portfolio_id : portfolioholding -> Integer #NEW

	observation_equations
		forall p. p.parent.id = p.parent_portfolio_id #added
		forall p. p.portfolio_strategy.id = p.strategy_id
		forall a. a.asset_strategy.id = a.strategy_id
		forall p. p.position_client.id = p.client_id
		forall c. c.currency_country.id = c.country_id
		forall t. t.transaction_currency.id = t.currency_id
		forall t. t.transaction_asset.id = t.asset_id
		forall p. p.portfolioholding_holding.id = p.holding_id
		forall p. p.portfolioholding_client.id = p.client_id
		forall p. p.portfolioholding_portfolio.id = p.portfolio_id
		forall p. p.position_asset.id = p.asset_id
		forall h. h.holding_currency.id = h.currency_id
		forall t. t.transaction_portfolio.id = t.portfolio_id
		forall p. p.position_current_value_currency.id = p.current_value_currency_id
		forall p. p.position_cost_basis_currency.id = p.cost_basis_currency_id
}

####################################

query ETL = literal : getSchema Colimit -> Target {
 entity currency -> {
 		from
 			c:Ref_currency
		attributes
			code -> c.Ref_currency_code
			name -> c.Ref_currency_name
			id -> c.Ref_currency_id
			country_id -> c.Ref_currency_country_id
		foreign_keys
		 	currency_country -> {c -> c.Ref_currency_country}
}

entity strategy -> {
		from
			s:Ref_strategy
		attributes
			name -> s.Ref_strategy_name
			description -> s.Ref_strategy_description
			id -> s.Ref_strategy_id
	}

entity	asset -> {
		from
			a:Ref_asset
		attributes
			id -> a.Ref_asset_id
			description -> a.Ref_asset_description
			name -> a.Ref_asset_name
			strategy_id -> a.Ref_asset_strategy_id
		foreign_keys
		 	asset_strategy -> {s -> a.Ref_asset_strategy}
	}

entity	client -> {
		from
			c:Client_client
		attributes
			description -> c.Client_client_description
			name -> c.Client_client_name
			id -> c.Client_client_id
	}

entity	country -> {
		from
			c:Ref_country
		attributes
			code -> c.Ref_country_code
			name -> c.Ref_country_name
			id -> c.Ref_country_id
	}

entity	transaction -> {
		from
			t:Trans_transaction p:Portfolio_portfolio c:Client_client c1:Client_client
		where
			t.Trans_transaction_portfolio_id = p.Portfolio_portfolio_id
		attributes
			id -> t.Trans_transaction_id
			asset_id -> t.Trans_transaction_asset_id
			buy_sell_ind -> t.Trans_transaction_buy_sell_ind
			quantity -> t.Trans_transaction_quantity
			price -> t.Trans_transaction_price
			date -> t.Trans_transaction_date
			currency_id -> t.Trans_transaction_currency_id
			portfolio_id -> t.Trans_transaction_portfolio_id
		foreign_keys
			transaction_currency -> {c -> t.Trans_transaction_currency}
			transaction_asset -> {a -> t.Trans_transaction_asset}
			transaction_portfolio -> {p -> p} 	#NEW
	}

entity	position -> {
		from
			p:HoldPos_position a:Ref_asset cv:Ref_currency cb:Ref_currency
		where
			p.HoldPos_position_asset_id = a.Ref_asset_id
			p.HoldPos_position_current_value_currency_code = cv.Ref_currency_code
			p.HoldPos_position_cost_basis_currency_code = cb.Ref_currency_code
		attributes
			quantity -> p.HoldPos_position_quantity
			current_value -> p.HoldPos_position_current_value
			cost_basis -> p.HoldPos_position_cost_basis
			id -> p.HoldPos_position_id
			client_id -> p.HoldPos_position_client_no
			asset_id -> p.HoldPos_position_asset_id
			current_value_currency_id -> cv.Ref_currency_id
			cost_basis_currency_id -> cb.Ref_currency_id
 		foreign_keys
		 	position_client -> {c -> p.HoldPos_position_client}
			position_asset -> {a -> a}  #NEW
			position_current_value_currency -> {c -> cv}  #NEW
			position_cost_basis_currency -> {c -> cb}  #NEW
	}

	entity portfolio -> {
		from
			p:Portfolio_portfolio
		attributes
			parent_portfolio_id -> p.Portfolio_portfolio_parent_id #added
			name -> p.Portfolio_portfolio_name
			description -> p.Portfolio_portfolio_description
			id -> p.Portfolio_portfolio_id
			strategy_id -> p.Portfolio_portfolio_strategy_id
		foreign_keys
 			parent -> {p -> p.Portfolio_portfolio_parent}  #added
			portfolio_strategy -> {s -> p.Portfolio_portfolio_strategy}
	}

	entity holding -> {
		from
			h:HoldPos_holding c:Ref_currency a:Ref_asset
		where
			a.Ref_asset_id = h.HoldPos_holding_asset_id
			h.HoldPos_holding_currency_code = c.Ref_currency_code
		attributes
			id -> h.HoldPos_holding_id
			asset_id -> h.HoldPos_holding_asset_id
			currency_id -> c.Ref_currency_id
			purchase_price -> h.HoldPos_holding_purchase_price
			quantity -> h.HoldPos_holding_quantity
			purchase_date -> h.HoldPos_holding_purchase_date
			begin_date -> h.HoldPos_holding_begin_date
			end_date -> h.HoldPos_holding_end_date
		foreign_keys
 			holding_asset -> {a -> a}
			holding_currency -> {c -> c}
	}

entity	portfolioholding -> {
		from
			h:HoldPos_holding p:Portfolio_portfolio cur: Ref_currency c1:Client_client a:Ref_asset
		where
			a.Ref_asset_id = h.HoldPos_holding_asset_id
			p.Portfolio_portfolio_parent.Portfolio_portfolio_client_id = c1.Client_client_id
			c1.Client_client_id = h.HoldPos_holding_client.Client_client_id
			h.HoldPos_holding_currency_code = cur.Ref_currency_code
			p.Portfolio_portfolio_client_id = h.HoldPos_holding_client.Client_client_id
			h.HoldPos_holding_portfolio_id = p.Portfolio_portfolio_id
		attributes
			id -> keygen(h.HoldPos_holding_id, h.HoldPos_holding_client.Client_client_id, p.Portfolio_portfolio_id) #added by ryan
			holding_id -> h.HoldPos_holding_id
			client_id -> c1.Client_client_id
			portfolio_id -> p.Portfolio_portfolio_id
		foreign_keys
			portfolioholding_holding -> {h -> h c -> cur a -> a}
			portfolioholding_client -> {c -> h.HoldPos_holding_client}
			portfolioholding_portfolio -> {p -> p}
	}
}

instance Output = distinct eval ETL E
