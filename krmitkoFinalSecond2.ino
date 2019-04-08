#include <Servo.h>
#include <Wire.h>
#include <TimeLib.h>
#include <DS1307RTC.h>
#include <DS3231.h>
#include <HX711.h>
#include <SoftwareSerial.h>
#include <EEPROM.h>

// HX711.DOUT  - pin #A1
// HX711.PD_SCK - pin #A0
HX711 scale(A1, A0);

Servo myservo;

DS3231 clock;
RTCDateTime dt;

//BLE HM-10 .TX- pin #16
//BLE HM-10 .RX- pin #10
SoftwareSerial bluetooth(16, 10);

int pos = 0;
char order = ' ';

float calibration_factor = 8350.f;
float dish_weight = 17;

byte feedT1 = -1; //EEPROM 1
byte feedT2 = -1; //EEPROM 2
byte feedT3 = -1; //EEPROM 3
byte feedT4 = -1; //EEPROM 4
byte feedT5 = -1; //EEPROM 5

byte feedQaunt = -1; //EEPROM 0

int Hrs = 0;
int Min = 0;
int Sec = 0;
int lastTime = 0;

void setup() {
  bluetooth.begin(9600);
  Serial.begin(9600);
  scale.set_scale(calibration_factor);
  scale.tare();

  clock.begin();
  clock.setDateTime(__DATE__, __TIME__);

  readEEPROM();
}

void loop() {
 // writeEEPROMVal();
 // calibration();
  checkFeedTime();
  blueToothListener();

}

void blueToothListener() {
  byte bluetoothData;

  if (bluetooth.available() > 0) {
    bluetoothData = bluetooth.read();
    Serial.println(bluetoothData);

    if (bluetoothData == 'f') {
      dumpFeed();
    } else if (bluetoothData == 's') {
      char myBuffer[5];
      int i = 1;
      bluetoothData = bluetooth.read();
      myBuffer[0] = bluetoothData;
      while (bluetooth.available() > 0) {
        delay(10);
        bluetoothData = bluetooth.read();
        myBuffer[i] = bluetoothData;
        i += 1;
      }
      saveSettings(myBuffer);
      readEEPROM();
    } else if (bluetoothData == 'w') {
      writeEEPROMVal();
    } else if (bluetoothData == 'x') {
      factoryReset();
    } else if (bluetoothData == 't') {
      bluetooth.println("Joujou");
    } else {
      Serial.println("!!! Unknow order !!!");
    }
  }
  delay(500);
}

void saveSettings(char myBuffer[]) {
  int memIndex = checkZero((int)myBuffer[0] - 48);
  int val;
  val += checkZero((int)myBuffer[3] - 48);
  val += checkZero(((int)myBuffer[2] - 48) * 10);
  val += checkZero(((int)myBuffer[1] - 48) * 100);
  Serial.print("memIndex = "); Serial.print(memIndex);
  Serial.print(" || value = "); Serial.println(val);
  EEPROM.write(memIndex, val);
}

void checkFeedTime() {
  dt = clock.getDateTime();
  if (lastTime != dt.second) {
    lastTime = dt.second;
    Serial.print(lastTime); Serial.println("");

    if (isTimeToEat() == true) {
      if (isEnoughFeed() == false) {
        fillDish();
      }
    }
  }
}

bool isEnoughFeed() {
  //scale.power_up();
  delay(5000);
  float weight = (scale.get_units());
  Serial.print("weight: "); Serial.println(weight);
  float reqWeight = (feedQaunt / 2) + dish_weight;
  if (weight < reqWeight) {
    Serial.print("*** NOT ENOUGH FEED!! ***"); Serial.println("");
    //scale.power_down();
    return false;
  }
  Serial.print("*** ENOUGH FEED - NO MORE FEED!! ***"); Serial.println("");
  //scale.power_down();
  return true;
}

