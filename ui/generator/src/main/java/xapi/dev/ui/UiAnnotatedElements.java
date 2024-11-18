package xapi.dev.ui;

import xapi.ui.api.Ui;
import xapi.ui.api.UiField;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by james on 6/6/16.
 */
public class UiAnnotatedElements {
    Map<TypeElement, Ui> uiTypes;
    Map<ExecutableElement, Ui> uiMethods;
    Map<ExecutableElement, UiField> boundMethods;
    Map<VariableElement, UiField> boundFields;
    Set<ExecutableElement> boundParameters;

    public UiAnnotatedElements() {
        uiTypes = new LinkedHashMap<>();
        uiMethods = new LinkedHashMap<>();
        boundMethods = new LinkedHashMap<>();
        boundFields = new LinkedHashMap<>();
        boundParameters = new LinkedHashSet<>();
    }

    public void maybeAddType(TypeElement element) {
        final Ui typeUi = element.getAnnotation(Ui.class);
        if (typeUi != null) {
            uiTypes.put(element, typeUi);
        }
    }

    public void maybeAddVariable(VariableElement var) {
    }

    public void maybeAddExecutable(ExecutableElement exe) {
        final Ui exeUi = exe.getAnnotation(Ui.class);
        if (exeUi != null) {
            uiMethods.put(exe, exeUi);
        }
        // for now, we will ignore Gwt UiField annotations, and only handle our own
        // in the future, we can find any annotation named UiField, and adapt accordingly
        final UiField uiField = exe.getAnnotation(UiField.class);
        if (uiField != null) {
            boundMethods.put(exe, uiField);
        }

        boolean hadUnannotated = false;
        boolean hadAnnotated = false;
        for (VariableElement param : exe.getParameters()) {
            final UiField paramAnno = param.getAnnotation(UiField.class);
            if (paramAnno == null) {
                if (hadAnnotated) {
                    throwBadParameterAnnotations(exe);
                }
                hadUnannotated = true;
            } else {
                if (hadUnannotated) {
                    throwBadParameterAnnotations(exe);
                }
                hadAnnotated = true;
                boundParameters.add(exe);
            }
        }

    }

    public boolean hasAnyAnnotations() {
        return hasUiAnnotations() || hasBoundAnnotations();
    }

    public boolean hasUiAnnotations() {
        return !(uiTypes.isEmpty() && uiMethods.isEmpty());
    }

    public boolean hasBoundAnnotations() {
        return !(boundMethods.isEmpty() && boundFields.isEmpty() && boundParameters.isEmpty());
    }

    private void throwBadParameterAnnotations(ExecutableElement exe) {
        throw new IllegalStateException("ExecutableElement " + exe + " had bad parameter annotations; " +
                "Contains some but not all parameters with @UiField annotations. " + exe.getParameters());
    }

  @Override
  public String toString() {
    return "UiAnnotatedElements{" +
        "uiTypes=" + uiTypes +
        ", uiMethods=" + uiMethods +
        ", boundMethods=" + boundMethods +
        ", boundFields=" + boundFields +
        ", boundParameters=" + boundParameters +
        ", sAnyAnnotations=" + hasAnyAnnotations() +
        ", sUiAnnotations=" + hasUiAnnotations() +
        ", sBoundAnnotations=" + hasBoundAnnotations() +
        '}';
  }
}
