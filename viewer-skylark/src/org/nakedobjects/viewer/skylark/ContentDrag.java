package org.nakedobjects.viewer.skylark;

import org.apache.log4j.Logger;


/**
 * Details a drag event that affects a view's content (as opposed to the view
 * itself).
 */
public class ContentDrag extends Drag {
    private static final Logger LOG = Logger.getLogger(ContentDrag.class);

    public static ContentDrag create(View source, Location mouseLocation, int modifiers) {
        ContentDrag drag = new ContentDrag(source, mouseLocation, modifiers);

        return drag.dragging == null ? null : drag;
    }

    private final View dragging;
    private Location mouseLocation;
    private View previousTarget;
    private View target;
    private final Workspace workspace;

    /**
     * Creates a new drag event. The source view has its pickup(), and then,
     * exited() methods called on it. The view returned by the pickup method
     * becomes this event overlay view, which is moved continuously so that it
     * tracks the pointer,
     * 
     * @param source
     *                       the view over which the pointer was when this event started
     * @param mouseLocation
     *                       the location within the viewer (the Frame/Applet/Window etc)
     * @param modifiers
     *                       the button and key modifiers (@see java.awt.event.MouseEvent)
     */
    private ContentDrag(View source, Location mouseLocation, int modifiers) {
        super(source, modifiers);

        workspace = source.getWorkspace();
        dragging = source.pickup(this);
        if (dragging != null) {
            source.exited();
            LOG.debug("pickup " + this);
            this.mouseLocation = mouseLocation;
            setDraggingLocation();
            source.getViewManager().setOverlayView(dragging);
            source.getViewManager().showHandCursor();
        }
    }

    /**
     * Cancels drag by calling dragOut() on the current target, and changes the
     * cursor back to the default.
     */
    protected void cancel() {
        if (target != null) {
            target.dragOut(this);
        }
        getSourceView().getViewManager().clearOverlayView(dragging);
        getSourceView().getViewManager().showDefaultCursor();
    }

    protected void drag() {
        if (dragging != null) {
            dragging.markDamaged();
            setDraggingLocation();
            dragging.markDamaged();
        }

        if (target != previousTarget) {
            if (previousTarget != null) {
                LOG.debug("drag out " + previousTarget);
                previousTarget.dragOut(this);
                previousTarget = null;
            }

            LOG.debug("drag in " + target);
            target.dragIn(this);
            previousTarget = target;
        }
    }

    /**
     * Ends the drag by calling drop() on the current target, and changes the
     * cursor back to the default.
     */
    protected void end() {
        LOG.debug("drop on " + target);
        target.drop(this);
        getSourceView().getViewManager().clearOverlayView(dragging);
        getSourceView().getViewManager().showDefaultCursor();
    }

    /**
     * Returns the Content object from the source view.
     */
    public Content getSourceContent() {
        return getSourceView().getContent();
    }

    public View getSourceView() {
        return view;
    }

    public Location getTargetLocation() {
        Location location = new Location(mouseLocation);
        location.subtract(target.getAbsoluteLocation());
        return location;
    }

    /**
     * Returns the current target view.
     */
    public View getTargetView() {
        return target;
    }

    private void setDraggingLocation() {
        Location location = new Location(mouseLocation);
        dragging.setLocation(location);
        workspace.limitBounds(dragging);
    }

    public void subtract(int x, int y) {
    //      mouseLocation.move(x, y);
    }

    public String toString() {
        return "ContentDrag [" + super.toString() + "]";
    }

    /**
     * Captures the latest pointer information (target and location).
     */
    protected void update(Location mouseLocation, View target) {
        this.mouseLocation = mouseLocation;
        this.target = target;
    }
}

/*
 * Naked Objects - a framework that exposes behaviourally complete business
 * objects directly to the user. Copyright (C) 2000 - 2003 Naked Objects Group
 * Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address
 * of Naked Objects Group is Kingsway House, 123 Goldworth Road, Woking GU21
 * 1NR, UK).
 */