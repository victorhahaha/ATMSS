package ATMSS.TouchDisplayHandler.Emulator;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.MBox;
import AppKickstarter.misc.Msg;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//======================================================================
// TouchDisplayEmulatorController
public class TouchDisplayEmulatorController {
    private String id;
    private AppKickstarter appKickstarter;
    private Logger log;
    private TouchDisplayEmulator touchDisplayEmulator;
    private MBox touchDisplayMBox;
    public Label blankTopLabel;
    public Label blankScreenLabel;
    public Label blankAmountLabel;
    public Label menuLabel;
    public Label menuTopLabel;
    public HBox buttonHBox;
    public VBox vboxLeft;
    public VBox vboxRight;
    public Label confirmationLabel;
    public Label confirmationInformationLabel;
    public HBox confirmationHBox;

    public String[] funcAry = {"Cash Deposit", "Money Transfer", "Cash Withdrawal", "Account Balance Enquiry", "Change Operating Account"};

    private final Integer startTime = 4;
    private Integer countDown = startTime;
    private static boolean loggedIn = false;
    private static StringBuilder blankAmountStringBuild = new StringBuilder();
    private static String operatingAcc = "";
    private static int currentPage = 0;         //0: welcome, 1: enter PIN, 2: initial account select, 3: main menu, 4: cash deposit, 5: money transfer, 6: cash withdraw, 7: account enquiry


    //------------------------------------------------------------
    // initialize
    public void initialize(String id, AppKickstarter appKickstarter, Logger log, TouchDisplayEmulator touchDisplayEmulator) {
        this.id = id;
        this.appKickstarter = appKickstarter;
        this.log = log;
        this.touchDisplayEmulator = touchDisplayEmulator;
        this.touchDisplayMBox = appKickstarter.getThread("TouchDisplayHandler").getMBox();
    } // initialize


    //------------------------------------------------------------
    // td_mouseClick
    public void td_mouseClick(MouseEvent mouseEvent) {
        int x = (int) mouseEvent.getX();
        int y = (int) mouseEvent.getY();

        log.fine(id + ": mouse clicked: -- (" + x + ", " + y + ")");
        if (loggedIn) {
            if ("StackPane".equals(mouseEvent.getSource().getClass().getSimpleName())) {
                StackPane targetPane = (StackPane) mouseEvent.getSource();
                Label targetLabel = (Label) targetPane.getChildren().get(0);
                Pattern accPattern = Pattern.compile("\\d{3}-\\d{3}-\\d{3}");
                Matcher accMatcher = accPattern.matcher(targetLabel.getText());
                if (accMatcher.matches()) {
                    if (currentPage == 2) {
                        operatingAcc = targetLabel.getText();
                        touchDisplayMBox.send(new Msg(id, touchDisplayMBox, Msg.Type.TD_UpdateDisplay, "MainMenu"));
                    }
                    touchDisplayMBox.send(new Msg(id, touchDisplayMBox, Msg.Type.Selected_Acc, targetLabel.getText()));
                } else {
                    touchDisplayMBox.send(new Msg(id, touchDisplayMBox, Msg.Type.TD_MouseClicked, targetLabel.getText()));
                }
            } else {
                touchDisplayMBox.send(new Msg(id, touchDisplayMBox, Msg.Type.TD_MouseClicked, x + " " + y + " Logged In: " + loggedIn));
            }

        } else {
            touchDisplayMBox.send(new Msg(id, touchDisplayMBox, Msg.Type.TD_MouseClicked, x + " " + y + " Logged In: " + loggedIn));
        }
    } // td_mouseClick

    public void setLoginTrue() {
        loggedIn = true;
        touchDisplayMBox.send(new Msg(id, touchDisplayMBox, Msg.Type.GetAccount, ""));
    }

    public static int getCurrentPage() {
        return currentPage;
    }

    public void welcomePage() {
        welcomePage("1 1 1");
    }

