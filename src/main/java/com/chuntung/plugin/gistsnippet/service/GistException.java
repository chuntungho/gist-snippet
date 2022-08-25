/*
 * Copyright (c) 2020 Chuntung Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.service;

public class GistException extends RuntimeException {
    GistException(Exception e) {
        super(e);
    }

    public GistException(String message) {
        super(message);
    }
}
