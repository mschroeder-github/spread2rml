package com.github.mschroedergithub.fno;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ahocorasick.trie.PayloadEmit;
import org.ahocorasick.trie.PayloadTrie;
import org.ahocorasick.trie.PayloadTrie.PayloadTrieBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.XSD;
import org.apache.poi.ss.usermodel.DateUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

/**
 * Custom Functions for the use cases.
 */
public class CustomFunctions {

    //data types
    private static SimpleDateFormat xsdDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static DateTimeFormatter xsdDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static SimpleDateFormat xsdDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static DateTimeFormatter xsdDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static Boolean parseBoolean(String jsonStr, List<String> booleanTrueSymbols, List<String> booleanFalseSymbols) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        JSONObject json = new JSONObject(jsonStr);

        if (json.getString("cellType").equals("string")) {

            int trueCount = 0;
            int falseCount = 0;
            String text = json.getString("valueString");

            for (String symb : booleanTrueSymbols) {
                if (text.equals(symb)) {
                    return true;
                }

                if (text.contains(symb)) {
                    trueCount++;
                }
            }
            for (String symb : booleanFalseSymbols) {
                if (text.equals(symb)) {
                    return false;
                }

                if (text.contains(symb)) {
                    falseCount++;
                }
            }

            //fuzzy
            if (trueCount == falseCount) {
                return null;
            } else if (trueCount > falseCount) {
                return true;
            } else {
                return false;
            }

        } else if (json.getString("cellType").equals("boolean")) {

            return json.getBoolean("valueBoolean");
        } else if (json.getString("cellType").equals("numeric")) {

            double num = json.getDouble("valueNumeric");

            if (num == 1.0) {
                return true;
            } else if (num == 0.0) {
                return false;
            }
        }

