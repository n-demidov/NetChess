package edu.demidov.netchess.common.model.users;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AbstractUserTest {

    private static final Random random = new Random();
    private static final int INT_RANGE = Integer.MAX_VALUE;

    @Test
    public void UserProfile_8args() {
        final String inputName = "testName";
        final int inputRank = random.nextInt(INT_RANGE);
        final int inputWins = random.nextInt(INT_RANGE);
        final int inputDefeats = random.nextInt(INT_RANGE);
        final int inputDraws = random.nextInt(INT_RANGE);
        final int inputTotalTimeOnServer = random.nextInt(INT_RANGE);

        final AbstractUser profile = new AbstractUser(inputName, inputRank, inputWins, inputDefeats, inputDraws, inputTotalTimeOnServer) {
        };

        assertEquals(inputName, profile.getName());
        assertEquals(inputRank, profile.getRank());
        assertEquals(inputWins, profile.getWins());
        assertEquals(inputDefeats, profile.getDefeats());
        assertEquals(inputDraws, profile.getDraws());
        assertEquals(inputTotalTimeOnServer, profile.getTotalTimeOnServer());
    }

    @Test
    public void testSetName() {
        final String inputValue = "testName";

        final AbstractUserImpl user = new AbstractUserImpl();
        user.setName(inputValue);

        final String actualValue = user.getName();

        assertEquals(inputValue, actualValue);
    }

    @Test
    public void testSetRank() {
        final int inputValue = random.nextInt(INT_RANGE);

        final AbstractUserImpl user = new AbstractUserImpl();
        user.setRank(inputValue);

        final int actualValue = user.getRank();

        assertEquals(inputValue, actualValue);
    }

    @Test
    public void testSetWins() {
        final int inputValue = random.nextInt(INT_RANGE);

        final AbstractUserImpl user = new AbstractUserImpl();
        user.setWins(inputValue);

        final int actualValue = user.getWins();

        assertEquals(inputValue, actualValue);
    }

    @Test
    public void testSetDefeats() {
        final int inputValue = random.nextInt(INT_RANGE);

        final AbstractUserImpl user = new AbstractUserImpl();
        user.setDefeats(inputValue);

        final int actualValue = user.getDefeats();

        assertEquals(inputValue, actualValue);
    }

    @Test
    public void testSetDraws() {
        final int inputValue = random.nextInt(INT_RANGE);

        final AbstractUserImpl user = new AbstractUserImpl();
        user.setDraws(inputValue);

        final int actualValue = user.getDraws();

        assertEquals(inputValue, actualValue);
    }

    @Test
    public void testSetTotalTimeOnServer() {
        final int inputValue = random.nextInt(INT_RANGE);

        final AbstractUserImpl user = new AbstractUserImpl();
        user.setTotalTimeOnServer(inputValue);

        final int actualValue = user.getTotalTimeOnServer();

        assertEquals(inputValue, actualValue);
    }

    @Test
    public void testAddTotalTimeOnServer() {
        final AbstractUserImpl user = new AbstractUserImpl();

        for (int i = 0; i < 100; i++) {
            final int initTotalTimeOnServer = user.getTotalTimeOnServer();
            final int randomInt = random.nextInt(999);

            user.addTotalTimeOnServer(randomInt);

            final int actualValue = user.getTotalTimeOnServer();
            assertEquals(initTotalTimeOnServer + randomInt, actualValue);
        }
    }

    @Test
    public void testEqualsWhenNamesEquals() {
        final String sameName = "Bob";
        final String name1 = new String(sameName);
        final String name2 = new String(sameName);
        assertFalse(name1 == name2);
        assertEquals(name1, name2);

        final AbstractUserImpl user1 = new AbstractUserImpl();
        final AbstractUserImpl user2 = new AbstractUserImpl();

        user1.setName(name1);
        user2.setName(name2);

        user1.setRank(random.nextInt(INT_RANGE));
        user1.setRank(random.nextInt(INT_RANGE));
        user1.setWins(random.nextInt(INT_RANGE));
        user2.setWins(random.nextInt(INT_RANGE));
        user2.setDefeats(random.nextInt(INT_RANGE));
        user2.setDefeats(random.nextInt(INT_RANGE));
        user1.setDraws(random.nextInt(INT_RANGE));
        user2.setDraws(random.nextInt(INT_RANGE));
        user2.setTotalTimeOnServer(random.nextInt(INT_RANGE));
        user2.setTotalTimeOnServer(random.nextInt(INT_RANGE));

        assertEquals(user1, user2);
    }

    private static final class AbstractUserImpl extends AbstractUser {
    }

}
