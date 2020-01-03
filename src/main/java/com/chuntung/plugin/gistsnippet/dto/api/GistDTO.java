package com.chuntung.plugin.gistsnippet.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class GistDTO {
    private String id;
    private String url;

    @SerializedName("forks_url")
    private String forksUrl;

    @SerializedName("commits_url")
    private String commitsUrl;

    @SerializedName("node_id")
    private String nodeId;

    @SerializedName("git_pull_url")
    private String gitPullUrl;

    @SerializedName("git_push_url")
    private String gitPushUrl;

    @SerializedName("html_url")
    private String htmlUrl;

    private Map<String, GistFileDTO> files;

    // IC 2019.1 use gson to parse JSON
    @SerializedName("public")
    // IC 2019.3 use jackson to parse JSON
    @JsonProperty("public")
    private Boolean isPublic;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    private String description;

    private Integer comments;

    private String user;

    @SerializedName("comments_url")
    private String commentsUrl;

    private Boolean truncated;

    private GistOwnerDTO owner;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getForksUrl() {
        return forksUrl;
    }

    public void setForksUrl(String forksUrl) {
        this.forksUrl = forksUrl;
    }

    public String getCommitsUrl() {
        return commitsUrl;
    }

    public void setCommitsUrl(String commitsUrl) {
        this.commitsUrl = commitsUrl;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getGitPullUrl() {
        return gitPullUrl;
    }

    public void setGitPullUrl(String gitPullUrl) {
        this.gitPullUrl = gitPullUrl;
    }

    public String getGitPushUrl() {
        return gitPushUrl;
    }

    public void setGitPushUrl(String gitPushUrl) {
        this.gitPushUrl = gitPushUrl;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public Map<String, GistFileDTO> getFiles() {
        return files;
    }

    public void setFiles(Map<String, GistFileDTO> files) {
        this.files = files;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getComments() {
        return comments;
    }

    public void setComments(Integer comments) {
        this.comments = comments;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getCommentsUrl() {
        return commentsUrl;
    }

    public void setCommentsUrl(String commentsUrl) {
        this.commentsUrl = commentsUrl;
    }

    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    public GistOwnerDTO getOwner() {
        return owner;
    }

    public void setOwner(GistOwnerDTO owner) {
        this.owner = owner;
    }
}
