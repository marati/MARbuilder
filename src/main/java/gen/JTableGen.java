package gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;

import com.marati.marbuilder.MarForm;

public class JTableGen {
    final MarForm marForm;
    
    JTableGen(MarForm mainMarForm) {
        marForm = mainMarForm;
    }
    
    private void parseXsd(File xsdFile) {
        
        
    }
    
    public void addTablesInPane(HashMap<String, List<String>> extDirsWithFiles) {
        //extDirsWithFiles.values().iterator()
        for (Map.Entry<String, List<String>> entryExtDirs: extDirsWithFiles.entrySet()) {
            
            //get Dir
            String curentDir = entryExtDirs.getKey();
            
            //get Files
            List<String> filesNameWithoutExt = new ArrayList<String>();
            for (String fileName : filesNameWithoutExt) {
                //вытащить файл и отпарсить
            }
        }
    }
}
