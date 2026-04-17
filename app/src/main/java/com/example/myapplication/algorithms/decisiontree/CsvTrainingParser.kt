package com.example.myapplication.algorithms.decisiontree

object CsvTrainingParser {

    fun parse(text: String): Result<TrainingSet> {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.size < 2) {
            return Result.failure(IllegalArgumentException("Нужны заголовок и хотя бы одна строка данных"))
        }
        val header = splitCsvLine(lines.first())
        if (header.size < 2) {
            return Result.failure(IllegalArgumentException("Нужно минимум два столбца (признак и цель)"))
        }
        val targetName = header.last()
        val featureNames = header.dropLast(1)
        val rows = mutableListOf<Map<String, String>>()
        for (i in 1 until lines.size) {
            val cells = splitCsvLine(lines[i])
            if (cells.isEmpty()) continue
            if (cells.size != header.size) {
                return Result.failure(
                    IllegalArgumentException("Строка ${i + 1}: ожидается ${header.size} столбцов, получено ${cells.size}")
                )
            }
            val row = header.indices.associate { idx -> header[idx] to cells[idx].trim() }
            rows.add(row)
        }
        if (rows.isEmpty()) {
            return Result.failure(IllegalArgumentException("Нет ни одной строки данных"))
        }
        return Result.success(TrainingSet(featureNames, targetName, rows))
    }

    private fun splitCsvLine(line: String): List<String> {
        return line.split(',').map { it.trim().trim('"') }
    }
}