// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.io.XmlWriter;

/**
 * Common abstract implementation of other download tasks
 * @param <T> The downloaded data type
 * @since 2322
 */
public abstract class AbstractDownloadTask<T> implements DownloadTask {
    private final List<Object> errorMessages;
    private boolean canceled;
    private boolean failed;
    protected T downloadedData;

    public AbstractDownloadTask() {
        errorMessages = new ArrayList<>();
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    protected final void rememberErrorMessage(String message) {
        errorMessages.add(message);
    }

    protected final void rememberException(Exception exception) {
        errorMessages.add(exception);
    }

    protected final void rememberDownloadedData(T data) {
        this.downloadedData = data;
    }

    /**
     * Replies the downloaded data.
     * @return The downloaded data.
     */
    public final T getDownloadedData() {
        return downloadedData;
    }

    @Override
    public List<Object> getErrorObjects() {
        return errorMessages;
    }

    @Override
    public String acceptsDocumentationSummary() {
        StringBuilder buff = new StringBuilder("<tr><td>");
        buff.append(getTitle())
            .append(":</td><td>");
        String[] patterns = getPatterns();
        if (patterns.length > 0) {
            buff.append("<ul>");
            for (String pattern: patterns) {
                buff.append("<li>")
                    .append(XmlWriter.encode(pattern))
                    .append("</li>");
            }
            buff.append("</ul>");
        }
        buff.append("</td></tr>");
        return buff.toString();
    }

    // Can be overridden for more complex checking logic
    public boolean acceptsUrl(String url) {
        if (url == null) return false;
        for (String p: getPatterns()) {
            if (url.matches(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check / decide if the task is safe for remotecontrol.
     *
     * Keep in mind that a potential attacker has full control over the content
     * of the file that will be downloaded.
     * If it is possible to run arbitrary code or write to the local file
     * system, then the task is (obviously) not save for remote execution.
     *
     * The default value is false = unsafe. Override in a subclass to
     * allow running the task via remotecontol.
     *
     * @return true if it is safe to download and open any file of the
     * corresponding format, false otherwise
     */
    public boolean isSafeForRemotecontrolRequests() {
        return false;
    }

    @Override
    public boolean acceptsUrl(String url, boolean isRemotecontrol) {
        if (isRemotecontrol && !isSafeForRemotecontrolRequests()) return false;
        return acceptsUrl(url);
    }

    // Default name to keep old plugins compatible
    @Override
    public String getTitle() {
        return getClass().getName();
    }

    @Override
    public String toString() {
        return this.getTitle();
    }

    // Default pattern to keep old plugins compatible
    @Override
    public String[] getPatterns() {
        return new String[]{};
    }

}
