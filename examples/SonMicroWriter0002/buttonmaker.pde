/*
  This file makes buttons for the screen. You can change the names of the buttons
  or the number of buttons just by changing the buttonNames list.
  
  based on the Processing Buttons example.
*/

color currentcolor;        // current button color
ArrayList buttons = new ArrayList();  // list of buttons

// the buttons themselves:
String[]  buttonNames = { 
  "antenna power", "select tag", "authenticate", "read block", "seek Tag",
  "write block", "firmware version"
};

boolean locked = false; // whether the buttons are locked

void makeButtons() {
  // set up the base color of the buttons:
  color baseColor = color(220, 220, 255);
  currentcolor = baseColor;

  // Define and create rectangle button
  for (int b = 0; b < buttonNames.length; b++) {
    // create a new button with the next name in the list: 
    color buttoncolor = color(180, 180, 220);
    color highlight = color(102, 102, 180); 
    RectButton thisButton = new RectButton(400, 30 +b*30, 150, 20, buttoncolor, highlight, buttonNames[b]);
    buttons.add(thisButton);
  }

}
/*
  draw all the buttons
*/
void drawButtons() {
  background(currentcolor);
  stroke(255);
  update(mouseX, mouseY);
  for (int b = 0; b < buttons.size(); b++) {
    RectButton thisButton = (RectButton)buttons.get(b);
    thisButton.display();
  }
}

void update(int x, int y) {
  if (locked == false) {  
    for (int b = 0; b < buttons.size(); b++) {
      RectButton thisButton = (RectButton)buttons.get(b);
      thisButton.update();
    }
  } 
  else {
    locked = false;
  }
}


void mouseReleased() {
  // iterate over the buttons, activate the one pressed
  for (int b = 0; b < buttons.size(); b++) {
    RectButton thisButton = (RectButton)buttons.get(b);
    if (thisButton.pressed()) {
      buttonPressed(thisButton);
    }
  }
}


class Button {
  int x, y, w, h;
  color basecolor, highlightcolor;
  color currentcolor;
  boolean over = false;
  boolean pressed = false;   

  void update()  {
    if(over()) {
      currentcolor = highlightcolor;
    } 
    else {
      currentcolor = basecolor;
    }
  }

  boolean pressed() {
    if (over) {
      locked = true;
      return true;
    } 
    else {
      locked = false;
      return false;
    }    
  }


  boolean over() 
  { 
    return true; 
  }

  boolean overRect(int x, int y, int width, int height)  {
    if (mouseX >= x && mouseX <= x+width && 
      mouseY >= y && mouseY <= y+height) {
      return true;
    } 
    else {
      return false;
    }
  }

}

class RectButton extends Button {
  String name;

  RectButton(int _x, int _y, int _w, int _h, color _color, color _highlight, String _name) {
    x = _x;
    y = _y;
    h = _h;
    w = _w;
    basecolor = _color;
    highlightcolor = _highlight;
    currentcolor = basecolor;
    name = _name;
  }

  boolean over()  {
    if( overRect(x, y, w, h) ) {
      over = true;
      return true;
    } 
    else {
      over = false;
      return false;
    }
  }

  void display() {
    fill(currentcolor);
    rect(x, y, w, h);
    //put the name in the middle of the button:
    fill(0);
    textAlign(CENTER, CENTER);
    text(name, x+w/2, y+h/2);
  }
}





