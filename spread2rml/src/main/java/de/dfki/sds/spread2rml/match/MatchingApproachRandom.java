
package de.dfki.sds.spread2rml.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * 
 */
public class MatchingApproachRandom extends MatchingApproach {

    {
        this.name = "Random";
    }
    
    @Override
    public void match(Model actual, Model expected, List<Map<Resource, Resource>> matchingSolutions) {
        
        List<Resource> expectedSO = new ArrayList<>(getSubjectObjects(expected));
        List<Resource> actualSO = new ArrayList<>(getSubjectObjects(actual));
        
        List<Resource> expectedP = new ArrayList<>(getProperties(expected));
        List<Resource> actualP = new ArrayList<>(getProperties(actual));
        
        Random rnd = new Random();
        
        Map<Resource, Resource> matching = new HashMap<>();
        
        while(!expectedSO.isEmpty() && !actualSO.isEmpty()) {
            
            Resource e = expectedSO.remove(rnd.nextInt(expectedSO.size()));
            Resource a = actualSO.remove(rnd.nextInt(actualSO.size()));
            
            matching.put(a, e);
        }
        
        while(!expectedP.isEmpty() && !actualP.isEmpty()) {
            
            Resource e = expectedP.remove(rnd.nextInt(expectedP.size()));
            Resource a = actualP.remove(rnd.nextInt(actualP.size()));
            
            matching.put(a, e);
        }
        
        matchingSolutions.add(matching);
    }

}
