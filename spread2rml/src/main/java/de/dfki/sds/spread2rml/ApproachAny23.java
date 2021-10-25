
package de.dfki.sds.spread2rml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.apache.any23.configuration.Configuration;
import org.apache.any23.configuration.DefaultConfiguration;
import org.apache.any23.extractor.ExtractionContext;
import org.apache.any23.extractor.ExtractionParameters;
import org.apache.any23.extractor.ExtractionParameters.ValidationMode;
import org.apache.any23.extractor.ExtractionResultImpl;
import org.apache.any23.extractor.csv.CSVExtractor;
import org.apache.any23.writer.TurtleWriter;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * 
 */
public class ApproachAny23 extends Approach {

    {
        this.name = "Any23";
        this.tags.add("csv");
    }
    
    @Override
    public void run(ApproachContext ctx) throws Exception {
        
        CSVExtractor extractor = new CSVExtractor();
        
        Configuration config = DefaultConfiguration.copy();
        ExtractionParameters parameters = new ExtractionParameters(config, ValidationMode.NONE);
        
        ValueFactory valueFactory = SimpleValueFactory.getInstance();
        
        List<File> csvFiles = new ArrayList<>(Arrays.asList(ctx.getDataset().getFolder().listFiles()));
        
        for(File csvFile : csvFiles) {
            ExtractionContext context = new ExtractionContext("name", valueFactory.createIRI("urn:uuid:" + UUID.randomUUID().toString()));

            File turtleFile = new File(ctx.getOutputFolder(), csvFile.getName() + ".ttl");
            
            //StringWriter sw = new StringWriter();
            //new WriterOutputStream(sw, StandardCharsets.UTF_8)
            
            TurtleWriter turtleWriter = new TurtleWriter(new FileOutputStream(turtleFile));

            ExtractionResultImpl result = new ExtractionResultImpl(context, extractor, turtleWriter);

            InputStream is = new ByteArrayInputStream(FileUtils.readFileToString(csvFile, StandardCharsets.UTF_8).getBytes());
            
            extractor.run(parameters, context, is, result);
            
            turtleWriter.close();
            
            result.close();
            
            ctx.getInput2output().put(csvFile, turtleFile);
        }
    }
    
    @Override
    public Map<String, ModelMatrix> getModelMatrixMap(ApproachContext ctx) throws Exception {
        
        Map<String, ModelMatrix> m = new HashMap<>();
        
        for(Entry<File, File> entry : ctx.getInput2output().entrySet()) {
            
            Model model = ModelFactory.createDefaultModel().read(new FileReader(entry.getValue()), null, "TTL");
            
            //System.out.println(entry.getValue() + " " + model.size());
            
            Property colPos = model.createProperty("http://tools.ietf.org/html/rfc4180columnPosition");
            Property rowPos = model.createProperty("http://tools.ietf.org/html/rfc4180rowPosition");
            Property rowProp = model.createProperty("http://tools.ietf.org/html/rfc4180row");
            Resource Row = model.createResource("http://tools.ietf.org/html/rfc4180Row");
            
            int maxRow = 0;
            for(Statement stmt : iterable(model.listStatements(null, rowPos, (RDFNode) null))) {
                maxRow = Math.max(maxRow, stmt.getInt());
            }
            
            int maxCol = 0;
            for(Statement stmt : iterable(model.listStatements(null, colPos, (RDFNode) null))) {
                maxCol = Math.max(maxCol, stmt.getInt());
            }
            
            ModelMatrix modelMatrix = new ModelMatrix(maxCol+1, maxRow+2);
            
            //to make sure that we put the triples in the model matrix
            Model remainingModel = ModelFactory.createDefaultModel().add(model);
            
            for(Statement stmt : iterable(model.listStatements())) {
                
                Literal rowLit = literal(model, stmt.getSubject(), rowPos);
                Literal colLit = literal(model, stmt.getPredicate(), colPos);
                Literal subColLit = literal(model, stmt.getSubject(), colPos);
                
                if(rowLit != null && colLit != null) {
                    
                    //row 0 is reserved for properties
                    int row = rowLit.getInt() + 1;
                    int col = colLit.getInt();
                    
                    modelMatrix.append(col, row, stmt);
                    
                    remainingModel.remove(stmt);
                }
                
                /**
                 * <urn:uuid:dbc69362-f263-448e-b1c8-214e4640e719Reviewfor> 
                 *      <http://www.w3.org/2000/01/rdf-schema#label> "reviewFor";
                        <http://tools.ietf.org/html/rfc4180columnPosition> 6 .
                 */
                if(subColLit != null) {
                    if(!stmt.getPredicate().equals(colPos)) {
                        modelMatrix.append(subColLit.getInt(), 0, stmt);
                    }
                    remainingModel.remove(stmt);
                }
                
                if(stmt.getPredicate().equals(RDF.type) && stmt.getObject().equals(Row)) {
                    remainingModel.remove(stmt);
                }
                if(stmt.getPredicate().equals(colPos)) {
                    remainingModel.remove(stmt);
                }
                if(stmt.getPredicate().equals(rowPos)) {
                    remainingModel.remove(stmt);
                }
            }
            
            String name = entry.getKey().getName();
            name = name.substring(0, name.length() - ".csv".length());
            m.put(name, modelMatrix);
        }
        
        return m;
    }

    
    
}
