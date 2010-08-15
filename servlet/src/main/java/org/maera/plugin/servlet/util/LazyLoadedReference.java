/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */
package org.maera.plugin.servlet.util;

import com.atlassian.util.concurrent.LazyReference;

/**
 * Thread-safe lock-less (see note) reference that is not constructed until
 * required. This class is used to maintain a reference to an object that is
 * expensive to create and must be constructed once and once only. Therefore
 * this reference behaves as though the <code>final</code> keyword has been used
 * (you cannot reset it once it has been constructed).
 * <p/>
 * When using this class you need to implement the {@link #create()} method to
 * return the object this reference will hold.
 * <p/>
 * For instance:
 * <p/>
 * <pre>
 * final LazyLoadedReference ref = new LazyLoadedReference()
 * {
 *     protected Object create() throws Exception
 *     {
 *         // Do some useful object construction here
 *         return new MyObject();
 *     }
 * };
 * </pre>
 * <p/>
 * Then call to get a reference to the object:
 * <p/>
 * <pre>
 *   MyObject myLazyLoadedObject = (MyObject) ref.get()
 * </pre>
 * <p/>
 * <strong>Note:</strong> Copied from JIRA
 * com.atlassian.jira.util.concurrent.ThreadsafeLazyLoadedReference and modified
 * to use generics and java.util.concurrent.
 *
 * @since 2.1.0
 * @deprecated since 2.5.0 use {@link LazyReference} directly instead.
 */
@Deprecated
public abstract class LazyLoadedReference<V> extends LazyReference<V> {
}