package cn.edu.pku.dr.requirement.elicitation.tools;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.netbeans.api.visual.widget.*;
import org.netbeans.api.visual.widget.general.IconNodeWidget;
import org.openide.util.Utilities;

public class LayoutAction implements ActionListener {

    public LayoutAction(DemoGraphScene s) {
        this.scene = s;
    }

    public void actionPerformed(ActionEvent evt) {
        node.clear();
        edge.clear();
        rootSet.clear();
        struct.clear();
        clearScene();
        createNodes();
        computeLayout();
        displayNode();
        displayEdge();
        scene.validate();
    }

    private void displayEdge() {
        Collection<Point> cp = new ArrayList<Point>();
        loop: for (Edge cur : edge) {
            for (Node x : rootSet) {
                if (x.equals(this.getNode(cur.source))) continue loop;
            }
            cp.clear();
            ConnectionWidget e = (ConnectionWidget) scene.addEdge(cur.name);
            scene.setEdgeSource(cur.name, cur.source);
            scene.setEdgeTarget(cur.name, cur.target);
            Node sourceNode = null;
            Node targetNode = null;
            Iterator i = node.iterator();
            while (i.hasNext()) {
                Node n = (Node) i.next();
                if (n.name.equals(cur.source)) sourceNode = n;
                if (n.name.equals(cur.target)) targetNode = n;
            }
            if (cur.tag == 1) e.setLineColor(Color.RED);
            if (cur.tag == 1 && sourceNode.sons.size() > 1) {
                Point p = new Point(sourceNode.location.x, sourceNode.location.y + deltaY / 7 * 3);
                Point q = new Point(targetNode.location.x, sourceNode.location.y + deltaY / 7 * 3);
                Point r = new Point(sourceNode.location.x, sourceNode.location.y + scene.findWidget(cur.source).getBounds().height / 2 - 5);
                Point s = new Point(targetNode.location.x, targetNode.location.y - scene.findWidget(cur.target).getBounds().height / 2 - 4);
                cp.add(r);
                cp.add(p);
                cp.add(q);
                cp.add(s);
                e.setControlPoints(cp, true);
            } else if (cur.tag == 1 && sourceNode.sons.get(0).location.x != sourceNode.location.x) {
                Point p = new Point(sourceNode.location.x, sourceNode.location.y + deltaY / 7 * 3);
                Point q = new Point(targetNode.location.x, sourceNode.location.y + deltaY / 7 * 3);
                Point r = new Point(sourceNode.location.x, sourceNode.location.y + scene.findWidget(cur.source).getBounds().height / 2 - 5);
                Point s = new Point(targetNode.location.x, targetNode.location.y - scene.findWidget(cur.target).getBounds().height / 2 - 4);
                cp.add(r);
                cp.add(p);
                cp.add(q);
                cp.add(s);
                e.setControlPoints(cp, true);
            } else if (cur.tag == 0 && targetNode.order - sourceNode.order > 1) {
                int x = computeX(sourceNode, targetNode);
                Point p = new Point(x, sourceNode.location.y + deltaY / 7 * 3 - 5);
                Point q = new Point(x, targetNode.location.y + deltaY / 7 * 3 - deltaY);
                Point r = new Point(sourceNode.location.x, sourceNode.location.y + scene.findWidget(cur.source).getBounds().height / 2 - 5);
                Point s = new Point(targetNode.location.x, targetNode.location.y - scene.findWidget(cur.target).getBounds().height / 2 - 4);
                Point t = new Point(sourceNode.location.x, sourceNode.location.y + deltaY / 7 * 3 - 5);
                Point u = new Point(targetNode.location.x, targetNode.location.y + deltaY / 7 * 3 - deltaY);
                cp.add(r);
                cp.add(t);
                cp.add(p);
                cp.add(q);
                cp.add(u);
                cp.add(s);
                e.setControlPoints(cp, true);
            } else if (cur.tag == 0 && targetNode.order - sourceNode.order == 1) {
                Point p = new Point(sourceNode.location.x, sourceNode.location.y + deltaY / 7 * 3 - 3);
                Point q = new Point(targetNode.location.x, sourceNode.location.y + deltaY / 7 * 3 - 3);
                Point r = new Point(sourceNode.location.x, sourceNode.location.y + scene.findWidget(cur.source).getBounds().height / 2 - 5);
                Point s = new Point(targetNode.location.x, targetNode.location.y - scene.findWidget(cur.target).getBounds().height / 2 - 4);
                cp.add(r);
                cp.add(p);
                cp.add(q);
                cp.add(s);
                e.setControlPoints(cp, true);
            }
        }
        scene.validate();
    }

