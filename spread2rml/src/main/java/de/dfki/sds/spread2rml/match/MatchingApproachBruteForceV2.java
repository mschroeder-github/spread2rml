package de.dfki.sds.spread2rml.match;

import java.util.ArrayList;
import java.util.Comparator;
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
public class MatchingApproachBruteForceV2 extends MatchingApproach {

    {
        this.name = "Brute Force V2";
    }

    private Comparator<Statement> literalLast = (Statement a, Statement b) -> {
        
        boolean aLit = a.getObject().isLiteral();
        boolean bLit = b.getObject().isLiteral();
        
        return Boolean.compare(aLit, bLit);
    };

    @Override
    public void match(Model actual, Model expected, List<Map<Resource, Resource>> matchingSolutions) {
        
        match(actual, expected, new HashMap<>(), matchingSolutions);
    }
    
    public void match(Model actual, Model expected, Map<Resource, Resource> preMatch, List<Map<Resource, Resource>> matchingSolutions) {

        List<Statement> aStmts = actual.listStatements().toList();
        List<Statement> eStmts = expected.listStatements().toList();

        aStmts.sort(literalLast);
        eStmts.sort(literalLast);
        
        List<Map<Resource, Resource>> possibleMatches = choose(aStmts, eStmts, 0.0, preMatch, new StatementMatchDistance() {

            @Override
            public double distance(Statement stmtA, Statement stmtB, Map<Resource, Resource> match) {
                Resource aS = stmtA.getSubject();
                Resource eS = stmtB.getSubject();
                
                //use inverse to make sure that we have a 1:1 matching
                Map<Resource, Resource> inv = new HashMap<>();
                match.forEach((x,y) -> inv.put(y,x));

                //do nothing if equal
                if(!aS.equals(eS)) {
                    
                    //subject check: both directions for 1:1 matching
                    if (match.containsKey(aS) && !match.get(aS).equals(eS) || inv.containsKey(eS) && !inv.get(eS).equals(aS)) {
                        return 1.0;
                    }
                }
                
                match.put(aS, eS);
                inv.put(eS, aS);
                
                //predicate check
                Resource aP = stmtA.getPredicate();
                Resource eP = stmtB.getPredicate();
                
                //do nothing if equal
                if(!aP.equals(eP)) {
                
                    if (match.containsKey(aP) && !match.get(aP).equals(eP) || inv.containsKey(eP) && !inv.get(eP).equals(aP)) {
                        //correct subject mapping
                        match.remove(aS);
                        return 1.0;
                    }
                }
                
                match.put(aP, eP);
                inv.put(eP, aP);

                //object check
                RDFNode aO = stmtA.getObject();
                RDFNode eO = stmtB.getObject();

                if (aO.isLiteral() && eO.isLiteral()) {
                    
                    //literal similarity check
                    if (!aO.equals(eO)) {
                        
                        //correct subject & predicate mapping
                        match.remove(aS);
                        match.remove(aP);
                        
                        return 1.0;
                    }

                } else if (aO.isResource() && eO.isResource()) {

                    //do nothing if equal
                    if(!aO.asResource().equals(eO.asResource())) {
                    
                        //resource object check 
                        if (match.containsKey(aO.asResource()) && !match.get(aO.asResource()).equals(eO.asResource()) || 
                            inv.containsKey(eO.asResource()) && !inv.get(eO.asResource()).equals(aO.asResource())) {

                            //correct subject & predicate mapping
                            match.remove(aS);
                            match.remove(aP);

                            return 1.0;
                        }
                    }
                    match.put(aO.asResource(), eO.asResource());

                } else {
                    
                    //correct subject & predicate mapping
                    match.remove(aS);
                    match.remove(aP);
                    
                    //object has not the same type
                    return 1.0;
                }

                return 0.0;
            }

        });

        matchingSolutions.addAll(possibleMatches);
    }

    private long complexity(long sizeA, long sizeB) {
        return CombinatoricsUtils.factorial((int) Math.max(sizeA, sizeB)) / CombinatoricsUtils.factorial((int) Math.abs(sizeA - sizeB));
    }

    private class Statistics {

        int skipRecursion;
        int checkCount;

        @Override
        public String toString() {
            return "Statistics{" + "skipRecursion=" + skipRecursion + ", checkCount=" + checkCount + '}';
        }

    }
    
    private class MaxPartialMatches {
        
        List<Map<Resource, Resource>> partialMatches;
        
        public MaxPartialMatches() {
            partialMatches = new ArrayList<>();
        }
        
        public int getPartialMatchSize() {
            if(partialMatches.isEmpty())
                return 0;
            
            return partialMatches.get(0).size();
        }
        
        //new since 2021-07-31
        public int getEqualMatchCount() {
            if(partialMatches.isEmpty())
                return 0;
            
            Map<Resource, Resource> pm = partialMatches.get(0);
            return getEqualMatchCount(pm);
        }
        
        //new since 2021-07-31
        public int getEqualMatchCount(Map<Resource, Resource> pm) {
            int sum = 0;
            for(Entry<Resource, Resource> entry : pm.entrySet()) {
                if(entry.getKey().equals(entry.getValue())) {
                    sum++;
                }
            }
            return sum;
        }
        
