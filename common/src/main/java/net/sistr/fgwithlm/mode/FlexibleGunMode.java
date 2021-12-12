package net.sistr.fgwithlm.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sistr.flexibleguns.util.*;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

//ロジック概要
//ターゲットが居る場合常に実行
//常に横移動し、かつプレイヤーと射線が被らないように動く
//視線が通るなら射撃
//射線を味方が遮るなら即座に中断
public class FlexibleGunMode extends Mode {
    protected final LittleMaidEntity maid;
    protected final int maxModeHoldTime;
    protected int modeHoldTime;
    protected Vec3d targetAt;
    protected Vec3d targetAtVelocity;
    protected boolean prevCanSee = true;
    protected boolean rightMove;
    protected boolean frontMove;
    protected int soundTimer;

    public FlexibleGunMode(ModeType<? extends Mode> modeType, String name, LittleMaidEntity maid, int maxModeHoldTime) {
        super(modeType, name);
        this.maid = maid;
        this.maxModeHoldTime = maxModeHoldTime;
    }

    @Override
    public boolean shouldExecute() {
        return this.maid.getTarget() != null && this.maid.getTarget().isAlive();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return 0 < modeHoldTime || shouldExecute();
    }

    @Override
    public void startExecuting() {
        if (this.maid.getTarget() == null) {
            throw new RuntimeException("ターゲットが存在しない。");
        }
        modeHoldTime = maxModeHoldTime;
        Vec3d maidPos = this.maid.getCameraPosVec(1F);
        targetAt = maidPos.add(
                this.maid.getRotationVec(1f)
                        .multiply(
                                maidPos.subtract(this.maid.getTarget().getPos()).length()
                        )
        );
        targetAtVelocity = Vec3d.ZERO;
        this.maid.setAimingBow(true);
        this.maid.play(LMSounds.SIGHTING);
    }

    @Override
    public void tick() {
        modeHoldTime--;
        soundTimer--;

        //横移動
        LivingEntity target = this.maid.getTarget();
        if (target != null && target.isAlive() && this.maid.getRandom().nextInt(5) == 0) {
            this.maid.getTameOwner().ifPresent(owner -> {
                double targetDistSq = target.squaredDistanceTo(owner);
                //16ブロックより遠いなら近づく
                frontMove = 16 * 16 < targetDistSq;

                Vec3d tToOVec = owner.getCameraPosVec(1f).subtract(target.getCameraPosVec(1f)).multiply(1, 0, 1).normalize();
                Vec3d tToMVec = maid.getCameraPosVec(1f).subtract(target.getCameraPosVec(1f)).multiply(1, 0, 1).normalize();
                double oYaw = MathHelper.atan2(tToOVec.x, tToOVec.z) * (180 / Math.PI);
                double mYaw = MathHelper.atan2(tToMVec.x, tToMVec.z) * (180 / Math.PI);
                //ターゲットからご主人を見て、メイドが-45度以上かつ0度以下、または45度以上、のいずれかなら右に移動
                rightMove = (-45 < mYaw - oYaw && mYaw - oYaw < 0) || 45 < mYaw - oYaw;
            });
        }
        this.maid.getMoveControl().strafeTo(frontMove ? 0.5f : -0.5f, rightMove ? 1.0f : -1.0f);

        ItemStack stack = this.maid.getMainHandStack();
        ItemInstance itemIns = ((CustomItemStack) (Object) stack).getItemInstanceFG();
        if (itemIns instanceof ShootableItem) {
            gunTick((ShootableItem) itemIns);
        }
    }

