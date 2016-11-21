package edu.demidov.netchess.common.model.exceptions;

public abstract class AbstractNetworkChessException extends Exception {

    public AbstractNetworkChessException() {
        super();
    }

    public AbstractNetworkChessException(final String message) {
        super(message);
    }

    public AbstractNetworkChessException(final Throwable cause) {
        super(cause);
    }

}
