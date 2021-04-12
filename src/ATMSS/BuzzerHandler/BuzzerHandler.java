package ATMSS.BuzzerHandler;

import ATMSS.ATMSSStarter;
import ATMSS.HWHandler.HWHandler;
import AppKickstarter.misc.Msg;

public class BuzzerHandler extends HWHandler {
    private static boolean operate = true;

    public BuzzerHandler(String id, ATMSSStarter atmssStarter) {
        super(id, atmssStarter);
    }

    //------------------------------------------------------------
    // processMsg
    protected void processMsg(Msg msg) {
        if (operate) {
            switch (msg.getType()) {
                case Alert:
                    alert(msg.getDetails());
                    break;

                default:
                    log.warning(id + ": unknown message type: [" + msg + "]");
            }
        }
    } // processMsg

    protected boolean alert(String msg) {
        log.info(id + ": alert -- " + msg);
        return true;
    }

    protected void shutdown() {
        super.shutdown();
        operate = false;
    }

    protected void reset() {
        super.reset();
        operate = true;
    }
}
