options
	always_reload = true

typeside Sql = literal {
	imports sql
	external_functions
		stringToInt : String -> Integer =
		 "x => { if (!x.isEmpty()) { return java.util.Optional.of(java.lang.Integer.parseInt(x.get())) } 
		         else { return java.util.Optional.empty() } }"
}

schema S0 = literal : Sql {
	entities
		E
	attributes
		att : E -> Integer
}

#  Person.csv is the file
#   pId
#   0
#   1
#   2
#
#   Employee.csv is the file
#    eId,is
#    10,0
#    11,1
#    12,2

command createCsvData = exec_js {
	"Java.type(\"catdata.Util\").writeFile(\"pId\\n0\\n1\\n2\", \"Person.csv\")"
	"Java.type(\"catdata.Util\").writeFile(\"eId,is\\n10,0\\n11,1\\n12,2\", \"Employee.csv\")"
}

instance I0 = import_csv "." : Sql 

mapping F = literal : S0 -> schemaOf I0 {
	entity
		E -> Employee
	attributes
		att -> lambda e. stringToInt(e.is)
}
query Q = literal : schemaOf I0 -> S0 {
	entity 
		E -> {from e:Employee attributes att->stringToInt(e.is)}
}

instance I = delta F I0
instance J = eval Q I0

command exportCsvData = export_csv_instance I0 "exported"
command exportCsvData2 = export_csv_transform (identity I0) "exported_trans.csv"
