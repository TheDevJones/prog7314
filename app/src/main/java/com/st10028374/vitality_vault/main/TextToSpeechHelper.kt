package com.st10028374.vitality_vault.main

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

/**
 * Helper class for Text-to-Speech functionality
 * Handles TTS initialization, speech, and cleanup
 */
class TextToSpeechHelper(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val speechQueue = mutableListOf<String>()

    companion object {
        private const val TAG = "TextToSpeechHelper"
    }

    /**
     * Initialize TextToSpeech engine
     */
    fun initialize(onInitialized: ((Boolean) -> Unit)? = null) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported")
                    isInitialized = false
                    onInitialized?.invoke(false)
                } else {
                    Log.d(TAG, "TTS initialized successfully")
                    isInitialized = true

                    // Set speech parameters
                    textToSpeech?.setPitch(1.0f)
                    textToSpeech?.setSpeechRate(1.0f)

                    // Set utterance listener
                    setupUtteranceListener()

                    onInitialized?.invoke(true)

                    // Process any queued speech
                    processSpeechQueue()
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
                isInitialized = false
                onInitialized?.invoke(false)
            }
        }
    }

    /**
     * Setup utterance progress listener for tracking speech events
     */
    private fun setupUtteranceListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Speech started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Speech completed: $utteranceId")
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Speech error: $utteranceId")
            }
        })
    }

    /**
     * Speak text immediately
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, queueing speech")
            speechQueue.add(text)
            initialize()
            return
        }

        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = UUID.randomUUID().toString()

        textToSpeech?.speak(text, queueMode, params)
    }

    /**
     * Speak text with priority (interrupts current speech)
     */
    fun speakImmediate(text: String) {
        speak(text, TextToSpeech.QUEUE_FLUSH)
    }

    /**
     * Add text to speech queue
     */
    fun speakQueued(text: String) {
        speak(text, TextToSpeech.QUEUE_ADD)
    }

    /**
     * Process queued speech requests
     */
    private fun processSpeechQueue() {
        if (speechQueue.isNotEmpty()) {
            val queuedText = speechQueue.joinToString(". ")
            speechQueue.clear()
            speak(queuedText)
        }
    }

    /**
     * Stop current speech
     */
    fun stop() {
        textToSpeech?.stop()
    }

    /**
     * Set speech rate (0.5 to 2.0)
     */
    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate)
    }

    /**
     * Set pitch (0.5 to 2.0)
     */
    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch)
    }

    /**
     * Check if TTS is speaking
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }

    /**
     * Announce countdown (3, 2, 1, Go!)
     */
    fun announceCountdown(count: Int) {
        when (count) {
            3, 2, 1 -> speak(count.toString(), TextToSpeech.QUEUE_ADD)
            0 -> speak("Go!", TextToSpeech.QUEUE_ADD)
        }
    }

    /**
     * Announce workout start
     */
    fun announceWorkoutStart(workoutType: String, duration: String, intensity: String) {
        val announcement = "Starting your $intensity intensity $workoutType workout. " +
                "Goal duration is $duration. Let's get started!"
        speak(announcement)
    }

    /**
     * Announce workout milestone
     */
    fun announceWorkoutMilestone(minutes: Int, workoutType: String) {
        speak("$minutes minutes completed in your $workoutType workout. Keep going!")
    }

    /**
     * Announce workout completion
     */
    fun announceWorkoutComplete(
        workoutType: String,
        duration: String,
        calories: String
    ) {
        val announcement = "Workout complete! You finished your $workoutType in $duration " +
                "and burned $calories. Great job!"
        speak(announcement)
    }

    /**
     * Announce current stats during workout
     */
    fun announceWorkoutStats(
        distance: String,
        pace: String,
        heartRate: String,
        calories: String
    ) {
        val announcement = buildString {
            append("Current stats: ")
            if (distance.isNotEmpty()) append("Distance $distance. ")
            if (pace.isNotEmpty()) append("Pace $pace per kilometer. ")
            if (heartRate.isNotEmpty()) append("Heart rate $heartRate beats per minute. ")
            if (calories.isNotEmpty()) append("Calories burned $calories.")
        }
        speak(announcement)
    }

    /**
     * Announce route progress
     */
    fun announceRouteProgress(
        distance: String,
        duration: String,
        pace: String
    ) {
        speak("You've covered $distance in $duration. Current pace is $pace per kilometer.")
    }

    /**
     * Announce social interaction
     */
    fun announceSocialEvent(userName: String, action: String) {
        speak("$userName $action your post")
    }

    /**
     * Read notification aloud
     */
    fun readNotification(title: String, message: String) {
        speak("$title. $message")
    }

    /**
     * Shutdown TTS engine
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        speechQueue.clear()
        Log.d(TAG, "TTS shutdown")
    }
}