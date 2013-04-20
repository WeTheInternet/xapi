package wetheinter.net.dev.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

@SuppressWarnings("rawtypes")
public class SourcesSelector extends JPanel{

  private static final long serialVersionUID = 1153019705058563667L;

  private JSplitPane splitter;
  private Label label;
  private DefaultListModel dir;
  private final Set<File> testSources = new LinkedHashSet<File>();


  private JFileChooser chooser;

  private JList list;

  @SuppressWarnings({"unchecked", "serial"})
  public SourcesSelector() {
    super(new BorderLayout(0, 5));
    JPanel buttons = new JPanel(new GridLayout(4, 1,0,10));
    buttons.add(new JButton(new AbstractAction("Add Source") {
      @Override
      public void actionPerformed(ActionEvent e) {
        chooser.showOpenDialog(null);
      }
    }));
    buttons.add(new JButton(new AbstractAction("Load Sources") {
      @Override
      public void actionPerformed(ActionEvent e) {
        //TODO: use an xml config file to load sources from a saved file
        refreshModules();
      }
    }));
    buttons.add(new JButton(new AbstractAction("Remove Source") {
      @Override
      public void actionPerformed(ActionEvent e) {
        int[] selected = list.getSelectedIndices();
        int pos = selected.length;
        while(pos>0)
          dir.remove(selected[--pos]);
        refreshModules();
      }
    }));
    buttons.add(new JButton(new AbstractAction("Remove All Sources") {
      @Override
      public void actionPerformed(ActionEvent e) {
        dir.removeAllElements();
        refreshModules();
      }
    }));
    splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitter.setLeftComponent(buttons);
    dir = new DefaultListModel();
    chooser = new JFileChooser();
    chooser.setFileFilter(new FileFilter() {
      @Override
      public String getDescription() {
        return "Directories Or Jars";
      }

      @Override
      public boolean accept(File f) {
        return f.isDirectory()||f.toString().endsWith(".jar");
      }
    });
    chooser.setMultiSelectionEnabled(true);
    chooser.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (File f :chooser.getSelectedFiles()){
          int ind = dir.indexOf(f);
          if (ind>-1)
            dir.remove(ind);
          dir.add(0, f);
        }
        refreshModules();
      }
    });
    list = new JList(dir);
    list.setCellRenderer(new DefaultListCellRenderer(){
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
        try{
          File f = (File) value;
          if (f.isDirectory()){
            return super.getListCellRendererComponent(list, f.toString(), index, isSelected, cellHasFocus);
          }else{
            return super.getListCellRendererComponent(list, f.getName(), index, isSelected, cellHasFocus);
          }
        }catch (Exception e) {
          e.printStackTrace();
        }
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }
    });
    list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        int[] selected = list.getSelectedIndices();
        if (selected.length>0){
          File f = (File) dir.get(selected[0]);
          if (f.isDirectory()){
            chooser.setCurrentDirectory(f);
          }else{
            chooser.setCurrentDirectory(f.getParentFile());
          }
        }
      }
    });
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    JScrollPane scroller = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    splitter.setRightComponent(scroller);
    add(splitter);
    splitter.setDividerLocation(0.5);
  }
  protected void refreshModules() {
    //Scan the selected sources for .gwt.xml files with <entry-point> elements

  }
  public SourcesSelector(String title) {
    this();
    setTitle(title);
  }
  public void setTitle(String title) {
    if (null==label){
      label = new Label(title);
      label.setAlignment(Label.CENTER);
      add(label,BorderLayout.NORTH);
    }else{
      label.setText(title);
    }

  }
  public void addSource(File file) {
    addSource(file,true);
  }
  public void addTestSource(File file) {
    addSource(file,true);
    testSources.add(file);
  }
  @SuppressWarnings("unchecked")
  public void addSource(File file,boolean resolve) {
    //TODO: put in some whitelist / blacklist parameters
    if (resolve && file.isDirectory()){
      //resolve the /src/main/java portion of source directories.
      File next = new File(file,"src");
      if (next.exists()){
        file = next;
        next = new File(file,"main");
        if (next.exists()){
          file = next;
          next = new File(file,"java");
          if (next.exists()){
            file = next;
          }
        }
      }
    }

    if (file.isDirectory()||file.toString().endsWith(".jar")){
      int was = dir.indexOf(file);
      if (was>-1){
        dir.remove(was);
      }
      dir.add(dir.getSize(),file);
    }
  }
  /**
   * @return A java exec compatible string of all jars on the classpath
   */
  public String getClasspath(boolean includeTestSource, String cpSep) {
    int size = dir.getSize();
    StringBuilder b = new StringBuilder();
    String prefix = "";
    for(int i = -1;++i < size;){
      File item = (File)dir.get(i);
      if (!includeTestSource){
        if (testSources.contains(item))
          continue;
      }
      if (item.isFile()){
        b.append(prefix+item);
        prefix=cpSep;
      }
    }
    return b.toString();
  }
  public LinkedList<String> getSourcePaths(boolean includeTestSource) {
    LinkedList<String> paths = new LinkedList<String>();
    int size = dir.getSize();
    for(int i = -1;++i < size;){
      File item = (File)dir.get(i);
      if (!includeTestSource){
        if (testSources.contains(item))
          continue;
      }
      if (item.isDirectory()){
        paths.add(item.toString());
      }
    }
    return paths;
  }

}
