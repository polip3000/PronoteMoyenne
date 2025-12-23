import os

from TurbApi import TurbApi

def get_qr_code(username, password, path):
    cookie_file = os.path.join(path, "cookies.txt")
    client = TurbApi(username, password, cookie_file)

    qr_number = client.get_qr_payload()

    return qr_number