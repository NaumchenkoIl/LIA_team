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

    private Model3D currentModel = null;

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

    public void setCurrentModel(Model3D model) {
        this.currentModel = model;
        clearSelection();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void selectVertex(int index) {
        if (editMode && currentModel != null) {
            selectedVertexIndex = index;
            selectedPolygonIndex = -1;
            System.out.println("Выбрана вершина #" + index +
                    " в модели " + currentModel.getName());
        }
    }

    public void selectPolygon(int index) {
        if (editMode && currentModel != null) {
            selectedPolygonIndex = index;
            selectedVertexIndex = -1;
            System.out.println("Выбран полигон #" + index +
                    " в модели " + currentModel.getName());
        }
    }

    public void deleteSelectedVertex(Model3D model) {
        if (selectedVertexIndex >= 0 && selectedVertexIndex < model.getVertices().size()) {
            ObservableList<Vector3D> vertices = model.getVertices();
            ObservableList<Polygon> polygons = model.getPolygons();

            vertices.remove(selectedVertexIndex);

            for (int i = polygons.size() - 1; i >= 0; i--) {
                Polygon polygon = polygons.get(i);
                List<Integer> indices = polygon.getVertexIndices();

                if (indices.contains(selectedVertexIndex)) {
                    polygons.remove(i);
                } else {
                    List<Integer> newIndices = new ArrayList<>();
                    for (Integer idx : indices) {
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

    public void deleteVertex(Model3D model, int vertexIndex) {
        if (vertexIndex >= 0 && vertexIndex < model.getVertices().size()) {
            ObservableList<Vector3D> vertices = model.getVertices();
            ObservableList<Polygon> polygons = model.getPolygons();

            vertices.remove(vertexIndex);

            for (int i = polygons.size() - 1; i >= 0; i--) {
                Polygon polygon = polygons.get(i);
                List<Integer> indices = polygon.getVertexIndices();

                if (indices.contains(vertexIndex)) {
                    polygons.remove(i);
                } else {
                    List<Integer> newIndices = new ArrayList<>();
                    for (Integer idx : indices) {
                        if (idx > vertexIndex) {
                            newIndices.add(idx - 1);
                        } else {
                            newIndices.add(idx);
                        }
                    }
                    polygons.set(i, new Polygon(newIndices));
                }
            }
        }
    }

    public void deletePolygon(Model3D model, int polygonIndex) {
        if (polygonIndex >= 0 && polygonIndex < model.getPolygons().size()) {
            model.getPolygons().remove(polygonIndex);
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

    public Model3D getCurrentModel() {
        return currentModel;
    }
}