    public void welcomePage(String details) {
        currentPage = 0;
        loggedIn = false;
        operatingAcc = "";
        eraseText();
        String[] detail = details.split("/");
        StringTokenizer denomsTokens = new StringTokenizer(detail[0]);
        if (detail.length > 1 && detail[1].equals("Out of Service")) {
            //if critical situation occurs, atm is out of service
            blankAmountStringBuild.append(detail[1]);
        } else {
            //show money notes inventory
            String denomsAvailable = "\n\nMoney Notes Denominations available: ";
            int count = 0;
            for (int i = 0; denomsTokens.hasMoreTokens(); i++) {
                String token = denomsTokens.nextToken();
                if (Integer.parseInt(token) > 0) {
                    if (i == 0) {
                        denomsAvailable += "$100, ";
                    } else if (i == 1) {
                        denomsAvailable += "$500, ";
                    } else if (i == 2) {
                        denomsAvailable += "$1000, ";
                    }
                    count++;
                }
            }
            blankAmountStringBuild.append("Please Insert ATM Card");
            if (count > 0) {
                blankAmountStringBuild.append(denomsAvailable);
            }
            if (detail.length > 1) {
                //non-critical situation
                blankAmountStringBuild.append("\n\nComponents not working: ");
                StringTokenizer malTokens = new StringTokenizer(detail[1]);
                blankAmountStringBuild.append(malTokens.nextToken());
                while (malTokens.hasMoreTokens()) {
                    blankAmountStringBuild.append(", ").append(malTokens.nextToken());
                }
            }
        }
        blankTopLabel.setText("Welcome to ATM system emulator");
        blankScreenLabel.setText(blankAmountStringBuild.toString());
        blankAmountStringBuild.delete(0, blankAmountStringBuild.length());
    }

    public void enterPINPage(boolean enterPIN) {
        currentPage = 1;
        blankTopLabel.setText("Please Enter the PIN:");
        blankScreenLabel.setText("Please Press Enter Button after Entering PIN\n\nPlease Press Erase Button If You Type Wrong\n\nPlease Press Cancel If You Want to Cancel Transaction\n\n\"00\" cannot be used at this page\n\n");
        if (enterPIN) {
            if (blankAmountStringBuild.length() < 9) {
                blankAmountStringBuild.append("*");
            }
            blankAmountLabel.setText(blankAmountStringBuild.toString());
        } else {
            eraseText();
        }
    }

    public void eraseText(){
        blankAmountStringBuild.delete(0, blankAmountStringBuild.length());
        blankAmountLabel.setText("");
    }

    public void mainMenuBox() {
        blankAmountStringBuild.delete(0, blankAmountStringBuild.length());
        currentPage = 3;
        menuLabel.setText("Welcome back, "+ operatingAcc +"\n\nPlease select ...");
        mainMenuReset();
        menuDrawing(3, funcAry);
    }

    public void accountSelectGUI(String acc, boolean transfer) {
        String[] accounts = acc.split("/");
        if (!transfer) {        //initial account select
            currentPage = 2;
            menuLabel.setText("Please select an account you want to operate");
        } else {                //money transfer account select
            menuTopLabel.setText("Operating Account Number: " + operatingAcc);
            currentPage = 5;
            menuLabel.setText("Please select an account you want to transfer to");
            String[] remainingAccounts = new String[accounts.length - 1];
            int j = 0;
            for (String account : accounts) {
                if (!operatingAcc.equals(account)) {
                    remainingAccounts[j] = account;
                    j++;
                }
            }
            accounts = remainingAccounts;
        }
        menuDrawing(2, accounts);
    }

    public void cashDepositPage() {
        cashDepositPage("");
    }

