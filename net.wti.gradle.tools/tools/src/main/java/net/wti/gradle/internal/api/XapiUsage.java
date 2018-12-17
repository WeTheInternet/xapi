package net.wti.gradle.internal.api;

import org.gradle.api.attributes.Usage;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/10/18 @ 2:29 AM.
 */
public interface XapiUsage extends Usage {
    String API = "xapi-api";
    String API_SOURCE = "xapi-api-source";
    String API_TEST = "xapi-api-test";
    String API_TEST_SOURCE = "xapi-api-test-source";

    String SPI = "xapi-spi";
    String SPI_SOURCE = "xapi-spi-source";
    String SPI_TEST = "xapi-spi-test";
    String SPI_TEST_SOURCE = "xapi-spi-test-source";

    String MAIN = "xapi-main";
    String MAIN_SOURCE = "xapi-spi-source";
    String MAIN_TEST = "xapi-test";
    String MAIN_TEST_SOURCE = "xapi-test-source";

}
