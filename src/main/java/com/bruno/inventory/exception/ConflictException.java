package com.bruno.inventory.exception;

/** The request is valid but clashes with the current state of the data (e.g. a duplicate SKU). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
