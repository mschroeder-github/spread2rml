
package de.dfki.sds.hephaistos.forge.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * The Function RML Extension. 
 */
public class FNML {

    public static final String NS = "http://semweb.mmlab.be/ns/fnml#";
    
    public static final Resource FunctionMap = ResourceFactory.createResource(NS + "FunctionMap");
    
    public static final Property functionValue = ResourceFactory.createProperty(NS + "functionValue");
    
}
