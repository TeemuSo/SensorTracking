from sense_hat import SenseHat
import numpy as np
import time
sense = SenseHat()

class SensorValues:
    acceleration = .0
    gyroscope = .0
    magnetometer = .0
    '''

        Get the initial values. You can access them with ax,gx,mx...

    '''

    def __init__(self):
        acceleration = sense.get_accelerometer_raw()
        self.ax, self.ay, self.az = acceleration['x'], acceleration['y'], acceleration['z']

        gyroscope = sense.get_gyroscope_raw()
        self.gx, self.gy, self.gz = gyroscope['x'], gyroscope['y'], gyroscope['z']

        magnetometer = sense.get_compass_raw()
        self.mx, self.my, self.mz = magnetometer['x'], magnetometer['y'], magnetometer['z']

    '''

        Return sensor values

    '''

    def read_sensors(self):
        ax = ay = az = gx = gy = gz = mx = my = mz = .0
        self.ninedof = np.asarray([[gx, gy, gz], [ax, ay, az], [mx, my, mz]])

        gyroscope = sense.get_gyroscope_raw()
        self.ninedof[0] = np.asarray([gyroscope['x'], gyroscope['y'], gyroscope['z']])

        acceleration = sense.get_accelerometer_raw()
        self.ninedof[1] = np.asarray([acceleration['x'], acceleration['y'], acceleration['z']])

        magnetometer = sense.get_compass_raw()
        self.ninedof[2] = np.asarray([magnetometer['x'], magnetometer['y'], magnetometer['z']])
        return self.ninedof


