package de.dfki.sds.spread2rml;

import de.dfki.sds.spread2rml.util.MemoryUtility;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.tika.utils.ExceptionUtils;

/**
 *
 */
public class Evaluation {

    private File datasetsFolder;
    private File resultsFolder;

    private List<Dataset> datasets;
    private List<Approach> approaches;
    private List<ApproachContext> contexts;

    public Evaluation() {
        datasetsFolder = new File("datasets");
        resultsFolder = new File("results");

        datasets = new ArrayList<>();
        approaches = new ArrayList<>();
        contexts = new ArrayList<>();

        loadDatasets();
        loadApproaches();
    }

    private void loadDatasets() {
        System.out.println("Datasets");
        
        loadDataGov();
        
        loadDatasproutDatasets();
        
        //omitted because of private data
        //loadIndustry();

        //but also the ones from data.gov
        System.out.println(datasets.size() + " datasets loaded, " + MemoryUtility.memoryStatistics());
        System.out.println();
    }

    private void loadDatasproutDatasets() {

        File folder = new File(datasetsFolder, "datasprout");

        int i = 0;

        for (File ds : folder.listFiles()) {
            for (File modeFolder : ds.listFiles()) {

                if (modeFolder.getName().equals("All_ProvenanceAsCellComment")) {
                    continue;
                }

                String mode = modeFolder.getName().replace("SinglePattern_", "");

                Dataset dataset = new Dataset();
                dataset.setFolder(modeFolder);
                dataset.setName("datasprout " + ds.getName() + " " + mode);
                dataset.setAbbrev("DS" + i);
                dataset.getProperties().put("mode", mode);
                dataset.getProperties().put("source", ds.getName());
                dataset.getProperties().put("generator", "datasprout");
                dataset.setWorkbookFile(new File(modeFolder, "workbook.xlsx"));
                dataset.getTags().add("xlsx");
                dataset.setExpectedRdfFile(new File(modeFolder, "expected.ttl.gz"));
                dataset.setProvenanceCsvFile(new File(modeFolder, "provenance.csv.gz"));

                datasets.add(dataset);

                File xlsx2csvDir = new File(modeFolder, "xlsx2csv");
                if (xlsx2csvDir.exists()) {
                    Dataset child = new Dataset();
                    child.getProperties().put("mode", mode);
                    child.getProperties().put("preprocessing", "xlsx2csv");
                    child.getProperties().put("source", ds.getName());
                    child.getProperties().put("generator", "datasprout");
                    child.setFolder(xlsx2csvDir);
                    child.setName(dataset.getName() + " with xlsx2csv");
                    child.setAbbrev(dataset.getAbbrev() + "-x2c");
                    child.getTags().add("csv");
                    child.setParent(dataset);

                    datasets.add(child);
                }

                File ssconvertDir = new File(modeFolder, "ssconvert");
                if (ssconvertDir.exists()) {
                    Dataset child = new Dataset();
                    child.getProperties().put("mode", mode);
                    child.getProperties().put("preprocessing", "ssconvert");
                    child.getProperties().put("source", ds.getName());
                    child.getProperties().put("generator", "datasprout");
                    child.setFolder(ssconvertDir);
                    child.setName(dataset.getName() + " with ssconvert");
                    child.setAbbrev(dataset.getAbbrev() + "-ssc");
                    child.getTags().add("csv");
                    child.setParent(dataset);

                    datasets.add(child);
                }

                i++;
            }
        }
    }

