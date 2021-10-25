package de.dfki.sds.spread2rml.match;

import java.io.IOException;

/**
 *
 */
public class MatchMain {

    public static void main(String[] args) throws IOException {
        MatchingEvaluation eval = new MatchingEvaluation();

        eval.executeAndEvaluate();
    }
    
}
