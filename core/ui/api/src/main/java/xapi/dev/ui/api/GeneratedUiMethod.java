package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.api.GeneratedUiImplementation.RequiredMethodType;
import xapi.fu.In2Out1;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Out2;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiMethod extends GeneratedUiMember {

    private final In2Out1<GeneratedUiLayer, UiContainerExpr, String> implFactory;
    private UiContainerExpr source;
    private RequiredMethodType type;
    private final StringTo<String> params;
    private ApiGeneratorContext<?> context;

    public GeneratedUiMethod(Type memberType, String memberName) {
        this(memberType, memberName, (ig, nored)->memberType.toSource());
    }

    public GeneratedUiMethod(Type memberType, String memberName, In2Out1<GeneratedUiLayer, UiContainerExpr, String> implFactory) {
        super(memberType, memberName);
        params = X_Collect.newStringMapInsertionOrdered(String.class);
        this.implFactory = implFactory;
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

    /**
     * @return A mappable iterable of [type, name, type, name, ...] pairs.
     */
    public MappedIterable<Out2<String, String>> getParams() {
        return params.forEachItem()
            // params are stored as a name->type relationship,
            // but when viewing them, it is natural to expect type then name
            // so we reverse the iterable
            .map(Out2::reverse);
    }

    public String toSignature(GeneratedUiLayer in, UiContainerExpr ui) {
        final String retType;
        if (in == null) {
            retType = getMemberType().toSource();
        } else {
            retType = implFactory.io(in, ui);
        }
        return "public " + retType + " " + getMemberName() +"(" +
            getParams()
                .map2(Out2::join, " ") // space between Type and name
                .join(",") // commas between arg pairs
            +")"
            ;

    }
    public final String toSignature() {
        return toSignature(null, null);
    }
}
