package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.model.exceptions.NoSuchUserException;
import edu.demidov.netchess.server.model.invitations.InvitationManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.users.AccountManager;
import edu.demidov.netchess.server.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InviteToPlayResponseHandler implements NetworkMessageHandler
{

    private final AccountManager accountManager = AccountManager.getInstance();
    private InvitationManager inviteManager;
    
    private static InviteToPlayResponseHandler instance;
    private final static Logger log = LoggerFactory.getLogger(InviteToPlayResponseHandler.class);

    public static synchronized InviteToPlayResponseHandler getInstance()
    {
        if (instance == null)
        {
            instance = new InviteToPlayResponseHandler();
        }
        return instance;
    }

    private InviteToPlayResponseHandler() {}

    public void setInviteManager(final InvitationManager inviteManager)
    {
        this.inviteManager = inviteManager;
    }

    public static void setInstance(final InviteToPlayResponseHandler instance)
    {
        InviteToPlayResponseHandler.instance = instance;
    }

    /**
     * Принимает запрос о том, что пользователь принял\отклонил входящее приглашение
     * @param snm
     * @throws IllegalRequestParameter 
     */
    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter
    {
        log.trace("process snm={}", snm);
        try
        {
            final String offerrerUserName = snm.getNetMsg().getParam(NetworkMessage.INVITE_NAME, String.class);
            final String type = snm.getNetMsg().getParam(NetworkMessage.INVITE_TYPE, String.class);
            final boolean isAccept = type.equals(NetworkMessage.INVITE_TYPE_YES);
        
            final User offerrerUser = accountManager.getUser(offerrerUserName);
            final User sender = snm.getSender();
            
            if (isAccept)
            {
                inviteManager.acceptIncomingInvite(offerrerUser, sender);
            } else
            {
                inviteManager.rejectIncomingInvite(offerrerUser, sender);
            }
        } catch (final NoSuchUserException ex)
        {
            // В случае ненахождения такого пользователя ничего не делаем
            log.warn("process: NoSuchUserException snm={}", snm);
        }
    }

}
