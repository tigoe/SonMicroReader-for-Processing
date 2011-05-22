
/** 
    SonMicroReader library

    Reads and writes to Mifare RFID tags
    Using a SonMicro SM130 reader.

    created 12 March 2008
    by Tom Igoe, J¿rn Knutsen, Einar Marinussen, and Timo Arnall
    modified 19 May 2011
    by Tom Igoe with help from Brian Jepson

    Many good  ideas for this came from Xbee API library
    by Rob Faludi and Daniel Shiffman
    http://www.faludi.com
    http://www.shiffman.net       
 */

package sonMicroReader;

import java.lang.reflect.Method;
import processing.core.PApplet;
import processing.serial.Serial;

public class SonMicroReader extends Thread {
    PApplet parent;            // the parent sketch (applet)
    Serial port;               // We'll need the parent PApplet to make a Serial object for us
    Method sonMicroMethod;     // used to generate a sonMicroEvent

    private boolean running;   // is the thread running? 
    boolean available = false; // is new data available?

    int[] data;                // array of new data

    static boolean DEBUG = false;

    private int command = 0;               // received command, from the packet    
    private int packetLength = 0;          // length of the response, from the packet
    private int checkSum = 0;              // checksum value received
    private int[] payload = null;          // data received 
    private int tagNumber = 0;    	       // tag number 
    private int tagType = 0;               // the type of tag
    private int errorCode = 0;             // error code from some commands
    private String errorMsg = "";          // descriptive error message
    private int antennaPower = 1;          // antenna power level
    private int versionNumber = 003;       // version of the library
 
    // Constructor, create the thread. It is not running by default
    public SonMicroReader(PApplet p, Serial s) {
        running = false;
        parent = p;
        port = s;

        try {
            sonMicroMethod = parent.getClass().getMethod("sonMicroEvent", new Class[] { 
                    SonMicroReader.class                                                                                                             }
            );
        } 
        catch (Exception e) {
            errorMsg = "You forgot to implement the sonMicroEvent() method.";
            System.out.println(errorMsg);
        }
    }

    public void start() {
        running = true;
        super.start();
    }

