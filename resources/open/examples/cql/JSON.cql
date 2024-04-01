
command createJsonData = exec_js {
	"Java.type(\"catdata.Util\").writeFile(\" {  \\\"glossary\\\": {\\r\\n        \\\"title\\\": \\\"example glossary\\\",\\r\\n\\t\\t\\\"GlossDiv\\\": {\\r\\n            \\\"title\\\": \\\"S\\\",\\r\\n\\t\\t\\t\\\"GlossList\\\": {\\r\\n                \\\"GlossEntry\\\": {\\r\\n                    \\\"ID\\\": \\\"SGML\\\",\\r\\n\\t\\t\\t\\t\\t\\\"SortAs\\\": \\\"SGML\\\",\\r\\n\\t\\t\\t\\t\\t\\\"GlossTerm\\\": \\\"Standard Generalized Markup Language\\\",\\r\\n\\t\\t\\t\\t\\t\\\"Acronym\\\": \\\"SGML\\\",\\r\\n\\t\\t\\t\\t\\t\\\"Abbrev\\\": \\\"ISO 8879:1986\\\",\\r\\n\\t\\t\\t\\t\\t\\\"GlossDef\\\": {\\r\\n                        \\\"para\\\": \\\"A meta-markup language, used to create markup languages such as DocBook.\\\",\\r\\n\\t\\t\\t\\t\\t\\t\\\"GlossSeeAlso\\\": [\\\"GML\\\", \\\"XML\\\"]\\r\\n                    },\\r\\n\\t\\t\\t\\t\\t\\\"GlossSee\\\": \\\"markup\\\"\\r\\n                }\\r\\n            }\\r\\n        }\\r\\n    } \\r\\n} \", \"temp.json\")" 
}

instance I = import_json_ld_all "temp.json"

instance K = spanify I

command out = export_json_instance I "exported.json"

