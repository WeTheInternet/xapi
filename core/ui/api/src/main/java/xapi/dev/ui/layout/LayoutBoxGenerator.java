package xapi.dev.ui.layout;

import xapi.ui.layout.HasDefaultSlot;

/**
 * Flagship hand-written layout generator.
 *
 * As simple as <box /> may seem, we want to expose things like alignment,
 * which may actually require complex / different / conditional node
 * structures in different runtimes.
 *
 * This class will be one big hack until those hacks become a tested, well used API.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 6/11/17.
 */
public class LayoutBoxGenerator implements LayoutGenerator, HasDefaultSlotGenerator {

    @Override
    public String getTagName() {
        return "box"; // user-generated tag names, we might enforce - to be used
    }
}
