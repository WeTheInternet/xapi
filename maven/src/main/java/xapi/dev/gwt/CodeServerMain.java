package xapi.dev.gwt;

import xapi.dev.gwt.gui.CodeServerGui;
import xapi.dev.gwt.i18n.CodeServerDebugMessages.Debug;
import xapi.fu.Lazy;

import java.io.*;
import java.lang.management.ManagementFactory;

public class CodeServerMain {

  private static File guiPid;

  public static void main(String[] args) throws Exception{
    for (String arg : args)
      System.out.println(arg);
    //TODO: parse some cmd line options to construct an initialized CodeServer;
    CodeServerGuiOptions opts = new CodeServerGuiOptions();
    String failure = opts.parseArgs(args);
    if (failure!=null) {
      if (failure.length()>0){
        System.err.println(Debug.unableToStartServer()+": "+failure);
      }
      System.exit(1);
    }

    //TODO: check for an existing, running server to send initialization params.
    Long pid = getRunningGui();
    if (null!=pid){
      //try to write these commands to existing gui's System.in stream.
      //if this fails, we will just start a new server
      try{
        writeOptionsToExistingProcess(pid,opts);
        return;
      }catch (Exception e) {
      }

    }
      //Start a new server
      CodeServerGui server = new CodeServerGui();
      server.run(opts);
      trySavePid();
  }

  private static void writeOptionsToExistingProcess(Long pid, CodeServerGuiOptions opts) {
    throw new RuntimeException();
  }

  private static void trySavePid() {
    FileOutputStream out = null;
    try{
     File shared = getSharedFile();
     if (null!=shared){
       out = new FileOutputStream(shared);
       out.write(Long.toString(getCurrentPid()).getBytes());
     }
    }catch (Exception e) {
      e.printStackTrace();
    }finally{
      if (null!=out)
        try {
          out.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
    }
  }

  private static File getSharedFile(){
    if (null==guiPid){
      try {
        guiPid = File.createTempFile("gwt-codeserver-gui", ".x");
      } catch (IOException e) {
        e.printStackTrace();//ignore
      }
    }
    return guiPid;
  }

  private static final Lazy<File> fileProvider = Lazy.deferred1(CodeServerMain::getSharedFile);

  private static Long getCurrentPid(){
    try{
      String name = ManagementFactory.getRuntimeMXBean().getName();
      int index = name.indexOf('@');
      Long processId = Long.parseLong(name.substring(0,index));
      return processId;
    }catch (Exception e) {
      return null;
    }
  }

  private static Long getRunningGui() {
    File shared = fileProvider.out1();
    if (null!=shared){
      BufferedReader read = null;
      try {
        read = new BufferedReader(new FileReader(shared));
        String first = read.readLine();
        if (null!=first){
          return Long.parseLong(first);
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (read != null)try {read.close();} catch (IOException e) {}
      }
    }
    return null;
  }
}
