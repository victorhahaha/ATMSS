package ATMSS.DispenserSlotHandler.Emulator;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.MBox;
import AppKickstarter.misc.Msg;

import java.util.logging.Logger;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


public class DispenserSlotEmulatorController {

    private String id;
    private AppKickstarter appKickstarter;
    private Logger log;
    private DispenserSlotEmulator DispenserSlotEmulator;
    private MBox DispenserSlotMBox;

    private boolean dispense = false;
    private static int denom100 = 10000;
    private static int denom500 = 10000;
    private static int denom1000 = 10000;

    public TextField amtField;
    public TextField DispenserSlotStatusField;
    public TextArea DispenserSlotTextArea;
    public Button withdrawBtn;

    //------------------------------------------------------------
    // initialize
    public void initialize(String id, AppKickstarter appKickstarter, Logger log, DispenserSlotEmulator DispenserSlotEmulator) {
        this.id = id;
        this.appKickstarter = appKickstarter;
        this.log = log;
        this.DispenserSlotEmulator = DispenserSlotEmulator;
        this.DispenserSlotMBox = appKickstarter.getThread("DispenserSlotHandler").getMBox();
    } // initialize

    public void buttonPressed(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();

        if ("Withdraw".equals(btn.getText())) {
            dispense(false);
        } else {
            log.warning(id + ": unknown button: [" + btn.getText() + "]");
        }
    } // buttonPressed

    //------------------------------------------------------------
    // updateCardStatus
    public void updateCardStatus(String status) {
        DispenserSlotStatusField.setText(status);
    } // updateCardStatus

    public void updateAmtField(String amt) {            //upon receive msg from atmss regarding denominations to dispense (update amtfield of dispenser slot)
        amtField.setText(amt);
    }

    //------------------------------------------------------------
    // appendTextArea
    public void appendTextArea(String status) {
        DispenserSlotTextArea.appendText(status + "\n");
    } // appendTextArea

    protected boolean getTransactionStatus() {
        return dispense;
    }

    protected void setTransactionStatus() {
        dispense = !dispense;
        withdrawBtn.setDisable(!dispense);
    }

    protected void updateDenomsInventory(String denoms, boolean increase) {
        String[] denomsTypes = denoms.split(" ");
        int amtSum = 0;
        if (increase) {
            //denomsInventory increase caused by cash deposit
            denom100 += Integer.parseInt(denomsTypes[0]);
            denom500 += Integer.parseInt(denomsTypes[1]);
            denom1000 += Integer.parseInt(denomsTypes[2]);
        } else {
            //denomsInventory decrease is always due to cash withdrawal
            for (int i = 0; i < denomsTypes.length; i++) {
                if (i == 0) {
                    amtSum = amtSum + Integer.parseInt(denomsTypes[i]) * 100;
                    appendTextArea(denomsTypes[i] + " $100 note(s) dispensed.");
                    denom100 -= Integer.parseInt(denomsTypes[i]);
                } else if (i == 1) {
                    amtSum = amtSum + Integer.parseInt(denomsTypes[i]) * 500;
                    appendTextArea(denomsTypes[i] + " $500 note(s) dispensed.");
                    denom500 -= Integer.parseInt(denomsTypes[i]);
                } else if (i == 2) {
                    amtSum = amtSum + Integer.parseInt(denomsTypes[i]) * 1000;
                    appendTextArea(denomsTypes[i] + " $1000 note(s) dispensed.");
                    denom1000 -= Integer.parseInt(denomsTypes[i]);
                }
            }
            appendTextArea("Cash Dispensed");
            updateAmtField("" + amtSum);
        }
        log.info(id + ": denoms inventory: $100: " + denom100 + ", $500: " + denom500 + ", $1000: " + denom1000);
    }

    protected void checkDenomsInventory() {
        DispenserSlotMBox.send(new Msg(id, DispenserSlotMBox, Msg.Type.DenomsInventoryCheck, "" + denom100 + " " + denom500 + " " + denom1000));
        if (denom100 <= 0) {
            DispenserSlotMBox.send(new Msg(id, DispenserSlotMBox, Msg.Type.Error, "run out of $100 money notes"));
        }
        if (denom500 <= 0) {
            DispenserSlotMBox.send(new Msg(id, DispenserSlotMBox, Msg.Type.Error, "run out of $500 money notes"));
        }
        if (denom1000 <= 0) {
            DispenserSlotMBox.send(new Msg(id, DispenserSlotMBox, Msg.Type.Error, "run out of $1000 money notes"));
        }
    }

    protected boolean dispense(boolean diagnostic) {
        if (dispense && (amtField.getText().length() != 0)) {
            DispenserSlotTextArea.appendText("Withdrawing " + amtField.getText() + "\n");
            if (!diagnostic) {
                DispenserSlotMBox.send(new Msg(id, DispenserSlotMBox, Msg.Type.DispenseFinish, amtField.getText()));        //when finish withdraw close the slot
            }
            amtField.setText("");
            return true;
        } else {
            DispenserSlotTextArea.appendText("Slot not open! Unable to withdraw!\n");
            return false;
        }
    }
}
