package de.dfki.sds.spread2rml.io;

import de.dfki.sds.datasprout.excel.ExcelTable;
import de.dfki.sds.datasprout.utils.SemanticUtility;
import de.dfki.sds.hephaistos.forge.FnoLoader;
import de.dfki.sds.hephaistos.forge.Starter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 */
public class RmlMapperOnTable {

    private Model entityModel;
    private Model propertyModel;
    private File datasetFile;
    
    public RmlMapperOnTable() {
        entityModel = ModelFactory.createDefaultModel();
        propertyModel = ModelFactory.createDefaultModel();
    }

    public void run(ExcelTable tbl, File outputFolder) throws Exception {
        File mappingFile = new File(outputFolder, tbl.getSheetName() + ".rml.ttl");
        File outFile = new File(outputFolder, tbl.getSheetName() + ".ttl");
        File mappingFolder = mappingFile.getParentFile();

        if (entityModel.size() > 0) {
            datasetFile = new File(outputFolder, "entities.ttl");
            FileUtils.write(datasetFile, SemanticUtility.toTTL(entityModel), StandardCharsets.UTF_8);
        }

        FileUtils.write(mappingFile, SemanticUtility.toTTL(tbl.getRmlMappingModel()), StandardCharsets.UTF_8);

        FileUtils.deleteQuietly(new File("fno.json"));
        //config
        if (datasetFile != null) {
            JSONObject entityLinking = new JSONObject();
            entityLinking.put("dataset", datasetFile.getAbsolutePath());
            entityLinking.put("excludeType", new JSONArray());
            JSONObject fnoObj = new JSONObject();
            fnoObj.put("entityLinking", entityLinking);
            FileUtils.writeStringToFile(new File("fno.json"), fnoObj.toString(2), StandardCharsets.UTF_8);
        }

        buildFnoLibrary(mappingFolder);

        String provName = outFile.getName();
        provName = provName.substring(0, provName.lastIndexOf(".")) + ".prov.ttl";
        File provFile = new File(outFile.getParentFile(), provName);

        String mmName = outFile.getName();
        mmName = mmName.substring(0, mmName.lastIndexOf(".")) + ".modelmatrix.csv";
        File modelMatrixFile = new File(outFile.getParentFile(), mmName);

        System.out.println("RML Mapper");
        Starter.rmlmapper(mappingFolder, mappingFile, mappingFolder, provFile, outFile);

        System.out.println("Model Matrix");
        createModelMatrix(provFile, modelMatrixFile);
    }

