package org.maera.plugin.refimpl;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.util.Properties;

/**
 * Initializes app
 */
public class InitListener implements ServletContextListener {

    public InitListener() {
    }

    public void contextInitialized(ServletContextEvent sce) {
        initializeLogger();

        Logger.getLogger(InitListener.class).info("Logging initialized.");
        ContainerManager.setInstance(new ContainerManager(sce.getServletContext()));
        ContainerManager mgr = ContainerManager.getInstance();
        mgr.getPluginAccessor().getPlugins();
    }

    public void contextDestroyed(ServletContextEvent sce) {
        ContainerManager mgr = ContainerManager.getInstance();
        if (mgr != null)
            mgr.shutdown();
        ContainerManager.setInstance(null);
    }

    private void initializeLogger() {
        Properties logProperties = new Properties();

        try {
            logProperties.load(getClass().getResourceAsStream("/log4j.properties"));
            PropertyConfigurator.configure(logProperties);

        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load logging property", e);
        }
    }

}
