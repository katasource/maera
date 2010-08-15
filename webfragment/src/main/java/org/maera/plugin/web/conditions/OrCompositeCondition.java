package org.maera.plugin.web.conditions;

import org.maera.plugin.web.Condition;

import java.util.Map;

public class OrCompositeCondition extends AbstractCompositeCondition {
    public boolean shouldDisplay(Map<String, Object> context) {
        for (Condition condition : conditions) {
            if (condition.shouldDisplay(context))
                return true;
        }

        return false;
    }
}
