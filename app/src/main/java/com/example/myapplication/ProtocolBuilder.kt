package com.example.myapplication

enum class ProtocolKind(
    val label: String,
    val headerSecondByte: Int,
    val fieldBytes: Int,
) {
    Eb90("EB90", 0x90, 1),
    Eb48("EB48", 0x48, 2),
}

data class ProtocolBuildResult(
    val message: String,
    val length: Int,
    val checksum: String,
)

object ProtocolBuilder {
    fun sanitizeHex(text: String, maxChars: Int? = null): String {
        val normalized = text
            .filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
            .uppercase()
        return if (maxChars == null) normalized else normalized.take(maxChars)
    }

    fun build(
        kind: ProtocolKind,
        receiveCodeText: String,
        sendCodeText: String,
        commandCodeText: String,
        parameterText: String,
    ): ProtocolBuildResult {
        val fieldHexChars = kind.fieldBytes * 2
        val receiveCode = parseFixedField(receiveCodeText, fieldHexChars, "收站码")
        val sendCode = parseFixedField(sendCodeText, fieldHexChars, "发站码")
        val commandCode = parseFixedField(commandCodeText, fieldHexChars, "命令字")
        val parameters = parseParameter(parameterText)

        val frame = when (kind) {
            ProtocolKind.Eb90 -> buildEb90(
                receiveCode = receiveCode,
                sendCode = sendCode,
                commandCode = commandCode,
                parameters = parameters,
            )

            ProtocolKind.Eb48 -> buildEb48(
                receiveCode = receiveCode,
                sendCode = sendCode,
                commandCode = commandCode,
                parameters = parameters,
            )
        }

        return ProtocolBuildResult(
            message = toHexString(frame),
            length = frame.size,
            checksum = "%02X".format(frame.last()),
        )
    }

    private fun buildEb90(
        receiveCode: Int,
        sendCode: Int,
        commandCode: Int,
        parameters: List<Int>,
    ): List<Int> {
        val payload = listOf(receiveCode, sendCode, commandCode) + parameters
        val totalLen = payload.size + 3
        val frame = mutableListOf(0xEB, 0x90)
        frame.add(totalLen and 0xFF)
        frame.add((totalLen ushr 8) and 0xFF)
        frame.addAll(payload)
        frame.add(calcChecksum(frame))
        return frame
    }

    private fun buildEb48(
        receiveCode: Int,
        sendCode: Int,
        commandCode: Int,
        parameters: List<Int>,
    ): List<Int> {
        val payloadLen = parameters.size + 9
        val frame = mutableListOf(0xEB, 0x48)
        frame.add(payloadLen and 0xFF)
        frame.add((payloadLen ushr 8) and 0xFF)
        appendU16(frame, receiveCode)
        appendU16(frame, sendCode)
        appendU16(frame, commandCode)
        frame.addAll(parameters)
        frame.add(calcChecksum(frame))
        return frame
    }

    private fun parseFixedField(text: String, expectedHexChars: Int, name: String): Int {
        val hex = sanitizeHex(text)
        require(hex.length == expectedHexChars) { "$name 应为 $expectedHexChars 位十六进制" }
        val value = hex.toInt(16)
        if (name == "收站码" || name == "发站码") {
            val maxValue = if (expectedHexChars == 2) 0xFF else 0xFFFF
            val rangeText = if (expectedHexChars == 2) "0x00 到 0xFF" else "0x0000 到 0xFFFF"
            require(value in 0..maxValue) { "$name 应为 $rangeText" }
        }
        return value
    }

    private fun parseParameter(text: String): List<Int> {
        val hex = sanitizeHex(text)
        require(hex.length % 2 == 0) { "命令参数必须为偶数位十六进制" }
        return if (hex.isEmpty()) emptyList() else parseHexBytes(hex)
    }

    private fun parseHexBytes(hex: String): List<Int> {
        require(hex.all { it.isDigit() || it in 'A'..'F' }) { "包含非十六进制字符" }
        return hex.chunked(2).map { it.toInt(16) }
    }

    private fun appendU16(frame: MutableList<Int>, value: Int) {
        frame.add(value and 0xFF)
        frame.add((value ushr 8) and 0xFF)
    }

    private fun calcChecksum(frameWithoutChecksum: List<Int>): Int {
        var sum = 0
        for (index in 2 until frameWithoutChecksum.size) {
            sum = (sum + frameWithoutChecksum[index]) and 0xFF
        }
        return ((sum.inv() + 1) and 0xFF)
    }

    private fun toHexString(data: List<Int>): String {
        return data.joinToString(" ") { "%02X".format(it and 0xFF) }
    }
}
