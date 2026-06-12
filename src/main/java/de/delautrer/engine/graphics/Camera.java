package de.delautrer.engine.graphics;

import de.delautrer.game.settings.SettingsManager;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class Camera {

    private final Vector3d position = new Vector3d(8.0, 20.0, 30.0);
    private final Vector3f front = new Vector3f(0.0f, -0.5f, -1.0f).normalize();
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

    private float yaw = -90.0f;
    private float pitch = -30.0f;

    private boolean firstMouse = true;
    private double lastX = 400.0;
    private double lastY = 300.0;

    public void update(long windowHandle, float deltaTime, Vector3d playerPos) {
        this.position.set(playerPos);

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

        float rawSensitivity = SettingsManager.get().mouseSensitivity;
        // f(x) = 0.5333 * x^3 + 0.0666 * x  --> f(0.5) = 0.1, f(1.0) = 0.6
        float sensitivity = rawSensitivity * (0.5333f * rawSensitivity * rawSensitivity + 0.0666f);
        xoffset *= sensitivity;
        yoffset *= sensitivity;

        yaw += (float) xoffset;

        if (SettingsManager.get().invertY) {
            pitch -= (float) yoffset;
        } else {
            pitch += (float) yoffset;
        }

        yaw %= 360.0f;
        if (yaw < 0.0f)
            yaw += 360.0f;

        if (pitch > 89.0f)
            pitch = 89.0f;
        if (pitch < -89.0f)
            pitch = -89.0f;

        Vector3f direction = new Vector3f();
        direction.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        direction.y = (float) Math.sin(Math.toRadians(pitch));
        direction.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.set(direction).normalize();
    }

    private float bobbingOffsetX = 0.0f;
    private float bobbingOffsetY = 0.0f;

    public void setBobbing(float offsetX, float offsetY) {
        this.bobbingOffsetX = offsetX;
        this.bobbingOffsetY = offsetY;
    }

    public void resetMouseTracking() {
        this.firstMouse = true;
    }

    public Matrix4f getViewMatrix() {
        Matrix4f matrix = new Matrix4f();
        // Apply camera-space transformations first
        matrix.translate(-bobbingOffsetX, -bobbingOffsetY, 0.0f);
        
        // Then apply the lookAt transformation
        matrix.lookAt(new Vector3f(0, 0, 0), front, up);
        return matrix;
    }

    public void setPosition(Vector3d position) {
        this.position.set(position);
    }

    public Vector3d getPosition() {
        return position;
    }

    public Vector3f getFront() {
        return front;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Vector3f getUp() {
        return up;
    }

}
