import pronotepy
from pronotepy.ent import ent_ecollege78

try:
    client = pronotepy.Client("https://0781886B.index-education.net/pronote/eleve.html",
                              username="nina.rinaldi2",
                              password="rinaldin080612",
                              ent=ent_ecollege78)
    if client.logged_in:
        print("connexion réussie")


except Exception as e:
    print(e)