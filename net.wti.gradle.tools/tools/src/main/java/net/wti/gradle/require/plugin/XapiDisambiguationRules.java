package net.wti.gradle.require.plugin;

import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.MultipleCandidatesDetails;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 19/10/19 @ 8:44 AM.
 */
public class XapiDisambiguationRules implements AttributeDisambiguationRule<String> {
    @Override
    public void execute(MultipleCandidatesDetails<String> dets) {
        if (dets.getConsumerValue() == null) {
            for (String candidateValue : dets.getCandidateValues()) {
                dets.closestMatch(candidateValue);
            }
            return;
        }
        String notSource = null;
        boolean multipleNotSource = false;
        for (String candidateValue : dets.getCandidateValues()) {
            if (dets.getConsumerValue().equals(candidateValue)) {
                dets.closestMatch(candidateValue);
            }
            if (!candidateValue.contains("source")) {
                if (notSource == null) {
                    notSource = candidateValue;
                } else {
                    multipleNotSource = false;
                }
            }
        }
        if (notSource != null && !multipleNotSource) {
            dets.closestMatch(notSource);
        }

    }
}
