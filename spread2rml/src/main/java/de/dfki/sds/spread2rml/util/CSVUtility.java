
package de.dfki.sds.spread2rml.util;

import java.awt.Dimension;
import java.util.List;
import org.apache.commons.csv.CSVRecord;

/**
 * 
 */
public class CSVUtility {

    public static Dimension getDimension(List<CSVRecord> records) {
        int maxCol = 0;
        for(CSVRecord rec : records) {
            maxCol = Math.max(rec.size(), maxCol);
        }
        return new Dimension(maxCol, records.size());
    }
    
}
