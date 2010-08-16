package it.org.maera.plugin.refimpl;

import net.sourceforge.jwebunit.junit.WebTestCase;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.refimpl.ParameterUtils;
import org.maera.plugin.webresource.UrlMode;

public class IndexTest extends WebTestCase {

    public IndexTest(String name) {
        super(name);
    }

    @Before
    public void setUp() throws Exception {
        getTestContext().setBaseUrl(ParameterUtils.getBaseUrl(UrlMode.ABSOLUTE));
    }

    @Test
    public void testIndex() {
        beginAt("/");
        assertTextPresent("org.maera.plugin.osgi.bridge");

        assertTextNotPresent("Installed");
        assertTextPresent("General Decorator");
    }
}
