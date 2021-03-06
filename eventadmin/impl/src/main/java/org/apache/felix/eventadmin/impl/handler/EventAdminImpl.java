/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.eventadmin.impl.handler;

import org.apache.felix.eventadmin.impl.tasks.AsyncDeliverTasks;
import org.apache.felix.eventadmin.impl.tasks.DefaultThreadPool;
import org.apache.felix.eventadmin.impl.tasks.SyncDeliverTasks;
import org.apache.felix.eventadmin.impl.util.Matchers;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This is the actual implementation of the OSGi R4 Event Admin Service (see the
 * Compendium 113 for details). The implementation uses a <tt>HandlerTasks</tt>
 * in order to determine applicable <tt>EventHandler</tt> for a specific event and
 * subsequently dispatches the event to the handlers via <tt>DeliverTasks</tt>.
 * To do this, it uses two different <tt>DeliverTasks</tt> one for asynchronous and
 * one for synchronous event delivery depending on whether its <tt>post()</tt> or
 * its <tt>send()</tt> method is called. Note that the actual work is done in the
 * implementations of the <tt>DeliverTasks</tt>. Additionally, a stop method is
 * provided that prevents subsequent events to be delivered.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventAdminImpl implements EventAdmin
{
    /** The tracker for the event handlers. */
    private volatile EventHandlerTracker tracker;

    // The asynchronous event dispatcher
    private final AsyncDeliverTasks m_postManager;

    // The synchronous event dispatcher
    private final SyncDeliverTasks m_sendManager;

    // matchers for ignore topics
    private Matchers.Matcher[] m_ignoreTopics;

    /**
     * The constructor of the <tt>EventAdmin</tt> implementation.
     *
     * @param syncPool The synchronous thread pool
     * @param asyncPool The asynchronous thread pool
     */
    public EventAdminImpl(
                    final BundleContext bundleContext,
                    final DefaultThreadPool syncPool,
                    final DefaultThreadPool asyncPool,
                    final int timeout,
                    final String[] ignoreTimeout,
                    final boolean requireTopic,
                    final String[] ignoreTopics)
    {
        checkNull(syncPool, "syncPool");
        checkNull(asyncPool, "asyncPool");

        this.tracker = new EventHandlerTracker(bundleContext);
        this.tracker.update(ignoreTimeout, requireTopic);
        this.tracker.open();
        m_sendManager = new SyncDeliverTasks(syncPool, timeout);
        m_postManager = new AsyncDeliverTasks(asyncPool, m_sendManager);
        m_ignoreTopics = Matchers.createEventTopicMatchers(ignoreTopics);
    }

    /**
     * Check if the event admin is active and return the tracker
     * @return The tracker
     * @throws IllegalArgumentException if the event admin has been stopped
     */
    private EventHandlerTracker getTracker() {
        final EventHandlerTracker localTracker = tracker;
        if ( localTracker == null ) {
            throw new IllegalStateException("The EventAdmin is stopped");
        }
        return localTracker;
    }

    /**
     * Check whether the topic should be delivered at all
     */
    private boolean checkTopic( final Event event )
    {
        boolean result = true;
        if ( this.m_ignoreTopics != null )
        {
            for(final Matchers.Matcher m : this.m_ignoreTopics)
            {
                if ( m.match(event.getTopic()) )
                {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Post an asynchronous event.
     *
     * @param event The event to be posted by this service
     *
     * @throws IllegalStateException - In case we are stopped
     *
     * @see org.osgi.service.event.EventAdmin#postEvent(org.osgi.service.event.Event)
     */
    @Override
    public void postEvent(final Event event)
    {
        if ( checkTopic(event) )
        {
            m_postManager.execute(this.getTracker().getHandlers(event), event);
        }
    }

    /**
     * Send a synchronous event.
     *
     * @param event The event to be send by this service
     *
     * @throws IllegalStateException - In case we are stopped
     *
     * @see org.osgi.service.event.EventAdmin#sendEvent(org.osgi.service.event.Event)
     */
    @Override
    public void sendEvent(final Event event)
    {
        if ( checkTopic(event) )
        {
            m_sendManager.execute(this.getTracker().getHandlers(event), event, false);
        }
    }

    /**
     * This method can be used to stop the delivery of events.
     */
    public void stop()
    {
        this.tracker.close();
        this.tracker = null;
    }

    /**
     * Update the event admin with new configuration.
     */
    public void update(final int timeout,
                    final String[] ignoreTimeout,
                    final boolean requireTopic,
                    final String[] ignoreTopics)
    {
        this.tracker.close();
        this.tracker.update(ignoreTimeout, requireTopic);
        this.m_sendManager.update(timeout);
        this.tracker.open();
        this.m_ignoreTopics = Matchers.createEventTopicMatchers(ignoreTopics);
    }

    /**
     * This is a utility method that will throw a <tt>NullPointerException</tt>
     * in case that the given object is null. The message will be of the form
     * "${name} + may not be null".
     */
    private void checkNull(final Object object, final String name)
    {
        if (null == object)
        {
            throw new NullPointerException(name + " may not be null");
        }
    }
}
