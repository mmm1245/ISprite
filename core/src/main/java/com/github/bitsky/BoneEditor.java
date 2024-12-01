package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoneEditor extends Editor {
    private static AnimatedSpritePose EMPTY_POSE = new AnimatedSpritePose(new HashMap<>());
    private UUID movingId;
    public BoneEditor() {
        this.movingId = null;
    }
    @Override
    public void render() {
        super.render();
        AnimatedSprite sprite = ISpriteMain.getInstance().sprite;
        HashMap<UUID, Transform> transforms = EMPTY_POSE.getBoneTransforms(sprite, new Transform().lock());
        UUID moused = null;
        Vector3 worldMouse3 = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 worldMouse = new Vector2(worldMouse3.x, worldMouse3.y);
        for(Map.Entry<UUID, Transform> entry : transforms.entrySet()){
            if(entry.getValue().translation.dst(worldMouse) < 10f){
                moused = entry.getKey();
            }
        }

        shapeRenderer.begin();
        EMPTY_POSE.drawDebugBones(sprite, shapeRenderer, moused);
        shapeRenderer.end();

        if(movingId != null){
            AnimatedSpriteBone movingBone = sprite.bones.get(movingId);
            if(movingBone != null && movingBone.parent != null) {
                Transform parentTransform = transforms.get(movingBone.parent);
                movingBone.baseTransform.translation.set(worldMouse.cpy().sub(parentTransform.translation).rotateRad(-parentTransform.rotation));
            }
        }

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)){
            if(movingId == null)
                movingId = moused;
            else
                movingId = null;
        }

        if(moused != null){
            if(Gdx.input.isKeyJustPressed(Input.Keys.D)){
                sprite.removeNode(sprite.bones.get(moused));
            }
            if(Gdx.input.isKeyJustPressed(Input.Keys.C)){
                movingId = sprite.addChildNodeTo(sprite.bones.get(moused)).id;
            }
        }
    }

    @Override
    public boolean scrolled(float v, float v1) {
        if(movingId != null){
            AnimatedSprite sprite = ISpriteMain.getInstance().sprite;
            AnimatedSpriteBone movingBone = sprite.bones.get(movingId);
            if(movingBone != null && movingBone.parent != null) {
                movingBone.baseTransform.rotation += v1/10f;
            }
        }
        return false;
    }
}
