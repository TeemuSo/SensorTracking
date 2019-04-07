import RPi.GPIO as GPIO
import numpy as np
import time


class Odometry:
    # Car constants
    WHEEL_DIAMETER = 0
    TICKS_PER_ROUND = 4
    GPIO_LEFT = None
    GPIO_RIGHT = None

    # Hidden variables
    distance = 0
    velocity = 0
    acceleration = 0
    spins_right = 0
    spins_left = 0

    # Variables for tracking
    right_detection_high = False
    left_detection_high = False

    total_detections_right = None
    total_detections_left = None

    start_spin = 0
    end_spin = 0

    def __init__(self, diameter, ticks_round, left_wheel_gpio, right_wheel_gpio):
        self.WHEEL_DIAMETER = np.pi * diameter
        self.TICKS_PER_ROUND = ticks_round
        self.GPIO_LEFT = left_wheel_gpio
        self.GPIO_RIGHT = right_wheel_gpio

        # Set GPIO board for hall sensors
        GPIO.setmode(GPIO.BOARD)
        GPIO.setup(self.GPIO_RIGHT, GPIO.IN, pull_up_down=GPIO.PUD_UP)
        GPIO.setup(self.GPIO_LEFT, GPIO.IN, pull_up_down=GPIO.PUD_UP)
        print("Pin for right {}, pin for left {} ".format(self.GPIO_RIGHT, self.GPIO_LEFT))

        # Attributes
        self.total_detections_left = 0
        self.total_detections_right = 0

    # Newtons equations for calculation

    def calculate_distance(self, x0, dt, velocity):
        self.distance = x0 + dt * velocity

    def calculate_distance_odometry(self, x0, prev_detections, detections_now):
        self.distance = x0 + ((detections_now - prev_detections)*(1/4))*self.WHEEL_DIAMETER

    def calculate_velocity(self, v0, dt, acceleration):
        self.velocity = v0 + dt * acceleration

    def setup(self):
        GPIO.add_event_detect(self.GPIO_LEFT, GPIO.BOTH, callback=self.__sensor_callback_left, bouncetime=100)
        GPIO.add_event_detect(self.GPIO_RIGHT, GPIO.BOTH, callback=self.__sensor_callback_right, bouncetime=100)


    # Function to define always the biggest amount of rotationst
    def get_detections(self):
        # Compare left and right amounts. If other one is bigger than othen, it means other one fails.
        # So, assign bigger value to smaller. Return "bigger value".
        if self.total_detections_right > self.total_detections_left:
            self.total_detections_left = self.total_detections_right
            print("total_detections_right was BIGGER!!")
            return self.total_detections_right
        if self.total_detections_left > self.total_detections_right:
            self.total_detections_right = self.total_detections_left
            print("total_detections_left was BIGGER!!")
            return self.total_detections_right
        return self.total_detections_right

    '''' 
        Here we define private callback functions 
    '''

    def __sensor_callback_left(self, channel):
        if GPIO.input(channel):
            # If it's first time calling left, add 1 to detections
            # Also change that left has been detected
            if not self.left_detection_high:
                self.total_detections_left += 1
                self.left_detection_high = True
                print("left detection high added")

                # If 4 also detections has happened, add 1 spins
                if self.total_detections_left % 4 == 0:
                    self.spins_left += 1
                    print("spin added LEFT HIGH")

        else:
            # If it's first time calling right, add 1 to detections
            if self.left_detection_high:
                self.total_detections_left += 1
                self.left_detection_high = False
                print("left detection low added")

                # If 4 detections has happened, add 1 spins
                if self.total_detections_left % 4 == 0:
                    self.spins_left += 1
                    print("spin added LEFT LOW")

    def __sensor_callback_right(self, channel):
        if GPIO.input(channel):
            # If it's first time calling left, add 1 to detections
            # Also change right detection as HIGH = True
            if not self.right_detection_high:
                self.total_detections_right += 1
                self.right_detection_high = True
                print("right detection high added")

                # If 4 detections has happened, add 1 spins
                if self.total_detections_right % 4 == 0:
                    self.spins_right += 1
                    print("spin added RIGHT HIGH")

                    # measure time
                    if self.total_detections_right == 4:
                        self.start_spin = time.time()
                    # measure time
                if self.spins_right % 2 == 0:
                    self.end_spin = time.time()
                    print("time is: ", self.end_spin - self.start_spin)

        else:
            # If it's first time calling right as LOW
            # Also set right detection as LOW
            if self.right_detection_high:
                self.total_detections_right += 1
                self.right_detection_high = False
                print("right detection low added")

                # If 4 detections has happened, add 1 spins
                if self.total_detections_right % 4 == 0:
                    self.spins_right += 1
                    print("spin added RIGHT LOW")
                    if self.total_detections_right == 4:
                        self.start_spin = time.time()
                    # measure time
                if self.spins_right % 2 == 0:
                    self.end_spin = time.time()
                    print("time is: ", self.end_spin - self.start_spin)





