package xapi.bytecode.impl;

import java.util.Arrays;

import xapi.bytecode.annotation.Annotation;
import xapi.bytecode.annotation.AnnotationMemberValue;
import xapi.bytecode.annotation.AnnotationsAttribute;
import xapi.bytecode.annotation.ArrayMemberValue;
import xapi.bytecode.annotation.BooleanMemberValue;
import xapi.bytecode.annotation.ByteMemberValue;
import xapi.bytecode.annotation.CharMemberValue;
import xapi.bytecode.annotation.ClassMemberValue;
import xapi.bytecode.annotation.DoubleMemberValue;
import xapi.bytecode.annotation.EnumMemberValue;
import xapi.bytecode.annotation.FloatMemberValue;
import xapi.bytecode.annotation.IntegerMemberValue;
import xapi.bytecode.annotation.LongMemberValue;
import xapi.bytecode.annotation.MemberValue;
import xapi.bytecode.annotation.MemberValueVisitor;
import xapi.bytecode.annotation.ShortMemberValue;
import xapi.bytecode.annotation.StringMemberValue;
import xapi.collect.impl.ToStringFifo;
import xapi.log.X_Log;
import xapi.source.X_Modifier;
import xapi.source.api.IsAnnotationValue;
import xapi.source.impl.ImmutableAnnotationValue;
import xapi.util.api.ConvertsValue;

public class BytecodeUtil {

  public static Annotation[] extractAnnotations(AnnotationsAttribute visible, AnnotationsAttribute invisible) {
    Annotation[] 
        vis = visible == null ? null : visible.getAnnotations()
        , invis = invisible == null ? null : invisible.getAnnotations();
    if (vis == null) {
      return invis == null ? new Annotation[0] : invis;
    }
    if (invis == null)
      return vis;
    vis = Arrays.copyOf(vis, vis.length+invis.length);
    System.arraycopy(invis, 0, vis, vis.length - invis.length, invis.length);
    return vis;
  }

  public static IsAnnotationValue extractValue(MemberValue value, BytecodeAdapterService service) {
    return new ValueExtractor().extract(value, service);
  }
  
  private static final class ArrayTypeExtractor implements MemberValueVisitor {
    String type;
    int modifier;
    @Override
    public void visitAnnotationMemberValue(AnnotationMemberValue node) {
      type = node.getValue().getTypeName();
      modifier = X_Modifier.ANNOTATION;
    }
    @Override
    public void visitArrayMemberValue(ArrayMemberValue node) {
      throw new IllegalStateException("Array types cannot have array members");
    }
    @Override
    public void visitBooleanMemberValue(BooleanMemberValue node) {
      type = "boolean";
      modifier = -1;
    }
    @Override
    public void visitByteMemberValue(ByteMemberValue node) {
      type = "byte";
      modifier = -1;
    }
    @Override
    public void visitCharMemberValue(CharMemberValue node) {
      type = "char";
      modifier = -1;
    }
    @Override
    public void visitDoubleMemberValue(DoubleMemberValue node) {
      type = "double";
      modifier = -1;
    }
    @Override
    public void visitEnumMemberValue(EnumMemberValue node) {
      type = node.getType();
      modifier = X_Modifier.ENUM;
    }
    @Override
    public void visitFloatMemberValue(FloatMemberValue node) {
      type = "float";
      modifier = -1;
    }
    @Override
    public void visitIntegerMemberValue(IntegerMemberValue node) {
      type = "int";
      modifier = -1;
    }
    @Override
    public void visitLongMemberValue(LongMemberValue node) {
      type = "long";
      modifier = -1;
    }
    @Override
    public void visitShortMemberValue(ShortMemberValue node) {
      type = "short";
      modifier = -1;
    }
    @Override
    public void visitStringMemberValue(StringMemberValue node) {
      type = "java.lang.String";
    }
    @Override
    public void visitClassMemberValue(ClassMemberValue node) {
      type = "java.lang.Class";
    }
  }
  private static final class ValueExtractor implements MemberValueVisitor {

