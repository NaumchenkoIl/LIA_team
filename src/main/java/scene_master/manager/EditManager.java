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
        this.selectedVertexIndex = index;
        this.selectedPolygonIndex = -1; // сбрасываем выбор полигона
        System.out.println("EditManager: выбрана вершина " + index);
    }

    public void selectPolygon(int index) {
        this.selectedPolygonIndex = index;
        this.selectedVertexIndex = -1;
        System.out.println("EditManager: выбран полигон " + index);
    }

    public void deleteSelectedVertex(Model3D model) {
        if (selectedVertexIndex < 0 || selectedVertexIndex >= model.getVertices().size()) {
            return;
        }

        int vertexToDelete = selectedVertexIndex;
        model.getVertices().remove(vertexToDelete);

        for (int i = model.getPolygons().size() - 1; i >= 0; i--) {
            Polygon polygon = model.getPolygons().get(i);
            List<Integer> indices = polygon.getVertexIndices();

            if (indices.contains(vertexToDelete)) {
                model.getPolygons().remove(i);
                continue;
            }

            List<Integer> newIndices = new ArrayList<>();
            for (Integer idx : indices) {
                newIndices.add(idx > vertexToDelete ? idx - 1 : idx);
            }
            polygon.setVertexIndices(newIndices);
        }

        model.calculateVertexNormals();
        clearSelection();
    }

    public void deleteSelectedPolygon(Model3D model) {
        if (selectedPolygonIndex >= 0 && selectedPolygonIndex < model.getPolygons().size()) {
            model.getPolygons().remove(selectedPolygonIndex);
            clearSelection();
            System.out.println("Удалён полигон #" + selectedPolygonIndex);
        }
    }

    public void selectAll(Model3D model) {
        if (model != null) {
            selectedVertexIndex = model.getVertices().isEmpty() ? -1 : 0;
            selectedPolygonIndex = model.getPolygons().isEmpty() ? -1 : 0;
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