package cn.edu.pku.dr.requirement.elicitation.tools;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
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
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;

import org.netbeans.api.visual.widget.*;
import org.netbeans.api.visual.widget.general.IconNodeWidget;

public class Draw {
    public Draw(DemoGraphScene scene, DemoGraphScene editorScene,
            String nodeFileName, String edgeFileName) {
        this.scene = scene;
        this.editorScene = editorScene;
        this.nodeFileName = nodeFileName;
        this.edgeFileName = edgeFileName;
    }

    public void start() {
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

    public void test() {
        this.clearScene();
        Widget w = scene.addNode("123", DemoGraphScene.RECTANGLE_NODE);
        w.setPreferredLocation(new Point(10, 10));
    }

    private void displayEdge() {
        Collection<Point> cp = new ArrayList<Point>();
        loop: for (Edge cur: edge) {
            // ɾ���editor�����l�ıߡ�������߲�Ӧ�ñ���ʾ��4��
            for (Node x: rootSet) {
                if (x.equals(this.getNode(cur.source)))
                    continue loop;
            }

            cp.clear();
            ConnectionWidget e = (ConnectionWidget) scene.addEdge(cur.name);
            scene.setEdgeSource(cur.name, cur.source);
            scene.setEdgeTarget(cur.name, cur.target);
            // for(Point p: e.getControlPoints())
            // cp.add(p);
            Node sourceNode = null;
            Node targetNode = null;
            Iterator i = node.iterator();
            while (i.hasNext()) {
                Node n = (Node) i.next();
                if (n.name.equals(cur.source))
                    sourceNode = n;
                if (n.name.equals(cur.target))
                    targetNode = n;
            }

            // if(cur.tag == 1)
            // e.setLineColor(Color.RED);

            if (cur.tag == 1 && sourceNode.sons.size() > 1) {
                Point p = new Point(sourceNode.location.x + deltaX / 2,
                        sourceNode.location.y - 16);
                Point q = new Point(sourceNode.location.x + deltaX / 2,
                        targetNode.location.y - 16);
                Point r = new Point(sourceNode.location.x
                        + scene.findWidget(cur.source).getBounds().width / 2,
                        sourceNode.location.y - 16);
                Point s = new Point(targetNode.location.x
                        - scene.findWidget(cur.target).getBounds().width / 2,
                        targetNode.location.y - 16);
                // Point r = new Point(sourceNode.location.x,
                // sourceNode.location.y);
                // Point s = new Point(targetNode.location.x,
                // targetNode.location.y);
                cp.add(r);
                cp.add(p);
                cp.add(q);
                cp.add(s);
                e.setControlPoints(cp, true);
            } else if (cur.tag == 1
                    && sourceNode.sons.get(0).location.y != sourceNode.location.y) {
                Point p = new Point(sourceNode.location.x + deltaX / 2,
                        sourceNode.location.y - 16);
                Point q = new Point(sourceNode.location.x + deltaX / 2,
                        targetNode.location.y - 16);
                Point r = new Point(sourceNode.location.x
                        + scene.findWidget(cur.source).getBounds().width / 2,
                        sourceNode.location.y - 16);
                Point s = new Point(targetNode.location.x
                        - scene.findWidget(cur.target).getBounds().width / 2,
                        targetNode.location.y - 16);
                cp.add(r);
                cp.add(p);
                cp.add(q);
                cp.add(s);
                e.setControlPoints(cp, true);
            } else if (cur.tag == 0 && targetNode.order - sourceNode.order > 1) {
                int y = computeY(sourceNode, targetNode);
                // Point p = new Point(sourceNode.location.x + deltaX/7*3,
                // sourceNode.location.y-13);
                // Point q = new Point(sourceNode.location.x + deltaX/7*3,
                // targetNode.location.y-13);
                // Point r = new Point(sourceNode.location.x +
                // scene.findWidget(cur.source).getBounds().width/2,
                // sourceNode.location.y-13);
                // Point s = new Point(targetNode.location.x -
                // scene.findWidget(cur.target).getBounds().width/2,
                // targetNode.location.y-13);
                // Point t = new Point(sourceNode.location.x,
                // sourceNode.location.y + deltaY/7*3-5);
                // Point u = new Point(targetNode.location.x,
                // targetNode.location.y + deltaY/7*3 - deltaY);

                Point p = new Point(sourceNode.location.x + deltaX / 2, y - 16);// Point
                                                                                // p =
                                                                                // new
                                                                                // Point(sourceNode.location.x
                                                                                // +
                                                                                // deltaX/2-3,
                                                                                // y-16);
                Point q = new Point(
                        targetNode.location.x + deltaX / 2 - deltaX, y - 16);
                Point r = new Point(sourceNode.location.x
                        + scene.findWidget(cur.source).getBounds().width / 2,
                        sourceNode.location.y - 16);
                Point s = new Point(targetNode.location.x
                        - scene.findWidget(cur.target).getBounds().width / 2,
                        targetNode.location.y - 16);
                Point t = new Point(sourceNode.location.x + deltaX / 2,
                        sourceNode.location.y - 16);// Point t = new
                                                    // Point(sourceNode.location.x
                                                    // + deltaX/2-3,
                                                    // sourceNode.location.y-16);
                Point u = new Point(
                        targetNode.location.x + deltaX / 2 - deltaX,
                        targetNode.location.y - 16);
                /*
                 * Point s; if(x < targetNode.location.x) s = new
                 * Point(targetNode.location.x -
                 * scene.findWidget(cur.target).getBounds().width/2,
                 * targetNode.location.y-4); else s = new
                 * Point(targetNode.location.x +
                 * scene.findWidget(cur.target).getBounds().width/2,
                 * targetNode.location.y-4);
                 */
                cp.add(r);
                cp.add(t);
                cp.add(p);
                cp.add(q);
                cp.add(u);
                cp.add(s);
                e.setControlPoints(cp, true);
            } else if (cur.tag == 0 && targetNode.order - sourceNode.order == 1) {
                Point p = new Point(sourceNode.location.x + deltaX / 2,
                        sourceNode.location.y - 16);
                Point q = new Point(sourceNode.location.x + deltaX / 2,
                        targetNode.location.y - 16);
                Point r = new Point(sourceNode.location.x
                        + scene.findWidget(cur.source).getBounds().width / 2,
                        sourceNode.location.y - 16);
                Point s = new Point(targetNode.location.x
                        - scene.findWidget(cur.target).getBounds().width / 2,
                        targetNode.location.y - 16);
                // Point p = new Point(sourceNode.location.x,
                // sourceNode.location.y + deltaY/7*3-3);
                // Point q = new Point(targetNode.location.x,
                // sourceNode.location.y + deltaY/7*3-3);
                // Point r = new Point(sourceNode.location.x,
                // sourceNode.location.y +
                // scene.findWidget(cur.source).getBounds().height/2-5);
                // Point s = new Point(targetNode.location.x,
                // targetNode.location.y -
                // scene.findWidget(cur.target).getBounds().height/2-4);
                cp.add(r);
                cp.add(p);
                cp.add(q);
                cp.add(s);
                e.setControlPoints(cp, true);
            }

            if (!cur.toolTipsText.equals("null")
                    && e.getControlPoints().size() >= 3) {
                e.setToolTipText(cur.toolTipsText);
                Widget tips;
                Point tipsLocation = new Point(e.getControlPoints().get(2));
                if (cur.toolTipsText.length() > 8) {
                    tips = scene.addNode(cur.toolTipsText.substring(0, 8)
                            + "..", DemoGraphScene.EDGETIP_NODE);
                    tipsLocation.x -= 15;
                    tipsLocation.y -= 5;
                } else {
                    tips = scene.addNode(cur.toolTipsText,
                            DemoGraphScene.EDGETIP_NODE);
                    tipsLocation.x += 5;
                    tipsLocation.y -= 5;
                }

                tips.setPreferredLocation(tipsLocation);
            }

        }
        scene.validate();
    }

    private void displayNode() {
        Point location = new Point();
        // ��ʼ����Ľ�㲼�֣�������һ��Ҷ������Ҳ�ƫ��ʾ����
        for (Node cur: node)
            cur.location = new Point();

        // ��Ҷ������Ҳ�ƫ��ʾ����ĳɾ�����ʾ����
        // �����У��ӵ���ڶ��㿪ʼ���Ե����ϣ��������ң�ÿһ��������һ������������ʾ
        ArrayList<Node> levelNodes = new ArrayList<Node>(); // ĳ����ļ���
        int farMostY = 0;
        int adjustY = 0;
        for (int ch = 0; ch < rootSet.size(); ++ch) {
            adjustY = farMostY + deltaChannel + deltaY;
            if (ch == 0)
                adjustY = deltaChannel / 2 + deltaY;
            farMostY = 0;
            for (int i = order - 1; i >= 0; --i) {
                // �õ���i����ļ��ϣ���levelNodes��¼
                levelNodes.clear();
                Iterator e = node.iterator();
                while (e.hasNext()) {
                    Node cur = (Node) e.next();
                    if (cur.order == i && cur.channel == ch)
                        levelNodes.add(cur);
                }

                // ��pre,�Ե�i��Ľ���������
                for (int m = 0; m < levelNodes.size(); ++m)
                    for (int n = m + 1; n < levelNodes.size(); ++n)
                        if (levelNodes.get(n).pre < levelNodes.get(m).pre) {
                            Node tmp = levelNodes.get(n);
                            levelNodes.set(n, levelNodes.get(m));
                            levelNodes.set(m, tmp);

                        }

                // �Ե�i��Ľ����о��в���
                if (i != 0) {
                    for (int m = 0; m < levelNodes.size(); ++m) {
                        Node cur = levelNodes.get(m);
                        cur.location.x = cur.order * deltaX - deltaX / 2;
                        if (cur.sons.size() == 0) {
                            // curû�ж��ӣ����cur����ߵĵ�һ���ֵܶ�cur���в���
                            if (m > 0) {
                                Node preNode = levelNodes.get(m - 1);
                                if (cur.location.y - preNode.location.y <= deltaY)
                                    cur.location.y = preNode.location.y
                                            + deltaY;
                            } else
                                cur.location.y = cur.pre * deltaY + adjustY;
                        } else {
                            Node firstSon = null;
                            Node lastSon = null;
                            for (Node son: node) {
                                if (son.equals(cur.sons.get(0)))
                                    firstSon = son;
                                if (son.equals(cur.sons
                                        .get(cur.sons.size() - 1)))
                                    lastSon = son;
                            }
                            cur.location.y = (firstSon.location.y + lastSon.location.y) / 2;
                            if (m > 0) {
                                Node preNode = levelNodes.get(m - 1);
                                if (cur.location.y - preNode.location.y <= deltaY)
                                    cur.location.y = preNode.location.y
                                            + deltaY;
                            }
                        }
                    }
                } else {
                    for (int m = 0; m < levelNodes.size(); ++m) {
                        Node cur = levelNodes.get(m);
                        cur.location.x = deltaX / 6;
                    }
                }

                if (!levelNodes.isEmpty()
                        && levelNodes.get(levelNodes.size() - 1).location.y > farMostY)
                    farMostY = levelNodes.get(levelNodes.size() - 1).location.y;
            }

            // Ӿ�7ָ���
            if (ch != rootSet.size() - 1) {
                for (int i = 0; i < 4; ++i) {
                    IconNodeWidget sp = new IconNodeWidget(editorScene);
                    sp.setImage(editorScene.separatorIcon);
                    editorScene.addChild(sp);
                    sp.setPreferredLocation(new Point(i * deltaX / 7 + 5,
                            farMostY + deltaChannel / 2 + 1));
                    editorScene.separator.add(sp);
                }

                for (int i = 0; i < 6 * order + 6; ++i) {
                    IconNodeWidget sp = new IconNodeWidget(scene);
                    sp.setImage(scene.separatorIcon);
                    // sp.setLabel("xxx");
                    scene.addChild(sp);
                    sp.setPreferredLocation(new Point(i * deltaX / 6, farMostY
                            + deltaChannel / 2));
                    scene.separator.add(sp);
                }
            } else {
                for (int i = 0; i < 8; ++i) {
                    IconNodeWidget sp = new IconNodeWidget(editorScene);
                    sp.setImage(editorScene.separatorIcon);
                    editorScene.addChild(sp);
                    sp.setPreferredLocation(new Point(i * deltaX / 14, farMostY
                            + deltaChannel / 2 + 9));
                    editorScene.separator.add(sp);
                }
                IconNodeWidget sp = new IconNodeWidget(editorScene);
                sp.setImage(editorScene.separatorIcon);
                editorScene.addChild(sp);
                sp.setPreferredLocation(new Point(71, farMostY + deltaChannel
                        / 2 + 9));
                editorScene.separator.add(sp);
            }

            // ����editor��λ�ã�ʹ����ʾ�ڿ���Ӿ�7ָ��ߵĵط�
            rootSet.get(ch).location.y = (adjustY + farMostY) / 2 - deltaY / 2;
            rootSet.get(ch).maxLength = farMostY - adjustY + 2 * deltaY;
        }
        /*
         * for(Node cur:node){ Widget w = scene.addNode(cur.name, cur.type);
         * if(w instanceof LabelWidget) ((LabelWidget)w).setLabel(cur.label);
         * else if(w instanceof EditorWidget)
         * ((EditorWidget)w).setLabel(cur.label);
         * w.setToolTipText(cur.fullText); w.setPreferredLocation(cur.location); }
         */
        for (Node cur: node) {
            if (cur.type.equals(DemoGraphScene.EDITOR_NODE)) {
                Widget w = editorScene.addNode(cur.name, cur.type);
                ((EditorWidget) w).setLabel(cur.label);
                ((EditorWidget) w).setMaxY(cur.maxLength);
                w.setToolTipText(cur.fullText);
                w.setPreferredLocation(cur.location);
            } else {
                Widget w = scene.addNode(cur.name, cur.type);
                ((LabelWidget) w).setLabel(cur.label);
                w.setToolTipText(cur.fullText);
                w.setPreferredLocation(cur.location);
            }
        }

        /*
         * ��editor��Ӿ��֮��ķָ��� for(int i = 0; i < (farMostY+
         * deltaChannel/2-10)/10; ++i){ IconNodeWidget sp = new
         * IconNodeWidget(scene); sp.setImage(scene.separatorIcon2);
         * scene.addChild(sp); sp.setPreferredLocation(new Point(deltaX/2,
         * i*10)); scene.separator.add(sp); } for(int i = 0; i < (farMostY+
         * deltaChannel/2-10)/10; ++i){ IconNodeWidget sp = new
         * IconNodeWidget(scene); sp.setImage(scene.separatorIcon2);
         * scene.addChild(sp); sp.setPreferredLocation(new Point(0, i*10));
         * scene.separator.add(sp); } for(int i = 0; i < (farMostY+
         * deltaChannel/2-10)/10; ++i){ IconNodeWidget sp = new
         * IconNodeWidget(scene); sp.setImage(scene.separatorIcon2);
         * scene.addChild(sp); sp.setPreferredLocation(new
         * Point(deltaX*(order+1), i*10)); scene.separator.add(sp); } for(int i =
         * 0; i < 6 * order + 6; ++i){ IconNodeWidget sp = new
         * IconNodeWidget(scene); sp.setImage(scene.separatorIcon); //
         * sp.setLabel("xxx"); scene.addChild(sp); sp.setPreferredLocation(new
         * Point(i*deltaX/6, 0)); scene.separator.add(sp); }
         */

        scene.validate();
        editorScene.validate();

        // ��ݽ������΢�����
        for (Node cur: node) {
            if (!cur.type.equals(DemoGraphScene.EDITOR_NODE)) {
                Widget w = scene.findWidget(cur.name);
                int fixX = w.getClientArea().width / 2;
                int fixY = w.getClientArea().height / 2;
                location.x = w.getPreferredLocation().x - fixX;
                location.y = w.getPreferredLocation().y - fixY;
                w.setPreferredLocation(new Point(location));
            }
        }

    }

    private void computeLayout() {
        for (Edge x: edge) {
            ++getNode(x.source).outDegree;
            ++getNode(x.target).inDegree;
        }

        // ��¼��ڵ㣬����Ӿ��ʱʹ��
        for (Node x: node)
            if (x.inDegree == 0) {
                rootSet.add(x);
                // ��һ���㣬��Ӿ�5ĸ��㣬�Ǹ�Ӿ�5�editor
                x.type = DemoGraphScene.EDITOR_NODE;
            }

        // ��������
        ArrayList<Node> nodeCopy = new ArrayList<Node>();
        for (Node x: node)
            nodeCopy.add(x);

        ArrayList<Edge> edgeCopy = new ArrayList<Edge>();
        ArrayList<Edge> edgeToBeDel = new ArrayList<Edge>();
        for (Edge x: edge)
            edgeCopy.add(x);

        order = 0;
        while (!nodeCopy.isEmpty()) {
            ArrayList<Node> curOrderNode = new ArrayList<Node>();
            edgeToBeDel.clear();
            for (Node x: nodeCopy)
                if (x.inDegree == 0) {
                    x.order = order;
                    curOrderNode.add(x);
                    for (Edge y: edgeCopy)
                        if (y.source.equals(x.name))
                            edgeToBeDel.add(y);
                }
            for (Edge yy: edgeToBeDel) {
                for (Node z: nodeCopy)
                    if (yy.target.equals(z.name))
                        --z.inDegree;
                edgeCopy.remove(yy);
            }

            if (curOrderNode.isEmpty()) {
                int minInDegree = 1000;
                Node minInDegreeNode = null;
                for (Node x: nodeCopy)
                    if (x.inDegree < minInDegree) {
                        minInDegree = x.inDegree;
                        minInDegreeNode = x;
                    }
                curOrderNode.add(minInDegreeNode);
                edgeToBeDel.clear();
                for (Edge y: edgeCopy) {
                    if (y.source.equals(minInDegreeNode.name)) {
                        for (Node z: nodeCopy)
                            if (y.target.equals(z.name))
                                --z.inDegree;
                        edgeToBeDel.add(y);
                    }
                }
                for (Edge yy: edgeToBeDel)
                    edgeCopy.remove(yy);
            }

            for (Node xx: curOrderNode)
                nodeCopy.remove(xx);
            struct.add(curOrderNode);
            order++;
        }

        // ��ȡ�������е���ṹ������ṹˢ��struct�ṹ
        for (int i = 0; i < struct.size() - 1; ++i) {
            ArrayList<Node> curOrderNode = struct.get(i);
            ArrayList<Node> nextOrderNode = struct.get(i + 1);
            for (Node x: nextOrderNode) {
                for (Node y: curOrderNode) {
                    Edge e = findEdge(y.name, x.name);
                    if (e != null) {
                        y.sons.add(x);
                        e.tag = 1;
                        break;
                    }

                }
            }
            nextOrderNode.clear();
            for (Node x: curOrderNode)
                for (Node y: x.sons)
                    nextOrderNode.add(y);
        }

        // Ϊÿ�����sons���ҵ���ͻ���ٵ�˳��
        // Ϊ�ˣ����ȼ���ÿ����Ŀɴ��㼯��
        for (Node nn: node)
            computeReachSet(nn);

        ArrayList<Integer> sonsTmp = new ArrayList<Integer>();
        ArrayList<Node> sonsResult = new ArrayList<Node>();
        int resultConflictCount = 10000;
        for (Node nn: node) {
            if (nn.sons.size() <= 2)
                continue;
            sonsResult.clear();
            resultConflictCount = 10000;
            boolean conflictRecord[][][] = new boolean[nn.sons.size()][][];
            for (int x = 0; x < nn.sons.size(); ++x)
                conflictRecord[x] = new boolean[nn.sons.size()][];
            for (int x = 0; x < nn.sons.size(); ++x)
                for (int y = 0; y < nn.sons.size(); ++y)
                    conflictRecord[x][y] = new boolean[nn.sons.size()];

            for (int x = 0; x < nn.sons.size(); ++x)
                for (int y = 0; y < nn.sons.size(); ++y)
                    for (int z = 0; z < nn.sons.size(); ++z)
                        if (x != y && y != z && z != x)
                            conflictRecord[x][y][z] = computeConflict(x, y, z,
                                    nn.sons);

            int choiceCount = (int) Math.pow(nn.sons.size(), nn.sons.size());
            for (int i = 0; i < choiceCount; ++i) {
                sonsTmp.clear();
                int choiceTmp = i;
                // ��һ����ӽڵ������
                for (int p = 0; p < nn.sons.size(); ++p) {
                    int choiceWeight = choiceTmp - choiceTmp / nn.sons.size()
                            * nn.sons.size();
                    choiceTmp /= nn.sons.size();
                    sonsTmp.add(choiceWeight);
                    // sonsTmp.add(nn.sons.get(choiceWeight));
                }

                // ���������������ظ���Ԫ�أ������������Ч
                boolean tag = true;
                for (int j = 0; j < sonsTmp.size() - 1; ++j)
                    for (int k = j + 1; k < sonsTmp.size(); ++k)
                        if (sonsTmp.get(j) == sonsTmp.get(k))
                            tag = false;
                if (tag == false)
                    continue;

                // ����Ч��������д���
                int conflictCount = 0;
                for (int x = 0; x < sonsTmp.size() - 2; ++x)
                    for (int y = x + 1; y < sonsTmp.size() - 1; ++y)
                        for (int z = y + 1; z < sonsTmp.size(); ++z)
                            if (conflictRecord[sonsTmp.get(x)][sonsTmp.get(y)][sonsTmp
                                    .get(z)])
                                ++conflictCount;

                if (conflictCount < resultConflictCount) {
                    resultConflictCount = conflictCount;
                    sonsResult.clear();
                    for (Integer x: sonsTmp)
                        sonsResult.add(nn.sons.get(x));
                }
            }
            nn.sons.clear();
            nn.sons.addAll(sonsResult);
        }

        // ˢ��struct�ṹ
        for (int i = 0; i < struct.size() - 1; ++i) {
            ArrayList<Node> curOrderNode = struct.get(i);
            ArrayList<Node> nextOrderNode = struct.get(i + 1);
            nextOrderNode.clear();
            for (Node x: curOrderNode)
                for (Node y: x.sons)
                    nextOrderNode.add(y);
        }

        // ����Ӿ��
        ArrayList<Node> curOrderNode = new ArrayList<Node>();
        ArrayList<Node> nextOrderNode = new ArrayList<Node>();
        // int farMostPre = 0;
        for (int i = 0; i < rootSet.size(); ++i) {
            Node root = rootSet.get(i);
            // root.pre = farMostPre;
            root.pre = 0;
            root.channel = i;
            // farMostPre = 0;
            curOrderNode.add(root);
            while (!curOrderNode.isEmpty()) {
                int p = 0;
                for (Node x: curOrderNode) {
                    if (p < x.pre)
                        p = x.pre;
                    for (Node y: x.sons) {
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
                // if(p > farMostPre) farMostPre = p;
            }
        }

    }

    private boolean computeConflict(int x, int y, int z, ArrayList<Node> sons) {
        int xy = 1000, xz = 1000, yz = 1000;

        for (Node nn: sons.get(x).reachSet)
            for (Node nm: sons.get(y).reachSet)
                if (nn == nm && nn.order < xy)
                    xy = nn.order;

        for (Node nn: sons.get(x).reachSet)
            for (Node nm: sons.get(z).reachSet)
                if (nn == nm && nn.order < xz)
                    xz = nn.order;

        for (Node nn: sons.get(y).reachSet)
            for (Node nm: sons.get(z).reachSet)
                if (nn == nm && nn.order < yz)
                    yz = nn.order;

        if (xy > xz || yz > xz)
            return true;
        return false;

    }

    private void computeReachSet(Node x) {
        x.reachSet.add(x);
        ArrayList<Node> newNode = new ArrayList<Node>();
        while (true) {
            newNode.clear();
            for (Node nn: x.reachSet)
                for (Edge e: edge)
                    if (nn.name.equals(e.source))
                        newNode.add(getNode(e.target));

            if (newNode.isEmpty())
                break;
            int countBeforeAdd = x.reachSet.size();
            x.reachSet.addAll(newNode);
            int countAfterAdd = x.reachSet.size();
            if (countBeforeAdd == countAfterAdd)
                break;
        }
        x.reachSet.remove(x);
    }

    /*
     * private boolean isConflict(Node x, Node y, Node z){ int xy = 1000, xz =
     * 1000, yz = 1000; for(Node nn:x.reachSet) for(Node nm:y.reachSet) if(nn ==
     * nm && nn.order < xy) xy = nn.order; for(Node nn:x.reachSet) for(Node
     * nm:z.reachSet) if(nn == nm && nn.order < xz) xz = nn.order; for(Node
     * nn:y.reachSet) for(Node nm:z.reachSet) if(nn == nm && nn.order < yz) yz =
     * nn.order; if( xy > xz || yz > xz ) return true; return false; }
     */
    /*
     * private void computeLayout(){ //��ʶ���н����ȷ�0�Ľ�㣬����order����Ϊ-1
     * Iterator e = edge.iterator(); while(e.hasNext()){ Edge edge =
     * (Edge)e.next(); Iterator i = node.iterator(); while(i.hasNext()){ Node
     * targetNode = (Node)i.next(); if(edge.target.equals(targetNode.name)){
     * targetNode.order = -1; } } } //����order��Ϊ0�Ľ����ȱ�ȻΪ0��������Ϊ����
     * ArrayList<Node> rootSet = new ArrayList<Node>(); Iterator i =
     * node.iterator(); while(i.hasNext()){ Node cur = (Node)i.next();
     * if(cur.order == 0) rootSet.add(cur); } //������ȱ��� for(int m = 0; m <
     * rootSet.size(); ++m){ Node root = rootSet.get(m); root.pre = m * 3 + 1; }
     * ArrayList<Node> nextLayerNodes = new ArrayList<Node>();
     * nextLayerNodes.clear(); order = 1; while(true){ i = rootSet.iterator();
     * if(!i.hasNext()) break; while(i.hasNext()){ Node cur = (Node)i.next();
     * int startPoint = 0; //Ϊ�˹���Ҷ���һֱ��ƫ����ṹ if(nextLayerNodes.size() <
     * cur.pre) startPoint = cur.pre - 1 - nextLayerNodes.size(); e =
     * edge.iterator(); while(e.hasNext()){ Edge edge = (Edge)e.next();
     * if(cur.name.equals(edge.source)){ for(Node target:node)
     * if(target.name.equals(edge.target) && target.order == -1){ target.pre =
     * nextLayerNodes.size() + 1 + startPoint; target.order = order;
     * nextLayerNodes.add(target); cur.sons.add(getNode(edge.target)); edge.tag =
     * 1; break; } } } } rootSet = nextLayerNodes; nextLayerNodes = new
     * ArrayList<Node>(); nextLayerNodes.clear(); order++; } order--; }
     */

    private void createNodes() {
        try {
            URL url = this.getClass().getResource(this.nodeFileName);
            InputStreamReader inReader = new InputStreamReader(url.openStream());
            BufferedReader inNodes = new BufferedReader(inReader);

            // BufferedReader inNodes = new BufferedReader(new
            // FileReader("NodesFile.txt"));
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

            url = this.getClass().getResource(this.edgeFileName);
            inReader = new InputStreamReader(url.openStream());
            BufferedReader inEdges = new BufferedReader(inReader);
            // BufferedReader inEdges = new BufferedReader(new
            // FileReader("EdgesFile.txt"));
            while ((s = inEdges.readLine()) != null)
                edge.add(new Edge(s, inEdges.readLine(), inEdges.readLine(),
                        inEdges.readLine()));
            inEdges.close();
        } catch (FileNotFoundException e) {
            // TODO �Զ���� catch ��
            e.printStackTrace();
        } catch (IOException e) {
            // TODO �Զ���� catch ��
            e.printStackTrace();
        }
        /*
         * for(Myparser.Nd x:FreeConnectTest.pNd){ Node n = new Node(x.id,
         * x.type); n.label = x.label; node.add(n); } for(Myparser.Ed
         * x:FreeConnectTest.pEd) edge.add(new Edge(x.id, x.source.id,
         * x.target.id));
         */
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
        while (e.hasNext())
            scene.removeEdge((String) e.next());

        Collection<String> nodes = scene.getNodes();
        Collection<String> nodesToBeDel = new ArrayList<String>();
        e = nodes.iterator();
        while (e.hasNext()) {
            String node = (String) e.next();
            nodesToBeDel.add(node);
        }
        e = nodesToBeDel.iterator();
        while (e.hasNext())
            scene.removeNode((String) e.next());

        // ������ϵı�ǽ��
        List<Widget> l = scene.mainLayer.getChildren();
        List<Widget> toBeDel = new ArrayList<Widget>();
        for (Widget x: l) {
            if (x instanceof LabelWidget)
                toBeDel.add(x);
        }
        Iterator ee = toBeDel.iterator();
        while (ee.hasNext())
            scene.mainLayer.removeChild(((Widget) ee.next()));

        scene.validate();

        editorScene.clearSeparator();
        editorScene.setSelectedAreaVisible(false);
        Collection<String> edges1 = editorScene.getEdges();
        Collection<String> edgesToBeDel1 = new ArrayList<String>();
        Iterator e1 = edges1.iterator();
        while (e1.hasNext()) {
            String edge1 = (String) e1.next();
            edgesToBeDel1.add(edge1);
        }
        e1 = edgesToBeDel1.iterator();
        while (e1.hasNext())
            editorScene.removeEdge((String) e1.next());

        Collection<String> nodes1 = editorScene.getNodes();
        Collection<String> nodesToBeDel1 = new ArrayList<String>();
        e1 = nodes1.iterator();
        while (e1.hasNext()) {
            String node = (String) e1.next();
            nodesToBeDel1.add(node);
        }
        e1 = nodesToBeDel1.iterator();
        while (e1.hasNext())
            editorScene.removeNode((String) e1.next());
        editorScene.validate();
    }

    private int computeY(Node source, Node target) {
        int result = 0;// source.location.x;
        // if(source.sons.isEmpty()) return result;
        if (source.sons.isEmpty()) {
            Node brother = null;
            ArrayList<Node> sourceLevel = struct.get(source.order);
            for (int i = 1; i < sourceLevel.size(); ++i) {
                if (sourceLevel.get(i) == source)
                    brother = sourceLevel.get(i - 1);
            }
            if (brother == null || brother.sons.isEmpty())
                return source.location.y;

            ArrayList<Node> levelNodes = new ArrayList<Node>();
            ArrayList<Node> tmp = new ArrayList<Node>();
            levelNodes.add(brother);
            for (int i = 0; i < target.order - brother.order - 1; ++i) {
                tmp.clear();
                for (Node nn: levelNodes)
                    tmp.addAll(nn.sons);
                levelNodes.clear();
                levelNodes.addAll(tmp);
                if (levelNodes.isEmpty())
                    break;
                Node farNode = levelNodes.get(levelNodes.size() - 1);
                if (farNode.location.y > result)
                    result = farNode.location.y;
            }

            if (result >= source.location.y)
                return result + deltaY / 2 + 10;
            else
                return source.location.y;

        }

        ArrayList<Node> levelNodes = new ArrayList<Node>();
        ArrayList<Node> tmp = new ArrayList<Node>();
        levelNodes.add(source);
        for (int i = 0; i < target.order - source.order - 1; ++i) {
            tmp.clear();
            for (Node nn: levelNodes)
                tmp.addAll(nn.sons);
            levelNodes.clear();
            levelNodes.addAll(tmp);
            if (levelNodes.isEmpty())
                return result;
            Node farNode = levelNodes.get(levelNodes.size() - 1);
            if (farNode.location.y > result)
                result = farNode.location.y;
        }

        return result + deltaY / 2 + 10;
    }

    private Edge findEdge(String source, String target) {
        for (Edge x: edge)
            if (x.source.equals(source) && x.target.equals(target))
                return x;
        return null;
    }

    private Node getNode(String name) {
        for (Node x: node)
            if (x.name.equals(name))
                return x;
        return null;
    }

    private ArrayList<ArrayList<Node>> struct = new ArrayList<ArrayList<Node>>();

    private ArrayList<Node> rootSet = new ArrayList<Node>();

    private ArrayList<Node> node = new ArrayList<Node>();

    private ArrayList<Edge> edge = new ArrayList<Edge>();

    private DemoGraphScene scene;

    private DemoGraphScene editorScene;

    private int order; // ��ߣ��ֲ�ͼ�ṹ�Ĳ���

    private final static int deltaX = 135;

    private final static int deltaY = 35;

    private final static int deltaChannel = 60;

    private String nodeFileName;

    private String edgeFileName;
}
