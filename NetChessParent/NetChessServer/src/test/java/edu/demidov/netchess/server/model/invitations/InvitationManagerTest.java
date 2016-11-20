package edu.demidov.netchess.server.model.invitations;

import edu.demidov.netchess.server.model.users.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class InvitationManagerTest
{
    private static final int STANDARD_INVITATIONS_FREQ_MANAGE_MINUTES = 1;
    private static final int STANDARD_INVITATIONS_TTL_MINUTES = 10;
    private static final int EXPIRED_INVITATIONS_TTL_MINUTES = -1;

    private InvitationManager invitationManager;
    private InvitationObserver invitationObserver;
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
        invitationManager.notifySubscribers(inviter, thinking);

        verify(invitationObserver, times(1)).usersAgreed(inviter, thinking);
    }

    @Test
    public void testRemoveListener() throws Exception
    {
        invitationManager.removeListener(invitationObserver);

        invitationManager.notifySubscribers(inviter, thinking);

        verify(invitationObserver, never()).usersAgreed(inviter, thinking);
    }

    @Test
    public void testIsInvited() throws Exception
    {
        invitationManager.invite(inviter, thinking);

        assertTrue(invitationManager.isInvited(inviter, thinking));
    }

    @Test
    public void testGetIncomingInviters() throws Exception
    {
        final int INVITERS_COUNT = 3;
        invitationManager.invite(inviter, thinking);
        invitationManager.invite(mock(User.class), thinking);
        invitationManager.invite(mock(User.class), thinking);

        final Set<User> incomingInviters = invitationManager.getIncomingInviters(thinking);

        assertTrue(incomingInviters.size() == INVITERS_COUNT);
    }

    @Test
    public void testGetIncomingInviters_WhenEmpty() throws Exception
    {
        final Set<User> incomingInviters = invitationManager.getIncomingInviters(thinking);

        assertTrue(incomingInviters.isEmpty());
    }

    @Test
    public void testUsersAgreed_WhenBothInvites() throws Exception
    {
        invitationManager.invite(inviter, thinking);
        invitationManager.invite(thinking, inviter);

        verify(invitationObserver, times(1)).usersAgreed(any(User.class), any(User.class));
        assertFalse(invitationManager.isInvited(inviter, thinking));
    }

    @Test
    public void testAcceptIncomingInvite() throws Exception
    {
        invitationManager.invite(inviter, thinking);
        invitationManager.acceptIncomingInvite(inviter, thinking);

        verify(invitationObserver, times(1)).usersAgreed(any(User.class), any(User.class));
        assertFalse(invitationManager.isInvited(inviter, thinking));
    }

    @Test
    public void testAcceptIncomingInvite_WhenNoInvites() throws Exception
    {
        invitationManager.acceptIncomingInvite(inviter, thinking);

        verify(invitationObserver, never()).usersAgreed(any(User.class), any(User.class));
    }

    @Test
    public void testAcceptIncomingInvite_WhenInviterInvitesThinkingWhichNo() throws Exception
    {
        invitationManager.acceptIncomingInvite(inviter, thinking);
        invitationManager.acceptIncomingInvite(thinking, inviter);

        verify(invitationObserver, never()).usersAgreed(any(User.class), any(User.class));
    }

    @Test
    public void testCancelInvite() throws Exception
    {
        invitationManager.invite(inviter, thinking);
        invitationManager.cancelInvite(inviter, thinking);
        invitationManager.acceptIncomingInvite(inviter, thinking);

        verify(invitationObserver, never()).usersAgreed(any(User.class), any(User.class));
    }

    @Test
    public void testRejectIncomingInvite() throws Exception
    {
        invitationManager.invite(inviter, thinking);
        invitationManager.rejectIncomingInvite(inviter, thinking);
        invitationManager.acceptIncomingInvite(inviter, thinking);

        verify(invitationObserver, never()).usersAgreed(any(User.class), any(User.class));
    }

    @Test
    public void testCheckTtls_WhenInvitationsNotExpired() throws Exception
    {
        final int INVITERS_COUNT = 3;

        invitationManager.invite(inviter, thinking);
        invitationManager.invite(mock(User.class), thinking);
        invitationManager.invite(mock(User.class), thinking);

        Set<User> incomingInviters = invitationManager.getIncomingInviters(thinking);
        assertTrue(incomingInviters.size() == INVITERS_COUNT);

        invitationManager.checkTTLs();

        incomingInviters = invitationManager.getIncomingInviters(thinking);
        assertTrue(incomingInviters.size() == INVITERS_COUNT);
    }

    @Test
    public void testCheckTtls_WhenInvitationsExpired() throws Exception
    {
        final int INVITERS_COUNT = 3;

        createInvitations(
                STANDARD_INVITATIONS_FREQ_MANAGE_MINUTES,
                EXPIRED_INVITATIONS_TTL_MINUTES);

        invitationManager.invite(inviter, thinking);
        invitationManager.invite(mock(User.class), thinking);
        invitationManager.invite(mock(User.class), thinking);

        Set<User> incomingInviters = invitationManager.getIncomingInviters(thinking);
        assertTrue(incomingInviters.size() == INVITERS_COUNT);

        invitationManager.checkTTLs();

        incomingInviters = invitationManager.getIncomingInviters(thinking);
        assertTrue(incomingInviters.isEmpty());
    }

    private void createInvitations(final int standartInvitationsTtlMinutes, final int standartInvitationsFreqManageMinutes)
    {
        inviter = mock(User.class);
        thinking = mock(User.class);

        invitationObserver = mock(InvitationObserver.class);

        invitationManager = new InvitationManager(standartInvitationsTtlMinutes, standartInvitationsFreqManageMinutes);
        invitationManager.addListener(invitationObserver);
    }
}