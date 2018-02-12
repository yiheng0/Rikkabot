package moe.yiheng

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
 * code很辣鸡,主要是懒得动脑筋就用死办法解决了 -_-
 */
class MyBot : TelegramLongPollingBot() {

    val stickers = HashSet<String>()

    val stickerFile = File("stickers.txt")

    val logFile = File("log.txt")

    val groups = HashMap<Long, Calendar>() // key:chatId,value:最后说话的时间

    val threads = HashMap<Long, Thread>() // chatId与线程的映射

    val random = Random()

    val canAdd = listOf<String>("kotomei", "RikkaW", "WordlessEcho", "Duang", "hyx01",
            "yiheng233", "KaCyan", "LetITFly", "YuutaW")

    val text = listOf<String>("好耶","坏耶","不 可 以")

    var min = 9 * 60 // 最小时间,单位为秒
    var max = 15 * 60 // 最大时间(不包括)


    inner class runThread(val chatId: Long) : Thread() {
        override fun run() {
            try {
                log("群组 $chatId 开始运行")
                while (true) {
                    val shouldSleep = random.nextInt(max - min) + min
                    log("下次发送:$shouldSleep")
                    Thread.sleep((shouldSleep * 1000).toLong())
                    when (random.nextInt(20)) {    //二十分之一的几率发文字
                        1 -> { execute(SendMessage(chatId, text[random.nextInt(text.size)])) }                          // 发文字
                        else -> {
                            var sendSticker = SendSticker().setChatId(chatId).setSticker(getRandomSticker())
                            try {
                                sendSticker(sendSticker)
                            } catch (e: Exception) {
                                log("在群组 $chatId 中发送sticker出错\n" + e.message)
                            }
                        }
                    }
                    log("向$chatId 发送了sticker或文字")
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
        // 读取sticker数据
        stickerFile.useLines { it.forEach { stickers.add(it) } }
        // 一个新的用来判断是否应当删除一个群组的线程,5分钟执行一次
        Thread {
            while (true) {
                log("开始检查群组消息")
                val now = GregorianCalendar()
                now.roll(Calendar.MINUTE, -5)
                val shouldDelete = ArrayList<Long>()
                groups.forEach { (chatId, calendar) ->
                    if (now.after(calendar)) { // 最后说话在距离现在超过5分钟
                        shouldDelete.add(chatId)
                    }
                }
                shouldDelete.forEach {
                    if (groups.remove(it) != null) {
                        log("删除了群组$it")
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
            if (groups.containsKey(message.chatId)) {  // 群组已经存在
                groups.remove(message.chatId)
                groups.put(message.chatId, GregorianCalendar()) // 刷新最后时间
                log("${message.chat.title}中,@${message.from.userName} 说:\"${message.text}\",message id为${message.messageId}")
            } else {
                groups.put(message.chatId, GregorianCalendar())
                log("发现新群组:${message.chat.title},id为${message.chatId}")
                val thread = runThread(message.chatId)
                threads.put(message.chatId, thread)
                thread.start()
            }
        }
        if (message.sticker != null && message.isUserMessage) { // 是sticker
            if (message.chat.userName in canAdd) {                             // 有add权限
                if (stickers.add(message.sticker.fileId)) {
                    stickerFile.appendText("${message.sticker.fileId}\n")
                    log("${message.chat.userName}添加了" + message.sticker.fileId)
                    execute(SendMessage(message.chatId, "添加了一个sticker,file id为${message.sticker.fileId}"))
                } else {
                    execute(SendMessage(message.chatId, "fileid为${message.sticker.fileId}的\nsticker已经存在"))
                }
            } else {
                execute(SendMessage(message.chatId, "没有add权限"))
            }
        }
        if (message.text == null) {
            return
        }
        if (message.isCommand) {
            log("get a command from ${message.from.userName}, saying ${message.text}")
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
            log("在${message.chatId}中通过指令发送了一个sticker")
            var sendSticker = SendSticker().setChatId(message.chatId).setSticker(getRandomSticker())
            try {
                sendSticker(sendSticker)
            } catch (e: Exception) {
                log("在群组 ${message.chat.title} 中发送sticker出错\n" + e.message)
            }
        }
    }


    override fun getBotToken(): String {
        return Config.token
    }

    override fun getBotUsername(): String {
        return Config.botName
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