    public void run () {
        while (running){
            // check for new data as long as the thread is running:
            data = getData();

            if (sonMicroMethod != null && data != null) {
                // get the useful parts from the data:
                // get the packet length:
                packetLength = data[2];
                // get the received command:
                command = data[3];
                // if packet length is 2, you have only a command and an error code:
                if (packetLength < 3) {
                    errorCode = data[4];
                    payload = null;
                    tagType = 0;
                    tagNumber = 0;
                } 
                else {
                    errorCode = 0; 
                    errorMsg = "";
                }
                // packet length > 6 indicates successful read of serial number
                // or data read:
                if (packetLength > 5) {
                    // get the tag type:
                    tagType = data[4];
                    // get the payload:
                    payload = new int[1];

                    payload[0] = data[5];
                    for (int i = 6; i < data.length-1; i++) {
                        payload = parent.append(payload, data[i]);
                    }
                }
                // get the checksum:
                checkSum = data[data.length-1];

                // some messages generate error codes.  Return them here.
                switch (command) {
                // reset produces nothing as of version 2.8 of the SM130 firmware:
                case 0x80:
                	break;
                // firmware version only produces the firmware version:
                case 0x81: 
                    errorCode = 0;
                    tagType = 0;
                    tagNumber = 0;
                    errorMsg = "Firmware version: ";
                     // if you got a good payload, it's a tag number:
                    if (payload != null) {
 	                    for (int i = 4; i < data.length-1; i++) {
 	                    	errorMsg += (char)data[i];
	                    }
                    }
                    break;
                case 0x82:  // seekTag    
                    if (errorCode == 0x55) {
                        errorMsg = "Reader error: Antenna is off";
                    }
                    if (errorCode == 0x4C) {
                        errorMsg = "Command in Progress";
                    }
                    // if you got a good payload, it's a tag number:
                    if (payload != null) {
 	                    for (int i = 0; i < payload.length; i++) {
 	                    	tagNumber = tagNumber << 8;
		                    tagNumber += payload[payload.length-1-i]; 
	                    }
                    }
                    break;
                case 0x83:  // selectTag
                    switch(errorCode) {
                    case 0x4E:
                        errorMsg = " Reader error: No Tag present";
                        break;
                    case 0x55:
                        errorMsg = " Reader error: Access failed because RF field is off";
                        break;
                    }
                    // if you got a good payload, it's a tag number:
                    if (payload != null) {
                         for (int i = 0; i < payload.length; i++) {
 	                    	tagNumber = tagNumber << 8;
		                    tagNumber += payload[payload.length-1-i]; 
	                    }
	                }
                    break;
                case 0x85:  //authenticate
                    switch(errorCode) {
                    case 0x45:
                        errorMsg = " Reader error: Invalid key format in EEPROM";
                        break;
                    case 0x4C:
                        //errorMsg = "Login successful";
                        break;
                    case 0x4E:
                        errorMsg = " Reader error: No tag present, or login failed";
                        break;
                    case 0x55:
                        errorMsg = " Reader error: login failed";
                        break;
                    }
                    break;
                case 0x86:  //read block
                    switch(errorCode) {
                    case 00:
                        // good read
                        break;
                    case 0x4E:
                        errorMsg = "Reader error: no tag present";
                        break;
                    case 0x46:
                        errorMsg = "Reader error: read failed";
                        break;
                    }
                    break; 
                case 0x87:  //read value block
                    switch(errorCode) {
                    case 0x4E:
                        errorMsg = "Reader error: no tag present";
                        break;
                    case 0x46:
                        errorMsg = "Reader error: read failed";
                        break;
                    case 0x49:
                        errorMsg = "Reader error: you tried to read a block number that isn't a value block";
                        break;
                    }
                    break; 
                case 0x89:  //write  block
                    switch(errorCode) {
                    case 0x55:
                        errorMsg = "Reader error: data read doesn't match data write";
                        break;
                    case 0x58:
                        errorMsg = "Reader error: the block you tried to write to is protected";
                        break;
                    case 0x4E:
                        errorMsg = "Reader error: no tag present";
                        break;
                    case 0x46:
                        errorMsg = "Reader error: write failed";
                        break;
                    }
                    break; 
                case 0x8A:  //write value block
                    switch(errorCode) {
                    case 0x4E:
                        errorMsg = "Reader error: no tag present";
                        break;
                    case 0x46:
                        errorMsg = "Reader error: read failed during verification";
                        break;
                    case 0x49:
                        errorMsg = "Reader error: you tried to write to a block number that isn't a value block";
                        break;
                    }
                    break; 
                case 0x8B:  //write  4 byte block
                    switch(errorCode) {
                    case 0x55:
                        errorMsg = "Reader error: data read doesn't match data write";
                        break;
                    case 0x58:
                        errorMsg = "Reader error: the block you tried to write to is protected";
                        break;
                    case 0x4E:
                        errorMsg = "Reader error: no tag present";
                        break;
                    case 0x46:
                        errorMsg = "Reader error: write failed";
                        break;
                    }
                    break; 
                case 0x8C:  //write master key
                    switch(errorCode) {
                    case 0x4E:
                        errorMsg = "Reader error: write master key failed";
                        break;
                    case 0x4C:
                        // success!
                        break;
                    }
                    break; 
                case 0x90:     // set antenna status
                    errorCode = 0;
                    antennaPower = data[4];
                    break;
                case 0x94:  //set baud rate
                    switch(errorCode) {
                    case 0x4E:
                        errorMsg = "Reader error: set baud rate failed";
                        break;
                    case 0x4C:
                        // success!
                        errorMsg = "Baud rate set";
                        break;
                    }
                    break; 
                }

                // generate a sonMicroEvent:
                try {
                    sonMicroMethod.invoke(parent, new Object[] {this});
                    available = true;  
                } 
                catch (Exception e) {
                    System.out.println("Problem with sonMicroEvent()");
                    e.printStackTrace();
                    sonMicroMethod = null;
                }
            }

            try {
                sleep(1);  // Should we sleep?
            } 
            catch (Exception e) {
                // Nothing for now. We'll get here if interrupt() is called
            }
        }
    }

    // Our method that quits the thread
    public void quit()
    {
        System.out.println("Quitting."); 
        running = false;  // Setting running to false ends the loop in run()
        super.interrupt();
    }
    /**
     *
     *  @return available   whether or not there's an availble data packet
     */
    public boolean available() {
        return available;
    }

