package xapi.dev.gwt.gui;

import javax.swing.DefaultListModel;
import javax.swing.JList;

public class ModuleSelector extends JList{

  //TODO: take a given source path, and lookup available modules
  private static final long serialVersionUID = 4183928788452362501L;

  public ModuleSelector() {
    super(new DefaultListModel());
  }
}