
package de.dfki.sds.spread2rml.match;

import de.dfki.sds.spread2rml.util.SetUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.jena.rdf.model.Resource;

/**
 * 
 */
public class MatchingSolution {

    private MatchingChallenge challenge;
    private MatchingApproach approach;

    @Deprecated
    private Map<Resource, Resource> suggestion;
    
    private List<Map<Resource, Resource>> suggestions;
    
    private long duration;
    private Exception exception;
    
    private int tp;
    private int fn;
    private int fp;
    
    private double precision;
    private double recall;
    
    public MatchingSolution() {
        suggestion = new HashMap<>();
        suggestions = new ArrayList<>();
    }
    
    public void compare() {
        if(hasException())
            return;
        
        if(suggestions.isEmpty()) {
            //empty suggestion
            suggestions.add(new HashMap<>());
        }
        
        for(Map<Resource, Resource> suggestion : suggestions) {
        
            Set<Entry<Resource, Resource>> suggestedSet = suggestion.entrySet();
            Set<Entry<Resource, Resource>> expectedSet = challenge.getMatching().entrySet();

            Set<Entry<Resource, Resource>> truePositive = SetUtility.intersection(suggestedSet, expectedSet);
            Set<Entry<Resource, Resource>> falsePositive = SetUtility.subtract(suggestedSet, expectedSet); //false alarm
            Set<Entry<Resource, Resource>> falseNegative = SetUtility.subtract(expectedSet, suggestedSet); //miss

            int tp = truePositive.size();
            int fp = falsePositive.size();
            int fn = falseNegative.size();

            //prec = tp / tp + fp
            double precision = truePositive.size() / (double) (truePositive.size() + falsePositive.size());

            //recall = tp / tp + fn
            double recall = truePositive.size() / (double) (truePositive.size() + falseNegative.size());
            
            this.tp = Math.max(tp, this.tp);
            this.fp = Math.max(fp, this.fp);
            this.fn = Math.max(fn, this.fn);
            this.precision = Math.max(precision, this.precision);
            this.recall = Math.max(recall, this.recall);
        }
        
        
    }
    
    public MatchingChallenge getChallenge() {
        return challenge;
    }

    public void setChallenge(MatchingChallenge challenge) {
        this.challenge = challenge;
    }

    public MatchingApproach getApproach() {
        return approach;
    }

    public void setApproach(MatchingApproach approach) {
        this.approach = approach;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Exception getException() {
        return exception;
    }
    
    public boolean hasException() {
        return exception != null;
    }


    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Deprecated
    public Map<Resource, Resource> getSuggestion() {
        return suggestion;
    }

    public List<Map<Resource, Resource>> getSuggestions() {
        return suggestions;
    }
    
    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public int getTp() {
        return tp;
    }

    public int getFn() {
        return fn;
    }

    public int getFp() {
        return fp;
    }

    
    
    
    
}
