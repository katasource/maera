package org.maera.plugin.loaders;

import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import java.util.Map;

public class TestLoaderUtils extends TestCase {
    public void testMultipleParameters() throws DocumentException {
        Document document = DocumentHelper.parseText("<foo>" +
                "<param name=\"colour\">green</param>" +
                "<param name=\"size\" value=\"large\" />" +
                "</foo>");

        Map params = LoaderUtils.getParams(document.getRootElement());
        assertEquals(2, params.size());

        assertEquals("green", params.get("colour"));
        assertEquals("large", params.get("size"));
    }

}