
package de.dfki.sds.spread2rml.demo;

import de.dfki.sds.spread2rml.match.MatchingApproach;
import de.dfki.sds.spread2rml.match.MatchingApproachBruteForceV2;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Spark;

/**
 * 
 */
public class Spread2RMLServer {
    
    private static final String ROOT_PATH = "/de/dfki/sds/spread2rml";

    //Spread RML
    private int port = (int) 'S' * 100 + (int) 'R';

    private File folderRuns;
    private File folderExceptions;

    public Spread2RMLServer(String[] args) {
        folderRuns = new File("runs");
        folderRuns.mkdir();
        
        folderExceptions = new File("exceptions");
        folderExceptions.mkdir();
    }
    
    public void start() {
        Spark.port(port);

        Spark.exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
            response.body(exception.getMessage());
        });

        Spark.staticFiles.location(ROOT_PATH + "/web");

        Spark.before((req, res) -> {
            String path = req.pathInfo();
            if (!path.equals("/") && path.endsWith("/")) {
                res.redirect(path.substring(0, path.length() - 1));
            }
        });

        Spark.awaitInitialization();
        System.out.println("server running at localhost:" + port);

        Spark.post("/runMatching", this::runMatching);
    }
    
    private Object runMatching(Request req, Response resp) {

        JSONObject json = new JSONObject(req.body());
        
        JSONObject result = new JSONObject();
        
        JSONArray solutionsArray = new JSONArray();
        
        try {
            String matchGraphA = json.getString("matchGraphA");
            String matchGraphB = json.getString("matchGraphB");
            
            Model graphA = ModelFactory.createDefaultModel().read(new StringReader(matchGraphA), "https://example.org/", "TTL");
            Model graphB = ModelFactory.createDefaultModel().read(new StringReader(matchGraphB), "https://example.org/", "TTL");
            
            MatchingApproach matchingApproach = new MatchingApproachBruteForceV2();
            
            List<Map<Resource, Resource>> matchingSolutions = new ArrayList<>();
            matchingApproach.match(graphA, graphB, matchingSolutions);
            
            for(Map<Resource, Resource> solution : matchingSolutions) {
                List<Entry<Resource, Resource>> entries = new ArrayList<>(solution.entrySet());
                entries.sort((a,b) -> a.getKey().getURI().compareTo(b.getKey().getURI()));
                
                JSONArray solutionArray = new JSONArray();
                
                for(Entry<Resource, Resource> entry : entries) {
                    JSONArray entryArray = new JSONArray();
                    entryArray.put(entry.getKey().getURI());
                    entryArray.put(entry.getValue().getURI());
                    
                    solutionArray.put(entryArray);
                }
                
                solutionsArray.put(solutionArray);
            }
            
            result.put("solutions", solutionsArray);
            
        } catch (Exception | Error ex) {
            String stackTrace = ExceptionUtils.getStackTrace(ex);
            
            //write exception for later checking
            File f = new File(folderExceptions, LocalDateTime.now().toString() + ".txt");
            try {
                FileUtils.writeStringToFile(f, stackTrace, StandardCharsets.UTF_8);
            } catch (IOException ex1) {
                //ignore
            }
            
            result.put("exception", stackTrace);
        }
        
        resp.type("application/json");
        return result.toString(2);
    }
    
    
    public static void main(String[] args) throws Exception {
        server(args);
    }
    
    private static void server(String[] args) {
        Spread2RMLServer server = new Spread2RMLServer(args);
        server.start();
    }
    
}
