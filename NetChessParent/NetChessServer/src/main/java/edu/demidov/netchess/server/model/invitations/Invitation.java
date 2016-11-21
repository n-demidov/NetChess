package edu.demidov.netchess.server.model.invitations;

import edu.demidov.netchess.server.model.users.User;

import java.util.Date;
import java.util.Objects;

public class Invitation {
    private final User sourceUser;
    private final Date invitedDate;

    public Invitation(final User sourceUser, final Date invitedDate) {
        assert sourceUser != null;
        assert invitedDate != null;

        this.sourceUser = sourceUser;
        this.invitedDate = invitedDate;
    }

    public User getSourceUser() {
        return sourceUser;
    }

    public Date getInvitedDate() {
        return invitedDate;
    }

    @Override
    public String toString() {
        return "Invitation{" + "sourceUser=" + sourceUser + ", invitedDate=" + invitedDate + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.sourceUser);
        hash = 61 * hash + Objects.hashCode(this.invitedDate);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final Invitation other = (Invitation) obj;
        if (!Objects.equals(this.sourceUser, other.sourceUser)) {
            return false;
        }
        if (!Objects.equals(this.invitedDate, other.invitedDate)) {
            return false;
        }
        return true;
    }
}
