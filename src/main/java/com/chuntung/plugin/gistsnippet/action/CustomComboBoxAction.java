/*
 * Copyright (c) 2020 Chuntung Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * ComboBox Action to be used in toolbar.
 */
public class CustomComboBoxAction extends ComboBoxAction implements DumbAware {
    private AnAction[] actions;
    private String myText;
    private Icon myIcon;

    public static CustomComboBoxAction create(AnAction... actions) {
        return new CustomComboBoxAction(actions);
    }

    public CustomComboBoxAction(AnAction... actions) {
        this.actions = actions;
        // set first action
        if (actions != null && actions.length > 0) {
            reset();
        }
    }

    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    public void update(@NotNull AnActionEvent e) {
        if (e.getPresentation() != null) {
            e.getPresentation().setText(myText);
            e.getPresentation().setIcon(myIcon);
        }
    }

    public String getText() {
        return myText;
    }

    public void reset() {
        Presentation first = actions[0].getTemplatePresentation();
        myText = first.getText();
        myIcon = first.getIcon();
    }

    @NotNull
    @Override
    protected DefaultActionGroup createPopupActionGroup(JComponent button) {
        DefaultActionGroup group = new DefaultActionGroup();
        for (AnAction action : actions) {
            group.add(new DelegatedAction(action));
        }
        return group;
    }

    private class DelegatedAction extends AnAction {
        private AnAction target;

        DelegatedAction(AnAction target) {
            this.target = target;
            getTemplatePresentation().setText(target.getTemplatePresentation().getText());
            getTemplatePresentation().setIcon(target.getTemplatePresentation().getIcon());
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            target.actionPerformed(e);
            // update combox text and icon
            myIcon = e.getPresentation().getIcon();
            myText = e.getPresentation().getText();
        }
    }
}
