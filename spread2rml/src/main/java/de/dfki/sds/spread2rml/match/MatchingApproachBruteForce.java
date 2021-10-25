
package de.dfki.sds.spread2rml.match;

import de.dfki.sds.mschroeder.commons.lang.CombinatoricsUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * 
 */
public class MatchingApproachBruteForce extends MatchingApproach {

    {
        this.name = "Brute Force";
    }
    
    @Override
    public void match(Model actual, Model expected, List<Map<Resource, Resource>> matchingSolutions) {
        
        long complexity = complexity(actual.size(), expected.size());
        
        System.out.println("expected complexity: " + complexity);
        
        List<Statement> aStmts = actual.listStatements().toList();
        List<Statement> eStmts = expected.listStatements().toList();
        
        List<Map<Statement, Statement>> possibleMatches = CombinatoricsUtility.possibleMatches(aStmts, eStmts);
        
        //Map<Map<Statement, Statement>, Integer> possMatch2count = new HashMap<>();

        //List<Map<Statement, Statement>> workingList = new ArrayList<>();
        //List<Map<Resource, Resource>> workingMatching = new ArrayList<>();
        
        for(Map<Statement, Statement> stmtMap : possibleMatches) {
            
            Map<Resource, Resource> matchMap = new HashMap<>();
            
            //int workingStmtCount = 0;
            boolean allWorking = true;
            
            for(Entry<Statement, Statement> entry : stmtMap.entrySet()) {
                
                Resource aS = entry.getKey().getSubject();
                Resource eS = entry.getValue().getSubject();
                
                if(matchMap.containsKey(aS) && !matchMap.get(aS).equals(eS)) {
                    allWorking = false;
                    break;
                }
                matchMap.put(aS, eS);
                
                Resource aP = entry.getKey().getPredicate();
                Resource eP = entry.getValue().getPredicate();
                if(matchMap.containsKey(aP) && !matchMap.get(aP).equals(eP)) {
                    allWorking = false;
                    break;
                }
                matchMap.put(aP, eP);
                
                RDFNode aO = entry.getKey().getObject();
                RDFNode eO = entry.getValue().getObject();
                
                if(aO.isLiteral() && eO.isLiteral()) {
                    
                    if(!aO.equals(eO)) {
                        allWorking = false;
                        break;
                    }
                    
                } else if(aO.isResource() && eO.isResource()) {
                    
                    if(matchMap.containsKey(aO.asResource()) && !matchMap.get(aO.asResource()).equals(eO.asResource())) {
                        allWorking = false;
                        break;
                    }
                    matchMap.put(aO.asResource(), eO.asResource());
                    
                } else {
                    //can not match
                    allWorking = false;
                    break;
                }
                
                //workingStmtCount++;
            }
            
            if(allWorking) {
                //workingList.add(stmtMap);
                matchingSolutions.add(matchMap);
            }
            
            //possMatch2count.put(stmtMap, workingStmtCount);
        }
    }

    private long complexity(long sizeA, long sizeB) {
        return CombinatoricsUtils.factorial((int) Math.max(sizeA, sizeB)) / CombinatoricsUtils.factorial((int) Math.abs(sizeA - sizeB));
    }
    
    private static void complexity() {
        List<Integer> a = new ArrayList<>();
        
        for(int i = 0; i < 12; i++) {
            
            a.add(i+1);
            
            List<Integer> b = new ArrayList<>();
            for(int j = 0; j < 12; j++) {
                
                b.add(j+1);
                
                
                int diff = Math.abs(a.size() - b.size());
                //if(diff < 0) {
                //    diff = 0;
                //}
                long numberOf = CombinatoricsUtils.factorial(Math.max(a.size(), b.size())) / CombinatoricsUtils.factorial(diff);
                //if(a.size() < b.size()) {
                //    numberOfChoose = 0;
                //}
                
                //  max(a,b)! / |a - b|!
                
                
                long begin = System.currentTimeMillis();
                int real = CombinatoricsUtility.possibleMatches(a, b).size();
                long duration = System.currentTimeMillis() - begin;
                
                //System.out.println(
                //        a.size() + ", " + b.size() + ": " /*+ 
                //        CombinatoricsUtility.choose(a, b.size()).size()*/ + " calculated " + numberOf + 
                //        ", possible matches: " + CombinatoricsUtility.possibleMatches(a, b).size()
                //);
                
                System.out.println(a.size() + ", " + b.size() + ": " + numberOf + " in " + duration + " ms");
                
                //System.out.println(a.size() + ", " + b.size() + ": " + CombinatoricsUtility.possibleMatches(a, b).size());
            }
        }
        
    }
}
