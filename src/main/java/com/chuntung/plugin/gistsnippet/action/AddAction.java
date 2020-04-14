/*
 * Copyright (c) 2020 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;


/**
 * Open add dialog to create gist.
 */
public class AddAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO check selected file or path or text, pass them to dialog
    }
}
