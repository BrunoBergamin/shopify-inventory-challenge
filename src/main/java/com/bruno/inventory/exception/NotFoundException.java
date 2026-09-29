package com.bruno.inventory.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String resource, Long id) {
        super(resource + " " + id + " not found");
    }
}
