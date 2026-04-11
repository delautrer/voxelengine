package de.delautrer.engine.player;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class Camera {

    private final Vector3f position = new Vector3f(8.0f, 20.0f, 30.0f);
    private final Vector3f front = new Vector3f(0.0f, -0.5f, -1.0f).normalize();
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

    private float yaw = -90.0f;
    private float pitch = -30.0f;

    private boolean firstMouse = true;
    private double lastX = 400.0;
    private double lastY = 300.0;

    private boolean escPressedLastFrame = false;
    private boolean cursorCaptured = true;

    public void update(long windowHandle, float deltaTime, Vector3f playerPos) {
        this.position.set(playerPos);

        boolean escPressed = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;

        if (escPressed && !escPressedLastFrame) {
            cursorCaptured = !cursorCaptured;
            if (cursorCaptured) {
                GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                firstMouse = true;
            } else {
                GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            }
        }
        escPressedLastFrame = escPressed;

        if (!cursorCaptured) {
            return;
        }

        // --- Kamera umschauen (Maus) ---
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(windowHandle, xpos, ypos);

        if (firstMouse) {
            lastX = xpos[0];
            lastY = ypos[0];
            firstMouse = false;
        }

        double xoffset = xpos[0] - lastX;
        double yoffset = lastY - ypos[0];
        lastX = xpos[0];
        lastY = ypos[0];

        float sensitivity = 0.1f;
        xoffset *= sensitivity;
        yoffset *= sensitivity;

        yaw += (float) xoffset;
        pitch += (float) yoffset;

        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        Vector3f direction = new Vector3f();
        direction.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        direction.y = (float) Math.sin(Math.toRadians(pitch));
        direction.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.set(direction).normalize();
    }

    public void resetMouseTracking() {
        this.firstMouse = true;
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, new Vector3f(position).add(front), up);
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getFront() {
        return front;
    }

    public boolean isCursorCaptured() {
        return cursorCaptured;
    }
}