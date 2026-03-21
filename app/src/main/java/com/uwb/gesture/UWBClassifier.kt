package com.uwb.gesture

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10

class UWBClassifier(context: Context) : AutoCloseable {

    private val tag = "UWBClassifier"
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    private val gestureMap = mapOf(
        0  to GestureAction.SWIPE_LR,
        1  to GestureAction.SWIPE_RL,
        2  to GestureAction.SWIPE_UD,
        3  to GestureAction.SWIPE_DU,
        4  to GestureAction.DIAG_LR_UD,
        5  to GestureAction.DIAG_LR_DU,
        6  to GestureAction.DIAG_RL_UD,
        7  to GestureAction.DIAG_RL_DU,
        8  to GestureAction.CLOCKWISE,
        9  to GestureAction.ANTICLOCKWISE,
        10 to GestureAction.INWARD_PUSH,
        11 to GestureAction.EMPTY
    )

    init {
        Log.d(tag, "Loading ONNX model...")
        val modelBytes = context.assets.open("gesture_recognition.onnx").readBytes()
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(4)
        }
        session = env.createSession(modelBytes, options)
        Log.d(tag, "ONNX session created OK")
        Log.d(tag, "Input  name: ${session.inputNames.first()}")
        Log.d(tag, "Output name: ${session.outputNames.first()}")
    }

    /**
     * Preprocesses 3 raw UWB receiver signals into a [1, 3, 128, 64] NCHW tensor.
     * ONNX uses NCHW (same as PyTorch) — no transpose needed.
     *
     * Each FloatArray = 8192 floats (128 time-steps × 64 range-bins), row-major.
     * index = timeStep * 64 + rangeBin
     */

    private fun preprocess(
        left:  FloatArray,
        right: FloatArray,
        top:   FloatArray
    ): FloatArray {
        require(left.size == 8192 && right.size == 8192 && top.size == 8192) {
            "Each receiver array must be 128×64 = 8192 floats"
        }

        val tensor = FloatArray(3 * 128 * 64)
        val channels = arrayOf(left, right, top)

        for (c in 0..2) {
            for (t in 0 until 128) {
                for (r in 0 until 64) {
                    val raw = channels[c][t * 64 + r]
                    var db  = (20.0 * log10(abs(raw.toDouble()) + 1e-9)).toFloat()
                    db = db.coerceIn(-60f, 0f)
                    tensor[c * 128 * 64 + t * 64 + r] = (db + 60f) / 60f
                }
            }
        }

        // Match training pipeline: divide by max if max > 0.01
        val maxVal = tensor.max()
        if (maxVal > 0.01f) {
            for (i in tensor.indices) tensor[i] /= maxVal
        }

        return tensor
    }

    /**
     * Converts raw logits to softmax probabilities.
     */
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.max()
        val expScores = logits.map { exp((it - maxLogit).toDouble()) }
        val sumExp = expScores.sum()
        return FloatArray(logits.size) { i -> (expScores[i] / sumExp).toFloat() }
    }

    /**
     * Full inference: 3 raw receiver arrays → GestureAction
     */
    fun classify(
        left:  FloatArray,
        right: FloatArray,
        top:   FloatArray
    ): GestureAction {
        return try {
            val flatInput = preprocess(left, right, top)
            val shape = longArrayOf(1, 3, 128, 64)

            val inputTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(flatInput),
                shape
            )

            val inputName = session.inputNames.first()
            val results   = session.run(mapOf(inputName to inputTensor))

            val outputValue = results[0].value

            val logits: FloatArray = when (outputValue) {
                is Array<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (outputValue as Array<FloatArray>)[0]
                }
                is FloatArray -> outputValue
                else -> {
                    val tensor = results[0] as OnnxTensor
                    val buf = tensor.floatBuffer
                    FloatArray(buf.remaining()) { buf.get() }
                }
            }

            inputTensor.close()
            results.close()

            // Convert logits → probabilities
            val probabilities = softmax(logits)

            val classIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
            val confidence  = if (classIndex >= 0) probabilities[classIndex] else 0f

            Log.d(tag, "Logits: ${logits.toList()}")
            Log.d(tag, "Probs : ${probabilities.map { "%.3f".format(it) }}")
            Log.d(tag, "Class=$classIndex  conf=${"%.1f".format(confidence * 100)}%")

            // Confidence threshold — lower if model isn't confident enough on real data
            if (confidence < 0.6f) {
                Log.d(tag, "Below threshold — returning EMPTY")
                GestureAction.EMPTY
            } else {
                gestureMap[classIndex] ?: GestureAction.UNKNOWN
            }

        } catch (e: Exception) {
            Log.e(tag, "classify() EXCEPTION: ${e.javaClass.name}: ${e.message}")
            e.printStackTrace()
            GestureAction.UNKNOWN
        }
    }

    override fun close() {
        session.close()
        env.close()
        Log.d(tag, "ONNX session closed")
    }
}