    /**
     *   Used mainly for debugging, this method returns all the bytes that come in from the reader
     *  @return data   an array of all the bytes from the reader in response to a given command
     */
    public int[] getSonMicroReading() {
        return data;
    }

    /**
     *   Waits for data to return from sent commands, and parses the data
     *   @return    responseArray   the array of all the bytes from the response
     */
    private int[] getData() {
        int inByte = -1;
        int[] responseArray = new int[1];
        int calculatedCheckSum = 0;     // checksum value calculated locally

        port.clear(); // flush the buffer so that no old data comes in

        while (port.available() < 1) {
            ; // do nothing while we wait for the buffer to fill
        }

        // read  bytes until you get an FF:
        while (inByte != 0xFF) {
            inByte = port.read();   
        }

        // you got an 0Xff, so clear your variables:
        responseArray[0] = inByte;
        calculatedCheckSum = 0;

        // wait until you have the header and the reserved byte:
        while (port.available() < 2) {
            ; // do nothing
        }
        // get the reserved byte:
        inByte = port.read();
        responseArray = parent.append(responseArray, inByte);
        // get the length:
        int length = port.read();
        responseArray = parent.append(responseArray, length);


        // save bytes until you have the whole packet
        while (port.available() <= length)  {
            ;// wait
        }

        // collect all the bytes of the payload and the checksum:
        // read the rest of the bytes, append to the response array
        while (port.available() > 0) {
            int thisByte = port.read();
            responseArray = parent.append(responseArray, thisByte);
        }   
        // calculate checksum:
        calculatedCheckSum = 0;
        for (int b = 1; b < responseArray.length-1; b++) {
            calculatedCheckSum += responseArray[b];
        }

        // get the last byte of the array, that's the checksum:
        int returnedCheckSum = responseArray[responseArray.length-1];
        calculatedCheckSum = calculatedCheckSum % 256;
		
		/*
		// Checksum was causing bad reads on seek when tag was already in field
        if (calculatedCheckSum != returnedCheckSum) {
            // checksum is bad:
            responseArray = null;
            errorMsg = "bad checksum";
            if (DEBUG) parent.println(responseArray);
        }
        */
        return responseArray;  
    }
    /**
     * @return  command the command as received by the reader
     * 
     */
    public int getCommand() {
        return command;
    }
    /**
     * @return  packetLength the length of the payload as reported by the reader
     * 
     */
    public int getPacketLength() {
        return packetLength;
    }
    /**
     * @return  checkSum the checksum from the reader
     * 
     */
    public int getCheckSum() {
        return checkSum;
    }
    /**
     * @return  payload the array of bytes in the payload of the message
     * 
     */
    public int[] getPayload() {
        return payload;
    }

    /**
     * @return  tagNumber the array of bytes in the tag number of the message
     * 
     */
    public long getTagNumber() {
        return tagNumber;
    }
    /**
     * @return  tagString the ASCII-encoded hex string of the tag number of the message
     * 
     */
    public String getTagString() {
        String tagID = null;
        if (tagNumber != 0) {
            tagID = parent.hex(tagNumber, 8);
        } 
        return tagID;
    }

    /**
     * Assuming there's a valid tag read, this returns the tag type, as follows:
     *
     * 0x01 Mifare Ultralight 
     * 0x02 Mifare Standard 1K 
     * 0x03 Mifare Classic 4K 
     * 0xFF Unknown Tag type 
     *
     * @return  tagType an integer for the tag type
     * 
     * 
     */
    public int getTagType() {
        return tagType;
    }

    /**
     * Resets the reader by sending command 0x80
     *
     */
    public void reset() {
        this.sendCommand(0x80);
    }

    /**
     * gets the firmware of the reader by sending command 0x81
     *
     */
    public void getFirmwareVersion() {
        this.sendCommand(0x81);
    }
    
