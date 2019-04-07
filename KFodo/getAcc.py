from sense_hat import SenseHat
from madgwickahrs import MadgwickAHRS
from quaternion import Quaternion
from get_sensor_values import read_sensors


class Acceleration:

    def __init__(self):
        self.sense = SenseHat()
        self.madgwick = MadgwickAHRS(.055, None, 1)

    def get_acceleration(self):
        acceleration = self.sense.get_accelerometer_raw()
        orientation = self.sense.get_orientation_degrees()
        # print("p: {pitch}, r: {roll}, y: {yaw}".format(**orientation))
        ori_pitch = orientation['pitch']
        acc_x = acceleration['x']
        print("acceleration before compensation: {:.4f}".format(acc_x))

    def get_quaternion(self):
        ninedofxyz = read_sensors() # Get data from sensors
        madgwick.update(ninedofxyz[0], ninedofxyz[1], ninedofxyz[2]) # Update
        self.ahrs = madgwick.quaternion.to_euler_angles()

acc = Acceleration
acc.get_quaternion()
print("ahrs: ", self.ahrs)