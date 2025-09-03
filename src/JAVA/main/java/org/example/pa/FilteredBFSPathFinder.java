package org.example.pa;

import java.util.*;
import java.util.function.Predicate;

import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.traverse.BFSPathFinder;
import jakarta.annotation.Nullable;

public class FilteredBFSPathFinder<T> extends BFSPathFinder {
    private final boolean DEBUG = false;
    private final Graph<T> G;
    private final Predicate<T> filter;
    private final Iterator<T> roots;
    private @Nullable ArrayDeque<T> Q = null;
    private @Nullable HashMap<Object, T> history = null;

    // private Predicate<Statement> neighborFilter = (Statement s)->{
    // test()
    // System.out.println(s.getNode().getMethod().getDeclaringClass().getClassLoader());
    // return false;
    // };

    public FilteredBFSPathFinder(Graph G, Iterator nodes, Predicate f) {
        super(G, nodes, f);
        this.G = G;
        this.filter = f;
        this.roots = nodes; // srcs
    }

    @Override
    protected Iterator getConnected(Object n) {

        return super.getConnected(n);

    }

    @Override
    public @Nullable List find() {
        Object N;
        if (this.Q == null) {
            this.Q = new ArrayDeque();
            this.history = HashMapFactory.make();

            while (this.roots.hasNext()) {
                N = this.roots.next();
                this.Q.addLast((T) N);
                this.history.put(N, (T) null);
            }
        }

        while (!this.Q.isEmpty()) {
            N = this.Q.removeFirst();
            if (this.filter.test((T) N)) {
                return this.makePath((T) N, this.history);
            }

            Iterator<? extends T> children = this.getConnected(N);

            while (children.hasNext()) {
                T c = children.next();
                System.out.println(c);
                if (!this.history.containsKey(c)) {
                    this.Q.addLast(c);
                    this.history.put(c, (T) N);
                }
            }
        }

        return null;
    }

    private List<T> makePath(T node, @Nullable Map<Object, T> history) {
        ArrayList<T> result = new ArrayList();
        T n = node;
        result.add(node);

        while (true) {
            T parent = history.get(n);
            if (parent == null) {
                return result;
            }

            result.add(parent);
            n = parent;
        }
    }

    public List<List<T>> findAllPaths(Set<T> srcs, Set<T> dsts) {
        List<List<T>> allPaths = new ArrayList<>();

        if (srcs == null || dsts == null || srcs.isEmpty() || dsts.isEmpty()) {
            return allPaths;
        }

        // Forward search initialization
        Deque<T> forwardQueue = new ArrayDeque<>();
        Map<T, T> forwardHistory = new HashMap<>();
        for (T src : srcs) {
            forwardQueue.addLast(src);
            forwardHistory.put(src, null);
        }

        // Backward search initialization
        Deque<T> backwardQueue = new ArrayDeque<>();
        Map<T, T> backwardHistory = new HashMap<>();
        for (T dst : dsts) {
            backwardQueue.addLast(dst);
            backwardHistory.put(dst, null);
        }

        // Perform bidirectional BFS
        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            // Forward step
            T forwardNode = forwardQueue.removeFirst();
            if (backwardHistory.containsKey(forwardNode)) {
                allPaths.add(constructPath(forwardNode, forwardHistory, backwardHistory));
                // Continue to find other paths
            }
            // Boolean allNeighborPrimordial=false;
            for (Iterator it = getConnected(forwardNode); it.hasNext();) {
                T neighbor = (T) it.next();
                if (!forwardHistory.containsKey(neighbor) && !((Statement) neighbor).getNode().getMethod()
                        .getDeclaringClass().getClassLoader().toString().contains("Primordial")) {
                    forwardQueue.addLast(neighbor);
                    forwardHistory.put(neighbor, forwardNode);
                }
            }

            // Backward step
            T backwardNode = backwardQueue.removeFirst();
            if (forwardHistory.containsKey(backwardNode)) {
                allPaths.add(constructPath(backwardNode, forwardHistory, backwardHistory));
                // Continue to find other paths
            }
            for (Iterator it = getConnected(backwardNode); it.hasNext();) {
                T neighbor = (T) it.next();
                if (!backwardHistory.containsKey(neighbor) && !((Statement) neighbor).getNode().getMethod()
                        .getDeclaringClass().getClassLoader().toString().contains("Primordial")) {
                    backwardQueue.addLast(neighbor);
                    backwardHistory.put(neighbor, backwardNode);
                }
            }
        }

