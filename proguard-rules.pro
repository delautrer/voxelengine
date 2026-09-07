# ==============================================================================
# PROGUARD KONFIGURATION FÜR VOXEL-ENGINE
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Allgemeine Einstellungen
# ------------------------------------------------------------------------------
# Schaltet die extrem speicherfressende Optimierung ab.
# (Verschleierung und das Entfernen ungenutzten Codes passieren trotzdem!)
-dontoptimize

# Warnungen und Hinweise ignorieren (sorgt für einen sauberen Build-Log)
-dontwarn **
-dontnote **
-ignorewarnings

# ------------------------------------------------------------------------------
# 2. Verschleierungs-Wörterbücher (Custom Obfuscation)
# ------------------------------------------------------------------------------
-obfuscationdictionary keywords.txt
-classobfuscationdictionary keywords.txt
-packageobfuscationdictionary keywords.txt

# ------------------------------------------------------------------------------
# 3. Einstiegspunkt & Java-Grundlagen
# ------------------------------------------------------------------------------
# Behalte den Namen der Main-Klasse bei (sonst weiß die .exe nicht, wo sie starten soll!)
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# Behalte wichtige Java-Attribute für Reflection und Gson (Generics, Signatures, Annotations etc.)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Behalte Standard-Methoden von Enums (Lebenswichtig für GSON und Switch-Cases)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ------------------------------------------------------------------------------
# 4. Externe Bibliotheken (Libraries)
# ------------------------------------------------------------------------------
# BEHALTE LWJGL, JOML UND GSON KOMPLETT UNANGETASTET!
# Wenn Vulkan/GLFW oder die Mathe-Bibliothek verschleiert wird, crasht das Spiel!
-keep class org.lwjgl.** { *; }
-keep class org.joml.** { *; }
-keep class com.google.gson.** { *; }

# ------------------------------------------------------------------------------
# 5. Data-Driven Klassen (Für Gson / JSON Parsing)
# ------------------------------------------------------------------------------
# Diese Klassen werden aus JSON-Dateien gelesen. Wenn ProGuard die
# Variablennamen verschleiert oder Annotationen entfernt, findet Gson die Felder
# nicht mehr und wirft Exceptions.

# -- Einstellungen (Settings) --
-keep class de.delautrer.game.settings.** { *; }
-keepclassmembers class de.delautrer.game.settings.** { *; }

# -- Audio & Sounds --
-keep class de.delautrer.engine.audio.data.** { *; }
-keepclassmembers class de.delautrer.engine.audio.data.** { *; }

# -- Blöcke (Block Registry & Block States) --
-keep class de.delautrer.game.blocks.data.** { *; }
-keepclassmembers class de.delautrer.game.blocks.data.** { *; }
-keep class de.delautrer.game.blocks.state.** { *; }
-keepclassmembers class de.delautrer.game.blocks.state.** { *; }

# -- Items (Item Registry) --
-keep class de.delautrer.game.items.data.** { *; }
-keepclassmembers class de.delautrer.game.items.data.** { *; }

# -- Loot Tables --
-keep class de.delautrer.game.loot.** { *; }
-keepclassmembers class de.delautrer.game.loot.** { *; }

# -- Rezepte & Crafting --
-keep class de.delautrer.game.crafting.** { *; }
-keepclassmembers class de.delautrer.game.crafting.** { *; }

# -- Biome & Features --
-keep class de.delautrer.game.world.generation.biome.** { *; }
-keepclassmembers class de.delautrer.game.world.generation.biome.** { *; }
-keep class de.delautrer.game.world.generation.feature.config.** { *; }
-keepclassmembers class de.delautrer.game.world.generation.feature.config.** { *; }

# -- Strukturen & Jigsaw Template Pools --
-keep class de.delautrer.game.world.generation.structure.** { *; }
-keepclassmembers class de.delautrer.game.world.generation.structure.** { *; }
-keep class de.delautrer.game.worldgen.pool.** { *; }
-keepclassmembers class de.delautrer.game.worldgen.pool.** { *; }

# -- Spielstände & Persistence --
-keep class de.delautrer.game.world.persistence.** { *; }
-keepclassmembers class de.delautrer.game.world.persistence.** { *; }

# ------------------------------------------------------------------------------
# 6. Engine Internals (Reflection-Safe)
# ------------------------------------------------------------------------------
# Behalte die Namen deiner Block-Properties, da sie im Code per Reflection
# (oder für State-Mapping) geladen werden könnten.
-keep class de.delautrer.game.blocks.state.BlockProperties$** { *; }