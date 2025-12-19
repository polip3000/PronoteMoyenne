package fr.algorythmice.pronotemoyenne

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import fr.algorythmice.pronotemoyenne.grades.NotesFragment
import fr.algorythmice.pronotemoyenne.infos.InfosFragment

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val drawer = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navigationView)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, NotesFragment())
            .commit()

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_notes -> {
                    openFragment(NotesFragment())
                }
                R.id.nav_profil -> {
                    openFragment(InfosFragment())
                }

            }
            drawer.closeDrawers()
            true
        }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
