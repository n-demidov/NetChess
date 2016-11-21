package edu.demidov.netchess.server.model.exceptions;

import edu.demidov.netchess.common.model.exceptions.AbstractNetworkChessException;

public class AccessConnectedUserException extends AbstractNetworkChessException {

    public AccessConnectedUserException(final String message) {
        super(message);
    }

}
