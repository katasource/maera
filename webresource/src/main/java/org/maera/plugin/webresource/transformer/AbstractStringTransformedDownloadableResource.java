package org.maera.plugin.webresource.transformer;

import org.apache.commons.io.IOUtils;
import org.maera.plugin.servlet.DownloadException;
import org.maera.plugin.servlet.DownloadableResource;

import java.io.*;

/**
 * Abstract class that makes it easy to create transforms that go from string to string.  Override
 * {@link #getEncoding()} to customize the character encoding of the underlying content and transformed content.
 * <p/>
 * For example, here is a minimal transformer that prepends text to the underlying resource:
 * <pre>
 * public class PrefixTransformer implements WebResourceTransformer
 *   {
 *       public DownloadableResource transform(Element configElement, ResourceLocation location, String filePath, DownloadableResource nextResource)
 *       {
 *           return new AbstractStringTransformedDownloadableResource(nextResource)
 *           {
 *               protected String transform(String originalContent)
 *               {
 *                   return "Prefix: "  + originalContent;
 *               }
 *           };
 *       }
 *    }
 * </pre>
 *
 * @since 2.5.0
 */
public abstract class AbstractStringTransformedDownloadableResource extends AbstractTransformedDownloadableResource {

    public AbstractStringTransformedDownloadableResource(DownloadableResource originalResource) {
        super(originalResource);
    }

    public void streamResource(OutputStream out) throws DownloadException {
        ByteArrayOutputStream delegateOut = new ByteArrayOutputStream();
        try {
            getOriginalResource().streamResource(delegateOut);
        }
        catch (DownloadException e) {
            throw e;
        }
        try {
            String originalContent = new String(delegateOut.toByteArray(), getEncoding());
            String transformedContent = transform(originalContent);
            IOUtils.copy(new StringReader(transformedContent.toString()), out, getEncoding());
        }
        catch (UnsupportedEncodingException e) {
            // should never happen
            throw new DownloadException(e);
        }
        catch (IOException e) {
            throw new DownloadException("Unable to stream to the output", e);
        }

    }

    /**
     * @return the encoding used to read the original resource and encode the transformed string
     */
    protected String getEncoding() {
        return "UTF-8";
    }

    /**
     * Override this method to transform the original content into a new format.
     *
     * @param originalContent The original content from the original downloadable resource.
     * @return The transformed content you want returned
     */
    protected abstract String transform(String originalContent);
}
