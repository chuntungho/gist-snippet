/*
 * Copyright (c) 2020 Chuntung Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.action;

import com.chuntung.plugin.gistsnippet.dto.FileNodeDTO;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.function.Consumer;

/**
 * Reload selected gist.
 */
public class ReloadAction extends AnAction implements DumbAware {
    private JTree tree;
    private Consumer consumer;

    public ReloadAction(JTree tree, Consumer consumer) {
        super("Reload", "Reload first selected gist file", null);
        this.tree = tree;
        this.consumer = consumer;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        consumer.accept(e);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // check if gist file selected
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (!(node.getUserObject() instanceof FileNodeDTO)) {
            e.getPresentation().setVisible(false);
        }
    }
}
