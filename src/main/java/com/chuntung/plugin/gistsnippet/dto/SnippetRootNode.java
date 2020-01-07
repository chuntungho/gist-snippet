package com.chuntung.plugin.gistsnippet.dto;

import com.chuntung.plugin.gistsnippet.dto.api.GistDTO;
import com.intellij.ui.treeStructure.SimpleNode;
import org.jetbrains.annotations.NotNull;

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

    public void resetChildren(List<GistDTO> gistList, ScopeEnum scope) {
        List<SnippetNodeDTO> children = new ArrayList<>(gistList.size());
        for (GistDTO gistDTO : gistList) {
            children.add(SnippetNodeDTO.of(gistDTO, scope));
        }
        this.children = children;
    }

}
