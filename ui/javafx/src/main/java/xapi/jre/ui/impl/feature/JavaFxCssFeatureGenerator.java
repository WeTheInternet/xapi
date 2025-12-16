package xapi.jre.ui.impl.feature;

import net.wti.lang.parser.ASTHelper;
import net.wti.lang.parser.ast.expr.CssContainerExpr;
import net.wti.lang.parser.ast.expr.CssRuleExpr;
import net.wti.lang.parser.ast.expr.CssSelectorExpr;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import xapi.collect.simple.SimpleStack;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.debug.NameGen;
import xapi.dev.source.PrintBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.StyleMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;
import xapi.jre.ui.css.Handler;
import xapi.source.X_Source;

import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/1/16.
 */
public class JavaFxCssFeatureGenerator extends UiFeatureGenerator {

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiAttrExpr attr
    ) {

        String panel = container.peekPanelName();
        final ClassBuffer cb = container.getSourceBuilder().getClassBuffer();
        final MethodBuffer mb = container.getMethod(panel);

        final StyleMetadata style = container.getStyle();

        for (String s : style.getClassNames()) {
            mb.println(panel+".getStyleClass().add(\"" + s + "\");");
        }

        final List<CssContainerExpr> rules = style.getRules();
        if (!rules.isEmpty()) {
            final String handler = mb.addImport(Handler.class);
            if (style.hasDynamicRules()) {

            } else {
                // static rules are nice; we can just generate String constants to use in stylesheets,
                // export them as actual .css files, and link to them with normal uris.

                final NameGen names = container.getNameGen();
                SimpleStack<String> items = new SimpleStack<>();
                SimpleStack<String> ruleVars = new SimpleStack<>();
                for (CssContainerExpr css : rules) {
                    for (CssSelectorExpr selector : css.getSelectors()) {
                        String name = names.newName("selector"+panel);
                        items.add(name);
                        cb.createField(String.class, name)
                            .setInitializer("\"" + X_Source.escape(selector.joinParts()) + "\"");
                    }
                    String selectors = items.join("+ \", \" +");
                    final String selectorName = names.newName("selectors"+panel);
                    cb.createField(String.class, selectorName)
                            .setInitializer(selectors);
                    items.clear();
                    String ruleName = names.newName("css" + panel);
                    for (CssRuleExpr rule : css.getRules()) {
                        String key = ASTHelper.extractStringValue(rule.getKey());
                        String value = ASTHelper.extractStringValue(rule.getValue());
                        key = renameForJavaFx(key);
                        String name = names.newName("rule" + panel);
                        items.add(name);
                        cb.createField(String.class, name)
                            .setInitializer("\"" + X_Source.escape(key + " : " + value + (value.endsWith(";") ? "" : ";")) + "\"");
                    }

                    final PrintBuffer out = cb.createField(String.class, ruleName).getInitializer();
                    out.println(selectorName + " + \"{\"+");
                    out.indent();
                    for (String item : items) {
                        out.println(item + " +");
                    }
                    items.clear();
                    out.outdent();
                    out.println("\"}\"");
                    ruleVars.add(ruleName);
                }

                String stylesheet = names.newName("stylesheet"+panel);
                String ruleset = ruleVars.join(" + ");
                cb.createField(String.class, stylesheet)
                    .setInitializer(handler+".registerStylesheet(\"" + stylesheet + "\", ()->" + ruleset+")");
                mb.println(panel+".getStylesheets().add(\"css:" + stylesheet+"\");");

            }
        }




        return super.startVisit(service, generator, source, container, attr);
    }

    private static String renameForJavaFx(String key) {
        switch (key.toLowerCase()) {
            case "color" :
                return "-fx-text-fill";
        }
        return "-fx-" + key;
    }
}
