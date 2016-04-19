package edu.demidov.netchess.server.model.invitations;

import edu.demidov.netchess.server.model.users.User;

public interface InvitationObservable
{
    
    void addListener(InvitationObserver listener);
    
    void removeListener(InvitationObserver listener);
    
    void notifySubscribers(User source, User target);
    
}
