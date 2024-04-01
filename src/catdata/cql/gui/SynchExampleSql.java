package catdata.cql.gui;

public class SynchExampleSql extends SynchExample {

	@Override
	public String getName() {
		return "Finance MS SQL";
	}

	@Override
	public String getSources() {
return """
		
schema MsCatalog = ms_catalog sql Other #schema for microsoft sql server catalogs

schema Client = import_jdbc_all "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Client;user=admin;password=$mk4bksA2TARKy$w;" 
instance ClientInstance2 = import_jdbc_direct "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Client;user=admin;password=$mk4bksA2TARKy$w;"
 "ROW_NUMBER() OVER (ORDER BY CQL_ZERO)" : Client  
constraints ClientConstraints = from_ms_catalog import_jdbc_direct "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Client;user=admin;password=$mk4bksA2TARKy$w;" 
 "ROW_NUMBER() OVER (ORDER BY CQL_ZERO)" : MsCatalog { options import_sql_direct_prefix = "Client.INFORMATION_SCHEMA." } : Client
instance ClientInstance = skolem ClientInstance2


schema Ref = import_jdbc_all "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Ref;user=admin;password=$mk4bksA2TARKy$w;" 
instance RefInstance2 = import_jdbc_direct "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Ref;user=admin;password=$mk4bksA2TARKy$w;"
 "ROW_NUMBER() OVER (ORDER BY CQL_ZERO)" : Ref  
constraints RefConstraints = from_ms_catalog import_jdbc_direct "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Ref;user=admin;password=$mk4bksA2TARKy$w;" 
 "ROW_NUMBER() OVER (ORDER BY CQL_ZERO)" : MsCatalog { options import_sql_direct_prefix = "Ref.INFORMATION_SCHEMA." } : Ref
instance RefInstance = skolem RefInstance2

schema HoldPos = import_jdbc_all "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=HoldPos;user=admin;password=$mk4bksA2TARKy$w;" 
instance HoldPosInstance2 = import_jdbc_direct "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=HoldPos;user=admin;password=$mk4bksA2TARKy$w;"
 "ROW_NUMBER() OVER (ORDER BY CQL_ZERO)" : HoldPos  
constraints HoldPosConstraints = from_ms_catalog import_jdbc_direct "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=HoldPos;user=admin;password=$mk4bksA2TARKy$w;" 
 "ROW_NUMBER() OVER (ORDER BY CQL_ZERO)" : MsCatalog { options import_sql_direct_prefix = "HoldPos.INFORMATION_SCHEMA." } : HoldPos
instance HoldPosInstance = skolem HoldPosInstance2

schema Portfolio = import_jdbc_all "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Portfolio;user=admin;password=$mk4bksA2TARKy$w;" 
instance PortfolioInstance2 = import_jdbc_direct "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Portfolio;user=admin;password=$mk4bksA2TARKy$w;"
 "ROW_NUMBER() OVER (ORDER BY CQL_ZERO)" : Portfolio  
constraints PortfolioConstraints = from_ms_catalog import_jdbc_direct "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Portfolio;user=admin;password=$mk4bksA2TARKy$w;" 
 "ROW_NUMBER() OVER (ORDER BY CQL_ZERO)" : MsCatalog { options import_sql_direct_prefix = "Portfolio.INFORMATION_SCHEMA." } : Portfolio
instance PortfolioInstance = skolem PortfolioInstance2

schema Trans = import_jdbc_all "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Trans;user=admin;password=$mk4bksA2TARKy$w;" 
instance TransInstance2 = import_jdbc_direct "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Trans;user=admin;password=$mk4bksA2TARKy$w;"
 "ROW_NUMBER() OVER (ORDER BY CQL_ZERO)" : Trans  
constraints TransConstraints = from_ms_catalog import_jdbc_direct "jdbc:sqlserver://conexus-demo-prime.cgvq3ryshzkp.us-east-1.rds.amazonaws.com:1433;databaseName=Trans;user=admin;password=$mk4bksA2TARKy$w;" 
 "ROW_NUMBER() OVER (ORDER BY CQL_ZERO)" : MsCatalog { options import_sql_direct_prefix = "Trans.INFORMATION_SCHEMA." } : Trans
instance TransInstance = skolem TransInstance2
		""";
	}
}
