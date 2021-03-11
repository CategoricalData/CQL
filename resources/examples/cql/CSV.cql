options
	always_reload = true

schema S0 = literal : sql {
	entities
		Employee
		Person
	foreign_keys
		eAsP : Employee -> Person
	attributes
		ssn : Person -> Integer
		eId : Employee -> Integer
		eSsn  : Employee -> Integer
	observation_equations
		eSsn = eAsP . ssn
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

instance I0 = import_csv "." : S0 {
	Employee -> {Employee -> eId   eAsP -> is    eSsn -> is}
	#eId -> eId can be ommitted

	Person -> {Person -> pId  ssn -> pId}
}

command exportCsvData = export_csv_instance I0 "exported"
command exportCsvData2 = export_csv_transform (identity I0) "exported_trans.csv"