    private void displayNode() {
        Point location = new Point();
        for (Node cur : node) cur.location = new Point();
        ArrayList<Node> levelNodes = new ArrayList<Node>();
        int farMostX = 0;
        int adjustX = 0;
        for (int ch = 0; ch < rootSet.size(); ++ch) {
            adjustX = farMostX + deltaX + deltaChannel;
            if (ch == 0) adjustX = deltaX;
            farMostX = 0;
            for (int i = order - 1; i >= 0; --i) {
                levelNodes.clear();
                Iterator e = node.iterator();
                while (e.hasNext()) {
                    Node cur = (Node) e.next();
                    if (cur.order == i && cur.channel == ch) levelNodes.add(cur);
                }
                for (int m = 0; m < levelNodes.size(); ++m) for (int n = m + 1; n < levelNodes.size(); ++n) if (levelNodes.get(n).pre < levelNodes.get(m).pre) {
                    Node tmp = levelNodes.get(n);
                    levelNodes.set(n, levelNodes.get(m));
                    levelNodes.set(m, tmp);
                }
                for (int m = 0; m < levelNodes.size(); ++m) {
                    Node cur = levelNodes.get(m);
                    cur.location.y = cur.order * deltaY + 50;
                    if (cur.sons.size() == 0) {
                        if (m > 0) {
                            Node preNode = levelNodes.get(m - 1);
                            if (cur.location.x - preNode.location.x <= deltaX) cur.location.x = preNode.location.x + deltaX;
                        } else cur.location.x = cur.pre * deltaX - 40 + adjustX;
                    } else {
                        Node firstSon = null;
                        Node lastSon = null;
                        for (Node son : node) {
                            if (son.equals(cur.sons.get(0))) firstSon = son;
                            if (son.equals(cur.sons.get(cur.sons.size() - 1))) lastSon = son;
                        }
                        cur.location.x = (firstSon.location.x + lastSon.location.x) / 2;
                        if (m > 0) {
                            Node preNode = levelNodes.get(m - 1);
                            if (cur.location.x - preNode.location.x <= deltaX) cur.location.x = preNode.location.x + deltaX;
                        }
                    }
                }
                if (!levelNodes.isEmpty() && levelNodes.get(levelNodes.size() - 1).location.x > farMostX) farMostX = levelNodes.get(levelNodes.size() - 1).location.x;
            }
            for (int i = 0; i < 2 * order + 2; ++i) {
                IconNodeWidget sp = new IconNodeWidget(scene);
                sp.setImage(scene.separatorIcon);
                scene.addChild(sp);
                sp.setPreferredLocation(new Point(farMostX + deltaX / 2 + deltaChannel / 2, i * deltaY / 2 + 10));
                scene.separator.add(sp);
            }
            rootSet.get(ch).location.x = adjustX;
        }
        for (Node cur : node) {
            Widget w = scene.addNode(cur.name, cur.type);
            if (w instanceof LabelWidget) ((LabelWidget) w).setLabel(cur.label); else if (w instanceof EditorWidget) ((EditorWidget) w).setLabel(cur.label);
            w.setToolTipText(cur.fullText);
            w.setPreferredLocation(cur.location);
        }
        scene.validate();
        for (Node cur : node) {
            Widget w = scene.findWidget(cur.name);
            int fix = w.getClientArea().width / 2;
            location.x = w.getPreferredLocation().x - fix;
            location.y = w.getPreferredLocation().y;
            w.setPreferredLocation(new Point(location));
        }
    }

