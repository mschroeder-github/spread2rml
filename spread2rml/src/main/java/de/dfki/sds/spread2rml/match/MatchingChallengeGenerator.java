
package de.dfki.sds.spread2rml.match;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;

/**
 * 
 */
public class MatchingChallengeGenerator {
    
    private List<Consumer<MatchingChallenge>> strategies;
    
    public MatchingChallengeGenerator() {
        strategies = new ArrayList<>();
        loadStategies();
    }
    
    @Deprecated
    private void loadStategies() {
        strategies.add(mc -> {
            mc.setDescription("SPO");
            
            Resource s = r();
            Property p = p();
            Resource o = r();
            
            mc.getActual().add(s, p, o);
            
            mc.getMatching().put(s, r());
            mc.getMatching().put(p, p());
            mc.getMatching().put(o, r());
            
            Model expected = copy(mc.getActual());
            substitute(mc.getMatching(), expected);
            
            mc.setExpected(expected);
        });
        
        strategies.add(mc -> {
            mc.setDescription("SPL");
            
            Resource s = r();
            Property p = p();
            Literal l = strl();
            
            mc.getActual().add(s, p, l);
            
            mc.getMatching().put(s, r());
            mc.getMatching().put(p, p());
            
            Model expected = copy(mc.getActual());
            substitute(mc.getMatching(), expected);
            
            mc.setExpected(expected);
        });
        
        strategies.add(mc -> {
            mc.setDescription("SPOO");
            
            Resource s = r();
            Property p = p();
            
            Resource o1 = r();
            Resource o2 = r();
            
            mc.getActual().add(s, p, o1);
            mc.getActual().add(s, p, o2);
            
            mc.getMatching().put(s, r());
            mc.getMatching().put(p, p());
            mc.getMatching().put(o1, r());
            mc.getMatching().put(o2, r());
            
            Model expected = copy(mc.getActual());
            substitute(mc.getMatching(), expected);
            
            mc.setExpected(expected);
        });
        
        strategies.add(mc -> {
            mc.setDescription("SPOPL");
            
            Resource s = r();
            Property p = p();
            Resource o = r();
            Property p2 = p();
            Literal l = strl();
            
            mc.getActual().add(s, p, o);
            mc.getActual().add(o, p2, l);
            
            mc.getMatching().put(s, r());
            mc.getMatching().put(p, p());
            mc.getMatching().put(o, r());
            mc.getMatching().put(p2, p());
            
            Model expected = copy(mc.getActual());
            substitute(mc.getMatching(), expected);
            
            mc.setExpected(expected);
        });
        
        strategies.add(mc -> {
            mc.setDescription("SPLPL");
            
            Resource s = r();
            Property p1 = p();
            Property p2 = p();
            Literal l1 = strl();
            Literal l2 = strl();
            
            mc.getActual().add(s, p1, l1);
            mc.getActual().add(s, p2, l2);
            
            mc.getMatching().put(s, r());
            mc.getMatching().put(p1, p());
            mc.getMatching().put(p2, p());
            
            Model expected = copy(mc.getActual());
            substitute(mc.getMatching(), expected);
            
            mc.setExpected(expected);
        });
        
        strategies.add(mc -> {
            mc.setDescription("SPL SPL");
            
            Resource s1 = r();
            Resource s2 = r();
            Property p1 = p();
            Property p2 = p();
            Literal l1 = strl();
            Literal l2 = strl();
            
            mc.getActual().add(s1, p1, l1);
            mc.getActual().add(s2, p2, l2);
            
            mc.getMatching().put(s1, r());
            mc.getMatching().put(s2, r());
            mc.getMatching().put(p1, p());
            mc.getMatching().put(p2, p());
            
            Model expected = copy(mc.getActual());
            substitute(mc.getMatching(), expected);
            
            mc.setExpected(expected);
        });
        
        strategies.add(mc -> {
            mc.setDescription("SpL SpL");
            
            Resource s1 = r();
            Resource s2 = r();
            Property p = p();
            Literal l1 = strl();
            Literal l2 = strl();
            
            mc.getActual().add(s1, p, l1);
            mc.getActual().add(s2, p, l2);
            
            mc.getMatching().put(s1, r());
            mc.getMatching().put(s2, r());
            mc.getMatching().put(p, p());
            
            Model expected = copy(mc.getActual());
            substitute(mc.getMatching(), expected);
            
            mc.setExpected(expected);
        });
    }

