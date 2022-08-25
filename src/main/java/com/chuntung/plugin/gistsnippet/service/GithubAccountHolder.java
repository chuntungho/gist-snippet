/*
 * Copyright (c) 2020 Chuntung Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.service;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;

import java.lang.reflect.Field;

public class GithubAccountHolder {
    private static final Logger logger = Logger.getInstance(GithubAccountHolder.class);

    private GithubAccount account;
    private final static Object NOT_FOUND = new Object();

    public static GithubAccountHolder getInstance(Project project) {
        return ServiceManager.getService(project, GithubAccountHolder.class);
    }

    public String getAccessToken() {
        String accountId = null;
        Object val = getProperty(account, "id");
        if (val == NOT_FOUND) {
            val = getProperty(account, "myId");
        }
        if (val != null && val != NOT_FOUND) {
            accountId = val.toString();
        }

        // org.jetbrains.plugins.github.authentication.accounts.GithubAccountManager.getTokenForAccount
        String token = PasswordSafe.getInstance().getPassword(new CredentialAttributes("IntelliJ Platform GitHub — " + accountId));
        if (token == null) {
            throw new GistException("Only token is supported to access GitHub API, please add account through token");
        }
        return token;
    }

    private Object getProperty(Object object, String property) {
        try {
            Field field = object.getClass().getDeclaredField(property);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception e) {
            return NOT_FOUND;
        }
    }

    public GithubAccount getAccount() {
        return account;
    }

    public void setAccount(GithubAccount account) {
        this.account = account;
    }
}
