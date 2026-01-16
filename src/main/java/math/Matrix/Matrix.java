package com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.Matrix;

import com.example.mathpartofrender.vsu.fcs.kg.Dubovickaya.math.modul.LinealAlgebra.Vector;

public interface Matrix<T extends Matrix<T, V>, V extends Vector<V>>{
    T add(T other);
    T subtract(T other);
    T multiply(float scalar);
    T multiply(T other);
    V multiply(V vector);
    T transpose();

    float determinant();
    T inverse();
    V solveLinealSystem(V vector);

    int getRows();
    int getColumns();
    float get(int row, int col);
    boolean equals(Object obj);
    String toString();
}
