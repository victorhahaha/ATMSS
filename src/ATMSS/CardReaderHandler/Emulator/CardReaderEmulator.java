package ATMSS.CardReaderHandler.Emulator;

import ATMSS.ATMSSStarter;
import ATMSS.CardReaderHandler.CardReaderHandler;

import AppKickstarter.misc.Msg;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;


//======================================================================
// CardReaderEmulator
public class CardReaderEmulator extends CardReaderHandler {
    private ATMSSStarter atmssStarter;
    private String id;
    private Stage myStage;
    private CardReaderEmulatorController cardReaderEmulatorController;

    //------------------------------------------------------------
    // CardReaderEmulator
    public CardReaderEmulator(String id, ATMSSStarter atmssStarter) {
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
	String fxmlName = "CardReaderEmulator.fxml";
	loader.setLocation(CardReaderEmulator.class.getResource(fxmlName));
	root = loader.load();
	cardReaderEmulatorController = (CardReaderEmulatorController) loader.getController();
	cardReaderEmulatorController.initialize(id, atmssStarter, log, this);
	myStage.initStyle(StageStyle.DECORATED);
	myStage.setScene(new Scene(root, 350, 470));
	myStage.setTitle("Card Reader");
	myStage.setResizable(false);
	myStage.setOnCloseRequest((WindowEvent event) -> {
	    atmssStarter.stopApp();
	    Platform.exit();
	});
	myStage.show();
    } // CardReaderEmulator


    //------------------------------------------------------------
    // handleCardInsert
    protected void handleCardInsert() {
        // fixme
	super.handleCardInsert();
		Platform.runLater(() -> {
			cardReaderEmulatorController.appendTextArea("Card Inserted");
		});

	cardReaderEmulatorController.updateCardStatus("Card Inserted");
    } // handleCardInsert


    //------------------------------------------------------------
    // handleCardEject
    protected void handleCardEject() {
        // fixme
	super.handleCardEject();
	cardReaderEmulatorController.appendTextArea("Card Ejected");
	cardReaderEmulatorController.updateCardStatus("Card Ejected");
    } // handleCardEject


    //------------------------------------------------------------
    // handleCardRemove
    protected void handleCardRemove() {
	// fixme
	super.handleCardRemove();
	cardReaderEmulatorController.clearCardNum();
	cardReaderEmulatorController.appendTextArea("Card Removed");
	cardReaderEmulatorController.updateCardStatus("Card Reader Empty");
    } // handleCardRemove

	//------------------------------------------------------------
	// handleCardRetain
	protected void handleCardRetain() {
		// fixme
		super.handleCardRetain();
		cardReaderEmulatorController.clearCardNum();
		cardReaderEmulatorController.cardRetain();
		cardReaderEmulatorController.appendTextArea("Card Retained");
		cardReaderEmulatorController.updateCardStatus("Card Reader Empty");
	} // handleCardRetain

	protected void reset() {
		super.reset();
		diagnostic();
		cardReaderEmulatorController.card1Btn.setDisable(false);
		cardReaderEmulatorController.card2Btn.setDisable(false);
		cardReaderEmulatorController.card3Btn.setDisable(false);
		cardReaderEmulatorController.cardReaderTextArea.setText("");
	}

	private void diagnostic() {
		int stepNum = 5;
		for (int i = 0; i < stepNum; i++) {
				switch (i) {
					case 3:
					case 0:
						handleCardInsert();
						if (statusNotEqual("Card Inserted")) {
							return;
						}
						break;

					case 1:
						handleCardEject();
						if (statusNotEqual("Card Ejected")) {
							return;
						}
						break;

					case 2:
						handleCardRemove();
						if (statusNotEqual("Card Reader Empty")) {
							return;
						}
						break;

					case 4:
						handleCardRetain();
						if (statusNotEqual("Card Reader Empty")) {
							return;
						}
						break;
				}
		}
		atmss.send(new Msg(id, mbox, Msg.Type.Reset, "healthy"));
	}

	private boolean statusNotEqual(String status) {
		if (!cardReaderEmulatorController.cardStatusField.getText().equals(status)) {
			atmss.send(new Msg(id, mbox, Msg.Type.Reset, "reset failure"));
			return true;
		}
		return false;
	}
} // CardReaderEmulator
