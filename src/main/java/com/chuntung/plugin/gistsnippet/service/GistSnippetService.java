/*
 * Copyright (c) 2020 Chuntung Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtil;
import org.kohsuke.github.GHGist;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * API Doc: https://developer.github.com/v3/gists/
 */
public class GistSnippetService {
    private static final Logger logger = Logger.getInstance(GistSnippetService.class);

    // cache in memory, can be collected
    private Map<String, List<String>> scopeCache = ContainerUtil.createConcurrentSoftValueMap();
    private Map<String, GHGist> gistCache = ContainerUtil.createConcurrentSoftValueMap();

    public static GistSnippetService getInstance() {
        return ServiceManager.getService(GistSnippetService.class);
    }

    // queryOwnGist
    public List<GHGist> queryOwnGist(String token, boolean forced) {
        String key = token + "#own";
        if (forced) {
            List<String> gists = scopeCache.computeIfPresent(key, (k, v) -> scopeCache.remove(k));
            removeFromCache(gists);
        }

        AtomicReference<List<GHGist>> result = new AtomicReference<>();
        List<String> idList = scopeCache.computeIfAbsent(key, (k) -> {
            try {
                GitHub github = new GitHubBuilder().withOAuthToken(token).build();
                PagedIterable<GHGist> pagedResult = github.listGists();
                List<GHGist> ghGists = pagedResult.toList();
                result.set(ghGists);
                return putIntoCache(ghGists);
            } catch (IOException e) {
                logger.info("Failed to query own gists, error: " + e.getMessage());
                throw new GistException(e);
            }
        });

        return decideResult(token, result, idList);
    }

    private void removeFromCache(List<String> gists) {
        if (gists != null) {
            for (String gistId : gists) {
                gistCache.remove(gistId);
            }
        }
    }

    private List<String> putIntoCache(List<GHGist> gistList) {
        if (gistList != null) {
            List<String> list = new ArrayList<>(gistList.size());
            for (GHGist gistDTO : gistList) {
                list.add(gistDTO.getGistId());
                gistCache.putIfAbsent(gistDTO.getGistId(), gistDTO);
            }
            return list;
        }
        return null;
    }

    private List<GHGist> decideResult(String token, AtomicReference<List<GHGist>> result, List<String> cacheList) {
        // load from cache
        if (result.get() == null && cacheList != null) {
            // N + 1
            List<GHGist> gistList = new ArrayList<>(cacheList.size());
            for (String gistId : cacheList) {
                gistList.add(getGistDetail(token, gistId, false));
            }
            result.set(gistList);
        }

        return result.get();
    }

    // queryStarredGist
    public List<GHGist> queryStarredGist(String token, boolean forced) {
        String key = token + "#starred";
        if (forced) {
            List<String> gists = scopeCache.computeIfPresent(key, (k, v) -> scopeCache.remove(k));
            removeFromCache(gists);
        }

        AtomicReference<List<GHGist>> result = new AtomicReference<>();
        List<String> cacheList = scopeCache.computeIfAbsent(key, (k) -> {
            try {
                // TODO starred gists
                GitHub github = new GitHubBuilder().withOAuthToken(token).build();
                PagedIterable<GHGist> pagedResult = github.listStarredGists();
                List<GHGist> gistList = pagedResult.toList();
                result.set(gistList);
                return putIntoCache(gistList);
            } catch (IOException e) {
                logger.info("Failed to query starred gists, error: " + e.getMessage());
                throw new GistException(e);
            }
        });

        return decideResult(token, result, cacheList);
    }

    // queryPublicGist
    public List<GHGist> queryPublicGists(String keyword) {
        // TODO
        return null;
    }

    /**
     * @param token
     * @param gistId
     * @param forced    true to load file content from remote server
     * @return
     */
    public GHGist getGistDetail(String token, String gistId, boolean forced) {
        if (forced) {
            gistCache.computeIfPresent(gistId, (k, v) -> gistCache.remove(k));
        }

        return gistCache.computeIfAbsent(gistId, (k) -> {
            try {
                GitHub github = new GitHubBuilder().withOAuthToken(token).build();
                return github.getGist(gistId);
            } catch (IOException e) {
                logger.info("Failed to get gist detail, error: " + e.getMessage());
                throw new GistException(e);
            }
        });
    }

    public void deleteGist(String token, List<String> gistIds) {
        try {
            for (String gistId : gistIds) {
                GitHub github = new GitHubBuilder().withOAuthToken(token).build();
                github.deleteGist(gistId);
                gistCache.remove(gistId);
            }
            String key = token + "#own";
            List<String> cacheList = scopeCache.get(key);
            if (cacheList != null) {
                cacheList.removeAll(gistIds);
            }
        } catch (IOException e) {
            logger.info("Failed to delete gist, error: " + e.getMessage());
            throw new GistException(e);
        }
    }
}
