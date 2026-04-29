# Schaltet die extrem speicherfressende Optimierung ab.
# (Verschleierung und das Entfernen ungenutzten Codes passieren trotzdem!)
-dontoptimize

# Warnungen ignorieren
-dontwarn **
-dontnote **

# Verschleierung aktivieren
-obfuscationdictionary keywords.txt
-classobfuscationdictionary keywords.txt
-packageobfuscationdictionary keywords.txt

# Behalte den Namen der Main-Klasse bei (sonst weiß die .exe nicht, wo sie starten soll!)
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# BEHALTE LWJGL, JOML UND GSON KOMPLETT UNANGETASTET!
# Wenn Vulkan/GLFW verschleiert wird, crasht das Spiel!
-keep class org.lwjgl.** { *; }
-keep class org.joml.** { *; }
-keep class com.google.gson.** { *; }


-keepattributes Signature,InnerClasses,EnclosingMethod

# Schütze das komplette Loot-Paket vor jeglicher Umbenennung
-keep class de.delautrer.game.loot.** { *; }

# Falls GSON intern Reflection auf Klassen-Member nutzt
-keepclassmembers class de.delautrer.game.loot.** {
    <fields>;
    <methods>;
}
-keepclassmembers class de.delautrer.game.loot.LootTable { *; }
-keepclassmembers class de.delautrer.game.loot.LootTable$* { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# (Optional aber sicher) Behalte die Namen deiner Block-Properties,
# da sie im Code per Reflection geladen werden könnten
-keep class de.delautrer.game.blocks.state.BlockProperties$** {
    *;
}

# Erlaube GSON, private Felder in deinen Speicher-Klassen zu lesen (WorldData, PlayerData)
-keepclassmembers class de.delautrer.game.world.persistence.** { *; }