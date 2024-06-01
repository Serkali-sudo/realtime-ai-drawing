package com.serhat.realtimeaidrawing.drawing;

import android.graphics.Path;

public class Stroke {

    public int colour;
    public float strokeWidth;
    public Path path;
    public boolean isErasePath;

    public Stroke(int colour, float width, Path path, boolean erase) {
        this.colour = colour;
        this.strokeWidth = width;
        this.path = path;
        this.isErasePath = erase;
    }

}