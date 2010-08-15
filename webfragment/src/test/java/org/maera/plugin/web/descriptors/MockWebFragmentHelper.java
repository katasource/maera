package org.maera.plugin.web.descriptors;

import org.maera.plugin.Plugin;
import org.maera.plugin.web.Condition;
import org.maera.plugin.web.ContextProvider;
import org.maera.plugin.web.WebFragmentHelper;
import org.maera.plugin.web.conditions.AlwaysDisplayCondition;
import org.maera.plugin.web.conditions.ConditionLoadingException;
import org.maera.plugin.web.conditions.NeverDisplayCondition;

import java.util.List;
import java.util.Map;

public class MockWebFragmentHelper implements WebFragmentHelper {
    public Condition loadCondition(String className, Plugin plugin) throws ConditionLoadingException {
        if (className.indexOf("AlwaysDisplayCondition") != -1) {
            return new AlwaysDisplayCondition();
        } else {
            return new NeverDisplayCondition();
        }
    }

    public ContextProvider loadContextProvider(String className, Plugin plugin) throws ConditionLoadingException {
        return null;
    }

    public String getI18nValue(String key, List arguments, Map context) {
        return null;
    }

    public String renderVelocityFragment(String fragment, Map context) {
        return null;
    }
}