        return allPaths;
    }

    // public boolean checkIfSuccAllPri(Statement d){
    // boolean
    // for(Iterator it = getConnected(d);it.hasNext()){
    //
    // }
    // }

    public List<T> findPath(Set<T> srcs, Set<T> dsts) {
        if (srcs == null || dsts == null || srcs.isEmpty() || dsts.isEmpty()) {
            return null;
        }

        // Forward search initialization
        Deque<T> forwardQueue = new ArrayDeque<>();
        Map<T, T> forwardHistory = new HashMap<>();
        for (T src : srcs) {
            forwardQueue.addLast(src);
            forwardHistory.put(src, null);
        }

        // Backward search initialization
        Deque<T> backwardQueue = new ArrayDeque<>();
        Map<T, T> backwardHistory = new HashMap<>();
        for (T dst : dsts) {
            backwardQueue.addLast(dst);
            backwardHistory.put(dst, null);
        }

        // Perform bidirectional BFS
        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            // Forward step
            T forwardNode = forwardQueue.removeFirst();
            if (backwardHistory.containsKey(forwardNode)) {
                return constructPath(forwardNode, forwardHistory, backwardHistory);
            }
            for (Iterator it = getConnected(forwardNode); it.hasNext();) {
                T neighbor = (T) it.next();
                if (!forwardHistory.containsKey(neighbor)) {
                    System.out.println("---forward----");
                    System.out
                            .println(((Statement) neighbor).getNode().getMethod().getDeclaringClass().getClassLoader());
                    forwardQueue.addLast(neighbor);
                    forwardHistory.put(neighbor, forwardNode);
                }
            }

            // Backward step
            T backwardNode = backwardQueue.removeFirst();
            if (forwardHistory.containsKey(backwardNode)) {
                return constructPath(backwardNode, forwardHistory, backwardHistory);
            }
            for (Iterator it = getConnected(backwardNode); it.hasNext();) {
                T neighbor = (T) it.next();
                if (!backwardHistory.containsKey(neighbor)) { // && neighborFilter.test((Statement) neighbor)
                    System.out.println("---backward----");
                    System.out
                            .println(((Statement) neighbor).getNode().getMethod().getDeclaringClass().getClassLoader());
                    backwardQueue.addLast(neighbor);
                    backwardHistory.put(neighbor, backwardNode);
                }
            }
        }

        // If no path found
        return null;
    }

    public List<T> findPath(Set<T> srcs, T dst) {
        if (srcs == null || dst == null || srcs.isEmpty()) {
            return null;
        }

        // Forward search initialization
        Deque<T> forwardQueue = new ArrayDeque<>();
        Map<T, T> forwardHistory = new HashMap<>();
        for (T src : srcs) {
            forwardQueue.addLast(src);
            forwardHistory.put(src, null);
        }

        // Backward search initialization (starts from dst)
        Deque<T> backwardQueue = new ArrayDeque<>();
        Map<T, T> backwardHistory = new HashMap<>();
        backwardQueue.addLast(dst);
        backwardHistory.put(dst, null);

        // Perform bidirectional BFS
        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            // Forward step
            T forwardNode = forwardQueue.removeFirst();
            if (backwardHistory.containsKey(forwardNode)) {
                return constructPath(forwardNode, forwardHistory, backwardHistory); // Path found from src to dst
            }
            for (Iterator it = getConnected(forwardNode); it.hasNext();) {
                T neighbor = (T) it.next();
                if (!forwardHistory.containsKey(neighbor)) { // && !((Statement)
                                                             // neighbor).getNode().getMethod().getDeclaringClass().getClassLoader().toString().contains("Primordial")
                    // System.out.println("---forward----");
                    // System.out.println(((Statement)
                    // neighbor).getNode().getMethod().getDeclaringClass().getClassLoader());
                    forwardQueue.addLast(neighbor);
                    forwardHistory.put(neighbor, forwardNode);
                }
            }

            // Backward step
            T backwardNode = backwardQueue.removeFirst();
            if (forwardHistory.containsKey(backwardNode)) {
                return constructPath(backwardNode, forwardHistory, backwardHistory); // Path found from src to dst
            }
            for (Iterator it = getConnected(backwardNode); it.hasNext();) {
                T neighbor = (T) it.next();
                if (!backwardHistory.containsKey(neighbor)) { // && !((Statement)
                                                              // neighbor).getNode().getMethod().getDeclaringClass().getClassLoader().toString().contains("Primordial")
                    // System.out.println("---backward----");
                    // System.out.println();
                    backwardQueue.addLast(neighbor);
                    backwardHistory.put(neighbor, backwardNode);
                }
            }
        }

        // If no path found
        return null;
    }

    // private List<T> constructPath(T meetNode, Map<T, T> forwardHistory, Map<T, T>
    // backwardHistory) {
    // List<T> forwardPath = new ArrayList<>();
    // List<T> backwardPath = new ArrayList<>();
    //
    // // Build forward path from meetNode to src
    // T node = meetNode;
    // while (node != null) {
    // forwardPath.add(node);
    // node = forwardHistory.get(node);
    // }
    // Collections.reverse(forwardPath); // Reverse to get the correct order
    //
    // // Build backward path from meetNode to dst
    // node = backwardHistory.get(meetNode);
    // while (node != null) {
    // backwardPath.add(node);
    // node = backwardHistory.get(node);
    // }
    //
    // // Combine forward and backward paths
    // forwardPath.addAll(backwardPath);
    // return forwardPath;
    // }

    public Map<T, List<T>> findPathsToMultipleDsts(Set<T> srcs, Set<T> dsts) {
        Map<T, List<T>> paths = new HashMap<>();
        Set<T> visitedDsts = new HashSet<>();

        for (T src : srcs) {
            Deque<T> queue = new ArrayDeque<>();
            Map<T, T> history = new HashMap<>();
            Map<T, Integer> depth = new HashMap<>();
            queue.addLast(src);
            history.put(src, null);
            depth.put(src, 0);

            boolean pathFound = false;

            while (!queue.isEmpty()) {
                T currentNode = queue.removeFirst();
                int currentDepth = depth.get(currentNode);

                if (currentDepth > 30) {
                    break;
                }

                if (dsts.contains(currentNode) && !visitedDsts.contains(currentNode)) {
                    List<T> path = constructPath(currentNode, history);
                    paths.put(currentNode, path);
                    visitedDsts.add(currentNode);
                    pathFound = true;
                    break;
                }

                for (Iterator it = getConnected(currentNode); it.hasNext();) {
                    T neighbor = (T) it.next();
                    if (!history.containsKey(neighbor)) {
                        if (((Statement) neighbor).getNode().getMethod().getDeclaringClass().getClassLoader().toString()
                                .contains("Application")
                                && (((Statement) neighbor).toString().contains("NORMAL_RET_CALLER")
                                        || ((Statement) neighbor).toString().contains("METHOD_ENTRY"))) {
                            // 要在这里找
                            // (((Statement) neighbor).toString().contains("NORMAL_RET_CALLER")
                            // || ((Statement) neighbor).toString().contains("METHOD_ENTRY"))
                            if (!((Statement) neighbor).getNode().getMethod().toString()
                                    .contains(((Statement) src).getNode().getMethod().toString())) {
                                continue;
                            }
                        }
                        queue.addLast(neighbor);
                        history.put(neighbor, currentNode);
                        depth.put(neighbor, currentDepth + 1);
                    }
                }
            }

            if (pathFound) {
                continue;
            }

            if (visitedDsts.containsAll(dsts)) {
                break;
            }
        }

        return paths;
    }

    private List<T> constructPath(T meetNode, Map<T, T> forwardHistory, Map<T, T> backwardHistory) {
        List<T> forwardPath = new ArrayList<>();
        List<T> backwardPath = new ArrayList<>();

        // Build forward path from meetNode to src
        T node = meetNode;
        while (node != null) {
            forwardPath.add(node);
            node = forwardHistory.get(node);
        }
        Collections.reverse(forwardPath);

        // Build backward path from meetNode to dst
        node = backwardHistory.get(meetNode);
        while (node != null) {
            backwardPath.add(node);
            node = backwardHistory.get(node);
        }

        // Combine forward and backward paths
        forwardPath.addAll(backwardPath);
        return forwardPath;
    }

    private List<T> constructPath(T endNode, Map<T, T> history) {
        List<T> path = new ArrayList<>();
        T node = endNode;
        while (node != null) {
            path.add(node);
            node = history.get(node);
        }
        Collections.reverse(path);
        return path;
    }

}
