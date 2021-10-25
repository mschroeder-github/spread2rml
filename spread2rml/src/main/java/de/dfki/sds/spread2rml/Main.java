
package de.dfki.sds.spread2rml;

import java.io.IOException;
import java.util.function.Predicate;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * 
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        ModelFactory.createDefaultModel();
        System.out.println();
        
        runApproaches();
        evaluateApproaches();
    }
    
    //uncomment to run different approach dataset evaluations
    private static Predicate<String> filter = name -> {
        //return name.contains("Any23 data.gov");
        //return name.contains("Any23 datasprout");
        //return name.contains("Any23 Industry");
        
        return name.contains("Spread2RML data.gov");
        //return name.contains("Spread2RML datasprout");
        //return name.contains("Spread2RML Industry");
    };
    
    private static void runApproaches() {
        Evaluation evaluation = new Evaluation();
        
        evaluation.runApproachesOnDatasets(filter);
    }
    
    private static void evaluateApproaches() throws IOException {
        Evaluation evaluation = new Evaluation();
        
        evaluation.runEvaluationOnResults(true, filter);
    }
    
}
