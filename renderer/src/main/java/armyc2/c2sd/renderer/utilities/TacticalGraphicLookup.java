/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package armyc2.c2sd.renderer.utilities;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author michael.spinelli
 */
public class TacticalGraphicLookup {
    
    private Map<String,Integer> symbolMap = new HashMap<String, Integer>();

    private static TacticalGraphicLookup _instance = null;
    
    private TacticalGraphicLookup() 
    {

    }
    
    public static synchronized TacticalGraphicLookup getInstance()
    {
        if(_instance == null)
        {
        _instance = new TacticalGraphicLookup();
        }
        return _instance;
    }
    
    /**
    * @name init
    *
    * @desc Simply calls xmlLoaded
    *
    * @return None
    */
    public synchronized void init(String xml)
    {
        
        populateLookup(xml);
    }
    
      
  
    /**
   * @name populateLookup
   *
   * @desc
   *
   * @param xml - IN -
   * @return None
   */
  private void populateLookup(String xml)
  {
    ArrayList<String> al = XMLUtil.getItemList(xml, "<SYMBOL>", "</SYMBOL>");
    for(int i = 0; i < al.size(); i++)
    {
      String data = (String)al.get(i);

      String basicID = XMLUtil.parseTagValue(data, "<SYMBOLID>", "</SYMBOLID>");
      //String description = XMLUtil.parseTagValue(data, "<DESCRIPTION>", "</DESCRIPTION>");
      String mapping = XMLUtil.parseTagValue(data, "<MAPPING>", "</MAPPING>");

      mapping = checkMappingIndex(mapping);
      
      symbolMap.put(basicID, Integer.valueOf(mapping));

    }
  }
  
  /**
   * Until XML files are updated, we need to shift the index
   * @param index
   * @return 
   */  
 private static String checkMappingIndex(String index)
 {
      int i = -1;
      if(SymbolUtilities.isNumber(index))
      {
          i = Integer.valueOf(index);
          
       	  return String.valueOf(i + 57000);
      }
      return index;
 }
  
    /**
   * given the milstd symbol code, find the font index for the symbol.
   * @param symbolCode
   * @return
   */
    public int getCharCodeFromSymbol(String symbolCode)
    {
        int symStd = RendererSettings.getInstance().getSymbologyStandard();

        return getCharCodeFromSymbol(symbolCode, symStd);

    }
  public int getCharCodeFromSymbol(String symbolCode, int symStd)
  {

      try
      {
    	  String basicID = symbolCode;
          int charCode = -1;
          if(SymbolUtilities.is3dAirspace(symbolCode)==false)
          {
              basicID = SymbolUtilities.getBasicSymbolID(symbolCode);
          }
          if(symbolMap.containsKey(basicID))
          {
        	  charCode = symbolMap.get(basicID);
              if(charCode == 59053)
              {
                  if(symStd == 1)
                  {
                      charCode = 59052;
                  }
              }
          }
          return charCode;
      }
      catch(Exception exc)
      {
          ErrorLogger.LogException("TacticalGraphicLookup", "getCharCodeFromSymbol", exc, Level.WARNING);
      }
    return -1;

  } 
    
}
