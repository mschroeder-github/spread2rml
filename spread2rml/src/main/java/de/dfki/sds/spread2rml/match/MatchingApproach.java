
package de.dfki.sds.spread2rml.match;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * 
 */
public abstract class MatchingApproach {

    protected String name;
    
    public abstract void match(Model actual, Model expected, List<Map<Resource, Resource>> matchingSolutions);
    
    public static Set<Resource> getSubjectObjects(Model model) {
        Set<Resource> r = new HashSet<>();
        for(Statement stmt : model.listStatements().toList()) {
            r.add(stmt.getSubject());
            if(stmt.getObject().isResource()) {
                r.add(stmt.getObject().asResource());
            }
        }
        return r;
    }
    
    public static Set<Resource> getProperties(Model model) {
        Set<Resource> r = new HashSet<>();
        for(Statement stmt : model.listStatements().toList()) {
            r.add(stmt.getPredicate());
        }
        return r;
    }

    public static Set<Resource> getAllResources(Model model) {
        Set<Resource> s = new HashSet<>();
        s.addAll(getSubjectObjects(model));
        s.addAll(getProperties(model));
        return s;
    }
    
    public Map<Literal, List<Statement>> getLiteralToStatementsMap(Model model) {
        Map<Literal, List<Statement>> m = new HashMap<>();
        for(Statement stmt : model.listStatements().toList()) {
           
            if(stmt.getObject().isLiteral()) {
                //TODO
            }
            
        }
        return m;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "MatchingApproach{" + "name=" + name + '}';
    }
    
}
