import java.io.File
import kotlin.math.abs

const val DIR = "D:\\kpi\\symcryptology\\lab\\cp_2\\Potuzhnyi_Svirsh_fi03_cp_2\\src\\main\\resources\\"

object CypherText {
    const val charset = "абвгдежзийклмнопрстуфхцчшщъыьэюя"
    var freq : Map<Char, Double>
    private fun letterProbability(fileName: String): Map<Char, Double> {
        val fileText = File(fileName).readText().lowercase().filter { it in charset }
        val letterCount = mutableMapOf<Char, Int>()

        for (char in charset) {
            letterCount[char] = fileText.count { it == char }
        }

        val totalLetters = fileText.length.toDouble()
        return letterCount.mapValues { (_, count) -> count / totalLetters }
    }
    
    init{
        freq = letterProbability(DIR + "big")
    }
}

class VigenereCipher {
    fun encode(text: String, key: String, charset: String = CypherText.charset): String {
        val upperKey = key.lowercase()
        val upperText = text.lowercase()
        return upperText.mapIndexed { index, char ->
            val keyIndex = charset.indexOf(upperKey[index % upperKey.length])
            val textIndex = charset.indexOf(char)
            charset[(textIndex + keyIndex) % charset.length]
        }.joinToString("")
    }

    fun decode(text: String, key: String, charset: String = CypherText.charset): String {
        val upperKey = key.lowercase()
        val upperText = text.lowercase()
        return upperText.mapIndexed { index, char ->
            val keyIndex = charset.indexOf(upperKey[index % upperKey.length])
            val textIndex = charset.indexOf(char)
            charset[(textIndex - keyIndex + charset.length) % charset.length]
        }.joinToString("")
    }
}

fun N_t(text: String, char: Char): Int = text.count { it == char }

fun M_i(block: String, g: Int, charset: String = CypherText.charset): Double {
    return CypherText.freq.entries.sumOf { (char, freq) ->
        freq * N_t(block, charset[(charset.indexOf(char) + g) % charset.length])
    }
}

fun I(text: String, charset: String = CypherText.charset): Double {
    var sum = 0.0
    for (char in charset) {
        val count = N_t(text, char)
        sum += count * (count - 1)
    }
    return sum / (text.length * (text.length - 1))
}

fun get_r(text: String, start: Int = 1, end: Int = 30): Map<Int, Double> {
    val match = mutableMapOf<Int, Double>()
    for (r in start..end) {
        val blocks = Array(r) { "" }
        for (i in text.indices) {
            blocks[i % r] = blocks[i % r] + text[i]
        }
        val result = blocks.map(::I)
        match[r] = result.average()
    }
    return match
}

fun main() {
    val openText = File(DIR +"fileToEncrypt").readText().lowercase().filter { it in CypherText.charset }
    val keys = listOf("ХЛ", "ЗЛО", "ХЛПК", "ПТХХЛ", "ЙОБАНАРУСНЯ".lowercase())
    val vigenere = VigenereCipher()

    val encodings = keys.map { key -> vigenere.encode(openText, key) }

    val outputFilePath = DIR + "output"

    File(outputFilePath).writeText("Encoded and Decoded texts:" + System.lineSeparator())
    encodings.forEachIndexed { index, encoding ->
        val decoded = vigenere.decode(encoding, keys[index])
        File(outputFilePath).appendText("Text ${index + 1}:" + System.lineSeparator())
        File(outputFilePath).appendText("Encoded: $encoding" + System.lineSeparator())
        File(outputFilePath).appendText("Decoded: $decoded" + System.lineSeparator())
    }

    val open_r = I(openText)
    val r_values = encodings.map(::I)

    File(outputFilePath).appendText("Ir open:" + System.lineSeparator())
    File(outputFilePath).appendText(open_r.toString() + System.lineSeparator())
    r_values.forEachIndexed { index, r ->
        File(outputFilePath).appendText("Ir ${keys[index].length}:" + System.lineSeparator())
        File(outputFilePath).appendText(r.toString() + System.lineSeparator())
    }

    val cypherFileName = DIR + "cipher_var15"
    val cypher = File(cypherFileName).readText().lowercase().filter { it in CypherText.charset }
    
    val matchDict = get_r(cypher)
    val matchList = matchDict.entries.sortedBy { (_, value) -> abs(value - open_r) }
    println("for encoded text")
    println(matchDict)
    println(matchList)
    val bestFit = matchList[0].key

    val blocks = Array(bestFit) { "" }
    for (i in cypher.indices) {
        blocks[i % bestFit] = blocks[i % bestFit] + cypher[i]
    }

    val sortedProbabilities = CypherText.freq.toList().sortedByDescending { it.second }

    val top10Chars = sortedProbabilities.take(10).map { it.first }

    val keysResult = mutableListOf<String>()
    for (mostPossible in top10Chars) {
        var key = ""
        for (i in 0 until bestFit) {
            val valMap = CypherText.charset.associateWith { char -> N_t(blocks[i], char) }
            val maxKey = valMap.maxByOrNull { it.value }!!.key
            val k = (CypherText.charset.indexOf(maxKey) - CypherText.charset.indexOf(mostPossible) + CypherText.charset.length) % 32
            key += CypherText.charset[k]
        }
        keysResult.add(key)
    }
    println(keysResult)

    val keyM = StringBuilder()
    for (block in blocks) {
        val valMap = CypherText.charset.associateWith { char -> M_i(block, CypherText.charset.indexOf(char)) }
        val maxKey = valMap.maxByOrNull { it.value }!!.key
        keyM.append(maxKey)
    }

    println(keyM.toString())
    println(vigenere.decode(cypher, keyM.toString()))
}
