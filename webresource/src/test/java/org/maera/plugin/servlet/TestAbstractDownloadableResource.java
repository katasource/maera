package org.maera.plugin.servlet;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.servlet.util.CapturingHttpServletResponse;
import org.maera.plugin.util.PluginUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * A test for AbstractDownloadableResource
 */
public class TestAbstractDownloadableResource extends TestCase {
    private static final String MINIFIED_CONTENT = "minified content";
    private static final String PLAIN_CONTENT = "plain content";
    private static final String NEVER_MINIFIED_CONTENT = "never minified content";

    private class MinifiedFileServingDownloableResource extends AbstractDownloadableResource {

        public MinifiedFileServingDownloableResource(final ResourceLocation resourceLocation) {
            super(null, resourceLocation, null);
        }

        @Override
        protected String getLocation() {
            return "somecode.js";
        }

        @Override
        public String getContentType() {
            return "minified/content";
        }

        @Override
        protected InputStream getResourceAsStream(final String resourceLocation) {
            if (resourceLocation.contains("-min.")) {
                assertEquals("somecode-min.js", resourceLocation);
                return newStream(MINIFIED_CONTENT);
            }
            assertEquals("somecode.js", resourceLocation);
            return newStream(PLAIN_CONTENT);
        }

        private InputStream newStream(final String s) {
            return new ByteArrayInputStream(s.getBytes());
        }
    }

    private class MyDownloableResource extends AbstractDownloadableResource {
        public String getPassedResourceLocation() {
            return passedResourceLocation;
        }

        private String passedResourceLocation;

        public MyDownloableResource(final ResourceLocation resourceLocation, final boolean disableMinification) {
            super(null, resourceLocation, "", disableMinification);
        }

        @Override
        protected InputStream getResourceAsStream(final String resourceLocation) {
            passedResourceLocation = resourceLocation;
            return newStream(resourceLocation);
        }

        private InputStream newStream(final String s) {
            return new ByteArrayInputStream(s.getBytes());
        }
    }

    private class NotMinifiedFileServingDownloableResouce extends AbstractDownloadableResource {
        public NotMinifiedFileServingDownloableResouce() {
            super(null, new ResourceLocation("/flintstone/fred.jpg", "fred.jpg", "stuff", "stuff", "stuff", null), null);
        }

        @Override
        protected String getLocation() {
            return "somemorecode.js";
        }

        @Override
        public String getContentType() {
            return "plain/content";
        }

        @Override
        protected InputStream getResourceAsStream(final String resourceLocation) {
            if (resourceLocation.contains("-min.")) {
                assertEquals("somemorecode-min.js", resourceLocation);
                return null;
            }
            assertEquals("somemorecode.js", resourceLocation);
            return newStream(PLAIN_CONTENT);
        }

        private InputStream newStream(final String s) {
            return new ByteArrayInputStream(s.getBytes());
        }

    }

    private class NeverMinifiedFileServingDownloableResouce extends AbstractDownloadableResource {
        public NeverMinifiedFileServingDownloableResouce() {
            super(null, new ResourceLocation("/flintstone/fred.jpg", "fred.jpg", "stuff", "stuff", "stuff", null), null);
        }

        @Override
        protected String getLocation() {
            return "neverminified.js";
        }

        @Override
        public String getContentType() {
            return "neverminified/content";
        }

        @Override
        protected InputStream getResourceAsStream(final String resourceLocation) {
            if (resourceLocation.contains("-min.")) {
                fail("it should never ask for this");
            }
            assertEquals("neverminified.js", resourceLocation);
            return newStream(NEVER_MINIFIED_CONTENT);
        }

        private InputStream newStream(final String s) {
            return new ByteArrayInputStream(s.getBytes());
        }
    }

