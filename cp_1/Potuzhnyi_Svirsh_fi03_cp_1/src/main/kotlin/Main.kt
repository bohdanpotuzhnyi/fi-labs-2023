import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.util.*
import kotlin.text.Regex
import kotlin.collections.HashMap
import kotlin.math.log2
import kotlin.math.round

object Output {
    fun getParity(n: Int): Boolean {
        var parity = false
        var nVar = n
        while (nVar != 0) {
            parity = !parity
            nVar = nVar and (nVar - 1)
        }
        return parity
    }

    fun <K, V : Comparable<V>> sortByValue(unsortedMap: Map<K, V>): Map<K, V> {
        val sortedList = unsortedMap.entries.sortedByDescending { it.value }
        val sortedMap = LinkedHashMap<K, V>()
        for (entry in sortedList) {
            sortedMap[entry.key] = entry.value
        }
        return sortedMap
    }

    fun <T : Ngrams> writeToFile(dirPath: String, name: String, data: T) {
        try {
            val path = dirPath + name
            val outFile = FileWriter(path)

            outFile.write("## Name: $name\n")

            val entropy = data.getEntropyValue()
            outFile.write("## Data entropy: $entropy\n")

            val redundancy = data.getRedundancyValue()
            outFile.write("## Data redundancy: $redundancy\n")

            outFile.write("|Text|Entropy|\n|:--:|:---:|\n")
            for (entry in data.ngrams) {
                outFile.write("|\"" + entry.key + "\"|" + entry.value + "|\n")
            }

            outFile.close()
        } catch (e: Exception) {
            println("An error occurred: ${e.message}")
        }
    }
}

object Algorithms {
    fun entropy(freqs: List<Float>, n: Int): Float {
        val temp = mutableListOf<Double>()

        for (e in freqs) {
            temp.add(elem_entr(e, n))
        }

        val entropy = -1.0 * temp.sum()
        return entropy.toFloat()
    }

    fun redundancy(e: Float): Float {
        val temp = 1 - e / (log2(32.0))
        return temp.toFloat()
    }

    private fun elem_entr(e: Float, n: Int): Double {
        val temp = e * (log2(e.toDouble()) / log2((n * 2).toDouble()))
        return round(temp * 10000) / 10000.0 // round to 5
    }
}

open class Ngrams(str: String, n: Int) {
    val ngrams: Map<String, Float>
    val entropy: Float
    val redundancy: Float


    init {
        val ngramList = split(str)
        ngrams = setFreqs(ngramList)

        val freqs = ngrams.values.toList()
        entropy = Algorithms.entropy(freqs, n)
        redundancy = Algorithms.redundancy(entropy)
    }

    protected open fun split(str: String): List<String> {
        val list = mutableListOf<String>()
        for (c in str) {
            list.add(c.toString())
        }
        return list
    }

    private fun setFreqs(ngrams: List<String>): Map<String, Float> {
        val unsorted = HashMap<String, Float>()
        val uniques = ngrams.toSet()
        for (ngram in uniques) {
            val occurrences = ngrams.count { it == ngram }.toFloat()
            val frequency = occurrences / ngrams.size
            unsorted[ngram] = frequency
        }
        return Output.sortByValue(unsorted)
    }

    fun getEntropyValue(): Float {
        return entropy
    }

    fun getRedundancyValue(): Float {
        return redundancy
    }
}

class Bigram(str: String, n: Int) : Ngrams(str, n) {
    override fun split(str: String): List<String> {
        val list = mutableListOf<String>()
        var length = str.length
        if (Output.getParity(length)) length--
        for (i in 1 until length step 1) {
            val prev = str[i - 1].toString()
            val currStr = prev + str[i]
            list.add(currStr)
        }
        return list
    }
}

class BigramCross(str: String, n: Int) : Ngrams(str, n) {
    override fun split(str: String): List<String> {
        val list = mutableListOf<String>()
        var length = str.length
        if (!Output.getParity(length)) length--
        for (i in 1 until length step 2) {
            val prev = str[i - 1].toString()
            val currStr = prev + str[i]
            list.add(currStr)
        }
        return list
    }
}

object Filter {
    fun filterText(str: String, whitespaces: Boolean): String {
        try {
            var temp = str.replace("ъ", "ь")
            temp = temp.replace("ё", "е")

            return if (whitespaces) {
                temp = temp.replace("[^а-я]".toRegex(), " ")
                temp.replace("\\s{2,}".toRegex(), " ")
            } else {
                temp.replace("[^а-я]".toRegex(), "")
            }
        } catch (e: Exception) {
            println("Invalid input!")
        }

        return ""
    }
}

fun main() {
    var filePath = "src/main/resources/big"

    var data = File(filePath).readText()
    data = data.lowercase()

    filePath += "_"

    val text = Filter.filterText(data, true) // whitespaces
    val textNoSpaces = Filter.filterText(data, false) // no whitespaces

    val monogramWhitespace = Ngrams(text, 1)
    Output.writeToFile(filePath, "monogramWhitespace.md", monogramWhitespace)

    val monogramNoWhitespace = Ngrams(textNoSpaces, 1)
    Output.writeToFile(filePath, "monogramNoWhitespace.md", monogramNoWhitespace)

    val bigramWhitespace = Bigram(text, 2)
    Output.writeToFile(filePath, "bigramWhitespace.md", bigramWhitespace)

    val bigramCrossWhitespace = BigramCross(text, 2)
    Output.writeToFile(filePath, "bigramCrossWhitespace.md", bigramCrossWhitespace)

    val bigramNoWhitespace = Bigram(textNoSpaces, 2)
    Output.writeToFile(filePath, "bigramNoWhitespace.md", bigramNoWhitespace)

    val bigramNoCrossWhitespace = BigramCross(textNoSpaces, 2)
    Output.writeToFile(filePath, "bigramCrossNoWhitespace.md", bigramNoCrossWhitespace)
}

