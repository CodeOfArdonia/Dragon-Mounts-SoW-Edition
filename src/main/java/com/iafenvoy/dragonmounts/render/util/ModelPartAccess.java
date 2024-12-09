package com.iafenvoy.dragonmounts.render.util;

public interface ModelPartAccess {
    float getXScale();

    float getYScale();

    float getZScale();

    void setXScale(float x);

    void setYScale(float y);

    void setZScale(float z);

    default void setRenderScale(float x, float y, float z) {
        this.setXScale(x);
        this.setYScale(y);
        this.setZScale(z);
    }
}