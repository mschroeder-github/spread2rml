package de.dfki.sds.spread2rml;

import de.dfki.sds.datasprout.RmlMapping;
import de.dfki.sds.datasprout.excel.ExcelColumn;
import de.dfki.sds.datasprout.excel.ExcelTable;
import de.dfki.sds.datasprout.excelgen.FontStyle;
import de.dfki.sds.datasprout.excelgen.FontStyle.FontStyleParseResult;
import de.dfki.sds.datasprout.utils.MinAvgMaxSdDouble;
import de.dfki.sds.datasprout.vocab.R2RML;
import de.dfki.sds.datasprout.vocab.SS;
import de.dfki.sds.hephaistos.DataStoreDescription;
import de.dfki.sds.hephaistos.storage.InternalStorageMetaData;
import de.dfki.sds.hephaistos.storage.excel.ExcelCell;
import de.dfki.sds.hephaistos.storage.excel.ExcelStorageIO;
import de.dfki.sds.mschroeder.commons.lang.RegexUtility;
import de.dfki.sds.mschroeder.commons.lang.StringUtility;
import de.dfki.sds.mschroeder.commons.lang.math.Histogram;
import de.dfki.sds.spread2rml.io.ColumnMemoryExcelStorage;
import de.dfki.sds.spread2rml.io.ConsoleLoadingListener;
import de.dfki.sds.spread2rml.io.RmlMapperOnTable;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.apache.poi.ss.util.CellAddress;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 *
 */
public class ApproachSpread2RML extends Approach {

    public static String fnNs = "java:com.github.mschroedergithub.fno.CustomFunctions.";

    public static int excelCellId = 1;

    //a contribution is to create RML and not directly RDF
    {
        this.name = "Spread2RML";
        this.tags.add("xlsx");
    }

    @Override
    public void run(ApproachContext ctx) throws Exception {

        excelCellId = Integer.MAX_VALUE / 2;

        //load spreadsheet
        ExcelStorageIO io = new ExcelStorageIO();
        DataStoreDescription dsd = new DataStoreDescription();
        dsd.getLocators().add(ctx.getDataset().getWorkbookFile().getAbsolutePath());

        ColumnMemoryExcelStorage excelStorage = new ColumnMemoryExcelStorage(new InternalStorageMetaData("id", "ColumnMemoryExcelStorage"));
        io.importing(dsd, excelStorage, new ConsoleLoadingListener());

        excelStorage.getTables().sort((a, b) -> a.getSheetName().compareTo(b.getSheetName()));

        Map<ExcelTable, Dimension> table2dim = new HashMap<>();

        Log log = new Log(new File(ctx.getOutputFolder(), "log.txt"));

        //move header cell
        for (ExcelTable tbl : excelStorage.getTables()) {

            Dimension dim = new Dimension();
            table2dim.put(tbl, dim);

            for (ExcelColumn col : tbl.getColumns()) {

                for (ExcelCell cell : col.getDataCells()) {
                    dim.height = Math.max(cell.getRow(), dim.height);
                    dim.width = Math.max(cell.getColumn(), dim.width);
                }

                col.getHeaderCells().add(col.getDataCells().remove(0));
            }
        }

        Model propertyModel = ModelFactory.createDefaultModel();

        int tblIndex = 0;
        for (ExcelTable tbl : excelStorage.getTables()) {

            //to select only those which have a model matrix
            if (!ctx.getSheetNameWhitelist().isEmpty() && !ctx.getSheetNameWhitelist().contains(tbl.getSheetName())) {
                continue;
            }

            RmlMapping rmlMapping = tbl.getRmlMapping();

            log.println("Sheet: " + tbl.getSheetName());
            log.println();

            //triples map with one subject map
            String sheetNameEncoded = tbl.getSheetName().replace(" ", "").replace("(", "").replace(")", "").replace("+", "_");
            Resource[] tblTypes = new Resource[]{rmlMapping.getModel().createResource("type://" + sheetNameEncoded)};
            Resource triplesMap = rmlMapping.createTriplesMap("For " + tbl.getSheetName());
            rmlMapping.createSubjectMap(triplesMap, "inst://" + sheetNameEncoded + "/{address}", tblTypes);

            //a logical source for the sheet
            Dimension dim = table2dim.get(tbl);
            Resource[] ls = rmlMapping.createLogicalSource(
                    triplesMap, tbl.getSheetName(),
                    ctx.getDataset().getStartColumnIndex(), 1, ctx.getDataset().getStartColumnIndex(), dim.height
            );
            //TODO make this relative before publish
            rmlMapping.getModel().add(ls[1], SS.url, ctx.getDataset().getWorkbookFile().getAbsolutePath());

            Model entityModel = ModelFactory.createDefaultModel();

            //for each column
            for (ExcelColumn col : tbl.getColumns()) {

                if (col.getIndex() < ctx.getDataset().getStartColumnIndex()) {
                    continue;
                }

                //where we are
                String relShift = "(" + (col.getIndex() - ctx.getDataset().getStartColumnIndex()) + ",0)";
                CellAddress addr = new CellAddress(0, col.getIndex());
                String columnLetter = addr.formatAsString().replaceAll("\\d+", "");

                //decides for one RML possiblility and creates RML definition
                predict(tbl, col, relShift, columnLetter, rmlMapping, triplesMap, entityModel, propertyModel, log);
            }

            //creates rml, entities output file and runs RML Mapper on it and creates ttl file and model matrix
            RmlMapperOnTable rmlMapperOnTable = new RmlMapperOnTable();
            rmlMapperOnTable.getPropertyModel().add(propertyModel);
            rmlMapperOnTable.getEntityModel().add(entityModel);
            rmlMapperOnTable.run(tbl, ctx.getOutputFolder());

            tblIndex++;
        }

    }

    private static class Log {

        private boolean print = true;

        private File logFile;
        private FileWriter fileWriter;

