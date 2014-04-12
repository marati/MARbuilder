package gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import nu.xom.Attribute;
import nu.xom.ParsingException;
import nu.xom.Document;
import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Elements;

import com.marati.marbuilder.MarForm;

public class JTableGen {
    private final MarForm marForm;
    
    HashMap<String, ArrayList<String>> tables;
    String tableName;
    ArrayList<String> columns;
    private Document doc = null;
    
    public JTableGen(MarForm mainMarForm) {
        marForm = mainMarForm;
        tables = new HashMap<String, ArrayList<String>>();
    }
    
    private void recurseXsd(Element parent) {
        Elements childs = parent.getChildElements();
        System.out.println("childs: " + childs.size());
        
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
                            if (!attr.getValue().equals("unbounded")) {
                                System.out.println("attr name" + attr.getValue());
                                columns.add(child.getAttributeValue("name"));
                            }
                        }
                    }
                }
                
                if (child.getChildCount() > 0)
                    recurseXsd(child);
            }
        }
        System.out.println("exit in func");
        
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
    
    public void addTablesInPane(HashMap<String, ArrayList<String>> extDirsWithFiles) throws ParsingException, IOException {
        for (Map.Entry<String, ArrayList<String>> entryExtDirs: extDirsWithFiles.entrySet()) {
            
            //get Dir
            String curentDir = entryExtDirs.getKey();
            
            //get Files
            ArrayList<String> filesNameWithoutExt = entryExtDirs.getValue();
            for (String fileName : filesNameWithoutExt) {
                File xsdFile = new File(curentDir + File.separator + fileName + ".xsd");
                System.out.println("parse " + xsdFile.getName());
                parseXsd(xsdFile);
                //вытащить файл и отпарсить
                //после этого вызвать метод, в который передать имя вкладки и список столбцов String[]
            }
        }
        
        marForm.addTabsInPane(tables);
    }
}