    public void testMinificationStrategyWrongFileType() throws Exception {
        final ResourceLocation resourceLocation = new ResourceLocation("/flintstone/fred.jpg", "fred.jpg", "stuff", "stuff", "stuff", null);
        final MyDownloableResource myDownloableResource = new MyDownloableResource(resourceLocation, false);
        myDownloableResource.streamResource(new ByteArrayOutputStream());

        assertEquals("/flintstone/fred.jpg", myDownloableResource.passedResourceLocation);
    }

    public void testMinificationStrategyCss() throws Exception {
        final ResourceLocation resourceLocation = new ResourceLocation("/flintstone/fred.css", "fred.css", "stuff", "stuff", "stuff", null);
        final MyDownloableResource myDownloableResource = new MyDownloableResource(resourceLocation, false);
        myDownloableResource.streamResource(new ByteArrayOutputStream());

        assertEquals("/flintstone/fred-min.css", myDownloableResource.passedResourceLocation);
    }

    public void testMinificationStrategyWithTwoMinsInName() throws Exception {
        final ResourceLocation resourceLocation = new ResourceLocation("/flintstone-min./fred-min.js", "fred-min.js", "stuff", "stuff", "stuff", null);
        final MyDownloableResource myDownloableResource = new MyDownloableResource(resourceLocation, false);
        myDownloableResource.streamResource(new ByteArrayOutputStream());

        assertEquals("/flintstone-min./fred-min.js", myDownloableResource.passedResourceLocation);
    }

    public void testMinificationStrategyAlreadyMinimised() throws Exception {
        final ResourceLocation resourceLocation = new ResourceLocation("/flintstone/fred-min.js", "fred-min.js", "stuff", "stuff", "stuff", null);
        final MyDownloableResource myDownloableResource = new MyDownloableResource(resourceLocation, false);
        myDownloableResource.streamResource(new ByteArrayOutputStream());

        assertEquals("/flintstone/fred-min.js", myDownloableResource.passedResourceLocation);
    }

    public void testMinificationStrategyNotMinimisedAndEnabled() throws Exception {
        final ResourceLocation resourceLocation = new ResourceLocation("/flintstone/fred.js", "fred.js", "stuff", "stuff", "stuff", null);
        final MyDownloableResource myDownloableResource = new MyDownloableResource(resourceLocation, false);
        myDownloableResource.streamResource(new ByteArrayOutputStream());

        assertEquals("/flintstone/fred-min.js", myDownloableResource.passedResourceLocation);
    }

    public void testMinificationStrategyNotMinimisedAndDisabled() throws Exception {
        final ResourceLocation resourceLocation = new ResourceLocation("/flintstone/fred.js", "fred.js", "stuff", "stuff", "stuff", null);
        final MyDownloableResource myDownloableResource = new MyDownloableResource(resourceLocation, true);
        myDownloableResource.streamResource(new ByteArrayOutputStream());

        assertEquals("/flintstone/fred.js", myDownloableResource.passedResourceLocation);
    }

    public void testMinificationStrategyNotMinimisedAndSystemDisabled() throws Exception {
        System.setProperty("atlassian.webresource.disable.minification", "true");

        final ResourceLocation resourceLocation = new ResourceLocation("/flintstone/fred.js", "fred.js", "stuff", "stuff", "stuff", null);
        final MyDownloableResource myDownloableResource = new MyDownloableResource(resourceLocation, false);
        myDownloableResource.streamResource(new ByteArrayOutputStream());

        assertEquals("/flintstone/fred.js", myDownloableResource.passedResourceLocation);
    }

    public void testMinificationStrategyNotMinimisedAndSystemEnabled() throws Exception {
        System.setProperty("atlassian.webresource.disable.minification", "false");

        final ResourceLocation resourceLocation = new ResourceLocation("/flintstone/fred.js", "fred.js", "stuff", "stuff", "stuff", null);
        final MyDownloableResource myDownloableResource = new MyDownloableResource(resourceLocation, false);
        myDownloableResource.streamResource(new ByteArrayOutputStream());

        assertEquals("/flintstone/fred-min.js", myDownloableResource.passedResourceLocation);
    }

