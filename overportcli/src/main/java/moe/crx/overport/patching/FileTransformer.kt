package moe.crx.overport.patching

import com.reandroid.json.JSONObject
import java.io.File

class FileTransformer(val file: File) {
    fun replace(input: String, output: String) {
        file.writeText(Regex(input, RegexOption.DOT_MATCHES_ALL).replace(file.readText(), output))
    }

    fun delete(): Boolean {
        return file.deleteRecursively()
    }

    fun readText(): String {
        return file.readText()
    }

    fun append(line: String) {
        file.appendText(line)
    }

    fun appendLine(line: String) {
        append(line)
        append("\n")
    }

    fun readJson(): JSONObject? {
        return runCatching { JSONObject(file.readText()) }.getOrNull()
    }

    fun writeText(text: String) {
        file.writeText(text)
    }

    fun writeBytes(bytes: ByteArray) {
        file.writeBytes(bytes)
    }

    fun writeJson(json: JSONObject) {
        writeText(json.toString())
    }

    fun replaceHex(input: String, output: String) {
        val bytes = file.readBytes()
        val inputBytes = input.split(' ')

        for (i in 0..<bytes.size - inputBytes.size + 1) {
            var matches = true

            for (j in i..<bytes.size) {
                val checkIndex = j - i
                if (checkIndex >= inputBytes.size) {
                    break
                }

                if (inputBytes[checkIndex] != "??" && bytes[j].toUByte() != inputBytes[checkIndex].toUByte(16)) {
                    matches = false
                    break
                }
            }

            if (matches) {
                val replaced = bytes.slice(0..<i) +
                        output.split(' ').map { it.toUByte(16).toByte() } +
                        bytes.slice(i + inputBytes.size..<bytes.size)
                file.writeBytes(replaced.toByteArray())
                return
            }
        }
    }
}