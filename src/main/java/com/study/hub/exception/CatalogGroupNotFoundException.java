package com.study.hub.exception;

public class CatalogGroupNotFoundException extends RuntimeException {

    public CatalogGroupNotFoundException(String groupId) {
        super("Catalog group '%s' was not found".formatted(groupId));
    }
}
