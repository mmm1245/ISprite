package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnimationEditor extends Editor {
    private SpriteAnimation animation;
    private UUID movingId;
    private float time;
    private boolean playing;
    public AnimationEditor() {
        this.movingId = null;
        this.animation = new SpriteAnimation();
        this.time = 0;
        this.playing = false;
    }
    @Override
    public void render() {
        super.render();
        AnimatedSprite sprite = ISpriteMain.getInstance().sprite;
        AnimatedSpritePose pose = animation.getPose(time);
        HashMap<UUID, Transform> transforms = pose.getBoneTransforms(sprite, new Transform().lock());
        UUID moused = null;
        Vector3 worldMouse3 = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 worldMouse = new Vector2(worldMouse3.x, worldMouse3.y);
        for(Map.Entry<UUID, Transform> entry : transforms.entrySet()){
            if(entry.getValue().translation.dst(worldMouse) < 10f){
                moused = entry.getKey();
            }
        }

        shapeRenderer.begin();
        UUID finalMoused = moused;
        pose.drawDebugBones(sprite, shapeRenderer, uuid -> uuid.equals(finalMoused)? Color.RED:Color.YELLOW);
        shapeRenderer.end();

        if(movingId != null){
            AnimatedSpriteBone movingBone = sprite.bones.get(movingId);
            if(movingBone != null && movingBone.parent != null) {
                Transform parentTransform = transforms.get(movingBone.parent);
                AnimationTrack track = animation.getTrack(movingId);
                track.translations.addKeyframe(time, worldMouse.cpy().sub(parentTransform.translation).rotateRad(-parentTransform.rotation));
            }
        }

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)){
            if(movingId == null)
                movingId = moused;
            else
                movingId = null;
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
            playing = !playing;
        }
        if(playing){
            time += Gdx.graphics.getDeltaTime();
        }
    }

    @Override
    public boolean scrolled(float v, float v1) {
        if(movingId != null){
            AnimatedSprite sprite = ISpriteMain.getInstance().sprite;
            AnimatedSpriteBone movingBone = sprite.bones.get(movingId);
            if(movingBone != null && movingBone.parent != null) {
                AnimationTrack track = animation.getTrack(movingId);
                track.rotations.addKeyframe(time, animation.getPose(time).getBoneTransforms(sprite, new Transform().lock()).get(movingId).rotation-v1/10f);
            }
            return true;
        }
        return super.scrolled(v, v1);
    }
}
