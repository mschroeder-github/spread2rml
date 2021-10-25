package de.dfki.sds.spread2rml.demo;

import de.dfki.sds.datasprout.utils.SemanticUtility;
import de.dfki.sds.hephaistos.forge.vocab.R2RML;
import de.dfki.sds.spread2rml.ApproachContext;
import de.dfki.sds.spread2rml.ApproachSpread2RML;
import de.dfki.sds.spread2rml.Dataset;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class DemoFrame extends javax.swing.JFrame {

    private Process libreoffice;
    private Timer timer;
    private long lastModified;
    
    private ApproachSpread2RML rmlPredictor;
    
    public DemoFrame() {
        initComponents();
        setSize(800, 800);
        setLocationRelativeTo(null);
        //1920x1080
        setLocation(1000, getY());
        
        rmlPredictor = new ApproachSpread2RML();
        
        jLabelRML.setText("");
        jLabelStmts.setText("");
        jLabelEntities.setText("");
        jLabelLog.setText("");
        
        openLibreoffice();
    }
    
    private void openLibreoffice() {
        
        File folder = new File("demo");
        folder.mkdirs();
        
        //clean dummy File
        File dummyFile = new File(folder, "dummy.xlsx");
        try {
            IOUtils.copy(DemoFrame.class.getResourceAsStream("/dummy.xlsx"), new FileOutputStream(dummyFile));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
        //FileUtils.write(dummyFile, data);
        
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("libreoffice", dummyFile.getAbsolutePath());
        try {
            libreoffice = pb.start();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
        lastModified = dummyFile.lastModified();
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(lastModified != dummyFile.lastModified()) {
                    
                    predictRml(dummyFile);
                    
                    lastModified = dummyFile.lastModified();
                }
            }
        }, 0, 1000);
    }
    
    private void predictRml(File xlsxFile) {
        
        Dataset dataset = new Dataset();
        dataset.setWorkbookFile(xlsxFile);
        dataset.setFolder(xlsxFile.getParentFile());
        
        ApproachContext ctx = new ApproachContext();
        ctx.setApproach(rmlPredictor);
        ctx.setDataset(dataset);
        ctx.setOutputFolder(xlsxFile.getParentFile());
        
        jTextAreaRML.setText("");
        jTextAreaStatements.setText("");
        jTextAreaEntities.setText("");
        jTextAreaLog.setText("");
        
        jLabelRML.setText("Predicting and Mapping ...");
        jLabelStmts.setText("");
        jLabelEntities.setText("");
        jLabelLog.setText("");
        
        File rmlFile = new File(xlsxFile.getParentFile(), "Sheet1.rml.ttl");
        File stmtsFile = new File(xlsxFile.getParentFile(), "Sheet1.ttl");
        File entitiesFile = new File(xlsxFile.getParentFile(), "entities.ttl");
        File logFile = new File(xlsxFile.getParentFile(), "log.txt");
        
        FileUtils.deleteQuietly(entitiesFile);
        
        try {
            rmlPredictor.run(ctx);
            
            String rml = FileUtils.readFileToString(rmlFile, StandardCharsets.UTF_8);
            
            Model rmlModel = ModelFactory.createDefaultModel().read(new StringReader(rml), null, "TTL");
            
            StringBuilder sb = new StringBuilder();
            List<Resource> poMaps = rmlModel.listSubjectsWithProperty(RDF.type, R2RML.PredicateObjectMap).toList();
            poMaps.sort((a,b) -> { return SemanticUtility.literal(rmlModel, a, RDFS.label).getString().compareTo(SemanticUtility.literal(rmlModel, b, RDFS.label).getString()); });
            for(Resource poMap : poMaps) {
                sb.append(SemanticUtility.toTTL(SemanticUtility.describeModel(rmlModel, poMap, 10), false));
                sb.append("\n");
                sb.append("\n");
            }
            
            //sb.append(SemanticUtility.describe(rmlModel, rmlModel.listSubjectsWithProperty(RDF.type, R2RML.TriplesMap).toList().get(0), 3));
            
            //sb.append(SemanticUtility.describe(rmlModel, rmlModel.listSubjectsWithProperty(RDF.type, RML.LogicalSource).toList().get(0), 3));
            
            jTextAreaRML.setText(sb.toString());
            jTextAreaRML.setCaretPosition(0);
            
            int predObjMapCount = rmlModel.listSubjectsWithProperty(RDF.type, R2RML.PredicateObjectMap).toList().size();
            
            jLabelRML.setText("RDF Mapping Language (RML): " + predObjMapCount + " Predicate-Object Maps");
            
            String stmts = FileUtils.readFileToString(stmtsFile, StandardCharsets.UTF_8);
            
            Model stmtModel = ModelFactory.createDefaultModel().read(new StringReader(stmts), null, "TTL");
            jLabelStmts.setText(stmtModel.size() + " Statements");
            
            Resource type = stmtModel.createResource("type://Sheet1");
            Property id = stmtModel.createProperty("http://example.org/0-0-id");
            
            StringBuilder sb2 = new StringBuilder();
            List<Resource> rows = stmtModel.listSubjectsWithProperty(RDF.type, type).toList();
            rows.sort((a,b) -> { return Integer.compare(SemanticUtility.literal(stmtModel, a, id).getInt(), SemanticUtility.literal(stmtModel, b, id).getInt()); });
            for(Resource row : rows) {
                sb2.append(SemanticUtility.toTTL(SemanticUtility.describeModel(stmtModel, row, 10), false));
                sb2.append("\n");
                sb2.append("\n");
            }
            jTextAreaStatements.setText(sb2.toString());
            jTextAreaStatements.setCaretPosition(0);
            
            if(entitiesFile.exists()) {
                String entities = FileUtils.readFileToString(entitiesFile, StandardCharsets.UTF_8);
                jTextAreaEntities.setText(entities);
                jTextAreaEntities.setCaretPosition(0);
                
                Model entitiesModel = ModelFactory.createDefaultModel().read(new StringReader(entities), null, "TTL");
                
                jLabelEntities.setText(entitiesModel.listSubjects().toList().size() + " Entities");
            }
            
            if(logFile.exists()) {
                String log = FileUtils.readFileToString(logFile, StandardCharsets.UTF_8);
                jTextAreaLog.setText(log);
                jTextAreaLog.setCaretPosition(0);
                jLabelLog.setText("Log");
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
            String stack = ExceptionUtils.getStackTrace(ex);
            jTextAreaRML.setText(stack);
            jTextAreaRML.setCaretPosition(0);
        }
        
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelRML = new javax.swing.JPanel();
        jLabelRML = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaRML = new javax.swing.JTextArea();
        jPanelStmts = new javax.swing.JPanel();
        jLabelStmts = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaStatements = new javax.swing.JTextArea();
        jPanelEntities = new javax.swing.JPanel();
        jLabelEntities = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextAreaEntities = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jLabelLog = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextAreaLog = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridLayout(4, 0));

        jLabelRML.setText("jLabel1");

        jTextAreaRML.setEditable(false);
        jTextAreaRML.setColumns(20);
        jTextAreaRML.setFont(new java.awt.Font("DialogInput", 0, 12)); // NOI18N
        jTextAreaRML.setRows(5);
        jScrollPane1.setViewportView(jTextAreaRML);

        javax.swing.GroupLayout jPanelRMLLayout = new javax.swing.GroupLayout(jPanelRML);
        jPanelRML.setLayout(jPanelRMLLayout);
        jPanelRMLLayout.setHorizontalGroup(
            jPanelRMLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
            .addComponent(jLabelRML, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanelRMLLayout.setVerticalGroup(
            jPanelRMLLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelRMLLayout.createSequentialGroup()
                .addComponent(jLabelRML)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE))
        );

        getContentPane().add(jPanelRML);

        jLabelStmts.setText("jLabel1");

        jTextAreaStatements.setEditable(false);
        jTextAreaStatements.setColumns(20);
        jTextAreaStatements.setFont(new java.awt.Font("DialogInput", 0, 12)); // NOI18N
        jTextAreaStatements.setRows(5);
        jScrollPane2.setViewportView(jTextAreaStatements);

        javax.swing.GroupLayout jPanelStmtsLayout = new javax.swing.GroupLayout(jPanelStmts);
        jPanelStmts.setLayout(jPanelStmtsLayout);
        jPanelStmtsLayout.setHorizontalGroup(
            jPanelStmtsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
            .addComponent(jLabelStmts, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanelStmtsLayout.setVerticalGroup(
            jPanelStmtsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelStmtsLayout.createSequentialGroup()
                .addComponent(jLabelStmts)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE))
        );

        getContentPane().add(jPanelStmts);

        jLabelEntities.setText("jLabel1");

        jTextAreaEntities.setEditable(false);
        jTextAreaEntities.setColumns(20);
        jTextAreaEntities.setFont(new java.awt.Font("DialogInput", 0, 12)); // NOI18N
        jTextAreaEntities.setRows(5);
        jScrollPane3.setViewportView(jTextAreaEntities);

        javax.swing.GroupLayout jPanelEntitiesLayout = new javax.swing.GroupLayout(jPanelEntities);
        jPanelEntities.setLayout(jPanelEntitiesLayout);
        jPanelEntitiesLayout.setHorizontalGroup(
            jPanelEntitiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelEntities, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
        );
        jPanelEntitiesLayout.setVerticalGroup(
            jPanelEntitiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelEntitiesLayout.createSequentialGroup()
                .addComponent(jLabelEntities)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE))
        );

        getContentPane().add(jPanelEntities);

        jLabelLog.setText("jLabel1");

        jTextAreaLog.setEditable(false);
        jTextAreaLog.setColumns(20);
        jTextAreaLog.setFont(new java.awt.Font("DialogInput", 0, 12)); // NOI18N
        jTextAreaLog.setRows(5);
        jScrollPane4.setViewportView(jTextAreaLog);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelLog, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabelLog)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        //uiae();
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DemoFrame().setVisible(true);
            }
        });
    }
    
    private static void uiae() {
        List<String> l = /*Arrays.asList("Ambient Assisted Living",
                        "Autonomous Driving",
                        "Deep Learning",
                        "Semantic Web",
                        "Safe and Secure Systems",
                        "Smart Agriculture Technologies",
                        "Wearable AI");*/
        /*Arrays.asList(
                "67677",
                "67685",
                "67685",
                "67693",
                "67699",
                "67691",
                "67686",
                "67678",
                "67680",
                "67697",
                "67688",
                "67699",
                "67685",
                "67681"
        );*/
        
                /*
        Arrays.asList(
                "Sets", 
                "Key Chains", 
                "Home decor", 
                "Backpacks and Lunch Boxes", 
                "Clothing", 
                "Accessories", 
                "Stationery", 
                "Decorations", 
                "Holiday", 
                "Backpacks", 
                "Polybag", 
                "Lunch Boxes", 
                "Brick Accessories and Kits", 
                "Minifigures", 
                "Brick Accessories"
        );
                */
        Arrays.asList(
                "Action",
                "Adventure",
                "Fighting",
                "Platform",
                "Puzzle",
                "Racing",
                "Roleplaying",
                "Shooter",
                "Simulation",
                "Sports",
                "Strategy",
                "Misc"
        );
        
        Random rnd = new Random();
        for(int i = 0; i < 20; i++) {
            List<String> L = new ArrayList<>(l);
            
            int a = 1 + rnd.nextInt(3);
            for(int j = 0; j < 1 + a; j++) {
                System.out.print(L.remove(rnd.nextInt(L.size())));
                
                
                //System.out.print(rnd.nextBoolean() ? ", " : "; ");
                
                if(j != a) {
                    System.out.print(", ");
                }
            }
            System.out.println();
        }
        
        int a = 0;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabelEntities;
    private javax.swing.JLabel jLabelLog;
    private javax.swing.JLabel jLabelRML;
    private javax.swing.JLabel jLabelStmts;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelEntities;
    private javax.swing.JPanel jPanelRML;
    private javax.swing.JPanel jPanelStmts;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTextArea jTextAreaEntities;
    private javax.swing.JTextArea jTextAreaLog;
    private javax.swing.JTextArea jTextAreaRML;
    private javax.swing.JTextArea jTextAreaStatements;
    // End of variables declaration//GEN-END:variables
}
