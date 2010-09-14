package org.maera.plugin.url;

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

/**
 */
class InnerJarURLConnection extends JarURLConnection {
    private URL baseResource;
    private String[] segments;
    private InputStream in;

    public InnerJarURLConnection(URL url) throws IOException {
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


        List<String> segments = new ArrayList<String>();

        StringTokenizer tokens = new StringTokenizer(extraText, "!");

        while (tokens.hasMoreTokens()) {
            segments.add(tokens.nextToken());
        }

        this.segments = segments.toArray(new String[segments.size()]);
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
    public void connect() throws IOException {
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
    protected void setupBaseResourceInputStream() throws IOException {
        this.in = getBaseResource().openStream();
    }

    /**
     * Setup the <code>InputStream</code> for URL with nested segments.
     *
     * @throws java.io.IOException If an I/O error occurs.
     */
    protected void setupPathedInputStream() throws IOException {
        InputStream curIn = getBaseResource().openStream();

        for (int i = 0; i < this.segments.length; ++i) {
            curIn = getSegmentInputStream(curIn, segments[i]);
        }

        this.in = curIn;
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
    protected InputStream getSegmentInputStream(InputStream baseIn, String segment) throws IOException {
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
    public InputStream getInputStream() throws IOException {
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

        if (url.startsWith("file:/")) {
            url = url.substring(6);
        }

        return new JarFile(URLDecoder.decode(url, "UTF-8"));
    }

    private static String normalizeUrlPath(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        // Looking for org/codehaus/werkflow/personality/basic/../common/core-idioms.xml
        //                                               |    i  |
        //                                               +-------+ remove
        //
        int i = name.indexOf("/..");

        // Can't be at the beginning because we have no root to refer to so
        // we start at 1.
        if (i > 0) {
            int j = name.lastIndexOf("/", i - 1);

            name = name.substring(0, j) + name.substring(i + 3);
        }

        return name;
    }

}
