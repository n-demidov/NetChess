package edu.demidov.netchess.server.model.exceptions;

import edu.demidov.netchess.common.model.exceptions.AbstractNetworkChessException;

public class NoSuchUserInGameException extends AbstractNetworkChessException {

    public NoSuchUserInGameException(final String message) {
        super(message);
    }

}
