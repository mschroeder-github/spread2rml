
package de.dfki.sds.spread2rml;

import de.dfki.sds.spread2rml.match.MatchingChallengeGenerator;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * 
 */
public class ModelConfusionMatrix {

    public static ModelConfusionMatrix get(Model actual, Model expected) {
        ModelConfusionMatrix matrix = new ModelConfusionMatrix();
        
        
        matrix.actual = actual;
        matrix.expected = expected;
        
        
        matrix.truePositive = expected.intersection(actual);
        matrix.falseNegative = expected.difference(actual);
        matrix.falsePositive = actual.difference(expected);
                
        return matrix;
    }
    
    public static ModelConfusionMatrix max(ModelConfusionMatrix a, ModelConfusionMatrix b) {
        if(a != null && b == null)
            return a;
        
        if(a == null && b != null)
            return b;
        
        if(a == null && b == null)
            return null;
        
        if(a.getFScore() > b.getFScore())
            return a;
        
        if(b.getFScore() > a.getFScore())
            return b;
        
        return a;
    }
    
    private Model actual;
    private Model expected;
    
    private Map<Resource, Resource> match;
    
    //confusion matrix for statements
    private Model truePositive; //expected intersection with actual
    private Model falsePositive; //actual difference with expected
    private Model falseNegative; //expected difference with actual
    
    public Model getTruePositive() {
        return truePositive;
    }
    
    public int getTP() {
        return (int) truePositive.size();
    }

    public Model getFalsePositive() {
        return falsePositive;
    }
    
    public int getFP() {
        return (int) falsePositive.size();
    }

    public Model getFalseNegative() {
        return falseNegative;
    }

    public int getFN() {
        return (int) falseNegative.size();
    }
    
    public double getPrecision() {
        return getTruePositive().size() / (double) (getTruePositive().size() + getFalsePositive().size());
    }

    public double getRecall() {
        return getTruePositive().size() / (double) (getTruePositive().size() + getFalseNegative().size());
    }

    public double getFScore() {
        double div = getPrecision() + getRecall();
        if(div == 0)
            return 0;
        return 2 * ((getPrecision() * getRecall()) / div);
    }
    
    public boolean hasError() {
        return falseNegative.size() > 0 || falsePositive.size() > 0;
    }

    public Model getActual() {
        return actual;
    }

    public Model getExpected() {
        return expected;
    }
    
    public String getTTLComparison() {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================").append("\n");
        sb.append(MatchingChallengeGenerator.toTTL(actual)).append("\n");
        sb.append(".........................................").append("\n");
        sb.append(MatchingChallengeGenerator.toTTL(expected)).append("\n");
        sb.append("=========================================");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ModelConfusionMatrix{tp=").append(truePositive.size());
        sb.append(", fp=").append(falsePositive.size());
        sb.append(", fn=").append(falseNegative.size());
        sb.append(", p=").append(getPrecision());
        sb.append(", r=").append(getRecall());
        sb.append(", f=").append(getFScore());
        sb.append('}');
        return sb.toString();
    }

    public Map<Resource, Resource> getMatch() {
        return match;
    }

    public void setMatch(Map<Resource, Resource> match) {
        this.match = match;
    }
    
}
