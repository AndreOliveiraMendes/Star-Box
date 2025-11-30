package com.starspeck.counter

import android.content.Context
import java.io.File

object ExportImport {

    private const val FILE_NAME = "starspeck_export.csv"

    fun exportCsv(context: Context, counters: Map<String, Int>): File {
        // counters: "star_1" -> 10, "shining_3" -> 2, etc.

        val file = File(context.getExternalFilesDir(null), FILE_NAME)

        file.printWriter().use { out ->
            out.println("tipo;qtd;total")

            counters.forEach { (key, value) ->
                val parts = key.split("_")
                val tipo = if (parts[0] == "star") "Star Speck" else "Shining Star Speck"
                val qtd = parts[1].toInt()

                out.println("$tipo;$qtd;$value")
            }
        }

        return file
    }
}

