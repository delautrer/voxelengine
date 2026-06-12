package de.delautrer.engine.audio;

import de.delautrer.engine.utils.AssetManager;
import de.delautrer.game.settings.SettingsManager;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AudioEngine {
    private long device;
    private long context;

    private final Map<String, Integer> soundBuffers = new HashMap<>();
    private final Random random = new Random();

    // --- NEU: Das Performance-Wunder (Source Pool) ---
    private static final int MAX_SOURCES = 32; // 32 Gleichzeitige Sounds reichen für 99% aller Situationen
    private final int[] sourcePool = new int[MAX_SOURCES];
    private int nextSourceIndex = 0; // Für Round-Robin (noch schnelleres Finden freier Kanäle)

    public void init() {
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        context = ALC10.alcCreateContext(device, (int[]) null);
        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);
        updateListener();

        // --- NEU: Wir generieren die Hardware-Kanäle EINZIGES MAL beim Start! ---
        for (int i = 0; i < MAX_SOURCES; i++) {
            sourcePool[i] = AL10.alGenSources();
        }
        System.out.println("Initialized with " + MAX_SOURCES + " pooled audio sources.");
    }

    public void updateListener() {
        updateListener(0, 0, 0, 0, 0, -1, 0, 1, 0);
    }

    public void updateListener(float x, float y, float z, float fX, float fY, float fZ, float uX, float uY, float uZ) {
        float masterVolume = SettingsManager.get().masterVolume;
        boolean isFocused = org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(de.delautrer.engine.Engine.get().getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_FOCUSED) == org.lwjgl.glfw.GLFW.GLFW_TRUE;
        if (!isFocused) {
            masterVolume = 0.0f;
        }
        AL10.alListenerf(AL10.AL_GAIN, masterVolume);
        AL10.alListener3f(AL10.AL_POSITION, x, y, z);
        float[] orientation = {fX, fY, fZ, uX, uY, uZ};
        AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
    }

    public void loadSound(String filepath) {
        if (soundBuffers.containsKey(filepath)) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channelsBuffer = stack.mallocInt(1);
            IntBuffer sampleRateBuffer = stack.mallocInt(1);

            ByteBuffer fileBuffer = AssetManager.loadResource(filepath);
            ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(fileBuffer, channelsBuffer, sampleRateBuffer);

            if (pcm == null) {
                System.err.println("Warnung: Sound defekt oder nicht lesbar: " + filepath);
                return;
            }

            int format = (channelsBuffer.get(0) == 1) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            int bufferId = AL10.alGenBuffers();
            AL10.alBufferData(bufferId, format, pcm, sampleRateBuffer.get(0));

            org.lwjgl.system.libc.LibCStdlib.free(pcm);
            soundBuffers.put(filepath, bufferId);
        } catch (Exception e) {
            System.err.println("Error loading sound " + filepath + ": " + e.getMessage());
        }
    }

    private void playBuffer(int bufferId, float volume, float pitch) {
        playBuffer(bufferId, volume, pitch, 0, 0, 0, true);
    }

    private void playBuffer(int bufferId, float volume, float pitch, float x, float y, float z, boolean relative) {
        float sfxVolume = SettingsManager.get().sfxVolume;
        boolean isFocused = org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(de.delautrer.engine.Engine.get().getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_FOCUSED) == org.lwjgl.glfw.GLFW.GLFW_TRUE;
        if (sfxVolume <= 0.01f || !isFocused) return;

        // --- NEU: Freien Kanal im Pool suchen (Round-Robin Methode) ---
        int chosenSource = -1;

        for (int i = 0; i < MAX_SOURCES; i++) {
            int sourceId = sourcePool[nextSourceIndex];

            // Zeiger weiterschieben
            nextSourceIndex = (nextSourceIndex + 1) % MAX_SOURCES;

            // Prüfen, ob der Kanal gerade schweigt
            int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
            if (state != AL10.AL_PLAYING) {
                chosenSource = sourceId;
                break; // Gefunden! Schleife sofort abbrechen
            }
        }

        // Wenn wir extrem viele Sounds abspielen (mehr als 32), wird der 33. Sound einfach ignoriert,
        // anstatt das Spiel zum Absturz zu bringen.
        if (chosenSource == -1) return;

        // Sound auf den freien Kanal legen und abfeuern
        AL10.alSourcei(chosenSource, AL10.AL_BUFFER, bufferId);
        AL10.alSourcef(chosenSource, AL10.AL_GAIN, sfxVolume * volume);
        AL10.alSourcef(chosenSource, AL10.AL_PITCH, pitch);
        AL10.alSourcei(chosenSource, AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
        AL10.alSource3f(chosenSource, AL10.AL_POSITION, x, y, z);
        AL10.alSourcef(chosenSource, AL10.AL_REFERENCE_DISTANCE, 3.0f); // Max volume within 3 blocks
        AL10.alSourcef(chosenSource, AL10.AL_ROLLOFF_FACTOR, 6.0f); // High rolloff for silent after ~9 blocks
        AL10.alSourcePlay(chosenSource);
    }

    public void playRandomFromList(List<String> filepaths, float volumeMult) {
        playRandomFromList(filepaths, volumeMult, 0.9f, 1.1f, 0, 0, 0, true);
    }

    public void playRandomFromList(List<String> filepaths, float volumeMult, float minPitch, float maxPitch, float x, float y, float z, boolean relative) {
        if (filepaths == null || filepaths.isEmpty()) return;

        String chosenPath = filepaths.get(random.nextInt(filepaths.size()));

        Integer bufferId = soundBuffers.get(chosenPath);
        if (bufferId != null) {
            float randomPitch = minPitch + (random.nextFloat() * (maxPitch - minPitch));
            playBuffer(bufferId, volumeMult, randomPitch, x, y, z, relative);
        } else {
            loadSound(chosenPath);
            bufferId = soundBuffers.get(chosenPath);
            if (bufferId != null) playBuffer(bufferId, volumeMult, 1.0f, x, y, z, relative);
        }
    }

    public void cleanup() {
        for (int i = 0; i < MAX_SOURCES; i++) {
            AL10.alSourceStop(sourcePool[i]);
            AL10.alDeleteSources(sourcePool[i]);
        }
        for (int buffer : soundBuffers.values()) AL10.alDeleteBuffers(buffer);
        if (context != 0) ALC10.alcDestroyContext(context);
        if (device != 0) ALC10.alcCloseDevice(device);
    }
}