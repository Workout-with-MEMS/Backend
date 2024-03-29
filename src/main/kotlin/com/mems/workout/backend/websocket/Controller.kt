package com.mems.workout.backend.websocket

import com.fasterxml.jackson.databind.JsonNode
import com.mems.workout.backend.db.Data
import com.mems.workout.backend.db.InvalidIdException
import com.mems.workout.backend.db.UseCase
import com.mems.workout.backend.mqtt.Subscriber
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RestController
@CrossOrigin(origins = ["http://localhost:3000"])
class Controller {
    @Autowired
    lateinit var template: SimpMessagingTemplate
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    @Autowired
    private lateinit var useCase: UseCase

    fun createMqttSubscriber(brokerHostName: String, subscribeTopics: Array<String>) {
        executor.submit {
            val subscriber = Subscriber(brokerHostName, subscribeTopics, template)
            subscriber.subscribe()
        }
    }

    @PostMapping("/add")
    fun add(@RequestBody dataJson: JsonNode): ResponseEntity<Map<String, Any>> {

        val id = 0 // this value is not important
        val datetime = Date()
        val data = Data(id, datetime, dataJson)

        val result = useCase.add(data)
        if (result) {
            println("[Success] added $dataJson")
            return ResponseEntity.ok(
                mapOf(
                    "data" to data,
                    "error" to "",
                )
            )
        } else {
            println("[Error] adding $dataJson failed")
            throw InvalidIdException("Failed to add $data")
        }
    }

    @GetMapping("/get")
    fun get(@RequestParam("id") id: Int): ResponseEntity<Map<String, Any>> {

        val data = useCase.get(id)
        return if (data != null) {
            println("[Success] got: \n$data")
            ResponseEntity.ok(
                mapOf(
                    "data" to data,
                    "error" to "",
                )
            )
        } else {
            println("[Error] getting $id failed")
            throw InvalidIdException("Invalid ID: $id")
        }
    }

    @GetMapping("/delete")
    fun delete(@RequestParam("id") id: Int): ResponseEntity<Map<String, Any>> {

        val result = useCase.delete(id)
        return if (result) {
            println("[Success] delete: \n$id")
            ResponseEntity.ok(
                mapOf(
                    "data" to id,
                    "error" to "",
                )
            )
        } else {
            println("[Error] deleting $id failed")
            throw InvalidIdException("Invalid ID: $id")
        }
    }

    @GetMapping("/list")
    fun getRecords(@RequestParam("offset") offset: Int): ResponseEntity<Map<String, Any>> {

        val data = useCase.getRecords(offset)
        return if (data != null) {
            println("[Success] got: \n$data")
            ResponseEntity.ok(
                mapOf(
                    "data" to data,
                    "error" to "",
                )
            )
        } else {
            println("[Error] getting list with $offset failed")
            throw InvalidIdException("Invalid offset: $offset")
        }
    }

    @ExceptionHandler(InvalidIdException::class)
    fun handleInvalidIdException(exception: InvalidIdException): ResponseEntity<Map<String, String>> {
        val error = exception.message ?: ""
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                mapOf(
                    "data" to "",
                    "error" to error,
                )
            )
    }

    @PostConstruct
    fun init() {
        createMqttSubscriber("host.docker.internal", arrayOf("topic", "topic2"))
//        createMqttSubscriber("localhost", "topic")
    }
}


