/*
 * Copyright (c) 2020 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.action;

import com.chuntung.plugin.gistsnippet.dto.ScopeEnum;
import com.chuntung.plugin.gistsnippet.dto.SnippetNodeDTO;
import com.chuntung.plugin.gistsnippet.dto.SnippetRootNode;
import com.chuntung.plugin.gistsnippet.service.GistSnippetService;
import com.chuntung.plugin.gistsnippet.service.GithubAccountHolder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.ui.tree.StructureTreeModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Delete selected own gists.
 */
public class DeleteAction extends AnAction implements DumbAware {
    private JTree tree;
    private StructureTreeModel structure;
    private SnippetRootNode root;
    private Project project;

    public DeleteAction(JTree tree, StructureTreeModel structure, SnippetRootNode root, Project project) {
        super("Delete", "Delete selected gists", null);
        this.tree = tree;
        this.structure = structure;
        this.root = root;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<String> gistIds = new ArrayList<>();
        Set<SnippetNodeDTO> gists = new HashSet<>();
        StringBuilder msgs = new StringBuilder();
        TreePath[] paths = tree.getSelectionPaths();
        for (TreePath path : paths) {
            Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (userObject instanceof SnippetNodeDTO) {
                SnippetNodeDTO node = (SnippetNodeDTO) userObject;
                if (node.getScope().equals(ScopeEnum.OWN)) {
                    gists.add(node);
                    gistIds.add(node.getId());
                    msgs.append('\n').append(node.getDescription() == null ? node.getId() : node.getDescription());
                }
            }
        }

        boolean yes = MessageDialogBuilder.yesNo("Delete", "Delete selected gists?" + msgs).isYes();
        if (yes) {
            new Task.Backgroundable(project, "Deleting gists...") {
                private boolean deleted = false;

                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    GithubAccount account = GithubAccountHolder.getInstance(project).getAccount();
                    GistSnippetService service = GistSnippetService.getInstance();
                    service.deleteGist(account, gistIds);
                    deleted = true;
                }

                @Override
                public void onSuccess() {
                    // refresh tree
                    if (deleted) {
                        root.children().removeAll(gists);
                        structure.invalidate();
                    }
                }
            }.queue();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                if (userObject instanceof SnippetNodeDTO) {
                    SnippetNodeDTO node = (SnippetNodeDTO) userObject;
                    if (node.getScope().equals(ScopeEnum.OWN)) {
                        return;
                    }
                }
            }
        }

        event.getPresentation().setVisible(false);
    }
}
