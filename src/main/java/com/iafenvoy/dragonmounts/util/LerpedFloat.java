package com.iafenvoy.dragonmounts.util;

import net.minecraft.util.math.MathHelper;

public class LerpedFloat {
    protected float current;
    protected float previous;

    public LerpedFloat() {
        this.current = this.previous = 0;
    }

    public LerpedFloat(float start) {
        this.current = this.previous = start;
    }

    public float get(float x) {
        return MathHelper.clampedLerp(this.previous, this.current, x);
    }

    public float get() {
        return this.current;
    }

    public void set(float value) {
        this.sync();
        this.current = value;
    }

    public void add(float value) {
        this.sync();
        this.current += value;
    }

    public void sync() {
        this.previous = this.current;
    }

    public float getPrevious() {
        return this.previous;
    }

    public static Clamped unit() {
        return new Clamped(0, 1);
    }

    /**
     * Clamped Implementation.
     * Basically just ensure that the value stays clamped within the specified {@link Clamped#min}-{@link Clamped#max} bounds.
     */
    public static class Clamped extends LerpedFloat {
        private final float min;
        private final float max;

        public Clamped(float start, float min, float max) {
            super(MathHelper.clamp(start, min, max));
            this.min = min;
            this.max = max;
        }

        public Clamped(float min, float max) {
            this(0, min, max);
        }

        @Override
        public void set(float value) {
            super.set(MathHelper.clamp(value, this.min, this.max));
        }

        @Override
        public void add(float value) {
            super.add(value);
            this.current = MathHelper.clamp(this.current, this.min, this.max);
        }

        public float getMin() {
            return this.min;
        }

        public float getMax() {
            return this.max;
        }
    }
}