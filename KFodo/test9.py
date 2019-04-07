import odometry
import data_processing
import GPS
import time
import motor_control
import threading
from multiprocessing import Process, Pipe
import os
import numpy as np


class Steering:

    def __init__(self, start_coord=None, end_coord=None):
        if start_coord is None and end_coord is None:
            self.start_coord = np.array([0., 0.])
            self.end_coord = np.array([0., 0.])
        else:
            self.start_coord = start_coord
            self.end_coord = end_coord

        # Create objects
        #self.odo = odometry.Odometry(diameter=6.5, ticks_round=4, left_wheel_gpio=38, right_wheel_gpio=36)
        detect_thread = odometry.DetectionThread()      # Create thread to run odometry on
        detect_thread.run()
        self.odo = detect_thread.odo
        self.motor = motor_control.Motor(4, 5)

        # create running distance
        self.distance = 0

    def calculate_steering_angle(self, start_coord, end_coord):

        # Pass lists including coord_x and coord_y
        start_coord_x, start_coord_y = start_coord
        end_coord_x, end_coord_y = end_coord

        angle = 0

        x_dif = end_coord_x - start_coord_x
        y_dif = end_coord_y - start_coord_y
        coord_offset = 90
        sign = -1
        if x_dif < 0 or y_dif < 0:
            coord_offset = 0
            sign = 1
        if y_dif != 0 and x_dif != 0:
            print("x_dif: ", x_dif)
            angle = (np.arctan(y_dif/x_dif))*(180/np.pi)
        return angle

    def angles_to_servovalue(self, angle):
        if angle > 0:
            return 430
        if angle < 0:
            return 280
        if angle == 0:
            return 350
        #return int(angle * 5 + 350)     # Really rough estimate. 20 degrees equal to value 100. Add offset of 350

    def turn_servo(self, value):
        self.motor.motor_turn(value)

    def calculate_difference(self, start_coord, end_coord):

        # Pass lists including coord_x and coord_y
        start_coord_x, start_coord_y = start_coord
        end_coord_x, end_coord_y = end_coord

        x_dif = end_coord_x - start_coord_x
        y_dif = end_coord_y - start_coord_y
        print("y_dif^2", y_dif**2)
        return np.sqrt(x_dif**2 + y_dif**2)   # Return hypotenuusa (XD)

    '''
        Calculates distance gone by x and y
        Must be updated at least every time angle changes!
    '''
    def calculate_distance_xy(self, angle, distance_in_angle):
        return distance_in_angle * np.cos(angle), distance_in_angle * np.sin(angle)

    # Make thread, which calculates constant running time based on acquired velocity
    def velocity_based_time(self, distance, isDone, conn):
        print("IN PROCESS")
        # While robot is travelling
        self.distance = distance
        while isDone:
            # Get the time travelled
            start = time.time()
            time.sleep(0.05)
            end = time.time()
            time_difference = end - start
            self.distance += time_difference * 6.81587
            conn.send(self.distance)
            #print("time_difference: ", time_difference)





def drive_straight(steer, data, start_coord, end_coord):
    # Calculate angle
    steering_angle = steer.calculate_steering_angle(start_coord, end_coord)
    servo_value = steer.angles_to_servovalue(steering_angle)
    print("\n\n\n\nangle: ", steering_angle)
    print("\nvalue: ", servo_value)

    # Turn servo the required amount
    steer.turn_servo(servo_value)
    diffr = steer.calculate_difference(start_coord, end_coord)

    # Boolean for if velocity_based_time thread has been started
    processStarted = False
    prev_distance = steer.odo.distance/2   # Prev distance saves distance from start of the drive_straight
    distance = 0

    curve_distance = 9999999
    if steering_angle != 0:
        curve_distance = ((np.abs(steering_angle) * 2 * np.pi * 32) / 360)
        print("\n\n\nCURVE DISTANCE", curve_distance)

    # Calculate hhhhypotenus
    hhypotenus = steer.calculate_difference(start_coord, end_coord)

    parent_conn, child_conn = Pipe()
    process = Process(target=steer.velocity_based_time, args=(distance, True, child_conn))
    distance = 0
    process.start()
    while distance < hhypotenus:
        distance = parent_conn.recv()
        distance2 = steer.odo.distance/2
        distance_format = "{} {}\n".format(distance, distance2)
        data.set_list(distance_format)
        print("Distance: ", distance)
    print("HYPO:", hhypotenus)

    # While robot is not at specified end coordinate
    '''
            OLD AND SHIT


    while steer.distance < hhypotenus:
        try:
            print("steer.disance", steer.distance)
            time.sleep(0.3)
            # Calculate distance for this instance
            distance = steer.odo.distance/2 - prev_distance
            #os.system('clear')
            print("\nstatic distance is: ", distance)
            #print("\nprev_distance", prev_distance)
            #print("\nsteer.odo.distance: ", steer.odo.distance)

            # Get distance x and y of this instance
            distance_x, distance_y = steer.calculate_distance_xy(steering_angle, distance)
            #print("distace_x: {}, distance_Y: {}".format(distance_x, distance_y))

            # Assign new start coord
            start_coord = distance_x, distance_y

            # Calculate new distance to track progress
            diffr = steer.calculate_difference(start_coord, end_coord)

            if steer.distance > curve_distance:
                steer.turn_servo(350)

            # If distance is over one wheel revolution, we can calculat velocity
            if distance > 30:
                velocity = steer.odo.calculate_velocity_odometry()
                print(processStarted)
                if velocity is not None and not processStarted:

                    print("velocity is: ", velocity)
                    # Create new process to make calculations parallel

                    processStarted = True
                    print("Process started")
                #print("velocity is: ", velocity)
            #print("\ndifference is: ", diffr)
        except KeyboardInterrupt:
            steer.motor.stop_motors()
            break
    if processStarted:
        process.terminate()
        processStarted = False
    '''


#def calculate_smooth_distance(distance, velocity):


''' ================ HERE STARTS THE ACTUAL MAIN ============= '''

# Set coordinates
'''x_start, y_start = .0, .0
x_end, y_end = .0, 100.0

x_start1, y_start1 = 0., 0.
x_end1, y_end1 = 100., -20.0
'''
# Coords
start_coord = np.array([0., 0.])
end_coord = np.array([30., 0.])

start_coord1 = np.array([0, 0])
end_coord1 = np.array([30., 10])

start_coord2 = np.array([0, 0])
end_coord2 = np.array([30., 0])


steer = Steering(start_coord, end_coord)    # Create object
data = data_processing.Data()
GPS = GPS.GPS()
data.open_list("wheel_odometry.txt")

steer.motor.stop_motors()
# Set initial steering angle and calculate servo value





'''

    THIS LOOP IS ONLY FOR ONE COORDINATE, NOT FOR MULTIPLE

'''

steer.motor.set_motors_forward()
steer.motor.motors_forward(1)
try:
    # function for straight driving
    #drive_straight(steer, start_coord, end_coord)
    #drive_straight(steer, start_coord1, end_coord1)
    drive_straight(steer, data, start_coord2, end_coord2)
except KeyboardInterrupt:
    steer.motor.stop_motors()

steer.motor.stop_motors()

'''
        ADD THIS WHEN WORKING WITH MULTIPLE COORDINATES

    # Calculate required steering angle, and set servos to it
    steering_angle = steer.calculate_steering_angle(start_coord, end_coord)
    servo_value = steer.angles_to_servovalue(steering_angle)

    '''