package com.czaplicki.eproba

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.databinding.FragmentProfileBinding
import com.czaplicki.eproba.db.User
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigInteger
import java.security.MessageDigest


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    private val binding get() = _binding!!

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.actionButton.text = getString(R.string.button_logout)
        binding.actionButton.setOnClickListener {
            mAuthStateManager.replace(AuthState())
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().clear().apply()
            lifecycleScope.launch((Dispatchers.IO)) {
                EprobaApplication.instance.database.clearAllTables()
            }
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
        binding.avatar.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_account
            )
        )
        getUserInfo()
    }

    private fun getRoundedCroppedBitmap(bitmap: Bitmap): Bitmap? {
        val widthLight = bitmap.width
        val heightLight = bitmap.height
        val output = Bitmap.createBitmap(
            bitmap.width, bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val paintColor = Paint()
        paintColor.flags = Paint.ANTI_ALIAS_FLAG
        val rectF = RectF(Rect(0, 0, widthLight, heightLight))
        canvas.drawRoundRect(rectF, (widthLight / 2).toFloat(), (heightLight / 2).toFloat(), paintColor)
        val paintImage = Paint()
        paintImage.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        canvas.drawBitmap(bitmap, Rect(0, 0, widthLight, heightLight), rectF, paintImage)
        return output
    }

    private fun getUserInfo() {
        binding.progressBar.visibility = View.VISIBLE
        EprobaApplication.instance.service.getUserInfo().enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                when (response.code()) {
                    200 -> {
                        if (response.body() == null) {
                            Toast.makeText(
                                requireContext(),
                                "Response body is empty, try again later",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            return
                        }
                        val user: User = response.body()!!
                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .edit()
                            .putString("user", Gson().toJson(response.body()))
                            .apply()
                        activity?.runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            binding.name.text = user.fullName
                        }
                        Request.Builder()
                            .url("https://www.gravatar.com/avatar/${BigInteger(1, MessageDigest.getInstance("MD5").digest(user.email!!.lowercase().toByteArray())).toString(16).padStart(32, '0')}?d=404")
                                .build()
                                .let { request ->
                                    OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
                                        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}

                                        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                                            if (response.isSuccessful) {
                                                activity?.runOnUiThread {
                                                    binding.avatar.setImageBitmap(getRoundedCroppedBitmap(response.body.byteStream().use { android.graphics.BitmapFactory.decodeStream(it) }))
                                                }
                                            } else if (response.code == 404) {
                                                Request.Builder()
                                                    .url("https://api.multiavatar.com/${user.nickname}.png")
                                                    .build()
                                                    .let { request ->
                                                        OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
                                                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}

                                                            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                                                                if (response.isSuccessful) {
                                                                    activity?.runOnUiThread {
                                                                        binding.avatar.setImageBitmap(response.body.byteStream().use { android.graphics.BitmapFactory.decodeStream(it) })
                                                                    }
                                                                }
                                                            }
                                                        })
                                                    }

                                            }
                                        }
                                    })
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
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
