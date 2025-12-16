package xapi.demo.gwt.client;

import net.wti.lang.parser.JavaParser;
import net.wti.lang.parser.ParseException;
import net.wti.lang.parser.ast.exception.NotFoundException;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.components.impl.JsSupport;
import xapi.components.impl.WebComponentSupport;
import xapi.demo.gwt.client.resources.DemoResources;
import xapi.demo.gwt.client.resources.DemoUiConfig;
import xapi.demo.gwt.client.ui.GwtXapiSlideComponent;
import xapi.demo.gwt.client.ui.GwtXapiSlidesComponent;
import xapi.elemental.X_Elemental;
import xapi.log.X_Log;
import xapi.model.api.ModelKey;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.resources.client.TextResource;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/3/17.
 */
public class XapiLangPresentation implements EntryPoint {

    private final StringTo<UiContainerExpr> content;
    private static XapiLangPresentation presentation;

    public XapiLangPresentation() {
        presentation = this;
        content = X_Collect.newStringMap(UiContainerExpr.class);
    }

    public static void showBio() {

    }
    public static void goHome() {

    }

    public static void openSibling(Object reference) {

    }

    public static void openSlide(ModelKey next) {
    }

    public static UiContainerExpr getSlide(String key) {
        return presentation.content.getOrCreate(key, k->{
            throw new NotFoundException(key);
        });
    }

    @Override
    public void onModuleLoad() {
        DemoResources res = GWT.create(DemoResources.class);
        save("home", res.getHome());
        Scheduler.get().scheduleDeferred(()->{
            save("Declarative", res.getDeclarative());
            save("-lang", res.getLang());
        });
        new WebComponentSupport()
            .ensureWebComponentApi(()->{
                DemoUiConfig assembler = new DemoUiConfig(X_Elemental.getElementalService(), res);
                GwtXapiSlidesComponent.assemble(assembler);
                GwtXapiSlideComponent.assemble(assembler);
            });

        JsSupport.console().log("Hi???!!");
    }

    private void save(String id, TextResource text) {
        try {
            content.put(id, JavaParser.parseUiContainer(text.getText()));
        } catch (ParseException e) {
            X_Log.error(XapiLangPresentation.class, "Cannot parse home:", text.getText(), e);
        }
    }

}
