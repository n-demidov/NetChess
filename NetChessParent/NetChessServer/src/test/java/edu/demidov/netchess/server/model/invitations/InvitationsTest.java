package edu.demidov.netchess.server.model.invitations;

import edu.demidov.netchess.server.model.users.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class InvitationsTest
{
    private static final int STANDARD_INVITATIONS_FREQ_MANAGE_MINUTES = 1;
    private static final int STANDARD_INVITATIONS_TTL_MINUTES = 10;
    private static final int EXPIRED_INVITATIONS_TTL_MINUTES = -1;

    private Invitations invitations;
    private InvitationsObserver invitationsObserver;
    private User inviter;
    private User thinking;

    @Before
    public void before() throws Exception
    {
        createInvitations(STANDARD_INVITATIONS_TTL_MINUTES, STANDARD_INVITATIONS_FREQ_MANAGE_MINUTES);
    }

    @Test
    public void testAddListener() throws Exception
    {
        invitations.notifySubscribers(inviter, thinking);

        verify(invitationsObserver, times(1)).usersAgreed(inviter, thinking);
    }

    @Test
    public void testRemoveListener() throws Exception
    {
        invitations.removeListener(invitationsObserver);

        invitations.notifySubscribers(inviter, thinking);

        verify(invitationsObserver, never()).usersAgreed(inviter, thinking);
    }

    @Test
    public void testIsInvited() throws Exception
    {
        invitations.invite(inviter, thinking);

        assertTrue(invitations.isInvited(inviter, thinking));
    }

    @Test
    public void testGetIncomingInviters() throws Exception
    {
        final int INVITERS_COUNT = 3;
        invitations.invite(inviter, thinking);
        invitations.invite(mock(User.class), thinking);
        invitations.invite(mock(User.class), thinking);

        final Set<User> incomingInviters = invitations.getIncomingInviters(thinking);

        assertTrue(incomingInviters.size() == INVITERS_COUNT);
    }

    @Test
    public void testGetIncomingInviters_WhenEmpty() throws Exception
    {
        final Set<User> incomingInviters = invitations.getIncomingInviters(thinking);

        assertTrue(incomingInviters.isEmpty());
    }

    @Test
    public void testUsersAgreed_WhenBothInvites() throws Exception
    {
        invitations.invite(inviter, thinking);
        invitations.invite(thinking, inviter);

        verify(invitationsObserver, times(1)).usersAgreed(any(User.class), any(User.class));
        assertFalse(invitations.isInvited(inviter, thinking));
    }

    @Test
    public void testAcceptIncomingInvite() throws Exception
    {
        invitations.invite(inviter, thinking);
        invitations.acceptIncomingInvite(inviter, thinking);

        verify(invitationsObserver, times(1)).usersAgreed(any(User.class), any(User.class));
        assertFalse(invitations.isInvited(inviter, thinking));
    }

    @Test
    public void testAcceptIncomingInvite_WhenNoInvites() throws Exception
    {
        invitations.acceptIncomingInvite(inviter, thinking);

        verify(invitationsObserver, never()).usersAgreed(any(User.class), any(User.class));
    }

    @Test
    public void testAcceptIncomingInvite_WhenInviterInvitesThinkingWhichNo() throws Exception
    {
        invitations.acceptIncomingInvite(inviter, thinking);
        invitations.acceptIncomingInvite(thinking, inviter);

        verify(invitationsObserver, never()).usersAgreed(any(User.class), any(User.class));
    }

    @Test
    public void testCancelInvite() throws Exception
    {
        invitations.invite(inviter, thinking);
        invitations.cancelInvite(inviter, thinking);
        invitations.acceptIncomingInvite(inviter, thinking);

        verify(invitationsObserver, never()).usersAgreed(any(User.class), any(User.class));
    }

    @Test
    public void testRejectIncomingInvite() throws Exception
    {
        invitations.invite(inviter, thinking);
        invitations.rejectIncomingInvite(inviter, thinking);
        invitations.acceptIncomingInvite(inviter, thinking);

        verify(invitationsObserver, never()).usersAgreed(any(User.class), any(User.class));
    }

    @Test
    public void testCheckTtls_WhenInvitationsNotExpired() throws Exception
    {
        final int INVITERS_COUNT = 3;

        invitations.invite(inviter, thinking);
        invitations.invite(mock(User.class), thinking);
        invitations.invite(mock(User.class), thinking);

        Set<User> incomingInviters = invitations.getIncomingInviters(thinking);
        assertTrue(incomingInviters.size() == INVITERS_COUNT);

        invitations.checkTTLs();

        incomingInviters = invitations.getIncomingInviters(thinking);
        assertTrue(incomingInviters.size() == INVITERS_COUNT);
    }

    @Test
    public void testCheckTtls_WhenInvitationsExpired() throws Exception
    {
        final int INVITERS_COUNT = 3;

        createInvitations(
                STANDARD_INVITATIONS_FREQ_MANAGE_MINUTES,
                EXPIRED_INVITATIONS_TTL_MINUTES);

        invitations.invite(inviter, thinking);
        invitations.invite(mock(User.class), thinking);
        invitations.invite(mock(User.class), thinking);

        Set<User> incomingInviters = invitations.getIncomingInviters(thinking);
        assertTrue(incomingInviters.size() == INVITERS_COUNT);

        invitations.checkTTLs();

        incomingInviters = invitations.getIncomingInviters(thinking);
        assertTrue(incomingInviters.isEmpty());
    }

    private void createInvitations(final int standartInvitationsTtlMinutes, final int standartInvitationsFreqManageMinutes)
    {
        inviter = mock(User.class);
        thinking = mock(User.class);

        invitationsObserver = mock(InvitationsObserver.class);

        invitations = new Invitations(standartInvitationsTtlMinutes, standartInvitationsFreqManageMinutes);
        invitations.addListener(invitationsObserver);
    }
}