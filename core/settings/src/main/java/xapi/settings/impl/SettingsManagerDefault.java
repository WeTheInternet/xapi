package xapi.settings.impl;

import net.wti.lang.parser.ast.expr.UiContainerExpr;
import xapi.settings.api.SettingsManager;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/17.
 */
public class SettingsManagerDefault extends AbstractSettingsManager {

    @Override
    protected SettingsManager createDelegate(String name, UiContainerExpr settings) {
        return new SettingsManagerDelegate(this, settings);
    }
}
