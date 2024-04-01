package catdata.cql.gui;

public class SynchExample2 extends SynchExample {

	@Override
	public String getName() {
		return "Finance Oracle";
	}

	@Override public String getOptions() {
		return super.getOptions() + " oracle_schema_mode = true     jdbc_zero = \"\"";
	}
	
	@Override
	public String getSources() {
return """
		
schema Client = import_jdbc_all "jdbc:oracle:thin:@//conexus-oracle.cdjgg9tjdgyz.us-east-1.rds.amazonaws.com:1521/ORCL?user=Client&password=password"
instance ClientInstance2 = import_jdbc_direct "jdbc:oracle:thin:@//conexus-oracle.cdjgg9tjdgyz.us-east-1.rds.amazonaws.com:1521/ORCL?user=Client&password=password"
 "ROW_NUMBER() OVER (Order by (0))" : Client  
instance ClientInstance = skolem ClientInstance2

schema Ref = import_jdbc_all "jdbc:oracle:thin:@//conexus-oracle.cdjgg9tjdgyz.us-east-1.rds.amazonaws.com:1521/ORCL?user=Ref&password=password"
instance RefInstance2 = import_jdbc_direct "jdbc:oracle:thin:@//conexus-oracle.cdjgg9tjdgyz.us-east-1.rds.amazonaws.com:1521/ORCL?user=Ref&password=password"
 "ROW_NUMBER() OVER (Order by (0))" : Ref  
instance RefInstance = skolem RefInstance2

schema Trans = import_jdbc_all "jdbc:oracle:thin:@//conexus-oracle.cdjgg9tjdgyz.us-east-1.rds.amazonaws.com:1521/ORCL?user=Trans&password=password"
instance TransInstance2 = import_jdbc_direct "jdbc:oracle:thin:@//conexus-oracle.cdjgg9tjdgyz.us-east-1.rds.amazonaws.com:1521/ORCL?user=Trans&password=password"
 "ROW_NUMBER() OVER (Order by (0))" : Trans  
instance TransInstance = skolem TransInstance2

schema HoldPos = import_jdbc_all "jdbc:oracle:thin:@//conexus-oracle.cdjgg9tjdgyz.us-east-1.rds.amazonaws.com:1521/ORCL?user=HoldPos&password=password"
instance HoldPosInstance2 = import_jdbc_direct "jdbc:oracle:thin:@//conexus-oracle.cdjgg9tjdgyz.us-east-1.rds.amazonaws.com:1521/ORCL?user=HoldPos&password=password"
 "ROW_NUMBER() OVER (Order by (0))" : HoldPos  
instance HoldPosInstance = skolem HoldPosInstance2 

schema Portfolio = import_jdbc_all "jdbc:oracle:thin:@//conexus-oracle.cdjgg9tjdgyz.us-east-1.rds.amazonaws.com:1521/ORCL?user=Portfolio&password=password"
instance PortfolioInstance2 = import_jdbc_direct "jdbc:oracle:thin:@//conexus-oracle.cdjgg9tjdgyz.us-east-1.rds.amazonaws.com:1521/ORCL?user=Portfolio&password=password"
 "ROW_NUMBER() OVER (Order by (0))" : Portfolio  
instance PortfolioInstance = skolem PortfolioInstance2

constraints ClientConstraints = literal : Client {
						forall x y : "CLIENT.CLIENT" where x.ID = y.ID -> where x = y
					}
					
					constraints PortfolioConstraints = literal : Portfolio {
						forall x y : "PORTFOLIO.STRATEGY"  where x.ID = y.ID -> where x = y
						forall x y : "PORTFOLIO.PORTFOLIO" where x.ID = y.ID -> where x = y
						#forall	s : "PORTFOLIO.PORTFOLIO" -> exists t:"PORTFOLIO.PORTFOLIO" where s.PARENT_ID = t.ID
						forall	s : "PORTFOLIO.PORTFOLIO" -> exists t:"PORTFOLIO.STRATEGY" where s.STRATEGY_ID = t.ID
					}
					
					constraints RefConstraints = literal : Ref {
						forall x y : "REF.COUNTRY"  where x.ID = y.ID -> where x = y
						forall x y : "REF.CURRENCY" where x.ID = y.ID -> where x = y
						forall x y : "REF.ASSET"    where x.ID = y.ID -> where x = y
						forall x y : "REF.STRATEGY" where x.ID = y.ID -> where x = y
						forall s : "REF.ASSET" -> exists t:"REF.STRATEGY" where s.STRATEGY_ID = t.ID
						forall s : "REF.CURRENCY" -> exists t:"REF.COUNTRY" where s.COUNTRY_ID = t.ID
					}
					
					constraints TransConstraints = literal : Trans {
						forall x y : "TRANS.ASSET"  where x.ID = y.ID -> where x = y
						forall x y : "TRANS.CURRENCY" where x.ID = y.ID -> where x = y
						forall x y : "TRANS.transaction"    where x.ID = y.ID -> where x = y
						forall s:"TRANS.transaction" -> exists t:"TRANS.ASSET" where s.ASSET_ID = t.ID
						forall s:"TRANS.transaction" -> exists t:"TRANS.CURRENCY" where s.CURRENCY_ID = t.ID
					}
					
					constraints HoldPosConstraints = literal : HoldPos {
						forall x y : "HOLDPOS.CLIENT"   where x.NO = y.NO -> where x = y
						forall x y : "HOLDPOS.HOLDING"  where x.ID = y.ID -> where x = y
						forall x y : "HOLDPOS.position" where x.ID = y.ID -> where x = y
						forall s:"HOLDPOS.position" -> exists t:"HOLDPOS.CLIENT" where s.CLIENT_NO = t.NO
						forall s:"HOLDPOS.HOLDING" -> exists t:"HOLDPOS.CLIENT" where s.CLIENT_NO = t.NO
					}
					

		""";
	}
	
