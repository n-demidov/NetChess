package edu.demidov.netchess.client.controllers.window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerTick implements ActionListener
{
    
    private final MainWindowController mainAppCntrl;
    private final static Logger log = LoggerFactory.getLogger(TimerTick.class);

    public TimerTick(final MainWindowController mainAppCntrl)
    {
        log.debug("TimerTick mainAppCntrl={}", mainAppCntrl);
        this.mainAppCntrl = mainAppCntrl;
    }

    @Override
    public void actionPerformed(final ActionEvent e) 
    {
        mainAppCntrl.everySecondTicks();
    }
        
}
