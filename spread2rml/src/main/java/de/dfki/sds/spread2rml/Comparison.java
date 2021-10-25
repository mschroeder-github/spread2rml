
package de.dfki.sds.spread2rml;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Resource;

/**
 * 
 */
public class Comparison<T> {
    
    private Comparison parent;
    private Point location;
    
    private T actual; //retrieved documents
    private T expected; //relevant documents
    
    private List<Map<Resource, Resource>> solutions;
    private Map<Map<Resource, Resource>, ModelConfusionMatrix> solution2confmatrix;
    private ModelConfusionMatrix bestConfMatrix;

    public Comparison() {
        solution2confmatrix = new HashMap<>();
    }
    
    public Comparison(T actual, T expected) {
        this();
        this.actual = actual;
        this.expected = expected;
    }
    
    public boolean isBothNull() {
        return actual == null && expected == null;
    }
    
    public T getActual() {
        return actual;
    }

    public void setActual(T actual) {
        this.actual = actual;
    }

    public T getExpected() {
        return expected;
    }

    public void setExpected(T expected) {
        this.expected = expected;
    }

    @Override
    public String toString() {
        return "Comparison{" + "actual=" + actual + ", expected=" + expected + '}';
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public Comparison getParent() {
        return parent;
    }

    public void setParent(Comparison parent) {
        this.parent = parent;
    }

    public List<Map<Resource, Resource>> getSolutions() {
        return solutions;
    }

    public void setSolutions(List<Map<Resource, Resource>> solutions) {
        this.solutions = solutions;
    }

    public Map<Map<Resource, Resource>, ModelConfusionMatrix> getSolution2confmatrix() {
        return solution2confmatrix;
    }

    public ModelConfusionMatrix getBestConfMatrix() {
        return bestConfMatrix;
    }

    public void setBestConfMatrix(ModelConfusionMatrix bestConfMatrix) {
        this.bestConfMatrix = bestConfMatrix;
    }
    
}
