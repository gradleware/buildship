/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Simon Scholz (vogella GmbH) - initial API and implementation and initial documentation
 */

package org.eclipse.buildship.ui.util.viewer;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Contains helper methods for {@link Viewer} objects.
 */
public final class ViewerUtils {

    private ViewerUtils() {
    }

    /**
     * Makes the header of a {@link TreeViewer} or a {@link TableViewer} visible or not.
     *
     * @param viewer {@link Viewer}
     * @param showHeader <code>true</code>, if header should be shown and otherwise
     *            <code>false</code>
     */
    public static void setHeaderVisible(Viewer viewer, boolean showHeader) {
        if (viewer instanceof TreeViewer) {
            Tree tree = ((TreeViewer) viewer).getTree();
            tree.setHeaderVisible(showHeader);
        } else if (viewer instanceof TableViewer) {
            Table table = ((TableViewer) viewer).getTable();
            table.setHeaderVisible(showHeader);
        }
    }

    public static void collapseTreeItem(TreeItem[] treeItems, Object element) {
        for (TreeItem treeItem : treeItems) {
            Object data = treeItem.getData();
            if (element.equals(data)) {
                expandOrCollapseChildItems(treeItem, false);
            }
        }
    }

    public static void expandTreeItem(TreeItem[] treeItems, Object element) {
        for (TreeItem treeItem : treeItems) {
            Object data = treeItem.getData();
            if (element.equals(data)) {
                expandOrCollapseChildItems(treeItem, true);
            }
        }
    }

    private static void expandOrCollapseChildItems(TreeItem parentTreeItem, boolean expand) {
        parentTreeItem.setExpanded(expand);
        TreeItem[] items = parentTreeItem.getItems();
        for (TreeItem treeItem : items) {
            expandOrCollapseChildItems(treeItem, expand);
        }
    }
}
