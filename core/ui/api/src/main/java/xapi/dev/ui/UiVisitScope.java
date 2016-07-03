package xapi.dev.ui;

import xapi.collect.api.StringTo;
import xapi.fu.In1;

import static xapi.collect.X_Collect.*;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/1/16.
 */
public class UiVisitScope {

    public enum ScopeType {
        CONTAINER, FEATURE
    }

    public static class UiVisitScopeImmutable extends UiVisitScope {
        public UiVisitScopeImmutable(ScopeType type) {
            super(type);
        }

        @Override
        protected <T> StringTo<T> newMap(Class<T> cls) {
            return collections().newStringMap(cls, IMMUTABLE);
        }

        @Override
        public UiVisitScope setVisitChildren(boolean visitChildren) {
            return this;
        }
    }

    @SuppressWarnings("all")
    public static final UiVisitScope DEFAULT_CONTAINER = new UiVisitScopeImmutable(ScopeType.CONTAINER);
    @SuppressWarnings("all")
    public static final UiVisitScope DEFAULT_FEATURE = new UiVisitScopeImmutable(ScopeType.FEATURE);
    private final ScopeType type;
    private boolean visitChildren;
    private StringTo<UiComponentGenerator> componentOverrides;
    private StringTo<UiFeatureGenerator> featureOverrides;
    private StringTo<Object> settings;

    public UiVisitScope(ScopeType type) {
        visitChildren = true;
        this.type = type;
        componentOverrides = newMap(UiComponentGenerator.class);
        featureOverrides = newMap(UiFeatureGenerator.class);
        settings = newMap(Object.class);
    }

    protected  <T> StringTo<T> newMap(Class<T> cls) {
        return newStringMap(cls);
    }

    public boolean isVisitChildren() {
        return visitChildren;
    }

    public UiVisitScope setVisitChildren(boolean visitChildren) {
        this.visitChildren = visitChildren;
        return this;
    }

    public StringTo<UiComponentGenerator> getComponentOverrides() {
        return componentOverrides;
    }

    public UiVisitScope viewComponentOverrides(In1<StringTo<UiComponentGenerator>> callback) {
        callback.in(componentOverrides);
        return this;
    }

    public StringTo<UiFeatureGenerator> getFeatureOverrides() {
        return featureOverrides;
    }

    public UiVisitScope viewFeatureOverrides(In1<StringTo<UiFeatureGenerator>> callback) {
        callback.in(featureOverrides);
        return this;
    }

    public StringTo<Object> getSettings() {
        return settings;
    }

    public UiVisitScope viewSettings(In1<StringTo<Object>> callback) {
        callback.in(settings);
        return this;
    }

    public ScopeType getType() {
        return type;
    }

    public static UiVisitScope visitScope(ScopeType type, boolean visitChildren) {
        return new UiVisitScope(type).setVisitChildren(visitChildren);
    }
}
