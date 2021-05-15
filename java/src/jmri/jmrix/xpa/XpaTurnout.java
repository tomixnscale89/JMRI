package jmri.jmrix.xpa;

import jmri.Turnout;
import jmri.implementation.AbstractTurnout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.concurrent.GuardedBy;

/**
 * Xpa+Modem implementation of the Turnout interface.
 * <p>
 * Based on XNetTurnout.java
 *
 * @author Paul Bender Copyright (C) 2004
 */
public class XpaTurnout extends AbstractTurnout {

    // Private data member to keep track of what turnout we control
    private final int _number;
    private final XpaTrafficController tc;

    @GuardedBy("this")
    protected int _mThrown = Turnout.THROWN;
    @GuardedBy("this")
    protected int _mClosed = Turnout.CLOSED;

    /**
     * Xpa turnouts use any address allowed as an accessory decoder address on
     * the particular command station.
     *
     * @param number turnout number
     * @param m      connection turnout is associated with
     */
    public XpaTurnout(int number, XpaSystemConnectionMemo m) {
        super(m.getSystemPrefix() + "T" + number);
        _number = number;
        tc = m.getXpaTrafficController();
    }

    public int getNumber() {
        return _number;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized protected void forwardCommandChangeToLayout(int newState) {
        XpaMessage m;
        // sort out states
        if ((newState & _mClosed) != 0) {
            // first look for the double case, which we can't handle
            if ((newState & _mThrown ) != 0) {
                // this is the disaster case!
                log.error("Cannot command both CLOSED and THROWN {}", newState);
                return;
            } else {
                // send a CLOSED command
                m = XpaMessage.getSwitchNormalMsg(_number);
            }
        } else {
            // send a THROWN command (or CLOSED if inverted)
            m = XpaMessage.getSwitchReverseMsg(_number);
        }
        tc.sendXpaMessage(m, null);
    }

    @Override
    protected void turnoutPushbuttonLockout(boolean _pushButtonLockout) {
        log.debug("Send command to {} Pushbutton PT{}", (_pushButtonLockout ? "Lock" : "Unlock"), _number);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized public void setInverted(boolean inverted) {
        log.debug("Inverting Turnout State for turnout {}", getSystemName() );
        if (inverted) {
            _mThrown = Turnout.CLOSED;
            _mClosed = Turnout.THROWN;
        } else {
            _mThrown = Turnout.THROWN;
            _mClosed = Turnout.CLOSED;
        }
        super.setInverted(inverted);
    }

    @Override
    public boolean canInvert() {
        return true;
    }

    private final static Logger log = LoggerFactory.getLogger(XpaTurnout.class);

}
