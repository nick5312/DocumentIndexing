package util;

import java.util.Arrays;

interface VectorApplier {
    float apply(int index, float value);
}

public class FloatVector {
    private final float[] data;

    public FloatVector(int length) {
        this.data = new float[length];
    }

    public FloatVector(String data, int length) {
        this.data = new float[length];
        var strNumbers = data.split("\\s+");
        this.apply((index, value) -> Float.parseFloat(strNumbers[index]));
    }

    public boolean zeroed() {
        for (var value : this.data) {
            if (Float.compare(value, 0.0f) != 0) {
                return false;
            }
        }
        return true;
    }

    public int getLength() {
        return data.length;
    }

    public void add(FloatVector rhs) {
        this.apply((index, value) -> value + rhs.data[index]);
    }

    public void div(float rhs) {
        if (Float.compare(rhs, 0.0f) != 0) {
            this.apply((index, value) -> value / rhs);
        }
    }

    public void set(int index, float value) {
        this.data[index] = value;
    }

    public float get(int index) {
        return this.data[index];
    }

    public void normalize() {
        this.div(this.getMagnitude());
    }

    public float getMagnitude() {
        float sum = 0.0f;
        for (var value : this.data) {
            sum += Math.pow(value, 2);
        }
        return (float)Math.sqrt(sum);
    }

    private void apply(VectorApplier op) {
        for (var i = 0; i < this.data.length; i++) {
            this.data[i] = op.apply(i, this.data[i]);
        }
    }

    @Override
    public String toString() {
        return "FloatVector{" +
                "data=" + Arrays.toString(data) +
                '}';
    }
}
