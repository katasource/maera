package net.maera.osgi.container.impl;

/**
 * Created by IntelliJ IDEA.
 * User: Les
 * Date: Sep 14, 2010
 * Time: 1:01:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultContainer extends LifecycleContainer {

    public DefaultContainer() {
        super();
    }

    public DefaultContainer(String startupThreadFactoryName) {
        super(startupThreadFactoryName);
    }

    @Override
    protected void onStart() throws Exception {
        super.onStart();    //To change body of overridden methods use File | Settings | File Templates.
    }
}
