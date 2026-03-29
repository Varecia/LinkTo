#include "esp_camera.h"
#include <WiFi.h>
#include <Wire.h>
#include <WebServer.h>
#include <math.h>
#include "SparkFun_BMI270_Arduino_Library.h"

// ===================
// Select camera model
// ===================
#define CAMERA_MODEL_AI_THINKER // Has PSRAM
#include "camera_pins.h"

// ===========================
// Enter your WiFi credentials
// ===========================
const char *ssid = "Mr.think的Mate 70 Pro+";
const char *password = "1234567*";

void startCameraServer();
void setupLedFlash(int pin);

// ===========================
// BMI270 wiring on ESP32-CAM
// IMPORTANT: These pins overlap with microSD pins on AI Thinker.
// Do NOT use microSD at the same time with this wiring.
// BMI270 SDO -> GND (I2C address 0x68)
// BMI270 CS  -> 3.3V (force I2C mode on many breakout boards)
// ===========================
static const int I2C_SDA_PIN = 13;
static const int I2C_SCL_PIN = 14;
static const uint8_t BMI270_I2C_ADDR = BMI2_I2C_PRIM_ADDR; // 0x68

BMI270 imu;
WebServer imuServer(82);

struct ImuSample {
  bool ok;
  uint32_t ts;
  float ax;
  float ay;
  float az;
  float gx;
  float gy;
  float gz;
  float temp;
  float accelMag;
};

ImuSample latestImu = {false, 0, 0, 0, 0, 0, 0, 0, NAN, 0};
uint32_t lastImuReadMs = 0;
const uint32_t IMU_READ_INTERVAL_MS = 50; // 20 Hz

bool initBMI270();
void readBMI270();
void setupImuHttpServer();
void handleImuJson();
void handleStatusJson();
String makeImuJson();
String makeStatusJson();

void setup() {
  Serial.begin(115200);
  Serial.setDebugOutput(true);
  Serial.println();

  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sccb_sda = SIOD_GPIO_NUM;
  config.pin_sccb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.frame_size = FRAMESIZE_UXGA;
  config.pixel_format = PIXFORMAT_JPEG;
  config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
  config.fb_location = CAMERA_FB_IN_PSRAM;
  config.jpeg_quality = 12;
  config.fb_count = 1;

  if (config.pixel_format == PIXFORMAT_JPEG) {
    if (psramFound()) {
      config.jpeg_quality = 10;
      config.fb_count = 2;
      config.grab_mode = CAMERA_GRAB_LATEST;
    } else {
      config.frame_size = FRAMESIZE_SVGA;
      config.fb_location = CAMERA_FB_IN_DRAM;
    }
  } else {
    config.frame_size = FRAMESIZE_240X240;
#if CONFIG_IDF_TARGET_ESP32S3
    config.fb_count = 2;
#endif
  }

#if defined(CAMERA_MODEL_ESP_EYE)
  pinMode(13, INPUT_PULLUP);
  pinMode(14, INPUT_PULLUP);
#endif

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed with error 0x%x\n", err);
    return;
  }

  sensor_t *s = esp_camera_sensor_get();
  if (s->id.PID == OV2640_PID) {
    s->set_vflip(s, 1);
    s->set_brightness(s, 1);
    s->set_saturation(s, -2);
  }
  if (config.pixel_format == PIXFORMAT_JPEG) {
    s->set_framesize(s, FRAMESIZE_QVGA);
  }

#if defined(CAMERA_MODEL_M5STACK_WIDE) || defined(CAMERA_MODEL_M5STACK_ESP32CAM)
  s->set_vflip(s, 1);
  s->set_hmirror(s, 1);
#endif

#if defined(CAMERA_MODEL_ESP32S3_EYE)
  s->set_vflip(s, 1);
#endif

#if defined(LED_GPIO_NUM)
  setupLedFlash(LED_GPIO_NUM);
#endif

  // Init I2C for BMI270 after camera init.
  // GPIO13/14 are reused from the microSD interface; leave microSD unused.
  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN, 400000);
  bool imuReady = initBMI270();
  Serial.printf("BMI270 init: %s\n", imuReady ? "OK" : "FAILED");

  WiFi.begin(ssid, password);
  WiFi.setSleep(false);

  Serial.print("WiFi connecting");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.println("WiFi connected");

  startCameraServer();
  setupImuHttpServer();

  Serial.print("Camera stream: http://");
  Serial.println(WiFi.localIP());
  Serial.print("IMU JSON:     http://");
  Serial.print(WiFi.localIP());
  Serial.println(":82/imu.json");
  Serial.print("Status JSON:  http://");
  Serial.print(WiFi.localIP());
  Serial.println(":82/status.json");
}

