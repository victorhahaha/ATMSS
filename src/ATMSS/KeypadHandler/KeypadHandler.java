package ATMSS.KeypadHandler;

import ATMSS.HWHandler.HWHandler;
import ATMSS.ATMSSStarter;
import AppKickstarter.misc.*;


//======================================================================
// KeypadHandler
public class KeypadHandler extends HWHandler {
    private static boolean operate = true;
    //------------------------------------------------------------
    // KeypadHandler
    public KeypadHandler(String id, ATMSSStarter atmssStarter) {
	super(id, atmssStarter);
    } // KeypadHandler


    //------------------------------------------------------------
    // processMsg
    protected void processMsg(Msg msg) {
        if (operate) {
            switch (msg.getType()) {
                case KP_KeyPressed:
                    atmss.send(new Msg(id, mbox, Msg.Type.KP_KeyPressed, msg.getDetails()));
                    break;

                case Alert:
                    alert();
                    break;

                default:
                    log.warning(id + ": unknown message type: [" + msg + "]");
            }
        }
    } // processMsg

    protected void alert() {
        log.info(id + ": alert user-- ");
    }

    protected void shutdown() {
        super.shutdown();
        operate = false;
    }

    protected void reset() {
        super.reset();
        operate = true;
    }
} // KeypadHandler
