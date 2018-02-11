import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.TelegramBotsApi

fun main(args: Array<String>) {
    ApiContextInitializer.init()
    val botApi = TelegramBotsApi()
    botApi.registerBot(MyBot())
}