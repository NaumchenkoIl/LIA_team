package com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra;

public interface Vector <T extends Vector<T>>{
    T add(T other);
    T subtract(T other);
    T multiply(float scalar);
    T divide(float scalar);

    float length();
    T normalize();
    float dot(T other);

    int getDimensions();
    float getComponent(int index);

    boolean equals(Object obj);
    String toString();
}