    private void loadDataGov() {
        File datagov = new File("datasets/data.gov");
        
        int i = 0;
        
        for(File f : datagov.listFiles()) {
            
            if(f.isFile())
                continue;
            
            Dataset dataset = new Dataset();
            dataset.setFolder(f);
            dataset.setWorkbookFile(new File(datagov, f.getName() + ".xls"));
            dataset.setName("data.gov " + f.getName());
            dataset.setAbbrev("DG" + i);
            dataset.getTags().add("xlsx");
            
            if(f.getName().equals("bc01528b-32e3-462c-acf3-70cb5c3881e9")) {
                dataset.setStartColumnIndex(2);
            }
            
            i++;
            
            datasets.add(dataset);
            
            File xlsx2csvDir = new File(f, "xlsx2csv");
            if (xlsx2csvDir.exists()) {
                Dataset child = new Dataset();
                child.setFolder(xlsx2csvDir);
                child.setName(dataset.getName() + " with xlsx2csv");
                child.setAbbrev(dataset.getAbbrev() + "-x2c");
                child.getProperties().put("preprocessing", "xlsx2csv");
                child.getTags().add("csv");
                child.setParent(dataset);

                datasets.add(child);
            }

            File ssconvertDir = new File(f, "ssconvert");
            if (ssconvertDir.exists()) {
                Dataset child = new Dataset();
                child.setFolder(ssconvertDir);
                child.setName(dataset.getName() + " with ssconvert");
                child.getProperties().put("preprocessing", "ssconvert");
                child.setAbbrev(dataset.getAbbrev() + "-ssc");
                child.getTags().add("csv");
                child.setParent(dataset);

                datasets.add(child);
            }
            
        }
        
    }
    
    private void loadApproaches() {
        approaches.add(new ApproachSpread2RML());
        approaches.add(new ApproachAny23());
        //approaches.add(new ApproachGTChangedURIs());
    }

    //--------------------
    public void createModelMatrixFromGroundTruth() throws IOException {
        int size = datasets.size();
        int num = 0;
        for (Dataset dataset : datasets) {
            if (dataset.getProvenanceCsvFile() == null) {
                size--;
                continue;
            }

            dataset.loadProvenanceCsv();

            Dataset.saveModelMatrixMap(dataset.getSheet2modelMatrix(), dataset.getFolder());

            dataset.clearProvenance();

            num++;

            System.out.println(num + "/" + size + ": " + dataset.getFolder());
        }
    }

    public void clearResultsFolder() {
        FileUtils.deleteQuietly(resultsFolder);
        resultsFolder.mkdirs();
    }

    public void runApproachesOnDatasets() {
        runApproachesOnDatasets(null);
    }
    
