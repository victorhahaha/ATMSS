package ATMSS.DepositSlotHandler.Emulator;

import ATMSS.ATMSSStarter;
import ATMSS.DepositSlotHandler.DepositSlotHandler;

import AppKickstarter.misc.Msg;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class DepositSlotEmulator extends DepositSlotHandler{
    private ATMSSStarter atmssStarter;
    private String id;
    private Stage myStage;
    private DepositSlotEmulatorController DepositSlotEmulatorController;

    public DepositSlotEmulator(String id, ATMSSStarter atmssStarter) {
        super(id, atmssStarter);
        this.atmssStarter = atmssStarter;
        this.id = id;
    } // CardReaderEmulator

    //------------------------------------------------------------
    // start
    public void start() throws Exception {
        Parent root;
        myStage = new Stage();
        FXMLLoader loader = new FXMLLoader();
        String fxmlName = "DepositSlotEmulator.fxml";
        loader.setLocation(DepositSlotEmulator.class.getResource(fxmlName));
        root = loader.load();       //error?
        DepositSlotEmulatorController = (DepositSlotEmulatorController) loader.getController();
        DepositSlotEmulatorController.initialize(id, atmssStarter, log, this);
        myStage.initStyle(StageStyle.DECORATED);
        myStage.setScene(new Scene(root, 350, 470));
        myStage.setTitle("Deposit Slot");
        myStage.setResizable(false);
        myStage.setOnCloseRequest((WindowEvent event) -> {
            atmssStarter.stopApp();
            Platform.exit();
        });
        myStage.show();
    } // CardReaderEmulator


    //------------------------------------------------------------
    // handleCardInsert
    protected void handleDepositCash() {
        // fixme
        super.handleDepositCash();
        DepositSlotEmulatorController.appendTextArea("Cash Deposited");
    } // handleDepositCash

    protected void handleDeposit(String msg) {
        super.handleDeposit(msg);
        switch (msg) {
            case "Confirm":
                //user confirm the deposit amount
                DepositSlotEmulatorController.confirmClear();
                break;

            case "OpenSlot":
                DepositSlotEmulatorController.updateCardStatus("Deposit Slot is open");
                DepositSlotEmulatorController.setTransactionStatus(msg);
                break;

            case "CloseSlot":
                DepositSlotEmulatorController.updateCardStatus("Deposit Slot is closed");
                DepositSlotEmulatorController.setTransactionStatus(msg);
                break;
        }
    }

    //------------------------------------------------------------
    //Handle alert and repositions GUI
    @Override
    protected void alert() {
        super.alert();
        DepositSlotEmulator depositSlotEmulator = this;

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                myStage.toFront();
            }
        });
    }

    protected void reset() {
        super.reset();
        diagnostic();
        DepositSlotEmulatorController.amtField.setText("");
        DepositSlotEmulatorController.DepositSlotTextArea.setText("");
    }

    private void diagnostic() {
        handleDeposit("OpenSlot");
        DepositSlotEmulatorController.amtField.setText("0");
        if (DepositSlotEmulatorController.deposit(true)) {
            handleDeposit("CloseSlot");
            atmss.send(new Msg(id, mbox, Msg.Type.Reset, "healthy"));
        } else {
            atmss.send(new Msg(id, mbox, Msg.Type.Reset, "reset failure"));
        }
    }
}