    protected void cashDepositPage(String amount) {
        currentPage = 4;
        confirmationLabel.setText("Operating Account Number: " + operatingAcc);
        int buttonNum = 2;
        Rectangle[] rectangles = new Rectangle[buttonNum];
        Label[] labels = new Label[buttonNum];
        StackPane[] stackPanes = new StackPane[buttonNum];
        for (int i = 0; i < buttonNum; i++) {
            rectangles[i] = rectangleInit(rectangles[i]);
            labels[i] = labelInit(labels[i]);
            if (i % 2 == 0) {
                labels[i].setText("Cancel");
            } else {
                labels[i].setText("Confirm Amount");
            }
            stackPanes[i] = new StackPane();
            stackPanes[i].getChildren().addAll(labels[i], rectangles[i]);
            stackPanes[i].setOnMousePressed(this::td_mouseClick);
            confirmationHBox.getChildren().add(stackPanes[i]);
        }
        if (amount.equals("")) {
            stackPanes[0].setVisible(false);
            stackPanes[0].setDisable(true);
            stackPanes[1].setVisible(false);
            stackPanes[1].setDisable(true);
            confirmationInformationLabel.setText("Please insert money in cash deposit collector");
        } else {
            stackPanes[0].setVisible(true);
            stackPanes[0].setDisable(false);
            stackPanes[1].setVisible(true);
            stackPanes[1].setDisable(false);
            String[] amounts = amount.split(" ");
            int total100 = Integer.parseInt(amounts[0]) * 100;
            int total500 = Integer.parseInt(amounts[1]) * 500;
            int total1000 = Integer.parseInt(amounts[2]) * 1000;
            confirmationInformationLabel.setText("Number of $100 money notes: " + amounts[0] + "= $100 x " + amounts[0] + " = " + "$" + total100 + "\n" + "Number of $500 money notes: " + amounts[1] + "= $500 x " + amounts[1] + " = " + "$" + total500 + "\n" + "Number of $1000 money notes: " + amounts[2] + "= $1000 x " + amounts[2] + " = " + "$" + total1000 + "\n\n" + "Total amount: $" + (total100 + total500 + total1000));
        }
    }

    protected void cashDepositFinish(String amount) {
        currentPage = 4;
        menuTopLabel.setText("Operating Account Number: " + operatingAcc);
        menuLabel.setText("Total amount deposit to " + operatingAcc + ": $" + amount);
        transactionFinalPage();
    }

    protected void moneyTransferPage(String transferAcc, String typed) {
        currentPage = 5;
        if (!typed.equals("")) {
            blankAmountStringBuild.append(typed);
            blankAmountLabel.setText(blankAmountStringBuild.toString());
        } else {
            eraseText();
        }
        blankTopLabel.setText("Operating Account Number: " + operatingAcc +"\n\n Selected Transfer Account Number: " + transferAcc);
        blankScreenLabel.setText("Please enter the amount you want to transfer\n\nPlease press Enter button after entering the amount\n\nPlease press Erase button if you type wrong");
    }

    protected void moneyTransferFinish(String details) {
        currentPage = 5;
        String[] detail = details.split("_");
        menuTopLabel.setText("Operating Account Number: " + operatingAcc);
        menuLabel.setText("Total amount transferred from " + operatingAcc + " to " +detail[0] + ": $" + detail[1]);
        transactionFinalPage();
    }

    protected void cashWithdrawalPage(String amount) {
        currentPage = 6;
        if (!amount.equals("")) {
            blankAmountStringBuild.append(amount);
            blankAmountLabel.setText(blankAmountStringBuild.toString());
        } else {
            eraseText();
        }
        blankTopLabel.setText("Operating Account Number: " + operatingAcc);
        blankScreenLabel.setText("Please enter the amount you want to withdraw\n\nPlease press Enter button after entering the amount\n\nPlease press Erase button if you type wrong");
    }

    protected void cashDispensePage(String amount, boolean taken) {
        currentPage = 6;
        menuTopLabel.setText("Operating Account Number: " + operatingAcc);
        menuLabel.setText("Total amount dispensed: $" + amount + "\n\nPlease take away your money before your next action");
        if (taken) {
            transactionFinalPage();
        }
    }

    protected void accountEnquiryMenu(String amount) {
        currentPage = 7;
        menuTopLabel.setText("Operating Account Number: " + operatingAcc);
        menuLabel.setText("Amount in this account: $" + amount);
        transactionFinalPage();
    }

