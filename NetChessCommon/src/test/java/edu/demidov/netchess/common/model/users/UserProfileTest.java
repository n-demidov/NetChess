package edu.demidov.netchess.common.model.users;

import java.util.Random;
import static org.junit.Assert.*;
import org.junit.Test;

public class UserProfileTest
{

    @Test
    public void UserProfile_8args()
    {
        final Random random = new Random();
        final String inputName = "testName";
        final int inputRank = random.nextInt(999);
        final int inputWins = random.nextInt(999);
        final int inputDefeats = random.nextInt(999);
        final int inputDraws = random.nextInt(999);
        final int inputTotalTimeOnServer = random.nextInt(999);
        final boolean inputInvited = true;
        final boolean inputPlaying = true;
        
        final UserProfile profile = new UserProfile(inputName, inputRank, inputWins, inputDefeats, inputDraws, inputTotalTimeOnServer, inputInvited, inputPlaying);

        assertEquals(inputName, profile.getName());
        assertEquals(inputRank, profile.getRank());
        assertEquals(inputWins, profile.getWins());
        assertEquals(inputDefeats, profile.getDefeats());
        assertEquals(inputDraws, profile.getDraws());
        assertEquals(inputTotalTimeOnServer, profile.getTotalTimeOnServer());
        assertEquals(inputInvited, profile.isInvited());
        assertEquals(inputPlaying, profile.isPlaying());
    }

    @Test
    public void testSetInvited()
    {
        final boolean inputValue = true;
        
        final UserProfile profile = new UserProfile();
        profile.setInvited(inputValue);
        
        final boolean actualValue = profile.isInvited();

        assertEquals(inputValue, actualValue);
    }
    
    @Test
    public void testSetPlaying()
    {
        final boolean inputValue = true;
        
        final UserProfile profile = new UserProfile();
        profile.setPlaying(inputValue);
        
        final boolean actualValue = profile.isPlaying();

        assertEquals(inputValue, actualValue);
    }
    
}
