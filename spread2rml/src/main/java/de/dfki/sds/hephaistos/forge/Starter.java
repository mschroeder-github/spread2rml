package de.dfki.sds.hephaistos.forge;

import be.ugent.rml.Executor;
import be.ugent.rml.functions.FunctionLoader;
import be.ugent.rml.metadata.MetadataGenerator;
import be.ugent.rml.records.RecordsFactory;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.store.RDF4JStore;
import be.ugent.rml.term.Term;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.rdf4j.rio.RDFFormat;

/**
 * To start working on building a KG.
 */
public class Starter {


    /**
     * Run the RML mapper without a GUI.
     *
     * @param rmlBaseFolder
     * @param mappingFile
     * @param fnoFolder can be null
     * @param outFile
     * @throws Exception
     */
    public static void rmlmapper(File rmlBaseFolder, File mappingFile, File fnoFolder, File outFile) throws Exception {
        rmlmapper(rmlBaseFolder, mappingFile, fnoFolder, null, outFile);
    }
    
    /**
     * Run the RML mapper without a GUI.
     *
     * @param rmlBaseFolder
     * @param mappingFile
     * @param fnoFolder can be null
     * @param provOutFile can be null
     * @param outFile
     * @throws Exception
     */
    public static void rmlmapper(File rmlBaseFolder, File mappingFile, File fnoFolder, File provOutFile, File outFile) throws Exception {
        RDF4JStore rmlStore = new RDF4JStore();

        rmlStore.read(new FileInputStream(mappingFile), null, RDFFormat.TURTLE);

        FunctionLoader functionLoader = null;
        if (fnoFolder != null) {
            Model fnoModel = getFunctionsModel(fnoFolder);
            QuadStore fnoQuadStore = new RDF4JStore();
            StringWriter fnoStringWriter = new StringWriter();
            fnoModel.write(fnoStringWriter, "TTL");
            fnoQuadStore.read(IOUtils.toInputStream(fnoStringWriter.toString(), StandardCharsets.UTF_8), null, RDFFormat.TURTLE);
            functionLoader = new FunctionLoader(fnoQuadStore);
        } else {
            functionLoader = new FunctionLoader();
        }

        //executor
        //(nquads (default), turtle, trig, trix, jsonld, hdt)
        QuadStore outputStore = new RDF4JStore(); //SimpleQuadStore support only nquads
        RecordsFactory factory = new RecordsFactory(rmlBaseFolder.getAbsolutePath());
        Executor executor = new Executor(rmlStore, factory, functionLoader, outputStore, null);
        //if null or empty the initalizer will read them from rmlStore
        List<Term> triplesMaps = new ArrayList<>();

        //also write out provenance
        MetadataGenerator mdg = null;
        QuadStore provQuadStore = null;
        if(provOutFile != null) {
            QuadStore inputQuadStore = new RDF4JStore();
            provQuadStore = new RDF4JStore();
            mdg = new MetadataGenerator(MetadataGenerator.DETAIL_LEVEL.TRIPLE, provOutFile.getAbsolutePath(), new String[0], inputQuadStore, provQuadStore);
        }
        
        QuadStore result = executor.execute(triplesMaps, false, mdg);

        result.write(new FileWriterWithEncoding(outFile, StandardCharsets.UTF_8), "turtle");
        
        //also write provenance
        if(provOutFile != null) {
            provQuadStore.write(new FileWriterWithEncoding(provOutFile, StandardCharsets.UTF_8), "turtle");
        }
    }

    private static Model getFunctionsModel(File fnoFolder) {
        Model m = ModelFactory.createDefaultModel();

        for (File file : fnoFolder.listFiles()) {
            if(!file.getName().endsWith("ttl"))
                continue;
                
            try {
                m.read(new FileReader(file), null, "TTL");
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }

        return m;
    }

}
