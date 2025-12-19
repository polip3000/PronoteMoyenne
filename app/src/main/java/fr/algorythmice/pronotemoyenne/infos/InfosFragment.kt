package fr.algorythmice.pronotemoyenne.infos

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import fr.algorythmice.pronotemoyenne.R
import fr.algorythmice.pronotemoyenne.databinding.FragmentInfosBinding

class InfosFragment : Fragment(R.layout.fragment_infos) {

    private var _bind: FragmentInfosBinding? = null
    private val bind get() = _bind!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _bind = FragmentInfosBinding.bind(view)

        val context = requireContext()

        val studentName = InfosCacheStorage.getStudentName(context)
        val className = InfosCacheStorage.getClassName(context)
        val establishment = InfosCacheStorage.getEstablishment(context)

        if (studentName == null && className == null && establishment == null) {
            bind.emptyText.visibility = View.VISIBLE
            bind.infoCard.visibility = View.GONE
        } else {
            bind.emptyText.visibility = View.GONE
            bind.infoCard.visibility = View.VISIBLE

            bind.studentName.text = studentName ?: "—"
            bind.className.text = className ?: "—"
            bind.establishment.text = establishment ?: "—"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bind = null
    }
}
