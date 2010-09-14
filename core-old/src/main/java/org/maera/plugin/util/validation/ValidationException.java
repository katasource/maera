package org.maera.plugin.util.validation;

import org.apache.commons.lang.Validate;
import org.maera.plugin.PluginParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception for a validation error parsing DOM4J nodes
 *
 * @since 2.2.0
 */
public class ValidationException extends PluginParseException {
    private final List<String> errors;

    public ValidationException(String msg, List<String> errors) {
        super(msg);

        Validate.notNull(errors);
        this.errors = Collections.unmodifiableList(new ArrayList<String>(errors));
    }

    /**
     * @return a list of the original errors
     */
    public List<String> getErrors() {
        return errors;
    }
}
