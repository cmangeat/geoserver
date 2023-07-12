/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogBeforeAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.platform.ExtensionPriority;
import org.geotools.util.logging.Logging;

public class CatalogEventDispatcher {
    /** logger */
    private static final Logger LOGGER = Logging.getLogger(CatalogImpl.class);

    /** listeners */
    protected List<CatalogListener> listeners = new CopyOnWriteArrayList<>();

    public Collection<CatalogListener> getListeners() {
        return Collections.unmodifiableCollection(listeners);
    }

    public void addListener(CatalogListener listener) {
        listeners.add(listener);
        Collections.sort(listeners, ExtensionPriority.COMPARATOR);
    }

    public void removeListener(CatalogListener listener) {
        listeners.remove(listener);
    }

    public void removeListeners(Class<? extends CatalogListener> listenerClass) {
        new ArrayList<>(listeners)
                .stream()
                        .filter(l -> listenerClass.isInstance(l))
                        .forEach(l -> listeners.remove(l));
    }

    static long[] delays = new long[100];

    public static void resetDelay() {
        delays = new long[100];
    }

    public static int deleteCount = 0;

    public static void printDelay() {
        for (int i = 0; i < 14; i++) {
            System.err.println("     listener " + i + " took " + delays[i] + " ms.");
        }
    }

    public void dispatch(CatalogEvent event) {
        CatalogException toThrow = null;

        if (event instanceof CatalogRemoveEvent) {
            deleteCount++;
        }

        int i = 0;
        long start;
        for (CatalogListener listener : listeners) {
            start = System.currentTimeMillis();
            try {
                if (event instanceof CatalogAddEvent) {
                    listener.handleAddEvent((CatalogAddEvent) event);
                } else if (event instanceof CatalogRemoveEvent) {
                    listener.handleRemoveEvent((CatalogRemoveEvent) event);
                } else if (event instanceof CatalogModifyEvent) {
                    listener.handleModifyEvent((CatalogModifyEvent) event);
                } else if (event instanceof CatalogPostModifyEvent) {
                    listener.handlePostModifyEvent((CatalogPostModifyEvent) event);
                } else if (event instanceof CatalogBeforeAddEvent) {
                    listener.handlePreAddEvent((CatalogBeforeAddEvent) event);
                }
            } catch (Throwable t) {
                if (t instanceof CatalogException && toThrow == null) {
                    toThrow = (CatalogException) t;
                } else if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                            Level.WARNING, "Catalog listener threw exception handling event.", t);
                }
            }
            delays[i] = delays[i] + System.currentTimeMillis() - start;
            i++;
        }

        if (toThrow != null) {
            throw toThrow;
        }
    }

    public void syncTo(CatalogEventDispatcher dispatcher) {
        dispatcher.listeners = listeners;
    }
}
