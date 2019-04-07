import gps
import time
import mtk3339 as mt


# Listen on port 2947 (gpsd) of localhost

class GPS:

    latitude = None
    longitude = None

    def __init__(self):
        self.session = gps.gps("localhost", "2947")
        self.session.stream(gps.WATCH_ENABLE | gps.WATCH_NEWSTYLE)

    def get_gps(self):
        try:
            report = self.session_current.next()
            # Wait for a 'TPV' report and display the current time
            # To see all report data, uncomment the line below
            # print(report)
            if report['class'] == 'TPV':
                if hasattr(report, 'time'):
                    print("at time: " + str(report.time) + ":\n")
                if hasattr(report, 'lon'):
                    print("Found LAT: " + str(report.lon) + ".")
                if hasattr(report, 'lat'):
                    print(" Found LON: " + str(report.lat) + ".\n\n")

                # Assign values to variables
                self.latitude = report.lat
                self.longitude = report.lon
        except KeyError:
            pass
        except KeyboardInterrupt:
            quit()
        except StopIteration:
            self.session = None
            print("GPSD has terminated")

'''gps = GPS()

while True:
    gps.get_gps()
    gps_data = "{} {}\n".format(gps.latitude, gps.longitude)
    data.set_list(gps_data, "coord")

    lat_data = "{}\n".format(gps.latitude)
    lon_data = "{}\n".format(gps.longitude)
    data.set_list(lat_data, "lat_coord")
    data.set_list(lon_data, "lon_coord")
'''