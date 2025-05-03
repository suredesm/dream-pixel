package lk.sure.dream.compose.components.canvas

import android.graphics.Bitmap
import androidx.annotation.IntRange
import androidx.compose.runtime.mutableStateListOf

class UndoRedoManager(
    maxStackSize: Int = 32,
) {
    private val undoStack = Stack<Work>(maxStackSize)
    private val redoStack = Stack<Work>(maxStackSize)
    private var currentWork: Work? = null

    fun pushAction(work: Work) {
        currentWork?.also {
            undoStack.push(it)
            redoStack.clear()
        }

        currentWork = work
    }

    fun undo(): Work {
        return undoStack.pop().also { work ->
            currentWork?.let { redoStack.push(it) }
            currentWork = work
        }
    }

    fun redo(): Work {
        return redoStack.pop().also { work ->
            currentWork?.let { undoStack.push(it) }
            currentWork = work
        }
    }

    fun canUndo() = undoStack.isNotEmpty()

    fun canRedo() = redoStack.isNotEmpty()

    fun configure(initialWork: Work) {
        currentWork = initialWork
    }
}

data class Work(
    val layerIndex: Int,
    val bitmap: Bitmap,
)

class Stack<T>(
    @IntRange(from = 1) val maxStackSize: Int = 16,
) {
    private val stack = mutableStateListOf<T>()

    fun push(item: T) {
        if (stack.size == maxStackSize)
            stack.removeAt(0)

        stack.add(item)
    }

    fun pop(): T = stack.removeAt(stack.lastIndex)

    fun isNotEmpty() = stack.isNotEmpty()

    fun clear() = stack.clear()
}