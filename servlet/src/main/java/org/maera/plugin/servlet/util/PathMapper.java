package org.maera.plugin.servlet.util;

import java.util.Collection;

/**
 * The PathMapper is used to map file patterns to keys, and find an appropriate key for a given file path. The pattern rules are consistent with those
 * defined in the Servlet 2.3 API on the whole. Wildcard patterns are also supported, using any combination of * and ?.
 * <h3>Example</h3>
 * <blockquote><code>
 * PathMapper pm = new PathMapper();<br>
 * <br>
 * pm.put("one","/");<br>
 * pm.put("two","/mydir/*");<br>
 * pm.put("three","*.xml");<br>
 * pm.put("four","/myexactfile.html");<br>
 * pm.put("five","/*\/admin/*.??ml");<br>
 * <br>
 * String result1 = pm.get("/mydir/myfile.xml"); // returns "two";<br>
 * String result2 = pm.get("/mydir/otherdir/admin/myfile.html"); // returns "five";<br>
 * </code></blockquote>
 * <p/>
 * This was copied from Atlassian Seraph 1.0
 *
 * @since 2.1.0
 */
public interface PathMapper {
    /**
     * Retrieve appropriate key by matching patterns with supplied path.
     */
    String get(String path);

    /**
     * Retrieve all mappings which match a supplied path.
     */
    Collection<String> getAll(String path);

    /**
     * Add a key and appropriate matching pattern.
     */
    void put(final String key, final String pattern);
}