    private void computeLayout() {
        for (Edge x : edge) {
            ++getNode(x.source).outDegree;
            ++getNode(x.target).inDegree;
        }
        for (Node x : node) {
            if (x.inDegree == 0) {
                rootSet.add(x);
                x.type = DemoGraphScene.EDITOR_NODE;
            }
        }
        ArrayList<Node> nodeCopy = new ArrayList<Node>();
        for (Node x : node) nodeCopy.add(x);
        ArrayList<Edge> edgeCopy = new ArrayList<Edge>();
        ArrayList<Edge> edgeToBeDel = new ArrayList<Edge>();
        for (Edge x : edge) edgeCopy.add(x);
        order = 0;
        while (!nodeCopy.isEmpty()) {
            ArrayList<Node> curOrderNode = new ArrayList<Node>();
            edgeToBeDel.clear();
            for (Node x : nodeCopy) if (x.inDegree == 0) {
                x.order = order;
                curOrderNode.add(x);
                for (Edge y : edgeCopy) if (y.source.equals(x.name)) edgeToBeDel.add(y);
            }
            for (Edge yy : edgeToBeDel) {
                for (Node z : nodeCopy) if (yy.target.equals(z.name)) --z.inDegree;
                edgeCopy.remove(yy);
            }
            if (curOrderNode.isEmpty()) {
                int minInDegree = 1000;
                Node minInDegreeNode = null;
                for (Node x : nodeCopy) if (x.inDegree < minInDegree) {
                    minInDegree = x.inDegree;
                    minInDegreeNode = x;
                }
                curOrderNode.add(minInDegreeNode);
                edgeToBeDel.clear();
                for (Edge y : edgeCopy) {
                    if (y.source.equals(minInDegreeNode.name)) {
                        for (Node z : nodeCopy) if (y.target.equals(z.name)) --z.inDegree;
                        edgeToBeDel.add(y);
                    }
                }
                for (Edge yy : edgeToBeDel) edgeCopy.remove(yy);
            }
            for (Node xx : curOrderNode) nodeCopy.remove(xx);
            struct.add(curOrderNode);
            order++;
        }
        for (int i = 0; i < struct.size() - 1; ++i) {
            ArrayList<Node> curOrderNode = struct.get(i);
            ArrayList<Node> nextOrderNode = struct.get(i + 1);
            for (Node x : nextOrderNode) {
                for (Node y : curOrderNode) {
                    Edge e = findEdge(y.name, x.name);
                    if (e != null) {
                        y.sons.add(x);
                        e.tag = 1;
                        break;
                    }
                }
            }
            nextOrderNode.clear();
            for (Node x : curOrderNode) for (Node y : x.sons) nextOrderNode.add(y);
        }
        for (Node nn : node) computeReachSet(nn);
        ArrayList<Integer> sonsTmp = new ArrayList<Integer>();
        ArrayList<Node> sonsResult = new ArrayList<Node>();
        int resultConflictCount = 10000;
        for (Node nn : node) {
            if (nn.sons.size() <= 2) continue;
            sonsResult.clear();
            resultConflictCount = 10000;
            boolean conflictRecord[][][] = new boolean[nn.sons.size()][][];
            for (int x = 0; x < nn.sons.size(); ++x) conflictRecord[x] = new boolean[nn.sons.size()][];
            for (int x = 0; x < nn.sons.size(); ++x) for (int y = 0; y < nn.sons.size(); ++y) conflictRecord[x][y] = new boolean[nn.sons.size()];
            for (int x = 0; x < nn.sons.size(); ++x) for (int y = 0; y < nn.sons.size(); ++y) for (int z = 0; z < nn.sons.size(); ++z) if (x != y && y != z && z != x) conflictRecord[x][y][z] = computeConflict(x, y, z, nn.sons);
            int choiceCount = (int) Math.pow(nn.sons.size(), nn.sons.size());
            for (int i = 0; i < choiceCount; ++i) {
                sonsTmp.clear();
                int choiceTmp = i;
                for (int p = 0; p < nn.sons.size(); ++p) {
                    int choiceWeight = choiceTmp - choiceTmp / nn.sons.size() * nn.sons.size();
                    choiceTmp /= nn.sons.size();
                    sonsTmp.add(choiceWeight);
                }
                boolean tag = true;
                for (int j = 0; j < sonsTmp.size() - 1; ++j) for (int k = j + 1; k < sonsTmp.size(); ++k) if (sonsTmp.get(j) == sonsTmp.get(k)) tag = false;
                if (tag == false) continue;
                int conflictCount = 0;
                for (int x = 0; x < sonsTmp.size() - 2; ++x) for (int y = x + 1; y < sonsTmp.size() - 1; ++y) for (int z = y + 1; z < sonsTmp.size(); ++z) if (conflictRecord[sonsTmp.get(x)][sonsTmp.get(y)][sonsTmp.get(z)]) ++conflictCount;
                if (conflictCount < resultConflictCount) {
                    resultConflictCount = conflictCount;
                    sonsResult.clear();
                    for (Integer x : sonsTmp) sonsResult.add(nn.sons.get(x));
                }
            }
            nn.sons.clear();
            nn.sons.addAll(sonsResult);
        }
        for (int i = 0; i < struct.size() - 1; ++i) {
            ArrayList<Node> curOrderNode = struct.get(i);
            ArrayList<Node> nextOrderNode = struct.get(i + 1);
            nextOrderNode.clear();
            for (Node x : curOrderNode) for (Node y : x.sons) nextOrderNode.add(y);
        }
        ArrayList<Node> curOrderNode = new ArrayList<Node>();
        ArrayList<Node> nextOrderNode = new ArrayList<Node>();
        for (int i = 0; i < rootSet.size(); ++i) {
            Node root = rootSet.get(i);
            root.pre = 0;
            root.channel = i;
            curOrderNode.add(root);
            while (!curOrderNode.isEmpty()) {
                int p = 0;
                for (Node x : curOrderNode) {
                    if (p < x.pre) p = x.pre;
                    for (Node y : x.sons) {
                        y.pre = p;
                        y.channel = i;
                        System.out.println(y.label + ":" + p);
                        ++p;
                        nextOrderNode.add(y);
                    }
                }
                ArrayList<Node> tmp = curOrderNode;
                curOrderNode = nextOrderNode;
                nextOrderNode = tmp;
                nextOrderNode.clear();
            }
        }
    }