    public void testWithMinifiedStrategyInPlay() throws DownloadException {
        // it should ask for -min files first and in this case get content back
        final MinifiedFileServingDownloableResource minifiedFileServingDownloableResource = new MinifiedFileServingDownloableResource(new ResourceLocation("/flintstone/fred.jpg", "fred.jpg", "stuff", "stuff", "stuff", null));
        assertContent(minifiedFileServingDownloableResource, MINIFIED_CONTENT);

        // it should ask for -min files first but get null and hence move on to the plain old content case.
        final NotMinifiedFileServingDownloableResouce notMinifiedFileServingDownloableResouce = new NotMinifiedFileServingDownloableResouce();
        assertContent(notMinifiedFileServingDownloableResouce, PLAIN_CONTENT);

    }

    public void testWhenSystemPropertyIsSet() throws DownloadException {
        verifySystemPropertyRespected("atlassian.webresource.disable.minification");
    }

    public void testWhenDevModeSystemPropertyIsSet() throws DownloadException {
        verifySystemPropertyRespected(PluginUtils.ATLASSIAN_DEV_MODE);
    }

    private void verifySystemPropertyRespected(String sysprop)
            throws DownloadException {
        try {
            System.setProperty(sysprop, "true");

            // now in this case it must never ask for minified files.  This class used will assert that.
            final NeverMinifiedFileServingDownloableResouce neverMinifiedFileServingDownloableResouce = new NeverMinifiedFileServingDownloableResouce();
            assertContent(neverMinifiedFileServingDownloableResouce, NEVER_MINIFIED_CONTENT);

            final MinifiedFileServingDownloableResource minifiedFileServingDownloableResouce = new MinifiedFileServingDownloableResource(new ResourceLocation("/flintstone/fred.jpg", "fred.jpg", "stuff", "stuff", "stuff", null));
            assertContent(minifiedFileServingDownloableResouce, PLAIN_CONTENT);

            // it should ask for -min files first but get null and hence move on to the plain old content case.
            final NotMinifiedFileServingDownloableResouce notMinifiedFileServingDownloableResouce = new NotMinifiedFileServingDownloableResouce();
            assertContent(notMinifiedFileServingDownloableResouce, PLAIN_CONTENT);

            System.setProperty(sysprop, "false");

            // it should ask for -min files first and in this case get content back
            assertContent(minifiedFileServingDownloableResouce, MINIFIED_CONTENT);

            // it should ask for -min files first but get null and hence move on to the plain old content case.
            assertContent(notMinifiedFileServingDownloableResouce, PLAIN_CONTENT);

            //
            // now this test is wierd but hey.  If I call back on a never minified resource class object it should
            // throw an assertion exception that it doesnt expect it.  This proves that it odes indeed get called
            // with a -min version of itself
            try {
                assertContent(neverMinifiedFileServingDownloableResouce, "doesnt matter");

                fail("This should have barfed in NeverMinifiedFileServingDownloableResouce");
            }
            catch (final AssertionFailedError expected) {
                // this is expected since the test class asserts tgat a -min file should never be called on it.
                // and hence by inference the atlassian.webresource.disable.minification property is not taking effect
            }
        }
        finally {
            // reset for this test
            System.setProperty(sysprop, "false");
        }
    }

    private void assertContent(final AbstractDownloadableResource downloadableResource, final String content) throws DownloadException {
        final CapturingHttpServletResponse httpServletResponse = new CapturingHttpServletResponse();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            downloadableResource.serveResource(null, httpServletResponse);
        }
        catch (final DownloadException e) {
            throw new RuntimeException(e);
        }
        downloadableResource.streamResource(baos);

        assertEquals(content, httpServletResponse.toString());
        assertEquals(content, baos.toString());
    }
}
