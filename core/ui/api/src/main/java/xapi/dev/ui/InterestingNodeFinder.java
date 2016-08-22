package xapi.dev.ui;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.expr.UiExpr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/28/16.
 */
public class InterestingNodeFinder {
    public static class InterestingNodeResults {
        private Map<ComponentGraph, UiAttrExpr> dataNodes;
        private Map<ComponentGraph, UiAttrExpr> refNodes;
        private Map<ComponentGraph, UiExpr> cssNodes;
        private boolean hasClassname;
        private Map<ComponentGraph, List<NameExpr>> templateNames;

        public InterestingNodeResults() {
            dataNodes = new IdentityHashMap<>();
            refNodes = new IdentityHashMap<>();
            templateNames = new IdentityHashMap<>();
            cssNodes = new IdentityHashMap<>();
        }

        public void addTemplateName(ComponentGraph g, NameExpr n) {
            templateNames.computeIfAbsent(g, i -> new ArrayList<>())
                  .add(n);
        }

        public void addRefNode(ComponentGraph g, UiAttrExpr n) {
            refNodes.put(g, n);
        }

        public void addDataNode(ComponentGraph g, UiAttrExpr n) {
            dataNodes.put(g, n);
        }

        public void addCssNode(ComponentGraph g, UiExpr n) {
            cssNodes.put(g, n);
        }

        public void foundClassnameFeature() {
            hasClassname = true;
        }

        public boolean hasDataNodes() {
            return !dataNodes.isEmpty();
        }

        public boolean hasCssNodes() {
            return !cssNodes.isEmpty();
        }

        public boolean hasCssOrClassname() {
            return !cssNodes.isEmpty() || hasClassname;
        }

        public boolean hasTemplateReferences() {
            return !templateNames.isEmpty();
        }

        public Map<ComponentGraph, UiAttrExpr> getDataNodes() {
            return dataNodes;
        }

        public Map<ComponentGraph, UiAttrExpr> getRefNodes() {
            return refNodes;
        }

        public Map<ComponentGraph, UiExpr> getCssNodes() {
            return cssNodes;
        }

        public boolean hasClassname() {
            return hasClassname;
        }

        public Map<ComponentGraph, List<NameExpr>> getTemplateNames() {
            return templateNames;
        }

        public Set<UiContainerExpr> getDataParents() {
            if (!hasDataNodes()) {
                return Collections.emptySet();
            }
            Map<UiContainerExpr, Boolean> map = new IdentityHashMap<>();
            dataNodes.keySet().forEach(containerFinder(map));
            return map.keySet();
        }

        public Set<UiContainerExpr> getCssParents() {
            if (!hasCssNodes()) {
                return Collections.emptySet();
            }
            Map<UiContainerExpr, Boolean> map = new IdentityHashMap<>();
            cssNodes.keySet().forEach(containerFinder(map));
            return map.keySet();
        }

        public Set<UiContainerExpr> getTemplateNameParents() {
            if (!hasTemplateReferences()) {
                return Collections.emptySet();
            }
            Map<UiContainerExpr, Boolean> map = new IdentityHashMap<>();
            templateNames.keySet().forEach(containerFinder(map));
            return map.keySet();
        }

        private Consumer<? super ComponentGraph> containerFinder(Map<UiContainerExpr, Boolean> map) {
            return n -> {
                UiContainerExpr c = n.getDeepestContainer();
                while (c != null) {
                    if (map.get(c) != null) {
                        break;
                    }
                    map.put(c, true);
                    c = ASTHelper.getContainerParent(c);
                }
            };
        }
    }

    public InterestingNodeResults findInterestingNodes(UiContainerExpr container) {

        InterestingNodeResults results = new InterestingNodeResults();

        ComponentMetadataFinder finder = new ComponentMetadataFinder();

        final ComponentMetadataQuery query = new ComponentMetadataQuery();
        query
              .setVisitChildContainers(true)
              .setVisitAttributeContainers(true)
              .addDataFeatureListener(results::addDataNode)
              .addRefFeatureListener(results::addRefNode)
              .addCssFeatureListener(results::addCssNode)
              .addAllFeatureListener((scope, attr) ->{
                  if ("class".equalsIgnoreCase(attr.getNameString())) {
                      results.foundClassnameFeature();
                      results.addCssNode(scope, attr);
                  }
              })
              .addNameListener((g, n) -> {
                  if (query.isTemplateName(n.getName())) {
                      results.addTemplateName(g, n);
                  }
              });

        container.accept(finder, query);
        return results;
    }
}
