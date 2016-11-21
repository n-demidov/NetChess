package edu.demidov.netchess.common.model.network;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class NetworkMessageTest {

    @Test(expected = IllegalRequestParameter.class)
    public void testGetParam_3args_WhenNoSuchKey() throws Exception {
        final String key = "someKey";

        final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.LoginUser);

        netMsg.getParam(key, String.class, false);
    }

    @Test
    public void testGetParam_3args_WhenParameterIsRight() throws Exception {
        final String key = "someKey";
        final String inputValue = "someValue";

        final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.LoginUser);
        netMsg.put(key, inputValue);

        final String actualValue = netMsg.getParam(key, String.class, false);

        assertEquals(inputValue, actualValue);
    }

    @Test(expected = IllegalRequestParameter.class)
    public void testGetParam_3args_WhenParameterHasIllegalType() throws Exception {
        final String key = "someKey";
        final int inputValue = 12345;
        final Class<String> notInputValueClass = String.class;

        final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.LoginUser);
        netMsg.put(key, inputValue);

        netMsg.getParam(key, notInputValueClass, false);
    }

    @Test
    public void testGetParam_3args_WhenParameterIsNull() throws Exception {
        final String key = "someKey";
        final String inputValue = null;
        final Class<String> expectedStringClass = String.class;

        final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.LoginUser);
        netMsg.put(key, inputValue);

        try {
            netMsg.getParam(key, expectedStringClass, false);
            fail("the value is null; and forbidden to take null value");
        } catch (final IllegalRequestParameter ex) {
        }

        final String actualValue = netMsg.getParam(key, expectedStringClass, true);
        assertEquals("the value is null; and allowed to take null value", inputValue, actualValue);
    }

}
