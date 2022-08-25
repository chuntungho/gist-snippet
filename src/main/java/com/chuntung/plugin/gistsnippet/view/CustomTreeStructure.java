/*
 * Copyright (c) 2020 Chuntung Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.view;

import com.chuntung.plugin.gistsnippet.dto.SnippetNodeDTO;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import org.jetbrains.annotations.NotNull;

/**
 * Custom tree structure to support filtering.
 */
public class CustomTreeStructure extends SimpleTreeStructure {
    private Object root;

    CustomTreeStructure(Object root) {
        this.root = root;
    }

    @NotNull
    @Override
    public Object getRootElement() {
        return root;
    }

    //  use this to filter out invisible item
    public boolean isValid(@NotNull Object element) {
        if (element instanceof SnippetNodeDTO) {
            return ((SnippetNodeDTO) element).isVisible();
        }
        return true;
    }
}
