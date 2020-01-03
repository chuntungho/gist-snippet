package com.chuntung.plugin.gistsnippet.service;

import com.chuntung.plugin.gistsnippet.dto.api.GistDTO;
import com.intellij.notification.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.plugins.github.api.GithubApiRequest;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutorManager;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;

import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GistSnippetService {
    public static final Logger logger = Logger.getInstance(GistSnippetService.class);

    public static final String MIME_TYPE = "application/vnd.github.v3+json";
    public static final String OWN_GISTS_URL = "https://api.github.com/gists";
    public static final String STARRED_GISTS_URL = "https://api.github.com/gists/starred";
    public static final String GIST_DETAIL_URL = "https://api.github.com/gists/%s";

    // Just cache in memory
    @XmlTransient
    private Map<String, List<GistDTO>> scopeCache = new ConcurrentHashMap<>();
    @XmlTransient
    private Map<String, GistDTO> gistCache = new ConcurrentHashMap<>();

    public static GistSnippetService getInstance() {
        return ServiceManager.getService(GistSnippetService.class);
    }

    // queryOwnGist
    public List<GistDTO> queryOwnGist(GithubAccount account, boolean forced) {
        String key = account.toString() + "#own";
        if (forced) {
            List<GistDTO> gistDTOS = scopeCache.computeIfPresent(key, (k, v) -> scopeCache.remove(k));
            if (gistDTOS != null) {
                for (GistDTO gistDTO : gistDTOS) {
                    gistCache.remove(gistDTO.getId());
                }
            }
        }

        return scopeCache.computeIfAbsent(key, (k) -> {
            try {
                GithubApiRequest.Get.JsonList request = new GithubApiRequest.Get.JsonList(OWN_GISTS_URL, GistDTO.class, MIME_TYPE);
                GithubApiRequestExecutor executor = GithubApiRequestExecutorManager.getInstance().getExecutor(account);
                List<GistDTO> result = (List<GistDTO>) executor.execute(request);
                return result;
            } catch (IOException e) {
                logger.info("Failed to query own gist, error: " + e.getMessage());
                notifyWarn("Failed to load own Gist, " + e.getMessage(), null);
                return null;
            }
        });
    }

    // queryStarredGist
    public List<GistDTO> queryStarredGist(GithubAccount account, boolean forced) {
        String key = account.toString() + "#starred";
        if (forced) {
            List<GistDTO> gistDTOS = scopeCache.computeIfPresent(key, (k, v) -> scopeCache.remove(k));
            if (gistDTOS != null) {
                for (GistDTO gistDTO : gistDTOS) {
                    gistCache.remove(gistDTO.getId());
                }
            }
        }

        return scopeCache.computeIfAbsent(key, (k) -> {
            try {
                GithubApiRequest.Get.JsonList<GistDTO> request = new GithubApiRequest.Get.JsonList<>(STARRED_GISTS_URL, GistDTO.class, MIME_TYPE);
                GithubApiRequestExecutor executor = GithubApiRequestExecutorManager.getInstance().getExecutor(account);
                List<GistDTO> result = (List<GistDTO>) executor.execute(request);
                return result;
            } catch (IOException e) {
                logger.info("Failed to query starred gist, error: " + e.getMessage());
                notifyWarn("Failed to load starred Gist, " + e.getMessage(), null);
                return null;
            }
        });
    }

    // queryPublicGist
    public List<GistDTO> getPublicGist(GithubAccount account) {
        // TODO
        return null;
    }

    public GistDTO getGistDetail(GithubAccount account, String gistId, boolean forced) {
        if (forced) {
            gistCache.computeIfPresent(gistId, (k, v) -> gistCache.remove(k));
        }

        return gistCache.computeIfAbsent(gistId, (k) -> {
            String url = String.format(GIST_DETAIL_URL, gistId);
            try {
                GithubApiRequest.Get.Json<GistDTO> request = new GithubApiRequest.Get.Json(url, GistDTO.class, MIME_TYPE);
                GithubApiRequestExecutor executor = GithubApiRequestExecutorManager.getInstance().getExecutor(account);
                return executor.execute(request);
            } catch (IOException e) {
                logger.info("Failed to get gist detail, error: " + e.getMessage());
                notifyWarn("Failed to get Gist files, " + e.getMessage(), null);
                return null;
            }
        });
    }

    // TODO catch exception at invoker side??
    public void notifyWarn(String warn, Project project) {
        NotificationGroup notificationGroup = new NotificationGroup("GistSnippet.NotificationGroup", NotificationDisplayType.BALLOON, true);
        Notification notification = notificationGroup.createNotification(warn, NotificationType.WARNING);
        Notifications.Bus.notify(notification, project);
    }
}
