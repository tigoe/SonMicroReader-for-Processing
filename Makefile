INSTALL = $(HOME)/Documents/Processing/libraries/SonMicroReader/library/

CORE = /Applications/Processing.app/Contents/Resources/Java/core.jar
SERIAL = /Applications/Processing.app/Contents/Resources/Java/libraries/serial/library/serial.jar
JAVA_FLAGS = -source 1.5 -target 1.5 -d . -classpath $(CORE):$(SERIAL)
JAVAC = javac
JAR = jar

SRC=src
BUILD=SonMicroReader
LIB=library

$(LIB)/%.jar : $(BUILD)/%.class
	$(JAR) -cf $@ $<

$(BUILD)/%.class : $(SRC)/%.java
	$(JAVAC) $(JAVA_FLAGS) $<

all: $(BUILD)/SonMicroReader.class

jar: $(LIB)/SonMicroReader.jar

install: jar
	cp $(LIB)/SonMicroReader.jar $(INSTALL)
clean:
	rm -f $(BUILD)/SonMicroReader.class
	rm -f $(LIB)/SonMicroReader.jar
