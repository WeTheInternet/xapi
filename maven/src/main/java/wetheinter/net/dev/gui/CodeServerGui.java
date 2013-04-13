package wetheinter.net.dev.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;

import wetheinter.net.dev.CodeServerGuiOptions;

@SuppressWarnings("serial")
public class CodeServerGui extends JFrame{

  private static File tmpConfig;
  static {
    try {
      tmpConfig= File.createTempFile("xapi-config", "xml");
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private SingleFileSelector test;
  private SourcesSelector sources;
  private CodeServerControls controls;
  private ProcessLog logger;

  public CodeServerGui() {
    super("XApi Codeserver");
    BorderLayout layout = new BorderLayout(5, 5);
    setLayout(layout);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setBounds(100, 100, 500, 300);
//    setAlwaysOnTop(true);
    test = new SingleFileSelector("Set Work Directory");
    test.setToolTipText("The working directory where gwt codeserver will write compiles.  Defaults to "+tmpConfig.getParent());
    test.setChooserType(JFileChooser.DIRECTORIES_ONLY);
    test.setFile(tmpConfig.getParentFile());
    add(test,BorderLayout.NORTH);
    controls = new CodeServerControls(new Runnable() {
      @Override
      public void run() {
        launchServer(isUseTestSources());
      }
    });
    add(controls,BorderLayout.SOUTH);
    sources = new SourcesSelector("Gwt Sources");
    logger = new ProcessLog(){
      Runnable recalc;
      @Override
      public void invalidate() {
        super.invalidate();
        if (null==recalc){
          recalc = new Runnable() {
            @Override
            public void run() {
              recalc = null;
              CodeServerGui.this.validate();
            }
          };
          SwingUtilities.invokeLater(recalc);
        }
      }
    };
    JScrollPane wrap =
        new JScrollPane(logger,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//    logger.setMaximumSize(new Dimension(500, 800));
//    wrap.setMaximumSize(new Dimension(500, 800));
    JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitter.setLeftComponent(sources);
    splitter.setRightComponent(wrap);
    splitter.setResizeWeight(0.5);
    add(splitter,BorderLayout.CENTER);
//    add(logger,BorderLayout.EAST);
  }

  protected boolean isUseTestSources() {
    return false;
  }


  protected String getXapiPluginJar() {
    return "/repo/wetheinter/net/xapi-maven-plugin/0.2/xapi-maven-plugin-0.2.jar";
  }

  protected void launchServer(boolean includeTestSources) {
    try{
      String cpSep = ":";
    String cp = getClasspath(includeTestSources, cpSep);
//    //TODO remove this
    String xapi = getXapiPluginJar();
    if (!StringUtils.isEmpty(xapi)) {
      cp = xapi+cpSep+cp;
    }
    String candidateParent = null;
    String version = "";
    if (!cp.contains("gwt-codeserver")){
      System.out.println("gwt-codeserver.jar was not found in classpath.  Trying to guess from existing classpath.");
      //if the user did not specify codeserver location, let's try to guess it.
      search: for (String chunk0 : cp.split("[:]")){//handle unix
        for (String chunk1 : chunk0.split("[;]")){//handle windows
          if ("".equals(chunk1))continue;
          if (chunk1.contains("gwt-user")){
            try{
              int ind = chunk1.lastIndexOf("gwt-user");
              if (ind<0)continue;
            File f = new File(chunk1.substring(0, ind));
            if (f.exists()){
              System.out.println("Checking for gwt-codeserver from "+f);
              version = chunk1.substring(ind+8).split(".jar")[0];
              String jarName = "gwt-codeserver";
              if (
                  !".jar".equals(version)&&version.length()>0
                  ){
                jarName+=version;
              }
              jarName += ".jar";
              System.out.println("Looking for "+jarName);
              File jar = new File(f,jarName);
              if (jar.exists()){
                cp = jar + cpSep + cp;
                candidateParent = f.toString();
                System.out.println("Found codeserver @ "+jar);
                break search;
              }else{
                //check if our gwt-user is in maven, and adjust paths accordingly.
                File parent = jar.getParentFile();
                version = version.replace("-", "");//strip the - from version
                if (parent.getName().equals(version)){//maven structure; go up two directories
                  parent = new File(parent.getParentFile().getParentFile(),"gwt-codeserver");
                  if (parent.exists()){
                    parent = new File(parent,version);
                    if (parent.exists()){
                      jar = new File(parent,jarName);
                      if (jar.exists()){
                        candidateParent = parent.getParentFile().getParent();
                        cp = jar.getAbsolutePath() + cpSep + cp;
                        System.out.println("Found codeserver @ "+jar);
                        break search;
                      }
                    }
                  }
                }
              }
            }
            }catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        }
      }
    }

    if (!cp.contains("gwt-dev")){
      System.out.println("gwt-dev not found in classpath.  Trying to guess from existing classpath.");
      if (null!=candidateParent){
        try{
        File parent = new File(candidateParent);
        String jarName = "gwt-dev";
        if (version.length()>0){
          version = version.replace("-", "");
          jarName += "-"+version;
        }
        jarName += ".jar";
        File candidate = new File(parent,jarName);
        System.out.println("Checking if "+candidate+" exists");
        if (candidate.exists()){
          System.out.println("Found gwt-dev @ "+candidate);
          cp = cp+cpSep+candidate;
        }else{
          if (version.length()>0){
            //we're in a maven directory. First, make sure we're in the correct parent.
            if (parent.getParentFile().getName().equals(version)){
              parent = parent.getParentFile();
            }
            if (parent.getParentFile().getName().equals("gwt-user")){
              parent = parent.getParentFile();
            }
            parent = new File(parent,"gwt-dev");
            if (parent.exists()){
              parent = new File(parent,version);
              if (parent.exists()){
                candidate = new File(parent,jarName);
                if (candidate.exists()){
                  System.out.println("Found gwt-dev @ "+candidate);
                  cp = cp+cpSep+candidate;
                }
              }
            }
          }
        }

        }catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }

    LinkedList<String> paths = getSourcePaths(includeTestSources);

    //TODO conditionally add testing dependencies as well...

//    paths.addFirst("/shared/xapi/super/src/test/java");
    for (String path : paths.toArray(new String[paths.size()])){
      File pathFile = new File(path);
      if (pathFile.exists()){
        if (path.endsWith("classes")){
          cp += cpSep+pathFile;
          paths.remove(path);
        }
      }
    }
    for (File path : classpath){
      if (path.getAbsolutePath().endsWith("classes")){
        cp = path+cpSep+cp;
      }else{
        cp += cpSep+path;
      }
    }
    int debugPort = getDebugPort();
    int len = debugPort > 0 ? 8 : 6;
    final String[] cmdArray = new String[len];
    cmdArray[0] = //path to java executable
      System.getProperty("java.home")+File.separator+"bin" +File.separator+"java";
    cmdArray[1] = "-cp";
    cmdArray[2] = cp.replaceAll("(\\s)", "\\\\$1");
    int pos = 3;
    if (debugPort > 0) {
      cmdArray[pos++] = "-Xdebug";
      cmdArray[pos++] =
        "-agentlib:jdwp=transport=dt_socket,address=localhost:" +
          debugPort+ ",server=y,suspend=y,timeout=10000";
      System.out.println("Waiting to attach debugger on port "+debugPort+
        " for 10 seconds");
    }

    cmdArray[pos++] = "com.google.gwt.dev.codeserver.CodeServer";
    cmdArray[pos++] = "-port";
    cmdArray[pos++] = Integer.toString(getPort());

    String [] srcArray = toCli(paths);

    len = 1+cmdArray.length+srcArray.length;

    final String[] exec = new String[len];
    pos = cmdArray.length;
    System.arraycopy(cmdArray, 0, exec, 0, pos);
    System.arraycopy(srcArray, 0, exec, pos, srcArray.length);
    exec[exec.length-1]=getModule();

    String toRun = Arrays.asList(exec).toString().replaceAll(", ", " ");
    System.out.println("exec:\n"+toRun.substring(1,toRun.length()-1));
    try {
      Process handle = Runtime.getRuntime().exec(exec);
      if (debugPort>0) {
        System.out.println("Not monitoring logs to avoid interfering with debugger");
      }else {
        logger.monitor(handle,getModule());
      }
      if (getWidth() < 1000){
        Rectangle b = getBounds();
        b.width = 1000;
        b.height = 600;
        setBounds(b);
      }
      final String module = getModule();


      final JPanel wrap = new JPanel(new FlowLayout());
      JButton restart = new JButton(new AbstractAction("Kill & Restart") {
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.stop(module);
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                try{
                Process handle = Runtime.getRuntime().exec(exec);
                logger.monitor(handle,getModule());
              }catch (Exception ex) {
                ex.printStackTrace();
              }
              }
            });
        }
      });
      JButton kill = new JButton(new AbstractAction("Kill") {
        @Override
        public void actionPerformed(ActionEvent e) {
          logger.stop(module);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              controls.remove(wrap);
              repaint();
            }
          });
        }
      });
      wrap.add(restart);
      wrap.add(kill);
      controls.add(getModule(),wrap);
    } catch (IOException e) {
      e.printStackTrace();
      logger.log("Startup failure", e);
    }

  }catch (Exception ex) {
    System.out.println(ex.toString());
    ex.printStackTrace();
    add(new JLabel(ex.toString()),BorderLayout.WEST);
  }
}

  private int getDebugPort() {
    return 0;//7331;
  }

  protected int getPort() {
    return 1733;
  }

  protected String[] toCli(LinkedList<String> sourcePaths) {
    ArrayList<String> parts = new ArrayList<String>();
    for (String path : sourcePaths){
      parts.add("-src");
      parts.add(path);
    }
    return parts.toArray(new String[parts.size()]);
  }

  protected String getModule() {
    String module=controls.getModule();
    if (null==module||module.length()==0){
      return "wetheinter.net.Demo";
    }
    return module;
  }
  public void setModule(String module){
    controls.setModule(module);
  }

  protected LinkedList<String> getSourcePaths(boolean includeTestSources) {
    return sources.getSourcePaths(includeTestSources);
  }

  protected String getClasspath(boolean includeTestSources, String cpSep) {
    return sources.getClasspath(includeTestSources, cpSep);
  }

  public void addSource(File baseDir) {
    sources.addSource(baseDir);
  }

  public void addTestSource(File baseDir) {
    sources.addTestSource(baseDir);
  }

  public void run(CodeServerGuiOptions opts) {

    if (opts.isUnload()){
      //if the current module is already running, try to stop it
      //TODO: implement this;
      return;
    }


    //make sure we are showing.
    if (!isVisible())
      setVisible(true);
    //
  }

  private final Set<File> classpath = new LinkedHashSet<File>();
  public void addToClasspath(File f) {
    sources.addSource(f);
    classpath.add(f);
  }
  private final Set<File> testClasspath = new LinkedHashSet<File>();
  public void addToTestClasspath(File f) {
    sources.addTestSource(f);
    testClasspath.add(f);
  }
}
