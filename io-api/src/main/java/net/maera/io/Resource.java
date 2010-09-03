package net.maera.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * Interface for a resource descriptor that abstracts from the actual
 * type of underlying resource, such as a file or class path resource.
 * <p/>
 * An InputStream can be opened for every resource if it exists in
 * physical form, but a URL or File handle can just be returned for
 * certain resources. The actual behavior is implementation-specific.
 * <p/>
 * Shamelessly copied from the Spring 3.0.3 distribution to prevent a runtime dependency on Spring for all API
 * implementations.  All license conditions and author attribution has remained in tact.
 *
 * @author Juergen Hoeller
 * @see #getInputStream()
 * @see #getURL()
 * @see #getURI()
 * @see #getFile()
 *      - @see FileSystemResource
 *      - @see ClassPathResource
 *      - @see UrlResource
 *      - @see ByteArrayResource
 *      - @see InputStreamResource
 *      - @see org.springframework.web.context.support.ServletContextResource
 * @since 28.12.2003
 */
//TODO - throw IOExceptions in this class?  Or create a common runtime ResourceException class?
public interface Resource extends InputStreamSource {

    /**
     * Return whether this resource actually exists in physical form.
     * <p/>
     * This method performs a definitive existence check, whereas the
     * existence of a {@code Resource} handle only guarantees a
     * valid descriptor handle.
     *
     * @return whether this resource actually exists in physical form.
     */
    boolean exists();

    /**
     * Return whether the contents of this resource can be read,
     * e.g. via {@link #getInputStream()} or {@link #getFile()}.
     * <p/>
     * Will be {@code true} for typical resource descriptors;
     * note that actual content reading may still fail when attempted.
     * However, a value of {@code false}} is a definitive indication
     * that the resource content cannot be read.
     *
     * @return whether the contents of this resource can be read.
     */
    boolean isReadable();

    /**
     * Return whether this resource represents a handle with an open
     * stream. If true, the InputStream cannot be read multiple times,
     * and must be read and closed to avoid resource leaks.
     * <p/>
     * Will be {@code false} for typical resource descriptors.
     *
     * @return whether this resource represents a handle with an open stream.
     */
    boolean isOpen();

    /**
     * Return a URL handle for this resource.
     *
     * @return a URL handle for this resource.
     * @throws IOException if the resource cannot be resolved as URL,
     *                     i.e. if the resource is not available as descriptor
     */
    URL getURL() throws IOException;

    /**
     * Return a URI handle for this resource.
     *
     * @return a URI handle for this resource.
     * @throws IOException if the resource cannot be resolved as URI,
     *                     i.e. if the resource is not available as descriptor
     */
    URI getURI() throws IOException;

    /**
     * Return a File handle for this resource.
     *
     * @return a File handle for this resource.
     * @throws IOException if the resource cannot be resolved as absolute
     *                     file path, i.e. if the resource is not available in a file system
     */
    File getFile() throws IOException;

    /**
     * Determine the last-modified timestamp for this resource.
     *
     * @return the last-modified timestamp for this resource.
     * @throws IOException if the resource cannot be resolved
     *                     (in the file system or as some other known physical resource type)
     */
    long lastModified() throws IOException;

    /**
     * Create a resource relative to this resource.
     *
     * @param relativePath the relative path (relative to this resource)
     * @return the resource handle for the relative resource
     * @throws IOException if the relative resource cannot be determined
     */
    Resource createRelative(String relativePath) throws IOException;

    /**
     * Return a filename for this resource, i.e. typically the last
     * part of the path: for example, "myfile.txt".
     *
     * @return a filename for this resource
     */
    String getFilename();

    /**
     * Return a description for this resource,
     * to be used for error output when working with the resource.
     * <p/>
     * Implementations are also encouraged to return this value from their {@link #toString() toString()}
     * implementation.
     *
     * @return a description for this resource
     * @see java.lang.Object#toString()
     */
    String getDescription();
}
