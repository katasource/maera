/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.maera.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Simple interface for objects that are sources for an {@link java.io.InputStream}.
 * <p/>
 * <p>This is the base interface for Spring's more extensive {@link Resource} interface.
 * <p/>
 * <p>For single-use streams, an {@code InputStreamResource} can be used for any
 * given <code>InputStream</code>. Additionally, the {@code ByteArrayResource} or any
 * file-based <code>Resource</code> implementation can be used as a concrete
 * instance, allowing one to read the underlying content stream multiple times.
 * This makes this interface useful as an abstract content source for mail
 * attachments, for example.
 * <p/>
 * Shamelessly copied from the Spring 3.0.3 distribution to prevent a runtime dependency on Spring for all API
 * implementations.  All license conditions and author attribution has remained in tact.
 *
 * @author Juergen Hoeller
 * @see java.io.InputStream
 * @see Resource
 *      -- @see InputStreamResource
 *      -- @see ByteArrayResource
 * @since 20.01.2004
 */
public interface InputStreamSource {

    /**
     * Return an {@link java.io.InputStream}.
     * <p>It is expected that each call creates a <i>fresh</i> stream.
     * <p>This requirement is particularly important when you consider an API such
     * as JavaMail, which needs to be able to read the stream multiple times when
     * creating mail attachments. For such a use case, it is <i>required</i>
     * that each <code>getInputStream()</code> call returns a fresh stream.
     *
     * @return an {@link java.io.InputStream}
     * @throws java.io.IOException if the stream could not be opened
     */
    //TODO - throw an IOException?  Or create a common ResourceException class?
    InputStream getInputStream() throws IOException;

}
