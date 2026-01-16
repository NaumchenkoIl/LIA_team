package scene_master.manager;

import math.LinealAlgebra.Vector3D;
import scene_master.model.Model3D;
import scene_master.model.Polygon;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.List;

public class EditManager {
    private boolean editMode = false;
    private int selectedVertexIndex = -1;
    private int selectedPolygonIndex = -1;
    private EditModeListener editModeListener;

    public interface EditModeListener {
        void onEditModeChanged(boolean enabled);
    }

    public void setEditModeListener(EditModeListener listener) {
        this.editModeListener = listener;
    }

    public void setEditMode(boolean enabled) {
        this.editMode = enabled;
        if (editModeListener != null) {
            editModeListener.onEditModeChanged(enabled);
        }
        if (!enabled) {
            clearSelection();
        }
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void selectVertex(int index) {
        if (editMode) {
            selectedVertexIndex = index;
            selectedPolygonIndex = -1;
        }
    }

    public void selectPolygon(int index) {
        if (editMode) {
            selectedPolygonIndex = index;
            selectedVertexIndex = -1;
        }
    }

    public void deleteSelectedVertex(Model3D model) {
        if (selectedVertexIndex >= 0 && selectedVertexIndex < model.getVertices().size()) {
            ObservableList<Vector3D> vertices = model.getVertices();
            ObservableList<Polygon> polygons = model.getPolygons();

            vertices.remove(selectedVertexIndex);// удаляем вершину

            // обновляем все полигоны: удаляем те, что содержали эту вершину,
            // и корректируем индексы в остальных
            for (int i = polygons.size() - 1; i >= 0; i--) {
                Polygon polygon = polygons.get(i);

                if (polygon.getVertexIndices().contains(selectedVertexIndex)) { // если полигон содержит эту вершину - удаляем весь полигон
                    polygons.remove(i);
                } else {
                    List<Integer> newIndices = new ArrayList<>();// корректируем индексы > selectedVertexIndex
                    for (Integer idx : polygon.getVertexIndices()) {
                        if (idx > selectedVertexIndex) {
                            newIndices.add(idx - 1);
                        } else {
                            newIndices.add(idx);
                        }
                    }
                    polygons.set(i, new Polygon(newIndices));
                }
            }

            clearSelection();
        }
    }

    public void deleteSelectedPolygon(Model3D model) {
        if (selectedPolygonIndex >= 0 && selectedPolygonIndex < model.getPolygons().size()) {
            model.getPolygons().remove(selectedPolygonIndex);
            clearSelection();
        }
    }

    public void clearSelection() {
        selectedVertexIndex = -1;
        selectedPolygonIndex = -1;
    }

    public int getSelectedVertexIndex() {
        return selectedVertexIndex;
    }

    public int getSelectedPolygonIndex() {
        return selectedPolygonIndex;
    }
}