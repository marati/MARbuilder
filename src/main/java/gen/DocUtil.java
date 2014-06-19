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
import javax.xml.xpath.*;
import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;

import com.marati.marbuilder.MarForm;
import java.util.logging.Level;

public class DocUtil {
    private final MarForm marForm;
    
    HashMap<String, ArrayList<String>> tables = new HashMap<String, ArrayList<String>>();
    String tableName;
    private String projectPath;
    ArrayList<String> columns;
    private Document doc = null;
    private static Logger logger = Logger.getLogger(DocUtil.class);
    
    public DocUtil(MarForm mainMarForm) {
        marForm = mainMarForm;
    }
    
    public void setProjectPath(String path) {
        projectPath = path;
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
    
    public void createSerializableXmlData(String fileName, String[] columns) {
        try {
            int dotPos = fileName.lastIndexOf(".");
            String fileNameWithoutExt = fileName.substring(0, dotPos);
            String xmlFileName = fileNameWithoutExt + ".xml";
            
            File xmlFile = new File(projectPath + File.separator + 
                    "xml" + File.separator + xmlFileName);
            
            Builder parser = new Builder();
            Document doc = parser.build(xmlFile);
            
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//title/price/text()");
            
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                logger.info("node in xml req file: " + nodes.item(i).getNodeValue()); 
            }
            
        } catch (ParsingException ex) {
            logger.error(ex);
        } catch (IOException ex) {
            logger.error(ex);
        } catch (XPathExpressionException ex) {
            logger.error(ex);
        }
    }
}
