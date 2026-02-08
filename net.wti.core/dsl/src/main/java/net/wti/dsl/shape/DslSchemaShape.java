package net.wti.dsl.shape;

import java.util.Map;
import java.util.Set;

///
/// DslSchemaShape:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/12/2025 @ 02:12
public final class DslSchemaShape {
    public final String rootElement;
    public final Map<String, Set<String>> elements;


//    private final String dslName;
//    private final String packageName;
//    private final Map<String, ElementShape> elementsByName;


    public DslSchemaShape(String rootElement, Map<String, Set<String>> elements) {
        this.rootElement = rootElement;
        this.elements = elements;
//        this.dslName = dslName;
//        this.packageName = packageName;
//        this.elementsByName = elementsByName;
    }
}
