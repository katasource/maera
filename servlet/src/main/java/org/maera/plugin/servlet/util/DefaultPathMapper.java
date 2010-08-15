package org.maera.plugin.servlet.util;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * Originally opied from Atlassian Seraph 1.0
 * <p/>
 * Modified to store a list of keys for a mapping rather than a single value.  This allows filters to be added that
 * listen on the same path.  The get() method will return the first value added if there are multiple keys matching the
 * path, while the getAll() method returns the aggregate matches.
 * <p/>
 * In practice, matching a servlet path should use the get() method and matching filters should use the getAll() method.
 *
 * @since 2.1.0
 */
public class DefaultPathMapper implements Serializable, PathMapper {

    private static final Pattern REDUNDANT_SLASHES = Pattern.compile("//+");
    private static final String[] DEFAULT_KEYS = {"/", "*", "/*"};

    private final Map<String, Collection<String>> mappings = new HashMap<String, Collection<String>>();
    private final Set<String> complexPaths = new HashSet<String>();

    private final KeyMatcher matcher = new KeyMatcher();

    // common use of this class is reentrant read-mostly (quite expensive calls too)
    // we don't want these reads to block each other so use a RWLock
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void put(final String key, final String pattern) {
        lock.writeLock().lock();
        try {
            if (pattern == null) {
                removeMappingsForKey(key);
                return;
            }
            addMapping(pattern, key);
            if ((pattern.indexOf('?') > -1) || ((pattern.indexOf("*") > -1) && (pattern.length() > 1))) {
                complexPaths.add(pattern);
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private void addMapping(String pattern, String key) {
        Collection<String> keys = mappings.get(pattern);
        if (keys == null) {
            keys = new LinkedHashSet<String>();
            mappings.put(pattern, keys);
        }
        keys.add(key);
    }

    // must be called under lock

    private void removeMappingsForKey(final String key) {
        for (final Iterator<Map.Entry<String, Collection<String>>> it = mappings.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<String, Collection<String>> entry = it.next();
            if (entry.getValue().remove(key) && entry.getValue().isEmpty()) {
                complexPaths.remove(entry.getKey());
                it.remove();
            }
        }
    }

    public String get(String path) {
        path = removeRedundantSlashes(path);
        lock.readLock().lock();
        try {
            if (path == null) {
                path = "/";
            }
            final String mapped = matcher.findKey(path, mappings, complexPaths);
            if (mapped == null) {
                return null;
            }
            Collection<String> keys = mappings.get(mapped);
            if (keys.isEmpty()) {
                return null;
            }
            return keys.iterator().next();
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /*
     * @see com.atlassian.seraph.util.PathMapper#getAll(java.lang.String)
     */

    public Collection<String> getAll(String path) {
        path = removeRedundantSlashes(path);
        lock.readLock().lock();
        try {
            if (path == null) {
                path = "/";
            }
            final Set<String> matches = new LinkedHashSet<String>();
            // find exact keys
            final String exactKey = matcher.findExactKey(path, mappings);
            if (exactKey != null) {
                matches.addAll(mappings.get(exactKey));
            }
            // find complex keys
            for (final Iterator<String> iterator = matcher.findComplexKeys(path, complexPaths).iterator(); iterator.hasNext();) {
                final String mapped = iterator.next();
                if (mappings.containsKey(mapped)) {
                    matches.addAll(mappings.get(mapped));
                }
            }
            // find default keys
            for (final Iterator<String> iterator = matcher.findDefaultKeys(mappings).iterator(); iterator.hasNext();) {
                final String mapped = iterator.next();
                matches.addAll(mappings.get(mapped));
            }
            return Collections.unmodifiableCollection(matches);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /**
     * <p>
     * Reduces sequences of more than one consecutive forward slash ("/") to a
     * single slash (see: https://studio.atlassian.com/browse/PLUG-597).
     * </p>
     *
     * @param path any string, including {@code null} (e.g. {@code "foo//bar"})
     * @return the input string, with all sequences of more than one
     *         consecutive slash removed (e.g. {@code "foo/bar"})
     */
    protected String removeRedundantSlashes(final String path) {
        return path == null ? null : REDUNDANT_SLASHES.matcher(path).replaceAll("/");
    }

    public String toString() {
        final StringBuffer sb = new StringBuffer(30 * (mappings.size() + complexPaths.size()));
        sb.append("Mappings:\n");
        for (final Iterator<String> iterator = mappings.keySet().iterator(); iterator.hasNext();) {
            final String key = (String) iterator.next();
            sb.append(key).append("=").append(mappings.get(key)).append("\n");
        }
        sb.append("Complex Paths:\n");
        for (final Iterator<String> iterator = complexPaths.iterator(); iterator.hasNext();) {
            final String path = (String) iterator.next();
            sb.append(path).append("\n");
        }

        return sb.toString();
    }

    private final class KeyMatcher {
        /**
         * Find exact key in mappings.
         */
        String findKey(final String path, final Map<String, Collection<String>> mappings, final Set<String> keys) {
            String result = findExactKey(path, mappings);
            if (result == null) {
                result = findComplexKey(path, keys);
            }
            if (result == null) {
                result = findDefaultKey(mappings);
            }
            return result;
        }

        /**
         * Check if path matches exact pattern ( /blah/blah.jsp ).
         */
        String findExactKey(final String path, final Map<String, Collection<String>> mappings) {
            if (mappings.containsKey(path)) {
                return path;
            }
            return null;
        }

        /**
         * Find single matching complex key
         */
        String findComplexKey(final String path, final Set<String> complexPaths) {
            for (String key : complexPaths) {
                if (match(key, path, false)) {
                    return key;
                }
            }
            return null;
        }

        /**
         * Find all matching complex keys
         */
        Collection<String> findComplexKeys(final String path, final Set<String> complexPaths) {
            final List<String> matches = new ArrayList<String>();
            for (String key : complexPaths) {
                if (match(key, path, false)) {
                    matches.add(key);
                }
            }
            return matches;
        }

        /**
         * Look for root pattern ( / ).
         */
        String findDefaultKey(final Map<String, Collection<String>> mappings) {
            for (int i = 0; i < DefaultPathMapper.DEFAULT_KEYS.length; i++) {
                if (mappings.containsKey(DefaultPathMapper.DEFAULT_KEYS[i])) {
                    return DefaultPathMapper.DEFAULT_KEYS[i];
                }
            }
            return null;
        }

        /**
         * Look for root patterns ( / ).
         */
        Collection<String> findDefaultKeys(final Map<String, Collection<String>> mappings) {
            final List<String> matches = new ArrayList<String>();

            for (int i = 0; i < DefaultPathMapper.DEFAULT_KEYS.length; i++) {
                if (mappings.containsKey(DefaultPathMapper.DEFAULT_KEYS[i])) {
                    matches.add(DefaultPathMapper.DEFAULT_KEYS[i]);
                }
            }

            return matches;
        }

        boolean match(final String pattern, final String str, final boolean isCaseSensitive) {
            final char[] patArr = pattern.toCharArray();
            final char[] strArr = str.toCharArray();
            int patIdxStart = 0;
            int patIdxEnd = patArr.length - 1;
            int strIdxStart = 0;
            int strIdxEnd = strArr.length - 1;
            char ch;

            boolean containsStar = false;
            for (int i = 0; i < patArr.length; i++) {
                if (patArr[i] == '*') {
                    containsStar = true;
                    break;
                }
            }

            if (!containsStar) {
                // No '*'s, so we make a shortcut
                if (patIdxEnd != strIdxEnd) {
                    return false; // Pattern and string do not have the same size
                }
                for (int i = 0; i <= patIdxEnd; i++) {
                    ch = patArr[i];
                    if (ch != '?') {
                        if (isCaseSensitive && (ch != strArr[i])) {
                            return false;// Character mismatch
                        }
                        if (!isCaseSensitive && (Character.toUpperCase(ch) != Character.toUpperCase(strArr[i]))) {
                            return false; // Character mismatch
                        }
                    }
                }
                return true; // String matches against pattern
            }

            if (patIdxEnd == 0) {
                return true; // Pattern contains only '*', which matches anything
            }

            // Process characters before first star
            while (((ch = patArr[patIdxStart]) != '*') && (strIdxStart <= strIdxEnd)) {
                if (ch != '?') {
                    if (isCaseSensitive && (ch != strArr[strIdxStart])) {
                        return false;// Character mismatch
                    }
                    if (!isCaseSensitive && (Character.toUpperCase(ch) != Character.toUpperCase(strArr[strIdxStart]))) {
                        return false;// Character mismatch
                    }
                }
                patIdxStart++;
                strIdxStart++;
            }
            if (strIdxStart > strIdxEnd) {
                // All characters in the string are used. Check if only '*'s are
                // left in the pattern. If so, we succeeded. Otherwise failure.
                for (int i = patIdxStart; i <= patIdxEnd; i++) {
                    if (patArr[i] != '*') {
                        return false;
                    }
                }
                return true;
            }

            // Process characters after last star
            while (((ch = patArr[patIdxEnd]) != '*') && (strIdxStart <= strIdxEnd)) {
                if (ch != '?') {
                    if (isCaseSensitive && (ch != strArr[strIdxEnd])) {
                        return false;// Character mismatch
                    }
                    if (!isCaseSensitive && (Character.toUpperCase(ch) != Character.toUpperCase(strArr[strIdxEnd]))) {
                        return false;// Character mismatch
                    }
                }
                patIdxEnd--;
                strIdxEnd--;
            }
            if (strIdxStart > strIdxEnd) {
                // All characters in the string are used. Check if only '*'s are
                // left in the pattern. If so, we succeeded. Otherwise failure.
                for (int i = patIdxStart; i <= patIdxEnd; i++) {
                    if (patArr[i] != '*') {
                        return false;
                    }
                }
                return true;
            }

            // process pattern between stars. padIdxStart and patIdxEnd point
            // always to a '*'.
            while ((patIdxStart != patIdxEnd) && (strIdxStart <= strIdxEnd)) {
                int patIdxTmp = -1;
                for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                    if (patArr[i] == '*') {
                        patIdxTmp = i;
                        break;
                    }
                }
                if (patIdxTmp == patIdxStart + 1) {
                    // Two stars next to each other, skip the first one.
                    patIdxStart++;
                    continue;
                }
                // Find the pattern between padIdxStart & padIdxTmp in str between
                // strIdxStart & strIdxEnd
                final int patLength = (patIdxTmp - patIdxStart - 1);
                final int strLength = (strIdxEnd - strIdxStart + 1);
                int foundIdx = -1;
                strLoop:
                for (int i = 0; i <= strLength - patLength; i++) {
                    for (int j = 0; j < patLength; j++) {
                        ch = patArr[patIdxStart + j + 1];
                        if (ch != '?') {
                            if (isCaseSensitive && (ch != strArr[strIdxStart + i + j])) {
                                continue strLoop;
                            }
                            if (!isCaseSensitive && (Character.toUpperCase(ch) != Character.toUpperCase(strArr[strIdxStart + i + j]))) {
                                continue strLoop;
                            }
                        }
                    }

                    foundIdx = strIdxStart + i;
                    break;
                }

                if (foundIdx == -1) {
                    return false;
                }

                patIdxStart = patIdxTmp;
                strIdxStart = foundIdx + patLength;
            }

            // All characters in the string are used. Check if only '*'s are left
            // in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }
    }
}
