package me.methodial.apothecary.event;

import me.methodial.apothecary.node.Node;

/** Base event. Events bubble from the target up to the root unless {@link #stopPropagation()} is called. */
public class UIEvent {
    private Node target;
    private Node currentTarget;
    private boolean propagationStopped;
    private boolean defaultPrevented;

    public Node target() { return target; }
    public Node currentTarget() { return currentTarget; }

    public void stopPropagation() { propagationStopped = true; }
    public boolean isPropagationStopped() { return propagationStopped; }

    /** Marks the event as handled: e.g. a consumed key press won't close the screen. */
    public void preventDefault() { defaultPrevented = true; }
    public boolean isDefaultPrevented() { return defaultPrevented; }

    /** Framework use: sets dispatch targets. */
    public void dispatching(Node target, Node current) {
        this.target = target;
        this.currentTarget = current;
    }
}
