package com.luizmarquesarruda.relogio20.device.gt3max.protocol

/**
 * Reassembles AB frames from notification chunks and separates concatenated frames.
 * It recognizes framing only; command semantics remain the responsibility of the
 * separately validated parser. The buffer is bounded against malformed input.
 */
internal class Gt3MaxAbStreamDecoder(
    private val maxDeclaredLength: Int = 512,
    private val maxBufferBytes: Int = 1_024
) {
    private val buffer = ArrayList<Byte>()

    init {
        require(maxDeclaredLength >= 4)
        require(maxBufferBytes >= maxDeclaredLength + 3)
    }

    @Synchronized
    fun append(chunk: ByteArray): List<Gt3MaxAbFrame> {
        if (chunk.isEmpty()) return emptyList()
        chunk.forEach { buffer.add(it) }
        if (buffer.size > maxBufferBytes) {
            val excess = buffer.size - maxBufferBytes
            repeat(excess) { if (buffer.isNotEmpty()) buffer.removeAt(0) }
        }

        val frames = mutableListOf<Gt3MaxAbFrame>()
        while (buffer.isNotEmpty()) {
            while (buffer.isNotEmpty() && unsigned(buffer[0]) != HEADER) {
                buffer.removeAt(0)
            }
            if (buffer.size < 3) break

            val declaredLength = (unsigned(buffer[1]) shl 8) or unsigned(buffer[2])
            if (declaredLength < MIN_DECLARED_LENGTH || declaredLength > maxDeclaredLength) {
                buffer.removeAt(0)
                continue
            }

            val totalLength = declaredLength + 3
            if (buffer.size < totalLength) break

            val candidate = ByteArray(totalLength) { index -> buffer[index] }
            val frame = Gt3MaxAbFrameParser.parse(candidate)
            if (frame == null) {
                buffer.removeAt(0)
                continue
            }
            frames += frame
            repeat(totalLength) { buffer.removeAt(0) }
        }
        return frames
    }

    @Synchronized
    fun reset() {
        buffer.clear()
    }

    @Synchronized
    fun bufferedByteCount(): Int = buffer.size

    private fun unsigned(value: Byte): Int = value.toInt() and 0xFF

    companion object {
        private const val HEADER = 0xAB
        private const val MIN_DECLARED_LENGTH = 4
    }
}