        public Log(File logFile) {
            this.logFile = logFile;
            try {
                fileWriter = new FileWriter(logFile);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        public void print(Object obj) {
            if (print) {
                System.out.print(obj == null ? "" : obj.toString());
            }

            try {
                fileWriter.append(obj == null ? "" : obj.toString());
                fileWriter.flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        public void println(Object obj) {
            if (print) {
                System.out.println(obj == null ? "" : obj.toString());
            }

            try {
                fileWriter.append(obj == null ? "" : obj.toString()).append("\n");
                fileWriter.flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        public void println() {
            println(null);
        }
    }

    private void print(Log log, ExcelColumn col) {
        ExcelCell head = col.getHeaderCells().get(0);
        log.println(head);
        log.println("============");
        for (ExcelCell dataCell : col.getDataCells()) {
            log.println(dataCell);
        }
        log.println();
    }

    private static void printLine(Log log, String columnLetter, ExcelColumn col, int max) {
        ExcelCell head = col.getHeaderCells().get(0);
        log.print(columnLetter + ": ");
        log.print(StringUtility.makeOneLine(head.getValue()) + ": ");
        int i = 0;
        for (ExcelCell dataCell : col.getDataCells()) {
            log.print(StringUtility.makeOneLine(dataCell.getValue()) + " | ");
            i++;
            if (i >= max) {
                break;
            }
        }
        log.println();
    }

    private Property pex(Model propertyModel, String name, String label) {
        Property p = propertyModel.createProperty("http://example.org/" + name);

        if (propertyModel.containsResource(p)) {
            System.out.println("WARNING: property already exists: " + p);
        }

        propertyModel.add(p, RDF.type, RDF.Property);
        if (label != null) {
            propertyModel.add(p, RDFS.label, label);
        }
        return p;
    }

    @Override
    public Map<String, ModelMatrix> getModelMatrixMap(ApproachContext ctx) throws Exception {
        Map<String, ModelMatrix> m = new HashMap<>();
        //is already created with RmlMapperOnTable
        return m;
    }

    //====================================================================================================
    private void predict(ExcelTable tbl, ExcelColumn col, String relShift, String columnLetter, RmlMapping rmlMapping, Resource triplesMap, Model entityModel, Model propertyModel, Log log) {

        //build an assumption tree
        AssumptionResult root = new AssumptionResult();
        checkAssumptionsRecursively(col, root, entityModel, new ArrayList<>());

        log.println();
        printLine(log, columnLetter, col, 5);
        log.println(root.toStringTree());

        //no solution
        if (root.getChildren().isEmpty()) {
            return;
        }

        String propertyName = "";
        if (!col.getHeaderCells().isEmpty()) {
            String val = col.getHeaderCells().get(0).getValue().toLowerCase();

            for (int i = 0; i < val.length(); i++) {
                char c = val.charAt(i);
                if (c >= 'a' && c <= 'z') {
                    propertyName += c;
                } else {
                    if (!propertyName.endsWith("_")) {
                        propertyName += '_';
                    }
                }
            }
        }

        //a formatted split
        if (root.getChildren().get(0).getAssumption().getClass().equals(StrFormattedAssumption.class)) {

            //for each format a property
            for (AssumptionResult formatResult : root.getChildren()) {

                int formatResultIndex = root.getChildren().indexOf(formatResult);

                //formulate a poMap
                Property p = pex(propertyModel, "" + tbl.getIndex() + "-" + col.getIndex() + "-" + formatResultIndex + "-" + propertyName, col.getHeaderCells().get(0).getValueString());
                Resource[] maps = rmlMapping.createPredicateObjectMap(triplesMap, p);
                rmlMapping.getModel().add(maps[0], RDFS.label, columnLetter);
                Resource oMap = maps[2];

                AssumptionResult selected = selectBest(formatResult.getChildren());
                formatResult.getParameters().put("assumption", selected.getAssumption());
                formatResult.getParameters().put("result", selected);

                //use the assuption and create the RML definition
                formatResult.getAssumption().rml(rmlMapping, oMap, relShift, formatResult);

                //found entities are added to the main entity model
                entityModel.add(selected.getEntityModel());
            }

        } else {
            //no formatted split, just take the first one which is the highest ranked

            //formulate one poMap
            Property p = pex(propertyModel, "" + tbl.getIndex() + "-" + col.getIndex() + "-" + propertyName, col.getHeaderCells().get(0).getValueString());
            Resource[] maps = rmlMapping.createPredicateObjectMap(triplesMap, p);
            rmlMapping.getModel().add(maps[0], RDFS.label, columnLetter);
            Resource oMap = maps[2];

            //use the one with highest score
            AssumptionResult selected = selectBest(root.getChildren());

            //use the assuption and create the RML definition
            selected.getAssumption().rml(rmlMapping, oMap, relShift, selected);

            //found entities are added to the main entity model
            entityModel.add(selected.getEntityModel());
        }
    }

    private AssumptionResult selectBest(List<AssumptionResult> results) {
        //results is already sorted

        AssumptionResult best = results.get(0);

        return best;

        //already done when sorted
        /*
        List<AssumptionResult> sameScoreAsBest = new ArrayList<>();
        for (AssumptionResult rs : results) {
            if (rs.getScore() == best.getScore()) {
                sameScoreAsBest.add(rs);
            }
        }

        //rank decides
        if (sameScoreAsBest.size() > 1) {
            sameScoreAsBest.sort((a, b) -> Integer.compare(b.getAssumption().getRank(), a.getAssumption().getRank()));
        }

        return sameScoreAsBest.get(0);
         */
    }

    private void checkAssumptionsRecursively(ExcelColumn col, AssumptionResult root, Model entityModel, List<Class> usedAssumptions) {

        Histogram<String> typeHist = new Histogram<>();
        Histogram<String> dataformatHist = new Histogram<>();
        Histogram<String> formattedHist = new Histogram<>();
        Map<String, ExcelColumn> format2column = new HashMap<>();
        for (ExcelCell cell : col.getDataCells()) {
            typeHist.add(cell.getCellType());

            //add cell to the special "unformatted" column
            boolean isFormatted = false;

            if (cell.getCellType().equals("numeric")) {
                if (cell.getDataFormat() != null && !cell.getDataFormat().trim().isEmpty()) {
                    dataformatHist.add(cell.getDataFormat());
                }
            } else if (cell.getCellType().equals("string")) {
                if (FormattedResult.isFormatted(cell)) {
                    formattedHist.add("yes");

                    FontStyleParseResult parseResult = FontStyle.parse(cell.getValueRichText());
                    for (FontStyle fs : parseResult.getStyles()) {

                        ExcelColumn formCol = format2column.computeIfAbsent(fs.getHash(), f -> {
                            ExcelColumn c = new ExcelColumn(col.getIndex());
                            c.getHeaderCells().addAll(col.getHeaderCells());
                            return c;
                        });

                        ExcelCell formCell = new ExcelCell();
                        formCell.setId(excelCellId++);
                        formCell.setCellType("string");
                        formCell.setValueString(parseResult.getTextCoveredBy(fs));
                        formCell.setRow(cell.getRow());
                        formCell.setColumn(cell.getColumn());
                        formCell.setAddress(cell.getAddress());

                        formCol.getDataCells().add(formCell);
                    }

                    isFormatted = true;

                } else {
                    formattedHist.add("no");
                }
            }

            if (!isFormatted) {
                ExcelColumn unformCol = format2column.computeIfAbsent("unformatted", f -> {
                    ExcelColumn c = new ExcelColumn(col.getIndex());
                    c.getHeaderCells().addAll(col.getHeaderCells());
                    return c;
                });
                unformCol.getDataCells().add(cell);
            }
        }

        //build an assumption list based on the attributes of the cells in the column
        List<Assumption> assumptions = new ArrayList<>();

        if (typeHist.contains("boolean")) {
            assumptions.add(new BoolAssumption());
        }

        if (typeHist.contains("numeric")) {
            assumptions.add(new NumIntAssumption().setRank(3));
            assumptions.add(new NumDecimalAssumption().setRank(4));
        }
        if (!dataformatHist.isEmpty()) {
            //higher rank because it is more complex then NumInt and NumDecimal
            assumptions.add(new NumFormatAssumption().setRank(5));
        }

        if (typeHist.contains("string")) {
            assumptions.add(new StrPlainSingleLiteralBoolAssumption().setRank(4).setEntityModel(entityModel)); //2021-07-26 change rank 1 to 4 to overvote StrPlainSingleResourceAssumption
            assumptions.add(new StrPlainSingleLiteralStrAssumption());
            assumptions.add(new StrPlainSingleLiteralIntAssumption().setRank(2));
            assumptions.add(new StrPlainSingleLiteralDecimalEnAssumption().setRank(1));
            assumptions.add(new StrPlainSingleLiteralDecimalDeAssumption().setRank(1));
            assumptions.add(new StrPlainSingleResourceAssumption().setRank(3).setEntityModel(entityModel));
            assumptions.add(new StrPlainSingleLiteralDateSimpleAssumption().setRank(2));
            assumptions.add(new StrPlainSingleLiteralDateTimeAssumption().setRank(3));

            //multi
            assumptions.add(new StrPlainMultiResourcesAssumption().setRank(4).setEntityModel(entityModel));
            assumptions.add(new StrPlainMultiLiteralIntAssumption().setRank(0));
        }

        //remove used assumptions to avoid cycle
        if (root.getAssumption() != null) {
            assumptions.removeIf(as -> usedAssumptions.contains(as.getClass()));
        }

        List<AssumptionResult> rootResults = new ArrayList<>();

        //there is no need to create a format node because everything is unformatted
        if (format2column.size() == 1 && format2column.containsKey("unformatted")) {

            //next children are directly under root
            rootResults.add(root);

        } else {
            //we create an extra format assumption

            for (Entry<String, ExcelColumn> entry : format2column.entrySet()) {

                AssumptionResult subRootResult = new AssumptionResult();

                subRootResult.assumption = new StrFormattedAssumption();
                subRootResult.parameters.put("style", entry.getKey());

                //an extra layer in the tree
                root.children.add(subRootResult);
                rootResults.add(subRootResult);
            }
        }

        //potential subcolumns (if formatted)
        List<ExcelColumn> subColumns = new ArrayList<>(format2column.values());
        for (ExcelColumn subColumn : subColumns) {
            int subColumnIndex = subColumns.indexOf(subColumn);
            root = rootResults.get(subColumnIndex);

            List<AssumptionResult> results = new ArrayList<>();

            //check all assumptions and collect results on that
            for (Assumption assumption : assumptions) {
                assumption.check(subColumn, results);
            }

            //choose from the list of results the best one
            results.sort((a, b) -> {

                int cmp = Double.compare(b.getScore(), a.getScore());

                //if score is same (e.g. 1.0)
                if (cmp == 0) {
                    //use higher rank
                    return Integer.compare(b.getAssumption().getRank(), a.getAssumption().getRank());
                }

                return cmp;
            });

            //printLine(subColumn, 5);
            //for (AssumptionResult rs : results) {
            //    System.out.println(rs);
            //}
            //System.out.println("");
            //add the results to the root
            root.children.addAll(results);

            //for each result you might go deeper in recursion for remaining cells
            for (AssumptionResult parentResult : results) {

                //do not go recursive if there are no remaining ones
                if (parentResult.getSuccessful().isEmpty() || parentResult.getRemaining().isEmpty()) {
                    continue;
                }

                //if there are successful ones but also remaining ones
                //it get interesting if we have some successful but also some remaining that could be solved in the next step
                //if(!parentResult.getSuccessful().isEmpty() && !parentResult.getRemaining().isEmpty()) {
                //    
                //}
                ExcelColumn remainingCol = new ExcelColumn(subColumn.getIndex());
                remainingCol.getHeaderCells().addAll(subColumn.getHeaderCells());
                remainingCol.getDataCells().addAll(parentResult.getRemaining());

                List<Class> nextUsedAssumptions = new ArrayList<>(usedAssumptions);
                nextUsedAssumptions.add(parentResult.getAssumption().getClass());
                checkAssumptionsRecursively(remainingCol, parentResult, entityModel, nextUsedAssumptions);
            }
        }

    }

    private abstract class Assumption {

        public int rank = 0;

        //to pass to the assumption an already filled entityModel
        private Model entityModel;

        public abstract void check(ExcelColumn column, List<AssumptionResult> results);

        public abstract void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision);

        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public String toString() {
            return getName() + " (" + rank + ")";
        }

        public int getRank() {
            return rank;
        }

        public Assumption setRank(int rank) {
            this.rank = rank;
            return this;
        }

        public Model getEntityModel() {
            return entityModel;
        }

        public Assumption setEntityModel(Model entityModel) {
            this.entityModel = entityModel;
            return this;
        }
    }

    private static class AssumptionResult {

        //a score between 0.0 - 1.0 telling
        //how good the assumptions were met
        private double score;

        //potential extracted entities
        private Model entityModel;

        //the remaining cells if only partially matched
        private ExcelColumn remainingColumn;

        //cells that where checked correctly
        private Set<ExcelCell> successful;

        //cells that did not pass the check
        private Set<ExcelCell> remaining;

        private Assumption assumption;

        private Map<String, Object> parameters;

        private List<AssumptionResult> children;

        public AssumptionResult() {
            successful = new HashSet<>();
            remaining = new HashSet<>();
            parameters = new HashMap<>();
            children = new ArrayList<>();
            entityModel = ModelFactory.createDefaultModel();
        }

        public AssumptionResult(Assumption assumption) {
            this();
            this.assumption = assumption;
        }

        public ExcelColumn getRemainingColumn() {
            return remainingColumn;
        }

        public void setRemainingColumn(ExcelColumn remainingColumn) {
            this.remainingColumn = remainingColumn;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            if (score == Double.NaN) {
                this.score = 0;
            } else {
                this.score = score;
            }
        }

        public Set<ExcelCell> getSuccessful() {
            return successful;
        }

        public Set<ExcelCell> getRemaining() {
            return remaining;
        }

        public Assumption getAssumption() {
            return assumption;
        }

        public void setAssumption(Assumption assumption) {
            this.assumption = assumption;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("AssumptionResult{score=").append(String.format(Locale.ENGLISH, "%.3f", getScore()));
            sb.append(", assumption=").append(assumption == null ? "null" : assumption);
            sb.append(", parameters=").append(StringUtility.makeOneLine(parameters.toString()));
            sb.append(", successful=").append(successful.size());
            sb.append(", remaining=").append(remaining.size());
            sb.append('}');
            return sb.toString();
        }

        public Model getEntityModel() {
            return entityModel;
        }

        public List<AssumptionResult> getChildren() {
            return children;
        }

        public String toStringTree() {
            StringBuilder sb = new StringBuilder();
            toStringTree("", true, sb);
            return sb.toString();
        }

        private void toStringTree(String prefix, boolean isTail, StringBuilder sb) {
            sb.append(prefix).append(isTail ? "└── " : "├── ").append(toString()).append("\n");
            for (int i = 0; i < children.size() - 1; i++) {
                children.get(i).toStringTree(prefix + (isTail ? "    " : "│   "), false, sb);
            }
            if (children.size() > 0) {
                children.get(children.size() - 1)
                        .toStringTree(prefix + (isTail ? "    " : "│   "), true, sb);
            }
        }

    }

    private static class DiversityResult {

        private Model entityModelSingle;

        private double cellContentDiversity;
        private double stringContentDiversity;
        private double stringTokenDiversity;
        private double characterDiversity;
        private double duplication;
        private double uriRatio;
        private double numberEntities;

        private MinAvgMaxSdDouble stringLength;

        private Set<ExcelCell> successful;
        private Set<ExcelCell> remaining;

        private Set<String> distinctStringContent;
        private Set<String> distinctTokens;
        private Set<String> distinctChars;
        private List<Object> itemList;

        private static Set<String> allChars;

        private ExcelColumn column;

        static {
            allChars = new HashSet<>();
            for (int i = 32; i <= 126; i++) {
                char c = (char) i;

                if (isPrintableChar(c)) {
                    allChars.add("" + c);
                }
            }
        }

        private static boolean isPrintableChar(char c) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            return (!Character.isISOControl(c))
                    && c != KeyEvent.CHAR_UNDEFINED
                    && block != null
                    && block != Character.UnicodeBlock.SPECIALS;
        }

        public DiversityResult() {
            entityModelSingle = ModelFactory.createDefaultModel();
            successful = new HashSet<>();
            remaining = new HashSet<>();
            itemList = new ArrayList<>();
        }

        //separators can be null, can be regex "[+&,]", caller needs to quote
        public static DiversityResult calculate(ExcelColumn column, String separatorsRegex, Model entityModel) {
            DiversityResult result = new DiversityResult();
            result.column = column;

            Set<String> distinctContent = new HashSet<>();
            column.getDataCells().forEach(cell -> distinctContent.add(cell.getValue()));

            result.stringLength = new MinAvgMaxSdDouble();

            double stringTypeCount = 0;
            double uriCount = 0;
            result.distinctTokens = new HashSet<>();
            result.distinctStringContent = new HashSet<>();
            result.distinctChars = new HashSet<>();
            double totalTokenCount = 0;
            double blankStringCount = 0;

            //AssumptionResult result = new AssumptionResult(this);
            for (ExcelCell cell : column.getDataCells()) {
                if (cell.getCellType().equals("string") && !FormattedResult.isFormatted(cell)) {

                    //no separator so the full cell
                    if (separatorsRegex == null) {
                        stringTypeCount++;

                        if (cell.getValueString().isEmpty()) {
                            blankStringCount++;
                        }

                        if(isURI(cell.getValueString())) {
                            uriCount++;
                        }
                        
                        List<String> tokens = new ArrayList<>(Arrays.asList(StringUtils.splitByCharacterTypeCamelCase(cell.getValueString())));
                        tokens.removeIf(token -> token.matches("\\s+"));

                        totalTokenCount += tokens.size();
                        result.distinctTokens.addAll(tokens);

                        result.distinctStringContent.add(cell.getValueString());

                        result.stringLength.add(cell.getValueString().length());

                        for (int i = 0; i < cell.getValueString().length(); i++) {
                            result.distinctChars.add("" + cell.getValueString().charAt(i));
                        }

                        if (!cell.getValueString().trim().isEmpty()) {
                            result.itemList.add(cell.getValueString());
                        }

                    } else {
                        //with separator
                        String[] segments = cell.getValueString().split(separatorsRegex);
                        for (String segment : segments) {
                            segment = segment.trim();

                            stringTypeCount++;

                            if (segment.isEmpty()) {
                                blankStringCount++;
                            }
                            
                            if(isURI(segment)) {
                                uriCount++;
                            }

                            List<String> tokens = new ArrayList<>(Arrays.asList(StringUtils.splitByCharacterTypeCamelCase(segment)));
                            tokens.removeIf(token -> token.matches("\\s+"));

                            totalTokenCount += tokens.size();
                            result.distinctTokens.addAll(tokens);

                            result.distinctStringContent.add(segment);

                            result.stringLength.add(segment.length());

                            for (int i = 0; i < segment.length(); i++) {
                                result.distinctChars.add("" + segment.charAt(i));
                            }

                            if (!segment.trim().isEmpty()) {
                                result.itemList.add(segment);
                            }
                        }
                    }

                    result.successful.add(cell);
                } else {
                    result.remaining.add(cell);
                    //just add something that is not a string, so that dupImpl can work
                    result.itemList.add(0);
                }
            }

            //in case it is single resource
            Resource type = result.entityModelSingle.createResource("urn:uuid:" + UUID.randomUUID().toString());
            for (String lbl : result.distinctStringContent) {

                lbl = lbl.trim();

                if (lbl.isEmpty()) {
                    continue;
                }

                
                boolean exists = false;

                //check if the entity is already in the model
                if (entityModel != null) {
                    exists = entityModel.listStatements(null, null, lbl).hasNext();
                }

                if (!exists) {
                    if(lbl.matches("\\d+")) {
                        result.numberEntities++;
                    }
                    
                    Resource res = result.entityModelSingle.createResource("urn:uuid:" + UUID.randomUUID().toString());
                    result.entityModelSingle.add(res, RDF.type, type);
                    result.entityModelSingle.add(res, RDFS.label, lbl);
                }
            }

            result.cellContentDiversity = distinctContent.size() / (double) column.getDataCells().size();
            result.stringContentDiversity = result.distinctStringContent.size() / stringTypeCount;
            result.stringTokenDiversity = result.distinctTokens.size() / totalTokenCount;
            result.characterDiversity = result.distinctChars.size() / allChars.size();

            result.uriRatio = uriCount / stringTypeCount;
            
            result.duplication = dupImplV2(result.itemList);

            return result;
        }

    }

    private static class FormattedResult {

        private Map<String, ExcelColumn> format2column;

        public FormattedResult() {
            format2column = new HashMap<>();
        }

        public static FormattedResult calculate(ExcelColumn column) {
            FormattedResult result = new FormattedResult();

            for (ExcelCell cell : column.getDataCells()) {

                if (!isFormatted(cell)) {
                    continue;
                }

                Document doc;
                try {
                    doc = Jsoup.parse(cell.getValueRichText(), "", Parser.xmlParser());
                } catch (Exception e) {
                    continue;
                }

                Elements leafs = doc.getAllElements();
                leafs.removeIf(e -> e.childrenSize() > 0);

                //try to get the leaf elements
                for (Element e : leafs.toArray(new Element[0])) {
                    if (e.tagName().equals("br")) {
                        leafs.remove(e);
                        leafs.add(e.parent());
                    }
                }

                //get the different formatting variations of the texts
                //TODO improve
                for (Element leaf : leafs) {

                    Element cur = leaf;
                    String formatKey = "";
                    while (true) {

                        if (!cur.tagName().equals("br")) {
                            formatKey += cur.tagName() + "-" + cur.attr("face") + "-" + cur.attr("color") + ";";
                        }

                        cur = cur.parent();

                        if (cur == null) {
                            break;
                        }
                    }

                    ExcelColumn texts = result.format2column.computeIfAbsent(formatKey, fmt -> new ExcelColumn());

                    ExcelCell plainCell = new ExcelCell();
                    plainCell.setId(excelCellId++);
                    plainCell.setCellType("string");
                    plainCell.setValueString(leaf.text());
                    texts.getDataCells().add(plainCell);
                }
            }

            return result;
        }

        public static boolean isFormatted(ExcelCell cell) {
            return cell.getValueRichText() != null && !cell.getValueRichText().trim().isEmpty();
        }

    }

    private static boolean isURI(String str) {
        try {
            str = str.trim();
            return str.startsWith("http://") || str.startsWith("https://");
        } catch(Exception e) {
            return false;
        }
    }
    
    ///celltype=bool/
    private class BoolAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            AssumptionResult result = new AssumptionResult(this);
            for (ExcelCell cell : column.getDataCells()) {
                if (cell.getCellType().equals("boolean")) {
                    result.getSuccessful().add(cell);
                } else {
                    result.getRemaining().add(cell);
                }
            }

            result.setScore(result.getSuccessful().size() / (double) column.getDataCells().size());

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            rmlMapping.createReference(oMap, relShift + ".valueBoolean");
            rmlMapping.datatype(oMap, XSD.xboolean);
        }

    }

    ///celltype=num/numtype=int/
    private class NumIntAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            MinAvgMaxSdDouble dist = new MinAvgMaxSdDouble();

            AssumptionResult result = new AssumptionResult(this);
            for (ExcelCell cell : column.getDataCells()) {
                if (cell.getCellType().equals("numeric")) {

                    int[] places = getNumberOfPlaces(cell.getValueNumeric());
                    if (places[1] == 0) {
                        dist.add(cell.getValueNumeric());
                        result.getSuccessful().add(cell);
                    } else {
                        result.getRemaining().add(cell);
                    }
                } else {
                    result.getRemaining().add(cell);
                }
            }

            result.setScore(result.getSuccessful().size() / (double) column.getDataCells().size());
            result.getParameters().put("distribution", dist);

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            rmlMapping.createReference(oMap, relShift + ".valueInt");

            MinAvgMaxSdDouble dist = (MinAvgMaxSdDouble) decision.getParameters().get("distribution");

            if (dist.getMin() >= 1900 && dist.getMax() <= 2030) {
                rmlMapping.datatype(oMap, XSD.gYear);
            } else {
                rmlMapping.datatype(oMap, XSD.integer);
            }
        }

    }

