package catdata.cql.gui;

import catdata.cql.gui.WarehouseUI.Example;

public class SynchExample extends Example {

		@Override
		public String getName() {
			return "Finance 1";
		}

		@Override
		public String getSources() {
			return """
					schema Client = literal : sql {
						entities
							client
						attributes
							id : client -> Integer
							name description : client -> Varchar
					}
					constraints ClientConstraints = literal : Client {
						forall x y : client where x.id = y.id -> where x = y
					}
					instance ClientInstance = literal : Client {
					     generators
					     	cc1 cc2 cc3 : client
					     multi_equations
					     	id -> {cc1 1, cc2 2, cc3 3}
					     	name -> {cc1 Tom, cc2 Dick, cc3 Harry}
					     	description -> {cc1 "Tom Client", cc2 "Dick Client", cc3 "Harry Client"}
					}
					command check_client = check ClientConstraints ClientInstance

					####################################

					schema Portfolio = literal : sql {
						entities
							strategy
							portfolio
						attributes
							id : strategy -> Integer
							name description : strategy -> Varchar
							id : portfolio -> Integer
							name description : portfolio -> Varchar
							client_id strategy_id parent_id : portfolio -> Integer
					}
					constraints PortfolioConstraints = literal : Portfolio {
						forall x y : strategy  where x.id = y.id -> where x = y
						forall x y : portfolio where x.id = y.id -> where x = y
						#forall	s:portfolio -> exists t:portfolio where s.parent_id = t.id
						forall	s:portfolio -> exists t:strategy where s.strategy_id = t.id
					}
					instance PortfolioInstance = literal : Portfolio {
					    generators
					     	ps1 ps2 ps3 : strategy
					     	pp1 pp2 pp3 : portfolio
						multi_equations
							parent_id -> {pp1 1, pp2 2, pp3 2}
							id -> {ps1 1, ps2 2, ps3 3, pp1 1, pp2 2, pp3 3}
							name -> {ps1 Strat1, ps2 Strat2, ps3 Strat3, pp1 Port1, pp2 Port2, pp3 Port3}
							description -> {ps1 Strategy1, ps2 Strategy2, ps3 Strategy3, pp1 Portfolio1, pp2 Portfolio2, pp3 Portfolio3}
							strategy_id -> {pp1 2, pp2 2, pp3 1}
							client_id -> {pp1 1, pp2 2, pp3 2}
					}
					command check_portfolio = check PortfolioConstraints PortfolioInstance

					####################################

					schema Ref = literal : sql {
						entities
							country
							currency
							asset
							strategy
						attributes
							id : country -> Integer
							code name : country -> Varchar
							id strategy_id : asset -> Integer
							name description : asset -> Varchar
							id : strategy -> Integer
							name description : strategy -> Varchar
							id country_id : currency -> Integer
							code name : currency -> Varchar
					}
					constraints RefConstraints = literal : Ref {
						forall x y : country  where x.id = y.id -> where x = y
						forall x y : currency where x.id = y.id -> where x = y
						forall x y : asset    where x.id = y.id -> where x = y
						forall x y : strategy where x.id = y.id -> where x = y
						forall s:asset -> exists t:strategy where s.strategy_id = t.id
						forall s:currency -> exists t:country where s.country_id = t.id
					}
					instance RefInstance = literal : Ref {
					     generators
					     	rcty1 rcty2 rcty3 rcty4 rcty5 rcty6 : country
							rcur1 rcur2 rcur3 : currency
							ra1 ra2 ra3 ra4 ra5 ra6 ra7 ra8 ra9 ra10 : asset
							rs1 rs2 rs3 rs4 rs5 : strategy
						multi_equations
							country_id  -> {rcur1 6, rcur2 4, rcur3 5}
							strategy_id  -> {ra1 1, ra2 3, ra3 2, ra4 4, ra5 5, ra6 4, ra7 4, ra8 5, ra9 3, ra10 1}
							id  -> {rcty1 1, rcty2 2, rcty3 3, rcty4 4, rcty5 5, rcty6 6, rcur1 1, rcur2 2, rcur3 3, ra1 1, ra2 2, ra3 3, ra4 4, ra5 5, ra6 6, ra7 7, ra8 8, ra9 9, ra10 10, rs1 1, rs2 2, rs3 3, rs4 4, rs5 5}
							code  -> {rcty1 AU, rcty2 CH, rcty3 CN, rcty4 EU, rcty5 JP, rcty6 US, rcur1 USD, rcur2 EUR, rcur3 JPY}
							name  -> {ra1 A1, ra2 A2, ra3 A3, ra4 A4, ra5 A5, ra6 A6, ra7 A7, ra8 A8, ra9 A9, ra10 A10, rs1 Strat1, rs2 Strat2, rs3 Strat3, rs4 Strat4, rs5 Strat5, rcty1 Australia, rcty2 Switzerland, rcty3 China, rcty4 "European Union", rcty5 Japan, rcty6 "United States of America", rcur1 "US Dollar", rcur2 "Euro", rcur3 "Japanese Yen" }
							description  -> {ra1 Asset1, ra2 Asset2, ra3 Asset3, ra4 Asset4, ra5 Asset5, ra6 Asset6, ra7 Asset7, ra8 Asset8, ra9 Asset9, ra10 Asset10, rs1 Strategy1, rs2 Strategy2, rs3 Strategy3, rs4 Strategy4, rs5 Strategy5}
					}

					####################################

					schema Trans = literal : sql {
						entities
							asset
							currency
							transaction
						attributes
							id : asset -> Integer
							name description : asset -> Varchar
							id : currency -> Integer
							code name : currency -> Varchar
							id asset_id portfolio_id quantity currency_id : transaction -> Integer
							date  : transaction -> Date	# added as a new column
							buy_sell_ind : transaction -> Varchar
							price : transaction -> Real
					}
					constraints TransConstraints = literal : Trans {
						forall x y : asset  where x.id = y.id -> where x = y
						forall x y : currency where x.id = y.id -> where x = y
						forall x y : transaction    where x.id = y.id -> where x = y
						forall s:transaction -> exists t:asset where s.asset_id = t.id
						forall s:transaction -> exists t:currency where s.currency_id = t.id
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
							asset_id -> {tt1 1, tt2 1, tt3 3, tt4 3, tt5 5, tt6 5}
							portfolio_id -> {tt1 1, tt2 1, tt3 2, tt4 2, tt5 3, tt6 3}
							buy_sell_ind -> {tt1 buy, tt2 sell, tt3 buy, tt4 sell, tt5 buy, tt6 sell}
							quantity -> {tt1 200, tt2 100, tt3 150, tt4 150, tt5 200, tt6 100}
							price -> {tt1 "5.51", tt2 "5.5", tt3 "2.5", tt4 "3.5", tt5 "1.6", tt6 "1.5"}
							date -> {tt1 "1/1/2011", tt2 "6/1/2011", tt3 "9/1/2013", tt4 "3/1/2014", tt5 "2/1/2013", tt6 "7/1/2013"}
							currency_id -> {tt1 2, tt2 2, tt3 1, tt4 1, tt5 3, tt6 3}
					}

					####################################

					schema HoldPos = literal : sql {
						entities
							client
							holding
							position
						attributes
							no : client -> Integer
							nm desc : client -> Varchar
							id client_no portfolio_id asset_id quantity : holding -> Integer
							purchase_date begin_date end_date : holding -> Date
							purchase_price : holding -> Real
							currency_code : holding -> Varchar
							id client_no asset_id quantity current_value cost_basis : position -> Integer
							current_value_currency_code cost_basis_currency_code : position -> Varchar
					}
					constraints HoldPosConstraints = literal : HoldPos {
						forall x y : client   where x.no = y.no -> where x = y
						forall x y : holding  where x.id = y.id -> where x = y
						forall x y : position where x.id = y.id -> where x = y
						forall s:position -> exists t:client where s.client_no = t.no
						forall s:holding -> exists t:client where s.client_no = t.no
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

										""";
		}

