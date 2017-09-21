package xapi.demo.gwt;

import com.google.gwt.core.client.EntryPoint;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/3/17.
 */
public class XapiLangPresentation implements EntryPoint {

    public static void showBio() {

    }
    public static void goHome() {

    }

    @Override
    public void onModuleLoad() {
        hi();
    }

    private native void hi()
        /*-{ $wnd.alert('hi'); }-*/;
}
