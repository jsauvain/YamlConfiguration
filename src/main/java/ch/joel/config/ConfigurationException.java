package ch.joel.config;

import java.util.Collection;

public abstract class ConfigurationException extends RuntimeException {
    protected static final String NEWLINE = String.format("%n");

    private final Collection<String> errors;


    public ConfigurationException(String path, Collection<String> errors) {
        super(formatMessage(path, errors));
        this.errors = errors;
    }
    public ConfigurationException(String path, Collection<String> errors, Throwable cause) {
        super(formatMessage(path, errors), cause);
        this.errors = errors;
    }

    public Collection<String> getErrors() {
        return errors;
    }

    protected static String formatMessage(String file, Collection<String> errors) {
        final StringBuilder msg = new StringBuilder(file);
        msg.append(errors.size() == 1 ? " has an error:" : " has the following errors:").append(NEWLINE);
        for (String error : errors) {
            msg.append("  * ").append(error).append(NEWLINE);
        }
        return msg.toString();
    }
}
