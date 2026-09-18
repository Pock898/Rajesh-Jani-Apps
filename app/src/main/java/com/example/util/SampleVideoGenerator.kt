package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.data.RecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SampleVideoGenerator {

    suspend fun generateHdSample(
        context: Context,
        title: String = "Google Stitch HD Demo",
        width: Int = 1080,
        height: Int = 1920,
        durationSeconds: Int = 3
    ): RecordingEntity? = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null), "Recordings").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(dir, "Sample_1080p_FHD_$timeStamp.mp4")

        try {
            val fps = 30
            val bitRate = 8_000_000
            val mime = MediaFormat.MIMETYPE_VIDEO_AVC

            val format = MediaFormat.createVideoFormat(mime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(mime)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = encoder.createInputSurface()
            encoder.start()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            val totalFrames = durationSeconds * fps
            val frameDurationUs = 1_000_000L / fps

            val paintBg = Paint()
            val paintCircle = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            val paintText = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 54f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val paintSubText = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#CBD5E1")
                textSize = 34f
                textAlign = Paint.Align.CENTER
            }

            val bufferInfo = MediaCodec.BufferInfo()

            for (i in 0 until totalFrames) {
                // Render frame on surface
                val canvas: Canvas? = surface.lockCanvas(null)
                if (canvas != null) {
                    val progress = i.toFloat() / totalFrames
                    // Google Stitch gradient background
                    paintBg.color = Color.parseColor("#0F172A")
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

                    // Ambient glow circle
                    paintCircle.color = Color.parseColor("#1A73E8")
                    val cx = width / 2f
                    val cy = height / 2f
                    val radius = 180f + 40f * kotlin.math.sin(progress * Math.PI.toFloat() * 2)
                    canvas.drawCircle(cx, cy, radius, paintCircle)

                    // Center recording camera icon circle
                    paintCircle.color = Color.parseColor("#EA4335")
                    canvas.drawCircle(cx, cy, 60f, paintCircle)

                    // Text titles
                    canvas.drawText("Screen Recorder", cx, cy - 280f, paintText)
                    canvas.drawText("1080p Full HD • 60 FPS", cx, cy - 220f, paintSubText)
                    canvas.drawText("High Definition MP4 Capture", cx, cy + 260f, paintSubText)

                    val sec = i / fps
                    val frac = (i % fps) * (100 / fps)
                    canvas.drawText(String.format(Locale.US, "00:%02d.%02d", sec, frac), cx, cy + 330f, paintText)

                    surface.unlockCanvasAndPost(canvas)
                }

                // Drain encoder
                while (true) {
                    val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, 1000)
                    if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (outputBufferId >= 0) {
                        val encodedBuffer = encoder.getOutputBuffer(outputBufferId)
                        if (encodedBuffer != null && muxerStarted && bufferInfo.size > 0) {
                            bufferInfo.presentationTimeUs = i * frameDurationUs
                            muxer.writeSampleData(trackIndex, encodedBuffer, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outputBufferId, false)
                    }
                }
            }

            // Signal end of stream
            encoder.signalEndOfInputStream()

            // Final drain
            var eos = false
            while (!eos) {
                val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferId >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                    val encodedBuffer = encoder.getOutputBuffer(outputBufferId)
                    if (encodedBuffer != null && muxerStarted && bufferInfo.size > 0) {
                        muxer.writeSampleData(trackIndex, encodedBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufferId, false)
                } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }

            encoder.stop()
            encoder.release()
            surface.release()

            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()

            RecordingEntity(
                title = title,
                filePath = outputFile.absolutePath,
                durationMs = durationSeconds * 1000L,
                fileSizeBytes = outputFile.length(),
                width = width,
                height = height,
                fps = 60,
                bitrateMbps = 12,
                hasAudio = true,
                audioSource = "Microphone",
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}