    /**
     * Gets the error code returned by some messages
     * Error codes as follows:
     * Select Tag (command 0x83)
     * 
     * 0x4E 'N' No Tag present. 
     * 0x55 'U' Access failed due to RF Field is OFF
     * 
     * Seek for Tag (command 0x82)
     * 0x4C  'L' Command in progress. 
     * 0x55 'U' Command in progress but RF Field is OFF  
     * 
     * Authenticate (command 0x85)
     * 0x4C 'L' Login Successful 
     * 0x4E 'N' No Tag present or Login Failed 
     * 0x55 'U' Login Failed 
     * 0x45 'E' Invalid key format in E2PROM 
     * 
     * Read Block (command 0x86) 
     * 0x4E 'N' No Tag present  
     * 0x46 'F' Read Failed 
     * 
     * Read Value Block (command 0x87) 
     * 0x4E 'N' No Tag present  
     * 0x49 'I' Invalid Value Block 
     * 0x46 'F' Read Failed 
     * 
     * Write Block (command 0x89)
     * 0x55 'U' Read after write failed
     * 0x58 'X' Unable to Read after write 
     * 0x4E 'N' No Tag present 
     * 0x46 'F' Write Failed 
     * 
     * 
     * Write Value Block (command 0x8A)
     * 0x4E 'N' No Tag present  
     * 0x49 'I' Invalid Value Block. The block was not in the  
     *          proper value format when read back. This could 
     *          be because there was an error in writing   
     * 0x46 'F' Read Failed during verification 
     * <p>
     * Write 4-byte block (command 0x8B)
     * 0x55 'U' Read after write failed
     * 0x58 'X' Unable to Read after write 
     * 0x4E 'N' No Tag present  
     * 0x46 'F' Write Failed 
     * 
     * Write Master Key (command 0x8C)
     * 0x4C 'L'  Write Master key successful 
     * 0x4E 'N'  Write Master key fail  
     * 
     * Set Antenna Power (command 0x90)
     * 0x00 RF Field switched Off 
     * 0x01 RF Field switched On  
     * 
     * 
     * Set Baud Rate (command 0x94)
     * 0x4C 'L' Change of Baud rate successful  
     * 0x4E 'N' Change of Baud rate failed  
     * @return  an int for the error code.
     */

    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 
     * @return  errorMsg    the descriptive error message
     */
    public String getErrorMessage() {
        return errorMsg;
    }
    /**
     * 
     * @return  antennaPower    the antenna power.  0x00 is off, anything else is on
     */
    public int getAntennaPower() {
        return antennaPower;
    }
    /**
     * Sends the Seek Tag command (0x83)
     *
     */
    public void seekTag() {
        this.sendCommand(0x82);
    }

    /**
     * Sends the Select Tag command (0x82)
     *
     */
    public void selectTag() {
        this.sendCommand(0x83);
    }

    /**
     * Sets the antenna power.  0x00 is off, anything else is on
     * @param level the antenna power level
     */
    public void setAntennaPower(int level) {
        int[] thisCommand = { 
                0x90, level                                                    };
        this.sendCommand(thisCommand);
    }

    /**
     * Sends the sleep command (0x96)
     *
     */
    public void sleep() {
        this.sendCommand(0x96);
    }

    /**
     * sets the baud rate. Possible baud rates:
     * <P>
     * 9600 bps  
     * 19200 bps  
     * 38400 bps  
     * 57600 bps  
     * 115200 bps  
     * 
     * @param baudRate  The data rate. 
     */
    public void setBaudRate(int baudRate) {

        int dataRate = 00;

        switch (baudRate){
        case 9600:
            dataRate = 0x00;
            break;
        case 19200:
            dataRate = 0x01;
            break;
        case 38400:
            dataRate = 0x02;
            break;
        case 57600:
            dataRate = 0x03;
            break;
        case 115200:
            dataRate = 0x04;
            break;
        }
        int[] thisCommand = {
                0x94, dataRate                                                        }; 

        sendCommand(thisCommand);
    }
    /**
     * Writes a 16-byte string to a block. If the string is less than 16 bytes
     * it fills the empty bytes with 0x00.  If more than 16 bytes, it generates
     * an error message.
     * 
     * You need to select and authenticate before you can read or write.
     * 
     * @param thisBlock     Block to write to
     * @param thisMessage   16-byte string to write
     */
    public void writeBlock(int thisBlock, String thisMessage) {
        // block needs to be broken into 16-byte sections
        if (thisMessage.length() > 16) {
            errorMsg = "You can't send more than 16 bytes with a writeBlock() command.";
        } 
        else {
            int[] thisCommand = {
                    0x89,thisBlock                                                                               }; 
            int thisByte;
            // make sure to write to all 16 bytes:
            for (int i = 0; i < 16; i++) {
                if (i < thisMessage.length()) {
                    thisByte = (int)thisMessage.charAt(i);
                } 
                else {
                    thisByte = 0;
                }
                thisCommand = parent.append(thisCommand, thisByte);
            } 

            sendCommand(thisCommand);
        }
    }
    /**
     * Writes a 4-byte string to a block. If the string is less than 16 bytes
     * it fills the empty bytes with 0x00.  If more than 4 bytes, it generates
     * an error message.
     * Used for Mifare ultralight tags
     * 
     * You need to select and authenticate before ou can read or write.
     * 
     * @param thisBlock     Block to write to
     * @param thisMessage   4-byte string to write
     */
    public void writeFourByteBlock(int thisBlock, String thisMessage) {
        // block needs to be broken into 4-byte sections
        if (thisMessage.length() > 4) {
            errorMsg = "You can't send more than 4 bytes with a writeFourByteBlock() command.";
        } 
        else {
            int[] thisCommand = {
                    0x8B,thisBlock                                                                               }; 
            int thisByte;
            // make sure to write to all 4 bytes:
            for (int i = 0; i < 4; i++) {
                if (i < thisMessage.length()) {
                    thisByte = (int)thisMessage.charAt(i);
                } 
                else {
                    thisByte = 0;
                }
                thisCommand = parent.append(thisCommand, thisByte);
            } 
            sendCommand(thisCommand);
        }
    }

