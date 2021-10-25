var prefixes = "@prefix rml:   <http://semweb.mmlab.be/ns/rml#> .\n" +
"@prefix rr:    <http://www.w3.org/ns/r2rml#> .\n" +
"@prefix dct:   <http://purl.org/dc/terms/> .\n" +
"@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
"@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
"@prefix fno:   <https://w3id.org/function/ontology#> .\n" +
"@prefix fnml:  <http://semweb.mmlab.be/ns/fnml#> .\n" +
"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
"@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
"@prefix ql:    <http://semweb.mmlab.be/ns/ql#> .\n" +
"@prefix rks:   <http://localhost:7280/ontology/rks/> .\n" +
"@prefix dc:    <http://purl.org/dc/elements/1.1/> .\n" +
"@prefix ex:    <http://example.org/> .\n" +
"@prefix :    <http://www.dfki.uni-kl.de/~mschroeder/demo/excel-rml#> .\n" +
"@prefix ss:    <http://www.dfki.uni-kl.de/~mschroeder/ld/ss#> .\n\n";

var matchExamples = [
    ["@prefix foaf: <http://xmlns.com/foaf/0.1/>\n" +
    "\n" +
    "<https://www.dfki.uni-kl.de/~mschroeder> a foaf:Person ;\n" +
    "    foaf:firstName \"Markus\";\n" +
    "    foaf:lastName \"Schröder\";\n" +
    "    foaf:knows <https://www.dfki.uni-kl.de/~jilek> .",

    "@prefix ex: <https://example.org/>\n" +
    "\n" +
    "ex:mschroeder a ex:Human ;\n" +
    "    ex:givenName \"Markus\";\n" +
    "    ex:surname \"Schröder\";\n" +
    "    ex:acquainted ex:jilek ."
    ],
    
    ["A2","B2"]
];

