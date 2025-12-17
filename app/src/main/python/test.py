import pronotepy
from pronotepy.ent import ile_de_france

# Connexion
try:
    client = pronotepy.Client("https://0782549x.index-education.net/pronote/eleve.html",
                              username="alexis.josseaume",
                              password="@Pronote2025",
                              ent=ile_de_france)
    if client.logged_in:
        print("connexion réussie")

except Exception as e:
    print(e)