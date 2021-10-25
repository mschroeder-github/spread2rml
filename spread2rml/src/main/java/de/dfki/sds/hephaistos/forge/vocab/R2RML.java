
package de.dfki.sds.hephaistos.forge.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * 
 */
public class R2RML {
    
    public static final String NS = "http://www.w3.org/ns/r2rml#";
    
    public static final Resource TriplesMap = ResourceFactory.createResource(NS + "TriplesMap");
    public static final Resource PredicateObjectMap = ResourceFactory.createResource(NS + "PredicateObjectMap");
    public static final Resource SubjectMap = ResourceFactory.createResource(NS + "SubjectMap");
    public static final Resource PredicateMap = ResourceFactory.createResource(NS + "PredicateMap");
    public static final Resource ObjectMap = ResourceFactory.createResource(NS + "ObjectMap");
    
    public static final Resource IRI = ResourceFactory.createResource(NS + "IRI");
    public static final Resource BlankNode = ResourceFactory.createResource(NS + "BlankNode");
    public static final Resource Literal = ResourceFactory.createResource(NS + "Literal");
    
    public static final Property template = ResourceFactory.createProperty(NS + "template");
    public static final Property clazz = ResourceFactory.createProperty(NS + "class");
    
    public static final Property predicateObjectMap = ResourceFactory.createProperty(NS + "predicateObjectMap");
    
    public static final Property subjectMap = ResourceFactory.createProperty(NS + "subjectMap");
    public static final Property predicateMap = ResourceFactory.createProperty(NS + "predicateMap");
    public static final Property objectMap = ResourceFactory.createProperty(NS + "objectMap");
    
    public static final Property subject = ResourceFactory.createProperty(NS + "subject");
    public static final Property predicate = ResourceFactory.createProperty(NS + "predicate");
    public static final Property object = ResourceFactory.createProperty(NS + "object");
    
    public static final Property constant = ResourceFactory.createProperty(NS + "constant");
    
    public static final Property datatype = ResourceFactory.createProperty(NS + "datatype");
    public static final Property termType = ResourceFactory.createProperty(NS + "termType");
    
}
