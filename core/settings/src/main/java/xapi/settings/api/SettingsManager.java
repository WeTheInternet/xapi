package xapi.settings.api;

import net.wti.lang.parser.ast.expr.*;
import xapi.except.NotYetImplemented;
import xapi.model.X_Model;
import xapi.model.api.Model;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/17.
 */
public interface SettingsManager {

    SettingsManager getApp(String name);

    UiContainerExpr getSettings();

    String getFileLocation();

    void addSettings(UiContainerExpr e);

    void setSettings(UiContainerExpr e);

    void saveSettings();

    String settingsLocation();

    default SettingsManager addModel(Model model) {
        return addModel(model.getType(), model);
    }
    default SettingsManager addModel(String key, Model model) {
        if (model == null) {
            return this;
        }
        String modelType = model.getType();
        UiContainerExpr out = new UiContainerExpr("model");

        out.addAttribute("type", NameExpr.of(modelType));

        if (model.getKey() != null) {
            final String keyString = X_Model.keyToString(model.getKey());
            out.addAttribute("key", StringLiteralExpr.stringLiteral(keyString));
        }

        for (String name : model.getPropertyNames()) {
            final Class<?> type = model.getPropertyType(name);
            final Object result = model.getProperty(name);
            addModelProperty(out, name, type, result);
        }

        return this;
    }

    default void addModelProperty(UiContainerExpr out, String name, Class<?> type, Object result) {
        if (type.isPrimitive()) {
            if (type == double.class || type == float.class) {
                out.addAttribute(name, DoubleLiteralExpr.doubleLiteral(((Number)result).doubleValue()));
            } else if (type == long.class) {
                out.addAttribute(name, LongLiteralExpr.longLiteral((Long)result));
            } else if (type == char.class) {
                out.addAttribute(name, CharLiteralExpr.charLiteral((Character)result));
            } else if (type == boolean.class) {
                out.addAttribute(name, BooleanLiteralExpr.boolLiteral((Boolean)result));
            } else if (type == byte.class || type == short.class || type == int.class) {
                out.addAttribute(name, IntegerLiteralExpr.intLiteral(((Number)result).intValue()));
            } else {
                throw new IllegalArgumentException("Type " + type + " not supported");
            }
        } else if (type == String.class){
            out.addAttribute(name, StringLiteralExpr.stringLiteral((String)result));
        } else {
            // we'll add more cases as things break
            throw new NotYetImplemented("Type " + type + " not yet supported");
        }
    }
}