    ///celltype=num/numtype=decimal/
    private class NumDecimalAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            AssumptionResult result = new AssumptionResult(this);
            for (ExcelCell cell : column.getDataCells()) {
                if (cell.getCellType().equals("numeric")) {

                    int[] places = getNumberOfPlaces(cell.getValueNumeric());
                    if (places[1] > 0 || cell.getValueNumeric() == 0.0) {
                        result.getSuccessful().add(cell);
                    } else {
                        result.getRemaining().add(cell);
                    }
                } else {
                    result.getRemaining().add(cell);
                }
            }

            result.setScore(result.getSuccessful().size() / (double) column.getDataCells().size());

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            rmlMapping.createReference(oMap, relShift + ".valueNumeric");
            rmlMapping.datatype(oMap, XSD.decimal);
        }

    }

    protected static int[] getNumberOfPlaces(double d) {
        DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        df.setMaximumFractionDigits(340);
        String str = df.format(Math.abs(d));
        String[] segs = str.split("\\.");

        if (segs.length == 1) {
            return new int[]{segs[0].length(), 0};
        }

        return new int[]{
            segs[0].length(),
            segs[1].length()
        };
    }

    ///celltype=num/numtype=dataformat/format=date etc
    private class NumFormatAssumption extends Assumption {

        private Map<String, List<String>> type2formats;

        public NumFormatAssumption() {
            type2formats = new HashMap<>();
            type2formats.put("date", Arrays.asList(
                    //de
                    "TT.MM.JJ",
                    "TT.MM.JJJJ",
                    "T. MMM JJ",
                    "T. MMM JJJJ",
                    "T. MMM. JJJJ",
                    "T. MMMM JJJJ",
                    "NN, T. MMM JJ",
                    "NN TT.MMM JJ",
                    "NN, T. MMMM JJJJ",
                    //en
                    "yy\\-mm\\-d;@",
                    "yyyy\\-mm\\-d;@",
                    "m/dd/yyyy;@",
                    "M/DD/YYYY;@",
                    "d/m/yyyy;@",
                    "mm/dd/yyyy;@",
                    "dd/mm/yyyy;@",
                    "m/dd/yyyy;@",
                    "d/m/yyyy;@",
                    "mm/dd/yyyy;@",
                    "dd/mm/yyyy;@",
                    "dd/m/yyyy;@",
                    "yy/mm/dd;@",
                    "d/mm/yyyy;@",
                    "mm/dd/yy;@",
                    "m/dd/yy;@",
                    "d/m/yy;@",
                    "d/mm/yy;@",
                    "m/d/yy",
                    "MM/DD/YY",
                    "MM/DD/YYYY",
                    "MM/DD/YYYY",
                    "MMM D, YY",
                    "MMM D, YYYY",
                    "D. MMM. YYYY",
                    "MMMM D, YYYY",
                    "D. MMMM YYYY",
                    "NN, MMM D, YY"
            ));
            type2formats.put("dateTime", Arrays.asList(
                    //de
                    "HH:MM",
                    "HH:MM:SS",
                    "HH:MM AM/PM",
                    "HH:MM:SS AM/PM",
                    "[HH]:MM:SS",
                    "MM:SS,00",
                    "[HH]:MM:SS,00",
                    "TT.MM.JJJJ HH:MM",
                    "TT.MM.JJ HH:MM",
                    "TT.MM.JJJJ HH:MM:SS",
                    "JJJJ-MM-TT HH:MM:SS",
                    "JJJJ-MM-TT\"T\"HH:MM:SS",
                    //en
                    "m/d/yy\\ h:mm;@",
                    "m/d/yy h:mm",
                    "m/d/yy h:mm",
                    "MM:SS.00",
                    "[HH]:MM:SS.00",
                    "MM/DD/YYYY HH:MM AM/PM",
                    "MM/DD/YY HH:MM AM/PM",
                    "MM/DD/YYYY HH:MM:SS",
                    "YYYY-MM-DD HH:MM:SS",
                    "YYYY-MM-DD\"T\"HH:MM:SS"
            ));
        }

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            boolean printDataFormat = false;

            //for each type a assumption result
            Map<String, AssumptionResult> type2result = new HashMap<>();
            for (Entry<String, List<String>> entry : type2formats.entrySet()) {
                AssumptionResult result = new AssumptionResult(this);
                result.getParameters().put("type", entry.getKey());
                type2result.put(entry.getKey(), result);
            }

            for (ExcelCell cell : column.getDataCells()) {
                if (cell.getCellType().equals("numeric")) {

                    if (printDataFormat) {
                        System.out.println("[DataFormat] " + cell.getDataFormat());
                    }

                    for (Entry<String, List<String>> entry : type2formats.entrySet()) {

                        AssumptionResult result = type2result.get(entry.getKey());

                        //if dataformat matches, we take it
                        for (String dataFormat : entry.getValue()) {
                            //TODO later you can do fuzzy matching here
                            if (dataFormat.equalsIgnoreCase(cell.getDataFormat())) {
                                result.getSuccessful().add(cell);

                                //rest get the cell as remaining
                                type2result.values().forEach(rs -> {
                                    if (rs != result) {
                                        rs.getRemaining().add(cell);
                                    }
                                });

                                break;
                            }
                        }
                    }

                } else {
                    //add remaining to all of the types
                    type2result.values().forEach(result -> result.getRemaining().add(cell));
                }
            }

            //calculate score of each
            type2result.values().forEach(result -> result.setScore(result.getSuccessful().size() / (double) column.getDataCells().size()));

            results.addAll(type2result.values());
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {

            String type = (String) decision.getParameters().get("type");

            if (type.equals("dateTime")) {

                rmlMapping.createFunctionMap(oMap,
                        ResourceFactory.createResource(fnNs + "parseDateTime"),
                        RmlMapping.Param.r(relShift + ".json", String.class)
                );
                rmlMapping.datatype(oMap, XSD.dateTime);

            } else if (type.equals("date")) {

                rmlMapping.createFunctionMap(oMap,
                        ResourceFactory.createResource(fnNs + "parseDate"),
                        RmlMapping.Param.r(relShift + ".json", String.class)
                );
                rmlMapping.datatype(oMap, XSD.date);

            }

        }

    }

    ///celltype=str/strtype=plain/cardinality=single/content=resource/
    private class StrPlainSingleResourceAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            DiversityResult divRes = DiversityResult.calculate(column, null, getEntityModel());

            //low diversity -> indicator for resources
            //high diversity -> indicator for strings
            //high duplication -> indicator for resources
            double score = divRes.duplication;

            AssumptionResult result = new AssumptionResult(this);

            result.getSuccessful().addAll(divRes.successful);
            result.getRemaining().addAll(divRes.remaining);
            result.setScore(score);

            result.getEntityModel().add(divRes.entityModelSingle);

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            rmlMapping.createFunctionMap(oMap,
                    ResourceFactory.createResource(fnNs + "entityLinking"),
                    RmlMapping.Param.r(relShift + ".valueString", String.class)
            );
            rmlMapping.termTypeIRI(oMap);
        }

    }

    ///celltype=str/strtype=plain/cardinality=single/content=literal/type=str/
    private class StrPlainSingleLiteralStrAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            DiversityResult divRes = DiversityResult.calculate(column, null, getEntityModel());

            //low diversity -> indicator for resources
            //high diversity -> indicator for strings
            //double score = toScoreDiversity(divRes);
            //low duplication -> indicator for strings
            double score = 1.0 - divRes.duplication;

            AssumptionResult result = new AssumptionResult(this);

            result.getSuccessful().addAll(divRes.successful);
            result.getRemaining().addAll(divRes.remaining);
            result.setScore(score);
            
            result.getParameters().put("uriRatio", divRes.uriRatio);

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            rmlMapping.createReference(oMap, relShift + ".value");
            
            if((double) decision.getParameters().get("uriRatio") > 0.75) {
                rmlMapping.termTypeIRI(oMap);
            } else {
                rmlMapping.datatype(oMap, XSD.xstring);
            }
        }

    }

    ///celltype=str/strtype=plain/cardinality=multiple/content=resources/
    private class StrPlainMultiResourcesAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            Map<Character, Integer> char2count = getSeparators(column);

            //no separator found so no results
            if (char2count.isEmpty()) {
                return;
            }

            //System.out.println("possible separators: " + StringUtility.makeOneLine(char2count.toString()));
            //multiple separators implementation
            StringJoiner sj = new StringJoiner("", "[", "]");
            char2count.keySet().forEach(sep -> sj.add(RegexUtility.quote("" + sep)));
            String separatorsRegex = sj.toString();
            DiversityResult divRes = DiversityResult.calculate(column, separatorsRegex, getEntityModel());

            //high duplication -> indicator for resources
            double score = divRes.duplication;

            AssumptionResult result = new AssumptionResult(this);

            result.getSuccessful().addAll(divRes.successful);
            result.getRemaining().addAll(divRes.remaining);
            result.setScore(score);
            result.getParameters().put("separators", separatorsRegex);

            result.getEntityModel().add(divRes.entityModelSingle);

            results.add(result);

            //single separator implementation
            /*
            for(Entry<Character, Integer> entry : char2count.entrySet()) {
                
                String separator = entry.getKey().toString();
                
                DiversityResult divRes = DiversityResult.calculate(column, separator);
            
                //low diversity -> indicator for resources
                //high diversity -> indicator for strings

                double score = 1.0 - divRes.stringContentDiversity;

                AssumptionResult result = new AssumptionResult(this);

                result.getSuccessful().addAll(divRes.successful);
                result.getRemaining().addAll(divRes.remaining);
                result.setScore(score);
                result.getParameters().put("separator", separator);

                result.getEntityModel().add(divRes.entityModelSingle);

                results.add(result);
            }
             */
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            rmlMapping.createFunctionMap(oMap,
                    ResourceFactory.createResource(fnNs + "entityLinking"),
                    RmlMapping.Param.r(relShift + ".valueString", String.class)
            );
            rmlMapping.termTypeIRI(oMap);
        }

    }

    ///celltype=str/strtype=plain/cardinality=single/content=literal/type=num/numtype=int (parseNumber)
    private class StrPlainSingleLiteralIntAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            MinAvgMaxSdDouble dist = new MinAvgMaxSdDouble();

            AssumptionResult result = new AssumptionResult(this);
            for (ExcelCell cell : column.getDataCells()) {
                if (cell.getCellType().equals("string")) {

                    //maybe the 0 is important so we assume a string id here
                    if (cell.getValueString().startsWith("0")) {
                        result.getRemaining().add(cell);
                    } else {

                        //TODO later check thousender (1,000) and the problem with "," vs "."
                        try {
                            int value = Integer.parseInt(cell.getValueString().trim());
                            dist.add(value);
                            result.getSuccessful().add(cell);
                        } catch (Exception e) {
                            result.getRemaining().add(cell);
                        }
                    }

                } else if (cell.getCellType().equals("numeric")) {

                    int[] places = getNumberOfPlaces(cell.getValueNumeric());
                    if (places[1] == 0) {
                        dist.add(cell.getValueNumeric());
                        result.getSuccessful().add(cell);
                    } else {
                        result.getRemaining().add(cell);
                    }

                } else {
                    result.getRemaining().add(cell);
                }
            }

            result.setScore(result.getSuccessful().size() / (double) column.getDataCells().size());
            result.getParameters().put("distribution", dist);

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            rmlMapping.createFunctionMap(oMap,
                    ResourceFactory.createResource(fnNs + "parseNumber"),
                    RmlMapping.Param.r(relShift + ".json", String.class),
                    RmlMapping.Param.c("en"),
                    RmlMapping.Param.c(true)
            );

            MinAvgMaxSdDouble dist = (MinAvgMaxSdDouble) decision.getParameters().get("distribution");

            if (dist.getMin() >= 1900 && dist.getMax() <= 2030) {
                rmlMapping.datatype(oMap, XSD.gYear);
            } else {
                rmlMapping.datatype(oMap, XSD.integer);
            }
        }

    }

    ///celltype=str/strtype=plain/cardinality=single/content=literal/type=num/numtype=decimal/lang=en (parseNumber)
    private class StrPlainSingleLiteralDecimalEnAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            AssumptionResult result = new AssumptionResult(this);
            for (ExcelCell cell : column.getDataCells()) {
                if (cell.getCellType().equals("string")) {

                    if (cell.getValueString().startsWith("0")) {
                        result.getRemaining().add(cell);
                    } else {
                        try {
                            Double.parseDouble(cell.getValueString().trim());
                            result.getSuccessful().add(cell);
                        } catch (Exception e) {
                            result.getRemaining().add(cell);
                        }
                    }

                } else if (cell.getCellType().equals("numeric")) {

                    int[] places = getNumberOfPlaces(cell.getValueNumeric());
                    if (places[1] > 0) {
                        result.getSuccessful().add(cell);
                    } else {
                        result.getRemaining().add(cell);
                    }
                } else {
                    result.getRemaining().add(cell);
                }
            }

            result.setScore(result.getSuccessful().size() / (double) column.getDataCells().size());

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            rmlMapping.createFunctionMap(oMap,
                    ResourceFactory.createResource(fnNs + "parseNumber"),
                    RmlMapping.Param.r(relShift + ".json", String.class),
                    RmlMapping.Param.c("en"),
                    RmlMapping.Param.c(false)
            );
            rmlMapping.datatype(oMap, XSD.decimal);
        }

    }

    ///celltype=str/strtype=plain/cardinality=single/content=literal/type=num/numtype=decimal/lang=de (parseNumber)
    private class StrPlainSingleLiteralDecimalDeAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            AssumptionResult result = new AssumptionResult(this);
            for (ExcelCell cell : column.getDataCells()) {
                if (cell.getCellType().equals("string")) {

                    if (cell.getValueString().startsWith("0")) {
                        result.getRemaining().add(cell);
                    } else {
                        try {
                            String strSwitched = cell.getValueString().trim().replace('.', (char) 1).replace(",", ".").replace((char) 1, ',');
                            Double.parseDouble(strSwitched);
                            result.getSuccessful().add(cell);
                        } catch (Exception e) {
                            result.getRemaining().add(cell);
                        }
                    }

                } else if (cell.getCellType().equals("numeric")) {

                    int[] places = getNumberOfPlaces(cell.getValueNumeric());
                    if (places[1] > 0) {
                        result.getSuccessful().add(cell);
                    } else {
                        result.getRemaining().add(cell);
                    }
                } else {
                    result.getRemaining().add(cell);
                }
            }

            result.setScore(result.getSuccessful().size() / (double) column.getDataCells().size());

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            rmlMapping.createFunctionMap(oMap,
                    ResourceFactory.createResource(fnNs + "parseNumber"),
                    RmlMapping.Param.r(relShift + ".json", String.class),
                    RmlMapping.Param.c("de"),
                    RmlMapping.Param.c(false)
            );
            rmlMapping.datatype(oMap, XSD.decimal);
        }

    }

    ///celltype=str/strtype=plain/cardinality=single/content=literal/type=bool/
    private class StrPlainSingleLiteralBoolAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            DiversityResult divRes = DiversityResult.calculate(column, null, getEntityModel());

            divRes.distinctStringContent.removeIf(str -> str.trim().isEmpty());

            if (getEntityModel() != null) {
                //remove it if we already found it as an entity
                divRes.distinctStringContent.removeIf(str -> getEntityModel().listStatements(null, null, str).hasNext());
            }

            Map<String, String> lowercaseMap = new HashMap<>();

            //to lowercase (but need to put original in function parameter)
            Set<String> symbolSet = new HashSet<>();
            for (String dsc : divRes.distinctStringContent) {
                symbolSet.add(dsc.toLowerCase());
                lowercaseMap.put(dsc.toLowerCase(), dsc);
            }

            List<String> symbols = new ArrayList<>(symbolSet);

            List<String> trueIndicators = Arrays.asList("x", "y");
            List<String> falseIndicators = Arrays.asList("-", "n");

            //e.g. only "x" or "x"/"--" or "yes"/"no", "Y"/"N", but also "M"/"F" 
            if (!symbols.isEmpty() && symbols.size() <= 2) {

                List<String> paramNames = new ArrayList<>(Arrays.asList("trueSymbols", "falseSymbols"));

                boolean first = true;
                for (String t : trueIndicators) {
                    if (symbols.get(0).contains(t)) {
                        first = true;
                    }
                }
                for (String f : falseIndicators) {
                    if (symbols.get(0).contains(f)) {
                        first = false;
                    }
                }

                if (symbols.size() == 1 && !first) {
                    Collections.reverse(paramNames);
                }

                if (symbols.size() == 2) {
                    boolean second = false;
                    for (String t : trueIndicators) {
                        if (symbols.get(1).contains(t)) {
                            second = true;
                        }
                    }
                    for (String f : falseIndicators) {
                        if (symbols.get(1).contains(f)) {
                            second = false;
                        }
                    }

                    if (!first && second) {
                        Collections.reverse(symbols);
                    }
                }

                AssumptionResult result = new AssumptionResult(this);

                //also include booleans and numeric
                for (ExcelCell cell : divRes.remaining.toArray(new ExcelCell[0])) {
                    if (cell.getCellType().equals("boolean")) {
                        divRes.remaining.remove(cell);
                        divRes.successful.add(cell);
                    } else if (cell.getCellType().equals("numeric")) {
                        if (cell.getValueNumeric() == 1.0 || cell.getValueNumeric() == 0.0) {
                            divRes.remaining.remove(cell);
                            divRes.successful.add(cell);
                        }
                    }
                }

                result.getSuccessful().addAll(divRes.successful);
                result.getRemaining().addAll(divRes.remaining);
                result.getParameters().put(paramNames.get(0), Arrays.asList(lowercaseMap.get(symbols.get(0))));
                result.getParameters().put(paramNames.get(1), symbols.size() == 1 ? Arrays.asList() : Arrays.asList(lowercaseMap.get(symbols.get(1))));

                double rate = divRes.successful.size() / (double) column.getDataCells().size();

                //the shorter the better
                //one symbol: 1.0
                //two symbols: 0.5
                //"yes" "no" => 1/3 = 0.333
                //result.setScore(rate * 0.8 + (1.0 / divRes.stringLength.getAvg()) * 0.2);
                
                double avgLen = divRes.stringLength.getAvg();
                
                double threshold = 3.5;
                
                double boolScore = 1.0 - (avgLen - 1.0) / (threshold - 1.0);
                
                double dist = 1.0 - divRes.duplication;
                
                result.setScore(divRes.duplication + dist * boolScore);

                results.add(result);
            }
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            List<String> trueSymbols = (List<String>) decision.getParameters().get("trueSymbols");
            List<String> falseSymbols = (List<String>) decision.getParameters().get("falseSymbols");

            rmlMapping.createFunctionMap(oMap,
                    ResourceFactory.createResource(fnNs + "parseBoolean"),
                    RmlMapping.Param.r(relShift + ".json", String.class),
                    RmlMapping.Param.c(rmlMapping.createList(trueSymbols.toArray(new String[0])), List.class),
                    RmlMapping.Param.c(rmlMapping.createList(falseSymbols.toArray(new String[0])), List.class)
            );
            rmlMapping.datatype(oMap, XSD.xboolean);
        }

    }

    private class StrPlainMultiLiteralIntAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            Map<Character, Integer> char2count = getSeparators(column);

            //no separator found so no results
            if (char2count.isEmpty()) {
                return;
            }

            //multiple separators implementation
            StringJoiner sj = new StringJoiner("", "[", "]");
            char2count.keySet().forEach(sep -> sj.add(RegexUtility.quote("" + sep)));
            String separatorsRegex = sj.toString();
            DiversityResult divRes = DiversityResult.calculate(column, separatorsRegex, getEntityModel());

            MinAvgMaxSdDouble dist = new MinAvgMaxSdDouble();

            AssumptionResult result = new AssumptionResult(this);

            double successful = 0;

            for (String segment : divRes.distinctStringContent) {
                if (segment.matches("(-|\\+)?\\d+")) {
                    int i = Integer.parseInt(segment);
                    dist.add(i);
                    successful++;
                }
            }

            result.setScore(successful / (double) divRes.distinctStringContent.size());
            result.getParameters().put("distribution", dist);

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {
            rmlMapping.createFunctionMap(oMap,
                    ResourceFactory.createResource(fnNs + "parseNumber"),
                    RmlMapping.Param.r(relShift + ".json", String.class),
                    RmlMapping.Param.c("en"),
                    RmlMapping.Param.c(true)
            );

            MinAvgMaxSdDouble dist = (MinAvgMaxSdDouble) decision.getParameters().get("distribution");

            if (dist.getMin() >= 1900 && dist.getMax() <= 2030) {
                rmlMapping.datatype(oMap, XSD.gYear);
            } else {
                rmlMapping.datatype(oMap, XSD.integer);
            }
        }

    }

    private static Map<Character, Integer> getSeparators(ExcelColumn column) {
        //first search for potential separators
        Map<Character, Integer> char2count = new HashMap<>();

        // \n  ,   ;    :    are reasonable separators in written text
        //they can hook on words like "DFKI, ..."
        //other symbols should have a space on the left side like "DFKI + TUKL"
        Set<Character> attachedSeparators = new HashSet<>(Arrays.asList('\n', '\t', ',', ';', ':', ')', ']', '>', '}'));

        int strCellCount = 0;
        for (ExcelCell cell : column.getDataCells()) {
            if (cell.getCellType().equals("string")) {

                strCellCount++;

                String str = cell.getValueString().toLowerCase();
                for (int i = 0; i < str.length(); i++) {

                    char c = str.charAt(i);

                    char prev = i > 0 ? str.charAt(i - 1) : '^';

                    if ((c < 'a' || c > 'z')
                            && (c < '0' || c > '9')
                            && c != '_' && c != ' '
                            && c != 'í' && c != 'á' && c != 'ô' && c != 'é' && c != 'ó' && c != 'ú' && c != 'ý' && c != 'ù') {

                        //a not attached separator must have a space on the left side
                        //this avoids that we get '-' or '.' or '/' as separators in "MG 5-ZDG11.2106/39"
                        if (!attachedSeparators.contains(c) && prev != ' ') {
                            continue;
                        }

                        int count = char2count.computeIfAbsent(c, cc -> 0);
                        char2count.put(c, count + 1);
                    }
                }
            }
        }

        return char2count;
    }

    private class StrFormattedAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {
            //check not necessary, already done in checkAssumptionsRecursively
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {

            String style = (String) decision.getParameters().get("style");
            Assumption assumption = (Assumption) decision.getParameters().get("assumption");

            FontStyle fontStyle = FontStyle.fromHash(style);

            String termType = R2RML.Literal.getURI();
            String datatype = ""; //XSD.date.getURI()

            String assumptionName = assumption.getClass().getSimpleName();
            if (assumptionName.contains("Int")) {
                datatype = XSD.integer.getURI();

                AssumptionResult childResult = (AssumptionResult) decision.getParameters().get("result");

                MinAvgMaxSdDouble dist = (MinAvgMaxSdDouble) childResult.getParameters().get("distribution");
                if (dist.getMin() >= 1900 && dist.getMax() <= 2030) {
                    datatype = XSD.gYear.getURI();
                }

            } else if (assumptionName.contains("Decimal")) {
                datatype = XSD.decimal.getURI();
            } else if (assumptionName.contains("Bool")) {
                datatype = XSD.xboolean.getURI();
            } else if (assumptionName.contains("Resource")) {
                termType = R2RML.IRI.getURI();
            } else if (assumptionName.contains("DateSimple")) {
                datatype = XSD.date.getURI();
            } else if (assumptionName.contains("DateTime")) {
                datatype = XSD.date.getURI();
            } else {
                datatype = XSD.xstring.getURI();
            }

            if(fontStyle == null) {
                
                //getEntitiesByUnformatted
                rmlMapping.createFunctionMap(oMap,
                        ResourceFactory.createResource(fnNs + "getEntitiesByUnformatted"),
                        RmlMapping.Param.r(relShift + ".valueRichText", String.class),
                        RmlMapping.Param.c(termType),
                        RmlMapping.Param.c(datatype)
                );
                
            } else if (fontStyle.hasTag()) {

                //getEntitiesByTag
                rmlMapping.createFunctionMap(oMap,
                        ResourceFactory.createResource(fnNs + "getEntitiesByTag"),
                        RmlMapping.Param.r(relShift + ".valueRichText", String.class),
                        RmlMapping.Param.c(fontStyle.getTag()),
                        RmlMapping.Param.c(termType),
                        RmlMapping.Param.c(datatype)
                );

            } else {

                //getEntitiesByColor
                rmlMapping.createFunctionMap(oMap,
                        ResourceFactory.createResource(fnNs + "getEntitiesByColor"),
                        RmlMapping.Param.r(relShift + ".valueRichText", String.class),
                        RmlMapping.Param.c(fontStyle.getColor() == null ? "" : "#" + Integer.toHexString(fontStyle.getColor().getRGB()).substring(2)),
                        RmlMapping.Param.c(termType),
                        RmlMapping.Param.c(datatype)
                );
            }

            if (!datatype.isEmpty()) {
                rmlMapping.datatype(oMap, rmlMapping.getModel().createResource(datatype));
            } else {
                rmlMapping.termTypeIRI(oMap);
            }

        }

    }

    private class StrPlainSingleLiteralDateSimpleAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            AssumptionResult result = new AssumptionResult(this);

            for (ExcelCell cell : column.getDataCells()) {

                if (cell.getCellType().equals("string")) {

                    List<String> dates = parseDatesToXSD(cell.getValueString());
                    //YYYY-MM-DD => 10 chars

                    //larger date time wins
                    for (String date : dates.toArray(new String[0])) {
                        for (String dateTime : dates.toArray(new String[0])) {
                            if (dateTime.length() > date.length() && dateTime.startsWith(date)) {
                                dates.remove(date);
                            }
                        }
                    }

                    boolean hasDate = false;
                    for (String date : dates) {
                        if (date.length() == 10) {
                            hasDate = true;
                            break;
                        }
                    }

                    if (hasDate) {
                        result.getSuccessful().add(cell);
                    } else {
                        result.getRemaining().add(cell);
                    }

                } else if(cell.getCellType().equals("numeric")) {
                    //could be number as date
                    result.getSuccessful().add(cell);
                        
                } else {
                    result.getRemaining().add(cell);
                }

            }

            result.setScore(result.getSuccessful().size() / (double) column.getDataCells().size());

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {

            rmlMapping.createFunctionMap(oMap,
                    ResourceFactory.createResource(fnNs + "parseDate"),
                    RmlMapping.Param.r(relShift + ".json", String.class)
            );

            rmlMapping.datatype(oMap, XSD.date);
        }

    }

    private class StrPlainSingleLiteralDateTimeAssumption extends Assumption {

        @Override
        public void check(ExcelColumn column, List<AssumptionResult> results) {

            AssumptionResult result = new AssumptionResult(this);

            for (ExcelCell cell : column.getDataCells()) {

                if (cell.getCellType().equals("string")) {

                    List<String> dates = parseDatesToXSD(cell.getValueString());
                    //YYYY-MM-DD => 10 chars

                    boolean hasDateTime = false;
                    for (String date : dates) {
                        if (date.length() > 10) {
                            hasDateTime = true;
                            break;
                        }
                    }

                    if (hasDateTime) {
                        result.getSuccessful().add(cell);
                    } else {
                        result.getRemaining().add(cell);
                    }

                } else if(cell.getCellType().equals("numeric")) {
                    //could be number as date
                    result.getSuccessful().add(cell);
                        
                } else {
                    result.getRemaining().add(cell);
                }

            }

            result.setScore(result.getSuccessful().size() / (double) column.getDataCells().size());

            results.add(result);
        }

        @Override
        public void rml(RmlMapping rmlMapping, Resource oMap, String relShift, AssumptionResult decision) {

            rmlMapping.createFunctionMap(oMap,
                    ResourceFactory.createResource(fnNs + "parseDateTime"),
                    RmlMapping.Param.r(relShift + ".json", String.class)
            );

            rmlMapping.datatype(oMap, XSD.dateTime);
        }

    }

    private static DateTimeFormatter xsdDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static DateTimeFormatter xsdDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static List<String> parseDatesToXSD(String text) {
        /*
        if (locale == Locale.ENGLISH) {
            dateTimeStringFormats.add("MM/dd/yyyy HH:mm:ss");
            dateTimeStringFormats.add("yyyy-MM-dd HH:mm:ss");
            dateTimeStringFormats.add("HH:mm:ss yyyy-MM-dd");
            dateTimeStringFormats.add("yyyyMMdd-HHmmss");
            
            dateStringFormats.add("MM/dd/yyyy");
            dateStringFormats.add("yyyy-MM-dd");
            dateStringFormats.add("yyyyMMdd");

        } else if (locale == Locale.GERMAN) {
            dateTimeStringFormats.add("dd.MM.yyyy HH:mm:ss");
            dateTimeStringFormats.add("yyyy-MM-dd HH:mm:ss");
            dateTimeStringFormats.add("HH:mm:ss yyyy-MM-dd");
            dateTimeStringFormats.add("yyyyMMdd-HHmmss");
            
            dateStringFormats.add("dd.MM.yyyy");
            dateStringFormats.add("yyyy-MM-dd");
            dateStringFormats.add("yyyyMMdd");
        }
         */
        List<RegexDateConfig> configs = new ArrayList<>();
        configs.add(new RegexDateConfig("MM/dd/yyyy HH:mm:ss", true, "(\\d+)\\/(\\d+)\\/(\\d+) (\\d+):(\\d+):(\\d+)", m -> {
            return LocalDateTime.of(
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(4)),
                    Integer.parseInt(m.group(5)),
                    Integer.parseInt(m.group(6))
            );
        }));
        configs.add(new RegexDateConfig("dd.MM.yyyy HH:mm:ss", true, "(\\d+)\\.(\\d+)\\.(\\d+) (\\d+):(\\d+):(\\d+)", m -> {
            return LocalDateTime.of(
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(4)),
                    Integer.parseInt(m.group(5)),
                    Integer.parseInt(m.group(6))
            );
        }));
        configs.add(new RegexDateConfig("yyyy-MM-dd HH:mm:ss", true, "(\\d+)\\-(\\d+)\\-(\\d+) (\\d+):(\\d+):(\\d+)", m -> {
            return LocalDateTime.of(
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(4)),
                    Integer.parseInt(m.group(5)),
                    Integer.parseInt(m.group(6))
            );
        }));
        configs.add(new RegexDateConfig("HH:mm:ss yyyy-MM-dd", true, "(\\d+):(\\d+):(\\d+) (\\d+)\\-(\\d+)\\-(\\d+)", m -> {
            return LocalDateTime.of(
                    Integer.parseInt(m.group(4)),
                    Integer.parseInt(m.group(5)),
                    Integer.parseInt(m.group(6)),
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3))
            );
        }));
        configs.add(new RegexDateConfig("yyyyMMdd-HHmmss", true, "(\\d{4})(\\d{2})(\\d{2})\\-(\\d{2})(\\d{2})(\\d{2})", m -> {
            return LocalDateTime.of(
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(4)),
                    Integer.parseInt(m.group(5)),
                    Integer.parseInt(m.group(6))
            );
        }));
        configs.add(new RegexDateConfig("MM/dd/yyyy", false, "(\\d+)\\/(\\d+)\\/(\\d+)", m -> {
            return LocalDateTime.of(
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    0, 0, 0
            );
        }));
        configs.add(new RegexDateConfig("dd.MM.yyyy", false, "(\\d+)\\.(\\d+)\\.(\\d+)", m -> {
            return LocalDateTime.of(
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(1)),
                    0, 0, 0
            );
        }));
        /*
        configs.add(new RegexDateConfig("MM.dd.yyyy", false, "(\\d+)\\.(\\d+)\\.(\\d+)", m -> {
            return LocalDateTime.of(
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    0, 0, 0
            );
        }));
         */
        configs.add(new RegexDateConfig("yyyy-MM-dd", false, "(\\d+)\\-(\\d+)\\-(\\d+)", m -> {
            return LocalDateTime.of(
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    0, 0, 0
            );
        }));
        configs.add(new RegexDateConfig("yyyyMMdd", false, "(\\d{4})(\\d{2})(\\d{2})", m -> {
            return LocalDateTime.of(
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    0, 0, 0
            );
        }));

        Set<String> result = new HashSet<>();

        //StringBuilder sb = new StringBuilder(text);
        for (RegexDateConfig config : configs) {
            Matcher m = config.regex.matcher(text);
            while (m.find()) {
                LocalDateTime ldt;
                try {
                    ldt = config.f.apply(m);
                } catch (Exception e) {
                    //System.out.println("expection in parseDatesToXSD: " + text);
                    //throw e;
                    //we ignore invalid dates like "00.00.0000"
                    continue;
                }

                //year correction
                //if date is e.g. "1.7.18"
                if (ldt.getYear() >= 0 && ldt.getYear() < 30) {
                    ldt = ldt.plusYears(2000);
                } else if (ldt.getYear() >= 30 && ldt.getYear() <= 99) {
                    ldt = ldt.plusYears(1900);
                }

                //to xsd
                if (config.hasTime) {
                    result.add(ldt.format(xsdDateTimeFormatter));
                } else {
                    result.add(ldt.toLocalDate().format(xsdDateFormatter));
                }
            }

        }

        return new ArrayList<>(result);
    }

    private static class RegexDateConfig {

        private String format;
        private boolean hasTime;
        private Pattern regex;
        private Function<Matcher, LocalDateTime> f;

        public RegexDateConfig(String format, boolean hasTime, String regex, Function<Matcher, LocalDateTime> f) {
            this.format = format;
            this.hasTime = hasTime;
            this.regex = Pattern.compile(regex);
            this.f = f;
        }

    }
    
    private static double dupImplV2(List l) {

        if (l.size() <= 1) {
            return 0.0;
        }

        Set<String> set = new HashSet<>();

        Map<String, Integer> str2count = new HashMap<>();
        for (Object str : l) {
            if (str instanceof String) {
                str2count.put((String) str, 2);
                set.add((String) str);
            }
        }

        int extraUnique = 0;
        int extraParts = 0;

        for (Object obj : l) {
            if (obj instanceof String) {
                String str = (String) obj;
                Integer count = str2count.get((String) str);
                if (count != null) {
                    if (count == 1) {
                        str2count.remove(str);
                    } else {
                        str2count.put(str, count - 1);
                    }
                }
            } else {
                extraUnique++;
                extraParts++;
            }
        }

        int unique = str2count.keySet().size() + extraUnique;
        int parts = set.size() + extraParts;
        int n = l.size();
        
        double score = ((2 * n) - unique - parts + 1) / (double) (2 * n);
        
        //System.out.println("$\\mathit{dup}(\\{"+ l.toString().substring(1, l.toString().length()-1)  +"\\})$ &  $= "+ String.format(Locale.US, "%.2f", score) +"$ \\\\");
        
        
        
        //System.out.println(l + " " + score + ": unique=" + unique + " vs parts=" + parts);
        return score;
    }

}
