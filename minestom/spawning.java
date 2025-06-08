package com.meekdev;

import com.meekdev.animation.PlayerDisplayPart;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import com.meekdev.animation.BoneAnimation;
import com.meekdev.animation.BlockbenchAnimation;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerModel {

    public float getRotation() {
        return currentModelYaw;
    }

    public static class NoTickingEntity extends Entity {
        public NoTickingEntity(@NotNull EntityType entityType) {
            super(entityType);
            this.hasPhysics = false;
            setNoGravity(true);
            setAutoViewable(true);
        }

        @Override
        public void tick(long time) {
        }
    }

    private final Instance instance;
    private final Player targetPlayer;
    private final PlayerSkin skin;
    private final Entity rootEntity;
    private final Map<String, Entity> boneEntities = new HashMap<>();
    private final Map<String, Vec> baseBonePositions = new HashMap<>();

    private BlockbenchAnimation currentAnimation;
    private double animationTime = 0.0;
    private boolean isPlaying = false;
    private String lastPlayedAnimation = "idle";

    private float currentModelYaw = 0.0f;
    private float currentModelPitch = 0.0f;
    private double modelHeightOffset = 1.0;

    private double lastMovementSpeed = 0.0;
    private long lastMovementTime = 0;
    private static final long ANIMATION_TRANSITION_DELAY = 10;

    private static final double PIXELS_TO_BLOCKS = 1.0 / 16.0;
    private static final double TICKS_PER_SECOND = 20.0;

    public PlayerModel(Instance instance, Player targetPlayer) {
        this.instance = instance;
        this.targetPlayer = targetPlayer;
        this.skin = targetPlayer.getSkin();
        this.rootEntity = new NoTickingEntity(EntityType.ITEM_DISPLAY);
        this.rootEntity.setInvisible(true);
        this.rootEntity.setNoGravity(true);

        setupBaseBoneTransforms();
        createBoneEntities();
    }

    private void setupBaseBoneTransforms() {
        baseBonePositions.put("head", new Vec(0, 0.5, 0));
        baseBonePositions.put("right_arm", new Vec(0.25, 0.5, 0));
        baseBonePositions.put("left_arm", new Vec(-0.25, 0.5, 0));
        baseBonePositions.put("body", new Vec(0, 0.5, 0));
        baseBonePositions.put("right_leg", new Vec(-0.125, -0.3, 0));
        baseBonePositions.put("left_leg", new Vec(0.125, -0.3, 0));
    }

    private void createBoneEntities() {
        for (PlayerDisplayPart part : PlayerDisplayPart.values()) {
            Entity entity = new NoTickingEntity(EntityType.ITEM_DISPLAY);

            entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
                meta.setPosRotInterpolationDuration(3);
                meta.setTransformationInterpolationDuration(3);
                meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_RIGHT_HAND);
                meta.setViewRange(0.6f);

                ItemStack itemStack = ItemStack.builder(Material.PLAYER_HEAD)
                        .set(ItemComponent.ITEM_MODEL, part.getCustomModelData())
                        .set(ItemComponent.PROFILE, new HeadProfile(skin))
                        .build();

                meta.setItemStack(itemStack);
                meta.setScale(new Vec(1, 1, 1));
                meta.setTranslation(new Vec(0, part.getYTranslation(), 0));
                meta.setLeftRotation(new float[]{0.0f, 0.0f, 0.0f, 1.0f});
                meta.setRightRotation(new float[]{0.0f, 0.0f, 0.0f, 1.0f});
            });

            boneEntities.put(part.getBoneName(), entity);
        }
    }

    public void spawn() {
        targetPlayer.setInvisible(true);
        rootEntity.setInstance(instance, targetPlayer.getPosition().withView(0, 0));
        for (Entity boneEntity : boneEntities.values()) {
            rootEntity.addPassenger(boneEntity);
        }
        playAnimation("idle");
        updateBoneTransforms();
    }

    public void updateAndAnimate(double speed) {
        long currentTime = System.currentTimeMillis() / 50;

        String newAnimation = determineAnimationSmooth(speed, currentTime);
        if (!newAnimation.equals(lastPlayedAnimation)) {
            playAnimation(newAnimation);
            lastPlayedAnimation = newAnimation;
        }
        updateAnimationTick();
    }

    private void playAnimation(String animationName) {
        BlockbenchAnimation animation = Main.getAnimations().get(animationName);
        if (animation != null && (currentAnimation == null || !currentAnimation.getName().equals(animationName))) {
            this.currentAnimation = animation;
            this.animationTime = 0.0;
            this.isPlaying = true;
        }
    }

    private String determineAnimationSmooth(double movementSpeed, long currentTime) {
        boolean isMoving = movementSpeed > 0.02;
        boolean wasMoving = lastMovementSpeed > 0.01;

        if (isMoving != wasMoving) {
            if (currentTime - lastMovementTime < ANIMATION_TRANSITION_DELAY) {
                return lastPlayedAnimation;
            }
            lastMovementTime = currentTime;
        }

        lastMovementSpeed = movementSpeed;

        if (isMoving) {
            return "walk";
        }
        return "idle";
    }

    private void updateAnimationTick() {
        if (isPlaying && currentAnimation != null) {
            animationTime += 1.0 / TICKS_PER_SECOND;
            if (animationTime >= currentAnimation.getLength()) {
                if (currentAnimation.isLoop()) {
                    animationTime %= currentAnimation.getLength();
                } else {
                    isPlaying = false;
                    animationTime = currentAnimation.getLength();
                }
            }
        }
        updateBoneTransforms();
    }

    public void setRotation(float yaw, float pitch) {
        this.currentModelYaw = yaw;
        this.currentModelPitch = pitch;
    }

    public void teleport(Pos position) {
        this.rootEntity.teleport(position.withView(0, 0));
    }

    private void updateBoneTransforms() {
        Quaternionf playerRotation = new Quaternionf().rotateY((float) Math.toRadians(-currentModelYaw));
        Quaternionf headPitchRotation = new Quaternionf().rotateX((float) Math.toRadians(-currentModelPitch));

        for (Map.Entry<String, Entity> entry : boneEntities.entrySet()) {
            String boneName = entry.getKey();
            Entity boneEntity = entry.getValue();

            Vec basePosition = baseBonePositions.getOrDefault(boneName, Vec.ZERO);
            Vec animPosition = Vec.ZERO;
            Vec animRotation = Vec.ZERO;
            Vec animScale;

            if (currentAnimation != null) {
                BoneAnimation boneAnim = currentAnimation.getBoneAnimation(boneName);
                if (boneAnim != null) {
                    animPosition = boneAnim.getPositionAt(animationTime).mul(PIXELS_TO_BLOCKS);
                    animRotation = boneAnim.getRotationAt(animationTime);
                    animScale = boneAnim.getScaleAt(animationTime);
                } else {
                    animScale = Vec.ONE;
                }
            } else {
                animScale = Vec.ONE;
            }

            Vec localPosition = basePosition.add(animPosition).add(0, modelHeightOffset, 0);
            Vec rotatedPosition = rotateVectorByQuaternion(localPosition, playerRotation);

            Quaternionf boneRotationQuat = eulerToQuaternion(animRotation);
            Quaternionf finalRotationQuat = new Quaternionf(playerRotation).mul(boneRotationQuat);

            if (boneName.equals("head")) {
                finalRotationQuat.mul(headPitchRotation);
            }

            PlayerDisplayPart part = Arrays.stream(PlayerDisplayPart.values())
                    .filter(p -> p.getBoneName().equals(boneName))
                    .findFirst()
                    .orElse(null);

            boneEntity.editEntityMeta(ItemDisplayMeta.class, meta -> {
                meta.setPosRotInterpolationDuration(3);
                meta.setTransformationInterpolationDuration(3);
                meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_RIGHT_HAND);
                meta.setViewRange(0.6f);

                if (part != null) {
                    Vec finalTranslation = rotatedPosition.add(0, part.getYTranslation(), 0);
                    meta.setTranslation(finalTranslation);
                } else {
                    meta.setTranslation(rotatedPosition);
                }

                meta.setLeftRotation(new float[]{
                        finalRotationQuat.x, finalRotationQuat.y, finalRotationQuat.z, finalRotationQuat.w
                });
                meta.setRightRotation(new float[]{0.0f, 0.0f, 0.0f, 1.0f});
                meta.setScale(animScale);
            });
        }
    }

    private Vec rotateVectorByQuaternion(Vec vector, Quaternionf quaternion) {
        org.joml.Vector3f vec = new org.joml.Vector3f((float) vector.x(), (float) vector.y(), (float) vector.z());
        quaternion.transform(vec);
        return new Vec(vec.x, vec.y, vec.z);
    }

    private Quaternionf eulerToQuaternion(Vec euler) {
        float pitch = (float) Math.toRadians(euler.x());
        float yaw = (float) Math.toRadians(euler.y());
        float roll = (float) Math.toRadians(euler.z());
        return new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);
    }

    public void remove() {
        targetPlayer.setInvisible(false);
        for (Entity boneEntity : boneEntities.values()) {
            boneEntity.remove();
        }
        rootEntity.remove();
    }

    public Entity getRootEntity() {
        return rootEntity;
    }

    public Pos getPosition() {
        return rootEntity.getPosition();
    }
}