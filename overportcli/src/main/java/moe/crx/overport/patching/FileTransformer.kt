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
        val outputBytes = output.split(' ')
        var startIndex = -1

        check(inputBytes.size == outputBytes.size)

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
                startIndex = i
            }
        }

        if (startIndex == -1) {
            return
        }

        for (i in 0..<inputBytes.size) {
            if (outputBytes[i] != "??") {
                bytes[i + startIndex] = outputBytes[i].toUByte(16).toByte()
            } else if (inputBytes[i] != "??") {
                bytes[i + startIndex] = inputBytes[i].toUByte(16).toByte()
            }
        }

        file.writeBytes(bytes)
    }
}