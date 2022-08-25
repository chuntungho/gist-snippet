/*
 * Copyright (c) 2020 Chuntung Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.dto;

import com.google.gson.annotations.SerializedName;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHGistFile;

public class FileNodeDTO extends SimpleNode {
    private String filename;
    private String type;
    private String language;

    @SerializedName("raw_url")
    private String rawUrl;

    private Long size;
    private Boolean truncated;
    private String content;

    public FileNodeDTO() {
    }

    public FileNodeDTO(GHGistFile gistFile) {
        this.setFilename(gistFile.getFileName());
        this.setContent(gistFile.getContent());
        this.setLanguage(gistFile.getLanguage());
        this.setRawUrl(gistFile.getRawUrl());
        this.setType(gistFile.getType());
        this.setSize((long) gistFile.getSize());
        this.setTruncated(gistFile.isTruncated());
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRawUrl() {
        return rawUrl;
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @NotNull
    @Override
    public SimpleNode[] getChildren() {
        return NO_CHILDREN;
    }

    @NotNull
    protected PresentationData createPresentation() {
        PresentationData presentation = new PresentationData();
        // file type icon
        FileType fileType = getFileType(this);
        setIcon(fileType.getIcon());

        String sizeTxt = " " + (size < 1024 ? (size + " B") : (size / 1024 + " KB"));
        presentation.addText(filename, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.addText(sizeTxt, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
        return presentation;
    }

    @NotNull
    public static FileType getFileType(FileNodeDTO dto) {
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        FileType fileType = fileTypeManager.getFileTypeByFileName(dto.getFilename());
        if (UnknownFileType.INSTANCE == fileType) {
            fileType = dto.getLanguage() == null ?
                    PlainTextFileType.INSTANCE : fileTypeManager.getStdFileType(dto.getLanguage());
        }
        return fileType;
    }
}
