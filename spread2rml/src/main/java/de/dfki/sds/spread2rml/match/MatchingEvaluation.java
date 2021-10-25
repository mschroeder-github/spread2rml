
package de.dfki.sds.spread2rml.match;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 
 */
public class MatchingEvaluation {
    
    private MatchingChallengeGenerator gen;
    private List<MatchingChallenge> challenges;
    private List<MatchingApproach> approaches;
    
    public MatchingEvaluation() {
        gen = new MatchingChallengeGenerator();
        challenges = gen.manual();
        approaches = new ArrayList<>();
        loadApproaches();
    }
    
    private void loadApproaches() {
        //approaches.add(new MatchingApproachRandom());
        //approaches.add(new MatchingApproachJenaGraphMatcher());
        //approaches.add(new MatchingApproachBruteForce());
        approaches.add(new MatchingApproachBruteForceV2());
    }
    
    public void executeAndEvaluate() throws IOException {
        
        CSVPrinter csv = CSVFormat.DEFAULT.print(new File("matching-results.csv"), StandardCharsets.UTF_8);
        csv.printRecord(
                "challenge desc",
                "approach",
                "precision",
                "recall",
                "tp",
                "fn (miss)",
                "fp (false alarm)",
                "suggestions",
                "duration",
                "exception"
        );
        
        for(MatchingChallenge challenge : challenges) {
            
            //System.out.println();
            //System.out.println(challenge);
            
            for(MatchingApproach approach : approaches) {
                
                MatchingSolution solution = new MatchingSolution();
                solution.setApproach(approach);
                solution.setChallenge(challenge);
                challenge.getSolutions().add(solution);
                
                long begin = System.currentTimeMillis();
                try {
                    approach.match(challenge.getActual(), challenge.getExpected(), solution.getSuggestions());
                } catch(Exception e) {
                    solution.setException(e);
                }
                solution.setDuration(System.currentTimeMillis() - begin);
                
                solution.compare();
             
                csv.printRecord(
                    challenge.getDescription(),
                    approach.getName(),
                    solution.getPrecision(),
                    solution.getRecall(),
                    solution.getTp(),
                    solution.getFn(),
                    solution.getFp(),
                    solution.getSuggestions().size(), 
                    solution.getDuration(),
                    solution.hasException() ? ExceptionUtils.getStackTrace(solution.getException()) : ""
                );
            }
        }
        
        csv.close();
    }
    
}
