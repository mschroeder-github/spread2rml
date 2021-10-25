
package de.dfki.sds.spread2rml;

import de.dfki.sds.spread2rml.util.CSVUtility;
import java.awt.Dimension;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;

/**
 * 
 */
public class ApproachGTChangedURIs extends Approach {

    {
        this.name = "GTChangedURIs";
        this.tags.add("xlsx");
    }
    
    @Override
    public void run(ApproachContext ctx) throws Exception {
        
    }
    
    @Override
    public Map<String, ModelMatrix> getModelMatrixMap(ApproachContext ctx) throws Exception {
        
        Map<String, ModelMatrix> m = new HashMap<>();
        
        Dataset ds = ctx.getRootDataset();
        for(File f : ds.getModelMatrixFiles()) {
            
            List<CSVRecord> records = CSVFormat.DEFAULT.parse(new FileReader(f)).getRecords();
            Dimension dim = CSVUtility.getDimension(records);
            
            String sheetName = f.getName().substring(0, f.getName().length() - ".modelmatrix.csv".length());
            
            ModelMatrix modelMatrix = new ModelMatrix(dim.width, dim.height);
            
            Set<Resource> allResources = new HashSet<>();
            for(int i = 0; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                for(int j = 0; j < dim.width; j++) {
                    Model model = ModelFactory.createDefaultModel().read(new StringReader(rec.get(j)), null, "TTL");
                    for(Statement stmt : model.listStatements().toList()) {
                        
                        allResources.add(stmt.getSubject());
                        allResources.add(stmt.getPredicate());
                        if(stmt.getObject().isResource()) {
                            allResources.add(stmt.getObject().asResource());
                        }
                    }
                }
            }
            
            //random remap
            Map<Resource, Resource> map = new HashMap<>();
            for(Resource res : allResources) {
                map.put(res, ResourceFactory.createResource("urn:uuid:" + UUID.randomUUID().toString()));
            }
            
            for(int i = 0; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                for(int j = 0; j < dim.width; j++) {
                    Model model = ModelFactory.createDefaultModel().read(new StringReader(rec.get(j)), null, "TTL");
                    
                    ApproachContext.substitute(map, model);
                    
                    StringWriter sw = new StringWriter();
                    model.write(sw, "TTL");
                    modelMatrix.set(j, i, sw.toString());
                }
            }
            
            m.put(sheetName, modelMatrix);
        }
        
        return m;
    }
    
}
