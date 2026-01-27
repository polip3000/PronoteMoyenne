package fr.algorythmice.pronotemoyenne

import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import fr.algorythmice.pronotemoyenne.grades.GradesFragment
import fr.algorythmice.pronotemoyenne.homeworks.HomeworksFragment
import fr.algorythmice.pronotemoyenne.infos.InfosFragment
import fr.algorythmice.pronotemoyenne.turboself.TurboSelfFragment

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Charge le fragment initial (Notes)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, GradesFragment(), "notesFragment")
                .commit()
        }

        // Configuration de la bottom navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_grades -> {
                    openFragment(GradesFragment(), "notesFragment")
                    true
                }
                R.id.nav_homeworks -> {
                    openFragment(HomeworksFragment(), "homeworksFragment")
                    true
                }
                R.id.nav_infos -> {
                    openFragment(InfosFragment(), "infosFragment")
                    true
                }
                R.id.nav_turboself -> {
                    openFragment(TurboSelfFragment(), "turboselfFragment")
                    true
                }
                else -> false
            }
        }

        // Sélectionne l'item des notes par défaut
        bottomNav.selectedItemId = R.id.nav_grades
    }

    @RequiresApi(Build.VERSION_CODES.O)
    val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val fragment = supportFragmentManager.findFragmentByTag("notesFragment") as? GradesFragment
            fragment?.reloadNotes()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    val turboSelfLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val fragment = supportFragmentManager.findFragmentByTag("turboselfFragment") as? TurboSelfFragment
            fragment?.refreshUIAfterLogin()
        }
    }

    private fun openFragment(fragment: Fragment, tag: String? = null) {
        val transaction = supportFragmentManager.beginTransaction()

        // Cache tous les fragments déjà ajoutés
        supportFragmentManager.fragments.forEach {
            transaction.hide(it)
        }

        val existingFragment = tag?.let { supportFragmentManager.findFragmentByTag(it) }

        if (existingFragment != null) {
            // Affiche le fragment existant
            transaction.show(existingFragment)
        } else {
            // Ajoute le nouveau fragment
            transaction.add(R.id.fragmentContainer, fragment, tag)
        }

        transaction.commit()
    }

}
