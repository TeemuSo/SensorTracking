import os.path

class Data:

    file = None

    def open_list(self, filename):
        self.file = open(filename, "w")

    def set_list(self, content):
        self.file.write(content)

    # Open certain file, and get its whole content as string
    def get_list(self, filename):
        self.file = open(filename, "r")
        return file.read()

    def close_list(self):
        self.file.close()