package xapi.bytecode.impl;

import java.lang.reflect.Array;
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
import xapi.source.api.IsClass;
import xapi.source.api.IsType;
import xapi.source.impl.ImmutableAnnotationValue;
import xapi.util.X_Debug;
import xapi.util.api.ConvertsValue;
import xapi.util.converters.ConvertsStringValue;

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

  public static IsAnnotationValue extractValue(MemberValue value, BytecodeAdapterService service, IsType type) {
    return new ValueExtractor().extract(value, service, type);
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
    private IsType knownType;
    
    public IsAnnotationValue extract(MemberValue member, BytecodeAdapterService service, IsType type) {
      this.service = service;
      this.knownType = type;
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
      if (type == null) {
        try {
          node.getType(service.getClassLoader());
        } catch (ClassNotFoundException e) {
          if (val.length == 0 && this.knownType != null) {
              // We have a zero-arg array value of a known type; we can create the necessary value
            
            IsClass cls = service.toClass(knownType.getQualifiedName());
            Object value;
            ConvertsValue<Object, String> toStringer = new ConvertsValue<Object, String>() {
              @Override
              public String convert(Object from) {
                return "{}";
              }
            };
            int modifier = 0;
            if (cls.isPrimitive()) {
              modifier = -1;
              switch (cls.getQualifiedName()) {
                case "boolean":
                  value = new boolean[0];
                  break;
                case "byte":
                  value = new byte[0];
                  break;
                case "char":
                  value = new char[0];
                  break;
                case "short":
                  value = new short[0];
                  break;
                case "int":
                  value = new int[0];
                  break;
                case "long":
                  value = new long[0];
                  break;
                case "float":
                  value = new float[0];
                  break;
                case "double":
                  value = new double[0];
                  break;
                default:
                  throw new IllegalArgumentException();
              }
            } else {
              if (X_Modifier.isEnum(cls.getModifier())) {
                modifier = X_Modifier.ENUM;
                try {
                  Class<?> clazz = cls.toClass(service.getClassLoader());
                  value = Array.newInstance(clazz, 0);
                } catch (ClassNotFoundException e1) {
                  throw X_Debug.rethrow(e1);
                }
              } else if (X_Modifier.isAnnotation(cls.getModifier())) {
                modifier = X_Modifier.ANNOTATION;
                value = new Class[0];
                try {
                  Class<?> clazz = cls.toClass(service.getClassLoader());
                  value = Array.newInstance(clazz, 0);
                } catch (ClassNotFoundException e1) {
                  throw X_Debug.rethrow(e1);
                }
              } else if (cls.getQualifiedName().equals(String.class.getName())) {
                value = new String[0];
              } else {
                // Must be a class array
                value = new Class[0];
              }
            }
            this.value = new ImmutableAnnotationValue(cls.getQualifiedName(), value, modifier);
          } else {
            X_Log.error(getClass(), "Unable to load array member value type", node,"from",this.value,  e);
          }
          return;
        }
        type = node.getType();
      }
      for (MemberValue member : val) {
        toString.give(extract.extract((type = member), service, null));
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
