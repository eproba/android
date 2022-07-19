package com.czaplicki.eproba

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.czaplicki.eproba.databinding.FragmentCameraBinding
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val client = OkHttpClient()

    companion object {
        val MEDIA_TYPE_MARKDOWN = "text/x-markdown; charset=utf-8".toMediaType()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        pickImage()
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_CameraFragment_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun extractTasks(textBlocks: List<Text.TextBlock>, tableTop: Int?, averageLineHeight: Float?): MutableList<String> {
        if (tableTop == null) {
            return mutableListOf()
        }
        val tasks = mutableListOf<String>()
        for (block in textBlocks) {
            val cornerPoints = block.cornerPoints ?: continue
            if ((cornerPoints[2]?.y ?: 0) > tableTop && cornerPoints[3].y > tableTop && block.text.length > 10) {
                if (block.lines.size > 1 && averageLineHeight != null) {
                    if (block.lines.size * 1.5 * averageLineHeight < block.boundingBox!!.height()) {
                        block.lines.forEach { line ->
                            var task = line.text.replace("\n"," ")
                            while (task[0].isDigit() || task[0] == '.') {
                                task = task.substring(1)
                            }
                            task = task.trim()
                            if (task.endsWith(".")) {
                                task = task.substringBeforeLast(".")
                            }
                            if (task.isNotEmpty()) {
                                tasks.add(task)
                            }
                        }
                        continue
                    }
                }
                var task = block.text.replace("\n"," ")
                while (task[0].isDigit() || task[0] == '.') {
                    task = task.substring(1)
                }
                task = task.trim()
                if (task.endsWith(".")) {
                    task = task.substringBeforeLast(".")
                }
                if (task.isNotEmpty()) {
                    tasks.add(task)
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

    private val getContentOld = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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
                        (text.contains("próba na") || text.contains("proba na")) && exam.name == null -> exam.name = line.text
                        (text.contains("imię") || text.contains("imie")) && exam.first_name == null -> when {
                            text.contains("imię: ") || text.contains("imie: ") -> exam.setFirstName(line.text.substring(6).trim())
                            text.contains("imię:") || text.contains("imię ") || text.contains("imie:") || text.contains("imie ") -> exam.setFirstName(line.text.substring(5).trim())
                            text.contains("imię") || text.contains("imie") -> exam.setFirstName(line.text.substring(4).trim())
                        }
                        text.contains("nazwisko") && exam.last_name == null -> when {
                            text.contains("nazwisko: ") -> exam.setLastName(line.text.substring(10).trim())
                            text.contains("nazwisko:") || text.contains("nazwisko ") -> exam.setLastName(line.text.substring(9).trim())
                            text.contains("nazwisko") -> exam.setLastName(line.text.substring(8).trim())
                        }
                        text.contains("pseudonim") && exam.nickname == null -> when {
                            text.contains("pseudonim: ") -> exam.setNickname(line.text.substring(11).trim())
                            text.contains("pseudonim:") || text.contains("pseudonim ") -> exam.setNickname(line.text.substring(10).trim())
                            text.contains("pseudonim") -> exam.setNickname(line.text.substring(9).trim())

                        }
                        (text.contains("drużyna") || text.contains("druzyna")) && exam.team == null -> when {
                            text.contains("drużyna: ") || text.contains("druzyna: ") -> exam.setTeam(line.text.substring(9).trim())
                            text.contains("drużyna:") || text.contains("drużyna ") || text.contains("druzyna:") || text.contains("druzyna ") -> exam.setTeam(line.text.substring(8).trim())
                            text.contains("drużyna") || text.contains("druzyna") -> exam.setTeam(line.text.substring(7).trim())
                        }
                        else -> {
                            exam.updateAverageLineHeight(line.boundingBox!!.height())
                        }

                    }

                }
                exam.tasks = extractTasks(visionText.textBlocks, exam.tasksTableTopCoordinate, exam.averageLineHeight)
                binding.progressBar.visibility = View.GONE
                binding.textviewCamera.text = exam.toFormattedString()
                binding.textviewCamera.visibility = View.VISIBLE
                binding.submitButton.visibility = View.VISIBLE
                binding.submitButton.setOnClickListener {
                    Snackbar.make(it, "Not implemented yet", Snackbar.LENGTH_SHORT).show()
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

    fun submitExam(exam: Exam) {
        // send post request to server using okhttp
//        val json = exam.toJson()


        val request = Request.Builder()
            .url("https://scouts-exams.herokuapp.com/api/exam/")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }

                    Log.d("Response", response.body!!.string())
                }
            }
        })
    }
}