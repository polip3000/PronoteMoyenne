from collections import defaultdict
from pronotepy import Client
from pronotepy.ent import *

def get_notes(url, username, password, ent):
    try:
        ent_class = globals()[ent]
    except KeyError:
        raise ValueError(f"ENT '{ent}' inconnu. Choisir parmi : {list(globals().keys())}")

    client = Client(url, username=username, password=password, ent=ent_class)
    period = client.current_period

    notes_par_matiere = defaultdict(list)

    for grade in period.grades:
        nom_matiere = grade.subject.name
        notes_par_matiere[nom_matiere].append(grade)

    output = ""

    for matiere, grades in notes_par_matiere.items():
        output += f"\nMatière : {matiere}\n"
        for g in grades:
            output += f"{g.grade}/{g.out_of}  (coef: {g.coefficient})\n"

    return output