    public List<MatchingChallenge> generate() {
        List<MatchingChallenge> l = new ArrayList<>();
        
        List<Model> collector = new ArrayList<>();
        Model model = ModelFactory.createDefaultModel();
        
        resetIds();
        generate(model, collector, 0, 4);
        
        //4,   6990 models (w/o literal reuse)
        //5, 267338 models (w/o literal reuse)
        
        
        /*
        for(Model m : collector) {
            System.out.println();
            System.out.println("=============");
            System.out.println(toTTL(m));
        }
        */
        //System.out.println(collector.size() + " models");
        
        /*
        for(Consumer<MatchingChallenge> strategy : strategies) {
            resetIds();
            MatchingChallenge mc = new MatchingChallenge();
            strategy.accept(mc);
            l.add(mc);
        }
        */
        
        int i = 0;
        for(Model actual : collector) {
            
            MatchingChallenge mc = new MatchingChallenge();
            mc.setDescription("Generation " + i);
            mc.setActual(actual);
            
            i++;
            
            for(Statement stmt : actual.listStatements().toList()) {
                
                List<Resource> rList = new ArrayList<>();
                rList.add(stmt.getSubject());
                rList.add(stmt.getPredicate());
                if(stmt.getObject().isResource()) {
                    rList.add(stmt.getObject().asResource());
                }
                
                for(Resource res : rList) {
                    if(!mc.getMatching().containsKey(res)) {
                        mc.getMatching().put(res, r());
                    }
                }
            }
            
            Model expected = copy(mc.getActual());
            substitute(mc.getMatching(), expected);
            
            mc.setExpected(expected);
            
            l.add(mc);
        }
        
        
        for(Model actual : collector) {
            
            if(actual.size() <= 1)
                continue;
            
            MatchingChallenge mc = new MatchingChallenge();
            mc.setDescription("Generation " + i + " actual stmt removed");
            mc.setActual(actual);
            
            i++;
            
            for(Statement stmt : actual.listStatements().toList()) {
                
                List<Resource> rList = new ArrayList<>();
                rList.add(stmt.getSubject());
                rList.add(stmt.getPredicate());
                if(stmt.getObject().isResource()) {
                    rList.add(stmt.getObject().asResource());
                }
                
                for(Resource res : rList) {
                    if(!mc.getMatching().containsKey(res)) {
                        mc.getMatching().put(res, r());
                    }
                }
            }
            
            Model expected = copy(mc.getActual());
            substitute(mc.getMatching(), expected);
            
            mc.setExpected(expected);
            
            Model ifRemovedModel = copy(mc.getActual());
            Statement toBeRemoved = mc.getActual().listStatements().toList().get(0);
            ifRemovedModel.remove(toBeRemoved);
            
            Set<Resource> allRes = MatchingApproach.getAllResources(ifRemovedModel);
            
            List<Resource> rList = new ArrayList<>();
            rList.add(toBeRemoved.getSubject());
            rList.add(toBeRemoved.getPredicate());
            if(toBeRemoved.getObject().isResource()) {
                rList.add(toBeRemoved.getObject().asResource());
            }
            
            for(Resource r : rList) {
                if(!allRes.contains(r)) {
                    mc.getMatching().remove(r);
                }
            }
            
            mc.setActual(ifRemovedModel);
            
            l.add(mc);
        }
        
        return l;
    }
    
    public void generate(Model model, List<Model> collector, int depth, int maxDepth) {
        if(depth >= maxDepth)
            return;
        
        Set<Property> properties = new HashSet<>();
        for(Statement stmt : model.listStatements().toList()) {
            properties.add(stmt.getPredicate());
        }
        
        Set<Literal> literals = new HashSet<>();
        for(RDFNode obj : model.listObjects().toList()) {
            if(obj.isLiteral()) {
                literals.add(obj.asLiteral());
            }
        }
        
        //everything is new
        generateSPO(copy(model), null, null, null, collector, depth + 1, maxDepth);
        generateSPL(copy(model), null, null, null, collector, depth + 1, maxDepth);
        
        //reuse object
        for(RDFNode obj : model.listObjects().toList()) {
            if(obj.isResource()) {
                generateSPO(copy(model), obj.asResource(), null, null, collector, depth + 1, maxDepth);
                generateSPL(copy(model), obj.asResource(), null, null, collector, depth + 1, maxDepth);
                
                //reuse literal
                //for(Literal l : literals) {
                //    generateSPL(copy(model), obj.asResource(), null, l, collector, depth + 1, maxDepth);
                //}
                
                //reuse property
                for(Property p : properties) {
                    generateSPO(copy(model), obj.asResource(), p, null, collector, depth + 1, maxDepth);
                    generateSPL(copy(model), obj.asResource(), p, null, collector, depth + 1, maxDepth);
                    
                    //reuse literal
                    //for(Literal l : literals) {
                    //    generateSPL(copy(model), obj.asResource(), p, l, collector, depth + 1, maxDepth);
                    //}
                }
            }
        }
        
        //reuse subject
        for(Resource sub : model.listSubjects().toList()) {
            generateSPO(copy(model), sub, null, null, collector, depth + 1, maxDepth);
            generateSPL(copy(model), sub, null, null, collector, depth + 1, maxDepth);
            
            //reuse literal
            //for(Literal l : literals) {
            //    generateSPL(copy(model), sub, null, l, collector, depth + 1, maxDepth);
            //}
            
            for(Property p : properties) {
                generateSPO(copy(model), sub, p, null, collector, depth + 1, maxDepth);
                generateSPL(copy(model), sub, p, null, collector, depth + 1, maxDepth);
                
                //reuse literal
                //for(Literal l : literals) {
                //    generateSPL(copy(model), sub, p, l, collector, depth + 1, maxDepth);
                //}
            }
        }
    }
    
