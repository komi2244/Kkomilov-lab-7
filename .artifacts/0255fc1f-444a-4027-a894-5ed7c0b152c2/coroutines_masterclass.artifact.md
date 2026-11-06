# Kotlin Coroutines Masterclass (15 + 5 Topshiriqlar To'plami)

Assalomu alaykum! Ushbu qo'llanmada siz Kotlin 2.2 va `kotlinx-coroutines-core` (1.10.2) kutubxonasi asosida tuzilgan **15 ta asosiy** va **5 ta qo'shimcha (extra)** amaliy savol-topshiriqlar uchun to'liq kodlar, kutilayotgan natijalar va strukturaviy konkurensiya (structured concurrency), thread'lar hamda `suspend` mexanizmlariga asoslangan batafsil ilmiy-amaliy tushuntirshlarni topasiz.

---

## 1. Asosiy Topshiriqlar (Questions 1–15)

### Question 1. First coroutine (`runBlocking`, `delay`, print order)

#### Kod:
```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    launch {
        delay(1000L)
        println("Coroutine finished")
    }
    println("Order accepted")
}
```

#### Kutilayotgan Natija (Output):
```text
Order accepted
Coroutine finished
```

#### Tushuntirish:
- **Nima uchun "Order accepted" birinchi chiqadi?** `runBlocking` asosiy thread'ni bloklaydi va uning ichida koroutinalar scope'ini yaratadi. `launch` builder'i esa kodi asinxron tarzda ishga tushiradi. `delay(1000L)` thread'ni bloklamagan holda koroutinani 1 sekundga to'xtatib turadi (`suspend` qiladi). Shu vaqtda `runBlocking` ichidagi keyingi qator darhol bajarilib, `"Order accepted"` matnini konsolga chiqaradi. 1 sekund o'tgach, koroutina o'z ishini davom ettiradi va `"Coroutine finished"` deb yozadi.

---

### Question 2. Three baristas (3 coroutines with delay vs Thread.sleep)

#### Kod:
```kotlin
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

fun main() = runBlocking {
    val time = measureTimeMillis {
        val b1 = async { delay(1000L); "Coffee 1" }
        val b2 = async { delay(1000L); "Coffee 2" }
        val b3 = async { delay(1000L); "Coffee 3" }
        println("Ready: ${b1.await()}, ${b2.await()}, ${b3.await()}")
    }
    println("Time taken: $time ms")
}
```

#### Kutilayotgan Natija (Output):
```text
Ready: Coffee 1, Coffee 2, Coffee 3
Time taken: ~1000 ms
```

#### Tushuntirish:
- `delay` funksiyasi non-blocking (bloksiz) to'xtashni ta'minlaydi. Uchta barista (`async` orqali yaratilgan koroutinalar) bir vaqtning o'zida bir xil thread yoki thread pool'da ishga tushib, parallel ravishda 1 sekund kutishadi. Natijada umumiy vaqt 3 sekund emas, balki eng uzun operatsiya vaqti bo'lgan ~1 sekundni tashkil qiladi. Agar `Thread.sleep` ishlatilganda, thread'lar ketma-ket bloklanib, vaqt 3 sekund bo'lar edi.

---

### Question 3. Chain of suspend functions (3 sequential suspend functions)

#### Kod:
```kotlin
import kotlinx.coroutines.*

suspend fun step1(): String {
    delay(500L)
    return "Step 1"
}

suspend fun step2(): String {
    delay(500L)
    return "Step 2"
}

suspend fun step3(): String {
    delay(500L)
    return "Step 3"
}

fun main() = runBlocking {
    val r1 = step1()
    val r2 = step2()
    val r3 = step3()
    println("$r1 -> $r2 -> $r3")
}
```

#### Kutilayotgan Natija (Output):
```text
Step 1 -> Step 2 -> Step 3
```

#### Tushuntirish:
- `suspend` funksiyalar faqat koroutina yoki boshqa `suspend` funksiya ichidan chaqirilishi mumkin. Bu yerda `step1`, `step2` va `step3` ketma-ket (`sequential`) chaqirilmoqda. Har biri 500ms dan kechikishga ega bo'lgani uchun umumiy bajarilish vaqti ~1500ms bo'ladi va ular aniq navbat bilan natija qaytaradi.

---

### Question 4. Calling suspend from normal code (compile error and fixes)

