package com.meekdev.animation;
// @emortaldev
public enum PlayerDisplayPart {
    HEAD(0, "animated_java:blueprint/player_display/head", "head"),
    RIGHT_ARM(-1024, "animated_java:blueprint/player_display/right_arm", "right_arm"),
    LEFT_ARM(-2048, "animated_java:blueprint/player_display/left_arm", "left_arm"),
    TORSO(-3072, "animated_java:blueprint/player_display/torso", "body"),
    RIGHT_LEG(-4096, "animated_java:blueprint/player_display/right_leg", "right_leg"),
    LEFT_LEG(-5120, "animated_java:blueprint/player_display/left_leg", "left_leg");

    private final double yTranslation;
    private final String customModelData;
    private final String boneName;

    PlayerDisplayPart(double yTranslation, String customModelData, String boneName) {
        this.yTranslation = yTranslation;
        this.customModelData = customModelData;
        this.boneName = boneName;
    }

    public double getYTranslation() {
        return yTranslation;
    }

    public String getCustomModelData() {
        return customModelData;
    }

    public String getBoneName() {
        return boneName;
    }
}