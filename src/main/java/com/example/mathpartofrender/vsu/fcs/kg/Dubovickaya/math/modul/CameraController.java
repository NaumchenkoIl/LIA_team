package com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul;

import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.Vector3D;
import javafx.scene.input.KeyCode;

public class CameraController {
    private Camera camera;
    private float moveSpeed = 0.5f;      // скорость перемещения
    private float rotateSpeed = 2.0f;    // скорость поворота

    public CameraController(Camera camera) {
        this.camera = camera;
    }

    public void handleKeyPress(KeyCode key) {
        Vector3D pos = camera.getPosition();
        Vector3D target = camera.getTarget();
        Vector3D up = camera.getUp();

        // Направление взгляда
        Vector3D forward = target.subtract(pos).normalize();

        // Правая ось
        Vector3D right = forward.cross(up).normalize();

        switch (key) {
            case W: // Вперёд
                camera.setPosition(pos.add(forward.multiply(moveSpeed)));
                camera.setTarget(target.add(forward.multiply(moveSpeed)));
                break;

            case S: // Назад
                camera.setPosition(pos.subtract(forward.multiply(moveSpeed)));
                camera.setTarget(target.subtract(forward.multiply(moveSpeed)));
                break;

            case A: // Влево
                camera.setPosition(pos.subtract(right.multiply(moveSpeed)));
                camera.setTarget(target.subtract(right.multiply(moveSpeed)));
                break;

            case D: // Вправо
                camera.setPosition(pos.add(right.multiply(moveSpeed)));
                camera.setTarget(target.add(right.multiply(moveSpeed)));
                break;

            case Q: // Поднять камеру вверх (без изменения направления взгляда)
                camera.setPosition(pos.add(up.multiply(moveSpeed)));
                camera.setTarget(target.add(up.multiply(moveSpeed)));
                break;

            case E: // Опустить вниз
                camera.setPosition(pos.subtract(up.multiply(moveSpeed)));
                camera.setTarget(target.subtract(up.multiply(moveSpeed)));
                break;

            case R: // Вращение вокруг цели (влево/вправо по горизонтали)
                Vector3D offset = pos.subtract(target);
                float angleRad = (float) Math.toRadians(rotateSpeed);
                float cos = (float) Math.cos(angleRad);
                float sin = (float) Math.sin(angleRad);

                // Вращение вокруг Y-оси
                float newX = offset.getX() * cos + offset.getZ() * sin;
                float newZ = -offset.getX() * sin + offset.getZ() * cos;

                Vector3D newPos = new Vector3D(newX, offset.getY(), newZ).add(target);
                camera.setPosition(newPos);
                break;

            case F: // Обратное вращение
                offset = pos.subtract(target);
                angleRad = (float) Math.toRadians(-rotateSpeed);
                cos = (float) Math.cos(angleRad);
                sin = (float) Math.sin(angleRad);

                newX = offset.getX() * cos + offset.getZ() * sin;
                newZ = -offset.getX() * sin + offset.getZ() * cos;

                newPos = new Vector3D(newX, offset.getY(), newZ).add(target);
                camera.setPosition(newPos);
                break;
        }
    }
}