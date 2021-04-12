package ATMSS.DepositSlotHandler.Emulator;

import ATMSS.ATMSSStarter;
import ATMSS.DepositSlotHandler.DepositSlotHandler;

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
        DepositSlotEmulatorController.setTransactionStatus(msg);

    	if (msg.equals("OpenSlot")) { 		//if is set to true, means deposit slot open
    		DepositSlotEmulatorController.updateCardStatus("Deposit Slot is open");
    	}else if (msg.equals("CloseSlot")) {
    		DepositSlotEmulatorController.updateCardStatus("Deposit Slot is closed");
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
}
