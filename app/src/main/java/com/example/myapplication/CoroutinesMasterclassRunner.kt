package com.example.myapplication

import kotlinx.coroutines.*

object CoroutinesMasterclassRunner {

    suspend fun runTask(taskId: Int): String = when (taskId) {
        1 -> runTask1()
        2 -> runTask2()
        3 -> runTask3()
        4 -> runTask4()
        5 -> runTask5()
        6 -> runTask6()
        7 -> runTask7()
        8 -> runTask8()
        9 -> runTask9()
        10 -> runTask10()
        11 -> runTask11()
        12 -> runTask12()
        13 -> runTask13()
        14 -> runTask14()
        15 -> runTask15()
        16 -> runExtra1()
        17 -> runExtra2()
        18 -> runExtra3()
        19 -> runExtra4()
        20 -> runExtra5()
        else -> "Unknown task ID"
    }

    private suspend fun runTask1(): String = coroutineScope {
        val sb = StringBuilder()
        launch {
            delay(100L)
            sb.append("Coroutine finished\n")
        }
        sb.append("Order accepted\n")
        delay(200L)
        sb.toString()
    }

    private suspend fun runTask2(): String = coroutineScope {
        val time = kotlin.system.measureTimeMillis {
            val b1 = async { delay(300L); "Coffee 1" }
            val b2 = async { delay(300L); "Coffee 2" }
            val b3 = async { delay(300L); "Coffee 3" }
            b1.await(); b2.await(); b3.await()
        }
        "Ready: Coffee 1, Coffee 2, Coffee 3\nTime taken: ~$time ms (concurrent)"
    }

    private suspend fun runTask3(): String = coroutineScope {
        suspend fun s1(): String { delay(100L); return "Step 1" }
        suspend fun s2(): String { delay(100L); return "Step 2" }
        suspend fun s3(): String { delay(100L); return "Step 3" }
        val r1 = s1()
        val r2 = s2()
        val r3 = s3()
        "$r1 -> $r2 -> $r3"
    }

    private suspend fun runTask4(): String = coroutineScope {
        suspend fun getData(): String {
            delay(100L)
            return "Data retrieved via coroutineScope"
        }
        getData()
    }

    private suspend fun runTask5(): String = coroutineScope {
        val threadBefore = Thread.currentThread().name
        var threadInside = ""
        var threadAfter = ""
        suspend fun doWork() {
            threadInside = Thread.currentThread().name
            delay(50L)
            threadAfter = Thread.currentThread().name
        }
        doWork()
        "Before: $threadBefore\nInside: $threadInside\nAfter delay: $threadAfter"
    }

    private suspend fun runTask6(): String = coroutineScope {
        val results = mutableListOf<String>()
        repeat(5) { i ->
            launch {
                delay(100L)
                results.add("Download $i completed")
            }
        }
        delay(200L)
        results.joinToString("\n")
    }

    private suspend fun runTask7(): String = coroutineScope {
        val list = mutableListOf<String>()
        list.add("1")
        launch {
            delay(100L)
            list.add("2")
        }
        launch {
            list.add("3")
        }
        list.add("4")
        delay(150L)
        list.joinToString(", ")
    }

    private suspend fun runTask8(): String = coroutineScope {
        val sb = StringBuilder()
        val job = launch {
            launch {
                delay(200L)
                sb.append("Child 1 completed\n")
            }
            launch {
                delay(100L)
                sb.append("Child 2 completed\n")
            }
            sb.append("Parent body running\n")
        }
        job.join()
        sb.append("Parent completed")
        sb.toString()
    }

    private suspend fun runTask9(): String = coroutineScope {
        val sb = StringBuilder()
        val parent = launch {
            launch {
                try {
                    delay(500L)
                } finally {
                    sb.append("Task 1 cleaned up\n")
                }
            }
            launch {
                try {
                    delay(500L)
                } finally {
                    sb.append("Task 2 cleaned up\n")
                }
            }
        }
        delay(100L)
        parent.cancelAndJoin()
        sb.append("Parent cancelled")
        sb.toString()
    }