void loop() {
  imuServer.handleClient();

  if (millis() - lastImuReadMs >= IMU_READ_INTERVAL_MS) {
    lastImuReadMs = millis();
    readBMI270();
  }

  delay(2);
}

bool initBMI270() {
  for (int attempt = 0; attempt < 5; ++attempt) {
    int8_t rslt = imu.beginI2C(BMI270_I2C_ADDR);
    if (rslt == BMI2_OK) {
      latestImu.ok = true;
      return true;
    }
    Serial.printf("BMI270 beginI2C failed, code=%d, retry=%d\n", rslt, attempt + 1);
    delay(500);
  }
  latestImu.ok = false;
  return false;
}

void readBMI270() {
  if (!latestImu.ok) {
    // Try to recover if sensor was not initialized.
    latestImu.ok = initBMI270();
    if (!latestImu.ok) {
      return;
    }
  }

  int8_t rslt = imu.getSensorData();
  if (rslt != BMI2_OK) {
    latestImu.ok = false;
    Serial.printf("BMI270 read failed, code=%d\n", rslt);
    return;
  }

  latestImu.ts = millis();
  latestImu.ax = imu.data.accelX;
  latestImu.ay = imu.data.accelY;
  latestImu.az = imu.data.accelZ;
  latestImu.gx = imu.data.gyroX;
  latestImu.gy = imu.data.gyroY;
  latestImu.gz = imu.data.gyroZ;
  latestImu.temp = 0.0f;
  latestImu.accelMag = sqrtf(
    latestImu.ax * latestImu.ax +
    latestImu.ay * latestImu.ay +
    latestImu.az * latestImu.az
  );

  static uint32_t lastPrintMs = 0;
  if (millis() - lastPrintMs >= 1000) {
    lastPrintMs = millis();
    Serial.printf(
      "BMI270 ax=%.3f ay=%.3f az=%.3f | gx=%.3f gy=%.3f gz=%.3f | temp=%.2f\n",
      latestImu.ax, latestImu.ay, latestImu.az,
      latestImu.gx, latestImu.gy, latestImu.gz,
      latestImu.temp
    );
  }
}

void setupImuHttpServer() {
  imuServer.on("/", HTTP_GET, []() {
    String html = "<html><body><h2>ESP32-CAM + BMI270</h2>";
    html += "<p>IMU JSON: <a href='/imu.json'>/imu.json</a></p>";
    html += "<p>Status JSON: <a href='/status.json'>/status.json</a></p>";
    html += "<p>Camera UI/stream is still served by the original camera server on port 80.</p>";
    html += "</body></html>";
    imuServer.send(200, "text/html", html);
  });

  imuServer.on("/imu.json", HTTP_GET, handleImuJson);
  imuServer.on("/status.json", HTTP_GET, handleStatusJson);
  imuServer.onNotFound([]() {
    imuServer.send(404, "application/json", "{\"error\":\"not found\"}");
  });
  imuServer.begin();
}

void handleImuJson() {
  imuServer.send(200, "application/json", makeImuJson());
}

void handleStatusJson() {
  imuServer.send(200, "application/json", makeStatusJson());
}

String makeImuJson() {
  String json = "{";
  json += "\"type\":\"imu\",";
  json += "\"ok\":" + String(latestImu.ok ? "true" : "false") + ",";
  json += "\"ts_ms\":" + String(latestImu.ts) + ",";
  json += "\"accel_mps2\":{";
  json += "\"x\":" + String(latestImu.ax, 4) + ",";
  json += "\"y\":" + String(latestImu.ay, 4) + ",";
  json += "\"z\":" + String(latestImu.az, 4) + "},";
  json += "\"gyro_dps\":{";
  json += "\"x\":" + String(latestImu.gx, 4) + ",";
  json += "\"y\":" + String(latestImu.gy, 4) + ",";
  json += "\"z\":" + String(latestImu.gz, 4) + "},";
  if (isnan(latestImu.temp)) {
    json += "\"temperature_c\":null,";
  } else {
    json += "\"temperature_c\":" + String(latestImu.temp, 2) + ",";
  }
  json += "\"accel_magnitude_mps2\":" + String(latestImu.accelMag, 4);
  json += "}";
  return json;
}

String makeStatusJson() {
  String json = "{";
  json += "\"wifi_connected\":" + String(WiFi.status() == WL_CONNECTED ? "true" : "false") + ",";
  json += "\"ip\":\"" + WiFi.localIP().toString() + "\",";
  json += "\"rssi\":" + String(WiFi.RSSI()) + ",";
  json += "\"free_heap\":" + String(ESP.getFreeHeap()) + ",";
  json += "\"psram\":" + String(psramFound() ? "true" : "false") + ",";
  json += "\"imu_ok\":" + String(latestImu.ok ? "true" : "false");
  json += "}";
  return json;
}
