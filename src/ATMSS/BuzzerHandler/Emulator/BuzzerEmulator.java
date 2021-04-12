package ATMSS.BuzzerHandler.Emulator;

import ATMSS.ATMSSStarter;
import ATMSS.BuzzerHandler.BuzzerHandler;
import AppKickstarter.misc.Msg;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class BuzzerEmulator extends BuzzerHandler {
    private ATMSSStarter atmssStarter;
    private String id;
    private Stage myStage;
    private BuzzerEmulatorController buzzerEmulatorController;

    public BuzzerEmulator(String id, ATMSSStarter atmssStarter) {
        super(id, atmssStarter);
        this.atmssStarter = atmssStarter;
        this.id = id;
    }

    public void start() throws Exception {
        Parent root;
        myStage = new Stage();
        FXMLLoader loader = new FXMLLoader();
        String fxmlName = "BuzzerEmulator.fxml";
        loader.setLocation(BuzzerEmulator.class.getResource(fxmlName));
        root = loader.load();
        buzzerEmulatorController = (BuzzerEmulatorController) loader.getController();
        buzzerEmulatorController.initialize(id, atmssStarter, log, this);
        myStage.initStyle(StageStyle.DECORATED);
        myStage.setScene(new Scene(root, 300, 300));
        myStage.setTitle("Buzzer");
        myStage.setResizable(false);
        myStage.setOnCloseRequest((WindowEvent event) -> {
            atmssStarter.stopApp();
            Platform.exit();
        });
        myStage.show();
    }

    protected boolean alert(String msg) {
        super.alert(msg);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                myStage.toFront();//move the stage to the front
                buzzerEmulatorController.sound();
                buzzerEmulatorController.appendTextArea(msg);
            }
        });
        return true;
    }

    protected void reset() {
        super.reset();
        diagnostic();
    }

    private void diagnostic() {
        if (alert("")) {
            atmss.send(new Msg(id, mbox, Msg.Type.Reset, "healthy"));
        } else {
            atmss.send(new Msg(id, mbox, Msg.Type.Reset, "reset failure"));
        }
    }
}
