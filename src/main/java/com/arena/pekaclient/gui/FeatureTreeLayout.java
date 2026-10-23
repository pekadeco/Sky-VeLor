package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless geometry for the compact feature tree shown by the hub and editor navigation.
 * The renderer consumes this one source of indentation, arrow and connector positions so tiny GUI scales cannot
 * create a second, overlapping layout path.
 */
public final class FeatureTreeLayout {
    public static final float INDENT = 16f;
    public static final float ARROW_SIZE = 7f;
    public static final float CONNECTOR_TICK = 7f;

    private FeatureTreeLayout() { }

    public interface OpenState {
        boolean isOpen(String id, boolean fallback);
    }

    public static final class Node {
        public final String id;
        public final String parentId;
        public final int depth;
        public final boolean hasChildren;
        public final boolean initiallyOpen;
        public final float height;

        public Node(String id, String parentId, int depth, boolean hasChildren, boolean initiallyOpen, float height) {
            this.id = id == null ? "" : id;
            this.parentId = parentId;
            this.depth = Math.max(0, depth);
            this.hasChildren = hasChildren;
            this.initiallyOpen = initiallyOpen;
            this.height = Math.max(1f, height);
        }
    }

    public static final class Row {
        public final Node node;
        public final GuiLayoutToolkit.Rect bounds = new GuiLayoutToolkit.Rect();
        public final float arrowX;
        public final float connectorX;
        public final boolean visible;

        private Row(Node node, float x, float y, float width, boolean visible) {
            this.node = node;
            this.visible = visible;
            bounds.set(x, y, Math.max(0f, width), visible ? node.height : 0f);
            arrowX = x + 8f + node.depth * INDENT;
            connectorX = arrowX - INDENT * .5f;
        }
    }

    /** One vertical stem and horizontal tick. A row may have one connector per active ancestor. */
    public static final class Connector {
        public final float x, y1, y2, tickEnd;
        public final String ownerId;

        private Connector(float x, float y1, float y2, float tickEnd, String ownerId) {
            this.x = x;
            this.y1 = y1;
            this.y2 = y2;
            this.tickEnd = tickEnd;
            this.ownerId = ownerId;
        }
    }

    public static final class Result {
        private final List<Row> rows;
        private final List<Connector> connectors;
        public final float contentHeight;

        private Result(List<Row> rows, List<Connector> connectors, float contentHeight) {
            this.rows = Collections.unmodifiableList(rows);
            this.connectors = Collections.unmodifiableList(connectors);
            this.contentHeight = contentHeight;
        }

        public List<Row> rows() { return rows; }
        public List<Connector> connectors() { return connectors; }
        public Row find(String id) {
            if (id == null) return null;
            for (Row row : rows) if (id.equals(row.node.id)) return row;
            return null;
        }
    }

    /**
     * Lays visible nodes in input order. A descendant is visible only while every ancestor is open; closed branches
     * occupy no vertical space. This is intentionally independent from Minecraft rendering for test coverage.
     */
    public static Result layout(List<Node> nodes, OpenState openState, float x, float y, float width) {
        if (nodes == null || nodes.isEmpty()) return new Result(new ArrayList<Row>(), new ArrayList<Connector>(), 0f);
        Map<String, Node> known = new HashMap<String, Node>();
        for (Node node : nodes) if (node != null && node.id.length() > 0) known.put(node.id, node);
        List<Row> rows = new ArrayList<Row>();
        float cursor = y;
        for (Node node : nodes) {
            if (node == null) continue;
            boolean shown = visible(node, known, openState);
            Row row = new Row(node, x, cursor, width, shown);
            rows.add(row);
            if (shown) cursor += node.height;
        }
        List<Connector> connectors = connectors(rows, known, openState);
        return new Result(rows, connectors, Math.max(0f, cursor - y));
    }

    private static boolean visible(Node node, Map<String, Node> known, OpenState state) {
        String parent = node.parentId;
        int safety = 0;
        while (parent != null && safety++ < 64) {
            Node parentNode = known.get(parent);
            if (parentNode == null) return false;
            boolean open = state == null ? parentNode.initiallyOpen : state.isOpen(parentNode.id, parentNode.initiallyOpen);
            if (!open) return false;
            parent = parentNode.parentId;
        }
        return safety < 64;
    }

    private static List<Connector> connectors(List<Row> rows, Map<String, Node> known, OpenState state) {
        List<Connector> out = new ArrayList<Connector>();
        for (Row child : rows) {
            if (!child.visible || child.node.parentId == null) continue;
            Node parent = known.get(child.node.parentId);
            if (parent == null) continue;
            // A direct child receives a clear vertical stem and short branch tick in the existing dropdown style.
            float center = child.bounds.y + child.bounds.h * .5f;
            float x = child.bounds.x + 8f + parent.depth * INDENT + INDENT * .5f;
            out.add(new Connector(x, child.bounds.y, center, x + CONNECTOR_TICK, parent.id));
        }
        return out;
    }
}
