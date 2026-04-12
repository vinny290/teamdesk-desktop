package com.teamdesk.agent.input;

import com.teamdesk.agent.dto.InputEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class RemoteInputService {

    private static final Logger log = LoggerFactory.getLogger(RemoteInputService.class);

    private final Robot robot;
    private final Dimension screenSize;

    public RemoteInputService() {
        try {
            this.robot = new Robot();
            this.robot.setAutoDelay(5);
            this.screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        } catch (AWTException e) {
            throw new IllegalStateException("Failed to initialize Robot", e);
        }
    }

    public void handle(InputEventPayload payload) {
        if (payload == null || payload.getEventType() == null || payload.getEventType().isBlank()) {
            log.warn("Ignoring invalid input payload: {}", payload);
            return;
        }

        switch (payload.getEventType()) {
            case "mouse_move" -> handleMouseMove(payload);
            case "mouse_down" -> handleMouseDown(payload);
            case "mouse_up" -> handleMouseUp(payload);
            case "mouse_click" -> handleMouseClick(payload);
            case "mouse_double_click" -> handleMouseDoubleClick(payload);
            case "mouse_wheel" -> handleMouseWheel(payload);
            case "key_down" -> handleKeyDown(payload);
            case "key_up" -> handleKeyUp(payload);
            default -> log.warn("Unsupported input eventType: {}", payload.getEventType());
        }
    }

    private void handleMouseMove(InputEventPayload payload) {
        Point point = toScreenPoint(payload.getX(), payload.getY());
        robot.mouseMove(point.x, point.y);
    }

    private void handleMouseDown(InputEventPayload payload) {
        Point point = toScreenPoint(payload.getX(), payload.getY());
        robot.mouseMove(point.x, point.y);
        robot.mousePress(toMouseMask(payload.getButton()));
    }

    private void handleMouseUp(InputEventPayload payload) {
        Point point = toScreenPoint(payload.getX(), payload.getY());
        robot.mouseMove(point.x, point.y);
        robot.mouseRelease(toMouseMask(payload.getButton()));
    }

    private void handleMouseClick(InputEventPayload payload) {
        Point point = toScreenPoint(payload.getX(), payload.getY());
        int mask = toMouseMask(payload.getButton());

        robot.mouseMove(point.x, point.y);
        robot.mousePress(mask);
        robot.mouseRelease(mask);
    }

    private void handleMouseDoubleClick(InputEventPayload payload) {
        Point point = toScreenPoint(payload.getX(), payload.getY());
        int mask = toMouseMask(payload.getButton());

        robot.mouseMove(point.x, point.y);

        robot.mousePress(mask);
        robot.mouseRelease(mask);

        robot.delay(80);

        robot.mousePress(mask);
        robot.mouseRelease(mask);
    }

    private void handleMouseWheel(InputEventPayload payload) {
        int delta = payload.getWheelDelta() != null ? payload.getWheelDelta() : 0;
        robot.mouseWheel(delta);
    }

    private void handleKeyDown(InputEventPayload payload) {
        Integer keyCode = toKeyCode(payload);
        if (keyCode == null) {
            log.warn("Unsupported key_down payload: {}", payload);
            return;
        }

        robot.keyPress(keyCode);
    }

    private void handleKeyUp(InputEventPayload payload) {
        Integer keyCode = toKeyCode(payload);
        if (keyCode == null) {
            log.warn("Unsupported key_up payload: {}", payload);
            return;
        }

        robot.keyRelease(keyCode);
    }

    private Point toScreenPoint(Double normalizedX, Double normalizedY) {
        double x = normalizedX != null ? normalizedX : 0.0;
        double y = normalizedY != null ? normalizedY : 0.0;

        x = Math.max(0.0, Math.min(1.0, x));
        y = Math.max(0.0, Math.min(1.0, y));

        int screenX = (int) Math.round(x * Math.max(1, screenSize.width - 1));
        int screenY = (int) Math.round(y * Math.max(1, screenSize.height - 1));

        return new Point(screenX, screenY);
    }

    private int toMouseMask(Integer button) {
        int btn = button != null ? button : 1;

        return switch (btn) {
            case 1 -> InputEvent.BUTTON1_DOWN_MASK;
            case 2 -> InputEvent.BUTTON2_DOWN_MASK;
            case 3 -> InputEvent.BUTTON3_DOWN_MASK;
            default -> InputEvent.BUTTON1_DOWN_MASK;
        };
    }

    private Integer toKeyCode(InputEventPayload payload) {
        if (payload.getCode() == null) {
            return null;
        }

        return switch (payload.getCode()) {
            case "KeyA" -> KeyEvent.VK_A;
            case "KeyB" -> KeyEvent.VK_B;
            case "KeyC" -> KeyEvent.VK_C;
            case "KeyD" -> KeyEvent.VK_D;
            case "KeyE" -> KeyEvent.VK_E;
            case "KeyF" -> KeyEvent.VK_F;
            case "KeyG" -> KeyEvent.VK_G;
            case "KeyH" -> KeyEvent.VK_H;
            case "KeyI" -> KeyEvent.VK_I;
            case "KeyJ" -> KeyEvent.VK_J;
            case "KeyK" -> KeyEvent.VK_K;
            case "KeyL" -> KeyEvent.VK_L;
            case "KeyM" -> KeyEvent.VK_M;
            case "KeyN" -> KeyEvent.VK_N;
            case "KeyO" -> KeyEvent.VK_O;
            case "KeyP" -> KeyEvent.VK_P;
            case "KeyQ" -> KeyEvent.VK_Q;
            case "KeyR" -> KeyEvent.VK_R;
            case "KeyS" -> KeyEvent.VK_S;
            case "KeyT" -> KeyEvent.VK_T;
            case "KeyU" -> KeyEvent.VK_U;
            case "KeyV" -> KeyEvent.VK_V;
            case "KeyW" -> KeyEvent.VK_W;
            case "KeyX" -> KeyEvent.VK_X;
            case "KeyY" -> KeyEvent.VK_Y;
            case "KeyZ" -> KeyEvent.VK_Z;

            case "Digit0" -> KeyEvent.VK_0;
            case "Digit1" -> KeyEvent.VK_1;
            case "Digit2" -> KeyEvent.VK_2;
            case "Digit3" -> KeyEvent.VK_3;
            case "Digit4" -> KeyEvent.VK_4;
            case "Digit5" -> KeyEvent.VK_5;
            case "Digit6" -> KeyEvent.VK_6;
            case "Digit7" -> KeyEvent.VK_7;
            case "Digit8" -> KeyEvent.VK_8;
            case "Digit9" -> KeyEvent.VK_9;

            case "Enter" -> KeyEvent.VK_ENTER;
            case "Escape" -> KeyEvent.VK_ESCAPE;
            case "Backspace" -> KeyEvent.VK_BACK_SPACE;
            case "Tab" -> KeyEvent.VK_TAB;
            case "Space" -> KeyEvent.VK_SPACE;

            case "ArrowUp" -> KeyEvent.VK_UP;
            case "ArrowDown" -> KeyEvent.VK_DOWN;
            case "ArrowLeft" -> KeyEvent.VK_LEFT;
            case "ArrowRight" -> KeyEvent.VK_RIGHT;

            case "ShiftLeft", "ShiftRight" -> KeyEvent.VK_SHIFT;
            case "ControlLeft", "ControlRight" -> KeyEvent.VK_CONTROL;
            case "AltLeft", "AltRight" -> KeyEvent.VK_ALT;

            case "Delete" -> KeyEvent.VK_DELETE;
            case "Home" -> KeyEvent.VK_HOME;
            case "End" -> KeyEvent.VK_END;
            case "PageUp" -> KeyEvent.VK_PAGE_UP;
            case "PageDown" -> KeyEvent.VK_PAGE_DOWN;

            case "F1" -> KeyEvent.VK_F1;
            case "F2" -> KeyEvent.VK_F2;
            case "F3" -> KeyEvent.VK_F3;
            case "F4" -> KeyEvent.VK_F4;
            case "F5" -> KeyEvent.VK_F5;
            case "F6" -> KeyEvent.VK_F6;
            case "F7" -> KeyEvent.VK_F7;
            case "F8" -> KeyEvent.VK_F8;
            case "F9" -> KeyEvent.VK_F9;
            case "F10" -> KeyEvent.VK_F10;
            case "F11" -> KeyEvent.VK_F11;
            case "F12" -> KeyEvent.VK_F12;

            default -> null;
        };
    }
}