package catdata.cql.gui;

import catdata.cql.gui.WarehouseUI.Example;

public class ExcelExample extends Example {
	
	public ExcelExample(String sources, String links) {
		this.sources = sources;
		this.links = links;
	}

	private String sources, links;
	
	@Override
	public String getOptions() {
		return "options\r\n"
				+ "require_consistency=false\r\n"
				+ "dont_validate_unsafe=true\r\n"
				+ "prover_simplify_max = 512\r\n"
				+ "timeout = 480\r\n";
	}
			
	@Override
	public String getName() {
		return "Excel";
	}
	
	public static String getTy() {
		  return "typeside Ty = literal {\r\n"
				+ "	external_types\r\n"
				+ "		Integer -> \"java.lang.Integer\"\r\n"
				+ "		Float -> \"java.lang.Double\"\r\n"
				+ "		String -> \"java.lang.String\"\r\n"
				+ "	external_parsers \r\n"
				+ "		Float -> \"x => java.lang.Double.parseDouble(x)\"\r\n"
				+ "		Integer -> \"x => java.lang.Integer.parseInt(x)\"\r\n"
				+ "		String -> \"x => x\"\r\n"
				+ "	external_functions\r\n"
				+ "		\"+\" : Float,Float->Float = \"(x, y) => x + y\"	\r\n"
				+ "		\"*\" : Float,Float->Float = \"(x, y) => x * y\"	\r\n"
				+ "		\"-\" : Float,Float->Float = \"(x, y) => x - y\"	\r\n"
				+ "		\"/\" : Float,Float->Float = \"(x, y) => x / y\"	\r\n"
				+ "		\"MAX\" : Float,Float->Float = \"(x, y) => java.lang.Math.max(x,y)\"\r\n"
				+ "		\"MIN\" : Float,Float->Float = \"(x, y) => java.lang.Math.min(x,y)\"		\r\n"
				+ "}"; 
/*		return "\r\n"
				+ "typeside Ty = literal {\r\n"
				+ "	functions\r\n"
				+ "		castFloatString : Float->String\r\n"
				+ "\r\n"
				+ "	external_types\r\n"
				+ "		Integer -> \"java.lang.Integer\"\r\n"
				+ "		Float -> \"java.math.BigDecimal\"\r\n"
				+ "		String -> \"java.lang.String\"\r\n"
				+ "	external_parsers \r\n"
				+ "		Float -> \"x => java.math.BigDecimal.valueOf(java.lang.Double.parseDouble(x))\"\r\n"
				+ "		Integer -> \"x => java.lang.Integer.parseInt(x)\"\r\n"
				+ "		String -> \"x => x\"\r\n"
				+ "	external_functions\r\n"
				+ "		\"+\" : Float,Float->Float = \"(x, y) => x.add(y)\"	\r\n"
				+ "		\"*\" : Float,Float->Float = \"(x, y) => x.multiply(y)\"	\r\n"
				+ "		\"-\" : Float,Float->Float = \"(x, y) => x.subtract(y)\"	\r\n"
				+ "		\"/\" : Float,Float->Float = \"(x, y) => x.divide(y)\"	\r\n"
				+ "		\"MAX\" : Float,Float->Float = \"(x, y) => x.max(y)\"\r\n"
				+ "		\"MIN\" : Float,Float->Float = \"(x, y) => x.min(y)\"		\r\n"
				+ "	equations\r\n"
				+ "		\r\n"
				+ "}		"; */
	}

	@Override
	public String getSources() {
		return getTy() + "\r\n" + sources;
	
		
	}

	@Override
	public String getLinks() {
		return links;
	}

	@Override
	public String getTargets() {
		return "";
	}
	
}