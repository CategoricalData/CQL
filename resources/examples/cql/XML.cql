
command createXmlData = exec_js {
"Java.type(\"catdata.Util\").writeFile(\" <data xmlns=\\\"http://example.org\\\">\\n    <item>\\n        <name>Hello</name>\\n    </item>\\n    <item>\\n        <name>Hello</name>\\n    </item>\\n</data> \", \"tempX.xml\")" 
}

instance I = import_xml_all "tempX.xml"


command exportXmlData = export_rdf_instance_xml I "fileO.xml" {
	external_types
		Integer -> "http://www.w3.org/2001/XMLSchema#Integer" "x => x.toString()"
}
