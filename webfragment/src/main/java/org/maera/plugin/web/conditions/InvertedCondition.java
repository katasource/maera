package org.maera.plugin.web.conditions;

import org.maera.plugin.PluginParseException;
import org.maera.plugin.web.Condition;

import java.util.Map;

public class InvertedCondition implements Condition {
    private Condition wrappedCondition;

    public InvertedCondition(Condition wrappedCondition) {
        this.wrappedCondition = wrappedCondition;
    }

    public void init(Map<String, String> params) throws PluginParseException {
    }

    public boolean shouldDisplay(Map<String, Object> context) {
        return !wrappedCondition.shouldDisplay(context);
    }
}
