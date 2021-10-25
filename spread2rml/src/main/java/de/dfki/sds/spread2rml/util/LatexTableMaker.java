
package de.dfki.sds.spread2rml.util;

import de.dfki.sds.datasprout.utils.MinAvgMaxSdDouble;
import de.dfki.sds.mschroeder.commons.lang.StringUtility;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * 
 */
public class LatexTableMaker {
    
    public void writeDatasprout() throws IOException {
        StringBuilder sb = new StringBuilder();
        
        CSVParser p = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(new File("publish/evaluation/spread2rml-datasprout.csv")));
        List<CSVRecord> spread2rmlList = p.getRecords();
        
        p = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(new File("publish/evaluation/any23-datasprout.csv")));
        List<CSVRecord> any23List = p.getRecords();
        
        
        //precision	recall	f-score
        //mode
        
        Map<String, List<Double[]>> spread2rmlMap = new HashMap<>();
        Map<String, List<Double[]>> any23Map = new HashMap<>();
        
        for(CSVRecord r : spread2rmlList) {
            List<Double[]> ll = spread2rmlMap.computeIfAbsent(r.get("mode"), mode -> new ArrayList<>());
            ll.add(new Double[] { Double.parseDouble(r.get("precision")), Double.parseDouble(r.get("recall")), Double.parseDouble(r.get("f-score"))  });
        }
        
        for(CSVRecord r : any23List) {
            if(r.get("preprocessing").equals("ssconvert")) {
                List<Double[]> ll = any23Map.computeIfAbsent(r.get("mode"), mode -> new ArrayList<>());
                ll.add(new Double[] { Double.parseDouble(r.get("precision")), Double.parseDouble(r.get("recall")), Double.parseDouble(r.get("f-score"))  });
            }
        }
        
        for(Entry<String, List<Double[]>> e : spread2rmlMap.entrySet()) {
            
            MinAvgMaxSdDouble pStatS2R = new MinAvgMaxSdDouble();
            MinAvgMaxSdDouble rStatS2R = new MinAvgMaxSdDouble();
            MinAvgMaxSdDouble fStatS2R = new MinAvgMaxSdDouble();
            for(Double[] d : e.getValue()) {
                pStatS2R.add(d[0]);
                rStatS2R.add(d[1]);
                fStatS2R.add(d[2]);
            }
            
            String name = StringUtility.splitCamelCaseString(e.getKey()).replace("_", " ").replace("Intra Cell Additional Information  ", "");
            
            
            MinAvgMaxSdDouble pStatAny23 = new MinAvgMaxSdDouble();
            MinAvgMaxSdDouble rStatAny23 = new MinAvgMaxSdDouble();
            MinAvgMaxSdDouble fStatAny23 = new MinAvgMaxSdDouble();
            for(Double[] d : any23Map.get(e.getKey())) {
                pStatAny23.add(d[0]);
                rStatAny23.add(d[1]);
                fStatAny23.add(d[2]);
            }
            
            
            //sb.append("\\multicolumn{1}{|p{2cm}|}{"+ e.getKey() +"} &   &    &   & "+ String.format(Locale.US, "%.2f", avgArr[0]) +" & "+ String.format(Locale.US, "%.2f", avgArr[1]) +" & "+ String.format(Locale.US, "%.2f", avgArr[2]) +" \\\\").append("\n");
            
            
            sb.append("\\multicolumn{1}{|p{2cm}|}{"+ name +"} & "+ 
                    pStatAny23.toStringAvgSDLatex(2) +" & "+ 
                    rStatAny23.toStringAvgSDLatex(2) +" & "+ 
                    fStatAny23.toStringAvgSDLatex(2) +" & "+ 
                    pStatS2R.toStringAvgSDLatex(2) +" & "+ 
                    rStatS2R.toStringAvgSDLatex(2) +" & "+ 
                    fStatS2R.toStringAvgSDLatex(2) +" \\\\"
            ).append("\n");
            sb.append("\\hline").append("\n");
        }
        
        List<String> lines = new ArrayList<>(Arrays.asList(sb.toString().split("\n")));
        lines.sort((a,b) -> a.compareTo(b));
        
        for(String l : lines) {
            System.out.println(l);
            System.out.println("\\hline");
        }
        
    }
    
    public void writeDatagov() throws IOException {
        StringBuilder sb = new StringBuilder();
        
        CSVParser p = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(new File("publish/evaluation/any23-datagov.csv")));
        List<CSVRecord> any23datagovList = p.getRecords();
        
        p = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(new File("publish/evaluation/spread2rml-datagov.csv")));
        List<CSVRecord> spread2rmldatagovList = p.getRecords();
        
        Map<String, List<Double[]>> any23Map = new HashMap<>();
        Map<String, List<Double[]>> spread2rmlMap = new HashMap<>();
        
        for(CSVRecord r : any23datagovList) {
            
            String hash = r.get("name").split("[ \\-\\/]")[3];
            
            if(r.get("name").endsWith("ssconvert")) {
                List<Double[]> ll = any23Map.computeIfAbsent(hash, mode -> new ArrayList<>());
                ll.add(new Double[] { Double.parseDouble(r.get("precision")), Double.parseDouble(r.get("recall")), Double.parseDouble(r.get("f-score"))  });
            }
        }
        
        for(CSVRecord r : spread2rmldatagovList) {
            
            String hash = r.get("name").split("[ \\-\\/]")[3];
            
            List<Double[]> ll = spread2rmlMap.computeIfAbsent(hash, mode -> new ArrayList<>());
            ll.add(new Double[] { Double.parseDouble(r.get("precision")), Double.parseDouble(r.get("recall")), Double.parseDouble(r.get("f-score"))  });
        }
        
        for(Entry<String, List<Double[]>> e : any23Map.entrySet()) {
            
            List<Double[]> spread2rml = spread2rmlMap.get(e.getKey());
            List<Double[]> any23 = any23Map.get(e.getKey());
            
            sb.append("\\multicolumn{1}{|p{2cm}|}{"+ e.getKey() +"} & "+ 
                    String.format(Locale.US, "%.2f", any23.get(0)[0]) +" & "+ 
                    String.format(Locale.US, "%.2f", any23.get(0)[1]) +" & "+ 
                    String.format(Locale.US, "%.2f", any23.get(0)[2]) +" & "+ 
                    String.format(Locale.US, "%.2f", spread2rml.get(0)[0]) +" & "+ 
                    String.format(Locale.US, "%.2f", spread2rml.get(0)[1]) +" & "+ 
                    String.format(Locale.US, "%.2f", spread2rml.get(0)[2]) +" \\\\"
            ).append("\n");
        }
        
        List<String> lines = new ArrayList<>(Arrays.asList(sb.toString().split("\n")));
        lines.sort((a,b) -> a.compareTo(b));
        
        for(String l : lines) {
            System.out.println(l);
            System.out.println("\\hline");
        }
        
    }
    
    public static void main(String[] args) throws IOException {
        LatexTableMaker ltm = new LatexTableMaker();
        
        System.out.println();
        System.out.println("Datagov");
        ltm.writeDatagov();
        
        System.out.println();
        System.out.println("Datasprout");
        ltm.writeDatasprout();
    }
}
