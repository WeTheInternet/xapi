package xapi.dev.ui.tags.assembler;

import xapi.dev.source.PrintBuffer;

/**
 * Houses compile-time metadata for the assembly of a ui child node.
 *
 * Each element in a ui or shadow feature will have an individual assembly,
 * which will house the print buffer for filling in that element's construction,
 * as well as metadata about which features of the underlying tag are used
 * (so a separate class for each element can stuff state into an assembly).
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/19/18.
 */
public class ElementAssembly {

    private final String fieldName;
    private final PrintBuffer initMethod;

    public ElementAssembly(String fieldName, PrintBuffer initMethod) {
        this.fieldName = fieldName;
        this.initMethod = initMethod;
    }
}
