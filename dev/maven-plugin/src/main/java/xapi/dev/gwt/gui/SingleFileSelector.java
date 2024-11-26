package xapi.dev.gwt.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SingleFileSelector extends JPanel {

  private static final long serialVersionUID = 1213610727860383569L;

  private final JButton button;
  private final JLabel label;
  private final JFileChooser chooser;

  private int mode;

  private String buttonText;

  public SingleFileSelector() {
    super(new GridLayout(1, 2, 10, 5));
    button = new JButton("Select File");
    label = new JLabel();
    chooser = new JFileChooser();
    init();
  }

  public SingleFileSelector(String text) {
    this();
    buttonText = text;
    button.setText(buttonText);
    init();
  }

  public void setChooserType(int type) {
    chooser.setFileSelectionMode(type);
    this.mode = type;
    if (null == buttonText)
      button.setText(
          "Select " + (
              type == JFileChooser.DIRECTORIES_ONLY
                  ? "Directory" :
                  type == JFileChooser.FILES_AND_DIRECTORIES
                      ? "File or Directory" :
                      "File"
          )
      );
  }

  protected void init() {
    if (getComponentCount() > 0)
      return;
    button.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            //popup a JFileChooser
            chooser.showOpenDialog(null);
          }
        }
    );
    chooser.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            File file = chooser.getSelectedFile();
            if (null == file) {
            } else {
              switch (mode) {
                case JFileChooser.DIRECTORIES_ONLY:
                  if (!file.isDirectory()) {
                    //show an error
                  }
                case JFileChooser.FILES_ONLY:
                case JFileChooser.FILES_AND_DIRECTORIES:
                default:
              }
              setFile(file);
            }

          }
        }
    );
    add(button);
    //    add(Box.createHorizontalGlue());
    add(label);
    setAlignmentX(0);
  }

  public void setFile(final File baseDir) {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            chooser.setCurrentDirectory(baseDir);
          }
        }
    );
    label.setText(baseDir.toString());
  }

}
