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

public class InviteToPlayHandler implements NetworkMessageHandler
{

    private final AccountManager accountManager = AccountManager.getInstance();
    private InvitationManager inviteManager;
    
    private static InviteToPlayHandler instance;
    private final static Logger log = LoggerFactory.getLogger(InviteToPlayHandler.class);

    public static synchronized InviteToPlayHandler getInstance()
    {
        if (instance == null)
        {
            instance = new InviteToPlayHandler();
        }
        return instance;
    }

    private InviteToPlayHandler() {}

    public void setInviteManager(final InvitationManager inviteManager)
    {
        this.inviteManager = inviteManager;
    }

    /**
     * Принимает запрос о том, что пользователь пригласил\отозвал приглашение
     * @param snm
     * @throws IllegalRequestParameter 
     */
    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter
    {
        log.trace("process snm={}", snm);
        try
        {
            final String targetUserName = snm.getNetMsg().getParam(NetworkMessage.INVITE_NAME, String.class);
            final String type = snm.getNetMsg().getParam(NetworkMessage.INVITE_TYPE, String.class);
            final boolean isAccept = type.equals(NetworkMessage.INVITE_TYPE_YES);
        
            final User targetUser = accountManager.getUser(targetUserName);
            final User sender = snm.getSender();
            
            if (isAccept)
            {
                inviteManager.invite(sender, targetUser);
            } else
            {
                inviteManager.cancelInvite(sender, targetUser);
            }
        } catch (final NoSuchUserException ex)
        {
            // В случае ненахождения такого пользователя ничего не делаем
            log.warn("process: NoSuchUserException snm={}", snm);
        }
    }

}
