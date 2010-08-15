package org.maera.plugin.web.conditions;

import org.apache.commons.lang.exception.NestableException;

public class ConditionLoadingException extends NestableException {
    public ConditionLoadingException() {
    }

    public ConditionLoadingException(String string) {
        super(string);
    }

    public ConditionLoadingException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public ConditionLoadingException(Throwable throwable) {
        super(throwable);
    }
}
