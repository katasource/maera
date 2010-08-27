package org.maera.plugin.servlet.download.plugin;

import com.mockobjects.dynamic.Mock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.event.events.PluginModuleDisabledEvent;
import org.maera.plugin.event.events.PluginModuleEnabledEvent;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.module.ClassPrefixModuleFactory;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.module.PrefixDelegatingModuleFactory;
import org.maera.plugin.module.PrefixModuleFactory;
import org.maera.plugin.servlet.DownloadException;
import org.maera.plugin.servlet.DownloadStrategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

public class PluggableDownloadStrategyTest extends Assert {

    private PluggableDownloadStrategy strategy;

    @Before
    public void setUp() throws Exception {
        strategy = new PluggableDownloadStrategy(new DefaultPluginEventManager());
    }

    @Test
    public void testPluginModuleDisabled() throws Exception {
        ModuleDescriptor module = new DownloadStrategyModuleDescriptor(getDefaultModuleClassFactory()) {

            public String getCompleteKey() {
                return "jungle.plugin:lion-strategy";
            }

            public DownloadStrategy getModule() {
                return new StubDownloadStrategy("/lion", "ROAR!");
            }
        };

        strategy.pluginModuleEnabled(new PluginModuleEnabledEvent(module));
        assertTrue(strategy.matches("/lion/something"));

        strategy.pluginModuleDisabled(new PluginModuleDisabledEvent(module));
        assertFalse(strategy.matches("/lion/something"));
    }

    @Test
    public void testPluginModuleEnabled() throws Exception {

        ModuleDescriptor module = new DownloadStrategyModuleDescriptor(getDefaultModuleClassFactory()) {

            public String getCompleteKey() {
                return "jungle.plugin:lion-strategy";
            }

            public DownloadStrategy getModule() {
                return new StubDownloadStrategy("/lion", "ROAR!");
            }
        };

        strategy.pluginModuleEnabled(new PluginModuleEnabledEvent(module));

        assertTrue(strategy.matches("/lion/something"));

        StringWriter result = new StringWriter();
        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expectAndReturn("getWriter", new PrintWriter(result));
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getRequestURI", "/lion/something");

        strategy.serveFile((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
        assertEquals("ROAR!\n", result.toString());
    }

    @Test
    public void testRegister() throws Exception {
        strategy.register("monkey.key", new StubDownloadStrategy("/monkey", "Bananas"));

        assertTrue(strategy.matches("/monkey/something"));

        StringWriter result = new StringWriter();
        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expectAndReturn("getWriter", new PrintWriter(result));
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getRequestURI", "/monkey/something");

        strategy.serveFile((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
        assertEquals("Bananas\n", result.toString());
    }

    @Test
    public void testUnregister() throws Exception {
        strategy.register("monkey.key", new StubDownloadStrategy("/monkey", "Bananas"));
        strategy.unregister("monkey.key");

        assertFalse(strategy.matches("/monkey/something"));
    }

    @Test
    public void testUnregisterPluginModule() throws Exception {
        ModuleDescriptor module = new DownloadStrategyModuleDescriptor(getDefaultModuleClassFactory()) {

            public String getCompleteKey() {
                return "jungle.plugin:lion-strategy";
            }

            public DownloadStrategy getModule() {
                return new StubDownloadStrategy("/lion", "ROAR!");
            }
        };

        strategy.pluginModuleEnabled(new PluginModuleEnabledEvent(module));
        assertTrue(strategy.matches("/lion/something"));

        strategy.unregister("jungle.plugin:lion-strategy");
        assertFalse(strategy.matches("/lion/something"));
    }

    protected ModuleFactory getDefaultModuleClassFactory() {
        return new PrefixDelegatingModuleFactory(
                Collections.<PrefixModuleFactory>singleton(new ClassPrefixModuleFactory(new DefaultHostContainer())));
    }

    private static class StubDownloadStrategy implements DownloadStrategy {

        private final String output;
        private final String urlPattern;

        public StubDownloadStrategy(String urlPattern, String output) {
            this.urlPattern = urlPattern;
            this.output = output;
        }

        public boolean matches(String urlPath) {
            return urlPath.contains(urlPattern);
        }

        public void serveFile(HttpServletRequest request, HttpServletResponse response) throws DownloadException {
            try {
                response.getWriter().println(output);
            }
            catch (IOException e) {
                throw new DownloadException(e);
            }
        }
    }
}
