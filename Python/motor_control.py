import PCA9685 as p
import RPi.GPIO as GPIO


class Motor:

    # Motor constants
    PIN_RIGHT_WHEEL = None
    PIN_LEFT_WHEEL = None

    # booleans for motor states
    forward = False
    backward = False

    # Create global instance of PWM

    def __init__(self, pin_right_wheel, pin_left_wheel):
        # Create object of PCA9685
        self.pwm = p.PWM()
        self.pwm.frequency = 60

        # Initialize correct pins
        self.PIN_RIGHT_WHEEL = pin_right_wheel
        self.PIN_LEFT_WHEEL = pin_left_wheel

    '''
    def __set_motors_forward(self):
        GPIO.setwarnings(False)
        GPIO.setmode(GPIO.BOARD)
        GPIO.setup([Motor0_A, Motor0_B, Motor1_A, Motor1_B, 7], GPIO.OUT)  # Set all pins' mode as output
        GPIO.output(Motor0_A, GPIO.HIGH)
        GPIO.output(Motor0_B, GPIO.LOW)
        GPIO.output(Motor1_A, GPIO.HIGH)
        GPIO.output(Motor1_B, GPIO.LOW)'''

    # Functions for driving
    def motors_forward(self, speed):
        if not self.backward:
            print("motors running forward")
            scale_speed = speed * 300 + 1000
            if scale_speed > 4000:
                print("too high value. Max is 10")
                return
            self.pwm.write(self.PIN_LEFT_WHEEL, 0, scale_speed)
            self.pwm.write(self.PIN_RIGHT_WHEEL, 0, scale_speed)
            self.forward = True
        else:
            print("motors are running backwards already! stop it first man.")

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
