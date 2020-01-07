package com.chuntung.plugin.gistsnippet.service;

public class GistException extends RuntimeException {
    GistException(Exception e) {
        super(e);
    }
}
