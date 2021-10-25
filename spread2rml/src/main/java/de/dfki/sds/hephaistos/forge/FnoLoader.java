
package de.dfki.sds.hephaistos.forge;

import de.dfki.sds.hephaistos.forge.vocab.FNML;
import de.dfki.sds.hephaistos.forge.vocab.FNO;
import de.dfki.sds.hephaistos.forge.vocab.FNOI;
import de.dfki.sds.hephaistos.forge.vocab.FNOM;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.DOAP;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

/**
 * Loads from JAR and class name the methods as FnO functions.
 */
public class FnoLoader {

    
    public Model load(File jarFile, String className, String downloadPage) throws Exception {
        Model m = ModelFactory.createDefaultModel();
        
        m.setNsPrefixes(PrefixMapping.Standard);
        m.setNsPrefix("fno", FNO.NS);
        m.setNsPrefix("fnom", FNOM.NS);
        m.setNsPrefix("fnoi", FNOI.NS);
        m.setNsPrefix("fnml", FNML.NS);
        
        /*
        cf:custom-rml-functions
            a                  fnoi:JavaClass ;
            doap:download-page "... .jar" ;
            fnoi:class-name    "... .CustomFunctions" .
        */
        Resource implInst = m.createResource("java:" + className);
        m.add(implInst, RDF.type, FNOI.JavaClass);
        if(downloadPage == null) {
            m.add(implInst, DOAP.download_page, jarFile.getAbsolutePath()); //RMLMapper does not use basePath so it has to be absolute
        } else {
            m.add(implInst, DOAP.download_page, downloadPage);
        }
        m.add(implInst, FNOI.class_name, className);
        
        URL[] urls = { new URL("jar:file:" + jarFile.getAbsolutePath()+"!/") };
        URLClassLoader urlClassLoader = URLClassLoader.newInstance(urls);
        
        Class<?> cls = urlClassLoader.loadClass(className);
        
        /*
        cf:parseDate a fno:Function ;
            fno:expects ( grel:valueParam ) ;
            fno:returns ( grel:stringOut ) .

        cf:parseDateMapping
            a                    fnoi:Mapping ;
            fno:function         cf:parseDate ; # src
            fno:implementation   cf:custom-rml-functions ; # trg
            fno:methodMapping    [ a                fnom:StringMethodMapping ; # method
                                   fnom:method-name "parseDate" ] .
        */
        
        int maxParameterCount = 0;
        
        for(Method method : cls.getDeclaredMethods()) {
            
            //method needs to be "public static"
            if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            
            Resource functionInst = m.createResource("java:" + method.getDeclaringClass().getName() + "." + method.getName());
            m.add(functionInst, RDF.type, FNO.Function);
            m.add(functionInst, RDFS.label, method.getName());
            RDFList expectsList = m.createList();
            Map<Class<?>, Integer> typeName2count = new HashMap<>();
            maxParameterCount = Math.max(maxParameterCount, method.getParameterCount());
            //expects
            for(int i = 0; i < method.getParameterCount(); i++) {
                
                Parameter parameter = method.getParameters()[i];
                
                int c = typeName2count.computeIfAbsent(parameter.getType(), tn -> 0);
                typeName2count.put(parameter.getType(), c+1);
                
                Resource paramInst = typeToResource(parameter.getType(), "parameter", c, m);
                
                expectsList = expectsList.with(paramInst);
            }
            m.add(functionInst, FNO.expects, expectsList);
            
            //returns
            Class<?> returnType = method.getReturnType();
            RDFList returnsList = m.createList(new RDFNode[] { typeToResource(returnType, "return", -1, m) });
            m.add(functionInst, FNO.returns, returnsList);
            
            Resource mappingInst = m.createResource();
            m.add(mappingInst, RDF.type, FNO.Mapping);
            m.add(mappingInst, FNO.function, functionInst);
            m.add(mappingInst, FNO.implementation, implInst);
            
            Resource stringMethodMapping = m.createResource();
            m.add(mappingInst, FNO.methodMapping, stringMethodMapping);
            m.add(stringMethodMapping, RDF.type, FNOM.StringMethodMapping);
            m.add(stringMethodMapping, FNOM.method_name, method.getName());
        }
        
        
        
        return m;
    }
    
    public Model tbox(int maxParameterCount) {
        
        Model m = ModelFactory.createDefaultModel();
        
        m.setNsPrefixes(PrefixMapping.Standard);
        m.setNsPrefix("fno", FNO.NS);
        m.setNsPrefix("fnom", FNOM.NS);
        m.setNsPrefix("fnoi", FNOI.NS);
        m.setNsPrefix("fnml", FNML.NS);
        
        //default vocabulary: parameters and outputs
        
        /*
        grel:stringOut
            a             fno:Output ;
            fno:name      "output string" ;
            rdfs:label    "output string" ;
            fno:predicate grel:stringOutput ;
            fno:type      xsd:string .
        
        grel:valueParam3
            a             fno:Parameter ;
            fno:name      "input value 3" ;
            rdfs:label    "input value 3" ;
            fno:predicate grel:valueParameter3 ;
            fno:type      xsd:string ;
            fno:required  "true"^^xsd:boolean .
        */
        Map<Class<?>, Resource> javaType2rdfType = new HashMap<>();
        javaType2rdfType.put(String.class, XSD.xstring);
        javaType2rdfType.put(Boolean.class, XSD.xboolean);
        javaType2rdfType.put(Integer.class, XSD.xint);
        javaType2rdfType.put(Float.class, XSD.decimal);
        javaType2rdfType.put(Double.class, XSD.decimal);
        javaType2rdfType.put(Number.class, XSD.decimal);
        javaType2rdfType.put(String[].class, RDF.List);
        javaType2rdfType.put(Boolean[].class, RDF.List);
        javaType2rdfType.put(Integer[].class, RDF.List);
        javaType2rdfType.put(Double[].class, RDF.List);
        javaType2rdfType.put(Float[].class, RDF.List);
        javaType2rdfType.put(Number[].class, RDF.List);
        javaType2rdfType.put(List.class, RDF.List);

        for(Map.Entry<Class<?>, Resource> e : javaType2rdfType.entrySet()) {
            
            for(int i = 0; i < maxParameterCount; i++) {
                Resource param = typeToResource(e.getKey(), "parameter", i, m);
                
                m.add(param, RDF.type, FNO.Parameter);
                m.add(param, RDFS.label, "Input " + e.getKey().getSimpleName() + " " + i);
                m.add(param, FNO.type, e.getValue());
                m.add(param, FNO.required, m.createTypedLiteral(true));
                
                m.add(param, FNO.predicate, typeToResource(e.getKey(), "parameter.predicate", i, m));
            }
            
            Resource returnRes = typeToResource(e.getKey(), "return", -1, m);
            m.add(returnRes, RDF.type, FNO.Output);
            m.add(returnRes, RDFS.label, "Output " + e.getKey().getSimpleName());
            m.add(returnRes, FNO.type, e.getValue());
            
            m.add(returnRes, FNO.predicate, typeToResource(e.getKey(), "return.predicate", -1, m));
        }
        
        return m;
    }
    
    private Resource typeToResource(Class<?> type, String prefix, int i, Model m) {
        String typeName = type.getSimpleName().toLowerCase();
        
        //array
        if(typeName.endsWith("[]")) {
            typeName = typeName.replace("[]", ".array");
        }
        
        Resource paramInst = m.createResource("java:" + prefix + "." + typeName + (i >= 0 ? ("." + i) : ""));
        
        return paramInst;
    }
    
}
