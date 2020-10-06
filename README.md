"# SensorTracking" 

This application is inteded to be used in co-operation with Raspberry Pi 3. From sensors MPU-9250, and SenseHat() are supported. You need to do slight changes to "get_sensor_values.py", depending on which sensor you are using. Right now scripts are working for MPU-9250.

For car movements, I2C bus PCA9685 is used. This project also includes the necessary controllers for it.

The project includes mobile application for controlling RC car, and tracking the velocity and acceleration. This project was done as part of a mobile programming course, and the main emphasis was on getting the working logic right.
