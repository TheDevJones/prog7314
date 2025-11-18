package com.st10028374.vitality_vault.main

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Track
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.st10028374.vitality_vault.R
import com.st10028374.vitality_vault.databinding.FragmentMusicBinding

class MusicFragment : Fragment() {

    private var _binding: FragmentMusicBinding? = null
    private val binding get() = _binding!!

    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var isLiked = false
    private lateinit var audioManager: AudioManager

    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicBinding.inflate(inflater, container, false)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        loadUsername()

        // Use SpotifyManager: if already connected, skip re-auth
        if (SpotifyManager.isConnected()) {
            spotifyAppRemote = SpotifyManager.getRemote()
            setupPlayerControls()
        } else {
            SpotifyManager.connect(requireContext(), { remote ->
                // Check if fragment is still attached before accessing binding
                if (_binding != null) {
                    spotifyAppRemote = remote
                    setupPlayerControls()
                }
            }, { throwable ->
                // Check if fragment is still attached before accessing binding
                if (_binding != null) {
                    Log.e("MusicFragment", "Spotify connection failed: ${throwable.message}")
                    binding.tvSongTitle.text = "Spotify not connected"
                    binding.tvArtistName.text = "Make sure Spotify is installed"
                }
            })
        }
    }

    // Called by MainActivity after Spotify login
    fun onAuthorizationResponse(response: AuthorizationResponse) {
        // Check if fragment is still attached
        if (_binding == null) return

        when (response.type) {
            AuthorizationResponse.Type.TOKEN -> {
                Log.d("MusicFragment", "Spotify Auth successful")
                SpotifyManager.connect(requireContext(), { remote ->
                    if (_binding != null) {
                        spotifyAppRemote = remote
                        setupPlayerControls()
                    }
                }, { throwable ->
                    if (_binding != null) {
                        Log.e("MusicFragment", "Spotify connection failed: ${throwable.message}")
                    }
                })
            }
            AuthorizationResponse.Type.ERROR -> {
                Log.e("MusicFragment", "Spotify Auth failed: ${response.error}")
                binding.tvSongTitle.text = "Spotify authorization failed"
                binding.tvArtistName.text = response.error
            }
            else -> Log.d("MusicFragment", "Spotify auth cancelled or other")
        }
    }

    private fun loadUsername() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid

        // Get display name from Firebase Auth first (for SSO users)
        val displayName = user.displayName
        val email = user.email

        // Set initial letter immediately
        val firstLetter = (displayName?.firstOrNull() ?: email?.firstOrNull() ?: 'U')
        setProfileLetter(firstLetter.toString().uppercase())

        // Then try to get from Firestore for more info
        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                // Check if fragment is still attached
                if (_binding != null) {
                    val firestoreDisplayName = document.getString("displayName")
                    val finalDisplayName = firestoreDisplayName ?: displayName ?: "User"

                    binding.tvUsername.text = finalDisplayName

                    // Update profile letter if we got a different name from Firestore
                    if (firestoreDisplayName != null) {
                        val letter = firestoreDisplayName.firstOrNull()?.toString()?.uppercase() ?: "U"
                        setProfileLetter(letter)
                    }
                }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    binding.tvUsername.text = displayName ?: "User"
                }
            }
    }

    /**
     * Set the profile letter - will use TextView in layout
     */
    private fun setProfileLetter(letter: String) {
        binding.tvProfileAvatar.text = letter
    }

    private fun setupPlayerControls() {
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
            if (_binding != null) {
                updateUI(playerState)
            }
        }

        binding.btnPlayPause.setOnClickListener {
            spotifyAppRemote?.playerApi?.playerState?.setResultCallback { state ->
                if (state.isPaused) spotifyAppRemote?.playerApi?.resume()
                else spotifyAppRemote?.playerApi?.pause()
            }
        }

        binding.btnNext.setOnClickListener { spotifyAppRemote?.playerApi?.skipNext() }
        binding.btnPrevious.setOnClickListener { spotifyAppRemote?.playerApi?.skipPrevious() }

        setupSeekBar()
        setupVolumeControl()
        setupLikeButton()
    }

    private fun updateUI(playerState: PlayerState) {
        // Extra safety check
        if (_binding == null) return

        val track: Track? = playerState.track
        track?.let {
            binding.tvSongTitle.text = it.name
            binding.tvArtistName.text = it.artist.name
            binding.tvAlbumName.text = it.album.name

            spotifyAppRemote?.imagesApi?.getImage(it.imageUri)?.setResultCallback { bitmap ->
                if (_binding != null) {
                    binding.ivAlbumArt.setImageBitmap(bitmap)
                }
            }
        }

        val position = playerState.playbackPosition
        val duration = track?.duration ?: 0
        binding.seekBar.max = duration.toInt()
        binding.seekBar.progress = position.toInt()

        binding.tvCurrentTime.text = formatTime(position)
        binding.tvTotalTime.text = formatTime(duration)

        if (playerState.isPaused) {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            stopSeekBarUpdates()
        } else {
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            startSeekBarUpdater()
        }
    }

    private fun setupSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && _binding != null) {
                    spotifyAppRemote?.playerApi?.seekTo(progress.toLong())
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupLikeButton() {
        binding.btnLike.setOnClickListener {
            isLiked = !isLiked
            binding.btnLike.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
            )
        }
    }

    private fun setupVolumeControl() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        binding.volumeSeekBar.max = maxVolume
        binding.volumeSeekBar.progress = currentVolume

        // Initial icon update
        updateVolumeIcon(currentVolume, maxVolume)

        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                    if (_binding != null) {
                        updateVolumeIcon(progress, maxVolume)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Mute toggle
        binding.btnVolume.setOnClickListener {
            if (binding.volumeSeekBar.progress > 0) {
                binding.volumeSeekBar.tag = binding.volumeSeekBar.progress
                binding.volumeSeekBar.progress = 0
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                updateVolumeIcon(0, maxVolume)
            } else {
                val previousVolume = binding.volumeSeekBar.tag as? Int ?: (maxVolume / 2)
                binding.volumeSeekBar.progress = previousVolume
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)
                updateVolumeIcon(previousVolume, maxVolume)
            }
        }
    }

    private fun updateVolumeIcon(volume: Int, maxVolume: Int) {
        if (_binding == null) return

        val volumePercentage = (volume.toFloat() / maxVolume.toFloat()) * 100
        when {
            volume == 0 -> binding.btnVolume.setImageResource(R.drawable.ic_volume_mute)
            volumePercentage < 50 -> binding.btnVolume.setImageResource(R.drawable.ic_volume_low)
            else -> binding.btnVolume.setImageResource(R.drawable.ic_volume)
        }
    }

    private fun startSeekBarUpdater() {
        stopSeekBarUpdates()
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return

                spotifyAppRemote?.playerApi?.playerState?.setResultCallback { state ->
                    if (_binding == null) return@setResultCallback

                    val track = state.track ?: return@setResultCallback
                    val position = state.playbackPosition
                    val duration = track.duration

                    binding.seekBar.max = duration.toInt()
                    binding.seekBar.progress = position.toInt()
                    binding.tvCurrentTime.text = formatTime(position)
                    binding.tvTotalTime.text = formatTime(duration)

                    if (!state.isPaused) handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(updateSeekBarRunnable!!)
    }

    private fun stopSeekBarUpdates() {
        updateSeekBarRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onStop() {
        super.onStop()
        stopSeekBarUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopSeekBarUpdates()
        _binding = null
    }
}