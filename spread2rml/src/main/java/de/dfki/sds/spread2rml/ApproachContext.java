package de.dfki.sds.spread2rml;

import de.dfki.sds.spread2rml.match.MatchingApproachBruteForceV2;
import de.dfki.sds.spread2rml.match.MatchingChallengeGenerator;
import de.dfki.sds.mschroeder.commons.lang.StatisticUtility;
import java.awt.Point;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 */
public class ApproachContext {

    private Approach approach;
    private Dataset dataset;
    private File outputFolder;
    private Exception exception;

    private long begin;
    private long end;

    private Map<File, File> input2output;

    //actual to expected mapping
    //private Set<Resource> actualRows;
    //private Set<Resource> actualColumns;
    //private BidiMap<Resource, Resource> actualToExpectedBidiMapping;
    //private Map<Resource, Resource> actualToExpectedMapping;
    //private Map<Resource, Resource> propertyMapping;
    private List<Comparison<Model>> modelComparisons;

    private Map<String, Object> properties;

    private MatchingApproachBruteForceV2 matcher;
    
    private List<String> sheetNameWhitelist;

    public ApproachContext() {
        input2output = new HashMap<>();
        properties = new LinkedHashMap<>();
        //sheetComparisons = new ArrayList<>();
        matcher = new MatchingApproachBruteForceV2();
        sheetNameWhitelist = new ArrayList<>();
    }

