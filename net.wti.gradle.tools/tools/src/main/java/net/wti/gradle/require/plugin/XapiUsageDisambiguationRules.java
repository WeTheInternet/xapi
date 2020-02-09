package net.wti.gradle.require.plugin;

import net.wti.gradle.internal.api.XapiUsage;
import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.MultipleCandidatesDetails;
import org.gradle.api.attributes.Usage;

import javax.inject.Inject;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 19/10/19 @ 8:42 AM.
 */
public class XapiUsageDisambiguationRules implements AttributeDisambiguationRule<Usage> {

    private final Usage sourceJar;

    @Inject
    XapiUsageDisambiguationRules(Usage sourceJar) {
        this.sourceJar = sourceJar;

    }

    @Override
    public void execute(MultipleCandidatesDetails<Usage> candidates) {
        String consumer = candidates.getConsumerValue().getName();
        boolean consumingSource = consumer.equals(XapiUsage.SOURCE_JAR) || consumer.equals(XapiUsage.SOURCE);
        if (consumingSource) {
            for (Usage candidateValue : candidates.getCandidateValues()) {
                if (candidateValue.getName().equals(XapiUsage.SOURCE_JAR)) {
                    candidates.closestMatch(candidateValue);
                    return;
                }
                if (candidateValue.getName().equals(XapiUsage.SOURCE)) {
                    candidates.closestMatch(candidateValue);
                    return;
                }
            }
        } else {
//            Usage soFar = null;
//            boolean multiple = false;
//            for (Usage candidateValue : candidates.getCandidateValues()) {
//                if (!candidateValue.getName().startsWith(XapiUsage.SOURCE_PREFIX)) {
//                    multiple = soFar == null;
//                    soFar = candidateValue;
//                    candidates.closestMatch(soFar);
//                    return;
//                }
//            }
//            if (soFar != null && !multiple) {
//                candidates.closestMatch(soFar);
//                return;
//            }
        }

    }
}