    public void generateSPO(Model model, Resource s, Property p, Resource o, List<Model> collector, int depth, int maxDepth) {
        if(s == null)
            s = r();
        
        if(p == null)
            p = p();
        
        if(o == null)
            o = r();
        
        model.add(s, p, o);
        
        collector.add(model);
        
        generate(model, collector, depth, maxDepth);
    }
    
    public void generateSPL(Model model, Resource s, Property p, Literal l, List<Model> collector, int depth, int maxDepth) {
        if(s == null)
            s = r();
        
        if(p == null)
            p = p();
        
        if(l == null)
            l = strl();
        
        model.add(s, p, l);
        
        collector.add(model);
        
        generate(model, collector, depth, maxDepth);
    }
    
    
    public List<MatchingChallenge> load() {
        List<MatchingChallenge> l = new ArrayList<>();
        
        List<File> modelMatrixFileList = new ArrayList<>();
        
        try {
            Files.walk(Paths.get("datasets"))
                    .filter(f -> f.toFile().getName().endsWith("modelmatrix.csv"))
                    .forEach(f -> modelMatrixFileList.add(f.toFile()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        //Histogram<Integer> hist = new Histogram<>();
        
        int max = 25;
        
        int index = 0;
        
        for(File f : modelMatrixFileList) {
            
            if(l.size() > max)
                break;
            
            try {
                CSVParser p = CSVFormat.DEFAULT.parse(new FileReader(f));
                for(CSVRecord record : p.getRecords()) {
                    
                    if(l.size() > max)
                        break;
                    
                    for(int i = 0; i < record.size(); i++) {
                        
                        if(l.size() > max)
                            break;
                        
                        Reader r = new StringReader(record.get(i));
                        Model actual = ModelFactory.createDefaultModel().read(r, null, "TTL");
                        r.close();
                        
                        if(actual.isEmpty())
                            continue;
                        
                        
                        if(actual.size() < 10) {
                            continue;
                        }
                        
                        //hist.add(actual.size());
                        
                        MatchingChallenge mc = new MatchingChallenge();
                        mc.setDescription("Loaded " + index + " with " + actual.size());
                        mc.setActual(actual);

                        index++;

                        for(Statement stmt : actual.listStatements().toList()) {

                            List<Resource> rList = new ArrayList<>();
                            rList.add(stmt.getSubject());
                            rList.add(stmt.getPredicate());
                            if(stmt.getObject().isResource()) {
                                rList.add(stmt.getObject().asResource());
                            }

                            for(Resource res : rList) {
                                if(!mc.getMatching().containsKey(res)) {
                                    mc.getMatching().put(res, r());
                                }
                            }
                        }

                        Model expected = copy(mc.getActual());
                        substitute(mc.getMatching(), expected);

                        mc.setExpected(expected);

                        l.add(mc);
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            
        }
        
        //System.out.println(hist.toString());
        
        return l;
    }
    
    public List<MatchingChallenge> manual() {
        List<MatchingChallenge> l = new ArrayList<>();
        
        Resource a1 = r();
        Resource a2 = r();
        Resource a3 = r();
        Resource a4 = r();
        
        Property aP1 = p();
        Property aP2 = p();
        
        Literal lit1 = strl();
        Literal lit2 = strl();
        
        Resource e1 = r();
        Resource e2 = r();
        Resource e3 = r();
        Resource e4 = r();
        Resource e5 = r();
        
        Property eP1 = p();
        Property eP2 = p();
        
        MatchingChallenge mc = new MatchingChallenge();
        mc.setDescription("manual: partial match");
        mc.getActual().add(a1, aP1, lit1);
        mc.getActual().add(a2, aP1, lit2);
        mc.getActual().add(a3, aP2, a4);
        
        mc.getExpected().add(e1, eP1, lit1);
        mc.getExpected().add(e2, eP1, lit2);
        mc.getExpected().add(e3, eP2, strl());
        mc.getExpected().add(e4, eP2, strl());
        mc.getExpected().add(e5, eP1, strl());
        
        mc.getMatching().put(a1, e1);
        mc.getMatching().put(a2, e2);
        mc.getMatching().put(aP1, eP1);
        
        System.out.println(mc);
        //System.out.println();
        
        l.add(mc);
        
        
        /*
        <http://www.w3.org/2000/01/rdf-schema#label>
        a       <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> ;
        <http://www.w3.org/2000/01/rdf-schema#label>
                "label" .

        <urn:uuid:9b77f75e-5fb1-4cd1-a6c3-59cfdb7a81e6Label>
                <http://www.w3.org/2000/01/rdf-schema#label>
                        "label" .
        */
        MatchingChallenge mc2 = new MatchingChallenge();
        mc2.setDescription("manual: URI is equal, but also matched");
        
        mc2.getActual().read(new StringReader(
"<urn:uuid:9b77f75e-5fb1-4cd1-a6c3-59cfdb7a81e6Label>\n" +
"                <http://www.w3.org/2000/01/rdf-schema#label>\n" +
"                        \"label\" ."
        ), null, "TTL");
        
        
        mc2.getExpected().read(new StringReader(
"<http://www.w3.org/2000/01/rdf-schema#label>\n" +
"        a       <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> ;\n" +
"        <http://www.w3.org/2000/01/rdf-schema#label>\n" +
"                \"label\" ."
        ), null, "TTL");
        
        mc2.getMatching().put(
                mc2.getActual().getResource("urn:uuid:9b77f75e-5fb1-4cd1-a6c3-59cfdb7a81e6Label"), 
                mc2.getExpected().getResource("http://www.w3.org/2000/01/rdf-schema#label")
        );
        
        l.add(mc2);
        
        return l;
    }
    
    //--------------------
    
    private int resourceId;
    
    public void resetIds() {
        resourceId = 1;
    }
    
    public Resource r() {
        Resource r = ResourceFactory.createResource("urn:r:" + resourceId);
        resourceId++;
        return r;
    }
    
    public Property p() {
        Property p = ResourceFactory.createProperty("urn:p:" + resourceId);
        resourceId++;
        return p;
    }
    
    public Literal strl() {
        return ResourceFactory.createPlainLiteral(RandomStringUtils.randomAlphabetic(6));
    }
    
    public Literal intl() {
        return ResourceFactory.createTypedLiteral(new Random().nextInt());
    }
    
    //---------------------
    
    public static Model copy(Model model) {
        return ModelFactory.createDefaultModel().add(model);
    }
    
    public static String toTTL(Model model) {
        return toTTL(model, true);
    }
    
    public static String toTTL(Model model, boolean withPrefixHeader) {
        StringWriter sw = new StringWriter();
        model.write(sw, "TTL");
        String code = sw.toString().trim();
        
        if(!withPrefixHeader) {
            //remove @prefix
            while(code.startsWith("@prefix")) {
                int i = code.indexOf("\n");
                if(i == -1)
                    break;
                code = code.substring(i+1);
            }
            code = code.trim();
        }
        
        return code;
    }
    
    public static void substitute(Map<Resource, Resource> actual2expectedMapping, Model model) {
        List<Statement> statements = model.listStatements().toList();
        for (Statement oldStmt : statements) {
            Resource s = oldStmt.getSubject();
            Resource p = oldStmt.getPredicate();
            RDFNode o = oldStmt.getObject();

            boolean sSub = false;
            boolean pSub = false;
            boolean oSub = false;

            if (actual2expectedMapping.containsKey(s)) {
                s = actual2expectedMapping.get(s);
                sSub = true;
            }
            if (actual2expectedMapping.containsKey(p)) {
                p = actual2expectedMapping.get(p);
                pSub = true;
            }
            if (o.isResource() && actual2expectedMapping.containsKey(o.asResource())) {
                o = actual2expectedMapping.get(o.asResource());
                oSub = true;
            }

            if (sSub || pSub || oSub) {
                model.remove(oldStmt);
                
                Statement newStmt;
                
                if(p.isAnon()) {
                    newStmt = model.createStatement(s, p.as(Property.class), o);
                } else {
                    newStmt = model.createStatement(s, model.createProperty(p.getURI()), o);
                }
                
                model.add(newStmt);
            }
        }
    }
    
}