    private suspend fun runTask10(): String = coroutineScope {
        val sb = StringBuilder()
        suspend fun fetchProfile() = coroutineScope {
            launch { delay(200L); sb.append("Avatar loaded\n") }
            launch { delay(100L); sb.append("Bio loaded\n") }
            sb.append("Profile fetching started\n")
        }
        fetchProfile()
        sb.append("Profile ready")
        sb.toString()
    }

    private suspend fun runTask11(): String = coroutineScope {
        val sb = StringBuilder()
        fun CoroutineScope.startCounter(limit: Int): Job = launch {
            for (i in 1..limit) {
                delay(50L)
                sb.append("Count: $i\n")
            }
        }
        val job = startCounter(3)
        job.join()
        sb.append("Counter finished")
        sb.toString()
    }

    private suspend fun runTask12(): String = coroutineScope {
        val sb = StringBuilder()
        sb.append("--- asyncTask (Launch) ---\n")
        val job = launch {
            delay(100L)
            sb.append("Async task done\n")
        }
        sb.append("after asyncTask call\n")
        job.join()

        sb.append("--- syncTaskScope ---\n")
        coroutineScope {
            launch {
                delay(100L)
                sb.append("Sync scope task done\n")
            }
            sb.append("Scope block finished\n")
        }
        sb.append("after syncTaskScope")
        sb.toString()
    }

    private suspend fun runTask13(): String = coroutineScope {
        val sb = StringBuilder()
        val job = launch { delay(50L); sb.append("launch done\n") }
        job.join()
        val deferred = async { delay(50L); 42 }
        sb.append("async result: ${deferred.await()}\n")
        val cs = coroutineScope { delay(50L); "coroutineScope result" }
        sb.append("$cs\n")
        val wc = withContext(Dispatchers.Default) { delay(50L); "withContext result" }
        sb.append(wc)
        sb.toString()
    }

    private suspend fun runTask14(): String = coroutineScope {
        val sb = StringBuilder()
        val job: Job = launch {
            delay(50L)
            sb.append("Job running\n")
        }
        job.join()
        val deferred: Deferred<Int> = async {
            delay(50L)
            42
        }
        sb.append("Deferred result: ${deferred.await()}")
        sb.toString()
    }

    private suspend fun runTask15(): String = coroutineScope {
        val sb = StringBuilder()
        suspend fun exportData() = coroutineScope {
            launch { delay(200L); sb.append("Export 1 done\n") }
            launch { delay(50L); sb.append("Export 2 done\n") }
            launch { delay(100L); sb.append("Export 3 done\n") }
        }
        exportData()
        sb.append("All done")
        sb.toString()
    }

    private suspend fun runExtra1(): String = coroutineScope {
        val delayTime = kotlin.system.measureTimeMillis {
            coroutineScope {
                repeat(3) { launch { delay(200L) } }
            }
        }
        "Delay time (concurrent): ${delayTime}ms"
    }

    private suspend fun runExtra2(): String = coroutineScope {
        val sb = StringBuilder()
        sb.append("Parent scope started\n")
        launch {
            sb.append("Child 1 started\n")
            launch {
                delay(100L)
                sb.append("Grandchild finished\n")
            }
            delay(50L)
            sb.append("Child 1 finished\n")
        }
        launch {
            delay(80L)
            sb.append("Child 2 finished\n")
        }
        delay(150L)
        sb.append("Parent scope end")
        sb.toString()
    }

    private suspend fun runExtra3(): String = coroutineScope {
        val sb = StringBuilder()
        launch {
            delay(50L)
            sb.append("Launched inside coroutineScope builder (no scope error fixed)")
        }
        delay(100L)
        sb.toString()
    }

    private suspend fun runExtra4(): String = coroutineScope {
        suspend fun compute(x: Int): Int {
            delay(50L)
            return x * 2
        }
        val res = compute(21)
        "Computed via CPS suspend function: $res"
    }

    private suspend fun runExtra5(): String = coroutineScope {
        val sb = StringBuilder()
        val names = listOf("Alice", "Bob", "Charlie", "David")
        val time = kotlin.system.measureTimeMillis {
            coroutineScope {
                names.forEachIndexed { index, name ->
                    launch {
                        val pause = (index + 1) * 50L
                        delay(pause)
                        sb.append("Hello, $name! (${pause}ms)\n")
                    }
                }
            }
        }
        sb.append("Total time: ~${time}ms")
        sb.toString()
    }
}
