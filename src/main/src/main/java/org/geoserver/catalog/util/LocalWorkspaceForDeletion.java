package org.geoserver.catalog.util;

import org.geoserver.catalog.WorkspaceInfo;

public class LocalWorkspaceForDeletion {

    static ThreadLocal<WorkspaceInfo> workspace = new ThreadLocal<>();

    public static void set(WorkspaceInfo w) {
        workspace.set(w);
    }

    public static WorkspaceInfo get() {
        return workspace.get();
    }

    public static void remove() {
        workspace.remove();
    }
}
