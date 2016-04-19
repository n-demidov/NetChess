package edu.demidov.netchess.common.model.network;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageQueue<T>
{

    private final BlockingQueue<T> bq;
    private static MessageQueue instance;
    
    public static synchronized <V> MessageQueue <V> getInstance()
    {
        if (instance == null) instance = new MessageQueue<>();
        return instance;
    }

    private MessageQueue()
    {
        bq = new LinkedBlockingQueue<>();
    }
    
    public void putMessage(final T message) throws InterruptedException
    {
        bq.put(message);
    }
    
    public T takeMessage() throws InterruptedException
    {
        return bq.take();
    }
    
    public boolean hasMessages()
    {
        return !bq.isEmpty();
    }
    
    public int size()
    {
        return bq.size();
    }

}
