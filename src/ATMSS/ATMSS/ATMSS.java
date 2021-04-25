package ATMSS.ATMSS;

import AppKickstarter.AppKickstarter;
import AppKickstarter.misc.*;
import AppKickstarter.timer.Timer;

import java.util.StringTokenizer;


//======================================================================
// ATMSS
public class ATMSS extends AppThread {
    private int pollingTime;
    private final int bamsPollTime = 20000;
    private int atmssTimerID = -1;
    private int depositTimerID = -1;
    private int dispenseTimerID = -1;
    private int BAMSResponseTimerID = -1;

    private boolean loggedIn = false;
    private static String transaction = "";
    private static String cardNum = "";
    private static String selectedAcc = "";
    private static String transferAcc = "";
    private static int denom100 = 10000;
    private static int denom500 = 10000;
    private static int denom1000 = 10000;
    private static String denomsToChange = "";
    private String pin = "";
    private String amountTyped = "";
    private boolean getPin = false;
    private boolean getAmount = false;
    private int errorCount = 0;
    private static String malfunctions = "";

    private MBox cardReaderMBox;
    private MBox keypadMBox;
    private MBox touchDisplayMBox;
    private MBox DepositSlotMBox;
    private MBox DispenserSlotMBox;
    private MBox AdvicePrinterMBox;
    private MBox BuzzerMBox;
    private MBox bamsThreadMBox;

    //------------------------------------------------------------
    // ATMSS
    public ATMSS(String id, AppKickstarter appKickstarter) throws Exception {
        super(id, appKickstarter);
        pollingTime = Integer.parseInt(appKickstarter.getProperty("ATMSS.PollingTime"));
    } // ATMSS


