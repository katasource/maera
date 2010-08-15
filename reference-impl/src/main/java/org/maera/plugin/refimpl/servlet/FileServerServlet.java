package org.maera.plugin.refimpl.servlet;

import org.maera.plugin.refimpl.ContainerManager;
import org.maera.plugin.servlet.AbstractFileServerServlet;
import org.maera.plugin.servlet.DownloadStrategy;

import java.util.List;

public class FileServerServlet extends AbstractFileServerServlet {
    private List downloadStrategies;

    protected List<DownloadStrategy> getDownloadStrategies() {
        if (downloadStrategies == null)
            downloadStrategies = ContainerManager.getInstance().getDownloadStrategies();

        return downloadStrategies;
    }
}