	@Override
	public String getLinks() {
		return """
				entity_isomorphisms
					client_Client_to_HoldPos : Client."CLIENT.CLIENT" -> HoldPos."HOLDPOS.CLIENT"
					asset_Ref_to_Trans : Ref."REF.ASSET"     -> Trans."TRANS.ASSET"
					currency_Ref_to_Trans: Ref."REF.CURRENCY"  -> Trans."TRANS.CURRENCY"
					strategy_Ref_to_Portfolio : Ref."REF.STRATEGY"  -> Portfolio."PORTFOLIO.STRATEGY"
				equations
					e1: forall x:Client."CLIENT.CLIENT", x.ID          = x.client_Client_to_HoldPos.NO
					e2: forall x:Client."CLIENT.CLIENT", x.NAME        = x.client_Client_to_HoldPos.NM
					e3: forall x:Client."CLIENT.CLIENT", x.DESCRIPTION = x.client_Client_to_HoldPos.desc

					e4: forall x:Ref."REF.STRATEGY", x.ID          = x.strategy_Ref_to_Portfolio.ID
					e5: forall x:Ref."REF.STRATEGY", x.NAME        = x.strategy_Ref_to_Portfolio.NAME
					e6: forall x:Ref."REF.STRATEGY", x.DESCRIPTION = x.strategy_Ref_to_Portfolio.DESCRIPTION

					e7: forall x:Ref."REF.ASSET", x.ID          = x.asset_Ref_to_Trans.ID
					e8: forall x:Ref."REF.ASSET", x.NAME        = x.asset_Ref_to_Trans.NAME
					e9: forall x:Ref."REF.ASSET", x.DESCRIPTION = x.asset_Ref_to_Trans.DESCRIPTION

					e10: forall x:Ref."REF.CURRENCY", x.ID   = x.currency_Ref_to_Trans.ID
					e11: forall x:Ref."REF.CURRENCY", x.CODE = x.currency_Ref_to_Trans.CODE
					e12: forall x:Ref."REF.CURRENCY", x.NAME = x.currency_Ref_to_Trans.NAME
				constraints												""";
	}
	
	@Override
	public String getTargets() {
		return "";
	
	}
}
