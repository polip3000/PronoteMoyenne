package fr.algorythmice.pronotemoyenne

import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import fr.algorythmice.pronotemoyenne.grades.NotesFragment
import fr.algorythmice.pronotemoyenne.homeworks.HomeworksFragment
import fr.algorythmice.pronotemoyenne.infos.InfosFragment
import fr.algorythmice.pronotemoyenne.turboself.TurboSelfFragment

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val drawer = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navigationView)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, NotesFragment(), "notesFragment")
            .commit()


        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_notes -> {
                    openFragment(NotesFragment(), "notesFragment")
                }

                R.id.nav_homework -> {
                    openFragment(HomeworksFragment())
                }

                R.id.nav_profil -> {
                    openFragment(InfosFragment())
                }

                R.id.nav_turboself -> {
                    openFragment(TurboSelfFragment(), "turboselfFragment")
                }

            }
            drawer.closeDrawers()
            true
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val fragment = supportFragmentManager.findFragmentByTag("notesFragment") as? NotesFragment
            fragment?.reloadNotes()
        }
    }

    val turboSelfLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val fragment = supportFragmentManager.findFragmentByTag("turboselfFragment") as? TurboSelfFragment
            fragment?.refreshUIAfterLogin()
        }
    }



    private fun openFragment(fragment: Fragment, tag: String? = null) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
    }

}
