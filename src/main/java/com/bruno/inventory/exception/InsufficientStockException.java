package com.bruno.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String sku, int available, int requested) {
        super("Insufficient stock for " + sku + ": requested " + requested + ", available " + available);
    }
}
