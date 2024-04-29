package com.chuntung.plugin.gistsnippet.service;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.connector.GitHubConnector;
import org.kohsuke.github.connector.GitHubConnectorRequest;
import org.kohsuke.github.connector.GitHubConnectorResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class GithubHelper {
    private static final String fallbackEndpoint = "https://api-github-com.chuntung.com";

    private final ConcurrentMap<String, GitHub> clientCache = new ConcurrentHashMap<>();
    private final AtomicLong errorCount = new AtomicLong(0);

    public GithubHelper(){
    }

    public void logError() {
        errorCount.incrementAndGet();
        clientCache.clear();
    }

    public GitHub getClient(String token) throws IOException {
        GitHub gitHub = clientCache.computeIfAbsent(token, k -> {
            GitHubBuilder gitHubBuilder = new GitHubBuilder()
                    .withOAuthToken(token);
            if (errorCount.get() >= 3) {
                gitHubBuilder.withEndpoint(fallbackEndpoint)
                        .withConnector(new FallbackGitHubConnector(GitHubConnector.DEFAULT));
            }
            try {
                return gitHubBuilder.build();
            } catch (IOException e) {
                return null;
            }
        });
        if (gitHub == null) {
            throw new IOException("Failed to init github client");
        }
        return gitHub;
    }

    private static class FallbackGitHubConnector implements GitHubConnector {
        private GitHubConnector delegate;

        public FallbackGitHubConnector(GitHubConnector delegate) {
            this.delegate = delegate;
        }

        @Override
        public GitHubConnectorResponse send(GitHubConnectorRequest gitHubConnectorRequest) throws IOException {
            return new FallbackGitHubConnectorResponse(gitHubConnectorRequest, delegate.send(gitHubConnectorRequest));
        }
    }

    private static class FallbackGitHubConnectorResponse extends GitHubConnectorResponse {
        private final GitHubConnectorResponse response;

        protected FallbackGitHubConnectorResponse(GitHubConnectorRequest request, GitHubConnectorResponse response) {
            super(request, response.statusCode(), resolveLink(response.allHeaders()));
            this.response = response;
        }

        private static Map<String, List<String>> resolveLink(Map<String, List<String>> headers) {
            List<String> link = headers.get("Link");
            if (link != null) {
                List<String> resolved = link.stream()
                        .map(x -> x.replace("https://api.github.com", fallbackEndpoint))
                        .collect(Collectors.toList());
                if (!resolved.isEmpty()) {
                    HashMap<String, List<String>> newMap = new HashMap<String, List<String>>(headers);
                    newMap.put("Link", resolved);
                    return newMap;
                }
            }
            return headers;
        }

        @Override
        public InputStream bodyStream() throws IOException {
            return response.bodyStream();
        }

        @Override
        public void close() throws IOException {
            response.close();
        }
    }
}