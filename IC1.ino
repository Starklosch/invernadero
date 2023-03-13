#include <DHT.h>
#include <SoftwareSerial.h>

/*
* Project description:
*
* Concerns:
* Byte transmission order: little endian.
*/

// DEBUG
#define print(x) Serial.print(x)
#define println(x) Serial.println(x)

// Pins
#define RX_PIN 2
#define TX_PIN 3

#define DHT_SENSOR 4     // Digital pin connected to the DHT sensor
#define SOIL_SENSOR A0
#define LIGHT_SENSOR A1

#define HEAT_PIN 11
#define COLD_PIN 10
#define SOIL_PIN 8
#define HUMIDITY_PIN 9
#define LIGHT_PIN 5

// Information - 8 bytes
#define temperatureError 2
#define humidityError 5
#define soilHumidityError 0
#define lightError 25 // exponent

typedef unsigned long time_t;

DHT dht(DHT_SENSOR, DHT11);
SoftwareSerial HM10(TX_PIN, RX_PIN);

// Settings - 20 bytes
int expectedLightMinutes = 120; // 2 * 2 bytes
int lightIntensity = 0; 
int minLight = 0;
int maxLight = 100;
int minHumidity = 0;
int maxHumidity = 100;
int minSoilHumidity = 0;
int maxSoilHumidity = 100;
int minTemperature = 0;
int maxTemperature = 100; // 8 * 4 bytes

const time_t dhtWaitMillis = 60000; // 1 minute

const int dry = 1000; // value for dry sensor
const int wet = 0; // value for wet sensor
const int resistor = 10000;
const int totalVoltage = 5;
const int maxADC = 1023;

const float a = 8.27785f;
const float b = 6.0f;
const float c = 1.144547f;

// Values - 16 bytes
float light = 0;
float humidity = 0;
float soilHumidity = 0;
float temperature = 0;

int lightADC = 0;
int lightMinutes = 0;

void setup() {
    Serial.begin(9600);
    Serial.println("Iniciando invernadero");

    pinMode(LIGHT_SENSOR, INPUT);
    pinMode(SOIL_SENSOR, INPUT);
    pinMode(HEAT_PIN, OUTPUT);
    pinMode(COLD_PIN, OUTPUT);
    pinMode(SOIL_PIN, OUTPUT);
    pinMode(HUMIDITY_PIN, OUTPUT);
    pinMode(LIGHT_PIN, OUTPUT);
    
    dht.begin(9600);
    HM10.begin(9600); 
    delay(2000);
}

void loop() {
    updateDHT();
    updateLight();
    updateSoilHumidity();
    bluetoothControl();

    if (humidity < minLight){
        // Increase humidity
        digitalWrite(HUMIDITY_PIN, HIGH);
    }
    else if (humidity > maxHumidity){
        // Decrease humidity
    }
    else {
        digitalWrite(HUMIDITY_PIN, LOW);
    }

    if (temperature < minTemperature){
        // Increase Temperature
        digitalWrite(HEAT_PIN, HIGH);
    }
    else if (temperature > maxTemperature){
        // Decrease Temperature
        digitalWrite(COLD_PIN, HIGH);
    }
    else {
        digitalWrite(HEAT_PIN, LOW);
        digitalWrite(COLD_PIN, LOW);
    }

    if (light < minLight){
        // Increase light
        //digitalWrite(HEAT_PIN, 1);
    }

    if (light > maxLight){
        // Decrease light
    }
}

void updateDHT(){
    static time_t nextTimeUpdate = 0;
    const time_t now = millis();
    if (now < nextTimeUpdate)
        return;

    println("DHT");
  
    if (!dht.read())
        println("Failed to read from DHT sensor!");

    humidity = dht.readHumidity();
    temperature = dht.readTemperature();
    
    print("Humidity: ");
    print(humidity);
    print("%  Temperature: ");
    print(temperature);
    println("°C ");

    nextTimeUpdate = now + dhtWaitMillis;
}

void updateLight(){
    static time_t nextTimeUpdate = 0;
    const time_t now = millis();
    if (now < nextTimeUpdate)
        return;

    println("luz");
  
    lightADC = analogRead(LIGHT_SENSOR);
    float totalResistance = (float)maxADC / lightADC * resistor;
    float ldrResistance = totalResistance - resistor;
    light = a * pow(10, b) * pow(ldrResistance, -c);

    // Imprimimos por monitor serie el valor 
    print(lectura);
    print(" - ");
    print(light, 0);
    println(" lux");

    nextTimeUpdate = now + 1000;
}

void updateSoilHumidity()
{
    static time_t nextTimeUpdate = 0;
    const time_t now = millis();
    if (now < nextTimeUpdate)
        return;
        
    println("humedad de suelo");
    int sensorVal = analogRead(A0);

    // Sensor has a range of 239 to 595
    // We want to translate this to a scale or 0% to 100%
    // More info: https://www.arduino.cc/reference/en/language/functions/math/map/
    int percentageHumidity = map(sensorVal, wet, dry, 100, 0); 
    soilHumidity = percentageHumidity;
    print(sensorVal);
    print(" | ");
    print(percentageHumidity);
    println("%");
    
    nextTimeUpdate = now + 1000;
}

void bluetoothControl(){
    HM10.listen();  // listen the HM10 port
  
    if (HM10.available() == 0)
        return;
    
    char data = HM10.read();
    println(data);
    if (data == 'V') { // V = Read Values
        readValues('V');
    }
    else if (data == 'S'){ // S = Read Settings
        readSettings('S');
    }
    else if (data == 'W'){ // W = Set Settings
        setSettings();
    }
    else if (data == 'I'){
        readInformation('I');
    }
}

void readValues(char key){
    HM10.write(key);
    sendInt(light);
    sendInt(humidity);
    sendInt(soilHumidity);
    sendInt(temperature);
}

void readSettings(char key){
    HM10.write(key);
    sendInt(expectedLightMinutes);
    sendInt(lightIntensity);
    sendInt(minLight);
    sendInt(maxLight);
    sendInt(minHumidity);
    sendInt(maxHumidity);
    sendInt(minSoilHumidity);
    sendInt(maxSoilHumidity);
    sendInt(minTemperature);
    sendInt(maxTemperature);
}

void setSettings(){
    expectedLightMinutes = receiveInt();
    lightIntensity = receiveInt();
    minLight = receiveInt();
    maxLight = receiveInt();
    minHumidity = receiveInt();
    maxHumidity = receiveInt();
    minSoilHumidity = receiveInt();
    maxSoilHumidity = receiveInt();
    minTemperature = receiveInt();
    maxTemperature = receiveInt();
}

void readInformation(char key){
    HM10.write(key);
    sendInt(lightError);
    sendInt(humidityError);
    sendInt(soilHumidityError);
    sendInt(temperatureError);
}

void sendInt(int i){
    HM10.write((uint8_t*)&i, sizeof(int));
}

int receiveInt(){
    int buffer;
    HM10.readBytes((uint8_t*)&buffer, sizeof(int));
    return buffer;
}