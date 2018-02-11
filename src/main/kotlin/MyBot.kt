import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.methods.send.SendSticker
import org.telegram.telegrambots.api.objects.Update
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * code������,��Ҫ�����ö��Խ�������취����� -_-
 */
class MyBot : TelegramLongPollingBot() {

    val stickers = HashSet<String>()

    val stickerFile = File("stickers.txt")

    val logFile = File("log.txt")

    val groups = HashMap<Long, Calendar>() // key:chatId,value:���˵����ʱ��

    val threads = HashMap<Long, Thread>() // chatId���̵߳�ӳ��

    val random = Random()

    val canAdd = listOf<String>("kotomei", "RikkaW", "WordlessEcho", "Duang", "hyx01",
            "yiheng233", "KaCyan", "LetITFly", "YuutaW")
    var min = 9 * 60 // ��Сʱ��,��λΪ��
    var max = 15 * 60 // ���ʱ��(������)


    inner class runThread(val chatId: Long) : Thread() {
        override fun run() {
            try {
                log("Ⱥ�� $chatId ��ʼ����")
                while (true) {
                    val shouldSleep = random.nextInt(max - min) + min
                    log("�´η���:$shouldSleep")
                    Thread.sleep((shouldSleep * 1000).toLong())
                    var sendSticker = SendSticker().setChatId(chatId).setSticker(getRandomSticker())
                    try {
                        sendSticker(sendSticker)
                    } catch (e: Exception) {
                        log("��Ⱥ�� $chatId �з���sticker����\n" + e.message)
                    }
                    log("��$chatId ������sticker")
                }
            } catch (e: Exception) {
                log("$chatId deleted")
            }
        }
    }

    init {
        if (!stickerFile.exists()) {
            stickerFile.createNewFile()
        }
        if (!logFile.exists()) {
            logFile.createNewFile()
        } else {
            logFile.writeText("------------------------------\n")
        }
        // ��ȡsticker����
        stickerFile.useLines { it.forEach { stickers.add(it) } }
        // һ���µ������ж��Ƿ�Ӧ��ɾ��һ��Ⱥ����߳�,5����ִ��һ��
        Thread {
            while (true) {
                log("��ʼ���Ⱥ����Ϣ")
                val now = GregorianCalendar()
                now.roll(Calendar.MINUTE, -5)
                val shouldDelete = ArrayList<Long>()
                groups.forEach { (chatId, calendar) ->
                    if (now.after(calendar)) { // ���˵���ھ������ڳ���5����
                        shouldDelete.add(chatId)
                    }
                }
                shouldDelete.forEach {
                    if (groups.remove(it) != null) {
                        log("ɾ����Ⱥ��$it")
                        threads.get(it)?.interrupt()
                        threads.remove(it)
                    }
                }
                Thread.sleep(5 * 60 * 1000)
            }
        }.start()
    }


    override fun onUpdateReceived(update: Update?) {
        if (update == null) {
            return
        }
        if (!update.hasMessage()) {
            return
        }
        val message = update.message
        if (message.isGroupMessage || message.isSuperGroupMessage) {
            if (groups.containsKey(message.chatId)) {  // Ⱥ���Ѿ�����
                groups.remove(message.chatId)
                groups.put(message.chatId, GregorianCalendar()) // ˢ�����ʱ��
                log("ˢ����${message.chatId}�����˵��ʱ��")
            } else {
                groups.put(message.chatId, GregorianCalendar())
                log("������Ⱥ��:${message.chatId}")
                val thread = runThread(message.chatId)
                threads.put(message.chatId, thread)
                thread.start()
            }
        }
        if (message.sticker != null && message.isUserMessage) {                // ��sticker
            if (message.chat.userName in canAdd) {                             // ��addȨ��
                if (stickers.add(message.sticker.fileId)) {
                    stickerFile.appendText("${message.sticker.fileId}\n")
                    log("${message.chat.userName}�����" + message.sticker.fileId)
                    execute(SendMessage(message.chatId, "�����һ��sticker,file idΪ${message.sticker.fileId}"))
                } else {
                    execute(SendMessage(message.chatId, "sticker�Ѿ�����"))
                }
            } else {
                execute(SendMessage(message.chatId, "û��addȨ��"))
            }
        }
        if (message.text == null) {
            return
        }
        if (message.isCommand) {
            log("get a command from ${message.chat.firstName},called ${message.text}")
            when {
                message.text.equals("/get_stickers") -> {
                    if (message.isUserMessage) {
                        val content = StringBuffer()
                        stickers.forEach { content.append(it).append("\n") }
                        content.append("count:" + stickers.size)
                        execute(SendMessage(message.chatId, content.toString()))
                    }
                }
            }
        }

        if (message.text.contains("/sticker")) {
            log("��${message.chatId}��ͨ��ָ�����һ��sticker")
            var sendSticker = SendSticker().setChatId(message.chatId).setSticker(getRandomSticker())
            try {
                sendSticker(sendSticker)
            } catch (e: Exception) {
                log("��Ⱥ�� ${message.chatId} �з���sticker����\n" + e.message)
            }
        }
    }


    override fun getBotToken(): String {
        return botToken
    }

    override fun getBotUsername(): String {
        return botName
    }

    fun getRandomSticker(): String {
        val randInt = random.nextInt(stickers.size)
        return stickers.toArray()[randInt] as String
    }

    fun log(message: Any?) {
        val dateFormat = SimpleDateFormat("[yyyyMMdd HH:mm:ss]")
        val date = dateFormat.format(Date())
        println(date + (message?.toString() ?: "null"))
        logFile.appendText(date + (message?.toString() ?: "null") + "\n")
    }
}