    public void compare(List<Comparison<File>> fileComps, boolean writeHtml) throws IOException {
        modelComparisons = new ArrayList<>();
        
        //TokenizerFactory.makeTokenizerString(str)
        
        
        //actualRows = new HashSet<>();
        //actualColumns = new HashSet<>();
        //actualToExpectedBidiMapping = new DualHashBidiMap<>();
        //actualToExpectedMapping = new HashMap<>();
        //propertyMapping = new HashMap<>();
        for (Comparison<File> fileComp : fileComps) {

            List<CSVRecord> actRecords = CSVFormat.DEFAULT.parse(new FileReader(fileComp.getActual())).getRecords();
            List<CSVRecord> expRecords = CSVFormat.DEFAULT.parse(new FileReader(fileComp.getExpected())).getRecords();

            //for a new file new properties
            Map<Resource, Resource> internalPropertyMapping = new HashMap<>();

            System.out.println(fileComp + " with " + expRecords.size() + " rows");

            //row
            for (int i = 0; i < expRecords.size(); i++) {

                //System.out.println("row " + (i+1) + "/" + expRecords.size());
                CSVRecord expRecord = expRecords.get(i);
                CSVRecord actRecord = i >= actRecords.size() ? null : actRecords.get(i);

                //for each cell in row
                for (int j = 0; j < expRecord.size(); j++) {

                    if(j < dataset.getStartColumnIndex())
                        continue;
                    
                    //System.out.println("\tcol " + (j+1) + "/" + expRecord.size());
                    String expRdf = expRecord.get(j);
                    String actRdf = "";
                    if(actRecord != null) {
                        if(j < actRecord.size()) {
                            actRdf = actRecord.get(j);
                        }
                    }
                    
                    Model expModel = ModelFactory.createDefaultModel();
                    Model actModel = ModelFactory.createDefaultModel();

                    //takes some time to parse
                    if(!expRdf.isEmpty()) {
                        RDFDataMgr.read(expModel, new StringReader(expRdf), null, Lang.TURTLE);
                    }
                    
                    if(!actRdf.isEmpty()) {
                        RDFDataMgr.read(actModel, new StringReader(actRdf), null, Lang.TURTLE);
                    }
                    
                    Comparison<Model> modelComp = new Comparison<>(actModel, expModel);
                    modelComp.setParent(fileComp);
                    modelComp.setLocation(new Point(j, i));
                    modelComparisons.add(modelComp);

                    /*
                    //maybe
                    List<Map<Resource, Resource>> solutions = new ArrayList<>();
                    matcher.match(actModel, expModel, solutions);
                    modelComp.setSolutions(solutions);
                     */
                    
                    if (i == 0) {
                        //first row = header line = columns = properties 
                        matchProperties(actModel, expModel, modelComp, internalPropertyMapping);

                    } else {
                        //row line
                        matchRows(actModel, expModel, modelComp, internalPropertyMapping);
                    }

                }//for each col

            } //rows

        } //for file comparison

        //System.out.println(modelComparisons.size() + " model comparisons, " + MemoryUtility.memoryStatistics());
        int tp = 0;
        int fp = 0;
        int fn = 0;
        int P = 0;
        int R = 0;

        for (Comparison<Model> modelComp : modelComparisons) {

            ModelConfusionMatrix bestConfMatrix = null;
            
            //having no solution is like having an empty solution: nothing matches
            if(modelComp.getSolutions().isEmpty()) {
                modelComp.getSolutions().add(new HashMap<>());
            }

            for (Map<Resource, Resource> solution : modelComp.getSolutions()) {

                //use matching solution and change URIs accordingly
                Model actualCopy = MatchingChallengeGenerator.copy(modelComp.getActual());
                substitute(solution, actualCopy);

                //calculate confusion matrix
                ModelConfusionMatrix confmatrix = ModelConfusionMatrix.get(actualCopy, modelComp.getExpected());
                modelComp.getSolution2confmatrix().put(solution, confmatrix);
                confmatrix.setMatch(solution);

                //get the one with highest f-score
                bestConfMatrix = ModelConfusionMatrix.max(confmatrix, bestConfMatrix);
            }

            if (bestConfMatrix != null) {
                modelComp.setBestConfMatrix(bestConfMatrix);

                //if(bestConfMatrix.hasError()) {
                //    System.out.println(bestConfMatrix.getTTLComparison());
                //    int a = 0;
                //}
                tp += bestConfMatrix.getTP();
                fp += bestConfMatrix.getFP();
                fn += bestConfMatrix.getFN();

                P += modelComp.getExpected().size();
                R += modelComp.getActual().size();
            }
        }

        //accuracy = tp + tn / P + N
        double accuracy = tp / (double) P;
        double precision = tp / (double) (tp + fp);
        double recall = tp / (double) (tp + fn);

        properties.put("File Comparisons", fileComps.size());
        properties.put("Model Comparisons", modelComparisons.size());
        properties.put("True Positive Statements", tp);
        properties.put("True Positive Statements", tp);
        properties.put("False Positive Statements", fp);
        properties.put("False Negative Statements", fn);
        properties.put("Relevant Statements", P);
        properties.put("Retrieved Statements", R);
        properties.put("Accuracy", accuracy);
        properties.put("Precision", precision);
        properties.put("Recall", recall);
        properties.put("F-Score", StatisticUtility.fscore(precision, recall));

        //System.out.println(matchedRatio + " " + tp + "/" + T + ", " + MemoryUtility.memoryStatistics());
        if(writeHtml) {
            FileWriter fw = new FileWriter(new File(outputFolder, "comparison.html"));
            saveHtml(modelComparisons, properties, fw);
            fw.close();
        }

        //cleanup to free memory
        modelComparisons.clear();
        //actualRows.clear();
        //actualColumns.clear();
        //actualToExpectedBidiMapping.clear();
        //actualToExpectedMapping.clear();
    }

    public void matchProperties(Model actModel, Model expModel, Comparison<Model> modelComp, Map<Resource, Resource> propertyMapping) {

        List<Map<Resource, Resource>> solutions = new ArrayList<>();
        matcher.match(actModel, expModel, solutions);
        modelComp.setSolutions(solutions);

        if (solutions.size() > 1) {
            //if it is ambiguous we should not add it
            //we maybe pick the wrong one
            //if there is no solution we will later map properties in row matches, I guess
            
        } else if (solutions.size() == 1) {
            //good match, just fill this propery mapping map, it is used in matchRows
            propertyMapping.putAll(solutions.get(0));
        }
    }

    public void matchRows(Model actModel, Model expModel, Comparison<Model> modelComp, Map<Resource, Resource> propertyMapping) {

        List<Map<Resource, Resource>> solutions = new ArrayList<>();
        //we use the pre map propertyMapping to make it easier to match correctly
        matcher.match(actModel, expModel, new HashMap<>(propertyMapping), solutions);
        modelComp.setSolutions(solutions);
    }

