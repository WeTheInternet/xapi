package xapi.dev.ui;

import com.github.javaparser.ast.expr.CssContainerExpr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A place to put all of the extractable style information found per ui component
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/21/16.
 */
public class StyleMetadata {

    private List<CssContainerExpr> rules;
    private List<CssContainerExpr> applied;
    private Set<String> classNames;
    private boolean hasDynamicRules;

    public StyleMetadata() {
        classNames = new LinkedHashSet<>();
        rules = new ArrayList<>();
    }

    public boolean hasDynamicRules() {
        return hasDynamicRules;
    }

    public void setDynamicRules(boolean hasDynamicRules) {
        this.hasDynamicRules = hasDynamicRules;
    }

    public void addClassName(String cls) {
        classNames.add(cls);
    }

    public void addClassNames(String ... classes) {
        classNames.addAll(Arrays.asList(classes));
    }

    public void addClassNames(Iterable<String> classes) {
        classes.forEach(classNames::add);
    }

    public void addRules(Iterable<CssContainerExpr> cssRules) {
        if (cssRules instanceof Collection) {
            rules.addAll((Collection<CssContainerExpr>)cssRules);
        } else {
            cssRules.forEach(rules::add);
        }
    }

    public void addApplied(Iterable<CssContainerExpr> cssRules) {
        if (cssRules instanceof Collection) {
            applied.addAll((Collection<CssContainerExpr>)cssRules);
        } else {
            cssRules.forEach(applied::add);
        }
    }

    public Set<String> getClassNames() {
        return classNames;
    }

    public List<CssContainerExpr> getRules() {
        return rules;
    }
}
