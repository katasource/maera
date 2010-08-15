package org.springframework.osgi.maera;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

/**
 * Application context that initializes the bean definition reader to not validate via XML Schema.  Note that by
 * turning this off, certain defaults won't be populated like expected.  For example, XML Schema provides the default
 * autowire value of "default", but without this validation, that value is not set so autowiring will be turned off.
 * <p/>
 * This class exists in the same package as the parent so the log messages won't get confused as the parent class
 * logs against the instance class.
 *
 * @since 2.5.0
 */
public class NonValidatingOsgiBundleXmlApplicationContext extends OsgiBundleXmlApplicationContext {
    public NonValidatingOsgiBundleXmlApplicationContext(String[] configLocations) {
        super(configLocations);
    }

    @Override
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
        super.initBeanDefinitionReader(beanDefinitionReader);
        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
        beanDefinitionReader.setNamespaceAware(true);
    }
}
