package studio.milda.lsp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Application-level, persisted settings for the Milda LSP integration: which Node
 * runtime to use and (optionally) an explicit path to the server script overriding
 * auto-detection.
 */
@Service(Service.Level.APP)
@State(name = "MildaLspSettings", storages = [Storage("milda-lsp.xml")])
class MildaLspSettings : PersistentStateComponent<MildaLspSettings.State> {
    data class State(
        var nodeExecutable: String = "node",
        var serverScriptPath: String = "",
    )

    private var state = State()

    var nodeExecutable: String
        get() = state.nodeExecutable
        set(value) {
            state.nodeExecutable = value
        }

    var serverScriptPath: String
        get() = state.serverScriptPath
        set(value) {
            state.serverScriptPath = value
        }

    override fun getState(): State = state

    override fun loadState(newState: State) {
        state = newState
    }

    companion object {
        fun getInstance(): MildaLspSettings =
            ApplicationManager.getApplication().getService(MildaLspSettings::class.java)
    }
}
