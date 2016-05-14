package edu.demidov.netchess.server.model.invitations;

import edu.demidov.netchess.server.model.users.User;

public interface InvitationObserver
{
    
    void usersAgreed(User source, User target);
    
}