		@Override
		public String getLinks() {
			return """
					entity_isomorphisms
						client_Client_to_HoldPos : Client.client -> HoldPos.client
						asset_Ref_to_Trans : Ref.asset     -> Trans.asset
						currency_Ref_to_Trans: Ref.currency  -> Trans.currency
						strategy_Ref_to_Portfolio : Ref.strategy  -> Portfolio.strategy
					equations
						e1: forall x:Client.client, x.id          = x.client_Client_to_HoldPos.no
						e2: forall x:Client.client, x.name        = x.client_Client_to_HoldPos.nm
						e3: forall x:Client.client, x.description = x.client_Client_to_HoldPos.desc

						e4: forall x:Ref.strategy, x.id          = x.strategy_Ref_to_Portfolio.id
						e5: forall x:Ref.strategy, x.name        = x.strategy_Ref_to_Portfolio.name
						e6: forall x:Ref.strategy, x.description = x.strategy_Ref_to_Portfolio.description

						e7: forall x:Ref.asset, x.id          = x.asset_Ref_to_Trans.id
						e8: forall x:Ref.asset, x.name        = x.asset_Ref_to_Trans.name
						e9: forall x:Ref.asset, x.description = x.asset_Ref_to_Trans.description

						e10: forall x:Ref.currency, x.id   = x.currency_Ref_to_Trans.id
						e11: forall x:Ref.currency, x.code = x.currency_Ref_to_Trans.code
						e12: forall x:Ref.currency, x.name = x.currency_Ref_to_Trans.name
					constraints												""";
		}