    //------------------------------------------------------------
    // run
    public void run() {
        atmssTimerID = Timer.setTimer(id, mbox, pollingTime);
        BAMSResponseTimerID = Timer.setTimer(id, mbox, bamsPollTime);
        log.info(id + ": starting...");

        cardReaderMBox = appKickstarter.getThread("CardReaderHandler").getMBox();
        keypadMBox = appKickstarter.getThread("KeypadHandler").getMBox();
        touchDisplayMBox = appKickstarter.getThread("TouchDisplayHandler").getMBox();
        DepositSlotMBox = appKickstarter.getThread("DepositSlotHandler").getMBox();
        DispenserSlotMBox = appKickstarter.getThread("DispenserSlotHandler").getMBox();
        AdvicePrinterMBox = appKickstarter.getThread("AdvicePrinterHandler").getMBox();
        BuzzerMBox = appKickstarter.getThread("BuzzerHandler").getMBox();
        bamsThreadMBox = appKickstarter.getThread("BAMSThreadHandler").getMBox();

        HWreset();

        for (boolean quit = false; !quit; ) {
            Msg msg = mbox.receive();

            log.fine(id + ": message received: [" + msg + "].");
            if (!msg.getType().equals(Msg.Type.TimesUp) || !msg.getType().equals(Msg.Type.PollAck)) {
                log.info(id + ": bams timer reset");
                Timer.cancelTimer(id, mbox, BAMSResponseTimerID);
                Timer.setTimer(id, mbox, bamsPollTime, BAMSResponseTimerID);
            }

            switch (msg.getType()) {
                case TD_MouseClicked:
                    log.info("MouseCLicked: " + msg.getDetails());
                    processMouseClicked(msg);
                    //after processing click depending on x-y AND loggedin is true, change to different screen for deposit/withdraw/transfer
                    break;

                case KP_KeyPressed:
                    log.info("KeyPressed: " + msg.getDetails());
                    processKeyPressed(msg);
                    break;

                case CR_CardInserted:        //if receive card inserted from cardreader, do:
                    log.info("CardInserted: " + msg.getDetails());
                    cardNum = msg.getDetails();
                    getPin = true;        //if we are now looking for pin,
                    keypadMBox.send(new Msg(id, mbox, Msg.Type.Alert, ""));
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "PIN Required"));
                    //if card inserted proceed to ask pin (send msg to ask for PIN)
                    break;

                case LoggedIn: //BAMSHandler send msg back and indicate login success or fail
                    if (msg.getDetails().equals("Success")) {       //success
                        //if success login return some boolean variable that enable all methods that need login to be true to act
                        loggedIn = true;
                        getPin = false; //on login success, no need pin anymore
                        touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.LoggedIn, "Success")); //change screen to menu to select account
                        //send verification success notification to touchscreen display so that screen is changed
                    } else if (msg.getDetails().equals("Fail")) {   //fail
                        errorCount++;
                        if (errorCount >= 3) {      //if error PIN >= 3, retain card
                            log.info(id + ": enter wrong PIN three times, retain the card");
                            //jump to the page saying card is retained
                            //instruct card reader retain card
                            BuzzerMBox.send(new Msg(id, mbox, Msg.Type.Alert, "Incorrect pin thrice! Card retained!"));
                            cardReaderMBox.send(new Msg(id, mbox, Msg.Type.CR_RetainCard, ""));
                            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Error, "Card Retained"));
                        } else {        //situation that enter the wrong PIN for one or two times
                            //give error message
                            pin = "";
                            BuzzerMBox.send(new Msg(id, mbox, Msg.Type.Alert, "Incorrect pin! Please try again!"));
                            keypadMBox.send(new Msg(id, mbox, Msg.Type.Alert, ""));
                            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Error, "Wrong PIN\n\nPlease ensure you enter the right PIN"));
                        }
                    }
                    break;

                case GetAccount:        //send BAMSHandler msg and ask for the accounts info of the card
                    bamsThreadMBox.send(new Msg(id, mbox, Msg.Type.GetAccount, cardNum));
                    break;

                case ReceiveAccount:    //receive accounts info of specific card from BAMS
                    if (!selectedAcc.equals("") && !msg.getDetails().contains("/")) {       //only for money transfer at this moment
                        //this card has only one account and cannot do money transfer
                        touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Error, "This card has only one account\n\nCannot do " + transaction));
                    } else {
                        touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_SelectAccount, transaction + "_" + msg.getDetails()));
                    }
                    break;

                case Selected_Acc:      //receive input of selected account
                    if (!transaction.equals("Money Transfer")) {       //choose which account to operate
                        transaction = "";
                        selectedAcc = msg.getDetails();        //on logout please clear this value
                    } else {            //choose account to transfer money
                        if (!selectedAcc.equals("")) {
                            transferAcc = msg.getDetails();
                            getAmount = true;
                            //update touchdisplay
                            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "Money Transfer_" + transferAcc));
                        }
                    }
                    break;

                case MoneyTransferResult:           //Receive result about money transfer from BAMS
                    log.info(id + ": Money Transfer from " + selectedAcc + " to " + transferAcc + ": $" + msg.getDetails());
                    amountTyped = msg.getDetails();
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.MoneyTransferResult, transferAcc + "_" + msg.getDetails()));
                    break;

                case Dispense:          //Receive msg from BAMS that allow withdraw and dispense money
                    //may not have enough money in the account
                    log.info(id + ": Cash Dispense: $" + msg.getDetails());
                    amountTyped = msg.getDetails();
                    String amountDispense = denomDispenseCalculate(msg.getDetails());
                    denomsToChange = amountDispense;        //format: "0 0 0"
                    DispenserSlotMBox.send(new Msg(id, mbox, Msg.Type.Denom_sum, amountDispense));        //process the notes to dispense
                    BuzzerMBox.send(new Msg(id, mbox, Msg.Type.Alert, "Dispenser Slot Opening!"));
                    dispenseTimerID = Timer.setTimer(id, mbox, 15000);
                    DispenserSlotMBox.send(new Msg(id, mbox, Msg.Type.Dispense, "OpenSlot")); //this is supposed to open slot
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Dispense, msg.getDetails()));
                    break;

                case DispenseFinish:
                    DispenserSlotMBox.send(new Msg(id, mbox, Msg.Type.Dispense, "CloseSlot"));
                    //stop dispense slot timer
                    Timer.cancelTimer(id, mbox, dispenseTimerID);
                    //update touch display
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.DispenseFinish, msg.getDetails()));
                    //update money notes inventory
                    updateDenomsInventory(denomsToChange, false);
                    log.info(id + ": denoms change: decrease: " + denomsToChange);
                    log.info(id + ": denoms: $100: " + denom100 + " $500: " + denom500 + " $1000: " + denom1000);
                    break;

                case EnquiryResult:     //Account enquiry result from BAMS
                    amountTyped = msg.getDetails();
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.EnquiryResult, msg.getDetails()));
                    break;

                case Denom_sum:         //receive cash from deposit slot
                    //receive money notes, update the money notes inventory
                    denomsToChange = msg.getDetails();
                    String[] denom = msg.getDetails().split(" ");
                    amountTyped = (Integer.parseInt(denom[0]) * 100 + Integer.parseInt(denom[1]) * 500 + Integer.parseInt(denom[2]) * 1000) + "";
                    log.info("CashDeposit Denominations: " + amountTyped);
                    Timer.cancelTimer(id, mbox, depositTimerID);//remove deposit slot timer
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Denom_sum, msg.getDetails()));
                    break;

                case DepositResult:     //receive deposit result from BAMS
                    amountTyped = msg.getDetails();
                    if ((int) Double.parseDouble(amountTyped) > -1) {
                        DispenserSlotMBox.send(new Msg(id, mbox, Msg.Type.DenomsInventoryUpdate, denomsToChange));
                        updateDenomsInventory(denomsToChange, true);
                        log.info(id + ": denoms change: increase: " + denomsToChange);
                        log.info(id + ": denoms: $100: " + denom100 + " $500: " + denom500 + " $1000: " + denom1000);
                    }
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.DepositResult, msg.getDetails()));
                    break;

                case TimesUp:
                    StringTokenizer tokens = new StringTokenizer(msg.getDetails());
                    String msgtype = tokens.nextToken();
                    int timerID = Integer.parseInt(tokens.nextToken());

                    if (timerID == atmssTimerID) {
                        atmssTimerID = Timer.setTimer(id, mbox, pollingTime);
                        log.info("Poll: " + msg.getDetails());
                        cardReaderMBox.send(new Msg(id, mbox, Msg.Type.Poll, ""));
                        keypadMBox.send(new Msg(id, mbox, Msg.Type.Poll, ""));
                        touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Poll, ""));
                        DepositSlotMBox.send(new Msg(id, mbox, Msg.Type.Poll, ""));
                        DispenserSlotMBox.send(new Msg(id, mbox, Msg.Type.Poll, ""));
                        AdvicePrinterMBox.send(new Msg(id, mbox, Msg.Type.Poll, ""));
                        BuzzerMBox.send(new Msg(id, mbox, Msg.Type.Poll, ""));
                    } else if (timerID == depositTimerID) {
                        depositTimerID = -1;
                        DepositSlotMBox.send(new Msg(id, mbox, Msg.Type.Deposit, "CloseSlot"));
                        //touchdisplay update
                        touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Denom_sum, "0 0 0"));
                    } else if (timerID == dispenseTimerID) {
                        //emergency situation: retain card, logout, retain money
                        log.warning(id + ": dispenser time out, $" + amountTyped + " and card " + cardNum + " has been retained");
                        dispenseTimerID = -1;
                        DispenserSlotMBox.send(new Msg(id, mbox, Msg.Type.Dispense, "CloseSlot"));
                        DispenserSlotMBox.send(new Msg(id, mbox, Msg.Type.DenomsInventoryUpdate, denomsToChange));
                        cardReaderMBox.send(new Msg(id, mbox, Msg.Type.CR_RetainCard, ""));
                        touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Error, "Dispenser slot time out"));
                        log.info(id + ": denoms inventory: $100: " + denom100 + " $500: " + denom500 + " $1000: " + denom1000);
                    } else if (timerID == BAMSResponseTimerID) {
                        log.severe(id + " : Unable to receive response from BAMS within acceptable time frame!");
                        //some error msg, change touch display etc.
                        //this situation should happen when loggedIn is false
                        malfunctions = "Out of Service";
                        touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "Welcome_" + denom100 + " " + denom500 + " " + denom1000 + "/" + malfunctions));
                        notOperate();
                    }

                    break;

                case PollAck:
                    log.info("PollAck: " + msg.getDetails());
                    break;

                case BAMSAck:
                    Timer.cancelTimer(id, mbox, BAMSResponseTimerID);
                    Timer.setTimer(id, mbox, bamsPollTime, BAMSResponseTimerID);
                    log.info("BAMSAck: " + msg.getDetails());
                    break;

                case Terminate:
                    quit = true;
                    break;

                case DenomsInventoryCheck:
                    String denomsKeeping = denom100 + " " + denom500 + " " + denom1000;
                    if (!denomsKeeping.equals(msg.getDetails())) {
                        StringTokenizer denomsToken = new StringTokenizer(msg.getDetails());
                        denom100 = Integer.parseInt(denomsToken.nextToken());
                        denom500 = Integer.parseInt(denomsToken.nextToken());
                        denom1000 = Integer.parseInt(denomsToken.nextToken());
                        log.warning(id + ": Denoms inventory keeping incorrect");
                    }
                    break;

                case Reset:
                    if (msg.getDetails().equals("healthy")) {
                        log.info(id + " " + msg.getSender() + " reset: healthy");
                        touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "Welcome_" + denom100 + " " + denom500 + " " + denom1000 + "/" + malfunctions));
                        break;
                    } //others are reset failure, the consequence equals to error

                case Error:     //receive error that cannot fix by itself
                    log.severe(id + ": " + msg);
                    switch (msg.getSender()) {
                        case "AdvicePrinterHandler":
                            //non-critical situation
                            if (malfunctions.equals("")) {
                                malfunctions = "AdvicePrinter";
                            } else {
                                malfunctions += " AdvicePrinter";
                            }
                            break;

                        case "BuzzerHandler":
                            //non-critical situation
                            if (malfunctions.equals("")) {
                                malfunctions = "Buzzer";
                            } else {
                                malfunctions += " Buzzer";
                            }
                            break;

                        case "BAMSThreadHandler":

                        case "DepositSlotHandler":

                        case "DispenserSlotHandler":
                            if (msg.getDetails().contains("run out of")) {
                                break;
                            }

                        case "KeypadHandler":
                            //redirect to welcome page and show maintenance
                            //critical situation --> out of service
                            malfunctions = "Out of Service";
                            if (loggedIn) {
                                //send touchdisplay a error display
                                //then return the card (and advice)
                                cardReaderMBox.send(new Msg(id, mbox, Msg.Type.CR_EjectCard, ""));
                                touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Error, msg.getDetails() + "\n\nPlease take back your card"));
                                break;
                            }

                        case "CardReaderHandler":
                            //send touchdisplay a error display and ask user to contact the bank
                            //critical situation --> out of service
                            malfunctions = "Out of Service";
                            if (loggedIn) {
                                //send touchdisplay a error display
                                //then return the card (and advice)
                                cardReaderMBox.send(new Msg(id, mbox, Msg.Type.CR_RetainCard, ""));
                                touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Error, msg.getDetails() + "\n\nPlease contact the bank"));
                                break;
                            } else {
                                touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "Welcome_" + denom100 + " " + denom500 + " " + denom1000 + "/" + malfunctions));
                                notOperate();
                            }
                            break;

                        case "TouchDisplayHandler":
                            //shut down directly
                            notOperate();
                            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Shutdown, ""));
                            break;
                    }
                    break;

                case ErrorRedirect:         //redirect error page to another page
                    switch (transaction) {
                        case "":            //In enter PIN page
                            if (errorCount >= 3 || msg.getDetails().contains("Network")) {
                                touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "Welcome_" + denom100 + " " + denom500 + " " + denom1000 + "/" + malfunctions));
                                allReset();
                            } else {
                                getPin = true;
                                touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "PIN Required"));
                            }
                            break;

                        case "Cash Deposit":

                        case "Money Transfer":

                        case "Change Operating Account":

                        case "Cash Withdrawal":
                            if (msg.getDetails().equals("Dispenser slot time out")) {
                                touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "Welcome_" + denom100 + " " + denom500 + " " + denom1000 + "/" + malfunctions));
                                allReset();
                                break;
                            }

                        case "Account Balance Enquiry":
                            if (malfunctions.equals("Out of Service")) {
                                touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "Welcome_" + denom100 + " " + denom500 + " " + denom1000 + "/" + malfunctions));
                                allReset();
                            } else {
                                touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "MainMenu"));
                                halfRest();
                            }
                            break;
                    }
                    break;

                case Shutdown:
                    //accept shutdown okay or failed
                    log.info(id + " " + msg.getSender() + ": shutdown: " + msg.getDetails());
                    break;

                default:
                    log.warning(id + ": unknown message type: [" + msg + "]");
            }
        }

        // declaring our departure
        appKickstarter.unregThread(this);
        log.info(id + ": terminating...");
    } // run


    //------------------------------------------------------------
    // processKeyPressed
    private void processKeyPressed(Msg msg) {
        // *** The following is an example only!! ***
        log.info(id + ": " + msg.getDetails() + " is pressed");
        if (msg.getDetails().compareToIgnoreCase("Cancel") == 0) {      //terminate whole transaction and eject card
            cardReaderMBox.send(new Msg(id, mbox, Msg.Type.CR_EjectCard, ""));
            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "erasePIN"));
            //should be a screen showing thank you first
            allReset();        //if transaction canceled, reset pin variable
            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "Welcome_" + denom100 + " " + denom500 + " " + denom1000 + "/" + malfunctions));
        } else if (getPin) {        //stage of accepting PIN
            keypadMBox.send(new Msg(id, mbox, Msg.Type.Alert, ""));
            // Set maximum password length to 9
            if (msg.getDetails().compareToIgnoreCase("Erase") == 0) {
                pin = "";        //if transaction canceled, reset pin variable
                touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "erasePIN"));
            } else if (msg.getDetails().compareToIgnoreCase("Enter") == 0) {
                // Prevent entering "00" at the end
                if (pin.length() > 9) {
                    pin = pin.substring(0, 9);
                }

                log.info(id + " : verifying cardnum and pin");
                bamsThreadMBox.send(new Msg(id, mbox, Msg.Type.Verify, cardNum + " " + pin));
                getPin = false;

                log.info("pin: " + pin);
                //send variables cardNum and pin to BAMS for login
            } else {
                //"00" is not allowed in enter PIN
                if (pin.length() < 9) {
                    switch (msg.getDetails()) {
                        case "1":
                        case "2":
                        case "3":
                        case "4":
                        case "5":
                        case "6":
                        case "7":
                        case "8":
                        case "9":
                        case "0":
                            pin += msg.getDetails();
                            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "enterPIN"));
                            break;
                        default:
                            break;
                    }
                }
            }
        } else if (getAmount) {     //stage of accepting amount input, e.g. cash withdraw and money transfer
            keypadMBox.send(new Msg(id, mbox, Msg.Type.Alert, ""));
            if (msg.getDetails().compareToIgnoreCase("Erase") == 0) {
                amountTyped = "";
                if (transaction.equals("Cash Withdrawal")) {
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, transaction));
                } else if (transaction.equals("Money Transfer")) {
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, transaction + "_" + transferAcc));
                }
            } else if (msg.getDetails().compareToIgnoreCase("Enter") == 0) {
                //send amountTyped to BAMS
                int amount = 0;
                if (amountTyped.equals("")) {       //prevent enter nothing
                    amountTyped = "0";
                }
                try {
                    amount = Integer.parseInt(amountTyped);
                } catch (NumberFormatException e) {
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Error, "Invalid Amount"));
                }
                //look at which transaction it is
                if (transaction.equals("Cash Withdrawal")) {
                    //only amount that is divisible by 100 can be withdrawn
                    if (amount % 100 == 0 && amount > 0) {    //check if amount is divisible by 100 and larger than 0
                        //send bams withdraw request
                        bamsThreadMBox.send(new Msg(id, mbox, Msg.Type.CashWithdraw, cardNum + " " + selectedAcc + " " + amountTyped));
                    } else {
                        //return error and reject withdraw request
                        touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Error, "Invalid Amount"));
                    }
                } else if (transaction.equals("Money Transfer")) {
                    bamsThreadMBox.send(new Msg(id, mbox, Msg.Type.MoneyTransferRequest, cardNum + " " + selectedAcc + " " + transferAcc + " " + amountTyped));
                }
                getAmount = false;
            } else {
                switch (msg.getDetails()) {
                    case "1":
                    case "2":
                    case "3":
                    case "4":
                    case "5":
                    case "6":
                    case "7":
                    case "8":
                    case "9":
                    case "00":
                    case "0":
                        amountTyped += msg.getDetails();
                        break;
                    default:
                        break;
                }
                //identify which transaction it is
                if (transaction.equals("Money Transfer")) {     //it is money transfer
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TextTyped, transferAcc + "_" + msg.getDetails()));
                } else {        //it is cash withdrawal
                    touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TextTyped, msg.getDetails()));
                }
            }
        }
    } // processKeyPressed


    //------------------------------------------------------------
    // processMouseClicked
    private void processMouseClicked(Msg msg) {
        // *** process mouse click here!!! ***
        if (loggedIn) {
            switch (transaction) {
                case "":        //main menu page
                    transaction = msg.getDetails();

                    switch (transaction) {
                        case "Cash Deposit":  //deposit
                            //set transaction to true
                            depositTimerID = Timer.setTimer(id, mbox, 15000);
                            //change touch screen display to ask how much to deposit
                            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, transaction));
                            DepositSlotMBox.send(new Msg(id, mbox, Msg.Type.Alert, ""));  //alert deposit slot

                            DepositSlotMBox.send(new Msg(id, mbox, Msg.Type.Deposit, "OpenSlot")); //open deposit slot
                            BuzzerMBox.send(new Msg(id, mbox, Msg.Type.Alert, "Deposit Slot Opened!"));

                            break;
                        case "Change Operating Account":
                        case "Money Transfer":
                            //set transaction to true
                            //change touch screen display to choose which acc to transfer from
                            //choose which acc to transfer to
                            //send msg to bams to transfer
                            bamsThreadMBox.send(new Msg(id, mbox, Msg.Type.GetAccount, cardNum));
                            break;
                        case "Cash Withdrawal":
                            //set transaction to true
                            //set timer
                            //change touch screen display to ask how much to withdraw
                            //alert keypad
                            getAmount = true;
                            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, transaction));
                            keypadMBox.send(new Msg(id, mbox, Msg.Type.Alert, ""));
                            break;
                        case "Account Balance Enquiry":
                            //set transaction to true
                            //check balance from BAMS
                            String enquiryDetails = cardNum + " " + selectedAcc;
                            bamsThreadMBox.send(new Msg(id, mbox, Msg.Type.AccountEnquiry, enquiryDetails));
                            break;
                        default:
                            //do nothing
                            transaction = "";
                            break;
                    }
                    break;

                case "Cash Deposit":
                    switch (msg.getDetails()) {
                        case "Confirm Amount":
                            //confirm the amount input and send bams deposit request
                            bamsThreadMBox.send(new Msg(id, mbox, Msg.Type.Deposit, cardNum + " " + selectedAcc + " " + amountTyped));
                            DepositSlotMBox.send(new Msg(id, mbox, Msg.Type.Deposit, "Confirm"));
                            break;

                        case "Cancel":
                            //the deposit slot does not take the money until confirm amount is pressed
                            //set transaction to true
                            amountTyped = "";
                            depositTimerID = Timer.setTimer(id, mbox, 15000);
                            //change touch screen display to ask how much to deposit
                            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, transaction));
                            DepositSlotMBox.send(new Msg(id, mbox, Msg.Type.Alert, ""));  //alert deposit slot

                            DepositSlotMBox.send(new Msg(id, mbox, Msg.Type.Deposit, "OpenSlot")); //open deposit slot
                            break;

                        default:
                            break;
                    }

                case "Money Transfer":

                case "Cash Withdrawal":

                case "Account Balance Enquiry":

                default:
                    String status;
                    //see the resulting amount success or fail
                    if (amountTyped.equals("-1") || amountTyped.equals("")) {
                        status = "Fail";
                    } else {
                        status = "Success";
                    }
                    switch (msg.getDetails()) {
                        case "Continue Transaction and Print Advice":
                            //print advice
                            //reset the things and back to main menu
                            AdvicePrinterMBox.send(new Msg(id, mbox, Msg.Type.Print, selectedAcc + "_" + transaction + "_" + transferAcc + "_" + amountTyped + "_" + status));
                            BuzzerMBox.send(new Msg(id, mbox, Msg.Type.Alert, "Printed advice can be collected!"));

                            halfRest();
                            touchDisplayMBox.send(new Msg(id, touchDisplayMBox, Msg.Type.TD_UpdateDisplay, "MainMenu"));
                            break;

                        case "Continue Transaction":
                            //reset the things and back to main menu
                            halfRest();
                            touchDisplayMBox.send(new Msg(id, touchDisplayMBox, Msg.Type.TD_UpdateDisplay, "MainMenu"));
                            break;

                        case "End Transaction and Print Advice":
                            //print advice
                            //eject card
                            AdvicePrinterMBox.send(new Msg(id, mbox, Msg.Type.Print, selectedAcc + "_" + transaction + "_" + transferAcc + "_" + amountTyped + "_" + status));
                            BuzzerMBox.send(new Msg(id, mbox, Msg.Type.Alert, "Printed advice can be collected!"));
                            cardReaderMBox.send(new Msg(id, mbox, Msg.Type.CR_EjectCard, ""));
                            //should be a screen showing thank you first
                            allReset();        //if transaction canceled, reset pin variable
                            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "Welcome_" + denom100 + " " + denom500 + " " + denom1000 + "/" + malfunctions));
                            break;

                        case "End Transaction":
                            //eject card
                            cardReaderMBox.send(new Msg(id, mbox, Msg.Type.CR_EjectCard, ""));
                            //should be a screen showing thank you first
                            allReset();        //if transaction canceled, reset pin variable
                            touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.TD_UpdateDisplay, "Welcome_" + denom100 + " " + denom500 + " " + denom1000 + "/" + malfunctions));
                            break;

                        default:
                            break;
                    }
                    break;
            }

        }
    } // processMouseClicked

    //for end transaction use
    private void allReset() {
        loggedIn = false;
        cardNum = "";
        pin = "";
        errorCount = 0;
        selectedAcc = "";
        transaction = "";
        transferAcc = "";
        amountTyped = "";
        getPin = false;
        getAmount = false;
        denomsToChange = "";
    }

    //for continue transaction and return to main menu use
    private void halfRest() {
        transaction = "";
        transferAcc = "";
        amountTyped = "";
        getPin = false;
        getAmount = false;
        denomsToChange = "";
    }

    private String denomDispenseCalculate(String amount) {
        String denoms100 = "0";
        String denoms500 = "0";
        String denoms1000 = "0";
        int amountWithdraw = Integer.parseInt(amount);
        if (denom1000 > 0 && amountWithdraw > 0) {
            if (denom1000 * 1000 > amountWithdraw) {
                denoms1000 = (amountWithdraw / 1000) + "";
                amountWithdraw -= Integer.parseInt(denoms1000) * 1000;
            } else {
                denoms1000 = denom1000 + "";
                amountWithdraw -= denom1000 * 1000;
            }
        }
        if (denom500 > 0 && amountWithdraw > 0) {
            if (denom500 * 500 > amountWithdraw) {
                denoms500 = (amountWithdraw / 500) + "";
                amountWithdraw -= Integer.parseInt(denoms500) * 500;
            } else {
                denoms500 = denom500 + "";
                amountWithdraw -= denom500 * 500;
            }
        }
        if (denom100 > 0 && amountWithdraw > 0) {
            if (denom100 * 100 > amountWithdraw) {
                denoms100 = (amountWithdraw / 100) + "";
                amountWithdraw -= Integer.parseInt(denoms100) * 100;
            } else {
                denoms100 = denom100 + "";
                amountWithdraw -= denom100 * 100;
            }
        }
        return "" + denoms100 + " " + denoms500 + " " + denoms1000;
    }

    private void updateDenomsInventory(String denoms, boolean increase) {
        StringTokenizer tokens = new StringTokenizer(denoms);
        int count = 0;
        if (increase) {
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                if (count % 3 == 0) {
                    denom100 += Integer.parseInt(token);
                } else if (count % 3 == 1) {
                    denom500 += Integer.parseInt(token);
                } else if (count % 3 == 2) {
                    denom1000 += Integer.parseInt(token);
                }
                count++;
            }
        } else {
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                if (count % 3 == 0) {
                    denom100 -= Integer.parseInt(token);
                } else if (count % 3 == 1) {
                    denom500 -= Integer.parseInt(token);
                } else if (count % 3 == 2) {
                    denom1000 -= Integer.parseInt(token);
                }
                count++;
            }
        }
        DispenserSlotMBox.send(new Msg(id, mbox, Msg.Type.DenomsInventoryCheck, ""));
    }

    private void notOperate() {
        AdvicePrinterMBox.send(new Msg(id, mbox, Msg.Type.Shutdown, ""));
        bamsThreadMBox.send(new Msg(id, mbox, Msg.Type.Shutdown, ""));
        BuzzerMBox.send(new Msg(id, mbox, Msg.Type.Shutdown, ""));
        cardReaderMBox.send(new Msg(id, mbox, Msg.Type.Shutdown, ""));
        DepositSlotMBox.send(new Msg(id, mbox, Msg.Type.Shutdown, ""));
        DispenserSlotMBox.send(new Msg(id, mbox, Msg.Type.Shutdown, ""));
        keypadMBox.send(new Msg(id, mbox, Msg.Type.Shutdown, ""));
    }

    private void HWreset() {
        AdvicePrinterMBox.send(new Msg(id, mbox, Msg.Type.Reset, ""));
        bamsThreadMBox.send(new Msg(id, mbox, Msg.Type.Reset, ""));
        BuzzerMBox.send(new Msg(id, mbox, Msg.Type.Reset, ""));
        cardReaderMBox.send(new Msg(id, mbox, Msg.Type.Reset, ""));
        DepositSlotMBox.send(new Msg(id, mbox, Msg.Type.Reset, ""));
        DispenserSlotMBox.send(new Msg(id, mbox, Msg.Type.Reset, ""));
        keypadMBox.send(new Msg(id, mbox, Msg.Type.Reset, ""));
        touchDisplayMBox.send(new Msg(id, mbox, Msg.Type.Reset, ""));
    }
}
