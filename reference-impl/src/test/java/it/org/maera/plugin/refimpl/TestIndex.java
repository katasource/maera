package it.org.maera.plugin.refimpl;

import net.sourceforge.jwebunit.junit.WebTestCase;
import org.maera.plugin.refimpl.ParameterUtils;
import org.maera.plugin.webresource.UrlMode;

public class TestIndex extends WebTestCase {
    public TestIndex(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        getTestContext().setBaseUrl(ParameterUtils.getBaseUrl(UrlMode.ABSOLUTE));
    }

    public void testIndex() {
        beginAt("/");
        assertTextPresent("org.maera.plugin.osgi.bridge");

        assertTextNotPresent("Installed");
        assertTextPresent("General Decorator");
    }
}
