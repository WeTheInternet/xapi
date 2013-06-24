package xapi.dev.gwt;

import java.util.Arrays;

import xapi.dev.gwt.i18n.CodeServerDebugMessages.Debug;

import com.google.gwt.dev.ArgProcessorBase;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerInt;

public class CodeServerGuiOptions{

  private int port = 9876;
  private boolean unload;
  //exposed for use in type-free cross-process communication.
  public static final String
    moduleParam = "module"
    ,portParam = "port"
    ,unloadParam = "unload"
    ,testParam = "testing"
  ;
  
  
  private class ArgProcessor extends ArgProcessorBase{
    
    public ArgProcessor(CodeServerGuiOptions opts) {
      registerHandler(new PortFlag());
      registerHandler(new UnloadFlag(opts));
    }
    
    @Override
    protected String getName() {
      return CodeServerMain.class.getName();
    }
    
  }

  /**
   * 
   * @param args
   * @return non-null String to cause System.exit();
   */
  public String parseArgs(String[] args) {
    return new ArgProcessor(this).processArgs(args)
      ? null : Debug.unableToStartServer()+Arrays.asList(args);
  }
  

  public int getPort() {
    return port;
  }


  public boolean isUnload() {
    return unload;
  }


  public void setUnload(boolean unload) {
    this.unload = unload;
  }


  private class PortFlag extends ArgHandlerInt {
    
    @Override
    public String getTag() {
      return "-"+portParam;
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {portParam};
    }


    @Override
    public String getPurpose() {
      return "The port where the code server will run.";
    }

    @Override
    public void setInt(int newValue) {
      port = newValue;
    }
  }
  
  private class UnloadFlag extends ArgHandlerFlag{

    private final CodeServerGuiOptions opts;

    public UnloadFlag(CodeServerGuiOptions opts) {
      this.opts = opts;
    }
    
    @Override
    public boolean setFlag() {
      opts.setUnload(true);
      return true;
    }

    @Override
    public String getPurpose() {
      return "When supplied, tell the CodeServerGui to unload the running modules, if any.";
    }

    @Override
    public String getTag() {
      return "-"+unloadParam;
    }
    
  }

}
