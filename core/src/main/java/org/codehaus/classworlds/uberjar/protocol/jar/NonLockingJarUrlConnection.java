package org.codehaus.classworlds.uberjar.protocol.jar;

/*
 $Id: JarUrlConnection.java 78 2004-07-01 13:59:13Z jvanzyl $

 Copyright 2002 (C) The Werken Company. All Rights Reserved.

 Redistribution and use of this software and associated documentation
 ("Software"), with or without modification, are permitted provided
 that the following conditions are met:

 1. Redistributions of source code must retain copyright
    statements and notices.  Redistributions must also contain a
    copy of this document.

 2. Redistributions in binary form must reproduce the
    above copyright notice, this list of conditions and the
    following disclaimer in the documentation and/or other
    materials provided with the distribution.

 3. The name "classworlds" must not be used to endorse or promote
    products derived from this Software without prior written
    permission of The Werken Company.  For written permission,
    please contact bob@werken.com.

 4. Products derived from this Software may not be called "classworlds"
    nor may "classworlds" appear in their names without prior written
    permission of The Werken Company. "classworlds" is a registered
    trademark of The Werken Company.

 5. Due credit should be given to The Werken Company.
    (http://classworlds.werken.com/).

 THIS SOFTWARE IS PROVIDED BY THE WERKEN COMPANY AND CONTRIBUTORS
 ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 THE WERKEN COMPANY OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.

 */

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * This is copied from Classwords 1.1 org.codehaus.classworlds.uberjar.protocol.jar.JarURLConnection
 * so that an additional dependency does not need to be added to plugins.  The formatting is left as is to reduce
 * the diff.
 * <p/>
 * The setupPathedInputStream() method has been modified to improve the speed of resource lookups. It now
 * uses a ZipEntry to get random access to entries in the JAR.
 * <p/>
 * This change removes the ability for this connection class to load resources from JARs nested inside the outer
 * JAR. This is not used in Maera because the inner JAR loading is handled by
 * {@link org.maera.plugin.classloader.PluginClassLoader}.
 */
