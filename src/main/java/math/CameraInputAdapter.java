package math;
import math.LinealAlgebra.Vector3D;
import javafx.scene.input.KeyCode;

public class CameraInputAdapter {

    private final Camera camera;
    private double yaw = 0.0;      // горизонтальный поворот (вокруг Y)
    private double pitch = 0.0;    // вертикальный поворот (вокруг X)
    private final float moveSpeed = 0.5f;
    private final float rotateSpeed = 0.01f; // чувствительность мыши

    public CameraInputAdapter(Camera camera) {
        this.camera = camera;
        // Инициализируем углы на основе начального положения
        initAnglesFromCamera();
    }

    private void initAnglesFromCamera() {
        Vector3D pos = camera.getPosition();
        Vector3D target = camera.getTarget();
        Vector3D dir = target.subtract(pos).normalize();

        // yaw = atan2(x, z)
        yaw = Math.atan2(dir.getX(), dir.getZ());
        // pitch = asin(y)
        pitch = Math.asin(Math.max(-1.0, Math.min(1.0, dir.getY())));
    }

    public void onKeyPressed(KeyCode key) {
        Vector3D pos = camera.getPosition();
        Vector3D target = camera.getTarget();
        Vector3D up = new Vector3D(0, 1, 0);

        Vector3D forward = target.subtract(pos).normalize();
        Vector3D right = forward.cross(up).normalize();

        switch (key) {
            case W:
                moveCamera(forward, moveSpeed);
                break;
            case S:
                moveCamera(forward, -moveSpeed);
                break;
            case A:
                moveCamera(right,  -moveSpeed);
                break;
            case D:
                moveCamera(right, moveSpeed);
                break;
            case Q:
                moveCamera(up, moveSpeed);
                break;
            case E:
                moveCamera(up, -moveSpeed);
                break;
        }
    }

    private void moveCamera(Vector3D direction, float amount) {
        Vector3D offset = direction.multiply(amount);
        camera.setPosition(camera.getPosition().add(offset));
        camera.setTarget(camera.getTarget().add(offset));
    }

    /**
     * Вызывается при движении мыши с зажатой ПКМ.
     * deltaX, deltaY — смещение в пикселях.
     */
    public void onMouseDragged(float deltaX, float deltaY) {
        // Обновляем углы
        yaw += deltaX * rotateSpeed;
        pitch -= deltaY * rotateSpeed;

        // Ограничиваем pitch, чтобы камера не переворачивалась
        pitch = Math.max(-Math.PI / 2 + 0.01, Math.min(Math.PI / 2 - 0.01, pitch));

        // Пересчитываем позицию камеры вокруг цели
        updateCameraPosition();
    }

    private void updateCameraPosition() {
        Vector3D target = camera.getTarget();
        double distance = camera.getPosition().subtract(target).length();

        // Направление взгляда из углов
        double x = Math.sin(yaw) * Math.cos(pitch);
        double y = Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);

        Vector3D newPos = new Vector3D(
                (float)(target.getX() + x * distance),
                (float)(target.getY() + y * distance),
                (float)(target.getZ() + z * distance)
        );

        camera.setPosition(newPos);
    }

    public double getYaw() {
        return yaw;
    }

    public double getPitch() {
        return pitch;
    }
}