var mappings = [
    
    {
        name: "",
        desc: "To refer to an Excel file, use the <code>ql:Spreadsheet</code> reference formulation. \n\
With the help of our <a target='_blank' href='http://www.dfki.uni-kl.de/~mschroeder/ld/ss'>Spreadsheet Ontology</a> you refer to the Excel file, sheet and cells. Access a cell's meta-data in templates or references, for example, get its address or string value.",
        code: prefixes + 
            ":ls1\n" +
            "    a rml:LogicalSource ;\n" +
            "    rml:referenceFormulation ql:Spreadsheet ;\n" +
            "    rml:source [\n" +
            "      a ss:Workbook;\n" +
            "      ss:url \"workbook.xlsx\" ;\n" +
            "      ss:sheetName \"Papers\" ;\n" +
            "      ss:range \"A2:A5\" ;\n" +
            "    ] .\n" +
            "\n" +
            ":tm1 a rr:TriplesMap ;\n" +
            "  rml:logicalSource :ls1 ;\n" +
            "  rr:subjectMap [ \n" +
            "    a rr:SubjectMap ;\n" +
            "    rr:class     ex:Paper ;\n" +
            "    rr:template  \"http://example.org/{address}\"\n" +
            "  ] ;\n" +
            "  rr:predicateObjectMap [\n" +
            "    a rr:PredicateObjectMap ;\n" +
            "    rr:predicateMap  [ \n" +
            "      a rr:PredicateMap ;\n" +
            "      rr:constant  ex:title\n" +
            "    ] ;\n" +
            "    rr:objectMap [ \n" +
            "      a rr:ObjectMap ;\n" +
            "      rml:reference  \"valueString\"\n" +
            "    ]\n" +
            "  ] ."
    },
    
    {
        name: "",
        desc: "Use parentheses and indices to refer relatively to other cells. Access the floating point value of a cell as an integer with <code>valueInt</code>.",
        code: prefixes + 
":tm1 a rr:TriplesMap ;\n" +
"  rml:logicalSource :ls1 ;\n" +
"  rr:subjectMap [ \n" +
"    a rr:SubjectMap ;\n" +
"    rr:class     ex:Paper ;\n" +
"    rr:template  \"http://example.org/{address}\"\n" +
"  ] ;\n" +
"  rr:predicateObjectMap [\n" +
"    a rr:PredicateObjectMap ;\n" +
"    rr:predicateMap  [ \n" +
"      a rr:PredicateMap ;\n" +
"      rr:constant  ex:numberOfPages\n" +
"    ] ;\n" +
"    rr:objectMap [ \n" +
"      a rr:ObjectMap ;\n" +
"      rml:reference  \"(1,0).valueInt\" ;\n" +
"      rr:datatype xsd:integer\n" +
"    ]\n" +
"  ] .\n\n" + 
":ls1\n" +
"    a rml:LogicalSource ;\n" +
"    rml:referenceFormulation ql:Spreadsheet ;\n" +
"    rml:source [\n" +
"      a ss:Workbook;\n" +
"      ss:url \"workbook.xlsx\" ;\n" +
"      ss:sheetName \"Papers\" ;\n" +
"      ss:range \"A2:A5\" ;\n" +
"    ] ."
    },
    
    {
        name: "",
        desc: "Access the floating point value of a cell with <code>valueNumeric</code>.",
        code: prefixes + 
":tm1 a rr:TriplesMap ;\n" +
"  rml:logicalSource :ls1 ;\n" +
"  rr:subjectMap [ \n" +
"    a rr:SubjectMap ;\n" +
"    rr:class     ex:Paper ;\n" +
"    rr:template  \"http://example.org/{address}\"\n" +
"  ] ;\n" +
"  rr:predicateObjectMap [\n" +
"    a rr:PredicateObjectMap ;\n" +
"    rr:predicateMap  [ \n" +
"      a rr:PredicateMap ;\n" +
"      rr:constant  ex:price\n" +
"    ] ;\n" +
"    rr:objectMap [ \n" +
"      a rr:ObjectMap ;\n" +
"      rml:reference  \"(2,0).valueNumeric\";\n" +
"      rr:datatype xsd:decimal\n" +
"    ]\n" +
"  ] .\n" +
"\n" +
":ls1\n" +
"  a rml:LogicalSource ;\n" +
"  rml:referenceFormulation ql:Spreadsheet ;\n" +
"  rml:source [\n" +
"    a ss:Workbook;\n" +
"    ss:url \"workbook.xlsx\" ;\n" +
"    ss:sheetName \"Papers\" ;\n" +
"    ss:range \"A2:A5\" ;\n" +
"  ] ."
    },
    
    {
        name: "",
        desc: "Access the boolean value of a cell with <code>valueBoolean</code>.",
        code: prefixes + 
":tm1 a rr:TriplesMap ;\n" +
"  rml:logicalSource :ls1 ;\n" +
"  rr:subjectMap [ \n" +
"    a rr:SubjectMap ;\n" +
"    rr:class     ex:Paper ;\n" +
"    rr:template  \"http://example.org/{address}\"\n" +
"  ] ;\n" +
"  rr:predicateObjectMap [\n" +
"    a rr:PredicateObjectMap ;\n" +
"    rr:predicateMap  [ \n" +
"      a rr:PredicateMap ;\n" +
"      rr:constant  ex:accepted\n" +
"    ] ;\n" +
"    rr:objectMap [ \n" +
"      a rr:ObjectMap ;\n" +
"      rml:reference  \"(3,0).valueBoolean\" ;\n" +
"      rr:datatype xsd:boolean\n" +
"    ]\n" +
"  ] .\n" +
"\n" +
":ls1\n" +
"  a rml:LogicalSource ;\n" +
"  rml:referenceFormulation ql:Spreadsheet ;\n" +
"  rml:source [\n" +
"    a ss:Workbook;\n" +
"    ss:url \"workbook.xlsx\" ;\n" +
"    ss:sheetName \"Papers\" ;\n" +
"    ss:range \"A2:A5\" ;\n" +
"  ] ."
    },
    
    {
        name: "",
        desc: "Access the formatted text of a cell with <code>valueRichText</code>. This way you get the font face, color and styles (bold, italic, underlined or striked).",
        code: prefixes + 
":tm1 a rr:TriplesMap ;\n" +
"  rml:logicalSource :ls1 ;\n" +
"  rr:subjectMap [ \n" +
"    a rr:SubjectMap ;\n" +
"    rr:class     ex:Paper ;\n" +
"    rr:template  \"http://example.org/{address}\"\n" +
"  ] ;\n" +
"  rr:predicateObjectMap [\n" +
"    a rr:PredicateObjectMap ;\n" +
"    rr:predicateMap  [ \n" +
"      a rr:PredicateMap ;\n" +
"      rr:constant  ex:titleRichText\n" +
"    ] ;\n" +
"    rr:objectMap [ \n" +
"      a rr:ObjectMap ;\n" +
"      rml:reference  \"(0,0).valueRichText\"\n" +
"    ]\n" +
"  ] .\n" +
"\n" +
":ls1\n" +
"  a rml:LogicalSource ;\n" +
"  rml:referenceFormulation ql:Spreadsheet ;\n" +
"  rml:source [\n" +
"    a ss:Workbook;\n" +
"    ss:url \"workbook.xlsx\" ;\n" +
"    ss:sheetName \"Papers\" ;\n" +
"    ss:range \"A2:A5\" ;\n" +
"  ] ."
    },
    
    {
        name: "",
        desc: "If the cell type changes (like in column E), get a JSON representation of the cell and process it with an FnO function. The <code>cellType</code> key can be <code>\"string\"</code>, <code>\"numeric\"</code>, <code>\"boolean\"</code> or <code>\"formula\"</code>.",
        code: prefixes + 
":tm1 a rr:TriplesMap ;\n" +
"  rml:logicalSource :ls1 ;\n" +
"  rr:subjectMap [\n" +
"    a rr:SubjectMap ;\n" +
"    rr:class     ex:Paper ;\n" +
"    rr:template  \"http://example.org/{address}\"\n" +
"  ] ;\n" +
"  rr:predicateObjectMap [\n" +
"    a rr:PredicateObjectMap ;\n" +
"    rr:predicateMap  [\n" +
"      a rr:PredicateMap ;\n" +
"      rr:constant  ex:published\n" +
"    ] ;\n" +
"    rr:objectMap [\n" +
"      a rr:ObjectMap ;\n" +
"      rml:reference  \"(4,0).json\"\n" +
"    ]\n" +
"  ] .\n" +
"\n" +
":ls1\n" +
"  a rml:LogicalSource ;\n" +
"  rml:referenceFormulation ql:Spreadsheet ;\n" +
"  rml:source [\n" +
"    a ss:Workbook;\n" +
"    ss:url \"workbook.xlsx\" ;\n" +
"    ss:sheetName \"Papers\" ;\n" +
"    ss:range \"A2:A5\" ;\n" +
"  ] ."
    },
    
    {
        name: "",
        desc: "Use square brackets to refer to a cell with absolute coordinates.",
        code: prefixes + 
":tm1 a rr:TriplesMap ;\n" +
"  rml:logicalSource :ls1 ;\n" +
"  rr:subjectMap [ \n" +
"    a rr:SubjectMap ;\n" +
"    rr:class     ex:Paper ;\n" +
"    rr:template  \"http://example.org/{address}\"\n" +
"  ] ;\n" +
"  rr:predicateObjectMap [\n" +
"    a rr:PredicateObjectMap ;\n" +
"    rr:predicateMap  [ \n" +
"      a rr:PredicateMap ;\n" +
"      rr:template  \"http://example.org/{[1,0].valueString}\"\n" +
"    ] ;\n" +
"    rr:objectMap [ \n" +
"      a rr:ObjectMap ;\n" +
"      rml:reference  \"(1,0).valueInt\" ;\n" +
"      rr:datatype xsd:integer\n" +
"    ]\n" +
"  ] .\n\n" + 
":ls1\n" +
"    a rml:LogicalSource ;\n" +
"    rml:referenceFormulation ql:Spreadsheet ;\n" +
"    rml:source [\n" +
"      a ss:Workbook;\n" +
"      ss:url \"workbook.xlsx\" ;\n" +
"      ss:sheetName \"Papers\" ;\n" +
"      ss:range \"A2:A5\" ;\n" +
"    ] ."
    },
    
        {
        name: "",
        desc: "Access the background color of a cell and use an FnO function to assign a certain type when the background color is blue.",
        code: prefixes + 
":tm1 a rr:TriplesMap ;\n" +
"  rml:logicalSource :ls1 ;\n" +
"  rr:subjectMap [\n" +
"    a rr:SubjectMap ;\n" +
"    rr:class     ex:Paper ;\n" +
"    rr:template  \"http://example.org/{address}\"\n" +
"  ] ;\n" +
"  rr:predicateObjectMap [\n" +
"    a rr:PredicateObjectMap ;\n" +
"    rr:predicateMap  [\n" +
"      a rr:PredicateMap ;\n" +
"      rr:constant rdf:type\n" +
"    ] ;\n" +
"    rr:objectMap [\n" +
"        a rr:ObjectMap , fnml:FunctionMap ;\n" +
"        fnml:functionValue  [\n" +
"            rr:predicateObjectMap  [\n" +
"                rr:predicate  fno:executes ;\n" +
"                rr:object     <java:com.github.mschroedergithub.fno.CustomFunctions.ifRegexReturnElse>\n" +
"            ] ;\n" +
"            # String value\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.string.0> ;\n" +
"                rr:objectMap  [ rml:reference  \"backgroundColor\" ]\n" +
"            ] ;\n" +
"            # String pattern\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.string.1> ;\n" +
"                rr:objectMap  [ rr:constant  \"....ff\" ]\n" +
"            ] ;\n" +
"            # String ifResult\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.string.2> ;\n" +
"                rr:objectMap  [ rr:constant  ex:BestPaper ]\n" +
"            ] ;\n" +
"            # String elseResult\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.string.3> ;\n" +
"                rr:objectMap  [ rr:template  \"\" ]\n" +
"            ]\n" +
"        ] ;\n" +
"        rr:termType rr:IRI\n" +
"    ]\n" +
"  ] .\n" +
"\n" +
":ls1\n" +
"  a rml:LogicalSource ;\n" +
"  rml:referenceFormulation ql:Spreadsheet ;\n" +
"  rml:source [\n" +
"    a ss:Workbook;\n" +
"    ss:url \"workbook.xlsx\" ;\n" +
"    ss:sheetName \"Papers\" ;\n" +
"    ss:range \"A2:A5\" ;\n" +
"  ] ."
    },
    
    {
        name: "",
        desc: "Filter cells in the logical source with a <code>javaScriptFilter</code>. In our example, we iterate over papers that have less than five pages.",
        code: prefixes + 
            ":ls1\n" +
            "    a rml:LogicalSource ;\n" +
            "    rml:referenceFormulation ql:Spreadsheet ;\n" +
            "    rml:source [\n" +
            "      a ss:Workbook;\n" +
            "      ss:url \"workbook.xlsx\" ;\n" +
            "      ss:sheetName \"Papers\" ;\n" +
            "      ss:range \"A2:A5\" ;\n" +
            "      ss:javaScriptFilter \"record.get('(1,0).valueNumeric')[0] < 5\"\n" +
            "    ] .\n" +
            "\n" +
            ":tm1 a rr:TriplesMap ;\n" +
            "  rml:logicalSource :ls1 ;\n" +
            "  rr:subjectMap [ \n" +
            "    a rr:SubjectMap ;\n" +
            "    rr:class     ex:Paper ;\n" +
            "    rr:template  \"http://example.org/{address}\"\n" +
            "  ] ;\n" +
            "  rr:predicateObjectMap [\n" +
            "    a rr:PredicateObjectMap ;\n" +
            "    rr:predicateMap  [ \n" +
            "      a rr:PredicateMap ;\n" +
            "      rr:constant  ex:title\n" +
            "    ] ;\n" +
            "    rr:objectMap [ \n" +
            "      a rr:ObjectMap ;\n" +
            "      rml:reference  \"valueString\"\n" +
            "    ]\n" +
            "  ] ."
    },
    
    {
        name: "",
        desc: "Iterate over arbitrary cell ranges, for example, column cells.",
        code: prefixes + 
":ls2\n" +
"  a rml:LogicalSource ;\n" +
"  rml:referenceFormulation ql:Spreadsheet ;\n" +
"  rml:source [\n" +
"    a ss:Workbook;\n" +
"    ss:url \"workbook.xlsx\" ;\n" +
"    ss:sheetName \"Papers\" ;\n" +
"    ss:range \"A1:E1\" ;\n" +
"  ] .\n" +
"\n" +
":tm1 a rr:TriplesMap ;\n" +
"  rml:logicalSource :ls2 ;\n" +
"  rr:subjectMap [\n" +
"    a rr:SubjectMap ;\n" +
"    rr:class     rdf:Property ;\n" +
"    rr:template  \"http://example.org/{valueString}\"\n" +
"  ] ;\n" +
"  rr:predicateObjectMap [\n" +
"    a rr:PredicateObjectMap ;\n" +
"    rr:predicateMap  [\n" +
"      a rr:PredicateMap ;\n" +
"      rr:constant  rdfs:label\n" +
"    ] ;\n" +
"    rr:objectMap [\n" +
"      a rr:ObjectMap ;\n" +
"      rml:reference  \"valueString\"\n" +
"    ]\n" +
"  ] ."
    },
    
    {
        name: "",
        desc: "It can happen that cells contain multiple information of different properties (like column F). Use the special <code>ss:zip</code> feature to zip a list of properties with a list of objects returned from an FnO function.",
        code: prefixes + 
":tm1 a rr:TriplesMap ;\n" +
"  rml:logicalSource :ls1 ;\n" +
"  rr:subjectMap [\n" +
"    a rr:SubjectMap ;\n" +
"    rr:class     ex:Paper ;\n" +
"    rr:template  \"http://example.org/{address}\"\n" +
"  ] ;\n" +
"  rr:predicateObjectMap [\n" +
"    a rr:PredicateObjectMap ;\n" +
"    ss:zip true ;\n" +
"    rr:predicateMap  [\n" +
"      a rr:PredicateMap ;\n" +
"      rr:constant ( ex:numberOfPages ex:price )\n" +
"    ] ;\n" +
"    rr:objectMap [\n" +
"        a rr:ObjectMap , fnml:FunctionMap ;\n" +
"        fnml:functionValue  [\n" +
"            rr:predicateObjectMap  [\n" +
"                rr:predicate  fno:executes ;\n" +
"                rr:object     <java:com.github.mschroedergithub.fno.CustomFunctions.ifRegexReturnGroup>\n" +
"            ] ;\n" +
"            # String value\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.string.0> ;\n" +
"                rr:objectMap  [ rml:reference  \"(5,0).valueString\" ]\n" +
"            ] ;\n" +
"            # String pattern\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.string.1> ;\n" +
"                rr:objectMap  [ rr:constant  \"\\\\d+\\\\.?\\\\d*\" ]\n" +
"            ] ;\n" +
"            # Integer group\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.integer.0> ;\n" +
"                rr:objectMap  [ rr:constant  0 ]\n" +
"            ]\n" +
"        ] ;\n" +
"        rr:datatype xsd:decimal\n" +
"    ]\n" +
"  ] .\n" +
"\n" +
":ls1\n" +
"  a rml:LogicalSource ;\n" +
"  rml:referenceFormulation ql:Spreadsheet ;\n" +
"  rml:source [\n" +
"    a ss:Workbook;\n" +
"    ss:url \"workbook.xlsx\" ;\n" +
"    ss:sheetName \"Papers\" ;\n" +
"    ss:range \"A2:A5\" ;\n" +
"  ] ."
    },
    
    
    {
        name: "",
        desc: "It can happen that cells contain multiple complex entities like persons having first and last names (column G). Use an FnO function to extract persons and return them as an RDF graph in turtle syntax. By using the special term type <code>ss:Graph</code>, the returned value is correctly interpreted.",
        code: prefixes + 
":tm1 a rr:TriplesMap ;\n" +
"  rml:logicalSource :ls1 ;\n" +
"  rr:subjectMap [\n" +
"    a rr:SubjectMap ;\n" +
"    rr:class     ex:Paper ;\n" +
"    rr:template  \"http://example.org/{address}\"\n" +
"  ] ;\n" +
"  rr:predicateObjectMap [\n" +
"    a rr:PredicateObjectMap ;\n" +
"    rr:predicateMap  [\n" +
"      a rr:PredicateMap ;\n" +
"      rr:constant ex:hasAuthor\n" +
"    ] ;\n" +
"    rr:objectMap [\n" +
"        a rr:ObjectMap , fnml:FunctionMap ;\n" +
"        fnml:functionValue  [\n" +
"            rr:predicateObjectMap  [\n" +
"                rr:predicate  fno:executes ;\n" +
"                rr:object     <java:com.github.mschroedergithub.fno.CustomFunctions.personInformationExtraction>\n" +
"            ] ;\n" +
"            # String value\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.string.0> ;\n" +
"                rr:objectMap  [ rml:reference  \"(6,0).valueString\" ]\n" +
"            ] ;\n" +
"            # String personClassUri\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.string.1> ;\n" +
"                rr:objectMap  [ rr:constant  \"http://xmlns.com/foaf/0.1/Person\" ]\n" +
"            ] ;\n" +
"            # String firstnamePropertyUri\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.string.2> ;\n" +
"                rr:objectMap  [ rr:constant  \"http://xmlns.com/foaf/0.1/firstName\" ]\n" +
"            ] ;\n" +
"            # String lastnamePropertyUri\n" +
"            rr:predicateObjectMap [\n" +
"                rr:predicate  <java:parameter.predicate.string.3> ;\n" +
"                rr:objectMap  [ rr:constant  \"http://xmlns.com/foaf/0.1/lastName\" ]\n" +
"            ]\n" +
"        ] ;\n" +
"        rr:termType ss:Graph\n" +
"    ]\n" +
"  ] .\n" +
"\n" +
":ls1\n" +
"  a rml:LogicalSource ;\n" +
"  rml:referenceFormulation ql:Spreadsheet ;\n" +
"  rml:source [\n" +
"    a ss:Workbook;\n" +
"    ss:url \"workbook.xlsx\" ;\n" +
"    ss:sheetName \"Papers\" ;\n" +
"    ss:range \"A2:A5\" ;\n" +
"  ] ."
    },
    
];