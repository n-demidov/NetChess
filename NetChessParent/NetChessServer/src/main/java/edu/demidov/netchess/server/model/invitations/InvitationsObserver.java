package edu.demidov.netchess.server.model.invitations;

import edu.demidov.netchess.server.model.users.User;

public interface InvitationsObserver {
    void usersAgreed(User source, User target);
}
