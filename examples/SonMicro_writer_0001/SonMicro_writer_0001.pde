
/*
 SonMicro RFID Reader example
 Language: Processing
 
 This sketch uses the SonMicroReader library to demonstrate how to read from
 and write to Mifare RFID tags using the SonMicro reader.
 
 created 12 March 2008
 by Tom Igoe, Jørn Knutsen, Einar Marinussen, and Timo Arnall
 modified 4 March 2009
 by Tom Igoe
 
 Many good  ideas based on Xbee API library
 by Rob Faludi and Daniel Shiffman
 http://www.faludi.com
 http://www.shiffman.net 
 */


// import libraries:
import processing.serial.*;
import sonMicroReader.*;

String tagID = "";        // the string for the tag ID
Serial myPort;            // serial port instance
SonMicroReader myReader;  // sonMicroReader instance

int  lastCommand = 0;        // last command sent
int lastTagType = 0;         // last tag type received
int lastPacketLength = 0;    // last packet length received
String lastTag = null;       // last tag ID received
int lastErrorCode = 0;       // last error code received
int[] lastResponse = null;   // last response from the reader (raw data)
int lastAntennaPower = 0;    // last antenna power received
int lastChecksum = 0;        // last checksum received

int fontHeight = 14;         // font height for the text onscreen
String message = null;       // message read from tag
String outputString = "Hello world!";    // string to write to tag



void setup() {
  // set window size:
  size(600,400);
  // list all the serial ports:
  println(Serial.list());

  // based on the list of serial ports printed from the 
  // previous command, change the 0 to your port's number:
  String portnum = Serial.list()[2];
  // initialize the serial port. default data rate for
  // the SM130 reader is 19200:
  myPort = new Serial(this, portnum, 19200);
  // initialize the reader instance:
  myReader = new SonMicroReader(this,myPort);
  myReader.start();

  // create a font with the second font available to the system:
  PFont myFont = createFont(PFont.list()[2], fontHeight);
  textFont(myFont);
  // create the command buttons:
  makeButtons();
}

void draw() {
  // clear the screen:
  background(0);  
  // draw the command buttons:
  drawButtons(); 
  // draw the output fields:
  textAlign(LEFT);
  text("Command: " + hex(lastCommand, 2), 10, 30);
  text("Packet length: " +lastPacketLength, 10, 50);
  text("Antenna power: " + lastAntennaPower, 10, 70);
  text("Tag type: " + lastTagType, 10, 90);
  text("Tag string: " + lastTag, 10, 110);
  text("Error code: " + hex(lastErrorCode, 2), 10, 130);

  // print the hex values for all the bytes in the response:
  String responseString = "";
  if (lastResponse != null) {
    for (int b = 0; b < lastResponse.length; b++) {
      responseString += hex(lastResponse[b], 2) + " ";
    }
    // wrap the full text so it doesn't overflow the buttons
    // and make the screen all messy:
    text("Full response:\n" + responseString, 10, 150, 300, 200);
  }
  // print any error messages from the reader:
  text(myReader.getErrorMessage(), 10, 210);
  // if there's a message from the tag's memory,
  // print it:
  if (message != null) {
    text("message read from tag:\n" + message, 10, 230); 
  }
  
  // print the output message:
  text("type your message to write to tag:\n",10, 300); 
  fill(0,0,150);
  text(outputString, 10, 320);
  
  // show the firmware version:
  fill(0);
  text("SonMicroReader version: " + myReader.version(), width - 300, height - 30);
}



/*  
 This function is called automatically whenever there's 
 a valid packet of data from the reader
 */
void sonMicroEvent(SonMicroReader myReader) {
  // get all the relevant data from the last data packet:
  lastCommand = myReader.getCommand();
  lastTagType = myReader.getTagType();
  lastPacketLength =  myReader.getPacketLength();
  lastTag = myReader.getTagString();
  println(lastTag);
  lastErrorCode = myReader.getErrorCode();
  lastAntennaPower = myReader.getAntennaPower();
  lastResponse = myReader.getSonMicroReading(); 
  lastChecksum = myReader.getCheckSum();
  
  // if the last command sent was a read block command:
  if (lastCommand == 0x86) {
    int[] inputString = myReader.getPayload();
    message = "";
    for (int c = 0; c < inputString.length; c++) {
      message += char(inputString[c]);
    }
    println(message);
  } 
  else {
    message = null;
  }
}

/*
  if one of the command buttons is pressed, figure out which one
  and take the appropriate action.
*/
void buttonPressed(RectButton thisButton) {
  // figure out which button this is in the ArrayList:
  int buttonNumber = buttons.indexOf(thisButton);

// do the right thing:
  switch (buttonNumber) {
  case 0: //  set antenna power
    if (myReader.getAntennaPower() < 1) {
      myReader.setAntennaPower(0x01);
    } 
    else {
      myReader.setAntennaPower(0x00);
    }
    break;
  case 1: // select tag
    myReader.selectTag();
    break;
  case 2:  // authenticate
    myReader.authenticate(0x10, 0xFF);
    break; 
  case 3:   // readblock
    myReader.readBlock(0x10);
    break;
  case 4:  // seek tag
    myReader.seekTag();
    break;
  case 5:  // write tag - must be 16 bytes or less
    myReader.writeBlock(0x10, outputString);
    outputString = "";
    break;
  case 6:  // get reader firmware version
    myReader.getFirmwareVersion();
    break; 
  }
}

/*
  If a key is typed, either add it to the output string
  or delete the string if it's a backspace:
*/
void keyTyped() {
  switch (key) {
  case ENTER:
    outputString = "\0";
    break;
  case BACKSPACE:  // delete
    outputString = "\0";
    break;
  default:

    if (outputString.length() < 16) {
      outputString += key;
    } 
    else {
      outputString = "output string can't be more than 16 characters"; 
    }
  }
}












