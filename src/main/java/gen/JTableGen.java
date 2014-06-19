package gen;

import java.util.*;
import java.io.File;
import java.io.IOException;
import nu.xom.Attribute;
import nu.xom.ParsingException;
import nu.xom.Document;
import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Elements;
import org.apache.log4j.Logger;

import com.marati.marbuilder.MarForm;

public class JTableGen {
    private final MarForm marForm;
    
    HashMap<String, ArrayList<String>> tables = new HashMap<String, ArrayList<String>>();
    String tableName;
    ArrayList<String> columns;
    private Document doc = null;
    private static Logger logger = Logger.getLogger(JTableGen.class);
    
    public JTableGen(MarForm mainMarForm) {
        marForm = mainMarForm;
    }
    
    private void recurseXsd(Element parent) {
        Elements childs = parent.getChildElements();
        
        if (childs.size() > 0) {
            for (int e=0; e<childs.size(); e++) {
                Element child = childs.get(e);
                
                if (parent.getLocalName() == "schema" && e == 0) {
                    tableName = child.getAttributeValue("name");
                }
                
                if (child.getLocalName() == "element") {
                    for (int a=0; a<child.getAttributeCount(); ++a) {
                        Attribute attr = child.getAttribute(a);
                        //System.out.println("child" + attr.getLocalName());
                        if (attr.getLocalName().equals("maxOccurs")) {
                            //if (!attr.getValue().equals("unbounded")) {
                                //System.out.println("attr name" + attr.getValue());
                                columns.add(child.getAttributeValue("name"));
                            //}
                        }
                    }
                }
                
                if (child.getChildCount() > 0)
                    recurseXsd(child);
            }
        } 
    }
    
    private void parseXsd(File xsdFile) throws ParsingException, IOException {
        columns = new ArrayList<String>();
        tableName = new String();
        
        Builder parser = new Builder();
        Document doc = parser.build(xsdFile);
        final Element rootElement = doc.getRootElement();
        recurseXsd(rootElement);
        
        tables.put(tableName, columns);
    }
    
    public void createTablesFromXsd(HashMap<String, ArrayList<String>> extDirsWithFiles) throws ParsingException, IOException {
        tables.clear();
        
        for (Map.Entry<String, ArrayList<String>> entryExtDirs: extDirsWithFiles.entrySet()) {
            
            //get Dir
            String curentDir = entryExtDirs.getKey();
            
            //get Files
            ArrayList<String> filesNameWithoutExt = entryExtDirs.getValue();
            for (String fileName : filesNameWithoutExt) {
                File xsdFile = new File(curentDir + File.separator + fileName + ".xsd");
                parseXsd(xsdFile);
                
                String logMessage = new String("Convert: " + fileName + ".xml to " + xsdFile.getName());
                logger.info(logMessage);
            }
        }
        
        marForm.addTabsInPane(tables);
    }
}
