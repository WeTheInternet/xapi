package xapi.settings.impl;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.fu.Immutable;
import xapi.settings.api.SettingsManager;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/17.
 */
public class SettingsManagerDelegate extends AbstractSettingsManager {

    private final SettingsManager parent;

    public SettingsManagerDelegate(SettingsManager parent, UiContainerExpr settings) {
        super(parent::settingsLocation, Immutable.immutable1(settings));
        this.parent = parent;
    }

    @Override
    public void saveSettings() {
        parent.saveSettings();
    }
}
