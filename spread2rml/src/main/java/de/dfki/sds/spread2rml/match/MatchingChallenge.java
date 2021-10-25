
package de.dfki.sds.spread2rml.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

/**
 * 
 */
public class MatchingChallenge {

    private String description;
    private Model actual;
    private Model expected;
    private Map<Resource, Resource> matching;
    
    private List<MatchingSolution> solutions;

    public MatchingChallenge() {
        matching = new HashMap<>();
        solutions = new ArrayList<>();
        actual = ModelFactory.createDefaultModel();
        expected = ModelFactory.createDefaultModel();
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Model getActual() {
        return actual;
    }

    public void setActual(Model actual) {
        this.actual = actual;
    }

    public Model getExpected() {
        return expected;
    }

    public void setExpected(Model expected) {
        this.expected = expected;
    }

    public Map<Resource, Resource> getMatching() {
        return matching;
    }

    public List<MatchingSolution> getSolutions() {
        return solutions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MatchingChallenge{description=").append(description);
        sb.append("\n, actual=\n").append(MatchingChallengeGenerator.toTTL(actual));
        sb.append("\n, expected=\n").append(MatchingChallengeGenerator.toTTL(expected));
        sb.append("\n, matching=").append(matching);
        sb.append(", solutions=").append(solutions.size());
        sb.append('}');
        return sb.toString();
    }
    
    
    
    
    
}
