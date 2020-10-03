package com.emogoth.android.phone.mimi.db.extensions

import com.emogoth.android.phone.mimi.db.models.Board
import com.mimireader.chanlib.models.ChanBoard

fun List<Board>.toChanBoards(boards: List<Board>): List<ChanBoard> {
    val chanBoards = ArrayList<ChanBoard>(boards.size)
    for (board in boards) {
        chanBoards.add(board.toChanBoard())
    }

    return chanBoards
}