    public void runApproachesOnDatasets(Predicate<String> nameFilter) {
        System.out.println("Run Approaches on Datasets");

        List<ApproachContext> ctxs = new ArrayList<>();

        //get all approach context combis
        for (Approach approach : approaches) {
            for (Dataset dataset : datasets) {

                if (!approach.on(dataset)) {
                    continue;
                }

                ApproachContext ctx = new ApproachContext();
                ctx.setApproach(approach);
                ctx.setDataset(dataset);
                
                //only those that have a modelmatrix
                for(File f : dataset.getModelMatrixFiles()) {
                    ctx.getSheetNameWhitelist().add(f.getName().substring(0, f.getName().length() - ".modelmatrix.csv".length()));
                }
                
                ctxs.add(ctx);
            }
        }
        
        System.out.println(ctxs.size() + " approach-dataset contexts");

        for (ApproachContext ctx : ctxs) {
            
            //cleanup folder
            File outputFolder = new File(resultsFolder, ctx.getApproach().getName() + " " + ctx.getDataset().getName());
            
            if(nameFilter != null && !nameFilter.test(outputFolder.getName())) {
                continue;
            }
            
            System.out.println((ctxs.indexOf(ctx)+1) + "/" + ctxs.size() + ": " + ctx.getApproach().getName() + " on " + ctx.getDataset().getName() + " " + MemoryUtility.memoryStatistics());
            
            FileUtils.deleteQuietly(outputFolder);
            outputFolder.mkdirs();

            ctx.setOutputFolder(outputFolder);

            contexts.add(ctx);

            long begin = System.currentTimeMillis();
            try {
                ctx.getApproach().run(ctx);
            } catch (Exception e) {
                ctx.setException(e);
                e.printStackTrace();
            }
            long end = System.currentTimeMillis();

            ctx.setBegin(begin);
            ctx.setEnd(end);

            try {
                ctx.getApproach().save(ctx, outputFolder);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    public void runEvaluationOnResults() throws IOException {
        runEvaluationOnResults(false, null);
    }
    
    public void runEvaluationOnResults(boolean writeHtml, Predicate<String> nameFilter) throws IOException {
        System.out.println("Evaluation on Results");

        CSVPrinter csv = CSVFormat.DEFAULT.print(new File("evaluation-results.csv"), StandardCharsets.UTF_8);

        csv.printRecord(
                
                //7
                "name",
                "abbrev",
                "generator",
                "source",
                "mode",
                "preprocessing",
                "approach",
                
                //11
                "accuracy",
                "precision",
                "recall",
                "f-score",
                "tp",
                "fp",
                "fn",
                "relevant",
                "retrieved",
                "file-comp",
                "mod-comp",
                
                //4
                "approach duration",
                "evaluation duration",
                "approach exception",
                "evaluation exception"
        );

        int index = 0;
        File[] files = resultsFolder.listFiles();
        for (File resultFolder : files) {
            index++;

            if (!resultFolder.isDirectory()) {
                continue;
            }

            File ctxFile = new File(resultFolder, "ctx.json");
            if (!ctxFile.exists()) {
                continue;
            }

            if(nameFilter != null && !nameFilter.test(resultFolder.getName())) {
                continue;
            }
            
            System.out.println(index + "/" + files.length + ": " + resultFolder + " " + MemoryUtility.memoryStatistics());
            
            csv.print(resultFolder);

            long begin = System.currentTimeMillis();

            try {
                ApproachContext ctx = ApproachContext.read(ctxFile);

                Dataset dataset = ctx.getRootDataset();

                List<File> expectedMMFiles = dataset.getModelMatrixFiles();
                List<File> actualMMFiles = ctx.getModelMatrixFiles();

                List<Comparison<File>> fileComps = new ArrayList<>();

                for (File expFile : expectedMMFiles) {
                    for (File actFile : actualMMFiles) {
                        if (expFile.getName().equals(actFile.getName())) {
                            fileComps.add(new Comparison<>(actFile, expFile));
                            break;
                        }
                    }
                }

                ctx.compare(fileComps, writeHtml);

                long duration = System.currentTimeMillis() - begin;
                
                //free memory
                System.gc();

                //what dataset + what approach (7), here 6
                csv.print(ctx.getDataset().getAbbrev());
                csv.print(ctx.getDataset().getProperties().getOrDefault("generator", ""));
                csv.print(ctx.getDataset().getProperties().getOrDefault("source", ""));
                csv.print(ctx.getDataset().getProperties().getOrDefault("mode", ""));
                csv.print(ctx.getDataset().getProperties().getOrDefault("preprocessing", ""));
                csv.print(ctx.getApproach().getName());

                //results (11)
                csv.print(ctx.getProperties().getOrDefault("Accuracy", 0));
                csv.print(ctx.getProperties().getOrDefault("Precision", 0));
                csv.print(ctx.getProperties().getOrDefault("Recall", 0));
                csv.print(ctx.getProperties().getOrDefault("F-Score", 0));
                csv.print(ctx.getProperties().getOrDefault("True Positive Statements", 0));
                csv.print(ctx.getProperties().getOrDefault("False Positive Statements", 0));
                csv.print(ctx.getProperties().getOrDefault("False Negative Statements", 0));
                csv.print(ctx.getProperties().getOrDefault("Relevant Statements", 0));
                csv.print(ctx.getProperties().getOrDefault("Retrieved Statements", 0));
                csv.print(ctx.getProperties().getOrDefault("File Comparisons", 0));
                csv.print(ctx.getProperties().getOrDefault("Model Comparisons", 0));

                //time + exceptions
                csv.print(ctx.getDuration());
                csv.print(duration);
                csv.print(ctx.hasException() ? ExceptionUtils.getStackTrace(ctx.getException()) : "");
                csv.print("");

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - begin;

                //what dataset + what approach (7)
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");

                //results (11)
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");
                csv.print("");

                //time + exceptions (4)
                csv.print("");
                csv.print(duration);
                csv.print("");
                csv.print(ExceptionUtils.getStackTrace(e));
                
                System.err.println(ExceptionUtils.getStackTrace(e));
            }
            
            csv.println();
            
            csv.flush();
        }

        csv.close();
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public List<Approach> getApproaches() {
        return approaches;
    }

    public List<ApproachContext> getContexts() {
        return contexts;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();

        sb.append(datasets.size() + " datasets\n");
        sb.append(contexts.size() + " contexts\n\n");
        for (ApproachContext ctx : contexts) {
            sb.append(ctx.getApproach().getName() + " on " + ctx.getDataset().getName() + " took " + ctx.getDuration() + " ms");

            if (ctx.hasException()) {
                sb.append(" [exception]");
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}
