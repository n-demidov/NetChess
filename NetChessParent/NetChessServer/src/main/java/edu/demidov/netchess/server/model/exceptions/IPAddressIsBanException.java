package edu.demidov.netchess.server.model.exceptions;

import edu.demidov.netchess.common.model.exceptions.AbstractNetworkChessException;

public class IPAddressIsBanException extends AbstractNetworkChessException
{
    
    public IPAddressIsBanException(final String message)
    {
        super(message);
    }
    
}
