import motor_control
import odometry
import RPi.GPIO as GPIO
import data_processing
import GPS

import time as t

# Initialize instances of motor and odometry tracking
GPIO.cleanup()
motor = motor_control.Motor(4, 5)
odo = odometry.Odometry(diameter=6.5, ticks_round=4, left_wheel_gpio=38, right_wheel_gpio=36)
gps = GPS.GPS()
data = data_processing.Data()
coord = data_processing.Data()
lat = data_processing.Data()
lon = data_processing.Data()

#gps = GPS.GPS()

'''
    Here starts the actual routine.
    First set motors moving, the go to while loop
'''
motor.stop_motors()
t.sleep(2)

# Start motor and setup odo
motor.motors_forward(7)
odo.setup()
gps.get_gps()
print("GPS SIGNAL FOUND")
line_numbers = 0
# Track in centimeters
# Start calculating time
data.open_list("distance_odo")
coord.open_list("coord")
lat.open_list("lat")
lon.open_list("lon")
print("LISTS OPENED")

print("initial odo.distance: ", odo.distance)
while odo.distance < 800:
    try:
        prev_distance = odo.distance
        prev_detections = odo.total_detections_right
        print("detections: ", odo.total_detections_right)
        t.sleep(1.3)

        # >>>>>>>>>>>>>>> Debugging <<<<<<<<<<<<<<
        total_detections = odo.get_detections()
        print("total_detections: {}, total_det_right: {}, prev_distance: {}, prev_detections: {}"
              .format(total_detections, odo.total_detections_right, prev_distance, prev_detections))

        gps.get_gps()
        coord_data = "{} {}\n".format(gps.latitude, gps.longitude)
        coord.set_list(coord_data)

        lat_data = "{}\n".format(gps.latitude)
        coord.set_list(lat_data)

        lon_data = "{}\n".format(gps.longitude)
        coord.set_list(lon_data)
        print("GPS LISTS WRITTEN")

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

#data.close_list()
motor.stop_motors()

