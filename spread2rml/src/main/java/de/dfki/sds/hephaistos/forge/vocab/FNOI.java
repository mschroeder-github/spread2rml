
package de.dfki.sds.hephaistos.forge.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Function Ontology Implementation .
 */
public class FNOI {

    public static final String NS = "https://w3id.org/function/vocabulary/implementation#";
    
    public static final Resource JavaClass = ResourceFactory.createResource(NS + "JavaClass");
    public static final Resource JavaImplementation = ResourceFactory.createResource(NS + "JavaImplementation");
    
    public static final Resource JavaScriptFunction = ResourceFactory.createResource(NS + "JavaScriptFunction");
    public static final Resource JavaScriptImplementation = ResourceFactory.createResource(NS + "JavaScriptImplementation");
    
    public static final Resource JsonApi = ResourceFactory.createResource(NS + "JsonApi");
    public static final Resource NpmPackage = ResourceFactory.createResource(NS + "NpmPackage");
    public static final Resource WebApi = ResourceFactory.createResource(NS + "WebApi");
    
    public static final Property class_name = ResourceFactory.createProperty(NS + "class-name");
    
}
