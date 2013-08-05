package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.dev.jjs.MagicMethodGenerator;

public class DeclaredMethodInjector extends AbstractMethodInjector implements MagicMethodGenerator {

  @Override
  protected boolean isDeclared() {
    return true;
  }
  
  //  @Override
//  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall,
//      JMethod enclosingMethod, Context context, UnifyAstView ast)
//      throws UnableToCompleteException {
//    boolean isFromGwtReflect = methodCall.getArgs().size() == 3;
//    JExpression inst = isFromGwtReflect ? methodCall.getArgs().get(0) : methodCall.getInstance();
//    JClassLiteral classLit = extractClassLiteral(logger, inst, false);
//    List<JExpression> args = methodCall.getArgs();
//    JExpression arg0 = args.get(isFromGwtReflect?1:0), arg1 = args.get(isFromGwtReflect?2:1);
//    JStringLiteral stringLit = extractImmutableNode(logger,
//        JStringLiteral.class, arg0, false);
//    JNewArray newArray = extractImmutableNode(logger, JNewArray.class, arg1,
//        false);
//    if (classLit == null) {
//      if (logger.isLoggable(Type.DEBUG))
//        logger.log(Type.DEBUG,
//            "Non-final class literal used to invoke reflection method; "
//                + debug(methodCall.getInstance()));
//    } else if (stringLit == null) {
//      if (logger.isLoggable(Type.DEBUG))
//        logger.log(Type.DEBUG,
//            "Non-final string arg used to retrieve reflection method; "
//                + debug(arg0));
//    } else if (newArray == null) {
//      if (logger.isLoggable(Type.DEBUG))
//        logger.log(Type.DEBUG,
//            "Non-final array arg used to retrieve reflection method; "
//                + debug(arg1));
//    } else {
//      String name = stringLit.getValue();
//      ArrayList<JType> params = new ArrayList<JType>(); 
//      for (JExpression expr : newArray.initializers) {
//        JClassLiteral type = extractClassLiteral(logger, expr, false);
//        if (type == null) {
//          
//          for (JField field : methodCall.getTarget().getEnclosingType().getFields()) {
//            if (field.getName().equals("members")) {
//              for (JMethod memberPoolMethod : field.getEnclosingType().getMethods()) {
//                if (memberPoolMethod.getName().equals("getDeclaredMethod")) {
//                  
//                  JFieldRef ref = new JFieldRef(memberPoolMethod.getSourceInfo(), methodCall.getInstance(), field, ast.getProgram().getTypeJavaLangClass());
//                  methodCall = new JMethodCall(methodCall.getSourceInfo(), ref, memberPoolMethod);
//                  methodCall.addArg(arg0);
//                  methodCall.addArg(arg1);
//                  return methodCall.makeStatement().getExpr();
//                  
//                }
//              }
//            }
//          }
//          
//        } else {
//          params.add(type.getRefType());
//        }
//      } 
//       // We got all our literals; the class, method name and parameter classes
//      JDeclaredType selfType = ast.searchForTypeByBinary(classLit.getRefType().getName());
//      JMethod method; // get the requested method
//      if (selfType instanceof JInterfaceType) {
//        JInterfaceType iface = (JInterfaceType) selfType;
//        method = findMethod(iface, name, params);
//      } else {
//        JClassType cls = (JClassType) selfType;
//        method = findMethod(cls, name, params);
//      }
//      if (method == null) {
//        logger.log(Type.ERROR, "Unable to find method " + selfType.getName()+"."+name+ "("+params+")");
//        throw new UnableToCompleteException();
//      }
//      if (logger.isLoggable(Type.TRACE)) {
//        logger.log(Type.TRACE, "Found method " + method);
//      }
//      // now, get or make a handle to the requested method,
//      JMethodCall methodFactory = getMethodFactory(logger, ast, method, classLit, true);
//      // and return a call to the generated Method provider
//      return methodFactory.makeStatement().getExpr();
//    }
//    for (JField field : ast.getProgram().getTypeJavaLangClass().getFields()) {
//      if (field.getName().equals("members")) {
//        JDeclaredType result = ast.searchForTypeByBinary(field.getType().getName());
//        for (JMethod memberPoolMethod : result.getMethods()) {
//          if (memberPoolMethod.getName().equals("getDeclaredMethod")) {
//            JFieldRef ref = new JFieldRef(memberPoolMethod.getSourceInfo(), inst, field, ast.getProgram().getTypeJavaLangClass());
//            methodCall = new JMethodCall(memberPoolMethod.getSourceInfo(), ref, memberPoolMethod);
//            methodCall.addArg(arg0);
//            methodCall.addArg(arg1);
//            return methodCall.makeStatement().getExpr();
//          }
//        }
//      }
//    }
//    throw new UnableToCompleteException();
//  }

}
