package android.romstats

import java.io.BufferedReader
import java.lang.StringBuilder

fun BufferedReader.emitAllAndClose() : StringBuilder {
    val builder = StringBuilder()
    var line: String?
    while (true) {
        line = this.readLine()
        if (line == null)
            break
        builder.append(line)
    }
    this.close()
    return builder
}