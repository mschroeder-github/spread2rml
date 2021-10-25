
package de.dfki.sds.hephaistos.forge.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Function Ontology Mapping .
 */
public class FNOM {

    public static final String NS = "https://w3id.org/function/vocabulary/mapping#";
    
    public static final Resource DefaultReturnMapping = ResourceFactory.createResource(NS + "DefaultReturnMapping");
    public static final Resource ExceptionReturnMapping = ResourceFactory.createResource(NS + "ExceptionReturnMapping");
    public static final Resource PositionParameterMapping = ResourceFactory.createResource(NS + "PositionParameterMapping");
    public static final Resource PropertyParameterMapping = ResourceFactory.createResource(NS + "PropertyParameterMapping");
    public static final Resource StringMethodMapping = ResourceFactory.createResource(NS + "StringMethodMapping");
    
    public static final Property functionOutput = ResourceFactory.createProperty(NS + "functionOutput");
    public static final Property functionParameter = ResourceFactory.createProperty(NS + "functionParameter");
    
    public static final Property implementationParameterPosition = ResourceFactory.createProperty(NS + "implementationParameterPosition");
    public static final Property implementationProperty = ResourceFactory.createProperty(NS + "implementationProperty");
    public static final Property method_name = ResourceFactory.createProperty(NS + "method-name");
    
}
