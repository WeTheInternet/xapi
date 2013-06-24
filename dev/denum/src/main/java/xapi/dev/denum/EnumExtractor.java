package xapi.dev.denum;

import xapi.source.read.JavaVisitor.AnnotationMemberVisitor;
import xapi.source.read.JavaVisitor.ClassBodyVisitor;
import xapi.source.read.JavaVisitor.ClassVisitor;

public class EnumExtractor implements ClassVisitor<EnumDefinition> {

  @Override
  public AnnotationMemberVisitor<EnumDefinition> visitAnnotation(
      String annoName, String annoBody, EnumDefinition receiver) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void visitGeneric(String generic, EnumDefinition receiver) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visitJavadoc(String javadoc, EnumDefinition receiver) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visitModifier(int modifier, EnumDefinition receiver) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visitImport(String name, boolean isStatic, EnumDefinition receiver) {
    if (isStatic) {
      receiver.imports.addImport(name);
    } else {
      receiver.imports.addImport(name);
    }
  }

  @Override
  public void visitCopyright(String copyright, EnumDefinition receiver) {
    receiver.copyright = copyright;
  }

  @Override
  public void visitPackage(String pkg, EnumDefinition receiver) {
    receiver.packageName = pkg;
  }

  @Override
  public void visitName(String name, EnumDefinition receiver) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visitSuperclass(String superClass, EnumDefinition receiver) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visitInterface(String iface, EnumDefinition receiver) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ClassBodyVisitor<EnumDefinition> visitBody(String body,
      EnumDefinition receiver) {
    // TODO Auto-generated method stub
    return null;
  }

}
