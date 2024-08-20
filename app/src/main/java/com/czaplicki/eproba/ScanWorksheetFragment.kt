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
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentScanWorksheetBinding
import com.czaplicki.eproba.db.Worksheet
import com.czaplicki.eproba.db.WorksheetDao
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
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
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.util.Locale
import java.util.UUID

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ScanWorksheetFragment : Fragment() {

    private var _binding: FragmentScanWorksheetBinding? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var sharedPreferences: SharedPreferences


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val service: EprobaService = EprobaApplication.instance.service
    private lateinit var user: User
    private val worksheetDao: WorksheetDao = EprobaApplication.instance.database.worksheetDao()
    private val users = mutableListOf<User>()


    private lateinit var authService: AuthorizationService
    private lateinit var mAuthStateManager: AuthStateManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanWorksheetBinding.inflate(inflater, container, false)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        user = Gson().fromJson(sharedPreferences.getString("user", null), User::class.java)
        authService = AuthorizationService(requireContext())
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        binding.refreshButton.visibility = View.VISIBLE
        binding.refreshButton.setOnClickListener {
            binding.refreshButton.visibility = View.GONE
            getImage()
        }
        (requireActivity() as CreateWorksheetActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateUsers()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? CreateWorksheetActivity)?.bottomNavigationView?.setOnItemReselectedListener {
            _binding?.scrollView?.fullScroll(View.FOCUS_UP)
            (activity as? CreateWorksheetActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(
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
                    if (block.lines.size * 3 * averageLineHeight < block.boundingBox!!.height()) {
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
                                tasks.add(Task(UUID.fromString("00000000-0000-0000-0000-000000000000"), task))
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
                    tasks.add(Task(UUID.fromString("00000000-0000-0000-0000-000000000000"), task))
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
                    binding.editButton.visibility = View.GONE
                    getImage()
                }
            } else {
                recognizeWorksheet(uri)
            }
        }

    private fun recognizeWorksheet(uri: Uri) {
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
                val scannedWorksheet = ScannedWorksheet()
                val resultList = mutableListOf<Text.Line>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        resultList.add(line)
                    }
                }

                for (line in resultList) {
                    val text = line.text.lowercase(Locale.getDefault())
                    when {
                        text.contains("zadani") || ((text.contains("lp") || text.contains("podpis")) && scannedWorksheet.tasksTableTopCoordinate == null) -> {
                            scannedWorksheet.setTaskTableTopCoordinate(line.cornerPoints?.get(3)?.y ?: 0)
                        }

                        (text.contains("próba na") || text.contains("proba na")) && scannedWorksheet.worksheet.name == null -> scannedWorksheet.worksheet.name =
                            line.text

                        (text.contains("imię") || text.contains("imie")) && scannedWorksheet.first_name == null -> when {
                            text.contains("imię: ") || text.contains("imie: ") -> scannedWorksheet.setFirstName(
                                line.text.substring(6).trim()
                            )

                            text.contains("imię:") || text.contains("imię ") || text.contains("imie:") || text.contains(
                                "imie "
                            ) -> scannedWorksheet.setFirstName(line.text.substring(5).trim())

                            text.contains("imię") || text.contains("imie") -> scannedWorksheet.setFirstName(
                                line.text.substring(
                                    4
                                ).trim()
                            )
                        }

                        text.contains("nazwisko") && scannedWorksheet.last_name == null -> when {
                            text.contains("nazwisko: ") -> scannedWorksheet.setLastName(
                                line.text.substring(10).trim()
                            )

                            text.contains("nazwisko:") || text.contains("nazwisko ") -> scannedWorksheet.setLastName(
                                line.text.substring(9).trim()
                            )

                            text.contains("nazwisko") -> scannedWorksheet.setLastName(
                                line.text.substring(8).trim()
                            )
                        }

                        text.contains("pseudonim") && scannedWorksheet.nickname == null -> when {
                            text.contains("pseudonim: ") -> scannedWorksheet.setNickname(
                                line.text.substring(11).trim()
                            )

                            text.contains("pseudonim:") || text.contains("pseudonim ") -> scannedWorksheet.setNickname(
                                line.text.substring(10).trim()
                            )

                            text.contains("pseudonim") -> scannedWorksheet.setNickname(
                                line.text.substring(9).trim()
                            )

                        }

                        (text.contains("drużyna") || text.contains("druzyna")) && scannedWorksheet.team == null -> when {
                            text.contains("drużyna: ") || text.contains("druzyna: ") -> scannedWorksheet.setTeam(
                                line.text.substring(9).trim()
                            )

                            text.contains("drużyna:") || text.contains("drużyna ") || text.contains(
                                "druzyna:"
                            ) || text.contains("druzyna ") -> scannedWorksheet.setTeam(
                                line.text.substring(8).trim()
                            )

                            text.contains("drużyna") || text.contains("druzyna") -> scannedWorksheet.setTeam(
                                line.text.substring(7).trim()
                            )
                        }

                        else -> {
                            scannedWorksheet.updateAverageLineHeight(line.boundingBox!!.height())
                        }


                    }

                }
                scannedWorksheet.worksheet.tasks = extractTasks(
                    visionText.textBlocks,
                    scannedWorksheet.tasksTableTopCoordinate,
                    scannedWorksheet.averageLineHeight
                )
                binding.progressBar.visibility = View.GONE
                binding.textviewCamera.text = scannedWorksheet.toFormattedString()
                binding.textviewCamera.visibility = View.VISIBLE
                if (user.function >= 2) {
                    binding.userSelect.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        (binding.userSelect.editText as MaterialAutoCompleteTextView).setSimpleItems(
                            users.filter { it.teamId == user.teamId }
                                .map { it.nickname }.toTypedArray()
                        )
                        val nicknameCandidates =
                            users.filter { it.nickname?.lowercase() == scannedWorksheet.nickname?.lowercase() && it.nickname != null }
                        val fullNameCandidates =
                            users.filter { it.firstName?.lowercase() == scannedWorksheet.first_name?.lowercase() && it.lastName?.lowercase() == scannedWorksheet.last_name?.lowercase() && it.firstName != null && it.lastName != null }
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
                binding.worksheetName.visibility = View.VISIBLE
                binding.worksheetName.editText?.setText(scannedWorksheet.worksheet.name)
                binding.submitButton.visibility = View.VISIBLE
                binding.submitButton.setOnClickListener {
                    submitWorksheet(scannedWorksheet.worksheet)
                }
                binding.refreshButton.visibility = View.VISIBLE
                binding.editButton.visibility = View.VISIBLE
                binding.refreshButton.setOnClickListener {
                    binding.textviewCamera.text = ""
                    binding.textviewCamera.visibility = View.GONE
                    binding.submitButton.visibility = View.GONE
                    binding.refreshButton.visibility = View.GONE
                    binding.editButton.visibility = View.GONE
                    getImage()
                }
                binding.editButton.setOnClickListener {
                    val worksheet = scannedWorksheet.worksheet
                    worksheet.name = binding.worksheetName.editText?.text.toString()
                    val bundle = Bundle()
                    bundle.putString("initialData", Gson().toJson(worksheet))
                    bundle.putString(
                        "initialDataNickname",
                        binding.userSelect.editText?.text.toString()
                    )
                    view?.let { it1 ->
                        Navigation.findNavController(it1).navigate(R.id.navigation_compose, bundle)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error processing image:\n$e", Toast.LENGTH_SHORT).show()
                binding.refreshButton.visibility = View.VISIBLE
                binding.editButton.visibility = View.GONE
                binding.refreshButton.setOnClickListener {
                    binding.textviewCamera.text = ""
                    binding.textviewCamera.visibility = View.GONE
                    binding.submitButton.visibility = View.GONE
                    binding.refreshButton.visibility = View.GONE
                    binding.editButton.visibility = View.GONE
                    getImage()
                }
            }
        return

    }

    private fun submitWorksheet(worksheet: Worksheet) {
        worksheet.name = binding.worksheetName.editText?.text.toString()
        if (users.isEmpty()) {
            Toast.makeText(context, "Nie udało się pobrać listy użytkowników", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (user.function >= 2 && binding.userSelect.editText?.text.toString().isNotBlank()) {
            worksheet.userId =
                users.find { it.nickname == binding.userSelect.editText?.text.toString() }?.id
        } else {
            worksheet.userId = user.id
        }
        if (worksheet.name.isNullOrBlank()) {
            val worksheetNameInput: View =
                LayoutInflater.from(context).inflate(R.layout.worksheet_name_alert, null)
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
                .setView(worksheetNameInput)
                .show()
            val mPositiveButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            mPositiveButton.setOnClickListener {
                val worksheetName =
                    worksheetNameInput.findViewById<com.google.android.material.textfield.TextInputLayout>(
                        R.id.textField
                    )
                if (worksheetName.editText?.text.toString().isEmpty()) {
                    worksheetName.error = "To pole jest wymagane"
                } else {
                    worksheet.name = worksheetName.editText?.text.toString()
                    binding.worksheetName.editText?.setText(worksheet.name)
                    binding.textviewCamera.text = worksheet.toFormattedString()
                    mAlertDialog.dismiss()
                    submitWorksheet(worksheet)
                }
            }
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                worksheetDao.insert(
                    service.createWorksheet(
                        worksheet.toJson().toRequestBody("application/json".toMediaType())
                    )
                )
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                    )
                        .setTitle(R.string.worksheet_created)
                        .setIcon(R.drawable.ic_success)
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                            activity?.finish()
                        }
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is HttpException && e.code() == HTTP_FORBIDDEN) {
                    activity?.runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        MaterialAlertDialogBuilder(
                            requireContext(),
                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                        )
                            .setTitle(R.string.error_dialog_title)
                            .setMessage(R.string.worksheet_creating_unauthorized_error)
                            .setIcon(R.drawable.ic_error)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()

                    }
                } else {
                    activity?.runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        MaterialAlertDialogBuilder(
                            requireContext(),
                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                        )
                            .setTitle(R.string.error_dialog_title)
                            .setMessage(getString(R.string.worksheet_creating_error, e))
                            .setIcon(R.drawable.ic_error)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                }
            }
        }
    }


    private fun updateUsers() {
        lifecycleScope.launch {
            users.clear()
            users.addAll(EprobaApplication.instance.apiHelper.getUsers())
        }
    }


}