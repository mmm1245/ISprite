package com.github.bitsky;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class GraphEditor extends Editor {
    private final Texture linkInputTexture;
    private final Texture linkInputFilledTexture;
    private final Texture linkOutputTexture;

    private HashMap<UUID, GraphNode> nodes;
    private DragAndDrop dragAndDrop;
    private float time;
    private Table rightClickMenu;
    private HashMap<String, Supplier<GraphNode>> nodeTypes;
    public GraphEditor() {
        this.linkInputTexture = new Texture("link_input.png");
        this.linkInputFilledTexture = new Texture("link_input_filled.png");
        this.linkOutputTexture = new Texture("link_output.png");

        this.dragAndDrop = new DragAndDrop();
        this.nodes = new HashMap<>();

        addNode(new FinalPoseGraphNode(), Vector2.Zero);

        rightClickMenu = null;

        nodeTypes = new HashMap<>();
        nodeTypes.put("Animated Pose", AnimatedPoseGraphNode::new);
        nodeTypes.put("Blend Pose", BlendPoseGraphNode::new);
        nodeTypes.put("Multiply Pose", MultiplyPoseGraphNode::new);
        nodeTypes.put("Add Pose", AddPoseGraphNode::new);
    }

    public void removeNode(GraphNode node) {
        node.window.remove();

        this.nodes.remove(node.id, node);
        this.nodes.forEach((nodeKey, nodeValue) -> nodeValue.disconnectAll(node));
    }

    public void addNode(GraphNode node, Vector2 position){
        this.nodes.put(node.id, node);
        this.stage.addActor(node.window);
        node.window.pack();
        node.window.setPosition(position.x, position.y);
    }

    @Override
    public void render() {
        super.render();
        if(Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)){
            stage.getCamera().position.add(-Gdx.input.getDeltaX()*2f, Gdx.input.getDeltaY()*2f, 0);
        }
        if(Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)){
            if(rightClickMenu != null)
                rightClickMenu.remove();

            this.rightClickMenu = new Table();
            stage.addActor(rightClickMenu);
            Vector3 position = stage.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            rightClickMenu.setPosition(position.x, position.y);
            for(Map.Entry<String, Supplier<GraphNode>> entry : nodeTypes.entrySet()){
                TextButton button = new TextButton(entry.getKey(), ISpriteMain.getSkin());
                button.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Vector3 position = stage.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                        addNode(entry.getValue().get(), new Vector2(position.x, position.y));
                        if(rightClickMenu != null) {
                            rightClickMenu.remove();
                            rightClickMenu = null;
                        }
                    }
                });
                rightClickMenu.add(button).row();
            }
        }

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)){
            if(rightClickMenu != null) {
                rightClickMenu.remove();
                rightClickMenu = null;
            }
        }

        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.begin();
        shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
        for(GraphNode node : nodes.values()){
            for(Map.Entry<String, UUID> entry : node.inputs.entrySet()){
                Actor firstActor = node.inputActors.get(entry.getKey());
                Actor secondActor = nodes.get(entry.getValue()).outputActor;
                GraphNode secondNode = nodes.get(entry.getValue());

                Vector2 vector1 = firstActor.localToStageCoordinates(new Vector2(firstActor.getWidth()/2, firstActor.getHeight()/2));
                Vector2 vector2 = secondActor.localToStageCoordinates(new Vector2(secondActor.getWidth()/2, secondActor.getHeight()/2));

                // edge fix
                this.shapeRenderer.setColor(Color.valueOf("DA863E"));
                this.shapeRenderer.circle(secondNode.window.getX() + secondNode.window.getWidth(), vector2.y,10);
                shapeRenderer.rectLine(vector1.x, vector1.y, secondNode.window.getX() + secondNode.window.getWidth(), vector2.y, 10, Color.valueOf("A4DDDB"), Color.valueOf("DA863E"));
                // shapeRenderer.line(firstActor.localToStageCoordinates(new Vector2(firstActor.getWidth()/2, firstActor.getHeight()/2)), secondActor.localToStageCoordinates(new Vector2(secondActor.getWidth()/2, secondActor.getHeight()/2)));
            }
        }
        shapeRenderer.end();
        shapeRenderer.setProjectionMatrix(camera.combined);
    }

    public static class ConnectionData{
        public final UUID first;
        public ConnectionData(UUID first) {
            this.first = first;
        }
    }

    public abstract class GraphNode {
        public final UUID id;
        public final Window window;
        public final HashMap<String,UUID> inputs;
        public final HashMap<String,Actor> inputActors;
        public final HashMap<String, TextureRegion> inputRegions;
        public Actor outputActor;

        public final VerticalGroup verticalGroup;

        public GraphNode(String name, String description, boolean removable) {
            this.id = UUID.randomUUID();
            this.window = new Window(name, ISpriteMain.getSkin());

            this.inputRegions = new HashMap<>();
            this.inputs = new HashMap<>();
            this.inputActors = new HashMap<>();
            this.verticalGroup = new VerticalGroup();

            final HorizontalGroup miniToolBar = new HorizontalGroup();
            if (removable) {
                final TextButton removeButton = new TextButton("X", this.window.getSkin());
                removeButton.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        GraphEditor.this.removeNode(GraphEditor.GraphNode.this);
                        super.clicked(event, x, y);
                    }
                });

                miniToolBar.addActor(removeButton);
            }
            miniToolBar.addActor(new Label(description, this.window.getSkin()));
            this.verticalGroup.addActor(miniToolBar);
            this.verticalGroup.columnLeft();


            if (hasOutput()) {
                HorizontalGroup hgroup = new HorizontalGroup();
                hgroup.addActor(new Label("output", ISpriteMain.getSkin()));
                Actor dragOutput = new Image(new TextureRegion(linkOutputTexture));
                hgroup.addActor(dragOutput);
                dragAndDrop.addSource(new DragAndDrop.Source(dragOutput) {
                    @Override
                    public DragAndDrop.Payload dragStart(InputEvent inputEvent, float v, float v1, int i) {
                        dragOutput.setVisible(false);
                        DragAndDrop.Payload payload = new DragAndDrop.Payload();
                        Image connection = new Image(linkOutputTexture);
                        payload.setDragActor(connection);
                        payload.setObject(new ConnectionData(id));
                        dragAndDrop.setDragActorPosition(dragOutput.getWidth(), -dragOutput.getHeight() / 2);
                        return payload;
                    }

                    @Override
                    public void dragStop(InputEvent event, float x, float y, int pointer, DragAndDrop.Payload payload, DragAndDrop.Target target) {
                        dragOutput.setVisible(true);
                        super.dragStop(event, x, y, pointer, payload, target);
                    }
                });
                verticalGroup.addActor(hgroup);
                outputActor = dragOutput;
            }

            window.add(this.verticalGroup);

        }

        /**
         * remove every connection with specified node
         * @param graphNode
         */
        public void disconnectAll(GraphNode graphNode) {

            ArrayList<String> toRemove = new ArrayList<>();

            for (String key : this.inputs.keySet()) {
                UUID val = this.inputs.get(key);
                if (val.equals(graphNode.id)) {
                    this.inputRegions.get(key).setTexture(linkInputTexture);
                    toRemove.add(key);
                }
            }

            toRemove.forEach(this.inputs::remove);
        }

        public void addInput(String name) {
            HorizontalGroup hgroup = new HorizontalGroup();

            TextureRegion textureRegion = new TextureRegion(linkInputTexture);
            Actor dragInput = new Image(textureRegion);
            this.inputRegions.put(name, textureRegion);
            hgroup.addActor(dragInput);
            hgroup.addActor(new Label(name, ISpriteMain.getSkin()));

            dragAndDrop.addTarget(new DragAndDrop.Target(dragInput) {
                @Override
                public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float v, float v1, int i) {
                    return payload.getObject() instanceof ConnectionData;
                }
                @Override
                public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float v, float v1, int i) {
                    ConnectionData output = (ConnectionData) payload.getObject();
                    inputs.put(name, output.first);
                    textureRegion.setTexture(linkInputFilledTexture);
                }
            });
            verticalGroup.addActor(hgroup);
            this.inputActors.put(name, dragInput);
        }
        public abstract AnimatedSpritePose getOutputPose();
        public boolean hasOutput(){
            return true;
        }
        public AnimatedSpritePose getInput(String input){
            return nodes.get(inputs.get(input)).getOutputPose();
        }
    }
    public class FinalPoseGraphNode extends GraphNode{
        public FinalPoseGraphNode() {
            super("Final Pose", "Pose to be displayed by the animation renderer.", false);
            addInput("final");
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            throw new IllegalStateException();
        }
        @Override
        public boolean hasOutput() {
            return false;
        }
    }

    public class AnimatedPoseGraphNode extends GraphNode{
        public final SpriteAnimation animation;
        public AnimatedPoseGraphNode() {
            super("Animated Pose", "Animated Pose Node", true);

            this.animation = new SpriteAnimation();
            TextButton enterButton = new TextButton("Edit", ISpriteMain.getSkin());
            enterButton.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ISpriteMain.getInstance().setEditor(new AnimationEditor(animation));
                }
            });
            this.verticalGroup.addActor(enterButton);
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return animation.getPose(time);
        }
    }
    public class BlendPoseGraphNode extends GraphNode{
        public float blendValue;
        public BlendPoseGraphNode() {
            super("Blend Pose", "Blends two inputs.", true);
            addInput("Input1");
            addInput("Input2");
            this.blendValue = 0.5f;
            this.window.add(new Label("Blend: ", ISpriteMain.getSkin()));
            TextField textField = new TextField(""+blendValue, ISpriteMain.getSkin());
            textField.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || (c=='.' && !textField1.getText().contains(".")));
            textField.setTextFieldListener((textField1, c) -> {
                try {
                    blendValue = Float.parseFloat(textField1.getText());
                } catch(NumberFormatException e){
                    textField.setText(""+blendValue);
                }
            });
            this.window.add(textField);
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Input1").lerp(getInput("Input2"), blendValue);
        }
    }
    public class MultiplyPoseGraphNode extends GraphNode{
        public float multiplyValue;
        public MultiplyPoseGraphNode() {
            super("Multiply Pose", "Multiplies pose by set value.", true);
            addInput("Pose");
            this.multiplyValue = 1f;
            this.window.add(new Label("Multiply: ", ISpriteMain.getSkin()));
            TextField textField = new TextField(""+multiplyValue, ISpriteMain.getSkin());
            textField.setTextFieldFilter((textField1, c) -> Character.isDigit(c) || (c=='.' && !textField1.getText().contains(".")));
            textField.setTextFieldListener((textField1, c) -> {
                try {
                    multiplyValue = Float.parseFloat(textField1.getText());
                } catch(NumberFormatException e){
                    textField.setText(""+multiplyValue);
                }
            });
            this.window.add(textField);
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("Pose").multiply(multiplyValue);
        }
    }
    public class AddPoseGraphNode extends GraphNode{
        public AddPoseGraphNode() {
            super("Add Pose", "", true);
            addInput("first");
            addInput("second");
        }
        @Override
        public AnimatedSpritePose getOutputPose() {
            return getInput("first").add(getInput("second"));
        }
    }
}
