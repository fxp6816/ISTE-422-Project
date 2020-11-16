import java.io.*;
import java.util.*;
import javax.swing.*;
//import org.apache.logging.log4j.*;

public class ParseEdge implements Parser{
    
   private File parseFile;
   private BufferedReader br;
   
   private ArrayList alTables, alFields, alConnectors;
   private EdgeField tempField;
   private String style;
   private String text;
   
   
   private int numFigure, numConnector;
   private int endPoint1, endPoint2;
   

    //static variables
   public static final String EDGE_ID = "EDGE Diagram File"; //first line of .edg files should be this
   public static final String SAVE_ID = "EdgeConvert Save File"; //first line of save files should be this
   public static final String DELIM = "|";


    public ParseEdge(File parseFile, BufferedReader br){
        this.parseFile = parseFile;
        this.br = br;
        this.alTables = new ArrayList<EdgeTable>();
        this.alFields = new ArrayList<EdgeField>();
        this.alConnectors = new ArrayList<EdgeConnector>();
    }

  

    public void parse(){
        String tableName;
        String fieldName;
        String currentLine;


        int numLine;
        String endStyle1, endStyle2;

        boolean isEntity, isAttribute; 
        boolean isUnderlined = false;

        //logger.debug("Attempting to parse Edge File...");
  
        try{
        while ((currentLine = this.br.readLine()) != null) {
           currentLine = currentLine.trim();
           if (currentLine.startsWith("Figure ")) { //this is the start of a Figure entry
              numFigure = Integer.parseInt(currentLine.substring(currentLine.indexOf(" ") + 1)); //get the Figure number
              currentLine = this.br.readLine().trim(); // this should be "{"
              currentLine = this.br.readLine().trim();
              if (!currentLine.startsWith("Style")) { // this is to weed out other Figures, like Labels
                 continue;
              } else {
                 style = currentLine.substring(currentLine.indexOf("\"") + 1, currentLine.lastIndexOf("\"")); //get the Style parameter
                 
                 //logger.debug("Looking for Relations...");
  
                 if (style.startsWith("Relation")) { //presence of Relations implies lack of normalization
                    
                    //logger.debug("Relations Found...");
  
                    JOptionPane.showMessageDialog(null, "The Edge Diagrammer file\n" + parseFile + "\ncontains relations.  Please resolve them and try again.");
                    EdgeConvertGUI.setReadSuccess(false);
                    break;
                 } 
                 if (style.startsWith("Entity")) {
                    isEntity = true;
                 }else{
                  isEntity = false;
                 }
                 if (style.startsWith("Attribute")) {
                    isAttribute = true;
                 }else{
                  isAttribute = false;
                 }
                 if (!(isEntity || isAttribute)) { //these are the only Figures we're interested in
                    continue;
                 }
                 currentLine = this.br.readLine().trim(); //this should be Text
                 text = currentLine.substring(currentLine.indexOf("\"") + 1, currentLine.lastIndexOf("\"")).replaceAll(" ", ""); //get the Text parameter
                 
                 //logger.debug("Checking if any entities have blank names...");
  
                 if (text.equals("")) {
  
                    //logger.debug("Entities or attributes found with blank names...");
  
                    JOptionPane.showMessageDialog(null, "There are entities or attributes with blank names in this diagram.\nPlease provide names for them and try again.");
                    EdgeConvertGUI.setReadSuccess(false);
                    break;
                 }
                 int escape = text.indexOf("\\");
                 if (escape > 0) { //Edge denotes a line break as "\line", disregard anything after a backslash
                    text = text.substring(0, escape);
                 }
  
                 do { //advance to end of record, look for whether the text is underlined
                    currentLine = this.br.readLine().trim();
                    if (currentLine.startsWith("TypeUnderl")) {
                       isUnderlined = true;
                    }
                 } while (!currentLine.equals("}")); // this is the end of a Figure entry
                 
                 if (isEntity) { //create a new EdgeTable object and add it to the alTables ArrayList
  
                    //logger.debug("Looking for duplicates...");
  
                    if (isTableDup(text)) {
  
                       //logger.debug("Duplicates Found...");
  
                       JOptionPane.showMessageDialog(null, "There are multiple tables called " + text + " in this diagram.\nPlease rename all but one of them and try again.");
                       EdgeConvertGUI.setReadSuccess(false);
                       break;
                    }
                    alTables.add(new EdgeTable(numFigure + DELIM + text));
                 }
                 if (isAttribute) { //create a new EdgeField object and add it to the alFields ArrayList
                    tempField = new EdgeField(numFigure + DELIM + text);
                    tempField.setIsPrimaryKey(isUnderlined);
                    alFields.add(tempField);
                 }
                 //reset flags
                 isEntity = false;
                 isAttribute = false;
                 isUnderlined = false;
              }
           } // if("Figure")
           if (currentLine.startsWith("Connector ")) { //this is the start of a Connector entry
              numConnector = Integer.parseInt(currentLine.substring(currentLine.indexOf(" ") + 1)); //get the Connector number
              currentLine = this.br.readLine().trim(); // this should be "{"
              currentLine = this.br.readLine().trim(); // not interested in Style
              currentLine = this.br.readLine().trim(); // Figure1
              endPoint1 = Integer.parseInt(currentLine.substring(currentLine.indexOf(" ") + 1));
              currentLine = this.br.readLine().trim(); // Figure2
              endPoint2 = Integer.parseInt(currentLine.substring(currentLine.indexOf(" ") + 1));
              currentLine = this.br.readLine().trim(); // not interested in EndPoint1
              currentLine = this.br.readLine().trim(); // not interested in EndPoint2
              currentLine = this.br.readLine().trim(); // not interested in SuppressEnd1
              currentLine = this.br.readLine().trim(); // not interested in SuppressEnd2
              currentLine = this.br.readLine().trim(); // End1
              endStyle1 = currentLine.substring(currentLine.indexOf("\"") + 1, currentLine.lastIndexOf("\"")); //get the End1 parameter
              currentLine = this.br.readLine().trim(); // End2
              endStyle2 = currentLine.substring(currentLine.indexOf("\"") + 1, currentLine.lastIndexOf("\"")); //get the End2 parameter
  
              do { //advance to end of record
                 currentLine = this.br.readLine().trim();
              } while (!currentLine.equals("}")); // this is the end of a Connector entry
              
              alConnectors.add(new EdgeConnector(numConnector + DELIM + endPoint1 + DELIM + endPoint2 + DELIM + endStyle1 + DELIM + endStyle2));
           } // if("Connector")
        } // while()
        
        
        }catch(IOException ioe){
         ioe.printStackTrace();
        }
     } // parseEdgeFile()

    

    public ArrayList<EdgeTable> getAlTables(){return this.alTables;}
    public ArrayList<EdgeField> getAlFields(){return this.alFields;}
    public ArrayList<EdgeConnector> getAlConnectors(){return this.alConnectors;}
    
    
    public boolean isTableDup(String text){
      
      for(EdgeTable table : this.getAlTables()){
         if(text.equals(table.getName())){
            return true;
         }
      }
      
      return false;
    }
}
