package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

/**
 * A template literal is a `tick delimited section of text
 * in which you can preserve whitespace,
 * the only escaped sequence is \`
 * and it ends at the next undelimited tick`.
 *
 * If no extra template plugins are installed,
 * all template literals will act like strings in which you don't have to encode things.
 *
 * More advanced features will be embedded as configurable plugins, with features like:
 *
 * `text with $scopedReferences or ${inlineCode()} are pending`
 * prints java code:
 * "text with " + scopedReferences + " or " + (inlineCode()) + " are pending"
 *
 * `js: { dictionary: values.apply(this, []) } :js`
 * prints java code:
 * GWT.jso("dictionary", values.run());
 *
 * // GWT.jso is a psuedocode-only method; an XApi json lib / wrapper can be provided
 *
 * `{ javaMethod(this, ()->\`an embedded template scope
 * which is not $resolved when the outer template is\`, $resolved`) }`,
 * prints java code:
 * javaMethod(this, ()->"an embedded template scope\n" +
 * "which is not " + resolved + " when the out template is", resolved)
 *
 * // could also use `java: javaMethod(...) :java`, but `{ can(Be.Shorthand,For.Java()) }`
 *
 * Element e = `<div class={this::className} />` // creates an element with classname bound to this.className
 * // Installing a plugin (rebind) for an assigned type of Element,
 * // we can use the javac compiler diagnostics to find the expected type assignment via magic method injection:
 * prints java code:
 * Element e = X_Ui.magicElement("<div class={this::className} />");
 * which the javac compiler plugin transforms into:
 * Element e = element("div")
 *    .className(this::getClassName, this::setClassName)
 *    .build(); // or whatever means you want for creating elements.
 *
 *
 * Because the actual semantics of transforming a template into a type should be dynamic,
 * this will be done by implementing a TemplateParsingPlugin,
 * which allows you to define (or override) the handling for a given prefix + suffix of templates,
 * `go: fmt.Println(stringutil.Reverse("!dlroW ,olleH")) :go` // not for running in java code
 * or (eventually) for more complex rebinding rules, like "what type am I assigned to?",
 * "what kind of scope am I running in?", or "what type did you supply as a compile time literal?"
 *
 * Toy example for these features might look like:
 *
 * // Create a binding rule that templates assigned to MyClass are generated with MyGenerator:
 * @XApi(
 *  bindRule = {
 *    @BindRule(
 *      assignedTo: MyClass.class,
 *      generator: @Reference(MyGenerator.class)
 *    )
 *  }
 * )
 * class Test {
 *   static final Class&lt;Test> CLASS = Test.class;
 *   MyClass injected = `some kind of meaningful compile-time value that might want to use $CLASS variable reference`;
 * }
 * class MyGenerator implements TemplateValueResolver&lt;MyClass>{
 *   String generate(Context context, TemplateLiteralExpr template) {
 *     TypeMirror type = context.usefulService().findCompileTimeType(template, "CLASS");
 *     // finds the compile-time type of Test.class
 *     String sourceName = context.import(type);
 *     return `new MyClass($sourceName.class)`; // why not? :-)
 *   }
 * }
 *
 * // what kind of scope am I running in?
 * @XApi(
 *   bindRule = {
 *     @BindRule(
 *       assignedTo: MyClass.class,
 *       inScope: GlobalScope.class,
 *       provider: NewInstanceProvider.class
 *     ),
 *     @BindRule(
 *       assignedTo: MyClass.class,
 *       inScope: RequestScope.class,
 *       provider: FromScope.class
 *     )
 *   }
 * )
 * class Test {
 *
 *   <Raw, Full extends Raw> void runTest(GlobalScope inGlobalScope, Class<Full> cls) {
 *     MyClass global = `$cls`; // turns into new MyClass(cls)
 *     inGlobalScope.runInScope(RequestScope.class, requestScope->{
 *       MyClass request = `$cls,$global`; // turns into:
 *       // request = requestScope.newInstance(MyClass.class, new Class[]{ MyClass.class, Class.class }, global, cls);
 *       // which is effectively a cached, request-scope instance that will either be created with these parameters,
 *       // or if there is an onRead() method with matching parameters, then that method will be
 *       // called whenever an existing instance is read from scope.
 *     });
 *   }
 *
 *   class MyClass <T> {
 *     MyClass(Class<T> type) {
 *       ...
 *     }
 *
 *     MyClass(MyClass parent, Class<T> type) {
 *       this(type);
 *       ...
 *     }
 *
 *   }
 * }
 *
 *
 * TL;DR: All this class currently does is make `tick delimited strings
 * valid java strings without any encoding of special characters, except for \`
 * that is only terminated when a final tick` appears.
 *
 *
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/19/16.
 */
public class TemplateLiteralExpr extends UiExpr {

  private String value;

  public TemplateLiteralExpr(final int beginLine, final int beginColumn, final int endLine, final int endColumn,
                             final String value) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.value = value;
  }

  @Override public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
    return v.visit(this, arg);
  }

  @Override public <A> void accept(final VoidVisitor<A> v, final A arg) {
    v.visit(this, arg);
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getValueWithoutTicks() {
    if (value.startsWith("`")) {
      return value.substring(1, value.length() -1);
    }
    return value;
  }

  public String getValueWithTicks() {
    if (!value.startsWith("`")) {
      return '`' + value + "`";
    }
    return value;
  }
}