    private void createModelMatrix(File provFile, File modelMatrixFile) throws IOException {
        Model m = ModelFactory.createDefaultModel().read(new FileReader(provFile), null, "TTL");

        if (entityModel.isEmpty() && datasetFile != null) {
            entityModel.read(new FileReader(datasetFile), null, "TTL");
        }

        Property column = m.createProperty("http://www.w3.org/ns/prov#column");
        Property row = m.createProperty("http://www.w3.org/ns/prov#row");

        int maxCol = 0;
        int minCol = Integer.MAX_VALUE;
        for (Statement stmt : m.listStatements(null, column, (RDFNode) null).toList()) {
            maxCol = Math.max(maxCol, stmt.getInt());
            minCol = Math.min(minCol, stmt.getInt());
        }
        int maxRow = 0;
        int minRow = Integer.MAX_VALUE;
        for (Statement stmt : m.listStatements(null, row, (RDFNode) null).toList()) {
            maxRow = Math.max(maxRow, stmt.getInt());
            minRow = Math.min(minRow, stmt.getInt());
        }

        Model[][] modelMatrix = new Model[maxRow + 1][maxCol + 1];

        for (Resource subj : m.listSubjectsWithProperty(RDF.type, RDF.Statement).toList()) {

            int rowIndex = m.getRequiredProperty(subj, row).getInt();
            int colIndex = m.getRequiredProperty(subj, column).getInt();

            if (modelMatrix[rowIndex][colIndex] == null) {
                modelMatrix[rowIndex][colIndex] = ModelFactory.createDefaultModel();
            }

            modelMatrix[rowIndex][colIndex].add(
                    m.getRequiredProperty(subj, RDF.subject).getObject().asResource(),
                    m.getRequiredProperty(subj, RDF.predicate).getObject().as(Property.class),
                    m.getRequiredProperty(subj, RDF.object).getObject()
            );
        }

        //for each column
        for (int i = minCol; i <= maxCol; i++) {

            int r = minRow - 1;

            if (modelMatrix[r][i] != null) {
                throw new RuntimeException("has to be null here");
            }

            //if(modelMatrix[r][i] == null) {
            modelMatrix[r][i] = ModelFactory.createDefaultModel();
            //}

            Set<Resource> properties = new HashSet<>();
            for (int j = minRow; j <= maxRow; j++) {
                if (modelMatrix[j][i] != null) {

                    for (Statement stmt : modelMatrix[j][i].listStatements().toList()) {

                        if (!stmt.getPredicate().getURI().startsWith("http://example.org")) {
                            continue;
                        }

                        properties.add(stmt.getPredicate());
                    }

                }
            }

            for (Resource prop : properties) {
                modelMatrix[r][i].add(propertyModel.listStatements(prop, null, (RDFNode) null));
            }
        }

        CSVPrinter p = CSVFormat.DEFAULT.print(modelMatrixFile, StandardCharsets.UTF_8);

        for (int i = 0; i < modelMatrix.length; i++) {
            for (int j = 0; j < modelMatrix[i].length; j++) {

                if (modelMatrix[i][j] == null) {
                    p.print("");
                    continue;
                }

                for (RDFNode node : modelMatrix[i][j].listObjects().toList()) {
                    if (node.isResource()) {
                        modelMatrix[i][j].add(entityModel.listStatements(node.asResource(), null, (RDFNode) null));
                    }
                }

                StringWriter sw = new StringWriter();
                modelMatrix[i][j].write(sw, "TTL");
                p.print(sw.toString());

            }
            p.println();
        }
        p.close();

    }

    private Model buildFnoLibrary(File genFolder) throws Exception {
        File fnoProjectFolder = new File("fno");

        System.out.println("mvn -q package to build fno.jar");
        ProcessBuilder pb = new ProcessBuilder("mvn", "-q", "package");
        pb.directory(fnoProjectFolder);
        Process start = pb.start();
        int exitValue = start.waitFor();

        if (exitValue != 0) {
            throw new RuntimeException("fno was not built");
        }

        File srcJarFile = new File(fnoProjectFolder, "target/fno.jar");

        FnoLoader fnoLoader = new FnoLoader();
        Model fnoTboxModel = fnoLoader.tbox(5);

        File dstJarFile = new File(genFolder, "fno.jar");
        FileUtils.copyFile(srcJarFile, dstJarFile);

        Model fnoModel = fnoLoader.load(dstJarFile, "com.github.mschroedergithub.fno.CustomFunctions", dstJarFile.getAbsolutePath());
        File fnoFile = new File(genFolder, "fno.ttl");

        Model union = ModelFactory.createUnion(fnoModel, fnoTboxModel);

        FileUtils.writeStringToFile(fnoFile, SemanticUtility.toTTL(union), StandardCharsets.UTF_8);

        /*
        for(File folder : listLeafFolders(genFolder)) {
            
            Model fnoModel = fnoLoader.load(dstJarFile, "com.github.mschroedergithub.fno.CustomFunctions", dstJarFile.getAbsolutePath());
            
            File fnoFolder = new File(folder, "fno");
            fnoFolder.mkdir();
            File fnoFile = new File(fnoFolder, "fno.ttl");
            
            FileUtils.writeStringToFile(fnoFile, SemanticUtility.toTTL(ModelFactory.createUnion(fnoModel, fnoTboxModel)), StandardCharsets.UTF_8);
        }
         */
        return union;
    }

    public Model getEntityModel() {
        return entityModel;
    }

    public Model getPropertyModel() {
        return propertyModel;
    }

    public File getDatasetFile() {
        return datasetFile;
    }
    
}
