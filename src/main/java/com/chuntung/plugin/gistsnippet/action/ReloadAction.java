/*
 * Copyright (c) 2020 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.action;

import com.chuntung.plugin.gistsnippet.dto.api.GistFileDTO;
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
        if (!(node.getUserObject() instanceof GistFileDTO)) {
            e.getPresentation().setVisible(false);
        }
    }
}
