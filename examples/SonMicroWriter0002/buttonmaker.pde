/*
  This file makes buttons for the screen. You can change the names of the buttons
 or the number of buttons just by changing the buttonNames list.
 
 based on the Processing Buttons example.
 */

/*
 initialize all the buttons
 */
void makeButtons() {
  // Define and create rectangle button
  for (int b = 0; b < buttonNames.length; b++) {
    // create a new button with the next name in the list: 
    Button thisButton = new Button(400, 30 +b*30, 150, 20, buttoncolor, highlight, buttonNames[b]);
    buttons.add(thisButton);
  }
}
/*
  draw all the buttons
 */
void drawButtons() {
  for (int b = 0; b < buttons.size(); b++) {
    // get this button from the Arraylist:
    Button thisButton = (Button)buttons.get(b);
    // update its pressed status:
    thisButton.update();
    // draw the button:
    thisButton.display();
  }
}

void mousePressed() {
  // iterate over the buttons, activate the one pressed
  for (int b = 0; b < buttons.size(); b++) {
    Button thisButton = (Button)buttons.get(b);
    if (thisButton.containsMouse()) {
      doButtonAction(thisButton);
    }
  }
}

/*
  if one of the command buttons is pressed, figure out which one
 and take the appropriate action.
 */
void doButtonAction(Button thisButton) {
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
    myReader.authenticate(0x04, 0xFF);
    break; 
  case 3:   // readblock
    myReader.readBlock(0x04);
    break;
  case 4:  // seek tag
    myReader.seekTag();
    break;
  case 5:  // write tag - must be 16 bytes or less
    myReader.writeBlock(0x04, outputString);
    outputString = "";
    break;
  case 6:  // get reader firmware version
    myReader.getFirmwareVersion();
    break;
  }
}

class Button {
  int x, y, w, h;                    // positions of the buttons
  color basecolor, highlightcolor;   // color and highlight color
  color currentcolor;                // current color of the button
  String name;

  // Constructor: sets all the initial values for each instance of the Button class
  Button(int thisX, int thisY, int thisW, int thisH, 
  color thisColor, color thisHighlight, String thisName) {
    x = thisX;
    y = thisY;
    h = thisH;
    w = thisW;
    basecolor = thisColor;
    highlightcolor = thisHighlight;
    currentcolor = basecolor;
    name = thisName;
  }

  // if the mouse is over the button, change the button's color:
  void update() {
    if (containsMouse()) {
      currentcolor = highlightcolor;
    } 
    else {    
      currentcolor = basecolor;
    }
  }
  
  // draw the button and its text:
  void display() {
    fill(currentcolor);
    rect(x, y, w, h);
    //put the name in the middle of the button:
    fill(0);
    textAlign(CENTER, CENTER);
    text(name, x+w/2, y+h/2);
  }

  // check to see if the mouse position is inside
  // the bounds of the rectangle:
  boolean containsMouse() {
    if (mouseX >= x && mouseX <= x+w && 
      mouseY >= y && mouseY <= y+h) {
      return true;
    } 
    else {
      return false;
    }
  }
}

