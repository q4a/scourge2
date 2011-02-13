package org.scourge;

import com.ardor3d.framework.Canvas;
import com.ardor3d.input.*;
import com.ardor3d.input.logical.*;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.scourge.terrain.CreatureModel;
import org.scourge.ui.component.Window;

import java.util.Arrays;

/**
 * User: gabor
 * Date: 1/8/11
 * Time: 8:18 PM
 */
public class PlayerControl {
    private boolean playerMoveEnabled;
    private boolean dragging;
    private int startX, startY;
    private Main main;
    // Test boolean to allow us to ignore first mouse event. First event can wildly vary based on platform.
    private boolean firstPing = true;
    private MouseButton buttonDown;
    private TwoInputStates lastInputState;
    private boolean moving;


    public PlayerControl(Main main) {
        this.main = main;
    }

    public void setupTriggers() {
        setupMouseTriggers();
        setupKeyboardTriggers();
    }

    private void setupKeyboardTriggers() {
        main.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ESCAPE), new TriggerAction() {
            @Override
            public void perform(Canvas canvas, TwoInputStates twoInputStates, double v) {
                main.exit();
            }
        }));
        main.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F5), new TriggerAction() {
            @Override
            public void perform(Canvas canvas, TwoInputStates twoInputStates, double v) {
                main.toggleCameraAttached();
            }
        }));
        main.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.B), new TriggerAction() {
            @Override
            public void perform(Canvas canvas, TwoInputStates twoInputStates, double v) {
                main.toggleBounds();
            }
        }));
        main.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F), new TriggerAction() {
            @Override
            public void perform(Canvas canvas, TwoInputStates twoInputStates, double v) {
                main.toggleWireframe();
            }
        }));
        main.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F1), new TriggerAction() {
            @Override
            public void perform(Canvas canvas, TwoInputStates twoInputStates, double v) {
                main.saveScreenshot();
            }
        }));
        main.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.N), new TriggerAction() {
            @Override
            public void perform(Canvas canvas, TwoInputStates twoInputStates, double v) {
                main.toggleNormals();
            }
        }));
        main.getLogicalLayer().registerTrigger(new InputTrigger(new KeyHeldCondition(Key.W), new TriggerAction() {
            @Override
            public void perform(Canvas canvas, TwoInputStates twoInputStates, double tpf) {
                if (playerMoveEnabled) {
                    main.getPlayer().getCreatureModel().setAnimation(CreatureModel.Animations.run);
                    moving = true;
                }
            }
        }));
        main.getLogicalLayer().registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.W), new TriggerAction() {
            @Override
            public void perform(Canvas canvas, TwoInputStates twoInputStates, double tpf) {
                if (playerMoveEnabled) {
                    main.getPlayer().getCreatureModel().setAnimation(CreatureModel.Animations.stand);
                    moving = false;
                }
            }
        }));
        InputTrigger keyTrigger = new InputTrigger(new AnyKeyCondition(), new TriggerAction() {

            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                KeyEvent event = inputState.getCurrent().getKeyboardState().getKeyEvent();
                if (Window.getWindow() != null && Window.getWindow().onKey(event)) return;
            }
        });
        main.getLogicalLayer().registerTrigger(keyTrigger);
    }

    private void setupMouseTriggers() {
        // Mouse look
        final Predicate<TwoInputStates> mouseActed =
                Predicates.or(Predicates.or(
                        TriggerConditions.mouseMoved(),
                        new MouseButtonPressedCondition(MouseButton.LEFT),
                        new MouseButtonReleasedCondition(MouseButton.LEFT)));
        final TriggerAction mouseAction = new TriggerAction() {

            @Override
            public void perform(Canvas canvas, TwoInputStates inputStates, double tpf) {
                lastInputState = inputStates;
                mouseAction(inputStates, tpf);
            }
        };

        InputTrigger mouseTrigger = new InputTrigger(mouseActed, mouseAction);
        main.getLogicalLayer().registerTrigger(mouseTrigger);
    }

    private void mouseAction(TwoInputStates inputStates, double tpf) {
        final MouseState mouse = inputStates.getCurrent().getMouseState();
        MouseState prevMouse = inputStates.getPrevious().getMouseState();

        // what has changed?
        MouseButton button = null;
        boolean pressed = false;
        if(buttonDown != null && mouse.getButtonState(buttonDown) == ButtonState.UP) {
            button = buttonDown;
            pressed = false;
            buttonDown = null;
        } else {
            for(MouseButton mb : MouseButton.values()) {
                if(mouse.getButtonsPressedSince(prevMouse).contains(mb)) {
                    button = mb;
                    pressed = true;
                    buttonDown = mb;
                    break;
                }
            }
        }

        if(button != null) {
            System.err.println(">>> button=" + button + " " + (pressed ? "pressed" : "released"));
        }

        if (mouse.getButtonState(MouseButton.LEFT) == ButtonState.DOWN) {
            // mouse dragged
            if (!firstPing) {

                // cancel button click, if moved too far
                if(Window.getWindow() != null && Window.getWindow().onMove(mouse.getDx(), mouse.getDy(), mouse.getX(), mouse.getY())) return;

                boolean mouseMoved = Math.abs(startX - mouse.getX()) > 5 || Math.abs(startY - mouse.getY()) > 5;
                if(playerMoveEnabled) {
                    if(!dragging && startX >= 0 && startY >= 0) {
                        if(mouseMoved) {
                            dragging = true;
                        }
                    }

                    main.setMouseGrabbed(true);
                    if(mouseMoved) rotate(tpf, mouse.getDx(), mouse.getDy());
                }
            } else {
                firstPing = false;
            }
        } else if(mouse.getDwheel() != 0) {
//                    System.err.println("WHEEL: " + mouse.getDwheel());
            // wheel scrolled
            if(Window.getWindow() != null && Window.getWindow().onWheel(mouse.getDwheel(), mouse.getX(), mouse.getY())) return;
        } else {
            if(button == MouseButton.LEFT && !pressed) {
                firstPing = true;
            }
            // any other mouse action
            if(Window.getWindow() != null && Window.getWindow().onButton(button, pressed, mouse.getX(), mouse.getY())) return;

            if(playerMoveEnabled) {
                if(button == MouseButton.LEFT) {
                    if(pressed) {
                        if(!main.drag() &&
                           !(Window.getWindow() != null &&
                             Window.getWindow().getRectangle().contains(mouse.getX(), mouse.getY()))) {
                            startX = mouse.getX();
                            startY = mouse.getY();
                        }
                    } else {
                        startX = startY = -1;
                        main.setMouseGrabbed(false);
                        if(dragging) {
                            dragging = false;
                        } else {
                            main.mouseReleased();
                        }
                    }
                }
            }

            main.mouseMoved(mouse);
        }

    }

    public boolean isPlayerMoveEnabled() {
        return playerMoveEnabled;
    }

    public void setEnabled(boolean b) {
        playerMoveEnabled = b;
    }




    // Actual movement code
    private final static double PLAYER_ROTATE_STEP = -20;
    private Quaternion qX = new Quaternion();
    private Quaternion q2 = new Quaternion();


    protected void rotate(double time, double amountX, double amountY) {
        qX.fromRotationMatrix(main.getPlayer().getCreatureModel().getNode().getRotation());
        q2.fromAngleAxis(MathUtils.DEG_TO_RAD * amountX * PLAYER_ROTATE_STEP * time, Vector3.UNIT_Y);
        qX.multiplyLocal(q2);
        main.getPlayer().getCreatureModel().getNode().setRotation(qX);
    }

    public TwoInputStates getLastInputState() {
        return lastInputState;
    }

    public boolean isMoving() {
        return moving;
    }
}