#### Kod:
```kotlin
import kotlinx.coroutines.*

suspend fun getData(): String {
    delay(1000L)
    return "Data"
}

fun main() {
    // Xato: Suspend funksiyani oddiy funksiyadan to'g'ridan-to'g'ri chaqirib bo'lmaydi!
    // getData() -> Compile Error: Suspend function 'getData' should be called only from a coroutine or another suspend function

    // 1-tuzatish: runBlocking orqali ko'prik yaratish
    runBlocking {
        println(getData())
    }

    // 2-tuzatish: GlobalScope.launch (DIQQAT: Android UI thread'da hech qachon ishlatmang!)
    // GlobalScope.launch { println(getData()) }
}
```

#### Kutilayotgan Natija (Output):
```text
Data
```

#### Tushuntirish:
- Kompilyator `suspend` funksiyalarga yashirin `Continuation` obyektini qo'shadi. Oddiy `main` funksiyada esa koroutina konteksti mavjud emas. `runBlocking` sinxron dunyoni asinxron dunyo bilan bog'lovchi ko'prik vazifasini bajaradi. `GlobalScope.launch` esa structured concurrency'ni buzadi (memory leak xavfi bor), shuning uchun Android UI thread'ida yoki umumiy biznes mantiqda mutlaqo ishlatilmasligi kerak.

---

### Question 5. Suspend is not a thread

#### Kod:
```kotlin
import kotlinx.coroutines.*

suspend fun doWork() {
    println("Inside suspend: ${Thread.currentThread().name}")
    delay(100L)
    println("After delay: ${Thread.currentThread().name}")
}

fun main() = runBlocking {
    println("Before suspend: ${Thread.currentThread().name}")
    doWork()
    println("After suspend: ${Thread.currentThread().name}")
}
```

#### Kutilayotgan Natija (Output):
```text
Before suspend: main @coroutine#1
Inside suspend: main @coroutine#1
After delay: main @coroutine#1
After suspend: main @coroutine#1
```

#### Tushuntirish:
- Koroutina thread emas, u "engil vaznli thread" (light-weight thread). `suspend` funksiya chaqirilganda thread o'zgarmaydi (agar dispatcher maxsus o'zgartirilmagan bo'lsa). Kod bir xil `main` thread'da `main @coroutine#1` kontekstida bajarilishini davom ettiradi. `delay` esa thread'ni bloklamasdan faqat koroutina bajarilishini vaqtincha to'xtatib turadi.

---

### Question 6. Ten downloads, one thread (Concurrency vs Parallelism)

#### Kod:
```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    repeat(10) { i ->
        launch {
            delay(500L)
            println("Download $i completed on ${Thread.currentThread().name}")
        }
    }
}
```

#### Kutilayotgan Natija (Output):
```text
Download 0 completed on main @coroutine#2
Download 1 completed on main @coroutine#3
...
Download 9 completed on main @coroutine#11
(Barchasi ~500ms ichida bajariladi)
```