        public void add(Map<Resource, Resource> partialMatch) {
            if(partialMatch.isEmpty())
                return;
            
            int currentSize = getPartialMatchSize();
            int currentEqualMatchCount = getEqualMatchCount();
            
            if(partialMatch.size() > currentSize) {
                //reset and add anew
                partialMatches = new ArrayList<>();
                partialMatches.add(partialMatch);
                
            } else if(partialMatch.size() == currentSize) {
                
                if(getEqualMatchCount(partialMatch) > currentEqualMatchCount) {
                    //reset and add anew
                    partialMatches = new ArrayList<>();
                    partialMatches.add(partialMatch);
                } else {
                    //just add if size is equal
                    //but no duplicates
                    if(!partialMatches.contains(partialMatch)) {
                        partialMatches.add(partialMatch);
                    }
                }
            }
            //if smaller then do not add
        }
        
    }

    //https://www.javatpoint.com/permutation-and-combination-in-java
    private List<Map<Resource, Resource>> choose(List<Statement> a, List<Statement> b,
            double distanceThreshold, Map<Resource, Resource> match, StatementMatchDistance distanceFunction) {

        List<Map<Resource, Resource>> fullMatches = new ArrayList<>();

        boolean swap = false;
        if (a.size() < b.size()) {
            List<Statement> tmp = a;
            a = b;
            b = tmp;
            swap = true;
        }

        Statistics stats = new Statistics();
        MaxPartialMatches maxPartialMatches = new MaxPartialMatches();

        enumerate(a, b, a.size(), b.size(), swap, distanceFunction, distanceThreshold, match, fullMatches, maxPartialMatches, stats);

        //long complexity = complexity(a.size(), b.size());
        
        /*
        for(Map<Resource, Resource> fullMatch : fullMatches) {
            System.out.println("full match");
            fullMatch.forEach((x,y) -> System.out.println("\t" + x + "=" + y));
            System.out.println();
        }
        
        for(Map<Resource, Resource> partialMatch : maxPartialMatches.partialMatches) {
            System.out.println("partial match");
            partialMatch.forEach((x,y) -> System.out.println("\t" + x + "=" + y));
            System.out.println();
        }
        */
        
        //System.out.println("expected complexity: " + complexity + ", full solutions: " + fullMatches.size() + ", partial solutions:" + maxPartialMatches.partialMatches.size() +  ", stats: " + stats);

        //we return the maximal partial match (which can also be a full match)
        return maxPartialMatches.partialMatches;
    }

    //a is >= b in size
    //a is the one that is permuted
    private static <T> void enumerate(List<Statement> a, List<Statement> b, int n, int depth, boolean swap,
            StatementMatchDistance distanceFunction, double distanceThreshold,
            Map<Resource, Resource> match,
            List<Map<Resource, Resource>> fullMatches, MaxPartialMatches maxPartialMatches, Statistics stats) {
        
        //at the end of the recursion
        if (depth == 0) {
            //we are at the end of the permutation
            //and in match should be the current match

            //because we reached this depth there was no distance to large
            fullMatches.add(match);
            return;
        }

        for (int i = 0; i < n; i++) {
            swap(a, i, n - 1);

            //System.out.println("depth: " + depth + ", swap(" + i + "," + (n-1) + ")");
            
            //n-1 is fixed here for deeper recursion
            //the for loop will still increase i, so i is swapped with n-1 again
            //for the fixed ones we calculate distances of all statements
            //if the sum goes over a threshold we stop in this recursion depth
            double distanceSum = 0.0;
            //by default we have to go deeper in recursion
            //but maybe the distance is already above threshold
            boolean goDeeper = true;

            Map<Resource, Resource> tmpMatch = new HashMap<>(match);

            //n-1 and all to a.size() are fixed for this recursion
            for (int j = n - 1; j < a.size(); j++) {

                Statement aStmt = a.get(j);

                //we have to overlap them in this way:
                //a: [1,2,3,4,5]
                //b:     [1,2,3]
                //so they are aligned at the end
                int diff = a.size() - j;
                int bIndex = b.size() - diff; //b.size() - (a.size() - j) = |B| - |A| + j
                Statement bStmt = b.get(bIndex);

                stats.checkCount++;
                
                //we use the swap information so that the distance always
                //has the correct order and the tmpMatch is correctly filled
                Statement leftStmt = swap ? bStmt : aStmt;
                Statement rightStmt = swap ? aStmt : bStmt;

                //the distance function also fills the tmpMatch
                //the distance function has to revise the tmpMatch if the later elements do not match,
                //  e.g. s and p match but o does not so s and p also have to be removed from tmpMatch
                double distance = distanceFunction.distance(leftStmt, rightStmt, tmpMatch);

                distanceSum += distance;

                if (distanceSum > distanceThreshold) {
                    //do not go deeper because any other permutation based on that permutation 
                    //will be equal or larger in distance
                    goDeeper = false;
                    break;
                }
            }

            //System.out.println("depth: " + depth + ", distance: " + distanceSum + ", matches count: " + tmpMatch.size() + ", matches: " + tmpMatch);
            
            //add the tmpMatch to the partial matches to collect the maximal one
            maxPartialMatches.add(tmpMatch);
            
            //recursion: do it only if distance allows it
            if (goDeeper) {
                enumerate(a, b, n - 1, depth - 1, swap, distanceFunction, distanceThreshold, tmpMatch, fullMatches, maxPartialMatches, stats);
            } else {
                stats.skipRecursion++;
            }

            //change swap back to try another swap with an increased i in the next for loop
            swap(a, i, n - 1);

            //System.out.println("depth: " + depth + ", swap(" + i + "," + (n-1) + ")");
        }
    }

    private static <T> void swap(List<T> a, int i, int j) {
        T temp = a.get(i);
        a.set(i, a.get(j));
        a.set(j, temp);
    }

}
