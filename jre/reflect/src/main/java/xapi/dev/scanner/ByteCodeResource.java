package xapi.dev.scanner;

import java.io.DataInputStream;
import java.io.IOException;

import xapi.bytecode.ClassFile;
import xapi.util.X_Debug;

public class ByteCodeResource extends DelegateClasspathResource{

  private ClassFile data;

  public ByteCodeResource(ClasspathResource source) {
    super(source);
  }

  public ClassFile getClassData() {
    if (data == null) {
      try {
        DataInputStream in = new DataInputStream(open());
        try {
          data = new ClassFile(in);
        }finally {
          in.close();
        }
      } catch(IOException e) {
        throw X_Debug.wrap(e);
      }
    }
    return data;
  }

}
