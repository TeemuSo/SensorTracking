
import numpy as np
import time

'''
    Either use sensehat or MPU9250!!
    Choose your import correctly
    
    After that create object

'''
import FaBo9Axis_MPU9250
#from sense_hat import SenseHat



class SenseSensorValues:
    acceleration = .0
    gyroscope = .0
    magnetometer = .0
    #sense = SenseHat()
    '''

        Get the initial values. You can access them with ax,gx,mx...

    '''

    def __init__(self):
        ax = ay = az = gx = gy = gz = mx = my = mz = .0
        self.ninedof = np.asarray([[gx, gy, gz], [ax, ay, az], [mx, my, mz]])
        acceleration = sense.get_accelerometer_raw()
        self.ax, self.ay, self.az = acceleration['x'], acceleration['y'], acceleration['z']

        gyroscope = sense.get_gyroscope_raw()
        self.gx, self.gy, self.gz = gyroscope['x'], gyroscope['y'], gyroscope['z']

        magnetometer = sense.get_compass_raw()
        self.mx, self.my, self.mz = magnetometer['x'], magnetometer['y'], magnetometer['z']

    def read_sensors(self):
        gyroscope = sense.get_gyroscope_raw()
        self.ninedof[0] = np.asarray([gyroscope['x'], gyroscope['y'], gyroscope['z']])

        acceleration = sense.get_accelerometer_raw()
        self.ninedof[1] = np.asarray([acceleration['x'], acceleration['y'], acceleration['z']])

        magnetometer = sense.get_compass_raw()
        self.ninedof[2] = np.asarray([magnetometer['x'], magnetometer['y'], magnetometer['z']])

        return self.ninedof


class MPUSensorValues:


    def __init__(self):
        ax = ay = az = gx = gy = gz = mx = my = mz = .0
        self.ninedof = np.asarray([[gx, gy, gz], [ax, ay, az], [mx, my, mz]])
        self.mpu9250 = FaBo9Axis_MPU9250.MPU9250()  # accelerometer object

    def read_sensors(self):
        gyro = self.mpu9250.readGyro()
        self.ninedof[0] = np.asarray([gyro['x'], gyro['y'], gyro['z']])

        acc = self.mpu9250.readAccel()
        self.ninedof[1] = np.asarray([acc['x'], acc['y'], acc['z']])

        return self.ninedof