    protected void gunTick(ShootableItem gunInstance) {
        boolean isZoom = ((ZoomableEntity) this.maid).isZoom_FG();
        ((Inputable) this.maid).inputKeyFG(Input.FIRE, false);
        ((Inputable) this.maid).inputKeyFG(Input.RELOAD, false);
        ((Inputable) this.maid).inputKeyFG(Input.ZOOM, false);
        LivingEntity target = this.maid.getTarget();
        //ターゲット死亡のお知らせ
        if (target == null || !target.isAlive()) {
            //モードの終わり際にリロードする
            if (modeHoldTime == 5) {
                ((Inputable) this.maid).inputKeyFG(Input.RELOAD, true);
                if (isZoom) {
                    ((Inputable) this.maid).inputKeyFG(Input.ZOOM, true);
                }
            }
            return;
        }
        Vec3d maidPos = this.maid.getCameraPosVec(1);
        Vec3d lookFor = this.maid.getRotationVector();
        PrevEntity prevTarget = ((PrevEntityGetter) target).getPrevEntity(4);
        Vec3d targetPos = prevTarget.getPos().add(0, prevTarget.getEyeHeight(), 0);
        Vec3d forTargetVec = targetPos.subtract(targetAt).normalize();

        targetAtVelocity = targetAtVelocity.add(forTargetVec.multiply(0.1, 0.05, 0.1));
        targetAtVelocity = targetAtVelocity.multiply(0.95, 0.9, 0.95);
        targetAt = targetAt.add(targetAtVelocity);

        ((ServerWorld) this.maid.world).spawnParticles(DustParticleEffect.DEFAULT,
                targetAt.getX(), targetAt.getY(), targetAt.getZ(), 1, 0, 0, 0, 0);

        this.maid.getLookControl().lookAt(targetAt.getX(), targetAt.getY(), targetAt.getZ(), 30, 30);
        this.maid.yaw = this.maid.headYaw;
        //ターゲットへの視線チェックは10tickごと
        boolean canSee = this.maid.age % 10 == 0 ? this.maid.getVisibilityCache().canSee(target) : prevCanSee;
        prevCanSee = canSee;

        if (canSee) {
            modeHoldTime = maxModeHoldTime;
        }

        //狙いとターゲットが1以下かつ、視線が通っていれば射撃
        if (targetAt.subtract(targetPos).lengthSquared() < 1 && canSee) {
            if (soundTimer < 0) {
                soundTimer = 40;
                this.maid.play(LMSounds.SHOOT_BURST);
            }
            ((Inputable) this.maid).inputKeyFG(Input.FIRE, true);
            if (!isZoom) {
                ((Inputable) this.maid).inputKeyFG(Input.ZOOM, true);
            }
        }

        //射線上に味方居たら射撃中止

        float range = 32;/*MathHelper.clamp(
                gunInstance.getDecay() * gunInstance.getVelocity(),
                1,
                Math.min((float) targetPos.subtract(maidPos).length() + 5, 32)
        );*/
        Vec3d end = maidPos.add(lookFor.multiply(range));
        BlockHitResult bResult = this.maid.world.raycast(new RaycastContext(maidPos, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this.maid));
        if (bResult.getType() == HitResult.Type.BLOCK) {
            end = bResult.getPos();
            range = (float) maidPos.subtract(end).length();
        }

        float acc = gunInstance.getInAccuracy(this.maid) + 5;
        this.maid.world.getOtherEntities(
                this.maid,
                this.maid.getBoundingBox().stretch(lookFor.multiply(range)).expand(1),
                e -> {
                    if (e instanceof LivingEntity && this.maid.isFriend((LivingEntity) e)) {
                        Vec3d ePos = e.getCameraPosVec(1f);
                        float eWidthRange = getDeg((float) maidPos.subtract(ePos).length(), e.getWidth() / 2);
                        float eYaw = getTargetYaw(maidPos, ePos);
                        return inDegRange(this.maid.yaw, eYaw, acc + eWidthRange);
                    }
                    return false;
                }
        ).stream().findAny().ifPresent(e -> ((Inputable) this.maid).inputKeyFG(Input.FIRE, false));
    }

    @Override
    public void resetTask() {
        modeHoldTime = 0;
        this.maid.setAimingBow(false);
        this.maid.getMoveControl().strafeTo(0, 0);
        ((Inputable) this.maid).inputKeyFG(Input.FIRE, false);
        ((Inputable) this.maid).inputKeyFG(Input.RELOAD, false);
        ((Inputable) this.maid).inputKeyFG(Input.ZOOM, false);
    }

    //底辺の長さと高さから角度を取る
    protected float getDeg(float length, float height) {
        return (float) (Math.atan(height / length) * 180.0 / Math.PI);
    }

    protected float getTargetYaw(Vec3d pos, Vec3d target) {
        double x = target.getX() - pos.getX();
        double z = target.getZ() - pos.getZ();
        double rad = 180.0 / Math.PI;
        return (float) (MathHelper.atan2(z, x) * rad) - 90.0F;
    }

    public boolean inDegRange(float lookYaw, float toTargetYaw, float yawRange) {
        float yawSub = Math.abs(MathHelper.wrapDegrees(lookYaw - toTargetYaw));
        return yawSub <= yawRange;
    }

}