public class NonLockingJarUrlConnection
        extends JarURLConnection {
    // ----------------------------------------------------------------------
    //     Instance members
    // ----------------------------------------------------------------------

    /**
     * Base resource.
     */
    private URL baseResource;

    /**
     * Additional nested segments.
     */
    private String[] segments;

    /**
     * Terminal input-stream.
     */
    private InputStream in;

    // ----------------------------------------------------------------------
    //     Constructors
    // ----------------------------------------------------------------------

    /**
     * Construct.
     *
     * @param url Target URL of the connections.
     * @throws java.io.IOException If an error occurs while attempting to initialize
     *                             the connection.
     */
    NonLockingJarUrlConnection(URL url)
            throws IOException {
        super(url = normaliseURL(url));

        String baseText = url.getPath();

        int bangLoc = baseText.indexOf("!");

        String baseResourceText = baseText.substring(0, bangLoc);

        String extraText = "";

        if (bangLoc <= (baseText.length() - 2)
                &&
                baseText.charAt(bangLoc + 1) == '/') {
            if (bangLoc + 2 == baseText.length()) {
                extraText = "";
            } else {
                extraText = baseText.substring(bangLoc + 1);
            }
        } else {
            throw new MalformedURLException("No !/ in url: " + url.toExternalForm());
        }


        List segments = new ArrayList();

        StringTokenizer tokens = new StringTokenizer(extraText, "!");

        while (tokens.hasMoreTokens()) {
            segments.add(tokens.nextToken());
        }

        this.segments = (String[]) segments.toArray(new String[segments.size()]);

        this.baseResource = new URL(baseResourceText);
    }

    protected static URL normaliseURL(URL url) throws MalformedURLException {
        String text = normalizeUrlPath(url.toString());

        if (!text.startsWith("jar:")) {
            text = "jar:" + text;
        }

        if (text.indexOf('!') < 0) {
            text = text + "!/";
        }

        return new URL(text);
    }

    // ----------------------------------------------------------------------
    //     Instance methods
    // ----------------------------------------------------------------------

    /**
     * Retrieve the nesting path segments.
     *
     * @return The segments.
     */
    protected String[] getSegments() {
        return this.segments;
    }

    /**
     * Retrieve the base resource <code>URL</code>.
     *
     * @return The base resource url.
     */
    protected URL getBaseResource() {
        return this.baseResource;
    }

    /**
     * @see java.net.URLConnection
     */
    public void connect()
            throws IOException {
        if (this.segments.length == 0) {
            setupBaseResourceInputStream();
        } else {
            setupPathedInputStream();
        }
    }

    /**
     * Setup the <code>InputStream</code> purely from the base resource.
     *
     * @throws java.io.IOException If an I/O error occurs.
     */
    protected void setupBaseResourceInputStream()
            throws IOException {
        this.in = getBaseResource().openStream();
    }

    /**
     * Setup the <code>InputStream</code> for URL with nested segments.
     *
     * @throws java.io.IOException If an I/O error occurs.
     */
    protected void setupPathedInputStream()
            throws IOException {
        final JarFile jar = getJarFile();
        String entryName = segments[0].substring(1); // remove leading slash
        final ZipEntry zipEntry = jar.getEntry(entryName);

        if (zipEntry == null) {
            throw new IOException("Unable to locate entry: " + entryName + ", in JAR file: " + jar.getName());
        }

        final InputStream delegate = jar.getInputStream(zipEntry);
        this.in = new InputStream() {

            public int read() throws IOException {
                return delegate.read();
            }

            public int read(byte b[]) throws IOException {
                return delegate.read(b);
            }

            public int read(byte b[], int off, int len) throws IOException {
                return delegate.read(b, off, len);
            }

            public long skip(long n) throws IOException {
                return delegate.skip(n);
            }

            public int available() throws IOException {
                return delegate.available();
            }

            public void close() throws IOException {
                // close the stream and the plugin JAR file
                delegate.close();
                jar.close();
            }

            public synchronized void mark(int readlimit) {
                delegate.mark(readlimit);
            }

            public synchronized void reset() throws IOException {
                delegate.reset();
            }

            public boolean markSupported() {
                return delegate.markSupported();
            }
        };
    }

    /**
     * Retrieve the <code>InputStream</code> for the nesting
     * segment relative to a base <code>InputStream</code>.
     *
     * @param baseIn  The base input-stream.
     * @param segment The nesting segment path.
     * @return The input-stream to the segment.
     * @throws java.io.IOException If an I/O error occurs.
     */
    protected InputStream getSegmentInputStream(InputStream baseIn,
                                                String segment)
            throws IOException {
        JarInputStream jarIn = new JarInputStream(baseIn);
        JarEntry entry = null;

        while (jarIn.available() != 0) {
            entry = jarIn.getNextJarEntry();

            if (entry == null) {
                break;
            }

            if (("/" + entry.getName()).equals(segment)) {
                return jarIn;
            }
        }

        throw new IOException("unable to locate segment: " + segment);
    }

    /**
     * @see java.net.URLConnection
     */
    public InputStream getInputStream()
            throws IOException {
        if (this.in == null) {
            connect();
        }
        return this.in;
    }

    /**
     * @return JarFile
     * @throws java.io.IOException
     * @see java.net.JarURLConnection#getJarFile()
     */
    public JarFile getJarFile() throws IOException {
        String url = baseResource.toExternalForm();

        if (url.startsWith("file:")) {
            url = url.substring(5);
        }

        return new JarFile(URLDecoder.decode(url, "UTF-8"));
    }

    private static String normalizeUrlPath(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        int i = name.indexOf("/..");
        if (i > 0) {
            int j = name.lastIndexOf("/", i - 1);
            name = name.substring(0, j) + name.substring(i + 3);
        }

        return name;
    }
}
