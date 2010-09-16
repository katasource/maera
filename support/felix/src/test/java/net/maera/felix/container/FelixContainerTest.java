package net.maera.felix.container;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import net.maera.io.FileSystemResource;
import net.maera.osgi.container.impl.DefaultHostActivator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 0.1
 */
public class FelixContainerTest {

    @Test
    public void testDefault() throws Exception {
        DefaultHostActivator activator = new DefaultHostActivator();
        activator.setInitialBundlesLocation(new FileSystemResource(System.getProperty("seedBundlesZip")));
        activator.setInitialBundlesExtractionDirectory(new FileSystemResource(System.getProperty("seedBundlesDir") + "/unzipped"));
        
        FelixContainer container = new FelixContainer();
        Map<String,String> extras = new HashMap<String,String>();
        extras.put("org.slf4j", "1.5.6");
        extras.put("org.apache.commons.logging", "1.1.1");
        container.setExtraSystemPackages(extras);
        container.setHostActivator(activator);

        container.setCacheDirectoryPath(System.getProperty("project.build.directory"));
        container.init();
        container.start();
        container.stop();
        container.destroy();
    }

}
