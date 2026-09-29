package com.bruno.inventory.exception;

public class InvalidShipmentStateException extends ConflictException {
    public InvalidShipmentStateException(String message) {
        super(message);
    }
}
