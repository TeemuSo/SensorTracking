import PCA9685 as p
import RPi.GPIO as GPIO
import time


class Motor:

    # Motor constants
    PIN_RIGHT_WHEEL = None
    PIN_LEFT_WHEEL = None

    # booleans for motor states
    forward = False
    backward = False

    Motor0_A = 11  # pin11
    Motor0_B = 12  # pin12
    Motor1_A = 13  # pin13
    Motor1_B = 15  # pin15


    # Create global instance of PWM

    def __init__(self, pin_right_wheel, pin_left_wheel):
        # Create object of PCA9685
        self.pwm = p.PWM()
        self.pwm.frequency = 60
        self.pwm_servo = p.PWM()
        self.pwm_servo.frequency = 60

        # Initialize correct pins
        self.PIN_RIGHT_WHEEL = pin_right_wheel
        self.PIN_LEFT_WHEEL = pin_left_wheel

    def set_motors_forward(self):
        GPIO.setwarnings(False)
        GPIO.setmode(GPIO.BOARD)
        GPIO.setup([self.Motor0_A, self.Motor0_B, self.Motor1_A, self.Motor1_B, 7], GPIO.OUT)  # Set all pins' mode as output
        GPIO.output(self.Motor0_A, GPIO.LOW)
        GPIO.output(self.Motor0_B, GPIO.HIGH)
        GPIO.output(self.Motor1_A, GPIO.LOW)
        GPIO.output(self.Motor1_B, GPIO.HIGH)

    def set_motors_backward(self):
        GPIO.setwarnings(False)
        GPIO.setmode(GPIO.BOARD)
        GPIO.setup([self.Motor0_A, self.Motor0_B, self.Motor1_A, self.Motor1_B, 7], GPIO.OUT)  # Set all pins' mode as output
        GPIO.output(self.Motor0_A, GPIO.HIGH)
        GPIO.output(self.Motor0_B, GPIO.LOW)
        GPIO.output(self.Motor1_A, GPIO.HIGH)
        GPIO.output(self.Motor1_B, GPIO.LOW)

    # Functions for driving
    def motors_forward(self, speed):
        if not self.backward:
            print("motors running forward")
            scale_speed = speed * 300 + 1000
            if scale_speed > 4000:
                print("too high value. Max is 10")
                return
            index = 1000
            while index < scale_speed:
                time.sleep(0.01)
                self.pwm.write(self.PIN_LEFT_WHEEL, 0, index)
                self.pwm.write(self.PIN_RIGHT_WHEEL, 0, index)
                index += 50
                self.forward = True
        else:
            print("motors are running backwards already! stop it first man.")

    def motor_turn(self, speed):
        self.pwm.write(0, 0, speed)

    def motors_backward(self, speed):
        if not self.forward:
            print("motors running backward")
            scale_speed = speed*300 + 1000
            if scale_speed > 4000:
                print("too high value. Max is 10")
                return
            self.pwm.write(self.PIN_LEFT_WHEEL, 0, scale_speed)
            self.pwm.write(self.PIN_RIGHT_WHEEL, 0, scale_speed)
            self.backward = True
        else:
            print("motors are running forward already! stop it first man.")

    def stop_motors(self):
        print("Motors stopping...")
        self.pwm.write(self.PIN_LEFT_WHEEL, 0, 0)
        self.pwm.write(self.PIN_RIGHT_WHEEL, 0, 0)
        self.forward = False
        self.backward = False