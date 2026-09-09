package com.study.hub.exception;

public class ContractNotFoundException extends RuntimeException {

    public ContractNotFoundException() {
        super("Contract was not found");
    }
}
