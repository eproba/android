package com.czaplicki.eproba

import android.app.AlertDialog
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentScanExamBinding
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import com.czaplicki.eproba.db.UserDao
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ScanExamFragment : Fragment() {

    private var _binding: FragmentScanExamBinding? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var sharedPreferences: SharedPreferences


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val service: EprobaService = EprobaApplication.instance.service
    private lateinit var baseUrl: String
    private lateinit var user: User
    private val userDao: UserDao = EprobaApplication.instance.database.userDao()
    private val users = mutableListOf<User>()


    private lateinit var authService: AuthorizationService
    private lateinit var mAuthStateManager: AuthStateManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanExamBinding.inflate(inflater, container, false)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        baseUrl = sharedPreferences.getString("server", "https://eproba.pl")!!
        user = Gson().fromJson(sharedPreferences.getString("user", null), User::class.java)
        authService = AuthorizationService(requireContext())
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        binding.refreshButton.visibility = View.VISIBLE
        binding.refreshButton.setOnClickListener {
            binding.refreshButton.visibility = View.GONE
            getImage()
        }
        (requireActivity() as CreateExamActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateUsers()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? CreateExamActivity)?.bottomNavigationView?.setOnItemReselectedListener {
            binding.scrollView.fullScroll(View.FOCUS_UP)
            (activity as? CreateExamActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(
                true
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun extractTasks(
        textBlocks: List<Text.TextBlock>,
        tableTop: Int?,
        averageLineHeight: Float?
    ): MutableList<Task> {
        if (tableTop == null) {
            return mutableListOf()
        }
        val tasks = mutableListOf<Task>()
        for (block in textBlocks) {
            val cornerPoints = block.cornerPoints ?: continue
            if ((cornerPoints[2]?.y
                    ?: 0) > tableTop && cornerPoints[3].y > tableTop && block.text.length > 10
            ) {
                if (block.lines.size > 1 && averageLineHeight != null) {
                    if (block.lines.size * 1.5 * averageLineHeight < block.boundingBox!!.height()) {
                        block.lines.forEach { line ->
                            var task = line.text.replace("\n", " ")
                            while (task[0].isDigit() || task[0] == '.') {
                                task = task.substring(1)
                            }
                            task = task.trim()
                            if (task.endsWith(".")) {
                                task = task.substringBeforeLast(".")
                            }
                            if (task.isNotEmpty()) {
                                tasks.add(Task(-1, task))
                            }
                        }
                        continue
                    }
                }
                var task = block.text.replace("\n", " ")
                while (task[0].isDigit() || task[0] == '.') {
                    task = task.substring(1)
                }
                task = task.trim()
                if (task.endsWith(".")) {
                    task = task.substringBeforeLast(".")
                }
                if (task.isNotEmpty()) {
                    tasks.add(Task(-1, task))
                }
            }
        }
        return tasks
    }

    private fun getImage() {
        getImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val getImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(context, "No photo picked", Toast.LENGTH_SHORT).show()
                binding.refreshButton.visibility = View.VISIBLE
                binding.refreshButton.setOnClickListener {
                    binding.textviewCamera.text = ""
                    binding.textviewCamera.visibility = View.GONE
                    binding.submitButton.visibility = View.GONE
                    binding.refreshButton.visibility = View.GONE
                    getImage()
                }
            } else {
                recognizeExam(uri)
            }
        }

    private fun recognizeExam(uri: Uri) {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(requireContext(), uri)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val scannedExam = ScannedExam()
                val resultList = mutableListOf<Text.Line>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        resultList.add(line)
                    }
                }

                for (line in resultList) {
                    val text = line.text.lowercase(Locale.getDefault())
                    when {
                        text.contains("zadani") || ((text.contains("lp") || text.contains("podpis")) && scannedExam.tasksTableTopCoordinate == null) -> {
                            scannedExam.setTaskTableTopCoordinate(line.cornerPoints?.get(3)?.y ?: 0)
                        }

                        (text.contains("próba na") || text.contains("proba na")) && scannedExam.exam.name == null -> scannedExam.exam.name =
                            line.text

                        (text.contains("imię") || text.contains("imie")) && scannedExam.first_name == null -> when {
                            text.contains("imię: ") || text.contains("imie: ") -> scannedExam.setFirstName(
                                line.text.substring(6).trim()
                            )

                            text.contains("imię:") || text.contains("imię ") || text.contains("imie:") || text.contains(
                                "imie "
                            ) -> scannedExam.setFirstName(line.text.substring(5).trim())

                            text.contains("imię") || text.contains("imie") -> scannedExam.setFirstName(
                                line.text.substring(
                                    4
                                ).trim()
                            )
                        }

                        text.contains("nazwisko") && scannedExam.last_name == null -> when {
                            text.contains("nazwisko: ") -> scannedExam.setLastName(
                                line.text.substring(10).trim()
                            )

                            text.contains("nazwisko:") || text.contains("nazwisko ") -> scannedExam.setLastName(
                                line.text.substring(9).trim()
                            )

                            text.contains("nazwisko") -> scannedExam.setLastName(
                                line.text.substring(8).trim()
                            )
                        }

                        text.contains("pseudonim") && scannedExam.nickname == null -> when {
                            text.contains("pseudonim: ") -> scannedExam.setNickname(
                                line.text.substring(11).trim()
                            )

                            text.contains("pseudonim:") || text.contains("pseudonim ") -> scannedExam.setNickname(
                                line.text.substring(10).trim()
                            )

                            text.contains("pseudonim") -> scannedExam.setNickname(
                                line.text.substring(9).trim()
                            )

                        }

                        (text.contains("drużyna") || text.contains("druzyna")) && scannedExam.team == null -> when {
                            text.contains("drużyna: ") || text.contains("druzyna: ") -> scannedExam.setTeam(
                                line.text.substring(9).trim()
                            )

                            text.contains("drużyna:") || text.contains("drużyna ") || text.contains(
                                "druzyna:"
                            ) || text.contains("druzyna ") -> scannedExam.setTeam(
                                line.text.substring(8).trim()
                            )

                            text.contains("drużyna") || text.contains("druzyna") -> scannedExam.setTeam(
                                line.text.substring(7).trim()
                            )
                        }

                        else -> {
                            scannedExam.updateAverageLineHeight(line.boundingBox!!.height())
                        }

                    }

                }
                scannedExam.exam.tasks = extractTasks(
                    visionText.textBlocks,
                    scannedExam.tasksTableTopCoordinate,
                    scannedExam.averageLineHeight
                )
                binding.progressBar.visibility = View.GONE
                binding.textviewCamera.text = scannedExam.toFormattedString()
                binding.textviewCamera.visibility = View.VISIBLE
                if (user.scout.function >= 2) {
                    binding.userSelect.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        (binding.userSelect.editText as MaterialAutoCompleteTextView).setSimpleItems(
                            users.filter { it.scout.teamId == user.scout.teamId }
                                .map { it.nickname }.toTypedArray()
                        )
                        val nicknameCandidates =
                            users.filter { it.nickname?.lowercase() == scannedExam.nickname?.lowercase() && it.nickname != null }
                        val fullNameCandidates =
                            users.filter { it.firstName?.lowercase() == scannedExam.first_name?.lowercase() && it.lastName?.lowercase() == scannedExam.last_name?.lowercase() && it.firstName != null && it.lastName != null }
                        if (nicknameCandidates.isEmpty() && fullNameCandidates.isEmpty()) {
                            (binding.userSelect.editText as MaterialAutoCompleteTextView).setText(
                                user.nickname,
                                false
                            )
                        } else if (nicknameCandidates.size == 1) {
                            (binding.userSelect.editText as MaterialAutoCompleteTextView).setText(
                                nicknameCandidates[0].nickname,
                                false
                            )
                        } else if (fullNameCandidates.size == 1) {
                            (binding.userSelect.editText as MaterialAutoCompleteTextView).setText(
                                nicknameCandidates.find { it.id == fullNameCandidates[0].id }?.nickname,
                                false
                            )
                        } else {
                            (binding.userSelect.editText as MaterialAutoCompleteTextView).setText(
                                "",
                                false
                            )
                        }
                    }
                }
                binding.examName.visibility = View.VISIBLE
                binding.examName.editText?.setText(scannedExam.exam.name)
                binding.submitButton.visibility = View.VISIBLE
                binding.submitButton.setOnClickListener {
                    submitExam(scannedExam.exam)
                }
                binding.refreshButton.visibility = View.VISIBLE
                binding.refreshButton.setOnClickListener {
                    binding.textviewCamera.text = ""
                    binding.textviewCamera.visibility = View.GONE
                    binding.submitButton.visibility = View.GONE
                    binding.refreshButton.visibility = View.GONE
                    getImage()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error processing image:\n$e", Toast.LENGTH_SHORT).show()
                binding.refreshButton.visibility = View.VISIBLE
                binding.refreshButton.setOnClickListener {
                    binding.textviewCamera.text = ""
                    binding.textviewCamera.visibility = View.GONE
                    binding.submitButton.visibility = View.GONE
                    binding.refreshButton.visibility = View.GONE
                    getImage()
                }
            }
        return

    }

    private fun submitExam(exam: Exam) {
        exam.name = binding.examName.editText?.text.toString()
        if (users.isEmpty()) {
            Toast.makeText(context, "Nie udało się pobrać listy użytkowników", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (user.scout.function >= 2 && binding.userSelect.editText?.text.toString() != "") {
            exam.userId =
                users.find { it.nickname == binding.userSelect.editText?.text.toString() }?.id
        } else {
            exam.userId = user.id
        }
        if (exam.name.isNullOrBlank()) {
            val examNameInput: View =
                LayoutInflater.from(context).inflate(R.layout.exam_name_alert, null)
            val mAlertDialog = MaterialAlertDialogBuilder(
                requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle("Uzupełnij dane")
                .setMessage("Jak chcesz nazwać swoją próbę?")
                .setIcon(R.drawable.ic_help)
                .setNegativeButton("Anuluj") { dialog, _ ->
                    dialog.dismiss()
                    Snackbar.make(binding.root, "Próba nie została zapisana", Snackbar.LENGTH_SHORT)
                        .show()
                }
                .setPositiveButton("Zatwierdź", null)
                .setView(examNameInput)
                .show()
            val mPositiveButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            mPositiveButton.setOnClickListener {
                val examName =
                    examNameInput.findViewById<com.google.android.material.textfield.TextInputLayout>(
                        R.id.textField
                    )
                if (examName.editText?.text.toString().isEmpty()) {
                    examName.error = "To pole jest wymagane"
                } else {
                    exam.name = examName.editText?.text.toString()
                    binding.textviewCamera.text = exam.toFormattedString()
                    mAlertDialog.dismiss()
                    submitExam(exam)
                }
            }
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        service.createExam(exam.toJson().toRequestBody("application/json".toMediaType()))
            .enqueue(object : Callback<Exam> {
                override fun onFailure(call: Call<Exam>, t: Throwable) {
                    t.printStackTrace()
                    activity?.runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call<Exam>, response: Response<Exam>) {
                    if (!response.isSuccessful) {
                        activity?.runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                        }
                        when (response.code()) {
                            401, 403 -> {
                                activity?.runOnUiThread {
                                    MaterialAlertDialogBuilder(
                                        requireContext(),
                                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                    )
                                        .setTitle("Próba nie została zapisana")
                                        .setMessage("Nie jesteś autoryzowany do utworzenia próby, spróbuj ponownie, a jeśli problem będźie się dalej powtarzał autoryzuj ponownie aplikację. ${response.code()} ${response.message()}")
                                        .setIcon(R.drawable.ic_error)
                                        .setPositiveButton(R.string.retry) { _, _ ->
                                            binding.progressBar.visibility = View.VISIBLE
                                            mAuthStateManager.current.needsTokenRefresh = true
                                            submitExam(exam)
                                        }
                                        .setNegativeButton(R.string.cancel, null)
                                        .show()
                                }
                            }

                            else -> {
                                activity?.runOnUiThread {
                                    Snackbar.make(
                                        binding.root,
                                        "Error: ${response.code()}",
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                        return
                    }
                    activity?.runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        MaterialAlertDialogBuilder(
                            requireContext(),
                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                        )
                            .setTitle("Próba zapisana")
                            .setMessage("Próba została utworzona")
                            .setIcon(R.drawable.ic_success)
                            .setPositiveButton(
                                "OK"
                            ) { dialog, _ ->
                                dialog.dismiss()
                                activity?.finish()
                            }
                            .show()
                    }

                }
            })

    }


    private fun updateUsers() {
        lifecycleScope.launch {
            users.clear()
            users.addAll(EprobaApplication.instance.apiHelper.getUsers())
        }
    }


}