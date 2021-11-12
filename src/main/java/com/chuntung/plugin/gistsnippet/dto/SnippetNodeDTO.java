/*
 * Copyright (c) 2020 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.dto;

import com.chuntung.plugin.gistsnippet.dto.api.GistDTO;
import com.chuntung.plugin.gistsnippet.dto.api.GistFileDTO;
import com.chuntung.plugin.gistsnippet.dto.api.GistOwnerDTO;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnippetNodeDTO extends SimpleNode {
    private static Map<String, Icon> avatarCache = new ConcurrentHashMap<>();
    private Icon publicIcon = IconLoader.getIcon("/images/public.png", SnippetNodeDTO.class);
    private Icon secretIcon = IconLoader.getIcon("/images/secret.png", SnippetNodeDTO.class);

    public static final Pattern TITLE_PATTERN = Pattern.compile("#(.+)#");
    public static final Pattern TAG_PATTERN = Pattern.compile("\\[([^\\[\\]]+)\\]");

    private ScopeEnum scope;
    private boolean visible = true;

    private String id;
    private String title;
    private String htmlUrl;
    private String description;
    private Integer filesCount;
    private List<GistFileDTO> files;
    private List<String> tags;
    private GistOwnerDTO owner;
    private boolean isPublic;
    private String createdAt;
    private String updatedAt;

    public ScopeEnum getScope() {
        return scope;
    }

    public void setScope(ScopeEnum scope) {
        this.scope = scope;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(Integer filesCount) {
        this.filesCount = filesCount;
    }

    public List<GistFileDTO> getFiles() {
        return files;
    }

    public void setFiles(List<GistFileDTO> files) {
        this.files = files;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public GistOwnerDTO getOwner() {
        return owner;
    }

    public void setOwner(GistOwnerDTO owner) {
        this.owner = owner;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @NotNull
    @Override
    public SimpleNode[] getChildren() {
        return this.files.toArray(NO_CHILDREN);
    }

    private Icon findAvatar(String url) {
        return avatarCache.computeIfAbsent(url, (k) -> {
            try {
                return new ImageIcon(new URL(k));
            } catch (MalformedURLException e) {
                return null;
            }
        });
    }

    @NotNull
    protected PresentationData createPresentation() {
        PresentationData presentation = new PresentationData();
        render(presentation);
        return presentation;
    }

    @Override
    protected void update(PresentationData presentation) {
        render(presentation);
    }

    private void render(PresentationData presentation) {
        if (ScopeEnum.OWN.equals(scope)) {
            presentation.setIcon(isPublic ? publicIcon : secretIcon);
        } else {
            // refer to github plugin to lazy load avatar icon
            presentation.setIcon(isPublic ? publicIcon : secretIcon);
        }

        // Text format: tags TITLE Description n files
        if (tags != null) {
            for (String tag : tags) {
                presentation.addText(tag, SimpleTextAttributes.LINK_BOLD_ATTRIBUTES);
                presentation.addText(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }

        if (title != null) {
            presentation.addText(title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            presentation.addText(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        presentation.addText(description, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        String cntTxt = " " + filesCount + " file" + (filesCount > 1 ? "s" : "");
        presentation.addText(cntTxt, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);

        // tooltip
        String activeAt = updatedAt != null ? updatedAt : createdAt;
        String tooltip = String.format("Last active at %s by %s", activeAt, owner.getLogin());
        presentation.setTooltip(tooltip);
    }

    static SnippetNodeDTO of(GistDTO dto, ScopeEnum scope) {
        SnippetNodeDTO node = new SnippetNodeDTO();
        node.setScope(scope);
        node.setId(dto.getId());
        node.setHtmlUrl(dto.getHtmlUrl());
        node.setCreatedAt(dto.getCreatedAt());
        node.setUpdatedAt(dto.getUpdatedAt());
        node.setPublic(dto.getPublic());
        node.setOwner(dto.getOwner());
        node.setFilesCount(dto.getFiles().size());
        node.setFiles(new ArrayList<>(dto.getFiles().values()));

        parseDescription(dto, node);

        return node;
    }

    private static void parseDescription(GistDTO dto, SnippetNodeDTO node) {
        node.setTitle(null);
        node.setTags(null);
        if (dto.getDescription() == null || dto.getDescription().isEmpty()) {
            // set description as first file name if empty
            for (GistFileDTO fileDTO : dto.getFiles().values()) {
                node.setDescription(fileDTO.getFilename());
                break;
            }
        } else {
            // resolve description
            String txt = dto.getDescription();

            Matcher titleMatcher = TITLE_PATTERN.matcher(txt);
            if (titleMatcher.find()) {
                node.setTitle(titleMatcher.group(1));
                txt = titleMatcher.replaceFirst("");
            }

            List<String> tags = new ArrayList<>();
            Matcher tagMatcher = TAG_PATTERN.matcher(txt);
            while (tagMatcher.find()) {
                tags.add(tagMatcher.group(1));
            }
            if (tags.size() > 0) {
                node.setTags(tags);
                txt = tagMatcher.replaceAll("");
            }

            node.setDescription(txt.trim());
        }
    }

    public boolean update(GistDTO dto) {
        boolean updated = false;
        if (!Objects.equals(createdAt, dto.getCreatedAt()) || !Objects.equals(updatedAt, dto.getUpdatedAt())) {
            parseDescription(dto, this);

            setPublic(dto.getPublic());
            setCreatedAt(dto.getCreatedAt());
            setUpdatedAt(dto.getUpdatedAt());

            updated = true;
        }

        // merge files
        setFilesCount(dto.getFiles().size());
        Set<String> children = new HashSet<>();
        // traverse tree structure to remove non-existing items
        Iterator<GistFileDTO> iterator = getFiles().iterator();
        while (iterator.hasNext()) {
            GistFileDTO fileDTO = iterator.next();
            if (dto.getFiles().containsKey(fileDTO.getFilename())) {
                fileDTO.setContent(dto.getFiles().get(fileDTO.getFilename()).getContent());
                children.add(fileDTO.getFilename());
            } else {
                updated = true;
                iterator.remove();
            }
        }

        // traverse latest files to add missing items if gist changed
        for (GistFileDTO fileDTO : dto.getFiles().values()) {
            if (!children.contains(fileDTO.getFilename())) {
                updated = true;
                getFiles().add(fileDTO);
            }
        }

        return updated;
    }
}
