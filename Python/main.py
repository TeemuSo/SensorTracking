#!/usr/bin/python

import logging
import logging.handlers
import argparse
import sys
import os
import time
import numpy as np
import socket
import threading
from threading import Thread

from multiprocessing import Process, Queue

# Import custom objects
import get_sensor_values
import motor_control

from _thread import *
#from sense_hat import SenseHat
from bluetooth import *


class LoggerHelper(object):
    def __init__(self, logger, level):
        self.logger = logger
        self.level = level

    def write(self, message):
        if message.rstrip() != "":
            self.logger.log(self.level, message.rstrip())


class RealTimeThread:

    def __init__(self, sensor_object, conn):
        self.stoprequest = True
        self.sensor_object = sensor_object
        self.conn = conn

    def get_realtime_data(self):
        while True:
            try:
                item = q.get(True, 1)
                if item == "quit":
                    print("quitting")
                    break
            except:
                pass

            print("\nentered while loop in thread")
            acc_data = get_sensor_data(self.sensor_object)
            response_acc = "realtime\n" + str(acc_data)
            self.conn.send(response_acc)
            print("\nnew value sent: ", response_acc)
            time.sleep(0.1)

def stopThread():
    q.put("quit")
    print("put quit into que.")


def setup_logging():
    # Default logging settings
    LOG_FILE = "/home/pi/PythonPractise/btApp/Python/raspibt.log"
    LOG_LEVEL = logging.INFO

    # Define and parse command line arguments
    argp = argparse.ArgumentParser(description="Raspberry PI Bluetooth Server")
    argp.add_argument("-l", "--log", help="log (default '" + LOG_FILE + "')")

    # Grab the log file from arguments
    args = argp.parse_args()
    if args.log:
        LOG_FILE = args.log

    # Setup the logger
    logger = logging.getLogger(__name__)
    # Set the log level
    logger.setLevel(LOG_LEVEL)
    # Make a rolling event log that resets at midnight and backs-up every 3 days
    handler = logging.handlers.TimedRotatingFileHandler(LOG_FILE,
                                                        when="midnight",
                                                        backupCount=3)

    # Log messages should include time stamp and log level
    formatter = logging.Formatter('%(asctime)s %(levelname)-8s %(message)s')
    # Attach the formatter to the handler
    handler.setFormatter(formatter)
    # Attach the handler to the logger
    logger.addHandler(handler)

    # Replace stdout with logging to file at INFO level
    sys.stdout = LoggerHelper(logger, logging.INFO)
    # Replace stderr with logging to file at ERROR level
    sys.stderr = LoggerHelper(logger, logging.ERROR)
    print("setup_logging done")


def connect_to_device(self):
    try:
        self.conn = Peripheral(self.sensor_addr, self.addr_type)  # Yritetään yhdistää sensoriin
        # Aseteaan delegate joka vastaanottaa ilmoitukset (notifications)
        self.conn.setDelegate(MyDelegate())
        self.set_notifications(True)
        logging.info("BLE: Connection succesful to: {}".format(self.sensor_addr))
        return True
    except Exception as err:
        logging.error("BLE: Error in connecting to device: {}".format(err))
        return False


# Main loop
def main():
    print("main started")
    # Setup logging
    setup_logging()

    # Initialize variables to detect whether connection has been established once
    connection = False
    client_ID = None
    client_sock = None

    # We need to wait until Bluetooth init is done
    time.sleep(10)

    # Make device visible
    os.system("sudo hciconfig hci0 piscan")

    # Create a new server socket using RFCOMM protocol
    server_sock = BluetoothSocket(RFCOMM)
    # Bind to any port
    server_sock.bind(("", PORT_ANY))
    # Start listening
    server_sock.listen(1)

    # Get the port the server socket is listening
    port = server_sock.getsockname()[1]

    # The service UUID to advertise
    uuid = "7be1fcb3-5776-42fb-91fd-2ee7b5bbb86d"

    # Start advertising the service
    advertise_service(server_sock, "RaspiBtSrv",
                      service_id=uuid,
                      service_classes=[uuid, SERIAL_PORT_CLASS],
                      profiles=[SERIAL_PORT_PROFILE])

    # These are the operations the service supports
    # Feel free to add more

    # Main Bluetooth server loop
    while True:
        print("slept enough")
        accept_connection(server_sock)


'''


EDIT THIS PROPERLY:
    Uncomment whether you're using SenseHat or MPU9250


'''
def threaded_bt(conn):
    prev_command = None    # For checking previous control command
    #sensor_object = get_sensor_values.SenseSensorValues()
    sensor_object = get_sensor_values.MPUSensorValues()
    realtime_object = RealTimeThread(sensor_object, conn)

    motor = motor_control.Motor(4, 5)
    motor.set_motors_forward()

    print("Entered threaded client")

    while True:
        data = conn.recv(1024)
        if not data:
            break
        command = data.decode("utf-8")
        print("Received %s" % command)

        if command == "sensorData":
            print("Getting sensor values")
            threading.Thread(target=realtime_object.get_realtime_data).start()

            if conn.recv(1024) is not None:
                realtime_object.stoprequest = False

        if command == "noMonitor":
            Thread(target=stopThread).start()

        # If it's gps add, add identifier as first line of the file
        if command == "getGPS":
            response = "gps\n" + get_gps_data("getGPS")
            conn.send(response)

        if command == "getKalman":
            response = "gps\n" + get_gps_data("getKalman")
            conn.send(response)

        #
        # Motor control commands
        #
        if command == "1":
            motor.motor_turn(350)
            motor.set_motors_forward()
            motor.motors_forward(3)

        if command == "2":
            motor.motor_turn(350)
            motor.set_motors_backward()
            motor.motors_forward(3)

        if command == "3":
            motor.motor_turn(400)
            motor.motors_forward(3)

        if command == "4":
            motor.motor_turn(300)
            motor.motors_forward(3)

        if command == "stop":
            motor.motor_turn(350)
            motor.stop_motors()

    conn.close()


def get_sensor_data(sensor_object):
    ninedof = sensor_object.read_sensors()
    acc_data = " ".join(map(str, ninedof[0]))
    gyro_data = " ".join(map(str, ninedof[1]))
    magneto_data = ninedof[2]
    return acc_data, gyro_data


def get_gps_data(command):
    if command == "getGPS":
        file = open('gpsLONG', "r")
        lon = []
        print("Getting GPS")
        lines = file.readlines()
        for x in lines:
            lon.append(x)
        file.close()
        return np.array2string(np.array(lon), precision=5, separator='\n')
    if command == "getKalman":
        file = open('gpsLAT', "r")
        lat = []
        print("Getting Lat")
        lines = file.readlines()
        for x in lines:
            lat.append(x)
        file.close()
        return np.array2string(np.array(lat), precision=5, separator='\n')


def accept_connection(server_sock):
    print("Waiting for RFCOMM connection!!!")
    client_sock, client_info = server_sock.accept()
    print("client_sock", client_sock)
    print("client_info", client_info)

    start_new_thread(threaded_bt, (client_sock,))


q = Queue()
main()