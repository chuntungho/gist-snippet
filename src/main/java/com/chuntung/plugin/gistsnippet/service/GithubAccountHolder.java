/*
 * Copyright (c) 2020 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.service;


import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;

public class GithubAccountHolder {
    private GithubAccount account;

    public static GithubAccountHolder getInstance(Project project) {
        return ServiceManager.getService(project, GithubAccountHolder.class);
    }

    public GithubAccount getAccount() {
        return account;
    }

    public void setAccount(GithubAccount account) {
        this.account = account;
    }
}
