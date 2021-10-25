
package de.dfki.sds.spread2rml.match;

import java.util.Map;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * 
 */
@FunctionalInterface
public interface StatementMatchDistance {

    
    public double distance(Statement stmtA, Statement stmtB, Map<Resource, Resource> match);
    
}
