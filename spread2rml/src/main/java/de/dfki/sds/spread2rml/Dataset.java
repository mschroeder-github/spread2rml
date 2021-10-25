
package de.dfki.sds.spread2rml;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.poi.ss.util.CellAddress;

/**
 * 
 */
public class Dataset {

    //where we find it
    private File folder;
    //full name of the dataset
    private String name;
    //abbreviated name of the dataset
    private String abbrev;
    

    private File workbookFile;
    private File expectedRdfFile;
    private File provenanceCsvFile;
    
    private Map<String, Object> properties;
    private Set<String> tags;
    
    private Dataset parent;
    
    private Map<String, ModelMatrix> sheet2modelMatrix;
    
    private int startColumnIndex;
    
    public Dataset() {
        tags = new HashSet<>();
        sheet2modelMatrix = new HashMap<>();
        properties = new HashMap<>();
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public File getFolder() {
        return folder;
    }
    
    public List<File> getModelMatrixFiles() {
        if(folder.listFiles() == null) {
            return new ArrayList<>();
        }
        
        List<File> files = new ArrayList<>();
        for(File f : folder.listFiles()) {
            if(f.getName().endsWith("modelmatrix.csv")) {
                files.add(f);
            }
        }
        return files;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbrev() {
        return abbrev;
    }

    public void setAbbrev(String abbrev) {
        this.abbrev = abbrev;
    }

    public File getWorkbookFile() {
        return workbookFile;
    }

    public void setWorkbookFile(File workbookFile) {
        this.workbookFile = workbookFile;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Dataset getParent() {
        return parent;
    }

    public void setParent(Dataset parent) {
        this.parent = parent;
    }

    public File getExpectedRdfFile() {
        return expectedRdfFile;
    }

    public void setExpectedRdfFile(File expectedRdfFile) {
        this.expectedRdfFile = expectedRdfFile;
    }
    
    public Model getExpectedModel() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        if(expectedRdfFile.exists()) {
            String rdfStr;
            if(expectedRdfFile.getName().endsWith("gz")) {
                rdfStr = IOUtils.toString(new GZIPInputStream(new FileInputStream(expectedRdfFile)), StandardCharsets.UTF_8);
            } else {
                rdfStr = IOUtils.toString(new FileInputStream(expectedRdfFile), StandardCharsets.UTF_8);
            }
            model.read(new StringReader(rdfStr), null, "TTL");
        }
        return model;
    }

    public int getStartColumnIndex() {
        return startColumnIndex;
    }

    public void setStartColumnIndex(int startColumnIndex) {
        this.startColumnIndex = startColumnIndex;
    }
    
    public File getProvenanceCsvFile() {
        return provenanceCsvFile;
    }

    public void setProvenanceCsvFile(File provenanceCsvFile) {
        this.provenanceCsvFile = provenanceCsvFile;
    }
    
    public void loadProvenanceCsv() throws IOException {
        if(!sheet2modelMatrix.isEmpty())
            return;
        
        CSVParser csv = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new InputStreamReader(new GZIPInputStream(new FileInputStream(provenanceCsvFile))));
        
        Map<String, Dimension> sheet2dim = new HashMap<>();
        
        //first iter: get dimension
        Iterator<CSVRecord> iter = csv.iterator();
        while(iter.hasNext()) {
            CSVRecord record = iter.next();
            
            CellAddress addr = new CellAddress(record.get("address"));
            
            Dimension dim = sheet2dim.computeIfAbsent(record.get("sheet"), s -> new Dimension());
            
            dim.width = Math.max(dim.width, addr.getColumn());
            dim.height = Math.max(dim.height, addr.getRow());
        }
        
        //init matrix
        for(String sheet : sheet2dim.keySet()) {
            Dimension dim = sheet2dim.get(sheet);
            dim.width++;
            dim.height++;
            sheet2modelMatrix.put(sheet, new ModelMatrix(sheet2dim.get(sheet)));
        }
        
        //to have the prefixes
        String prefixes = prefixes(getExpectedModel());
        
        //second iter: create model matrix
        csv = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new InputStreamReader(new GZIPInputStream(new FileInputStream(provenanceCsvFile))));
        iter = csv.iterator();
        while(iter.hasNext()) {
            CSVRecord record = iter.next();
            
            Model model = ModelFactory.createDefaultModel();
            try {
                model.read(new StringReader(prefixes + record.get("statements")), null, "TTL");
            } catch(Exception e) {
                System.out.println(record.get("statements"));
                int a = 0;
            }
            
            /*
            sheet2modelMatrix.get(record.get("sheet")).set(record.get("address"), model);
            */
            
            StringWriter sw = new StringWriter();
            model.clearNsPrefixMap();
            model.write(sw, "TTL");
            
            sheet2modelMatrix.get(record.get("sheet")).set(record.get("address"), sw.toString());
        }
        
        sheet2modelMatrix.values().forEach(mm -> mm.setPrefixes(prefixes));
    }

    public Map<String, ModelMatrix> getSheet2modelMatrix() {
        return sheet2modelMatrix;
    }
    
    public void clearProvenance() {
        sheet2modelMatrix = new HashMap<>();
        System.gc();
    }
    
    public static String prefixes(Model model) {
        StringBuilder prefixes = new StringBuilder();
        for (Map.Entry<String, String> e : model.getNsPrefixMap().entrySet()) {
            prefixes.append("PREFIX ").append(e.getKey()).append(":").append(" <").append(e.getValue()).append(">");
            prefixes.append("\n");
        }
        return prefixes.toString();
    }
    
    @Override
    public String toString() {
        return "Dataset{" + "folder=" + folder + ", name=" + name + ", abbrev=" + abbrev + '}';
    }
    
    public static void saveModelMatrixMap(Map<String, ModelMatrix> map, File folder) throws IOException {
        for (Map.Entry<String, ModelMatrix> entry : map.entrySet()) {

            File file = new File(folder, entry.getKey() + ".modelmatrix.csv");

            CSVPrinter p = CSVFormat.DEFAULT.print(file, StandardCharsets.UTF_8);

            ModelMatrix mm = entry.getValue();

            Dimension dim = mm.getDimension();
            
            for (int y = 0; y < dim.height; y++) {
                for (int x = 0; x < dim.width; x++) {
                    String str = mm.get(x, y);
                    p.print(str);
                }
                p.println();
            }

            p.close();
        }
    }
    
}
