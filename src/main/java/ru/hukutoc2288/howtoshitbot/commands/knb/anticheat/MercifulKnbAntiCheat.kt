package ru.hukutoc2288.howtoshitbot.commands.knb.anticheat

/**
 * Античиты для КНБ в Сратьботе обычно представляют собой закрытые библиотеки
 * с целью сокрытия алгоритмов проверки и недопущения их обхода.
 * MercifulKnbAntiCheat – это заглушка, которая используется, если не предоставлен
 * иной античит. Этот "античит" ничего не проверяет, и всегда возвращает 0, за что
 * и получил своё название.
 * С точки зрения предотвращения жульничества он абсолютно бесполезен. Прямо как ты.
 */
class MercifulKnbAntiCheat: KnbAntiCheat {
    
    override fun checkCheating(games: Map<Int, Int>): Int {
        return 0
    }
}