package xapi.source.read;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import xapi.source.read.JavaModel.AnnotationMember;
import xapi.source.read.JavaModel.IsAnnotation;
import xapi.source.read.JavaModel.IsParameter;

public class JavaModelTest {


  @Test
  public void testSimpleParam () {
    IsParameter param = JavaLexer.lexParam("@NotNull String param");
    Assert.assertEquals("String", param.getType());
    Assert.assertEquals("param", param.getName());
    Iterator<IsAnnotation> annos = param.annotations.getAnnotations().iterator();
    Assert.assertTrue(annos.hasNext());
    Assert.assertEquals("NotNull", annos.next().qualifiedName);
  }

  @Test
  public void testArrayParam () {
    IsParameter param = JavaLexer.lexParam("Class<?>[] param");
    Assert.assertEquals("Class<?>[]", param.getType());
    Assert.assertEquals("param", param.getName());
  }

  @Test
  public void testComplexParam () {
    IsParameter param = JavaLexer.lexParam("final @Named(\"param\") @NotNull String param");
    Assert.assertEquals("String", param.getType());
    Assert.assertEquals("param", param.getName());
    Iterator<IsAnnotation> annos = param.annotations.getAnnotations().iterator();
    Assert.assertTrue(annos.hasNext());
    IsAnnotation anno = annos.next();
    Assert.assertEquals("Named", anno.qualifiedName);
    Iterator<AnnotationMember> members = anno.members.iterator();
    Assert.assertTrue(members.hasNext());
    Assert.assertEquals("\"param\"", members.next().value);
    Assert.assertTrue(annos.hasNext());
    Assert.assertEquals("NotNull", annos.next().qualifiedName);
  }

}
