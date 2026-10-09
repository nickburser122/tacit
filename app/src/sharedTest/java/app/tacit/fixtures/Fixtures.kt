package app.tacit.fixtures

import app.tacit.core.ItemKind
import app.tacit.core.SearchItem
import java.util.Random

object Fixtures {

    val namedContacts = listOf(
        "Asmaa Gouda", "أسماء جودة", "Ahmed Galal", "Gamal Osman", "Mohamed Salah", "محمد صلاح",
        "Nour El-Din", "Zoë Müller", "José Álvarez", "François Lefèvre", "Fatma Abdel-Rahman", "يوسف إبراهيم"
    )

    val namedApps = listOf(
        "YouTube", "YouTube Music", "WhatsApp", "WhatsApp Business", "Gmail", "Google", "Maps", "Calculator",
        "Calendar", "Camera", "Chrome", "Firefox", "Signal", "Telegram", "Spotify", "Settings", "Files", "Photos",
        "Clock", "Contacts", "Phone", "Messages", "Play Store", "F-Droid", "Termux", "K-9 Mail", "Aegis", "Bitwarden"
    )

    private val latinFirst = listOf(
        "Ahmed", "Mohamed", "Mahmoud", "Omar", "Ali", "Hassan", "Hussein", "Mostafa", "Karim", "Tarek", "Youssef",
        "Asmaa", "Fatma", "Mariam", "Nour", "Salma", "Hana", "Laila", "Dina", "Rana", "Aya", "Sara", "Mona",
        "John", "Emma", "Liam", "Olivia", "Noah", "Zoë", "José", "Chloé", "Renée", "Björn", "Søren"
    )

    private val latinLast = listOf(
        "Gouda", "Salah", "Galal", "Osman", "Abdelaziz", "El-Sayed", "Farouk", "Hamdy", "Ibrahim", "Kamal",
        "Mansour", "Nasser", "Radwan", "Shawky", "Zaki", "Smith", "Müller", "García", "Lefèvre", "Novák", "Øster"
    )

    private val arabicFirst = listOf("أحمد", "محمد", "محمود", "عمر", "علي", "حسن", "أسماء", "فاطمة", "مريم", "نور", "سلمى", "هناء", "ليلى", "يوسف", "إبراهيم")
    private val arabicLast = listOf("جودة", "صلاح", "جلال", "عثمان", "عبدالعزيز", "السيد", "فاروق", "حمدي", "كمال", "منصور", "ناصر", "رضوان", "زكي")

    private val appWords = listOf(
        "Note", "Notes", "Task", "Fit", "Pay", "Bank", "Wallet", "Scan", "Reader", "Player", "Radio", "Weather",
        "Map", "Ride", "Food", "Shop", "Chat", "Mail", "Cloud", "Drive", "Photo", "Video", "Music", "Book",
        "Health", "Sleep", "Timer", "Flash", "Vault", "Key", "Lens", "Draw", "Code", "Term", "Pod", "News"
    )
    private val appPrefixes = listOf("Simple", "Quick", "Open", "Smart", "Pro", "Lite", "Easy", "My", "Super", "Tiny", "")

    fun apps(count: Int = 1000, seed: Long = 11L): List<SearchItem> {
        val random = Random(seed)
        val result = ArrayList<SearchItem>(count)
        for (name in namedApps) result.add(SearchItem("app:fixture/$name", ItemKind.APP, name))
        var index = 0
        while (result.size < count) {
            val name = listOf(appPrefixes[random.nextInt(appPrefixes.size)], appWords[random.nextInt(appWords.size)],
                appWords[random.nextInt(appWords.size)]).filter { it.isNotEmpty() }.joinToString(" ") + " " + index
            result.add(SearchItem("app:fixture/$index", ItemKind.APP, name))
            index++
        }
        return result
    }

    fun contacts(count: Int = 3000, seed: Long = 17L): List<SearchItem> {
        val random = Random(seed)
        val result = ArrayList<SearchItem>(count)
        for (name in namedContacts) result.add(SearchItem("contact:fixture/$name", ItemKind.CONTACT, name, "+20100000000"))
        var index = 0
        while (result.size < count) {
            val name = when (random.nextInt(10)) {
                in 0..3 -> arabicFirst[random.nextInt(arabicFirst.size)] + " " + arabicLast[random.nextInt(arabicLast.size)]
                4 -> arabicFirst[random.nextInt(arabicFirst.size)] + " " + latinLast[random.nextInt(latinLast.size)]
                else -> latinFirst[random.nextInt(latinFirst.size)] + " " + latinLast[random.nextInt(latinLast.size)]
            }
            val number = "+2010" + (10_000_000 + random.nextInt(89_999_999))
            result.add(SearchItem("contact:fixture/$index", ItemKind.CONTACT, "$name $index", number, listOf(number)))
            index++
        }
        return result
    }

    fun all(): List<SearchItem> = apps() + contacts()

    val typingScript: List<String> = listOf(
        "g", "go", "gos", "", "y", "yt", "", "w", "wf", "wfi", "", "b", "bl", "blu", "blut", "bluto",
        "blutoo", "", "a", "as", "asm", " asm g", "asm go", "asm gou", "", "م", "مح", "محم", "", "zoe"
    )
}
