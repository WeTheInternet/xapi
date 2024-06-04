package net.wti.gradle.settings.api;

/**
 * InvalidSettingsException:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 03/06/2024 @ 1:20 a.m.
 */
public class InvalidSettingsException extends RuntimeException {
    public InvalidSettingsException(final String message) {
        super(message);
    }

    public InvalidSettingsException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
