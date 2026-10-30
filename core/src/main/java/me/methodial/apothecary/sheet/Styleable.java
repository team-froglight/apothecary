package me.methodial.apothecary.sheet;

import java.util.Set;

/** What a selector needs to know about an element. */
public interface Styleable {
    /** Type name, e.g. "button", "text", "slider". */
    String typeName();

    String id();

    Set<String> classes();

    /** Dynamic state: hover, active, focus, checked, disabled, or any custom state. */
    boolean hasState(String state);

    Styleable styleParent();

    /** 0-based index among the parent's children. */
    int indexInParent();

    int siblingCount();
}
