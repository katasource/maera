package net.maera.osgi.container;

import net.maera.io.Resource;
import net.maera.lifecycle.Startable;
import net.maera.lifecycle.Stoppable;
import org.osgi.framework.Bundle;

/**
 * Interface representing the operations possible when interacting with an OSGi framework implementation
 * (e.g. Apache Felix, Eclipse Equinox, et. al.)
 */
public interface Container extends Startable, Stoppable {

    @Override
    void start() throws ContainerException;

    @Override
    void stop() throws InterruptedException, ContainerException;

    void stop(long waitMillis) throws InterruptedException, ContainerException;

    Bundle installBundle(Resource resource) throws ContainerException;

}
