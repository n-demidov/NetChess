package edu.demidov.netchess.client.controllers.window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TimerTick implements ActionListener {

    private final static Logger log = LoggerFactory.getLogger(TimerTick.class);
    private final MainWindowController mainAppCntrl;

    public TimerTick(final MainWindowController mainAppCntrl) {
        log.debug("TimerTick mainAppCntrl={}", mainAppCntrl);
        this.mainAppCntrl = mainAppCntrl;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        mainAppCntrl.everySecondTicks();
    }

}
