package de.dfki.sds.spread2rml;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 *
 */
public abstract class Approach {

    protected String name;
    protected Set<String> tags;

    //runs an approach on a dataset
    public abstract void run(ApproachContext ctx) throws Exception;

    //per sheet a model matrix for comparison
    public abstract Map<String, ModelMatrix> getModelMatrixMap(ApproachContext ctx) throws Exception;

    public Approach() {
        tags = new HashSet<>();
    }

    //saves context json, but also model matrix
    public void save(ApproachContext ctx, File folder) throws Exception {

        ctx.save(new File(folder, "ctx.json"));
        
        if(ctx.hasException())
            return;
        
        Map<String, ModelMatrix> map = getModelMatrixMap(ctx);

        Dataset.saveModelMatrixMap(map, folder);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getTags() {
        return tags;
    }

    public boolean on(Dataset dataset) {
        Set<String> intersection = new HashSet<>(dataset.getTags());
        intersection.retainAll(this.tags);
        return intersection.size() > 0;
    }

    public static <T> Iterable<T> iterable(ExtendedIterator<T> extIter) {
        return () -> {
            return extIter;
        };
    }

    public static Resource resource(Model model, Resource s, Property p) {
        try {
            return model.getRequiredProperty(s, p).getResource();
        } catch (Exception e) {
            return null;
        }
    }

    public static Literal literal(Model model, Resource s, Property p) {
        try {
            return model.getRequiredProperty(s, p).getLiteral();
        } catch (Exception e) {
            return null;
        }
    }

}
