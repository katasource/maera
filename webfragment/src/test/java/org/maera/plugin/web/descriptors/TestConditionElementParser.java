package org.maera.plugin.web.descriptors;

import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.web.Condition;
import org.mockito.Mockito;

public class TestConditionElementParser extends TestCase {
    private static final String TYPE_OR = "<conditions type=\"OR\">";
    private static final String TYPE_AND = "<conditions type=\"AND\">";
    private static final String TYPE_CLOSE = "</conditions>";
    private static final String FALSE = "<condition class=\"org.maera.plugin.web.conditions.NeverDisplayCondition\" />";
    private static final String TRUE = "<condition class=\"org.maera.plugin.web.conditions.AlwaysDisplayCondition\" />";
    private static final String NOT_FALSE = "<condition class=\"org.maera.plugin.web.conditions.NeverDisplayCondition\" invert=\"true\" />";
    private static final String NOT_TRUE = "<condition class=\"org.maera.plugin.web.conditions.AlwaysDisplayCondition\" invert=\"true\" />";

    private final ConditionElementParser conditionElementParser = new ConditionElementParser(new MockWebFragmentHelper());

    public void testSimple() throws DocumentException, PluginParseException {
        assertConditions(TRUE, true); //true
        assertConditions(FALSE, false); //false
        assertConditions(NOT_TRUE, false); //NOT true = false
        assertConditions(NOT_FALSE, true); //NOT false = true
    }

    public void testAnd() throws DocumentException, PluginParseException {
        //using the condition elements only (by default they are grouped with AND operator)
        assertConditions(TRUE + TRUE, true);//true AND true = true
        assertConditions(TRUE + FALSE, false);//true AND false = false
        assertConditions(FALSE + TRUE, false);//false AND true = false
        assertConditions(FALSE + FALSE, false);//false AND false = false

        //using the conditions element and explicitly specifying the AND operator
        assertConditions(TYPE_AND + TRUE + TRUE + TYPE_CLOSE, true);//true AND true = true
        assertConditions(TYPE_AND + TRUE + FALSE + TYPE_CLOSE, false);//true AND false = false
        assertConditions(TYPE_AND + FALSE + TRUE + TYPE_CLOSE, false);//false AND true = false
        assertConditions(TYPE_AND + FALSE + FALSE + TYPE_CLOSE, false);//false AND false = false
    }

    public void testOr() throws DocumentException, PluginParseException {
        assertConditions(TYPE_OR + TRUE + TRUE + TYPE_CLOSE, true);//true OR true = true
        assertConditions(TYPE_OR + TRUE + FALSE + TYPE_CLOSE, true);//true OR false = true
        assertConditions(TYPE_OR + FALSE + TRUE + TYPE_CLOSE, true);//false OR true = true
        assertConditions(TYPE_OR + FALSE + FALSE + TYPE_CLOSE, false);//false OR false = false
    }

    //test nested AND's - normally all nested AND's should be a single AND
    public void testNestedAnd() throws DocumentException, PluginParseException {
        //nested AND'ed conditions
        assertConditions(TYPE_AND +
                TYPE_AND + TRUE + TRUE + TYPE_CLOSE +
                TYPE_AND + TRUE + TRUE + TYPE_CLOSE +
                TYPE_CLOSE, true);//(true AND true) AND (true AND true) = true

        //same as above but without explicit AND
        assertConditions(TYPE_AND + TRUE + TRUE + TYPE_CLOSE +
                TYPE_AND + TRUE + TRUE + TYPE_CLOSE, true);//(true AND true) AND (true AND true) = true

        assertConditions(TYPE_AND +
                TYPE_AND + TRUE + TRUE + TYPE_CLOSE +
                TYPE_AND + FALSE + TRUE + TYPE_CLOSE +
                TYPE_CLOSE, false);//(true AND true) AND (false AND true) = false
    }

    //test nested OR's - normally all nested OR's should be a single OR
    public void testNestedOr() throws DocumentException, PluginParseException {
        //nested OR'ed conditions
        assertConditions(TYPE_OR +
                TYPE_OR + FALSE + FALSE + TYPE_CLOSE +
                TYPE_OR + FALSE + TRUE + TYPE_CLOSE +
                TYPE_CLOSE, true);//(false OR false) OR (false OR true) = true

        assertConditions(TYPE_OR +
                TYPE_OR + FALSE + FALSE + TYPE_CLOSE +
                TYPE_OR + FALSE + FALSE + TYPE_CLOSE +
                TYPE_CLOSE, false);//(false OR false) OR (false OR false) = false
    }

    public void testNestedMix() throws DocumentException, PluginParseException {
        //AND with nested OR conditions
        assertConditions(TYPE_AND +
                TYPE_OR + FALSE + FALSE + TYPE_CLOSE +
                TYPE_OR + FALSE + TRUE + TYPE_CLOSE +
                TYPE_CLOSE, false);//(false OR false) AND (false OR true) = false

        assertConditions(TYPE_AND +
                TYPE_OR + TRUE + FALSE + TYPE_CLOSE +
                TYPE_OR + FALSE + TRUE + TYPE_CLOSE +
                TYPE_CLOSE, true);//(true OR false) AND (false OR true) = true

        //OR with nested AND conditions
        assertConditions(TYPE_OR +
                TYPE_AND + FALSE + FALSE + TYPE_CLOSE +
                TYPE_AND + FALSE + TRUE + TYPE_CLOSE +
                TYPE_CLOSE, false);//(false AND false) OR (false AND true) = false

        assertConditions(TYPE_OR +
                TYPE_AND + FALSE + FALSE + TYPE_CLOSE +
                TYPE_AND + TRUE + TRUE + TYPE_CLOSE +
                TYPE_CLOSE, true);//(false AND false) OR (true AND true) = true
    }

    public void testComplex() throws DocumentException, PluginParseException {
        assertConditions(TRUE +
                TYPE_OR +
                TYPE_AND +
                FALSE + FALSE +
                TYPE_CLOSE +
                FALSE +
                TYPE_AND +
                TRUE +
                TYPE_OR +
                FALSE + TRUE +
                TYPE_CLOSE +
                TYPE_CLOSE +
                TYPE_CLOSE, true);//true AND ((false AND false) OR false OR (true AND (false OR true))) = true
    }

    public void assertConditions(String conditionElement, boolean expectedResult) throws DocumentException, PluginParseException {
        String rootElement = "<root>" + conditionElement + "</root>";
        Document document = DocumentHelper.parseText(rootElement);

        Condition condition = conditionElementParser.makeConditions(Mockito.mock(Plugin.class), document.getRootElement(), AbstractWebFragmentModuleDescriptor.COMPOSITE_TYPE_AND);

        assertEquals(expectedResult, condition.shouldDisplay(null));
    }
}
