package net.maera.spring.io;

import net.maera.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * Maera {@link Resource} backed by a {@link org.springframework.core.io.Resource} instance.  This allows Maera
 * resources to be configured in a spring file using Spring's Resource abstractions.
 */
public class SpringResource implements Resource {

    private final org.springframework.core.io.Resource springResource;

    public SpringResource(org.springframework.core.io.Resource resource) {
        this.springResource = resource;
    }

    @Override
    public boolean exists() {
        return springResource.exists();
    }

    @Override
    public boolean isReadable() {
        return springResource.isReadable();
    }

    @Override
    public boolean isOpen() {
        return springResource.isOpen();
    }

    @Override
    public URL getURL() throws IOException {
        return springResource.getURL();
    }

    @Override
    public URI getURI() throws IOException {
        return springResource.getURI();
    }

    @Override
    public File getFile() throws IOException {
        return springResource.getFile();
    }

    @Override
    public long lastModified() throws IOException {
        return springResource.lastModified();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return new SpringResource(springResource.createRelative(relativePath));
    }

    @Override
    public String getFilename() {
        return springResource.getFilename();
    }

    @Override
    public String getDescription() {
        return springResource.getDescription();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return springResource.getInputStream();
    }

    @Override
    public int hashCode() {
        return springResource.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SpringResource) {
            SpringResource other = (SpringResource)obj;
            return springResource.equals(other.springResource);
        }
        return false;
    }

    @Override
    public String toString() {
        return springResource.toString();
    }
}
