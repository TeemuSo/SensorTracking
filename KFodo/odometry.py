import RPi.GPIO as GPIO
import numpy as np
import time

from threading import Thread
from multiprocessing import Process, Pipe


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
        self.WHEEL_DIAMETER = diameter
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

    def calculate_distance_odometry(self, prev_detections, detections_now):
        self.distance += ((detections_now - prev_detections)*(1/4))*self.WHEEL_DIAMETER     # * pi?!?!?!?!?
        return self.distance

    def calculate_velocity_odometry(self):
        if self.spins_right >= 2:
            print("time is: ", self.spin_time)
            return 20.4202 / self.spin_time     # One round is about 20cm
        else:
            print("Call velocity_odometry only after 2 spins. Spins done: ", self.spins_right)

    def calculate_velocity(self, v0, dt, acceleration):
        self.velocity = v0 + dt * acceleration
        return self.velocity


    def setup(self):
        GPIO.add_event_detect(self.GPIO_LEFT, GPIO.BOTH, callback=self.__sensor_callback_left, bouncetime=100)
        GPIO.add_event_detect(self.GPIO_RIGHT, GPIO.BOTH, callback=self.__sensor_callback_right, bouncetime=100)


    # Function to define always the biggest amount of rotationst
    def get_detections(self):
        # Compare left and right amounts. If other one is bigger than othen, it means other one fails.
        # So, assign bigger value to smaller. Return "bigger value".
        #if self.total_detections_right > self.total_detections_left:
        #    self.total_detections_left = self.total_detections_right
        #    print("total_detections_right was BIGGER!!")
        #    return self.total_detections_right
        #if self.total_detections_left > self.total_detections_right:
        #    self.total_detections_right = self.total_detections_left
        #    print("total_detections_left was BIGGER!!")
        #    return self.total_detections_right
        return self.total_detections_right, self.total_detections_left

    ''''
        Here we define private callback functions
    '''

    def __sensor_callback_left(self, channel):
        if GPIO.input(channel):
            # If it's first time calling left, add 1 to detections
            # Also change that left has been detected
            if not self.left_detection_high:
                self.distance += (1 / 4) * self.WHEEL_DIAMETER * np.pi
                print("dist callback", self.distance)
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
                self.distance += (1 / 4) * self.WHEEL_DIAMETER * np.pi
                print("dist callback", self.distance)
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
                self.distance += (1 / 4) * self.WHEEL_DIAMETER * np.pi
                print("dist callback", self.distance)
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
                self.distance += (1 / 4) * self.WHEEL_DIAMETER * np.pi
                print("dist callback", self.distance)
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
                    self.spin_time = self.end_spin - self.start_spin
                    print("time is: ", self.spin_time)

class DetectionThread(Thread):
    def __init__(self):
        Thread.__init__(self)
        self.odo = Odometry(diameter=6.5, ticks_round=4, left_wheel_gpio=38, right_wheel_gpio=36)

    def run(self):
        self.odo.setup()


class MultiProcess(Process):
    def __init__(self, conn):
        super(MultiProcess, self).__init__()
        self.odo = None
        self.conn = conn

    def run(self):
        print("process started")
        self.odo = Odometry(diameter=6.5, ticks_round=4, left_wheel_gpio=38, right_wheel_gpio=36)
        self.odo.setup()
        prev_det = 0
        while True:
            time.sleep(0.01)
            try:
                if self.odo.total_detections_right > prev_det:
                    self.conn.send(self.odo.total_detections_right)

            except KeyboardInterrupt:
                self.conn.close()
                break



    def killProcess(self):
        self.p.terminate()