    IsAnnotationValue value;
    private BytecodeAdapterService service;
    
    public IsAnnotationValue extract(MemberValue member, BytecodeAdapterService service) {
      this.service = service;
      member.accept(this);
      return value;
    }

    @Override
    public void visitAnnotationMemberValue(AnnotationMemberValue node) {
      final Annotation val = node.getValue();
      value = new ImmutableAnnotationValue(val.getTypeName(), service.toAnnotation(val), new ConvertsValue<Object, String>() {
        @Override
        public String convert(Object from) {
          return val.toString();
        }
      }, X_Modifier.ANNOTATION);
    }

    @Override
    public void visitArrayMemberValue(ArrayMemberValue node) {
      MemberValue[] val = node.getValue();
      ToStringFifo<IsAnnotationValue> toString = new ToStringFifo<IsAnnotationValue>();
      ValueExtractor extract = new ValueExtractor();
      MemberValue type = node.getType();
      for (MemberValue member : val) {
        toString.give(extract.extract((type = member), service));
      }
      ArrayTypeExtractor getType = new ArrayTypeExtractor();
      type.accept(getType);
      value = new ImmutableAnnotationValue(getType.type, toString, getType.modifier);
    }

    @Override
    public void visitBooleanMemberValue(BooleanMemberValue node) {
      value = new ImmutableAnnotationValue("boolean", node.getValue(), -1);
    }

    @Override
    public void visitByteMemberValue(ByteMemberValue node) {
      value = new ImmutableAnnotationValue("byte", node.getValue(), -1);
    }

    @Override
    public void visitCharMemberValue(CharMemberValue node) {
      value = new ImmutableAnnotationValue("char", node.getValue(), -1);
    }

    @Override
    public void visitDoubleMemberValue(DoubleMemberValue node) {
      value = new ImmutableAnnotationValue("double", node.getValue(), -1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visitEnumMemberValue(EnumMemberValue node) {
      Object val;
      try {
        Class<?> enumCls = Class.forName(node.getType(), false, service.getClassLoader());
        val = Enum.valueOf(Class.class.cast(enumCls), node.getValue());
      } catch (Exception e) {
        X_Log.error("Unable to load enum value type "+node.getType()+"."+node.getValue(),e);
        val = node.getValue();
      }
      value = new ImmutableAnnotationValue(node.getType(), val, X_Modifier.ENUM);
    }

    @Override
    public void visitFloatMemberValue(FloatMemberValue node) {
      value = new ImmutableAnnotationValue("float", node.getValue(), -1);
    }

    @Override
    public void visitIntegerMemberValue(IntegerMemberValue node) {
      value = new ImmutableAnnotationValue("int", node.getValue(), -1);
    }

    @Override
    public void visitLongMemberValue(LongMemberValue node) {
      value = new ImmutableAnnotationValue("long", node.getValue(), -1);
    }

    @Override
    public void visitShortMemberValue(ShortMemberValue node) {
      value = new ImmutableAnnotationValue("short", node.getValue(), -1);
    }

    @Override
    public void visitStringMemberValue(StringMemberValue node) {
      value = new ImmutableAnnotationValue("java.lang.String", node.getValue(), 0);
    }

    @Override
    public void visitClassMemberValue(ClassMemberValue node) {
      Object cls;
      final String name = node.getValue();
      try {
        cls = Class.forName(name, false, service.getClassLoader());
      } catch (Exception e ) {
        X_Log.error("Unable to load annotation class value ",node.getValue());
        cls = node.getValue();
      }
      value = new ImmutableAnnotationValue("java.lang.Class", cls, new ConvertsValue<Object, String>() {
        @Override
        public String convert(Object from) {
          return name;
        }
      }, 0);
    }
    
  }
   
}
