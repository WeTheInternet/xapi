package xapi.test.elemental.bdd;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.expr.UiExpr;
import com.github.javaparser.ast.plugin.UiTransformer;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.sun.source.tree.CompilationUnitTree;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.elemental.ElementalTemplatePlugin;
import xapi.fu.Printable;
import xapi.io.X_IO;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.model.JavaDocument;
import xapi.util.X_String;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static xapi.fu.X_Fu.notEmpty;
import static xapi.util.X_String.join;

//import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 Simple example:

 Element e = `<div
 class = .styleName { left: 5px } \
 .styleName a { color: #579 }
 onClick = this::onClick
 onMouseOver = this.addClassName("hovered")
 onMouseOut = this::removeClassName, "hovered"
 ><a
 href = this::submit
 > Hello world! </a></div>`;


 *Consideration* for a polyglot syntax:
 *
 html:
 shadowRoot:
 div:
 class = "testing"
 onclick = this::onClick(*)
 onmouseover = js:
 alert(this.innerText);
 :js
 onfocus = java:
 this.focused = true;
 :java

 Hello from inside the shadow DOM
 (this div is what is actually rendered,
 with any tags inside the web component injected below)
 :content:
 :div
 :shadowRoot
 div:
 Hello from the external DOM.

 This element is the only one visible when inspecting the element,
 but it will actually be rendered inside the <content /> tag in the shadow root.
 :div
 :html
 java:
 class $X implements HasClickHandler {
 boolean focused = false;
 }
 :java
 js:
 function onCreated() {
 console.log("this will be automatically bound to web component lifecycle");
 }
 function focus() {
 this.controller.@$X::focused = true;
 }
 :js
 css:
 .testing {
 background-color: navy;
 color: white;
 }
 :css


 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/9/16.
 */
public class TemplateGeneratorSteps {

  private JavaDocument lastCompiled;

  private StringTo<Node> compiledByName = X_Collect.newStringMap(Node.class);

  @Before
  public void before() {

  }

  @After
  public void after() {

  }

  @Given("^compile template:$")
  public void compileTemplate(List<String> lines) throws Throwable {
    lastCompiled = compile(lines);
  }

  private JavaDocument compile(List<String> lines) throws ParseException {
    final Optional<String> first = lines.stream().filter(notEmpty()).findFirst();
    if (!first.isPresent()) {
      throw new AssertionError("No text supplied to compile method.  You sent "+lines);
    }
    final char start = first.get().charAt(0);
    switch (start) {
      case '<':
        // Use HTML parser
      case '{':
        // Use Java parser
      case '.':
      case '#':
        // Use CSS parser
      case '@':
        // Grab Processing instruction

      default :
        // Maybe Java references, css classes, or possibly just text
        try {
          final BlockStmt block = JavaParser.parseBlock(X_String.joinStrings(lines.toArray(new String[lines.size()])));

        } catch (Exception e) {

        }
    }
    final CompilationUnitTree unit = null;
    final JavacService service = null;
    final TypeElement type = null;

    JavaDocument doc = new JavaDocument(service, unit, type);
    // will throw exceptions...
    return doc;
  }

  @Given("^compile ui with name (\\S)+:$")
  public void compileTemplate(String name, List<String> lines) throws Throwable {
    String src = join("\n", lines);
    final UiContainerExpr ui = JavaParser.parseUiContainer(src);
    compiledByName.put(name, ui);
  }

  @Given("^compile component with name (\\S)+:$")
  public void compileComponent(String name, List<String> lines) throws Throwable {
    String src = join("\n", lines);
    final CompilationUnit ui = JavaParser.parse(X_IO.toStreamUtf8(src));
    compiledByName.put(name, ui);
  }

  @Then("^source code of (\\S)+ is:$")
  public void sourceCodeOfNameIs(String name, List<String> lines) throws Throwable {
    String expected = lines.stream()
        .map(line->line.replaceAll("\\n", "\\\\n"))
        .collect(Collectors.joining("\n"));
    Node ui = getNode(name);
    final String transformed;
    Printable p = Printable.newPrinter();
    if (ui instanceof UiContainerExpr) {
      new ElementalTemplatePlugin<>().transformUi(p, (UiExpr) ui);
      transformed = p.toSource();
    } else {
      UiTransformer t = new UiTransformer();
      t.setPlugin(new ElementalTemplatePlugin<>());
      transformed = ui.toSource(t);
    }
    assertThat("Expected\n: " + expected+"\n\nActual:\n" + transformed, transformed, equalToIgnoringWhiteSpace(expected));
  }

  private Node getNode(String name) {
    final Node node = compiledByName.get(name);
    assertNotNull("No node for name " + name, node);
    return node;
  }
}