bool isTimeToEat() {
  if ((feedT1 > 0) && (lastTime == feedT1) ) {
    Serial.print("*** EAT TIME!! ***"); Serial.println("");
    return true;
  }

  if ((feedT2 > 0) && (lastTime == feedT2) ) {
    Serial.print("*** EAT TIME!! ***"); Serial.println("");
    return true;
  }

  if ((feedT3 > 0) && (lastTime == feedT3) ) {
    Serial.print("*** EAT TIME!! ***"); Serial.println("");
    return true;
  }

  if ((feedT4 > 0) && (lastTime == feedT4) ) {
    Serial.print("*** EAT TIME!! ***"); Serial.println("");
    return true;
  }

  if ((feedT5 > 0) && (lastTime == feedT5) ) {
    Serial.print("*** EAT TIME!! ***"); Serial.println("");
    return true;
  }
  return false;
}

void calibration() {
  scale.set_scale(calibration_factor);

  Serial.print("Reading: ");
  Serial.print(scale.get_units(), 3);
  Serial.print(" g | ");
  Serial.print(" calibration_factor: ");
  Serial.print(calibration_factor);
  Serial.println();

  if (Serial.available() > 0) {
    order = Serial.read();
  }

  switch (order)
  {
    case '1':
      calibration_factor -= 10;
      break;
    case '2':
      calibration_factor -= 100;
      break;
    case '3':
      calibration_factor -= 1000;
      break;
    case '4':
      calibration_factor += 10;
      break;
    case '5':
      calibration_factor += 100;
      break;
    case '6':
      calibration_factor += 1000;
      break;
    case '7':
      calibration_factor -= 10000;
      break;
    case '8':
      Serial.println(calibration_factor);
      break;
    case '9':
      calibration_factor += 10000;
      break;
    case 'r':
      scale.tare();
      break;
  }
  order = -1;
}

void fillDish() {
  //scale.power_up();
  delay(5000);
  float weight = (scale.get_units());
  int aux = 0;
  float reqWeight2 = feedQaunt + dish_weight;
  Serial.println(weight);
  Serial.println(reqWeight2);
  while (weight < reqWeight2) {
    Serial.println("in");
    Serial.print("weight: ");
    Serial.print(weight);
    dumpFeed();
    weight = (scale.get_units());
    delay(1000);
    aux++;
    if (aux == 10) {
      Serial.println("Servo feeding time out");
      break;
    }
  }
  //scale.power_down();
  Serial.println("Dish is full");
}

void dumpFeed() {
  Serial.println("-> Dumping feed");
  servoPos();
  delay(1000);
  servoNeg();
}

void servoPos() {
  myservo.attach(9);
  for (pos = -10; pos <= 200; pos += 1) {
    // in steps of 1 degree
    myservo.write(pos);
    delay(15);
  }
  myservo.detach();
}

void servoNeg() {
  myservo.attach(9);
  for (pos = 200; pos >= -10; pos -= 1) {
    myservo.write(pos);
    delay(15);
  }
  myservo.detach();
}

void factoryReset() {
  EEPROM.write(0, 50); //feedQaunt

  EEPROM.write(1, 7); //feedT1
  EEPROM.write(2, 12); //feedT2
  EEPROM.write(3, 18); //feedT3
  EEPROM.write(4, 0); //feedT4
  EEPROM.write(5, 0); //feedT5
  Serial.println("-> Factory reset");
  readEEPROM();
}

void readEEPROM() {
  feedQaunt = EEPROM.read(0);
  feedT1 = EEPROM.read(1);
  feedT2 = EEPROM.read(2);
  feedT3 = EEPROM.read(3);
  feedT4 = EEPROM.read(4);
  feedT5 = EEPROM.read(5);

  writeEEPROMVal();
}

void writeEEPROMVal() {
  Serial.println("*** EEPROM Values ***");
  Serial.println(feedQaunt);
  Serial.println(feedT1);
  Serial.println(feedT2);
  Serial.println(feedT3);
  Serial.println(feedT4);
  Serial.println(feedT5);
  Serial.println("*** --- ***");
  Serial.println();
}

int checkZero(int myValue) {
  if (myValue == (-48)) {
    return 0;
  }
  return myValue;
}
