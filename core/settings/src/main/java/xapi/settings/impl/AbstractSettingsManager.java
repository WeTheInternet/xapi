package xapi.settings.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.JsonPairExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.Out1;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.settings.api.SettingsManager;
import xapi.util.X_Properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/17.
 */
public abstract class AbstractSettingsManager implements SettingsManager {

    private final Lazy<String> settingsHome;
    private Lazy<UiContainerExpr> settings;

    public AbstractSettingsManager() {
        settingsHome = Lazy.deferred1Unsafe(this::settingsLocation);
        settings = Lazy.deferred1Unsafe(this::loadSettings);
    }
    public AbstractSettingsManager(Out1<String> location, Out1<UiContainerExpr> expr) {
        settingsHome = Lazy.deferred1(location);
        settings = Lazy.deferred1(expr);
    }

    protected UiContainerExpr loadSettings() throws Exception {
        final File settingsFile = settingsFile();
        if (settingsFile.length() == 0) {
            return new UiContainerExpr("settings");
        }
        try (
            FileInputStream fin = new FileInputStream(settingsFile)
        ) {
            final UiContainerExpr result = JavaParser.parseXapi(fin);
            validate(result);
            return result;
        }
    }

    @Override
    public String settingsLocation() {
        return new File(X_Properties.getPropertyUnsafe("user.home", ()->new File("~").getCanonicalPath())
            , "xapi").getAbsolutePath();
    }

    private File settingsFile() {
        final File settingsFile = new File(settingsHome.out1(), "settings.xapi");
        if (!settingsFile.exists()) {
            final boolean result;
            try {
                result = settingsFile.createNewFile();
                if (!result) {
                    X_Log.warn(AbstractSettingsManager.class, "Cannot initialize settings.xapi in ", settingsFile);
                }
            } catch (IOException e) {
                X_Log.warn(AbstractSettingsManager.class, "Error saving settings.xapi in ", settingsFile, e);
            }
        }
        return settingsFile;
    }

    protected void validate(UiContainerExpr result) {
        // TODO: only allow known properties, or at least warn about unhandled items
        assert "settings".equals(result.getName()) : "Settings must be backed by <settings /> element. You sent " + result;
    }

    @Override
    public UiContainerExpr getSettings() {
        return settings.out1();
    }

    @Override
    public String getFileLocation() {
        return settingsHome.out1();
    }

    @Override
    public void addSettings(UiContainerExpr e) {
        validate(e);
        merge(settings.out1(), e);
    }

    @Override
    public SettingsManager getApp(String name) {
        final UiContainerExpr s = getSettings();
        JsonContainerExpr apps = (JsonContainerExpr) s.getAttribute("app")
            .ifAbsentSupply(()->{
                JsonContainerExpr app = new JsonContainerExpr(false, new ArrayList<>());
                final UiAttrExpr attr = s.addAttribute("app", app);
                return attr;
            })
            .getExpression();
        UiContainerExpr settings = (UiContainerExpr) apps.getNodeMaybe(name)
            .mapIfAbsent(()-> {
                UiContainerExpr app = new UiContainerExpr("settings");
                // TODO: use a lock to ensure atomicity here
                apps.getPairs().add(new JsonPairExpr(name, app));
                return app;
            })
            .get();

        SettingsManager delegate = createDelegate(name, settings);
        return delegate;
    }

    protected SettingsManager createDelegate(String name, UiContainerExpr settings) {
        return new SettingsManagerDelegate(this, settings);
    }

    protected void merge(UiContainerExpr into, UiContainerExpr from) {
        for (UiAttrExpr attr : from.getAttributes()) {
            final Maybe<UiAttrExpr> source = into.getAttribute(attr.getNameString());
            if (source.isPresent()) {
                // need to merge contents...
                merge(source.get(), attr);
            } else {
                // just set the from attr in the into
                into.addAttribute(true, attr);
            }
        }

    }

    protected void merge(UiAttrExpr into, UiAttrExpr from) {
        final Expression intoExpr = into.getExpression();
        final Expression fromExpr = from.getExpression();
        if (intoExpr.getClass() != fromExpr.getClass()) {
            throw new IllegalArgumentException("Cannot merge different value types;" +
                "\nprevious source:" + intoExpr +
                "\nnew source:" + fromExpr
            );
        }
        if (intoExpr instanceof UiContainerExpr) {
            merge((UiContainerExpr)intoExpr, (UiContainerExpr)fromExpr);
        } else if (intoExpr instanceof JsonContainerExpr){
            final JsonContainerExpr intoJson = (JsonContainerExpr) intoExpr;
            final JsonContainerExpr fromJson = (JsonContainerExpr) fromExpr;
            if (intoJson.isArray() != fromJson.isArray()) {
                throw new IllegalArgumentException("Cannot merge json nodes of different type; " +
                    "\n" + intoExpr +
                    "\n" + fromExpr);
            }
            intoJson.getPairs().addAll(fromJson.getPairs());
        } else {
            throw new UnsupportedOperationException("Cannot merge " + into + " and " + from);
        }
    }

    @Override
    public void setSettings(UiContainerExpr e) {
        validate(e);
        settings = Lazy.immutable1(e);
    }

    @Override
    public void saveSettings() {
        final String source = settings.out1().toSource();
        try (
            FileOutputStream fout = new FileOutputStream(settingsFile())
        ) {
            X_IO.drain(fout, X_IO.toStreamUtf8(source));
        } catch (IOException e) {
            X_Log.error(AbstractSettingsManager.class, "Error saving settings", e);
        }
    }

}
