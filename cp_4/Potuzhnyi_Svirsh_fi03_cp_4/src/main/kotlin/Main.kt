import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.*

class LFSR(val n: Int, private val feedback: IntArray, private var state: BooleanArray) {

    fun step(): Boolean {
        var feedbackBit = false
        for (i in feedback) {
            feedbackBit = feedbackBit xor state[i]
        }
        state = booleanArrayOf(feedbackBit) + state.sliceArray(0 until n-1)
        return feedbackBit
    }

    fun run(steps: Int): BooleanArray {
        val output = BooleanArray(steps)
        for (i in 0 until steps) {
            output[i] = step()
        }
        return output
    }

    fun setState(newState: BooleanArray) {
        state = newState
    }
}

fun geffeGenerator(l1: LFSR, l2: LFSR, l3: LFSR, steps: Int): BooleanArray {
    val output = BooleanArray(steps)
    for (i in 0 until steps) {
        val x = l1.step()
        val y = l2.step()
        val s = l3.step()
        output[i] = (s && x) xor (!s && y)
    }
    return output
}

fun correlationAttack(lfsr: LFSR, sequence: BooleanArray, C: Double): List<BooleanArray> {
    val candidates = mutableListOf<BooleanArray>()
    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    for (i in 0 until 2.0.pow(lfsr.n).toInt()) {
        executor.execute {
            val initState = BooleanArray(lfsr.n) { j -> ((i shr j) and 1) == 1 }
            lfsr.setState(initState)
            val generatedSequence = lfsr.run(sequence.size)

            var R = 0.0
            for (j in sequence.indices) {
                R += if (sequence[j] == generatedSequence[j]) 1 else -1
            }

            if (R > C) {
                synchronized(candidates) {
                    candidates.add(initState)
                }
            }
        }
    }

    executor.shutdown()
    try {
        if (!executor.awaitTermination(60, TimeUnit.MINUTES)) {
            executor.shutdownNow()
        }
    } catch (e: InterruptedException) {
        executor.shutdownNow()
    }

    return candidates
}

fun verifyInitialFills(l1: LFSR, l2: LFSR, l3: LFSR, sequence: BooleanArray): Boolean {
    val generatedSequence = geffeGenerator(l1, l2, l3, sequence.size)
    for (i in sequence.indices) {
        if (sequence[i] != generatedSequence[i]) {
            return false
        }
    }
    return true
}

fun main() {
    // Define the LFSRs
    val l1 = LFSR(30, intArrayOf(0, 1, 4, 6), BooleanArray(30) { false })
    val l2 = LFSR(31, intArrayOf(0, 3), BooleanArray(31) { false })
    val l3 = LFSR(32, intArrayOf(0, 1, 2, 3, 5, 7), BooleanArray(32) { false })

    // Given sequence
    val sequenceStr = "100101111011100000101111001111000110010010100100111001000110101111001011011001101001011111000011011101000111101010101101001000000110001001111110100011010100111111100110011001001101110011000111011100111110001111010110111111010011000010101011101001000101101111111101000111010000111101000101100011010000100011100010100001001111011110111011011111000100010001111100101100011010110100010"
    val sequence = sequenceStr.map { it == '1' }.toBooleanArray()

    // Correlation threshold
    val C = sequence.size * 0.6

    // Perform correlation attack
    val candidatesL1 = correlationAttack(l1, sequence, C)
    val candidatesL2 = correlationAttack(l2, sequence, C)
    val candidatesL3 = correlationAttack(l3, sequence, C)

    // Print the candidates for the initial fill of the LFSRs
    println("Candidates for the initial fill of L1:")
    for (candidate in candidatesL1) {
        println(candidate.joinToString("") { if (it) "1" else "0" })
    }

    println("Candidates for the initial fill of L2:")
    for (candidate in candidatesL2) {
        println(candidate.joinToString("") { if (it) "1" else "0" })
    }

    println("Candidates for the initial fill of L3:")
    for (candidate in candidatesL3) {
        println(candidate.joinToString("") { if (it) "1" else "0" })
    }

    // Verify the found initial fills of L1, L2, and L3
    for (candidateL1 in candidatesL1) {
        l1.setState(candidateL1)
        for (candidateL2 in candidatesL2) {
            l2.setState(candidateL2)
            for (candidateL3 in candidatesL3) {
                l3.setState(candidateL3)
                if (verifyInitialFills(l1, l2, l3, sequence)) {
                    println("Found correct initial fills for L1, L2, and L3.")
                    return
                }
            }
        }
    }

    println("No correct initial fills found for L1, L2, and L3.")
}