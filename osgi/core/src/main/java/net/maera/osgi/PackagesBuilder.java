package net.maera.osgi;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * @since 0.1
 */
public class PackagesBuilder {

    private static final String JDK5_PACKAGES_FILE = "jdk-packages.txt";
    private static final String JDK6_PACKAGES_FILE = "jdk6-packages.txt";
    private static final String JDK7_PACKAGES_FILE = "jdk7-packages.txt";

    private static final char PACKAGE_DELIMITER = ',';
    private static final char PARAMETER_DELIMITER = ';';
    private static final String VERSION_PARAMETER = "version";
    private static final char VALUE_DELIMITER = '=';

    private static Logger log = LoggerFactory.getLogger(PackagesBuilder.class);

    private String toStringCache;

    private final StringBuilder buffer;

    public PackagesBuilder() {
        buffer = new StringBuilder();
    }

    public PackagesBuilder append(String pkgName) {
        return append(pkgName, null);
    }

    public PackagesBuilder append(String pkgName, String versionRange) {
        toStringCache = null;
        if (buffer.length() > 0) {
            buffer.append(PACKAGE_DELIMITER);
        }
        buffer.append(pkgName);
        if (versionRange != null) {
            buffer.append(PARAMETER_DELIMITER).append(VERSION_PARAMETER).append(VALUE_DELIMITER).append(versionRange);

        }
        return this;
    }

    /**
     * Append the OSGi packages by default (OSGi 4.2 specification package versions).
     *
     * @return 'this' for method chaining.
     */
    public PackagesBuilder withOsgiDefaults() {
        return append("org.osgi.framework", "1.5.0")
                .append("org.osgi.service.packageadmin", "1.2.0")
                .append("org.osgi.service.startlevel", "1.1.0")
                .append("org.osgi.service.url", "1.0.0")
                .append("org.osgi.util", "1.4.0")
                .append("org.osgi.util.tracker", "1.4.0");
    }

    public PackagesBuilder withJdkDefaults() {
        appendListedPackages(JDK5_PACKAGES_FILE);

        if (System.getProperty("java.specification.version").equals("1.6")) {
            appendListedPackages(JDK6_PACKAGES_FILE);
        }
        if (System.getProperty("java.specification.version").equals("1.7")) {
            appendListedPackages(JDK7_PACKAGES_FILE);
        }
        return this;
    }

    public String toString() {
        if (toStringCache == null) {
            toStringCache = buffer.toString();
        }
        return toStringCache;
    }

    @SuppressWarnings({"unchecked"})
    private void appendListedPackages(String resourcePath) {
        try {
            URL url = Resources.getResource(resourcePath);
            Resources.readLines(url, Charsets.UTF_8, new PackageLineProcessor(this));
        } catch (IOException e) {
            String msg = "Unable to acquire or use package list classpath file '" + resourcePath + "'.";
            throw new IllegalStateException(msg, e);
        }
    }

    private static class PackageLineProcessor implements LineProcessor {

        private final PackagesBuilder pb;

        public PackageLineProcessor(PackagesBuilder pb) {
            this.pb = pb;
        }

        @Override
        public Object getResult() {
            return null;
        }

        @Override
        public boolean processLine(String line) throws IOException {
            line = Strings.emptyToNull(line);
            if (line != null && line.charAt(0) != '#') {
                pb.append(line);
            }
            return true;
        }
    }

}
