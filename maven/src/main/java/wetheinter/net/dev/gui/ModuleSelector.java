package wetheinter.net.dev.gui;

import javax.swing.DefaultListModel;
import javax.swing.JList;

public class ModuleSelector extends JList{

	private static final long serialVersionUID = 4183928788452362501L;
  //TODO: take a given source path, and lookup available modules

public ModuleSelector() {
    super(new DefaultListModel());
  }
}
