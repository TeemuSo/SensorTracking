import motor_control
import odometry
import RPi.GPIO as GPIO
import data_processing
#import GPS
import FaBo9Axis_MPU9250
import numpy as np
from threading import Thread

import time as t

# Initialize instances of motor and odometry tracking
print("startup")
GPIO.cleanup()
motor = motor_control.Motor(4, 5)
odo = odometry.Odometry(diameter=6.5, ticks_round=4, left_wheel_gpio=38, right_wheel_gpio=36)
print("odo object done")
data = data_processing.Data()
acc_data = data_processing.Data()
acc_data_estimate = data_processing.Data()
mpu9250 = FaBo9Axis_MPU9250.MPU9250()
print("objects done")
#gps = GPS.GPS()

'''
    Here starts the actual routine.
    First set motors moving, the go to while loop
'''
motor.stop_motors()
t.sleep(2)

odo.setup()
line_numbers = 0
# Track in centimeters
# Start calculating time
data.open_list("distance_odo")
acc_data.open_list("acc_data")
acc_data_estimate.open_list("acc_data_estimate")
gyro_data.open_list("gyro_data")

# Start motor and setup odo


def get_accel(count):
    index = 0
    acc_mean = []
    while index < count:
        accel = mpu9250.readAccel()
        #gyro = mpu9250.readGyro()
        '''        if index % 20 == 0:

            # Get sum of elements in list
            mean_sum = .0
            for n in acc_mean:
                mean_sum += n

            # Get mean of the sum
            mean = mean_sum/20
            print("saving mean as = ", mean)
            # Save it to text file
            acc_mean = []
            print("list should be empty: ", acc_mean)
        '''
        accel_data_ready = "{}\n".format(accel['x'])  # Format to right format
        gyro_data_ready = "{}\n".format(gyro['x'])  # Format to right format

        acc_data.set_list(accel_data_ready) # save to file
        gyro_data.set_list(gyro_data_ready)

        #acc_mean.append(accel['x'])
        t.sleep(0.01)
        index += 1


def get_detections():
    right, left = odo.get_detections()
    print("\n\ndetections left: {}\ndetections right: {}\n\n".format(left, right))
    t.sleep(0.2)
#get_accel(100)

'''

    Testiajo kiihtyvyysanturille paikallaan
    
'''

get_accel(200)  # Hae kiihtyvyys
motor.set_motors_forward()  # Set motors forward
motor.motors_forward(4)     # Start motors
get_accel(1000)  # Hae kiihtyvyys
motor.stop_motors() # Stop motors
get_accel(200)  # Get acceleration

############################################

'''print("LISTS OPENED")
# For velocity measurements
distance_acc = 0

# thread for acceleration

velocity_acc = 0


#th = Thread(target=(get_accel()))
#th.start()
print("Thread started!")
print("initial odo.distance: ", odo.distance)
while odo.distance < 100:
    try:
        prev_distance = odo.distance
        print("prev_distance done")
        prev_detections = odo.total_detections_right
        print("detections: ", odo.total_detections_right)
        get_accel(130)

        # >>>>>>>>>>>>>>> Debugging <<<<<<<<<<<<<<
        total_detections = odo.get_detections()
        print("total_detections: {}, total_det_right: {}, prev_distance: {}, prev_detections: {}"
              .format(total_detections, odo.total_detections_right, prev_distance, prev_detections))

        # Calculate the actual distance from odometry
        odo.calculate_distance_odometry(prev_distance, prev_detections, total_detections)
        print("distance: ", odo.distance)

        # Get content for writing everything in a file
        line_numbers += total_detections - prev_detections
        print("line numbers ok")
        odo_data = "{} {}\n".format(line_numbers, odo.distance)
        print("ready to save")
        data.set_list(odo_data)      # Save data in a file named distance

    except:
        motor.stop_motors()
        print("error boy")
        break

get_accel()
#th.stop()
#data.close_list()
motor.stop_motors()'''

