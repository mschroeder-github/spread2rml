
package de.dfki.sds.hephaistos.forge.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class RML {
    
    public static final String NS = "http://semweb.mmlab.be/ns/rml#";
    
    public static final Resource LogicalSource = ResourceFactory.createResource(NS + "LogicalSource");
    
    //new by me for new termType
    public static final Resource Graph = ResourceFactory.createResource(NS + "Graph");
    //a special class that is used in the returned graph
    //we reuse rr:object to point to the objects in the graph that should be
    //returned and used for the predicateObjectMap
    // [
    //    a         rml:SelectedObjects ;
    //    rr:object <object1> ;
    //    rr:object <object2> ;
    //    rr:object <object3> 
    // ]
    public static final Resource SelectedObjects = ResourceFactory.createResource(NS + "SelectedObjects");
    
    
    //new by me
    public static final Property zip = ResourceFactory.createProperty(NS + "zip");
    
    
    public static final Property referenceFormulation = ResourceFactory.createProperty(NS + "referenceFormulation");
    public static final Property source = ResourceFactory.createProperty(NS + "source");
    public static final Property sheetName = ResourceFactory.createProperty(NS + "sheetName");
    public static final Property range = ResourceFactory.createProperty(NS + "range");
    public static final Property javaScriptFilter = ResourceFactory.createProperty(NS + "javaScriptFilter");
    public static final Property logicalSource = ResourceFactory.createProperty(NS + "logicalSource");
    public static final Property reference = ResourceFactory.createProperty(NS + "reference");
    
    
}
