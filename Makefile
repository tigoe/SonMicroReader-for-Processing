INSTALL = $(HOME)/Documents/Processing/libraries/SonMicroReader/library/
INSTALL_SRC = $(HOME)/Documents/Processing/libraries/SonMicroReader/src/

CORE = /Applications/Processing.app/Contents/Resources/Java/core.jar
SERIAL = /Applications/Processing.app/Contents/Resources/Java/modes/java/libraries/serial/library/serial.jar
JAVA_FLAGS = -source 1.5 -target 1.5 -d . -classpath $(CORE):$(SERIAL)
JAVAC = javac
JAR = jar

SRC=src
BUILD=sonMicroReader
LIB=library

$(LIB)/%.jar : $(BUILD)/%.class
	$(JAR) -cf $@ $<

$(BUILD)/%.class : $(SRC)/%.java
	$(JAVAC) $(JAVA_FLAGS) $<

all: $(BUILD)/SonMicroReader.class

jar: $(LIB)/SonMicroReader.jar

install: jar
	cp $(LIB)/SonMicroReader.jar $(INSTALL)
	cp $(SRC)/SonMicroReader.java $(INSTALL_SRC)
clean:
	rm -f $(BUILD)/SonMicroReader.class
	rm -f $(LIB)/SonMicroReader.jar
