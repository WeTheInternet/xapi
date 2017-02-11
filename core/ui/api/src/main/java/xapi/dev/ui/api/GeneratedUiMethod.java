package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.api.GeneratedUiImplementation.RequiredMethodType;
import xapi.fu.MappedIterable;
import xapi.fu.Out2;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiMethod extends GeneratedUiMember {

    private UiContainerExpr source;
    private RequiredMethodType type;
    private final StringTo<String> params;
    private ApiGeneratorContext<?> context;

    public GeneratedUiMethod(Type memberType, String memberName) {
        super(memberType, memberName);
        params = X_Collect.newStringMapInsertionOrdered(String.class);
    }

    public void setSource(UiContainerExpr source) {
        this.source = source;
    }

    public UiContainerExpr getSource() {
        return source;
    }

    public void setType(RequiredMethodType type) {
        this.type = type;
    }

    public RequiredMethodType getType() {
        return type;
    }

    public void addParam(String type, String paramName) {
        params.put(paramName, type);
    }

    public void setContext(ApiGeneratorContext<?> context) {
        this.context = context;
    }

    public ApiGeneratorContext<?> getContext() {
        return context;
    }

    public int getParamCount() {
        return params.size();
    }

    public MappedIterable<Out2<String, String>> getParams() {
        return params.forEachItem()
            .map(Out2::reverse);
    }
}
