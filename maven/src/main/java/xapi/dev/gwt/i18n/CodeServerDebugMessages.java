package xapi.dev.gwt.i18n;

import com.google.gwt.core.shared.GWT;

public interface CodeServerDebugMessages {
  /**
   * This class houses our static final il8n singleton.
   * Recommended use:
   * import xapi.dev.il8n.CodeServerDebugMessages.Static.*;
   * 
   * @author James X. Nelson (james@wetheinter.net, @ajax)
   *
   */
  public class Debug{
    static final CodeServerDebugMessages msgs;
    static{
      CodeServerDebugMessages candidate;
      try{
        candidate = GWT.create(CodeServerDebugMessages.class);
      }catch (Exception e) {
        try {
          candidate = Messages_EN.class.newInstance();
        } catch (Exception e1) {
          e1.printStackTrace(System.err);
          candidate = null;//needed for ide not to complain about unassigned finals
          System.exit(1);
        }
      }
      msgs = candidate;
    }
    public static String unableToStartServer(){
      return msgs.unableToStartServer();
    }
    public static String unableToParseArguments(){
      return msgs.unableToParseArguments();
    }
    
  }
  String unableToStartServer();
  String unableToParseArguments();

}
