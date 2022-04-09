package com.portafolio

import com.opencsv.CSVReader
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.FileReader
import java.io.FileWriter

import com.opencsv.CSVWriter
import java.util.*


@SpringBootApplication
class PortafolioApplication

fun main(args: Array<String>) {
    runApplication<PortafolioApplication>(*args)
    /*val country = "co"
    val iterator = 2
    val archCSV = "D:\\${country}_${iterator}.csv"
    val archCSVW = "D:\\${country}_${iterator}_users.csv"
    val csvReader = CSVReader(FileReader(archCSV))
    val writer = CSVWriter(FileWriter(archCSVW), '|', CSVWriter.NO_QUOTE_CHARACTER)
    var fila: Array<String>?
    var userId: String? = null
    var query: String
    var isFirstRow = true
    while (csvReader.readNext().also { fila = it } != null) {
        if (!isFirstRow) {
            userId = fila!![0]
            query = "INSERT INTO application_user_media_notifications (application_user_id, created_at, sms, mailings, push_marketing, inapp, whatsapp) VALUES ($userId, now()::date, true, true, true, true, true) ON CONFLICT (application_user_id) DO NOTHING;"
            /*println(
                fila!![0] + " | " + fila!![1]
            )*/
            writer.writeNext(arrayOf(query));
        }
        isFirstRow = false
    }
    println("Este es el userId final: $userId")

    csvReader.close()
    writer.close()*/

}

