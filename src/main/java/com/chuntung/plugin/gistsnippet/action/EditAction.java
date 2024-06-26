/*
 * Copyright (c) 2020 Chuntung Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Edit the first gist in the selection.
 */
public class EditAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

    }

    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
