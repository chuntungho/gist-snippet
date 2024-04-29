/*
 * Copyright (c) 2020 Chuntung Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.action;

import com.chuntung.plugin.gistsnippet.dto.FileNodeDTO;
import com.chuntung.plugin.gistsnippet.dto.SnippetNodeDTO;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Open the first gist in the selection in browser.
 */
public class OpenInBrowserAction extends DumbAwareAction {
    private JTree tree;

    public OpenInBrowserAction(JTree tree) {
        super("Open in browser");
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Note, this will use the first item in the selection.
        Object selected = tree.getLastSelectedPathComponent();
        Object userObject = ((DefaultMutableTreeNode) selected).getUserObject();
        String url = null;
        if (userObject instanceof SnippetNodeDTO) {
            url = ((SnippetNodeDTO) userObject).getHtmlUrl();
        } else if (userObject instanceof FileNodeDTO) {
            url = ((FileNodeDTO) userObject).getRawUrl();
        }
        if (url != null) {
            BrowserUtil.open(url);
        }
    }

    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
