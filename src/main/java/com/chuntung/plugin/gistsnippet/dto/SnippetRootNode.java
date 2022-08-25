/*
 * Copyright (c) 2020 Chuntung Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.dto;

import com.intellij.ui.treeStructure.SimpleNode;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHGist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SnippetRootNode extends SimpleNode {
    private List<SnippetNodeDTO> children = Collections.EMPTY_LIST;

    public SnippetRootNode() {
    }

    public List<SnippetNodeDTO> children() {
        return children;
    }

    @NotNull
    @Override
    public SimpleNode[] getChildren() {
        return children == null ? NO_CHILDREN : children.toArray(NO_CHILDREN);
    }

    public void resetChildren(List<GHGist> gistList, ScopeEnum scope) {
        List<SnippetNodeDTO> children = new ArrayList<>(gistList.size());
        for (GHGist gistDTO : gistList) {
            children.add(SnippetNodeDTO.of(gistDTO, scope));
        }
        this.children = children;
    }

}
