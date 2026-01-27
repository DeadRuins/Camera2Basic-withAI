/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2.basic.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.android.camera.utils.GenericListAdapter
import com.example.android.camera.utils.decodeExifOrientation
import com.example.android.camera2.basic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import kotlin.math.max
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

class ImageViewerFragment : Fragment() {

    private val args: ImageViewerFragmentArgs by navArgs()

    private val bitmapOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        if (max(outHeight, outWidth) > DOWNSAMPLE_SIZE) {
            val scaleFactorX = outWidth / DOWNSAMPLE_SIZE + 1
            val scaleFactorY = outHeight / DOWNSAMPLE_SIZE + 1
            inSampleSize = max(scaleFactorX, scaleFactorY)
        }
    }

    private val bitmapTransformation: Matrix by lazy { decodeExifOrientation(args.orientation) }
    private val isDepth: Boolean by lazy { args.depth }
    private val bitmapList: MutableList<Bitmap> = mutableListOf()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = ViewPager2(requireContext()).apply {
        offscreenPageLimit = 2
        adapter = GenericListAdapter(
                bitmapList,
                itemViewFactory = {
                    // Fix: Force match_parent layout params to avoid the crash
                    LayoutInflater.from(context).inflate(R.layout.item_image_viewer, null).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                }) { view, item, _ ->
            
            // Fix: Find the specific views inside our XML layout
            val imageView = view.findViewById<ImageView>(R.id.image_view)
            val textView = view.findViewById<TextView>(R.id.ai_description)

            Glide.with(imageView).load(item).into(imageView)

            // AI analysis logic
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    FirebaseApp.initializeApp(requireContext())
                    val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                        .generativeModel("gemini-2.5-flash")

                    val prompt = content {
                        image(item)
                        text("In one short sentence, describe what is in this picture.")
                    }

                    val response = model.generateContent(prompt)

                    withContext(Dispatchers.Main) {
                        textView.text = response.text
                        textView.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AI Error", e)
                    withContext(Dispatchers.Main) {
                        textView.text = "AI Error: ${e.message}"
                        textView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view as ViewPager2
        lifecycleScope.launch(Dispatchers.IO) {
            val inputBuffer = loadInputBuffer()
            addItemToViewPager(view, decodeBitmap(inputBuffer, 0, inputBuffer.size))

            if (isDepth) {
                try {
                    val depthStart = findNextJpegEndMarker(inputBuffer, 2)
                    addItemToViewPager(view, decodeBitmap(
                            inputBuffer, depthStart, inputBuffer.size - depthStart))
                    val confidenceStart = findNextJpegEndMarker(inputBuffer, depthStart)
                    addItemToViewPager(view, decodeBitmap(
                            inputBuffer, confidenceStart, inputBuffer.size - confidenceStart))
                } catch (exc: RuntimeException) {
                    Log.e(TAG, "Depth marker error")
                }
            }
        }
    }

    private fun loadInputBuffer(): ByteArray {
        val inputFile = File(args.filePath)
        return BufferedInputStream(inputFile.inputStream()).use { it.readBytes() }
    }

    private fun addItemToViewPager(view: ViewPager2, item: Bitmap) = view.post {
        bitmapList.add(item)
        view.adapter?.notifyDataSetChanged()
    }

    private fun decodeBitmap(buffer: ByteArray, start: Int, length: Int): Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(buffer, start, length, bitmapOptions)
        return Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, bitmapTransformation, true)
    }

    companion object {
        private val TAG = ImageViewerFragment::class.java.simpleName
        private const val DOWNSAMPLE_SIZE: Int = 1024
        private val JPEG_DELIMITER_BYTES = arrayOf(-1, -39)

        private fun findNextJpegEndMarker(jpegBuffer: ByteArray, start: Int): Int {
            for (i in start until jpegBuffer.size - 1) {
                if (jpegBuffer[i].toInt() == JPEG_DELIMITER_BYTES[0] &&
                        jpegBuffer[i + 1].toInt() == JPEG_DELIMITER_BYTES[1]) {
                    return i + 2
                }
            }
            throw RuntimeException("Separator marker not found")
        }
    }
}