    /**
     * Authenticates for reading and writing
     * You need to select and authenticate before ou can read or write.
     * 
     * @param thisBlock         Block to write to
     * @param authentication    Authentication type to use (see WriteMasterKey for more)
     */
     public void authenticate(int thisBlock) {
        int[] thisCommand = {
                0x85,thisBlock, 0xFF }; 
        sendCommand(thisCommand);

    }

    
    public void authenticate(int thisBlock, int authentication) {
        int[] thisCommand = {
                0x85,thisBlock, authentication }; 
        sendCommand(thisCommand);

    }
    
  public void authenticate(int thisBlock, int authentication, int[] thisKey) {
        int[] thisCommand = new int[thisKey.length + 3];
        thisCommand[0]= 0x85;
        thisCommand[1]= thisBlock;
        thisCommand[2]= authentication; 
        System.arraycopy(thisKey, 0, thisCommand, 2, thisKey.length);                  
        sendCommand(thisCommand);

    }

    
//authenticate(byte keyType, byte[] key)
    /**
     * Reads a block of data. You need to select and authenticate before ou can read or write.
     * You need to select and authenticate before ou can read or write.
     * @param thisBlock Block to read from
     */
    public void readBlock(int thisBlock) {
        int[] thisCommand = {
                0x86,thisBlock                                                                }; 
        sendCommand(thisCommand);
    }


    /**
     * Sends a single-byte command
     * 
     * @param thisCommand   a single-byte command to send
     */
    public void sendCommand(int thisCommand) {
        available = false;
        int[] byteArray = { 
                0xFF, 0x00, 0x01                                                                          };
        // calculate checksum:
        int myCheckSum = (thisCommand + byteArray[2]) % 256;

        byteArray = parent.append(byteArray, thisCommand);
        byteArray = parent.append(byteArray, myCheckSum);

        for (int c = 0; c < byteArray.length; c++) {
            port.write(byteArray[c]);
        }
    }

    /**
     * Sends a multi-byte command
     * @param thisCommand   An array of bytes to send
     */
    public void sendCommand(int[] thisCommand) {
        available = false;
        String commandSent = "";
        int[] byteArray = { 
                0xFF, 0x00                                                                          };
        // calculate checksum:
        int myCheckSum = thisCommand.length;
        // add the length to the array:
        byteArray = parent.append(byteArray, myCheckSum);
        // add the command bytes to the array:
        for (int i = 0; i < thisCommand.length; i++) {
            myCheckSum += (thisCommand[i]);
            byteArray = parent.append(byteArray, thisCommand[i]);
        }
        // finish calculating the checksum:
        myCheckSum = myCheckSum % 256;

        // append the checksum to the array:
        byteArray = parent.append(byteArray, myCheckSum);
        // send the bytes out the serial port:
        for (int c = 0; c < byteArray.length; c++) {
            port.write(byteArray[c]);
        }
    }
    /**
     * 
     * @return  versionNumber   the version number of the library
     */
    public int version() {
        return versionNumber;
    }
}



