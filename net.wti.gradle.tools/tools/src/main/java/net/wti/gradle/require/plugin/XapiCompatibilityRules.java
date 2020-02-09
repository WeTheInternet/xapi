package net.wti.gradle.require.plugin;

import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 19/10/19 @ 8:42 AM.
 */
public class XapiCompatibilityRules implements AttributeCompatibilityRule<String> {

    @Override
    public void execute(CompatibilityCheckDetails<String> dets) {
        if (dets.getConsumerValue() == null) {
            dets.compatible();
            return;
        }
        if (dets.getProducerValue() == null) {
            dets.compatible();
            return;
        }
        if (dets.getConsumerValue().equals(dets.getProducerValue())) {
            dets.compatible();
            return;
        }
        // now, check that the consumer is a derivative of producer.

    }
}
