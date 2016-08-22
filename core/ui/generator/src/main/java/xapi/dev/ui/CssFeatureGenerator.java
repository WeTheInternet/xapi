package xapi.dev.ui;

import com.github.javaparser.ast.expr.*;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.fu.Lazy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class CssFeatureGenerator extends UiFeatureGenerator {

  @Override
  public UiVisitScope startVisit(
        UiGeneratorTools service, UiComponentGenerator generator, ContainerMetadata container, UiAttrExpr attr
  ) {
    final Expression value = attr.getExpression();
    boolean isClassAttr = "class".equalsIgnoreCase(attr.getNameString());
    boolean isStyleAttr = "style".equalsIgnoreCase(attr.getNameString());
    boolean isCssAttr = "css".equalsIgnoreCase(attr.getNameString());
    if (! (isClassAttr || isStyleAttr || isCssAttr ) ) {
      throw new IllegalArgumentException("Unsupported css feature name: " + attr.getNameString()+"; " +
          "Expected names are: class, style or css.  See CssFeatureGenerator for details.");
    }

    List<CssContainerExpr> containers = new ArrayList<>();
    if (value instanceof CssBlockExpr) {
      containers.addAll(((CssBlockExpr)value).getContainers());
    } else if (value instanceof CssContainerExpr) {
      containers.add((CssContainerExpr) value);
    } else if (value instanceof StringLiteralExpr) {
      container.getStyle().addClassNames(((StringLiteralExpr)value).getValue().trim().split("\\s+"));
    } else {
      throw new IllegalArgumentException("Cannot assign a node of type " + value.getClass() + " to a css feature; bad data: " + value);
    }

    boolean hasDynamicCss = checkForDynamism(containers);
    container.getStyle().setDynamicRules(hasDynamicCss);
    Set<CssSelectorExpr> selectors = extractSelectors(containers);
    if (isStyleAttr) {
      // style features do not allow selectors;
      // they are meant to be applied directly to elements.
      if (!selectors.isEmpty()) {
        throw new IllegalArgumentException("Cannot use css blocks that have selectors in a style feature;" +
            "\nExpected format is: `style=.{ color: blue; }`, not: " + containers);
      }
      // Now transform the css ast into commands that will apply style to an element.
      container.getStyle().addApplied(containers);
      return UiVisitScope.DEFAULT_CONTAINER;
    }

    if (isClassAttr) {
      // if class features have selectors, they must begin with a .class-name.
      Set<String> classes = new LinkedHashSet<>(container.getStyle().getClassNames());
      if (selectors.isEmpty()) {
        // no selector?  We'll generate a classname for you...
        if (!containers.isEmpty()) {
          String newCls = container.getNameGen().newClass();
          List<String> parts = Collections.singletonList("." + newCls);
          selectors.forEach(selector->selector.setParts(parts));
          classes.add(newCls);
        }
      } else {
        // If you specified selectors, they must all start with a .className part,
        // and all such classnames will be added to element (without any subparts).
        selectors.forEach(selector->{
          if (selector.getParts().isEmpty()) {
            String newCls = container.getNameGen().newClass();
            selector.setParts(Collections.singletonList("." + newCls));
            classes.add(newCls);
          } else {
            String part0 = selector.getParts().get(0);
            if (!part0.startsWith(".")) {
              throw new IllegalArgumentException("You cannot have a class= feature that contains css selectors " +
                  "which do not start with a .className.\nOffending selector: " + selector +
                  "\nOffending ui: " + container.getUi());
            }
            part0 = sliceClassName(part0);
            classes.add(part0);
          }
        });
      }
      // Alright, we have a stack of classnames that we want to set as this element's class attribute,
      // as well as a set of rules to add to a stylesheet (which we must import).

      final ClassBuffer cb = container.getSourceBuilder().getClassBuffer();
      String varName = container.newVarName(container.getRefName()+"Classes");
      String lazy = cb.addImport(Lazy.class);
      final PrintBuffer initializer = cb.createField(
          lazy + "<String[]>",
          varName
      ).getInitializer()
          .print(lazy)
          .println(".deferred1(()->new String[]{")
          .indent();
      String prefix = "";
      for (String cls : classes) {
        initializer
            .print(prefix)
            .print("\"" + cls + "\"");

        prefix = ", ";
      }
      initializer.println().outdent().println("})");

      // For now, we will just export one method with the classnames, and put the rules into the StyleMetadata
      // since end user implementations will likely want to transform the rules individually.
      container.getStyle().addClassNames(classes);
      container.getStyle().addRules(containers);

    } else {
      // A css = .{ } block of css rules; we'll just add them to the style metadata...
      container.getStyle().addRules(containers);
    }

    return UiVisitScope.DEFAULT_CONTAINER;
  }

  private boolean checkForDynamism(List<CssContainerExpr> containers) {
    for (CssContainerExpr container : containers) {
      for (CssRuleExpr rule : container.getRules()) {
        if (isDynamic(rule.getKey())) {
          return true;
        }
        if (isDynamic(rule.getValue())) {
          return true;
        }
      }

    }

    return false;
  }

  private boolean isDynamic(Expression key) {
    if (key instanceof LiteralExpr) {
      // TODO narrow literals that might contain dynamism (like json nodes, for example)
      return false;
    }
    if (key instanceof NameExpr) {
      return ((NameExpr)key).getName().startsWith("$");
    }
    return true;
  }

  private String sliceClassName(String name) {
    int i = 0;
    if (name.startsWith(".")) {
      name = name.substring(1);
    }
    while (i < name.length()) {
      char c = name.charAt(i);
      switch (c) {
        case '-':
        case '_':
          assert i > 0 : "Not a valid classname " + name;
          i++;
          break;
        default:
          if (Character.isLetterOrDigit(c)) {
            assert i > 0 || Character.isLetter(c) : "Not a valid classname " + name;
            i++;
          } else {
            assert i > 0 : "Not a valid classname " + name;
            // TODO validate that this is a valid css selector join character, like . > ~ + [ (
            assert c == '.' || c == '>' || c == '~' || c == '+' || c == '[' || c == '(' :
                "Not a valid selector: " + name;
            return name.substring(0, i);
          }
      }
    }
    return name;
  }

  private Set<CssSelectorExpr> extractSelectors(List<CssContainerExpr> containers) {
    Set<CssSelectorExpr> selectors = new LinkedHashSet<>();
    containers.stream()
              .map(CssContainerExpr::getSelectors)
              .flatMap(List::stream)
              .forEach(selectors::add);
    return selectors;
  }

}
