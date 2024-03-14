package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeFilter.FilterResult;

/**
 A depth-first node traversor. Use to walk through all nodes under and including the specified root node, in document
 order. The {@link NodeVisitor#head(Node, int)} and {@link NodeVisitor#tail(Node, int)} methods will be called for
 each node.
 <p> During traversal, structural changes to nodes are supported (e.g. {{@link Node#replaceWith(Node)},
 {@link Node#remove()}}
 </p>

 一个深度优先的节点遍历器。用于按文档顺序遍历包括指定根节点在内的所有节点。

 每个节点都会调用 {@link NodeVisitor#head(Node, int)} 和 {@link NodeVisitor#tail(Node, int)} 方法。

 在遍历过程中，支持对节点进行结构性的更改（例如，{@link Node#replaceWith(Node)}）

 */
public class NodeTraversor {
    /**
     Run a depth-first traverse of the root and all of its descendants.
     @param visitor Node visitor.
     @param root the initial node point to traverse.
     @see NodeVisitor
     */
    public static void traverse(NodeVisitor visitor, Node root) {
        Validate.notNull(visitor);
        Validate.notNull(root);
        Node node = root;
        int depth = 0;
        
        while (node != null) {
            Node parent = node.parentNode(); // remember parent to find nodes that get replaced in .head
            int origSize = parent != null ? parent.childNodeSize() : 0;
            Node next = node.nextSibling();

            visitor.head(node, depth); // visit current node  调用 visitor 的 head 方法处理当前节点
            if (parent != null && !node.hasParent()) { // removed or replaced
                // 如果当前节点被移除或替换，会根据父节点的子节点数量是否发生变化来判断是被移除还是被替换。
                if (origSize == parent.childNodeSize()) { // replaced  如果被替换，会用新的节点替换当前节点；
                    node = parent.childNode(node.siblingIndex()); // replace ditches parent but keeps sibling index
                } else { // removed  如果被移除，会将 node 指向下一个兄弟节点，
                    node = next;
                    if (node == null) { // last one, go up  如果没有下一个兄弟节点，就向上回溯。
                        node = parent;
                        depth--;
                    }
                    continue; // don't tail removed   不要访问当前被移除节点的tail方法
                }
            }

            if (node.childNodeSize() > 0) { // descend 如果当前节点有子节点，会将 node 指向第一个子节点，并增加深度
                node = node.childNode(0);
                depth++;
            } else {
                // 如果当前节点没有子节点，会向上回溯，直到找到有兄弟节点的节点或回溯到根节点。在回溯的过程中，会调用 visitor 的 tail 方法处理节点，并减小深度。
                while (true) {
                    assert node != null; // as depth > 0, will have parent
                    if (!(node.nextSibling() == null && depth > 0)) break;
                    visitor.tail(node, depth); // when no more siblings, ascend
                    node = node.parentNode();
                    depth--;
                }
                visitor.tail(node, depth); // 调用 visitor 的 tail 方法处理当前节点
                if (node == root) // 如果 node 指向的是根节点，就会结束循环；
                    break;
                node = node.nextSibling(); // 否则，将 node 指向下一个兄弟节点，继续循环。
            }
        }
    }

    /**
     Run a depth-first traversal of each Element.
     @param visitor Node visitor.
     @param elements Elements to traverse.
     */
    public static void traverse(NodeVisitor visitor, Elements elements) {
        Validate.notNull(visitor);
        Validate.notNull(elements);
        for (Element el : elements)
            traverse(visitor, el);
    }

    /**
     Run a depth-first filtered traversal of the root and all of its descendants.
     @param filter NodeFilter visitor.
     @param root the root node point to traverse.
     @return The filter result of the root node, or {@link FilterResult#STOP}.

     @see NodeFilter
     */
    public static FilterResult filter(NodeFilter filter, Node root) {
        Node node = root;
        int depth = 0;

        while (node != null) {
            FilterResult result = filter.head(node, depth);
            if (result == FilterResult.STOP)
                return result;
            // Descend into child nodes:
            if (result == FilterResult.CONTINUE && node.childNodeSize() > 0) {
                node = node.childNode(0);
                ++depth;
                continue;
            }
            // No siblings, move upwards:
            while (true) {
                assert node != null; // depth > 0, so has parent
                if (!(node.nextSibling() == null && depth > 0)) break;
                // 'tail' current node:
                if (result == FilterResult.CONTINUE || result == FilterResult.SKIP_CHILDREN) {
                    result = filter.tail(node, depth);
                    if (result == FilterResult.STOP)
                        return result;
                }
                Node prev = node; // In case we need to remove it below.
                node = node.parentNode();
                depth--;
                if (result == FilterResult.REMOVE)
                    prev.remove(); // Remove AFTER finding parent.
                result = FilterResult.CONTINUE; // Parent was not pruned.
            }
            // 'tail' current node, then proceed with siblings:
            if (result == FilterResult.CONTINUE || result == FilterResult.SKIP_CHILDREN) {
                result = filter.tail(node, depth);
                if (result == FilterResult.STOP)
                    return result;
            }
            if (node == root)
                return result;
            Node prev = node; // In case we need to remove it below.
            node = node.nextSibling();
            if (result == FilterResult.REMOVE)
                prev.remove(); // Remove AFTER finding sibling.
        }
        // root == null?
        return FilterResult.CONTINUE;
    }

    /**
     Run a depth-first filtered traversal of each Element.
     @param filter NodeFilter visitor.
     @see NodeFilter
     */
    public static void filter(NodeFilter filter, Elements elements) {
        Validate.notNull(filter);
        Validate.notNull(elements);
        for (Element el : elements)
            if (filter(filter, el) == FilterResult.STOP)
                break;
    }
}
