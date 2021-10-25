
package de.dfki.sds.hephaistos.forge.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Function Ontology .
 */
public class FNO {
    
    public static final String NS = "https://w3id.org/function/ontology#";
    
    public static final Resource Algorithm = ResourceFactory.createResource(NS + "Algorithm");
    public static final Resource Execution = ResourceFactory.createResource(NS + "Execution");
    public static final Resource Function = ResourceFactory.createResource(NS + "Function");
    public static final Resource Implementation = ResourceFactory.createResource(NS + "Implementation");
    public static final Resource Mapping = ResourceFactory.createResource(NS + "Mapping");
    public static final Resource MethodMapping = ResourceFactory.createResource(NS + "MethodMapping");
    public static final Resource Output = ResourceFactory.createResource(NS + "Output");
    public static final Resource Parameter = ResourceFactory.createResource(NS + "Parameter");
    public static final Resource ParameterMapping = ResourceFactory.createResource(NS + "ParameterMapping");
    public static final Resource Problem = ResourceFactory.createResource(NS + "Problem");
    public static final Resource ReturnMapping = ResourceFactory.createResource(NS + "ReturnMapping");
    
    public static final Property executes = ResourceFactory.createProperty(NS + "executes");
    public static final Property expects = ResourceFactory.createProperty(NS + "expects");
    public static final Property function = ResourceFactory.createProperty(NS + "function");
    public static final Property implementation = ResourceFactory.createProperty(NS + "implementation");
    public static final Property implem = ResourceFactory.createProperty(NS + "implements");
    public static final Property methodMapping = ResourceFactory.createProperty(NS + "methodMapping");
    public static final Property parameterMapping = ResourceFactory.createProperty(NS + "parameterMapping");
    public static final Property predicate = ResourceFactory.createProperty(NS + "predicate");
    public static final Property returnMapping = ResourceFactory.createProperty(NS + "returnMapping");
    public static final Property returns = ResourceFactory.createProperty(NS + "returns");
    public static final Property solves = ResourceFactory.createProperty(NS + "solves");
    public static final Property type = ResourceFactory.createProperty(NS + "type");
    public static final Property uses = ResourceFactory.createProperty(NS + "uses");
    
    public static final Property nullable = ResourceFactory.createProperty(NS + "nullable");
    public static final Property required = ResourceFactory.createProperty(NS + "required");
    
    
}
