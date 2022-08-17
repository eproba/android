package com.czaplicki.eproba

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.databinding.FragmentCreateExamBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import net.openid.appauth.AuthorizationService
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class CreateExamFragment : Fragment() {

    private var _binding: FragmentCreateExamBinding? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val client = OkHttpClient()
    private lateinit var baseUrl: String


    private lateinit var authService: AuthorizationService


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCreateExamBinding.inflate(inflater, container, false)
        authService = AuthorizationService(requireContext())
        baseUrl = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString("server", "https://dev.eproba.pl")!!
        binding.refreshButton.visibility = View.VISIBLE
        binding.refreshButton.setOnClickListener {
            binding.refreshButton.visibility = View.GONE
            pickImage()
        }
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_CreateExamFragment_to_FirstFragment)
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
                                tasks.add(Task(task))
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
                    tasks.add(Task(task))
                }
            }
        }
        return tasks
    }

    private fun pickImage() {
        // check sdk version and launch the correct action
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
            intent.type = "image/*"
            getContent.launch(intent)
        } else {
            getContentOld.launch("image/*")
        }
    }

    private val getContentOld =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                recognizeExam(uri)
            } else {
                Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
                binding.refreshButton.visibility = View.VISIBLE
                binding.refreshButton.setOnClickListener {
                    binding.textviewCamera.text = ""
                    binding.textviewCamera.visibility = View.GONE
                    binding.submitButton.visibility = View.GONE
                    binding.refreshButton.visibility = View.GONE
                    pickImage()
                }
            }
        }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = it.data?.data
                if (uri == null) {
                    Toast.makeText(context, "No photo picked", Toast.LENGTH_SHORT).show()
                    binding.refreshButton.visibility = View.VISIBLE
                    binding.refreshButton.setOnClickListener {
                        binding.textviewCamera.text = ""
                        binding.textviewCamera.visibility = View.GONE
                        binding.submitButton.visibility = View.GONE
                        binding.refreshButton.visibility = View.GONE
                        pickImage()
                    }
                    return@registerForActivityResult
                }
                recognizeExam(uri)
            } else {
                Toast.makeText(context, "No photo picked", Toast.LENGTH_SHORT).show()
                binding.refreshButton.visibility = View.VISIBLE
                binding.refreshButton.setOnClickListener {
                    binding.textviewCamera.text = ""
                    binding.textviewCamera.visibility = View.GONE
                    binding.submitButton.visibility = View.GONE
                    binding.refreshButton.visibility = View.GONE
                    pickImage()
                }
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
                val exam = Exam()
                val resultList = mutableListOf<Text.Line>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        resultList.add(line)
                    }
                }

                for (line in resultList) {
                    val text = line.text.lowercase(Locale.getDefault())
                    when {
                        text.contains("zadani") || ((text.contains("lp") || text.contains("podpis")) && exam.tasksTableTopCoordinate == null) -> {
                            exam.setTaskTableTopCoordinate(line.cornerPoints?.get(3)?.y ?: 0)
                        }
                        (text.contains("próba na") || text.contains("proba na")) && exam.name == null -> exam.name =
                            line.text
                        (text.contains("imię") || text.contains("imie")) && exam.first_name == null -> when {
                            text.contains("imię: ") || text.contains("imie: ") -> exam.setFirstName(
                                line.text.substring(6).trim()
                            )
                            text.contains("imię:") || text.contains("imię ") || text.contains("imie:") || text.contains(
                                "imie "
                            ) -> exam.setFirstName(line.text.substring(5).trim())
                            text.contains("imię") || text.contains("imie") -> exam.setFirstName(
                                line.text.substring(
                                    4
                                ).trim()
                            )
                        }
                        text.contains("nazwisko") && exam.last_name == null -> when {
                            text.contains("nazwisko: ") -> exam.setLastName(
                                line.text.substring(10).trim()
                            )
                            text.contains("nazwisko:") || text.contains("nazwisko ") -> exam.setLastName(
                                line.text.substring(9).trim()
                            )
                            text.contains("nazwisko") -> exam.setLastName(
                                line.text.substring(8).trim()
                            )
                        }
                        text.contains("pseudonim") && exam.nickname == null -> when {
                            text.contains("pseudonim: ") -> exam.setNickname(
                                line.text.substring(11).trim()
                            )
                            text.contains("pseudonim:") || text.contains("pseudonim ") -> exam.setNickname(
                                line.text.substring(10).trim()
                            )
                            text.contains("pseudonim") -> exam.setNickname(
                                line.text.substring(9).trim()
                            )

                        }
                        (text.contains("drużyna") || text.contains("druzyna")) && exam.team == null -> when {
                            text.contains("drużyna: ") || text.contains("druzyna: ") -> exam.setTeam(
                                line.text.substring(9).trim()
                            )
                            text.contains("drużyna:") || text.contains("drużyna ") || text.contains(
                                "druzyna:"
                            ) || text.contains("druzyna ") -> exam.setTeam(
                                line.text.substring(8).trim()
                            )
                            text.contains("drużyna") || text.contains("druzyna") -> exam.setTeam(
                                line.text.substring(7).trim()
                            )
                        }
                        else -> {
                            exam.updateAverageLineHeight(line.boundingBox!!.height())
                        }

                    }

                }
                exam.tasks = extractTasks(
                    visionText.textBlocks,
                    exam.tasksTableTopCoordinate,
                    exam.averageLineHeight
                )
                binding.progressBar.visibility = View.GONE
                binding.textviewCamera.text = exam.toFormattedString()
                binding.textviewCamera.visibility = View.VISIBLE
                binding.submitButton.visibility = View.VISIBLE
                binding.submitButton.setOnClickListener {
                    submitExam(exam)
                }
                binding.refreshButton.visibility = View.VISIBLE
                binding.refreshButton.setOnClickListener {
                    binding.textviewCamera.text = ""
                    binding.textviewCamera.visibility = View.GONE
                    binding.submitButton.visibility = View.GONE
                    binding.refreshButton.visibility = View.GONE
                    pickImage()
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
                    pickImage()
                }
            }
        return

    }

    private fun submitExam(exam: Exam) {
        if (baseUrl == "https://eproba.pl") {
            Snackbar.make(
                binding.root,
                "You can't submit exam on main server. Please use a testing server.",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        val authStateManager = AuthStateManager.getInstance(requireContext())

//        if (authStateManager.current.accessToken == null) {
//            Snackbar.make(
//                binding.root,
//                "You are not logged in",
//                Snackbar.LENGTH_LONG
//            ).show()
//            return
//        }

        if (exam.name == null) {
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
                    ).editText?.text.toString()
                if (examName.isEmpty()) {
                    examNameInput.findViewById<com.google.android.material.textfield.TextInputLayout>(
                        R.id.textField
                    ).error = "To pole jest wymagane"
                } else {
                    exam.name = examName
                    binding.textviewCamera.text = exam.toFormattedString()
                    mAlertDialog.dismiss()
                    submitExam(exam)
                }
            }
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        authStateManager.current.performActionWithFreshTokens(
            authService
        ) { accessToken, _, _ ->
            authStateManager.updateSavedState()
            val request = Request.Builder()
                .url("$baseUrl/api/exam/")
                .header(
                    "Authorization",
                    "Bearer $accessToken"
                )
                .method("POST", exam.toJson().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()


            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    activity?.runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            activity?.runOnUiThread {
                                binding.progressBar.visibility = View.GONE
                            }
                            when (response.code) {
                                401, 403 -> {
                                    activity?.runOnUiThread {
                                        MaterialAlertDialogBuilder(
                                            requireContext(),
                                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                        )
                                            .setTitle("Próba nie została zapisana")
                                            .setMessage("Nie jesteś autoryzowany do utworzenia próby, spróbuj ponownie, a jeśli problem będźie się dalej powtarzał autoryzuj ponownie aplikację. ${response.code} ${response.message}")
                                            .setIcon(R.drawable.ic_error)
                                            .setPositiveButton(R.string.retry) { _, _ ->
                                                binding.progressBar.visibility = View.VISIBLE
                                                authStateManager.current.needsTokenRefresh = true
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
                                            "Error: ${response.code}",
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
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }
            })
        }
    }
}