package edu.demidov.netchess.server.model.exceptions;

import edu.demidov.netchess.common.model.exceptions.AbstractNetworkChessException;

public class LoginIsBanException extends AbstractNetworkChessException
{
    
    public LoginIsBanException(final String message)
    {
        super(message);
    }
    
}