		@Override
		public String getTargets() {
			return """
										schema Denormalized = literal : sql {
											entities
												asset client country currency portfolio portfolioholding position strategy transaction
											attributes
												id : country -> Integer
												code name : country -> Varchar

												id strategy_id parent_id : portfolio -> Integer
												name description : portfolio -> Varchar

												id asset_id portfolio_id currency_id quantity : transaction -> Integer
												asset_name portfolio_name buy_sell_ind : transaction -> Varchar
												date  : transaction -> Date
												price : transaction -> Real

												begin_date end_date purchase_date : portfolioholding -> Date
												portfolio_id client_id asset_id quantity : portfolioholding -> Integer
												portfolio_name client_name asset_name purchase_price_currency_code : portfolioholding -> Varchar
												purchase_price : portfolioholding -> Real

												id country_id : currency -> Integer
												code name : currency -> Varchar

												id : strategy -> Integer
												name description : strategy -> Varchar

												id client_id asset_id current_value quantity : position -> Integer
												client_name asset_name current_value_currency_code : position -> Varchar

												id : client -> Integer
												name description : client -> Varchar

												id strategy_id : asset -> Integer
												name description : asset -> Varchar
										}

										constraints DenormalizedConstraints = literal : Denormalized {
											forall x y:transaction where x.id = y.id -> where x = y
											forall x y:strategy where x.id = y.id -> where x = y
											forall x y:currency where x.id = y.id -> where x = y
											forall x y:client where x.id = y.id -> where x = y
											forall x y:portfolio where x.id = y.id -> where x = y
											forall x y:position where x.id = y.id -> where x = y
											forall x y:asset where x.id = y.id -> where x = y
											forall x y:country where x.id = y.id -> where x = y

											forall s:portfolioholding -> exists t:portfolio where s.portfolio_id = t.id
											forall s:portfolioholding -> exists t:client where s.client_id = t.id
											forall s:portfolioholding -> exists t:asset where s.asset_id = t.id
											forall s:transaction ->	exists t:asset where s.asset_id = t.id
											forall s:position -> exists t:client where s.client_id = t.id
											forall s:transaction ->	exists t:currency where	s.currency_id = t.id
											forall s:position -> exists t:asset where s.asset_id = t.id
											forall s:portfolio -> exists t:strategy where s.strategy_id = t.id
										#	forall s:portfolio -> exists t:portfolio where s.parent_id = t.id
											forall s:currency -> exists t:country where s.country_id = t.id
											forall s:asset -> exists t:strategy where s.strategy_id = t.id
										}

										query DenormalizedQuery = literal : getSchema Warehouse -> Denormalized {
										entity	client -> {
							from
								c:Client_client
							attributes
								description -> c.description
								name -> c.name
								id -> c.id
						}

						entity currency -> {
					 		from
					 			c:Ref_currency
							attributes
								code -> c.code
								name -> c.name
								id -> c.id
								country_id -> c.country_id
						}

						entity strategy -> {
							from
								s:Ref_strategy #vs
							attributes
								name -> s.name
								description -> s.description
								id -> s.id
						}

						entity	asset -> {
							from
								a:Ref_asset
							attributes
								id -> a.id
								description -> a.description
								name -> a.name
								strategy_id -> a.strategy_id
						}

						entity	country -> {
							from
								c:Ref_country
							attributes
								code -> c.code
								name -> c.name
								id -> c.id
						}

						entity portfolio -> {
							from
								p : Portfolio_portfolio
							attributes
								parent_id -> p.parent_id
								name -> p.name
								description -> p.description
								id -> p.id
								strategy_id -> p.strategy_id
						}

						entity	transaction -> {
							from
								t:Trans_transaction
								a:Ref_asset
								p:Portfolio_portfolio
							where
								a.id = t.asset_id
								p.id = t.portfolio_id
							attributes
								id -> t.id
								asset_id -> t.asset_id
								asset_name -> a.name
								buy_sell_ind -> t.buy_sell_ind
								quantity -> t.quantity
								price -> t.price
								portfolio_name -> p.name
								date -> t.date
								currency_id -> t.currency_id
								portfolio_id -> t.portfolio_id
						}

						entity	position -> {
							from
								p:HoldPos_position
								a:Ref_asset
								c:Client_client
							where
								p.asset_id=a.id
								c.id=p.client_no
							attributes
								quantity -> p.quantity
								current_value -> p.current_value
								id -> p.id
								client_id -> p.client_no
								client_name -> c.name
								asset_id -> p.asset_id

								asset_name -> a.name
								current_value_currency_code -> p.current_value_currency_code
						}

					entity	portfolioholding -> {
							from
								h:HoldPos_holding p:Portfolio_portfolio a:Ref_asset c:Client_client
							where
								h.portfolio_id = p.id	h.asset_id=a.id
								c.id=p.client_id
							attributes
								purchase_price_currency_code -> h.currency_code
								portfolio_name -> p.name
								asset_id -> a.id
								asset_name -> a.name
								quantity->h.quantity
								purchase_date->h.purchase_date
								purchase_price->h.purchase_price
								client_id -> p.client_id
								client_name -> c.name
								portfolio_id -> p.id
								begin_date -> h.begin_date
								end_date -> h.end_date
						}
					}
					
#####################

schema Agg = literal : sql {
	entities
		Sum
	attributes
		c_name : Sum -> Varchar
		value : Sum -> Real	
}
constraints AggConstraints = literal : Agg {
}
query AggQuery = literal : getSchema Warehouse -> Agg {
	entity Sum -> {from t1:Trans_currency
  			attributes 
  			c_name -> t1.name
  			value -> from t2:Trans_transaction
				where t2.currency_id = t1.id
  	    		return t2.price
  	    		aggregate 0@Real lambda arg1 arg2. "+"(arg1,arg2)
	}
}
										""";
		}

	}