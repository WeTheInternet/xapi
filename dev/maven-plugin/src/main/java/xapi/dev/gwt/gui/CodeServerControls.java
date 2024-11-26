package xapi.dev.gwt.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class CodeServerControls extends JPanel{

  private transient Runnable launch;
  JTextField module;
  private GridBagLayout layout;
  private final Map<Component,Component> alsoRemove = new HashMap<Component,Component>();

  public CodeServerControls(Runnable launch) {
    super();
    layout = new GridBagLayout();
    setLayout(layout);
    this.launch = launch;
    module = new JTextField("com.example.MyApp");
    module.setPreferredSize(new Dimension(350, 30));
    module.setSize(new Dimension(350, 30));
    render();
  }
  GridBagConstraints
  col0 = new GridBagConstraints(), col1 = new GridBagConstraints();

  protected void render() {
    layout.columnWeights = new double[]{0.3,0.7};
    layout.rowHeights = new int[]{30,30};
    JButton launcher = new JButton(new AbstractAction("Launch CodeServer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (null!=launch)
          SwingUtilities.invokeLater(launch);
      }
    });
    add(new JLabel("Target Module"),col0);
    add(module,col1);
    col0 = new GridBagConstraints();
    col1 = new GridBagConstraints();

    col0.gridy=1;
    col1.gridy=1;
    add(new JLabel("Start Server"),col0);
    add(launcher,col1);
  }



  @Override
  public Component add(String label, Component comp) {
    col0.gridy++;
    col1.gridy++;
    JLabel jlabel = new JLabel(label);
    add(jlabel, col0);
    add(comp, col1);
    alsoRemove.put(comp, jlabel);
    return comp;
  }

  @Override
  public void remove(Component comp) {
    super.remove(comp);
    Component alsoRem = alsoRemove.remove(comp);
    if (alsoRem != null) {
      super.remove(alsoRem);
    }
  }

  public String getModule() {
    return module.getText();
  }

  public void setModule(String module){
    this.module.setText(module);
    this.module.invalidate();
  }
}
