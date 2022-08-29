package com.example

import com.example.data.MongoContext
import com.example.model.TargetFrameworkMonikier
import com.example.model.Task
import com.example.model.TaskCreatedEvent
import com.example.model.TaskStatus
import com.mongodb.client.model.FindOneAndUpdateOptions
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.bson.conversions.Bson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.litote.kmongo.coroutine.CoroutineCollection
import java.time.Instant
import java.util.*

internal class TaskDispatcherTest {
    private val tasksMock = mockk<CoroutineCollection<Task>>()
    private val mongoContextMock = mockk<MongoContext> {
        every { tasks } returns tasksMock
    }

    @BeforeEach
    fun init() {
        clearMocks(tasksMock)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun run_onNewTask_shouldProduceEventAndUpdateStatus() = runTest {
        // Arrange
        val newTask = createTask()

        every { tasksMock.find(any<Bson>()) } returns mockk {
            every { ascendingSort(*anyVararg()) } returns this
            coEvery { first() } returns newTask andThen null
        }

        coEvery { tasksMock.findOneAndUpdate(any<Bson>(), any<Bson>(), any()) } returns newTask

        val callback = slot<Callback>()
        val producerMock = mockk<KafkaProducer<UUID, TaskCreatedEvent>> {
            every { send(any(), capture(callback)) } answers {
                callback.captured.onCompletion(mockk(), null)
                mockk()
            }
        }

        val dispatcher = TaskDispatcher(mongoContextMock, producerMock)

        // Act
        val job = launch {
            dispatcher.run()
        }
        runCurrent()
        job.cancel()

        // Assert
        verify(exactly = 1) {
            producerMock.send(ProducerRecord("tasks", newTask.id, TaskCreatedEvent(
                newTask.id,
                newTask.code,
                newTask.arguments,
                newTask.targetFrameworkMonikier,
                newTask.nugetPackages
            )), any())
        }

        coVerify(exactly = 1) {
            tasksMock.findOneAndUpdate(any<Bson>(), any<Bson>(), any<FindOneAndUpdateOptions>())
        }
    }

    private fun createTask() = Task(
        UUID.randomUUID(),
        "Console.WriteLine(\"Foo\")",
        listOf(),
        TargetFrameworkMonikier.net60,
        TaskStatus.Created,
        listOf(),
        Instant.now()
    )
}