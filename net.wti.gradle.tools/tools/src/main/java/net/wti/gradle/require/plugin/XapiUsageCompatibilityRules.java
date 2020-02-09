package net.wti.gradle.require.plugin;

import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.attributes.Usage;

import javax.inject.Inject;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 19/10/19 @ 8:42 AM.
 */
public class XapiUsageCompatibilityRules implements AttributeCompatibilityRule<Usage> {

    private final Usage sourceJar;

    @Inject
    XapiUsageCompatibilityRules(Usage sourceJar) {
        this.sourceJar = sourceJar;

    }

    @Override
    public void execute(CompatibilityCheckDetails<Usage> dets) {
        if (dets.getConsumerValue() == null) {
            dets.compatible();
            return;
        }
        if (dets.getProducerValue() == null) {
            dets.compatible();
            return;
        }
        if (dets.getConsumerValue().equals(sourceJar)) {
            if (dets.getProducerValue().getName().endsWith("-jars")) {
                dets.compatible();
                return;
            }
            if (dets.getProducerValue().getName().startsWith("java-runtime")) {
                dets.compatible();
                return;
            }
        }
        if (dets.getConsumerValue().getName().equals("java-runtime")) {
            if (dets.getProducerValue().equals(sourceJar)) {
                dets.compatible();
                return;
            }
        }
        if (dets.getConsumerValue().getName().endsWith("-jars")) {
            if (dets.getProducerValue().equals(sourceJar)) {
                dets.compatible();
                return;
            }
        }
        // now, check that the consumer is a derivative of producer.

    }
}
