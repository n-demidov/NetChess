package edu.demidov.netchess.server.model.invitations;

import edu.demidov.netchess.server.model.users.User;

public interface InvitationsObservable
{
    void addListener(InvitationsObserver listener);
    
    void removeListener(InvitationsObserver listener);
    
    void notifySubscribers(User source, User target);
}