    public static void substitute(Map<Resource, Resource> actual2expectedMapping, Model model) {
        //nothing to do if empty
        if(actual2expectedMapping.isEmpty())
            return;
        
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
                Statement newStmt = model.createStatement(s, model.createProperty(p.getURI()), o);
                model.add(newStmt);
            }
        }
    }

    private void saveHtml(List<Comparison<Model>> modelComparisons, Map<String, Object> properties, Writer w) throws IOException {
        w.append("<html>\n");

        w.append("<head>\n");
        w.append("<meta charset='utf-8'/>\n");
        w.append("</head>\n");

        w.append("<style>\n");
        w.append("table, td {\n"
                + "  border: 1px solid black;\n"
                + "}\n");
        w.append(".expected { background-color: #d4edda; }\n"); //green
        w.append(".matching { background-color: #d4edda; }\n"); //green
        w.append(".actual   { background-color: #e2e3e5; }\n"); //gray
        w.append(".missing  { background-color: #f8d7da; }\n"); //red
        w.append(".falseNegative  { background-color: #f8d7da; }\n"); //red
        w.append(".falsePositive  { background-color: #cce5ff; }\n"); //blue
        w.append("pre       { padding: 5px; }\n");
        w.append("td       { vertical-align: top; }\n");
        w.append("</style>\n");

        w.append("<body>\n");

        w.append("<table>\n");
        for (Entry<String, Object> property : properties.entrySet()) {
            w.append("<tr>\n");
            w.append("<td>").append(property.getKey() + "</td><td>" + property.getValue()).append("</td>\n");
            w.append("</tr>\n");
        }
        w.append("</table>\n");
        w.append("<br/>\n");

        //Mappings
        /*
        List<Entry<Resource, Resource>> mappingList = new ArrayList<>(); //actualToExpectedMapping.entrySet()
        mappingList.sort((a, b) -> a.getKey().getURI().compareTo(b.getKey().getURI()));
        w.append("<div style='height: 400px; overflow-y: auto;'>\n");
        w.append("<table>\n");
        for (Entry<Resource, Resource> entry : mappingList) {
            w.append("<tr>\n");
            w.append("<td>").append(entry.getKey().getURI()).append("</td>\n");
            w.append("<td>").append(entry.getValue().getURI()).append("</td>\n");
            //w.append("<td>").append(actualColumns.contains(entry.getKey()) ? "Column" : "Row").append("</td>\n");
            w.append("</tr>\n");
        }
        w.append("</table>\n");
        w.append("</div>\n");
        w.append("<br/>\n");
        */

        //TOC
        List<Comparison> parents = new ArrayList<>();
        for (Comparison<Model> modelComp : modelComparisons) {
            if (!parents.contains(modelComp.getParent())) {
                parents.add(modelComp.getParent());
            }
        }
        w.append("<ul>\n");
        for (Comparison comp : parents) {
            File f = (File) comp.getActual();
            w.append("<li><a href='#" + f.getName() + "'>").append(f.getName()).append("</a></li>");
        }
        w.append("</ul>\n");
        w.append("<br/>\n");

        Comparison parent = null;
        int lastY = -1;
        for (Comparison<Model> modelComp : modelComparisons) {

            if (modelComp.getParent() != parent) {
                if (parent != null) {
                    w.append("</tr>\n");
                    w.append("</tbody>\n");
                    w.append("</table>\n");
                    w.append("<br/>\n");
                    w.append("<br/>\n");
                }

                File actual = (File) modelComp.getParent().getActual();

                w.append("<h3 id='" + actual.getName() + "'>" + actual.getName() + "</h3>\n");
                w.append("<table>\n");
                w.append("<tbody>\n");

                parent = modelComp.getParent();
                lastY = -1;
            }

            if (lastY != modelComp.getLocation().y) {

                if (lastY != -1) {
                    w.append("</tr>\n");
                }

                lastY = modelComp.getLocation().y;
            }

            w.append("<td x='" + modelComp.getLocation().x + "' y='" + modelComp.getLocation().y + "'>\n");

            if (!modelComp.isBothNull()
                    && !(modelComp.getExpected().size() == 0 && modelComp.getActual().size() == 0)) {

                ModelConfusionMatrix confMatrix = modelComp.getBestConfMatrix();

                StringJoiner sj = new StringJoiner(", ");
                if (confMatrix != null) {
                    sj.add(confMatrix.getTruePositive().size() + " true positive");
                    sj.add(confMatrix.getFalseNegative().size() + " false negative (miss)");
                    sj.add(confMatrix.getFalsePositive().size() + " false positive (false alarm)");
                    sj.add(String.format(Locale.ENGLISH, "%.2f precision", confMatrix.getPrecision()));
                    sj.add(String.format(Locale.ENGLISH, "%.2f recall", confMatrix.getRecall()));
                } else {
                    sj.add("confMatrix is null");
                }
                sj.add(modelComp.getExpected().size() + " relevant");
                sj.add(modelComp.getActual().size() + " retrieved");

                w.append(sj.toString() + "<br/>\n");

                if (confMatrix != null) {
                    //w.append(confMatrix.getMatch() + "<br/>\n");
                    
                    if (!confMatrix.getFalseNegative().isEmpty()) {
                        w.append("<pre class='falseNegative'>\n");
                        w.append(StringEscapeUtils.escapeHtml4(MatchingChallengeGenerator.toTTL(confMatrix.getFalseNegative())));
                        w.append("</pre>\n");
                    }
                    
                    if (!confMatrix.getFalsePositive().isEmpty()) {
                        w.append("<pre class='falsePositive'>\n");
                        w.append(StringEscapeUtils.escapeHtml4(MatchingChallengeGenerator.toTTL(confMatrix.getFalsePositive())));
                        w.append("</pre>\n");
                    }

                } else {
                    w.append("<pre class='expected'>\n");
                    w.append(StringEscapeUtils.escapeHtml4(MatchingChallengeGenerator.toTTL(modelComp.getExpected())));
                    w.append("</pre>\n");

                    w.append("<pre class='actual'>\n");
                    w.append(StringEscapeUtils.escapeHtml4(MatchingChallengeGenerator.toTTL(modelComp.getActual())));
                    w.append("</pre>\n");
                }

            } else {
                w.append("(empty)\n");
            }

            w.append("</td>\n");
        }

        /*
        for(int y = 0; y < matrix.length; y++) {
            w.append("<tr>\n");
            for(int x = 0; x < matrix[y].length; x++) {
                w.append("<td x='"+x+"' y='"+y+"'>\n");
                CellComparison cc = matrix[y][x];
                
                if(cc == null || cc.isBothNull()) {
                    w.append("</td>\n");
                    continue;
                }
                
                Model actualModel = cc.getActualModel(fullMap);
                StringWriter sw = new StringWriter();
                actualModel.write(sw, "TTL");
                
                w.append("<pre class='actual'>\n");
                w.append(StringEscapeUtils.escapeHtml4(sw.toString()));
                w.append("</pre>\n");
                
                w.append("<pre class='expected'>\n");
                w.append(StringEscapeUtils.escapeHtml4(cc.getExpectedString()));
                w.append("</pre>\n");
                
                if(cc.getMissingStmtCount() > 0) {
                    w.append("<pre class='missing'>\n");
                    w.append(StringEscapeUtils.escapeHtml4(cc.getMissingString()));
                    w.append("</pre>\n");
                }
                
                w.append("</td>\n");
            
            }
            w.append("</tr>\n");
        }
         */
        w.append("</body>\n");

        w.append("</html>\n");
    }

    public Approach getApproach() {
        return approach;
    }

    public void setApproach(Approach approach) {
        this.approach = approach;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public Dataset getRootDataset() {
        Dataset cur = dataset;
        while (cur.getParent() != null) {
            cur = cur.getParent();
        }
        return cur;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public File getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
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

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getDuration() {
        return end - begin;
    }

    public Map<File, File> getInput2output() {
        return input2output;
    }

    //public List<SheetComparison> getSheetComparisons() {
    //    return sheetComparisons;
    //}
    public void save(File file) throws IOException {

        JSONObject ctxObj = new JSONObject();

        ctxObj.put("approach", approach.getClass().getName());

        ctxObj.put("outputFolder", outputFolder);
        if (hasException()) {
            ctxObj.put("exception", exception.getMessage());
        }
        ctxObj.put("dataset", toJSON(dataset));

        ctxObj.put("begin", begin);
        ctxObj.put("end", end);

        JSONObject i2o = new JSONObject();
        for (Entry<File, File> entry : input2output.entrySet()) {
            i2o.put(entry.getKey().toString(), entry.getValue());
        }
        ctxObj.put("input2output", i2o);

        FileUtils.writeStringToFile(file, ctxObj.toString(2), StandardCharsets.UTF_8);
    }

    private static JSONObject toJSON(Dataset ds) {
        JSONObject dsObj = new JSONObject();
        dsObj.put("expectedRdfFile", ds.getExpectedRdfFile());
        dsObj.put("folder", ds.getFolder());
        JSONObject props = new JSONObject();
        for (Entry<String, Object> entry : ds.getProperties().entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }
        dsObj.put("properties", props);
        dsObj.put("name", ds.getName());
        dsObj.put("abbrev", ds.getAbbrev());
        dsObj.put("tags", new JSONArray(ds.getTags()));
        dsObj.put("workbookFile", ds.getWorkbookFile());
        dsObj.put("provenanceCsvFile", ds.getProvenanceCsvFile());
        dsObj.put("startColumnIndex", ds.getStartColumnIndex());

        if (ds.getParent() != null) {
            dsObj.put("parent", toJSON(ds.getParent()));
        }

        return dsObj;
    }

    private static Dataset fromJSON(JSONObject dsObj) {
        Dataset ds = new Dataset();
        if (dsObj.has("expectedRdfFile")) {
            ds.setExpectedRdfFile(new File(dsObj.getString("expectedRdfFile")));
        }
        if (dsObj.has("folder")) {
            ds.setFolder(new File(dsObj.getString("folder")));
        }
        if (dsObj.has("workbookFile")) {
            ds.setWorkbookFile(new File(dsObj.getString("workbookFile")));
        }
        if (dsObj.has("provenanceCsvFile")) {
            ds.setProvenanceCsvFile(new File(dsObj.getString("provenanceCsvFile")));
        }
        if (dsObj.has("startColumnIndex")) {
            ds.setStartColumnIndex(dsObj.getInt("startColumnIndex"));
        }

        JSONObject props = dsObj.getJSONObject("properties");
        for (String key : props.keySet()) {
            Object value = props.get(key);
            ds.getProperties().put(key, value);
        }

        ds.setName(dsObj.getString("name"));
        ds.setAbbrev(dsObj.getString("abbrev"));
        dsObj.getJSONArray("tags").toList().forEach(o -> ds.getTags().add((String) o));

        if (dsObj.has("parent")) {
            ds.setParent(fromJSON(dsObj.getJSONObject("parent")));
        }

        return ds;
    }

    public static ApproachContext read(File file) throws IOException {
        ApproachContext ctx = new ApproachContext();

        String jsonStr = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(jsonStr);

        try {
            Class<Approach> cls = (Class<Approach>) Approach.class.getClassLoader().loadClass(json.getString("approach"));
            ctx.setApproach(cls.newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }

        ctx.dataset = fromJSON(json.getJSONObject("dataset"));

        if (json.has("outputFolder")) {
            ctx.setOutputFolder(new File(json.getString("outputFolder")));
        }

        if (json.has("exception")) {
            ctx.setException(new Exception(json.getString("exception")));
        }

        ctx.setBegin(json.getLong("begin"));
        ctx.setEnd(json.getLong("end"));

        JSONObject i2o = json.getJSONObject("input2output");
        for (String key : i2o.keySet()) {
            String value = i2o.getString(key);
            ctx.getInput2output().put(new File(key), new File(value));
        }

        return ctx;
    }

    public List<File> getModelMatrixFiles() {
        List<File> files = new ArrayList<>();
        for (File f : outputFolder.listFiles()) {
            if (f.getName().endsWith("modelmatrix.csv")) {
                files.add(f);
            }
        }
        return files;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public List<String> getSheetNameWhitelist() {
        return sheetNameWhitelist;
    }

}
