/*
 * Copyright (c) 2020 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.action;

import com.chuntung.plugin.gistsnippet.dto.SnippetNodeDTO;
import com.chuntung.plugin.gistsnippet.dto.api.GistFileDTO;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
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
        } else if (userObject instanceof GistFileDTO) {
            url = ((GistFileDTO) userObject).getRawUrl();
        }
        if (url != null) {
            BrowserUtil.open(url);
        }
    }
}
