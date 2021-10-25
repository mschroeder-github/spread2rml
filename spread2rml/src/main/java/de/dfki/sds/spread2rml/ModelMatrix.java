
package de.dfki.sds.spread2rml;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.poi.ss.util.CellAddress;

/**
 * 
 */
public class ModelMatrix {

    //takes too much RAM
    @Deprecated
    private Model[][] data;
    private String[][] strdata;
    
    private String prefixes;

    public ModelMatrix(Dimension dim) {
        this(dim.width, dim.height);
    }
    
    public ModelMatrix(int width, int height) {
        data = new Model[height][width];
        strdata = new String[height][width];
        prefixes = "";
    }

    @Deprecated
    public void set(String address, Model model) {
        CellAddress addr = new CellAddress(address);
        set(addr.getColumn(), addr.getRow(), model);
    }
    
    @Deprecated
    public void set(int column, int row, Model model) {
        data[row][column] = model;
    }
    
    public void set(String address, String rdf) {
        CellAddress addr = new CellAddress(address);
        set(addr.getColumn(), addr.getRow(), rdf);
    }
    
    public void set(int column, int row, String rdf) {
        strdata[row][column] = rdf;
    }
    
    
    
    public boolean isNull(int column, int row) {
        return strdata[row][column] == null;
    }
    
    public String get(int column, int row) {
        try {
            return strdata[row][column];
        } catch(IndexOutOfBoundsException e) {
            return null;
        }
    }
    
    public void append(int column, int row, Statement stmt) {
        if(isNull(column, row)) {
            set(column, row, "");
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RDFDataMgr.writeTriples(baos, Arrays.asList(stmt.asTriple()).iterator());
        
        try {
            strdata[row][column] += baos.toString("UTF-8") + "\n";
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public String getPrefixes() {
        if(prefixes == null)
            return "";
        
        return prefixes;
    }

    public void setPrefixes(String prefixes) {
        this.prefixes = prefixes;
    }
    
    @Deprecated
    public Model[][] getData() {
        return data;
    }

    public String[][] getStrdata() {
        return strdata;
    }
    
    public Dimension getDimension() {
        return new Dimension(strdata.length == 0 ? 0 : strdata[0].length, strdata.length);
    }
    
}
