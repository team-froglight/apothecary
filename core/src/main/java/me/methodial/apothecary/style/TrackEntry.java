package me.methodial.apothecary.style;

/** One entry of a grid track list: a single {@link TrackSize} or a {@link TrackSize.Repeat} group. */
public sealed interface TrackEntry permits TrackSize, TrackSize.Repeat { }