    private boolean computeConflict(int x, int y, int z, ArrayList<Node> sons) {
        int xy = 1000, xz = 1000, yz = 1000;
        for (Node nn : sons.get(x).reachSet) for (Node nm : sons.get(y).reachSet) if (nn == nm && nn.order < xy) xy = nn.order;
        for (Node nn : sons.get(x).reachSet) for (Node nm : sons.get(z).reachSet) if (nn == nm && nn.order < xz) xz = nn.order;
        for (Node nn : sons.get(y).reachSet) for (Node nm : sons.get(z).reachSet) if (nn == nm && nn.order < yz) yz = nn.order;
        if (xy > xz || yz > xz) return true;
        return false;
    }

    private void computeReachSet(Node x) {
        x.reachSet.add(x);
        ArrayList<Node> newNode = new ArrayList<Node>();
        while (true) {
            newNode.clear();
            for (Node nn : x.reachSet) for (Edge e : edge) if (nn.name.equals(e.source)) newNode.add(getNode(e.target));
            if (newNode.isEmpty()) break;
            int countBeforeAdd = x.reachSet.size();
            x.reachSet.addAll(newNode);
            int countAfterAdd = x.reachSet.size();
            if (countBeforeAdd == countAfterAdd) break;
        }
        x.reachSet.remove(x);
    }