        return null;
    }

    //maybe pass datatype so that it is clear because of the comma position
    /*
    public static Number parseNumber(String jsonStr, String locale, Boolean onlyInts) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        JSONObject json = new JSONObject(jsonStr);

        if (json.getString("cellType").equals("string")) {

            NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag(locale));

            Number number;
            try {
                number = fmt.parse(json.getString("valueString"));

                if (onlyInts) {
                    return number.intValue();
                }

                return number;
                //return number.doubleValue();
            } catch (ParseException ex) {
                return null;
            }

        } else if (json.getString("cellType").equals("numeric")) {

            double value = json.getDouble("valueNumeric");

            if (onlyInts) {
                return (int) value;
            }

            return value;
        }

        return null;
    }
     */
    public static List<Number> parseNumber(String jsonStr, String locale, Boolean onlyInts) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        JSONObject json = new JSONObject(jsonStr);

        if (json.getString("cellType").equals("string")) {

            String pStr = "(\\+|\\-)?\\d+";
            if (!onlyInts) {
                if (locale.equals("de")) {
                    pStr += ",";
                } else if (locale.equals("en")) {
                    pStr += ".";
                }
                pStr += "\\d+";
            }

            Pattern p = Pattern.compile(pStr);

            String text = json.getString("valueString");

            Matcher m = p.matcher(text);

            List<Number> numbers = new ArrayList<>();
            while (m.find()) {

                if (onlyInts) {
                    int intVal = Integer.parseInt(m.group());

                    //fix for years "1999-2003" because of the "-2003" problem
                    if (intVal < 0 && Math.abs(intVal) >= 1960 && Math.abs(intVal) <= 2020) {
                        intVal = Math.abs(intVal);
                    }

                    numbers.add(intVal);
                } else {
                    String found = m.group();
                    if(locale.equals("de")) {
                        found = found.replace(",", ".");
                    }
                    numbers.add(Double.parseDouble(found));
                }
            }

            return numbers;
            
        } else if (json.getString("cellType").equals("numeric")) {

            double value = json.getDouble("valueNumeric");

            if (onlyInts) {
                return Arrays.asList((int) value);
            }

            return Arrays.asList(value);
        }

        return new ArrayList<>();
    }

    public static List<Number> parseNumberWithNumberFormat(String jsonStr, String locale, Boolean onlyInts) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        JSONObject json = new JSONObject(jsonStr);

        if (json.getString("cellType").equals("string")) {

            NumberFormat fmt = NumberFormat.getInstance(Locale.forLanguageTag(locale));

            //try {
            ParsePosition pp = new ParsePosition(0);

            List<Number> numbers = new ArrayList<>();

            String text = json.getString("valueString");

            Number number;
            do {
                number = fmt.parse(text, pp);

                if (number != null) {

                    if (onlyInts) {
                        int intVal = number.intValue();

                        //fix for years "1999-2003" because of the "-2003" problem
                        if (intVal < 0 && Math.abs(intVal) >= 1960 && Math.abs(intVal) <= 2020) {
                            intVal = Math.abs(intVal);
                        }

                        numbers.add(intVal);
                    } else {
                        numbers.add(number);
                    }

                } else {
                    pp.setIndex(pp.getIndex() + 1);
                }

            } while (pp.getIndex() < text.length());

            return numbers;
            //return number.doubleValue();
            //} catch (ParseException ex) {
            //    return new ArrayList<>();
            //}

        } else if (json.getString("cellType").equals("numeric")) {

            double value = json.getDouble("valueNumeric");

            if (onlyInts) {
                return Arrays.asList((int) value);
            }

            return Arrays.asList(value);
        }

        return new ArrayList<>();
    }

    public static List<String> parseDate(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return Arrays.asList();
        }
        JSONObject json = new JSONObject(jsonStr);

        List<String> result = new ArrayList<>();

        if (json.getString("cellType").equals("string")) {
            result.addAll(parseDatesToXSD(json.getString("valueString")));

        } else if (json.getString("cellType").equals("numeric")) {
            Date date = DateUtil.getJavaDate(json.getDouble("valueNumeric"));
            result.add(xsdDateFormat.format(date));
        }

        result.removeIf(r -> r.contains(":"));

        return result;
    }

    public static List<String> parseDateTime(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return Arrays.asList();
        }
        JSONObject json = new JSONObject(jsonStr);

        List<String> result = new ArrayList<>();

        if (json.getString("cellType").equals("string")) {
            result.addAll(parseDatesToXSD(json.getString("valueString")));

        } else if (json.getString("cellType").equals("numeric")) {
            Date date = DateUtil.getJavaDate(json.getDouble("valueNumeric"));
            result.add(xsdDateTimeFormat.format(date));
        }

        result.removeIf(r -> !r.contains(":"));

        return result;
    }

    /*package*/ static List<String> parseDatesToXSD(String text) {
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

    //entities
    //will always return IRI
    public static List<String> entityLinking(String text) {
        List<String> entities = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return entities;
        }

        if (trie != null) {
            Map<Resource, Integer> res2count = new HashMap<>();

            for (PayloadEmit<Resource> emit : trie.parseText(text)) {
                int count = res2count.computeIfAbsent(emit.getPayload(), pl -> 0);
                res2count.put(emit.getPayload(), count + 1);
            }

            List<Entry<Resource, Integer>> l = new ArrayList<>(res2count.entrySet());
            l.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            for (Entry<Resource, Integer> e : l) {
                entities.add(e.getKey().getURI());
            }
        }

        //System.out.println("entityLinking: " + text + " => " + entities);
        return entities;
    }

    public static List<String> entityLinkingWithDelimiters(String text, List<String> delimiters) {
        return entityLinking(text);
    }

    //may return IRI or Literal of certain type
    public static String getEntityBySplitIndex(String text, List<String> delimiters, Integer index, String termType, String datatype) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        StringJoiner sj = new StringJoiner("|", "(", ")");
        delimiters.forEach(str -> sj.add(Pattern.quote(str)));

        List<String> segments = new ArrayList<>(Arrays.asList(text.split(sj.toString())));

        //there could be segments which are empty be design to match the correct index
        //e.g. "\nA\nB" => 0 is empty, 1 is A and 2 is B
        //segments.removeIf(str -> str.trim().isEmpty());
        if (index < 0 || index >= segments.size()) {
            return null;
        }

        List<String> entities = checkForEntity(Arrays.asList(segments.get(index)), termType, datatype);

        if (entities.isEmpty()) {
            return null;
        }

        String entity = entities.get(0);

        //[line: 16, col: 5 ] Lexical form '' not valid for datatype XSD decimal
        //no empty string, e.g. "" or "    "
        if (R2RML.Literal.getURI().equals(termType) && entity.trim().isEmpty()) {
            if (datatype.equals(XSD.decimal.getURI())
                    || datatype.equals(XSD.integer.getURI())
                    || datatype.equals(XSD.xboolean.getURI())
                    || datatype.equals(XSD.xstring.getURI())) {
                return null;
            }
        }

        //System.out.println(text + "\n====== split ====\n" + segments + "\nindex: " + index + "\nresult: " + entity);
        return entity;
    }

    public static List<String> getEntitiesByTag(String richText, String tag, String termType, String datatype) {
        List<String> texts = new ArrayList<>();

        //try to parse
        Document doc;
        try {
            doc = Jsoup.parse(richText, "", Parser.xmlParser());
        } catch (Exception e) {
            //empty
            return texts;
        }

        for (Element elem : doc.children()) {
            if (elem.tagName().equals(tag)) {
                String txt = elem.text();
                txt = txt.trim();

                if (!txt.isEmpty()) {
                    texts.add(txt);
                }
            }
        }
        
        //System.out.println("getEntitiesByTag " + richText + " = " + texts);

        return checkForEntity(texts, termType, datatype);
    }

    public static List<String> getEntitiesByColor(String richText, String color, String termType, String datatype) {
        List<String> texts = new ArrayList<>();
        
        //try to parse
        Document doc;
        try {
            doc = Jsoup.parse(richText, "", Parser.xmlParser());
        } catch (Exception e) {
            //empty
            return texts;
        }

        //no color attribute means automatically black
        doc.getElementsByTag("font").forEach(elem -> {
            if (!elem.hasAttr("color")) {
                elem.attr("color", "#000000");
            }
        });

        //System.out.println(doc);
        //if(!richText.trim().isEmpty()) System.out.println(richText);
        if (color.isEmpty()) {

            for (Element elem : doc.getElementsByTag("font")) {
                String txt = elem.text();
                txt = txt.trim();

                if (!elem.hasAttr("color") || elem.attr("color").equals("#000000")) {
                    if (!txt.isEmpty()) {
                        texts.add(txt);
                    }
                }
            }

        } else {

            for (Element elem : doc.getElementsByAttributeValue("color", color)) {
                String txt = elem.text();
                txt = txt.trim();

                if (!txt.isEmpty()) {
                    texts.add(txt);
                }
            }
        }

        //System.out.println("getEntitiesByColor " + richText + " = " + texts);
        
        return checkForEntity(texts, termType, datatype);
    }

    public static List<String> getEntitiesByUnformatted(String richText, String termType, String datatype) {
        List<String> texts = new ArrayList<>();

        //try to parse
        Document doc;
        try {
            doc = Jsoup.parse(richText, "", Parser.xmlParser());
        } catch (Exception e) {
            //empty
            return texts;
        }

        StringJoiner sj = new StringJoiner(" ");
        sj.add(doc.ownText());

        //when we load Excel file they put <font face='Calibri' color='#000000' >...</font>
        //in a rich text where we did not mention any formatting.
        //thus, we also see font color black as unformatted for now
        for (Element elem : doc.getElementsByAttributeValue("color", "#000000")) {
            sj.add(elem.text().trim());
        }

        //we just want the font children
        //<font face='bla'>text</font><b><font>...</font></b>
        /*
        for (Element element : doc.children()) {
            if (element.tagName().equals("font") && !element.hasAttr("color")) {
                sj.add(element.text());
            }
        }
        */

        String text = sj.toString().trim();
        
        //System.out.println("getEntitiesByUnformatted " + richText + " = " + text);

        //texts.addAll(getEntitiesByColor(richText, "#000000", termType, datatype));
        //because the delimiter are also unformatted we also get them
        //I guess we have two possiblities: 
        //(a) it is delimited and position based or 
        //(b) it is partial formatted and position is irrelevant (thus no delimiter is used)
        //<http://www.dfki.uni-kl.de/~mschroeder/ld/gl#hasNote> ",", ", Ptlr Bwc Mkpcx Yteuk Hwitv";
        List<String> strs = checkForEntity(Arrays.asList(text), termType, datatype);

        //avoid hasNote ""
        strs.removeIf(str -> str.isEmpty());

        return strs;
    }

    /*package*/ static List<String> checkForEntity(List<String> list, String termType, String datatype) {
        List<String> result = new ArrayList<>();
        for (String text : list) {

            if (isNullOrEmpty(termType)) {
                autodetect(text, result);

            } else if (termType.equals(R2RML.IRI.getURI())) {
                result.addAll(entityLinking(text));

            } else if (termType.equals(R2RML.Literal.getURI())) {
                if (isNullOrEmpty(datatype) || datatype.equals(XSD.xstring.getURI())) {
                    result.add(text);

                } else if (datatype.equals(XSD.date.getURI())) {
                    result.addAll(parseDate("{ \"cellType\": \"string\", \"valueString\": \"" + text + "\" }"));

                } else if (datatype.equals(XSD.dateTime.getURI())) {
                    result.addAll(parseDateTime("{ \"cellType\": \"string\", \"valueString\": \"" + text + "\" }"));

                } else if (datatype.equals(XSD.integer.getURI())) {
                    parseNumber("{ \"cellType\": \"string\", \"valueString\": \"" + text + "\" }", "en", true).forEach(num -> result.add("" + num));

                } else if (datatype.equals(XSD.decimal.getURI())) {
                    parseNumber("{ \"cellType\": \"string\", \"valueString\": \"" + text + "\" }", "en", false).forEach(num -> result.add("" + num));

                } else {
                    result.add(text);
                }
            }
        }

        return result;
    }

    private static void autodetect(String text, List<String> result) {
        String json = "{ \"cellType\": \"string\", \"valueString\": \"" + text + "\" }";
        List<String> dateTimes = parseDateTime(json);

        if (!dateTimes.isEmpty()) {
            result.add(dateTimes.get(0));

        } else {
            List<String> dates = parseDate(json);

            if (!dates.isEmpty()) {
                result.add(dates.get(0));

            } else {
                List<String> entities = entityLinking(text);

                if (!entities.isEmpty()) {
                    result.add(entities.get(0));

                } else {
                    result.add(text);
                }
            }
        }
    }

    private static boolean isNullOrEmpty(String uri) {
        return uri == null || uri.isEmpty();
    }

    private static PayloadTrie<Resource> trie;

    static {
        //load data for entity linking
        File fnoJsonFile = new File("fno.json");

        if (fnoJsonFile.exists()) {
            System.out.println(fnoJsonFile.getAbsolutePath() + " will be used as config");

            JSONObject conf;
            try {
                conf = new JSONObject(FileUtils.readFileToString(fnoJsonFile, StandardCharsets.UTF_8));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            if (conf.has("entityLinking")) {
                JSONObject entityLinking = conf.getJSONObject("entityLinking");
                String dataset = entityLinking.getString("dataset");
                JSONArray excludeType = entityLinking.getJSONArray("excludeType");

                System.out.println("for entity linking we load: " + dataset);

                Model model;
                try {
                    model = ModelFactory.createDefaultModel().read(new FileReader(new File(dataset)), null, "TTL");
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }

                PayloadTrieBuilder<Resource> builder = PayloadTrie.<Resource>builder();
                builder.ignoreOverlaps();

                for (Statement stmt : model.listStatements().toList()) {
                    if (stmt.getObject().isLiteral()) {
                        Literal l = stmt.getLiteral();
                        if (l.getValue() instanceof String) {
                            builder.addKeyword(l.getString(), stmt.getSubject());
                        }
                    }
                }

                trie = builder.build();
                System.out.println("trie built for entity linking");
            }

        } else {
            System.out.println(fnoJsonFile.getAbsolutePath() + " not found");
        }

    }

}
