#include <DHT.h>
#include <SoftwareSerial.h>

// Important:
// Byte transmission order: little endian.

// Pins
#define RX_PIN 2
#define TX_PIN 3

#define DHT_SENSOR 4
#define SOIL_SENSOR A0
#define LIGHT_SENSOR A1

#define TIME_PIN 12
#define HEAT_PIN 11
#define COLD_PIN 10
#define SOIL_PIN 8
#define HUMIDITY_PIN 9

// Information - 8 bytes
#define temperatureError 3
#define humidityError 5
#define soilHumidityError 3
#define lightError 2 // factor

typedef unsigned long time_t;
typedef unsigned int uint;

DHT dht(DHT_SENSOR, DHT11);
SoftwareSerial HM10(TX_PIN, RX_PIN);

// Settings - 18 bytes
int expectedLightMinutes = 120;
uint minLight = 1;
uint maxLight = 100;
int minHumidity = 0;
int maxHumidity = 100;
int minSoilHumidity = 0;
int maxSoilHumidity = 100;
int minTemperature = 0;
int maxTemperature = 100;

const time_t dhtWaitMillis = 5000; // 5 segundos

const int dry = 640; // valor para sensor seco
const int wet = 240; // valor para sensor húmedo
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
time_t lightTimeMillis = 0;
time_t lastTimeMillis = 0;

void setup() {
    Serial.begin(9600);
    Serial.println("Iniciando invernadero");
    
    setupPins();
    
    HM10.begin(9600);  
    dht.begin(9600);
}

void setupPins(){
    pinMode(LIGHT_SENSOR, INPUT);
    pinMode(SOIL_SENSOR, INPUT);
    pinMode(TIME_PIN, OUTPUT);
    pinMode(HEAT_PIN, OUTPUT);
    pinMode(COLD_PIN, OUTPUT);
    pinMode(SOIL_PIN, OUTPUT);
    pinMode(HUMIDITY_PIN, OUTPUT);
}

void loop() {
    updateDHT();
    updateLight();
    updateSoilHumidity();
    bluetoothControl();

    if (humidity < minHumidity){
        // Aumentar humedad
        digitalWrite(HUMIDITY_PIN, HIGH);
    }
    else {
        digitalWrite(HUMIDITY_PIN, LOW);
    }
    
    if (humidity > maxHumidity){
        // Disminuir humedad
    }
    else {
        
    }

    if (temperature < minTemperature){
        // Aumentar temperatura
        digitalWrite(HEAT_PIN, HIGH);
    }
    else {
        digitalWrite(HEAT_PIN, LOW);
    }
    
    if (temperature > maxTemperature){
        // Disminuir temperatura
        digitalWrite(COLD_PIN, HIGH);
    }
    else {
        digitalWrite(COLD_PIN, LOW);
    }

    if (soilHumidity < minSoilHumidity){
        // Aumentar humedad de suelo
        digitalWrite(SOIL_PIN, HIGH);
    }
    else {
        digitalWrite(SOIL_PIN, LOW);
    }
    
    if (soilHumidity > maxSoilHumidity){
        // Disminuir humedad de suelo
    }
    else {
        
    }
    
    time_t now = millis();
    if (light >= minLight && light <= maxLight){
        time_t elapsed = now - lastTimeMillis;
        lightTimeMillis += elapsed; 
    }
    
    int lightTimeMinutes = lightTimeMillis / 60000;
    if (lightTimeMinutes >= expectedLightMinutes){
        // Tiempo de luz completado
        digitalWrite(TIME_PIN, LOW);
    }
    else {
        digitalWrite(TIME_PIN, HIGH);
    }
    lastTimeMillis = now;
}

void updateDHT(){
    static time_t nextTimeUpdate = 0;
    const time_t now = millis();
    if (now < nextTimeUpdate)
        return;

    Serial.println("Sensando humedad y temperatura");
  
    if (!dht.read())
        Serial.println("Falló la lectura del sensor DHT");

    humidity = dht.readHumidity();
    temperature = dht.readTemperature();
    
    Serial.print("Humidity: ");
    Serial.print(humidity);
    Serial.print("%  Temperature: ");
    Serial.print(temperature);
    Serial.println("°C ");

    nextTimeUpdate = now + dhtWaitMillis;
}

void updateLight(){
    static time_t nextTimeUpdate = 0;
    const time_t now = millis();
    if (now < nextTimeUpdate)
        return;

    Serial.println("Sensando luz");
  
    lightADC = analogRead(LIGHT_SENSOR);
    float totalResistance = (float)maxADC / lightADC * resistor;
    float ldrResistance = totalResistance - resistor;
    light = a * pow(10, b) * pow(ldrResistance, -c);

    Serial.print(lightADC);
    Serial.print(" | ");
    Serial.print(light, 0);
    Serial.println(" lux");

    nextTimeUpdate = now + 1000;
}

void updateSoilHumidity(){
    static time_t nextTimeUpdate = 0;
    const time_t now = millis();
    if (now < nextTimeUpdate)
        return;
        
    Serial.println("Sensando humedad de suelo");
    int sensorVal = analogRead(A0);

    int percentageHumidity = map(sensorVal, wet, dry, 100, 0); 
    soilHumidity = percentageHumidity;
    Serial.print(sensorVal);
    Serial.print(" | ");
    Serial.print(percentageHumidity);
    Serial.println("%");
    
    nextTimeUpdate = now + 1000;
}

void bluetoothControl(){
    HM10.listen();
  
    if (HM10.available() == 0)
        return;
    
    char data = HM10.read();
    switch (data){
        case 'V': readValues('V'); break; // Leer valores
        case 'S': readSettings('S'); break; // Leer configuración
        case 'W': setSettings(); break; // Escribir configuración
        case 'I': readInformation('I'); break; // Leer constantes
    }
}

void readValues(char key){
    Serial.println("Transmitiendo valores");
    HM10.write(key);
    sendInt(light);
    sendInt(humidity);
    sendInt(soilHumidity);
    sendInt(temperature);
}

void readSettings(char key){
    Serial.println("Transmitiendo configuración");
    HM10.write(key);
    sendInt(expectedLightMinutes);
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
    Serial.println("Aplicando configuración");
    expectedLightMinutes = receiveInt();
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
    Serial.println("Transmitiendo constantes");
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