#### Tushuntirish:
- **Konkurensiya (Concurrency)** — ko'p vazifalarni bir vaqtning o'zida boshqarish (bitta thread'da navbat bilan yoki vaqtni taqsimlab bajarish). **Parallelizm (Parallelism)** — vazifalarni aynan bir vaqtning o'zida (turli yadrolarda/thread'larda) bajarish. Bu yerda 10 ta koroutina bitta `main` thread'da konkurent tarzda ishlaydi (`delay` tufayli biri to'xtaganda boshqasi ishlaydi), shuning uchun barchasi 500ms da tugaydi.

---

### Question 7. Order of launch

#### Kod:
```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    println("1")
    launch {
        delay(100L)
        println("2")
    }
    launch {
        println("3")
    }
    println("4")
}
```

#### Kutilayotgan Natija (Output):
```text
1
4
3
2
```

#### Tushuntirish:
1. `"1"` chop etiladi.
2. 1-koroutina (`launch` with delay) navbatga qo'yiladi.
3. 2-koroutina (`launch` without delay) navbatga qo'yiladi va darhol `"3"` ni chop etadi (chunki delay yo'q).
4. `"4"` chop etiladi (runBlocking oxiri).
5. 100ms o'tgach, 1-koroutina `"2"` ni chop etadi. `launch` hech qachon boshqaruvni darhol band qilmaydi, u shunchaki koroutinani rejalashtiradi (`schedule`).

---

### Question 8. Parent waits for child (Structured Concurrency)

#### Kod:
```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val job = launch {
        launch {
            delay(500L)
            println("Child 1 completed")
        }
        launch {
            delay(300L)
            println("Child 2 completed")
        }
        println("Parent body running")
    }
    job.join()
    println("Parent completed")
}
```

#### Kutilayotgan Natija (Output):
```text
Parent body running
Child 2 completed
Child 1 completed
Parent completed
```

#### Tushuntirish:
- Strukturaviy konkurensiya qoidasiga ko'ra, ota koroutina (`parent`) o'zining barcha bola koroutinalari (`children`) tugashini kutmasdan yakunlana olmaydi. `job.join()` ota koroutina to'liq tugashini kutadi. Bola 2 (300ms) oldin, Bola 1 (500ms) keyin tugaydi, va eng oxirida ota koroutina tugallanadi.

---

### Question 9. Screen closed, requests cancelled

#### Kod:
```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val parent = launch {
        launch {
            try {
                delay(1000L)
                println("Task 1 finished")
            } finally {
                println("Task 1 cleaned up")
            }
        }
        launch {
            try {
                delay(1000L)
                println("Task 2 finished")
            } finally {
                println("Task 2 cleaned up")
            }
        }
    }
    delay(200L)
    parent.cancelAndJoin()
    println("Parent cancelled")
}
```

#### Kutilayotgan Natija (Output):
```text
Task 1 cleaned up
Task 2 cleaned up
Parent cancelled
```

#### Tushuntirish:
- Ota koroutina bekor qilinganda (`cancelAndJoin`), bu signal uning barcha bolalariga avtomatik ravishda tarqatiladi (`cancellation propagation`). Bola koroutinalar `CancellationException` olib to'xtaydi va `finally` bloklari orqali resurslarni xavfsiz tozalash ishlarini (`cleanup`) bajaradi.

---

### Question 10. Profile ready (`coroutineScope`)

#### Kod:
```kotlin
import kotlinx.coroutines.*

suspend fun fetchProfile() = coroutineScope {
    launch {
        delay(300L)
        println("Avatar loaded")
    }
    launch {
        delay(200L)
        println("Bio loaded")
    }
    println("Profile fetching started")
}

fun main() = runBlocking {
    fetchProfile()
    println("Profile ready")
}
```

#### Kutilayotgan Natija (Output):
```text
Profile fetching started
Bio loaded
Avatar loaded
Profile ready
```

#### Tushuntirish:
- `coroutineScope` bu — suspend builder bo'lib, o'z ichidagi barcha bola koroutinalar tugaguncha chaqiruvchi kodni to'xtatib turadi. Shuning uchun `"Profile ready"` faqatgina `"Bio loaded"` va `"Avatar loaded"` ikkalasi ham to'liq bajarilib bo'lgandan keyingina konsolga chiqadi.

---

### Question 11. Reusable counter (Extension function returning Job)

#### Kod:
```kotlin
import kotlinx.coroutines.*

fun CoroutineScope.startCounter(limit: Int): Job = launch {
    for (i in 1..limit) {
        delay(100L)
        println("Count: $i")
    }
}

fun main() = runBlocking {
    val counterJob = startCounter(3)
    counterJob.join()
    println("Counter finished")
}
```

#### Kutilayotgan Natija (Output):
```text
Count: 1
Count: 2
Count: 3
Counter finished
```

#### Tushuntirish:
- `CoroutineScope` ga extension funksiya yozish orqali koroutinalarni inkapsulyatsiya qilish mumkin. `launch` funksiyasi `Job` qaytaradi, bu esa chaqiruvchiga (caller) ish jarayonini boshqarish (`join()`, `cancel()`) imkonini beradi.

---

### Question 12. Returns at once or waits (Extension vs suspend scope)

#### Kod:
```kotlin
import kotlinx.coroutines.*

fun CoroutineScope.asyncTask() = launch {
    delay(500L)
    println("Async task done")
}

suspend fun syncTaskScope() = coroutineScope {
    launch {
        delay(500L)
        println("Sync scope task done")
    }
    println("Scope block finished")
}

fun main() = runBlocking {
    println("--- asyncTask ---")
    asyncTask()
    println("after asyncTask")

    println("--- syncTaskScope ---")
    syncTaskScope()
    println("after syncTaskScope")
}
```

#### Kutilayotgan Natija (Output):
```text
--- asyncTask ---
after asyncTask
Async task done
--- syncTaskScope ---
Scope block finished
Sync scope task done
after syncTaskScope
```

#### Tushuntirish:
- `launch` o'z ichidagi kod tugashini kutmasdan darhol `Job` qaytarib o'tadi (`after asyncTask` oldin chiqadi). `coroutineScope` esa `suspend` funksiya bo'lgani uchun ichidagi barcha vazifalar yakunlanmaguncha chaqiruvchini to'xtatib turadi (`Scope block finished` va `Sync scope task done` chiqib, keyingiga o'tadi).

---

### Question 13. Five builders cheat sheet

#### Kod:
```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    // 1. runBlocking: thread'ni bloklaydi, natija qaytaradi
    val rb = runBlocking { delay(50L); "runBlocking" }
    println(rb)

    // 2. launch: non-blocking, Job qaytaradi
    val job = launch { delay(50L); println("launch done") }
    job.join()

    // 3. async: non-blocking, Deferred<T> qaytaradi
    val deferred = async { delay(50L); 42 }
    println("async: ${deferred.await()}")

    // 4. coroutineScope: suspend function, bolalarni kutadi
    val cs = coroutineScope { delay(50L); "coroutineScope" }
    println(cs)

    // 5. withContext: dispatcher'ni o'zgartiradi, suspend qiladi
    val wc = withContext(Dispatchers.Default) { delay(50L); "withContext" }
    println(wc)
}
```

#### Kutilayotgan Natija (Output):
```text
runBlocking
launch done
async: 42
coroutineScope
withContext
```

#### Tushuntirish (Cheat Sheet):
- **`runBlocking`**: Joriy thread'ni bloklaydi; lambda natijasini qaytaradi; barcha bolalarni kutadi.
- **`launch`**: Bloksiz ishlaydi; `Job` qaytaradi; bolalar tugashini avtomatik kutmaydi (lekin scope doirasida boshqariladi).
- **`async`**: Bloksiz ishlaydi; `Deferred<T>` (`await()` orqali natija olish) qaytaradi.
- **`coroutineScope`**: Suspend funksiya; bolalar tugashini kutadi; blok natijasini qaytaradi.
- **`withContext`**: Suspend funksiya; kontekstni (dispatcher) o'zgartiradi; blok natijasini qaytaradi.

---

### Question 14. Job or Deferred

#### Kod:
```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val job: Job = launch {
        delay(100L)
        println("Job running")
    }
    job.join()

    val deferred: Deferred<Int> = async {
        delay(100L)
        42
    }
    val result = deferred.await()
    println("Deferred result: $result")
}
```

#### Kutilayotgan Natija (Output):
```text
Job running
Deferred result: 42
```

#### Tushuntirish:
- `Job` — bu faqatgina hayot sikliga ega (Active, Completed, Cancelled) bajariluvchi vazifa, u natija qaytarmaydi. `Deferred<T>` esa `Job`'ning avloddoshi (subclass) bo'lib, o'z ichida `await()` orqali qiymat (`T`) qaytarish imkoniyatini saqlaydi.

---

### Question 15. Three exports in a scope

#### Kod:
```kotlin
import kotlinx.coroutines.*

suspend fun exportData() = coroutineScope {
    launch {
        delay(300L)
        println("Export 1 done")
    }
    launch {
        delay(100L)
        println("Export 2 done")
    }
    launch {
        delay(200L)
        println("Export 3 done")
    }
}

fun main() = runBlocking {
    exportData()
    println("All done")
}
```

#### Kutilayotgan Natija (Output):
```text
Export 2 done
Export 3 done
Export 1 done
All done
```

#### Tushuntirish:
- `coroutineScope` tufayli `exportData()` funksiyasi ichidagi uchala eksport koroutinasi tugaguncha `main` dagi `"All done"` chop etilishi to'xtatib turiladi. Hech qanday qo'lda `join()` yoki `delay()` ishlatish talab etilmaydi; strukturaviy konkurensiya buni avtomatik kafolatlaydi.

---

## 2. Qo'shimcha Topshiriqlar (Extra 1–5)

### Extra 1. Delay or sleep: measure it

#### Kod:
```kotlin
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

fun main() = runBlocking {
    val delayTime = measureTimeMillis {
        coroutineScope {
            repeat(3) { launch { delay(500L) } }
        }
    }
    println("Delay time: ${delayTime}ms")

    val sleepTime = measureTimeMillis {
        repeat(3) {
            launch(Dispatchers.Default) { Thread.sleep(500L) }
        }
    }
    println("Sleep time: ${sleepTime}ms")
}
```

#### Kutilayotgan Natija (Output):
```text
Delay time: ~500ms
Sleep time: ~500ms
```

#### Tushuntirish:
- `delay` koroutinani cooperatively to'xtatadi va thread'ni boshqa vazifalar uchun bo'shatadi. `Thread.sleep` esa OS thread'ini to'g'ridan-to'g'ri bloklaydi. Garchi vaqt jihatidan ikkalasi ham parallel ishlaganda bir xil ko'rinsa-da, `Thread.sleep` thread resurslarini isrof qiladi, `delay` esa juda yengil va samarali.

---

### Extra 2. Three levels of coroutines (Coroutine Tree)

#### Kod:
```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    println("Parent scope started")
    launch { // Child 1
        println("Child 1 started")
        launch { // Grandchild
            delay(200L)
            println("Grandchild finished")
        }
        delay(100L)
        println("Child 1 finished")
    }
    launch { // Child 2
        delay(150L)
        println("Child 2 finished")
    }
    println("Parent scope end")
}
```

#### Kutilayotgan Natija (Output):
```text
Parent scope started
Parent scope end
Child 1 started
Child 2 finished
Child 1 finished
Grandchild finished
```

#### Tushuntirish:
Iyerarxik daraxt tuzilishi:
```text
runBlocking (Root)
 ├── launch (Child 1)
 │    └── launch (Grandchild)
 └── launch (Child 2)
```
Ota koroutina o'z bolalari va nabiralari tugashini kutadi. Child 1 100ms da tugagandek ko'rinsa-da, uning bolasi (Grandchild) 200ms da tugagani uchun Child 1 butunlay yakunlanishi Grandchild'ga bog'liq bo'ladi.

---

### Extra 3. No scope, no launch

#### Kod:
```kotlin
import kotlinx.coroutines.*

// fun main() {
//     launch { delay(1000L) } // Xato: Unresolved reference: launch
// }

// Tuzatilgan holat:
fun main() = runBlocking {
    launch {
        delay(1000L)
        println("Launched inside runBlocking")
    }
}
```

#### Kutilayotgan Natija (Output):
```text
Launched inside runBlocking
```

#### Tushuntirish:
- `launch` funksiyasi `CoroutineScope` interfeysining extension funksiyasidir. Agar scope bo'lmasa, kompilyator `launch`'ni tanimaydi. `runBlocking` yoki `coroutineScope` orqali scope ta'minlanishi shart.

---

### Extra 4. What the compiler sees (Decompiled signature & CPS)

#### Kod va Nazariy Tushuntirish:
```kotlin
import kotlinx.coroutines.*
import kotlin.coroutines.Continuation

suspend fun compute(x: Int): Int {
    delay(100L)
    return x * 2
}
```

- Kompilyator ostida (Bytecode darajasida) `suspend fun compute(x: Int): Int` quyidagi ko'rinishga keladi (**Continuation-Passing Style - CPS**):
  ```kotlin
  fun compute(x: Int, $completion: Continuation<Int>): Any?
  ```
- **O'zgarishlar:**
  1. Qo'shimcha parametr: `$completion` (natijani qaytarish va qayta tiklash uchun).
  2. Qaytish turi: `Any?` (agar funksiya to'xtatilsa `COROUTINE_SUSPENDED` qaytariladi, aks holda tayyor natija).

---

### Extra 5. Greet everybody

#### Kod:
```kotlin
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

suspend fun greetEverybody(names: List<String>) = coroutineScope {
    names.forEachIndexed { index, name ->
        launch {
            val pause = (index + 1) * 100L
            delay(pause)
            println("Hello, $name! (paused ${pause}ms)")
        }
    }
}

fun main() = runBlocking {
    val names = listOf("Alice", "Bob", "Charlie", "David")
    println("--- Before greetEverybody ---")
    val time = measureTimeMillis {
        greetEverybody(names)
    }
    println("--- After greetEverybody (Total time: ${time}ms) ---")
}
```

#### Kutilayotgan Natija (Output):
```text
--- Before greetEverybody ---
Hello, Alice! (paused 100ms)
Hello, Bob! (paused 200ms)
Hello, Charlie! (paused 300ms)
Hello, David! (paused 400ms)
--- After greetEverybody (Total time: ~400ms) ---
```

#### Tushuntirish:
- `greetEverybody` funksiyasi `coroutineScope` ichida har bir ism uchun alohida `launch` ochadi. Ular bir vaqtning o'zida parallel ravishda ishlaydi (concurrent). Shuning uchun umumiy vaqt ularning yig'indisi (1000ms) emas, balki eng uzun kechikish vaqti bo'lgan ~400ms ni tashkil qiladi. `coroutineScope` barcha salomlashishlar tugamaguncha `"After greetEverybody"` ni chiqarmasligini kafolatlaydi.