    private Rectangle rectangleInit(Rectangle target) {
        target = new Rectangle();
        target.setStroke(Color.BLACK);
        target.setFill(Color.TRANSPARENT);
        target.setArcWidth(5);
        target.setArcHeight(5);
        target.setWidth(320);
        target.setHeight(100);
        return target;
    }

    private Label labelInit(Label target) {
        target = new Label();
        target.setWrapText(true);
        target.setAlignment(Pos.CENTER);
        target.setMaxSize(320, 100);
        target.setTextAlignment(TextAlignment.CENTER);
        target.setPrefSize(320, 100);
        target.setFont(new Font(20));
        return target;
    }

    private void mainMenuReset() {
        buttonHBox.setPrefHeight(300);
        menuLabel.setPrefHeight(130);
        menuLabel.setAlignment(Pos.TOP_CENTER);
    }

    private void menuDrawing(int rectEachSide, String[] array) {
        menuDrawing(rectEachSide, array, 0);
    }

    private void menuDrawing(int rectEachSide, String[] array, int menuPageNum) {
        Rectangle[] rectAry = new Rectangle[rectEachSide * 2];
        Label[] labelAry = new Label[rectEachSide * 2];
        //if less than 6 functions on one menu page, those no function part will be blank
        //if more than 6 functions on one menu page, rightLabel[2] will be for the next page
        //if only 6 functions on one menu page, rightLabel[2] will be normal
        int notSetFunc = array.length;
        int funcPerPage = rectEachSide * 2 - 1;
        for (int i = 0; i < rectEachSide * 2; i++) {
            rectAry[i] = rectangleInit(rectAry[i]);
            labelAry[i] = labelInit(labelAry[i]);
            if (notSetFunc > 0 && i == funcPerPage) {
                labelAry[i].setText("Next Page");
                menuPageNum++;
            } else if (notSetFunc > 0) {
                labelAry[i].setText(array[i + menuPageNum * funcPerPage]);
                notSetFunc--;
            }
        }
        menuStackPaneSetting(rectAry, labelAry, rectEachSide * 2);
    }

    private void menuStackPaneSetting(Rectangle[] rectAry, Label[] labelAry, int numButton) {
        if (numButton == 4) {
            menuLabel.setAlignment(Pos.CENTER);
            menuLabel.setPrefHeight(230);
            buttonHBox.setPrefHeight(200);
        }
        StackPane[] stack = new StackPane[numButton];
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new StackPane();
            stack[i].getChildren().addAll(labelAry[i], rectAry[i]);
            if (labelAry[i].getText().equals("")) {
                stack[i].setDisable(true);
            }
            if (i % 2 == 0) {
                vboxLeft.getChildren().add(stack[i]);
            } else {
                vboxRight.getChildren().add(stack[i]);
            }
            stack[i].setOnMousePressed(this::td_mouseClick);
        }
    }

    private void transactionFinalPage() {
        String[] labelText = {"Continue Transaction and Print Advice", "End Transaction and Print Advice", "Continue Transaction", "End Transaction"};
        int maxEachSide = 2;
        menuDrawing(maxEachSide, labelText);
    }

    protected void errorPage(String errorMsg) {
        blankTopLabel.setText("Error");
        blankScreenLabel.setText(errorMsg);
        countDown(errorMsg);
    }

    private void countDown(String details) {
        Timeline timeline = new Timeline();
        timeline.setCycleCount(5);
        if (timeline != null) {
            timeline.stop();
        }
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(1), event -> {
            countDown--;
            blankAmountLabel.setText(countDown.toString());
            if (countDown <= 0) {
                timeline.stop();
                touchDisplayMBox.send(new Msg(id, touchDisplayMBox, Msg.Type.ErrorRedirect, details));
            }
        });
        timeline.getKeyFrames().add(keyFrame);
        timeline.playFromStart();
    }
} // TouchDisplayEmulatorController
