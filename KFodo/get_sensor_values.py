from sense_hat import SenseHat
import numpy as np
import time
sense = SenseHat()

class SensorValues:
    acceleration = .0
    gyroscope = .0
    magnetometer = .0

def __init__(self):
    acceleration = sense.get_accelerometer_raw()
    ax, ay, az = acceleration['x'], acceleration['y'], acceleration['z']

    gyroscope = sense.get_gyroscope_raw()
    gx, gy, gz = gyroscope['x'], gyroscope['y'], gyroscope['z']

    magnetometer = sense.get_compass_raw()
    mx, my, mz = magnetometer['x'], magnetometer['y'], magnetometer['z']


def read_sensors():
    ax = ay = az = gx = gy = gz = mx = my = mz = .0
    ninedof = np.asarray([[gx, gy, gz], [ax, ay, az], [mx, my, mz]])

    gyroscope = sense.get_gyroscope_raw()
    ninedof[0] = np.asarray([gyroscope['x'], gyroscope['y'], gyroscope['z']])

    acceleration = sense.get_accelerometer_raw()
    ninedof[1] = np.asarray([acceleration['x'], acceleration['y'], acceleration['z']])

    magnetometer = sense.get_compass_raw()
    ninedof[2] = np.asarray([magnetometer['x'], magnetometer['y'], magnetometer['z']])
    return ninedof