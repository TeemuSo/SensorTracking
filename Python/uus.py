#!/usr/bin/python

import logging
import logging.handlers
import argparse
import sys
import os
import time
import numpy as np
import socket

# Import objects
import get_sensor_values

from _thread import *
from sense_hat import SenseHat
from bluetooth import *


class LoggerHelper(object):
    def __init__(self, logger, level):
        self.logger = logger
        self.level = level

    def write(self, message):
        if message.rstrip() != "":
            self.logger.log(self.level, message.rstrip())


def setup_logging():
    # Default logging settings
    LOG_FILE = "/home/pi/PythonPractise/BTApp/raspibtsrv.log"
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


def threaded_bt(conn):
    print("Entered threaded client")

    while True:
        data = conn.recv(1024)
        if not data:
            break
        command = data.decode("utf-8")
        print("Received %s" % command)

        if command == "sensorData":
            sensor_object = get_sensor_values.SensorValues()
            print("Getting sensor values")
            initial_command = command
            while True:
                acc_data = get_sensor_data(sensor_object)
                conn.send(acc_data)
                time.sleep(0.3)

        # If it's gps add, add identifier as first line of the file
        if command == "getGPS":
            response = "gps\n" + get_gps_data()
            conn.send(response)

    conn.close()


def get_sensor_data(sensor_object):
    ninedof = sensor_object.read_sensors()
    acc_data = ninedof[0]
    gyro_data = ninedof[1]
    magneto_data = ninedof[2]
    return acc_data


def get_gps_data():
    file = open('gpsLONG', "r")
    lon = []
    print("Getting GPS")
    lines = file.readlines()
    for x in lines:
        lon.append(x)
    file.close()
    return np.array2string(np.array(lon), precision=5, separator='\n')


def accept_connection(server_sock):

    print("Waiting for RFCOMM connection!!!")
    client_sock, client_info = server_sock.accept()
    print("client_sock", client_sock)
    print("client_info", client_info)

    start_new_thread(threaded_bt, (client_sock,))
    server_sock.close()


# Create objects

main()

'''

        OLD STRUCTURE
        
        
        print("Waiting for connection on RFCOMM channel %d" % port)

        try:
            print("entered try")
            #client_sock = None
            msg_sent = "testiboii"

            # This will block until we get a new connection
            if not connection:
                client_sock, client_info = server_sock.accept()
                print("Accepted connection from ", client_info)
                print("read client_sock: ", client_sock)
                client_ID = client_info
                connection = True
            else:
                print("moved to else statement. client id:", client_ID)
                server_sock.connect(client_ID)
                print("Established connection again")

            # Read the data sent by the client
            data = client_sock.recv(1024)
            if len(data) == 0:
                break
            command = data.decode("utf-8")
            print("Received %s" % command)

            # Handle the request
            if command == "getGPS":
                file = open('gpsLONG', "r")
                lon = []
                print("Getting GPS")
                lines = file.readlines()
                for x in lines:
                    lon.append(x)
                file.close()
                response = np.array2string(np.array(lon), precision=5, separator='\n')
            elif command == "getIMU":
                file = open('sensor_data', "r")
                data = []
                print("Getting IMU")
                lines = file.readlines()
                for x in lines:
                    data.append(x)
                file.close()
                response = np.array2string(np.array(data), precision=4, separator='\n')
            else:
                response = "invalid command. not recognized"
            client_sock.send(response)
            print(response)
            client_sock.send(msg_sent)
            print("Sent raw_input: {}", msg_sent)

        except IOError:
            pass

        except KeyboardInterrupt:
            if client_sock is not None:
                client_sock.close()

            server_sock.close()

            print("Server going down")
            break
'''
