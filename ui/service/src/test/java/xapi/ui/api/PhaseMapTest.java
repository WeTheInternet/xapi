package xapi.ui.api;

import org.junit.Test;
import xapi.ui.api.PhaseMap.PhaseNode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/27/16.
 */
public class PhaseMapTest {

    @Test
    public void testSimplePhaseMap() {
        PhaseMap<String> phases = new PhaseMap<>();
        for (Class<?> cls : UiPhase.CORE_PHASES) {
            final UiPhase phase = cls.getAnnotation(UiPhase.class);
            phases.addNode(phase.id(), phase.priority(), phase.prerequisite(), phase.block());
        }
        List<String> nodes = StreamSupport.stream(phases.forEachNode().spliterator(), false)
              .map(PhaseNode::getId)
              .collect(Collectors.toList());
        assertThat(nodes)
              .containsExactly("preprocess", "supertype", "integration", "implementation", "binding");

        nodes = StreamSupport.stream(phases.forEachNodeReverse().spliterator(), false)
              .map(PhaseNode::getId)
              .collect(Collectors.toList());
        assertThat(nodes)
              .containsExactly(
                    "binding",
                    "implementation",
                    "integration",
                    "supertype",
                    "preprocess"
              );

    }
}
