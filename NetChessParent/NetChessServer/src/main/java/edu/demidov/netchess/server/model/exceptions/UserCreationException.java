package edu.demidov.netchess.server.model.exceptions;

import edu.demidov.netchess.common.model.exceptions.AbstractNetworkChessException;

public class UserCreationException extends AbstractNetworkChessException
{

    public UserCreationException(final String message)
    {
        super(message);
    }
    
}
