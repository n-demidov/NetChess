package edu.demidov.netchess.server.model.exceptions;

import edu.demidov.netchess.common.model.exceptions.AbstractNetworkChessException;

public class UserLoginException extends AbstractNetworkChessException
{
    
    public UserLoginException(final String message)
    {
        super(message);
    }
    
}