    private void createNodes() {
        try {
            URL url = this.getClass().getResource("NodesFile.txt");
            InputStreamReader inReader = new InputStreamReader(url.openStream());
            BufferedReader inNodes = new BufferedReader(inReader);
            String s;
            while ((s = inNodes.readLine()) != null) {
                String label = inNodes.readLine();
                String fullText = inNodes.readLine();
                String type = inNodes.readLine();
                Node n = new Node(s, type);
                n.label = label;
                n.fullText = fullText;
                node.add(n);
            }
            inNodes.close();
            url = this.getClass().getResource("EdgesFile.txt");
            inReader = new InputStreamReader(url.openStream());
            BufferedReader inEdges = new BufferedReader(inReader);
            while ((s = inEdges.readLine()) != null) edge.add(new Edge(s, inEdges.readLine(), inEdges.readLine(), inEdges.readLine()));
            inEdges.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearScene() {
        scene.clearSeparator();
        scene.setSelectedAreaVisible(false);
        Collection<String> edges = scene.getEdges();
        Collection<String> edgesToBeDel = new ArrayList<String>();
        Iterator e = edges.iterator();
        while (e.hasNext()) {
            String edge = (String) e.next();
            edgesToBeDel.add(edge);
        }
        e = edgesToBeDel.iterator();
        while (e.hasNext()) scene.removeEdge((String) e.next());
        Collection<String> nodes = scene.getNodes();
        Collection<String> nodesToBeDel = new ArrayList<String>();
        e = nodes.iterator();
        while (e.hasNext()) {
            String node = (String) e.next();
            nodesToBeDel.add(node);
        }
        e = nodesToBeDel.iterator();
        while (e.hasNext()) scene.removeNode((String) e.next());
        scene.validate();
    }

    private int computeX(Node source, Node target) {
        int result = 0;
        if (source.sons.isEmpty()) {
            Node brother = null;
            ArrayList<Node> sourceLevel = struct.get(source.order);
            for (int i = 1; i < sourceLevel.size(); ++i) {
                if (sourceLevel.get(i) == source) brother = sourceLevel.get(i - 1);
            }
            if (brother == null || brother.sons.isEmpty()) return source.location.x;
            ArrayList<Node> levelNodes = new ArrayList<Node>();
            ArrayList<Node> tmp = new ArrayList<Node>();
            levelNodes.add(brother);
            for (int i = 0; i < target.order - brother.order - 1; ++i) {
                tmp.clear();
                for (Node nn : levelNodes) tmp.addAll(nn.sons);
                levelNodes.clear();
                levelNodes.addAll(tmp);
                if (levelNodes.isEmpty()) break;
                Node farNode = levelNodes.get(levelNodes.size() - 1);
                if (farNode.location.x > result) result = farNode.location.x;
            }
            if (result >= source.location.x) return result + deltaX / 2 + 10; else return source.location.x;
        }
        ArrayList<Node> levelNodes = new ArrayList<Node>();
        ArrayList<Node> tmp = new ArrayList<Node>();
        levelNodes.add(source);
        for (int i = 0; i < target.order - source.order - 1; ++i) {
            tmp.clear();
            for (Node nn : levelNodes) tmp.addAll(nn.sons);
            levelNodes.clear();
            levelNodes.addAll(tmp);
            if (levelNodes.isEmpty()) return result;
            Node farNode = levelNodes.get(levelNodes.size() - 1);
            if (farNode.location.x > result) result = farNode.location.x;
        }
        return result + deltaX / 2 + 10;
    }

    private Edge findEdge(String source, String target) {
        for (Edge x : edge) if (x.source.equals(source) && x.target.equals(target)) return x;
        return null;
    }

    private Node getNode(String name) {
        for (Node x : node) if (x.name.equals(name)) return x;
        return null;
    }

    private ArrayList<ArrayList<Node>> struct = new ArrayList<ArrayList<Node>>();

    private ArrayList<Node> rootSet = new ArrayList<Node>();

    private ArrayList<Node> node = new ArrayList<Node>();

    private ArrayList<Edge> edge = new ArrayList<Edge>();

    private DemoGraphScene scene;

    private int order;

    private static final int deltaX = 170;

    private static final int deltaY = 80;

    private static final int deltaChannel = 200;
}
