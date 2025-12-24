import datetime
from collections import defaultdict
from pronotepy import ParentClient
from pronotepy.ent import *

def get_notes(url, username, password, ent):

    # ------------ CONNECT CLIENT ------------
    if ent == "no_ent":
        client = ParentClient(url, username=username, password=password)
    else:
        try:
            ent_class = globals()[ent]
        except KeyError:
            raise ValueError(f"ENT '{ent}' inconnu")

        client = ParentClient(url,
                              username=username,
                              password=password,
                              ent=ent_class)
    period = client.current_period

    # ---------------- GRADES ----------------

    notes_par_matiere = defaultdict(list)

    for grade in period.grades:
        notes_par_matiere[grade.subject.name].append(grade)

    grade_text = ""

    for matiere, grades in notes_par_matiere.items():
        grade_text += f"\nMatière : {matiere}\n"
        for g in grades:
            grade_text += f"{g.grade}/{g.out_of}  (coef: {g.coefficient})\n"

    class_name = client.info.class_name
    establishment = client.info.establishment
    name = client.info.name

    # ---------------- HOMEWORKS ----------------
    date_from = datetime.date.today()

    homework_text = ""

    for hw in client.homework(date_from):
        homework_text += f"\nDate : {hw.date.isoformat()}\n"
        homework_text += f"Matière : {hw.subject.name}\n"
        homework_text += f"{hw.description.strip()}\n"

    return grade_text, class_name, establishment, name, homework_text
