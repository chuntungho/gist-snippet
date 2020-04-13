package com.chuntung.plugin.gistsnippet.service;

import com.chuntung.plugin.gistsnippet.dto.api.GistDTO;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.plugins.github.api.GithubApiRequest;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutorManager;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * API Doc: https://developer.github.com/v3/gists/
 */
public class GistSnippetService {
    public static final Logger logger = Logger.getInstance(GistSnippetService.class);

    public static final String MIME_TYPE = "application/vnd.github.v3+json";
    public static final String OWN_GISTS_URL = "https://api.github.com/gists";
    public static final String STARRED_GISTS_URL = "https://api.github.com/gists/starred";
    public static final String GIST_DETAIL_URL = "https://api.github.com/gists/%s";

    // cache in memory, can be collected
    private Map<String, List<String>> scopeCache = ContainerUtil.createConcurrentSoftValueMap();
    private Map<String, GistDTO> gistCache = ContainerUtil.createConcurrentSoftValueMap();

    public static GistSnippetService getInstance() {
        return ServiceManager.getService(GistSnippetService.class);
    }

    // queryOwnGist
    public List<GistDTO> queryOwnGist(GithubAccount account, boolean forced) {
        String key = account.toString() + "#own";
        if (forced) {
            List<String> gists = scopeCache.computeIfPresent(key, (k, v) -> scopeCache.remove(k));
            removeFromCache(gists);
        }

        AtomicReference<List<GistDTO>> result = new AtomicReference<>();
        List<String> idList = scopeCache.computeIfAbsent(key, (k) -> {
            try {
                GithubApiRequest.Get.JsonList request = new GithubApiRequest.Get.JsonList(OWN_GISTS_URL, GistDTO.class, MIME_TYPE);
                GithubApiRequestExecutor executor = GithubApiRequestExecutorManager.getInstance().getExecutor(account);
                List<GistDTO> gistList = (List<GistDTO>) executor.execute(request);
                result.set(gistList);
                return putIntoCache(gistList);
            } catch (IOException e) {
                logger.info("Failed to query own gists, error: " + e.getMessage());
                throw new GistException(e);
            }
        });

        return decideResult(account, result, idList);
    }

    private void removeFromCache(List<String> gists) {
        if (gists != null) {
            for (String gistId : gists) {
                gistCache.remove(gistId);
            }
        }
    }

    private List<String> putIntoCache(List<GistDTO> gistList) {
        if (gistList != null) {
            List<String> list = new ArrayList<>(gistList.size());
            for (GistDTO gistDTO : gistList) {
                list.add(gistDTO.getId());
                gistCache.putIfAbsent(gistDTO.getId(), gistDTO);
            }
            return list;
        }
        return null;
    }

    private List<GistDTO> decideResult(GithubAccount account, AtomicReference<List<GistDTO>> result, List<String> cacheList) {
        // load from cache
        if (result.get() == null && cacheList != null) {
            // N + 1
            List<GistDTO> gistList = new ArrayList<>(cacheList.size());
            for (String gistId : cacheList) {
                gistList.add(getGistDetail(account, gistId, false));
            }
            result.set(gistList);
        }

        return result.get();
    }

    // queryStarredGist
    public List<GistDTO> queryStarredGist(GithubAccount account, boolean forced) {
        String key = account.toString() + "#starred";
        if (forced) {
            List<String> gists = scopeCache.computeIfPresent(key, (k, v) -> scopeCache.remove(k));
            removeFromCache(gists);
        }

        AtomicReference<List<GistDTO>> result = new AtomicReference<>();
        List<String> cacheList = scopeCache.computeIfAbsent(key, (k) -> {
            try {
                GithubApiRequest.Get.JsonList<GistDTO> request = new GithubApiRequest.Get.JsonList<>(STARRED_GISTS_URL, GistDTO.class, MIME_TYPE);
                GithubApiRequestExecutor executor = GithubApiRequestExecutorManager.getInstance().getExecutor(account);
                List<GistDTO> gistList = (List<GistDTO>) executor.execute(request);
                result.set(gistList);
                return putIntoCache(gistList);
            } catch (IOException e) {
                logger.info("Failed to query starred gists, error: " + e.getMessage());
                throw new GistException(e);
            }
        });

        return decideResult(account, result, cacheList);
    }

    // queryPublicGist
    public List<GistDTO> getPublicGist(GithubAccount account) {
        // TODO
        return null;
    }

    /**
     * @param account
     * @param gistId
     * @param forced  true to load file content from remote server
     * @return
     */
    public GistDTO getGistDetail(GithubAccount account, String gistId, boolean forced) {
        if (forced) {
            gistCache.computeIfPresent(gistId, (k, v) -> gistCache.remove(k));
        }

        return gistCache.computeIfAbsent(gistId, (k) -> {
            String url = String.format(GIST_DETAIL_URL, gistId);
            GithubApiRequest.Get.Json<GistDTO> request = new GithubApiRequest.Get.Json(url, GistDTO.class, MIME_TYPE);
            try {
                GithubApiRequestExecutor executor = GithubApiRequestExecutorManager.getInstance().getExecutor(account);
                return executor.execute(request);
            } catch (IOException e) {
                logger.info("Failed to get gist detail, error: " + e.getMessage());
                throw new GistException(e);
            }
        });
    }

    public void deleteGist(GithubAccount account, List<String> gistIds) {
        try {
            GithubApiRequestExecutor executor = GithubApiRequestExecutorManager.getInstance().getExecutor(account);
            for (String gistId : gistIds) {
                String url = String.format(GIST_DETAIL_URL, gistId);
                // 2018.3
                GithubApiRequest.Delete delete = new GithubApiRequest.Delete(url);
                executor.execute(delete);
                gistCache.remove(gistId);
            }
        } catch (IOException e) {
            logger.info("Failed to delete gist, error: " + e.getMessage());
            throw new GistException(e);
        }
    }
}
