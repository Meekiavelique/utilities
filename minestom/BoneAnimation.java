package com.meekdev.animation;

import net.minestom.server.coordinate.Vec;
import java.util.Map;

public class BoneAnimation {
    private final Map<Double, Vec> rotationKeyframes;
    private final Map<Double, Vec> positionKeyframes;
    private final Map<Double, Vec> scaleKeyframes;

    public BoneAnimation(Map<Double, Vec> rotationKeyframes, Map<Double, Vec> positionKeyframes, Map<Double, Vec> scaleKeyframes) {
        this.rotationKeyframes = rotationKeyframes;
        this.positionKeyframes = positionKeyframes;
        this.scaleKeyframes = scaleKeyframes;
    }

    public Vec getRotationAt(double time) {
        return interpolateKeyframes(rotationKeyframes, time);
    }

    public Vec getPositionAt(double time) {
        return interpolateKeyframes(positionKeyframes, time);
    }

    public Vec getScaleAt(double time) {
        Vec interpolated = interpolateKeyframes(scaleKeyframes, time);
        if (interpolated.isZero()) return Vec.ONE;
        return interpolated.add(1, 1, 1);
    }

    private Vec interpolateKeyframes(Map<Double, Vec> keyframes, double time) {
        if (keyframes.isEmpty()) return Vec.ZERO;

        Double prevTime = null;
        Double nextTime = null;

        for (Double keyTime : keyframes.keySet()) {
            if (keyTime <= time) {
                prevTime = keyTime;
            } else {
                nextTime = keyTime;
                break;
            }
        }

        if (prevTime == null) {
            return keyframes.values().iterator().next();
        }
        if (nextTime == null) {
            return keyframes.get(prevTime);
        }

        Vec prevValue = keyframes.get(prevTime);
        Vec nextValue = keyframes.get(nextTime);

        double t = (time - prevTime) / (nextTime - prevTime);
        t = Math.max(0, Math.min(1, t));

        return new Vec(
                lerp(prevValue.x(), nextValue.x(), t),
                lerp(prevValue.y(), nextValue.y(), t),
                lerp(prevValue.z(), nextValue.z(), t)
        );
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}