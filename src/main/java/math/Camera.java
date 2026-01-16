package com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul;

import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.Vector3D;
import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.Matrix.Matrix4x4;

public class Camera {
    private Vector3D position;
    private Vector3D target;
    private Vector3D up;
    private float fov;
    private float aspect;
    private float near;
    private float far;

    public Camera(Vector3D position, Vector3D target) {
        this.position = position;
        this.target = target;
        this.up = new Vector3D(0, 1, 0);
        this.fov = 60.0f;
        this.aspect = 16.0f / 9.0f;
        this.near = 0.1f;
        this.far = 100.0f;
    }

    public Vector3D getPosition() { return position; }
    public Vector3D getTarget() { return target; }
    public Vector3D getUp() { return up; }

    public void setPosition(Vector3D position) { this.position = position; }
    public void setTarget(Vector3D target) { this.target = target; }
    public void setUp(Vector3D up) { this.up = up; }

    public float getFov() { return fov; }
    public void setFov(float fov) { this.fov = fov; }
    public void setAspectRatio(float aspect) { this.aspect = aspect; }

    public Matrix4x4 getViewMatrix() {
        return Matrix4x4.lookAt(position, target, up);
    }

    public Matrix4x4 getProjectionMatrix() {
        return Matrix4x4.perspective(fov, aspect, near, far);
    }
}