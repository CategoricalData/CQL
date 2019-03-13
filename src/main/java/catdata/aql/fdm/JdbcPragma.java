package catdata.aql.fdm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import catdata.Util;
import catdata.aql.Pragma;

public class JdbcPragma extends Pragma {

	private List<String> sqls = new LinkedList<>();
	
	private final List<String> responses = new LinkedList<>();
	
	private final String jdbcString;
	
	
	public JdbcPragma(String jdbcString, List<String> sqls) {
		this.sqls = sqls;
		this.jdbcString = jdbcString;
		
	}


	@Override
	public void execute() {
		try (Connection conn = DriverManager.getConnection(jdbcString)) {
			List<String> ret = new LinkedList<>();
			for (String sql : sqls) {
				try (Statement stmt = conn.createStatement()) {
					ret.add("START");
					ret.add(sql);
					ret.add("");
					boolean hasMoreResultSets = stmt.execute(sql);
				    while (hasMoreResultSets || stmt.getUpdateCount() != -1) {  
				        if (hasMoreResultSets) {  
				            try (ResultSet rs = stmt.getResultSet()) {
					            ret.add(print(rs));
				            } catch (SQLException e) {
								e.printStackTrace();
								throw new RuntimeException(e);
							}    
				        } else { // if ddl/dml/...
				            int queryResult = stmt.getUpdateCount();  
				            ret.add("Updated " + queryResult + " rows.");
				        } 
				        hasMoreResultSets = stmt.getMoreResults();  
				    }
					ret.add("END");
					ret.add("");
				}  catch (SQLException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} 
			}
			responses.add(Util.sep(ret, "\n"));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
	
		
	}
	
	private static String print(ResultSet rs) throws SQLException {		
		 ResultSetMetaData rsmd = rs.getMetaData();
		 int columnsNumber = rsmd.getColumnCount();
		 List<String> x = new LinkedList<>();
		 while (rs.next()) {
			 String ret = "";
			 for (int i = 1; i <= columnsNumber; i++) { //wow, this starts at 1, apparently
				 if (i > 1) {
					ret += ",  ";
				 }
				 String columnValue = rs.getString(i);
		         ret += rsmd.getColumnName(i) + " " + columnValue;
			 }
			 x.add(ret);
		}
		return Util.sep(x, "\n");
	}
		
	@Override
	public String toString() {
		return Util.sep(responses, "\n\n--------------\n\n");
	}
		
}
