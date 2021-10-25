
package de.dfki.sds.spread2rml.match;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.impl.GraphMatcher;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * 
 */
public class MatchingApproachJenaGraphMatcher extends MatchingApproach {

    {
        this.name = "Jena GraphMatcher";
    }
    
    @Override
    public void match(Model actual, Model expected, List<Map<Resource, Resource>> matchingSolutions) {
        
        Model aModel = MatchingChallengeGenerator.copy(actual);
        Map<Resource, Resource> uri2blankActual = new HashMap<>();
        Map<Resource, Resource> blank2uriActual = new HashMap<>();
        for(Resource r : getAllResources(aModel)) {
            Resource blank = aModel.createResource();
            uri2blankActual.put(r, blank);
            blank2uriActual.put(blank, r);
        }
        MatchingChallengeGenerator.substitute(uri2blankActual, aModel);
        
        
        Model eModel = MatchingChallengeGenerator.copy(expected);
        Map<Resource, Resource> uri2blankExpected = new HashMap<>();
        Map<Resource, Resource> blank2uriExpected = new HashMap<>();
        for(Resource r : getAllResources(eModel)) {
            Resource blank = eModel.createResource();
            uri2blankExpected.put(r, blank);
            blank2uriExpected.put(blank, r);
        }
        MatchingChallengeGenerator.substitute(uri2blankExpected, eModel);
        
        Node[][] nodes = GraphMatcher.match(aModel.getGraph(), eModel.getGraph());
        
        Map<Resource, Resource> matching = new HashMap<>();
        
        if(nodes != null) {
            for(Node[] nodePair : nodes) {
                
                Resource a = aModel.getRDFNode(nodePair[0]).asResource();
                Resource e = eModel.getRDFNode(nodePair[1]).asResource();
                
                matching.put(blank2uriActual.get(a), blank2uriExpected.get(e));
            }
            
            matchingSolutions.add(matching);
        }
    }

}
