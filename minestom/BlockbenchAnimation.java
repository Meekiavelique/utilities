package com.meekdev.animation;

import java.util.Map;

public class BlockbenchAnimation {
    private final String name;
    private final double length;
    private final boolean loop;
    private final Map<String, BoneAnimation> boneAnimations;

    public BlockbenchAnimation(String name, double length, boolean loop, Map<String, BoneAnimation> boneAnimations) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.boneAnimations = boneAnimations;
    }

    public String getName() {
        return name;
    }

    public double getLength() {
        return length;
    }

    public boolean isLoop() {
        return loop;
    }

    public BoneAnimation getBoneAnimation(String boneName) {
        return boneAnimations.get(boneName);
    }
}